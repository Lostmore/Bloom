package websocket

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"fmt"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/repository/postgres"
	"bloom.local/chat/internal/service"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/google/uuid"
	gorilla "github.com/gorilla/websocket"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	tcpostgres "github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

func setupTestDB(t *testing.T) (*pgxpool.Pool, func()) {
	ctx := context.Background()
	pgContainer, err := tcpostgres.RunContainer(ctx,
		testcontainers.WithImage("postgres:15-alpine"),
		tcpostgres.WithDatabase("chatdb"),
		tcpostgres.WithUsername("testuser"),
		tcpostgres.WithPassword("testpass"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").WithOccurrence(2).WithStartupTimeout(10*time.Second),
		),
	)
	if err != nil {
		t.Skipf("Failed to start postgres container (is docker running?): %v", err)
	}
	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	require.NoError(t, err)
	m, err := migrate.New("file://../../../migrations", connStr)
	require.NoError(t, err)
	err = m.Up()
	if err != migrate.ErrNoChange {
		require.NoError(t, err)
	}
	pool, err := pgxpool.New(ctx, connStr)
	require.NoError(t, err)
	teardown := func() {
		pool.Close()
		pgContainer.Terminate(ctx)
	}
	return pool, teardown
}

var (
	u10 = uuid.MustParse("00000000-0000-0000-0000-000000000010")
	u11 = uuid.MustParse("00000000-0000-0000-0000-000000000011")
	u12 = uuid.MustParse("00000000-0000-0000-0000-000000000012")
)

type MockTokenValidator struct {
	ValidTokens map[string]uuid.UUID
}

func (m *MockTokenValidator) ValidateToken(tokenStr string) (uuid.UUID, error) {
	if id, ok := m.ValidTokens[tokenStr]; ok {
		return id, nil
	}
	return uuid.Nil, fmt.Errorf("invalid token")
}

func TestE2EChat(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()

	msgrepo := postgres.NewMessageRepo(pool)
	roomrepo := postgres.NewRoomRepo(pool)
	svc := service.NewMessageService(msgrepo, roomrepo)
	hub := NewHub(svc)
	go hub.Run()

	validator := &MockTokenValidator{
		ValidTokens: make(map[string]uuid.UUID),
	}

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ServeWS(hub, validator, w, r)
	}))
	defer srv.Close()

	ctx := context.Background()
	room, err := svc.CreateRoom(ctx, u10, u11)
	require.NoError(t, err)

	// Helper to generate token
	generateToken := func(userID uuid.UUID) string {
		tokenStr := "token-" + userID.String()
		validator.ValidTokens[tokenStr] = userID
		return tokenStr
	}

	// 1. Connect User 10 (Allowed)
	token10 := generateToken(u10)
	u, _ := url.Parse(srv.URL)
	u.Scheme = "ws"
	u.RawQuery = "token=" + token10
	ws1, _, err := gorilla.DefaultDialer.Dial(u.String(), nil)
	require.NoError(t, err)
	defer ws1.Close()

	// 2. Connect User 11 (Allowed)
	token11 := generateToken(u11)
	u.RawQuery = "token=" + token11
	ws2, _, err := gorilla.DefaultDialer.Dial(u.String(), nil)
	require.NoError(t, err)
	defer ws2.Close()

	// 3. Connect User 12 (Forbidden - not in room)
	token12 := generateToken(u12)
	u.RawQuery = "token=" + token12
	ws3, _, err := gorilla.DefaultDialer.Dial(u.String(), nil)
	require.NoError(t, err, "dial should succeed because token is valid")
	defer ws3.Close()

	// Give the hub time to register clients
	time.Sleep(200 * time.Millisecond)

	// Helper to wait for a specific message type
	readExpectedMessage := func(ws *gorilla.Conn, expectedType string) WSMessage {
		for {
			var msg WSMessage
			err := ws.ReadJSON(&msg)
			require.NoError(t, err)
			if msg.Type == expectedType {
				return msg
			}
		}
	}

	msgContent := "hello from e2e test"
	payloadBytes, _ := json.Marshal(map[string]interface{}{"content": msgContent, "room_id": room.ID})
	err = ws1.WriteJSON(WSMessage{
		Type:    "new_message",
		Payload: payloadBytes,
	})
	require.NoError(t, err)
	require.Equal(t, "new_message", wsMsg1.Type)

	// User 10 receives broadcast
	wsMsg1 := readExpectedMessage(ws1, "new_message")
	var recMsg1 domain.Message
	err = json.Unmarshal(wsMsg1.Payload, &recMsg1)
	require.NoError(t, err)
	require.Equal(t, msgContent, recMsg1.Content)
	require.Equal(t, u10, recMsg1.SenderID)

	history, err := svc.GetRoomHistory(ctx, room.ID, u10)
	require.NoError(t, err)
	require.Len(t, history, 1)
	require.Equal(t, msgContent, history[0].Content)

	// User 11 receives broadcast
	wsMsg2 := readExpectedMessage(ws2, "new_message")
	var recMsg2 domain.Message
	err = json.Unmarshal(wsMsg2.Payload, &recMsg2)
	require.NoError(t, err)
	require.Equal(t, msgContent, recMsg2.Content)
	require.Equal(t, u10, recMsg2.SenderID)

	// User 12 should not receive anything
	ws3.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	var wsMsg3 WSMessage
	err = ws3.ReadJSON(&wsMsg3)
	require.Error(t, err, "expected read timeout since user 12 is not in the room")

	// Restore read deadline for future tests if needed
	ws3.SetReadDeadline(time.Time{})

	// --- 4. User 11 marks message as read ---
	markPayload, _ := json.Marshal(map[string]interface{}{"message_id": recMsg2.ID, "room_id": room.ID})
	err = ws2.WriteJSON(WSMessage{
		Type:    "mark_as_read",
		Payload: markPayload,
	})
	require.NoError(t, err)

	// User 10 receives broadcast
	_ = readExpectedMessage(ws1, "mark_as_read")

	_ = readExpectedMessage(ws2, "mark_as_read")

	// --- 5. User 10 edits message ---
	editPayload, _ := json.Marshal(map[string]interface{}{"message_id": recMsg2.ID, "room_id": room.ID, "content": "edited text"})
	err = ws1.WriteJSON(WSMessage{
		Type:    "edit_message",
		Payload: editPayload,
	})
	require.NoError(t, err)

	_ = readExpectedMessage(ws1, "edit_message")
	_ = readExpectedMessage(ws2, "edit_message")

	// --- 6. User 10 deletes message ---
	deletePayload, _ := json.Marshal(map[string]interface{}{"message_id": recMsg2.ID, "room_id": room.ID})
	err = ws1.WriteJSON(WSMessage{
		Type:    "delete_message",
		Payload: deletePayload,
	})
	require.NoError(t, err)

	_ = readExpectedMessage(ws1, "delete_message")
	_ = readExpectedMessage(ws2, "delete_message")
}
