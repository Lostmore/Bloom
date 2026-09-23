package websocket

import (
	"context"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/repository/postgres"
	"bloom.local/chat/internal/service"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
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
	require.NoError(t, err)
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

func TestE2EChat(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()
	repo := postgres.NewMessageRepo(pool)
	svc := service.NewMessageService(repo)
	hub := NewHub(svc)
	go hub.Run()
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ServeWS(hub, w, r)
	}))
	defer srv.Close()
	u, _ := url.Parse(srv.URL)
	u.Scheme = "ws"
	u.RawQuery = "room_id=1&user_id=10"
	ws, _, err := gorilla.DefaultDialer.Dial(u.String(), nil)
	require.NoError(t, err)
	defer ws.Close()
	time.Sleep(100 * time.Millisecond)
	msgContent := "hello from e2e test"
	err = ws.WriteJSON(map[string]string{"content": msgContent})
	require.NoError(t, err)
	var recMsg domain.Message
	err = ws.ReadJSON(&recMsg)
	require.NoError(t, err)
	require.Equal(t, msgContent, recMsg.Content)
	require.Equal(t, int64(10), recMsg.SenderID)
	ctx := context.Background()
	history, err := svc.GetRoomHistory(ctx, 1)
	require.NoError(t, err)
	require.Len(t, history, 1)
	require.Equal(t, msgContent, history[0].Content)
	u.RawQuery = "room_id=1&user_id=11"
	ws2, _, err := gorilla.DefaultDialer.Dial(u.String(), nil)
	require.NoError(t, err)
	defer ws2.Close()
	var recMsg2 domain.Message
	err = ws2.ReadJSON(&recMsg2)
	require.NoError(t, err)
	require.Equal(t, msgContent, recMsg2.Content)
	require.Equal(t, int64(10), recMsg2.SenderID)
}
