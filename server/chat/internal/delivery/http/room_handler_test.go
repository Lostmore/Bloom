package http

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"bloom.local/chat/internal/delivery/websocket"
	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
	"github.com/stretchr/testify/require"
)

var (
	u1 = uuid.MustParse("00000000-0000-0000-0000-000000000001")
	u2 = uuid.MustParse("00000000-0000-0000-0000-000000000002")
	u3 = uuid.MustParse("00000000-0000-0000-0000-000000000003")
)

type MockMessageRepo struct {
	SavedMessage *domain.Message
}
type MockRoomRepo struct {
	CreatedRoom *domain.Room
}

func (m *MockMessageRepo) Save(ctx context.Context, msg *domain.Message) error {
	msg.ID = 999
	m.SavedMessage = msg
	return nil
}
func (m *MockMessageRepo) GetByRoomID(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	return nil, nil
}
func (m *MockRoomRepo) CreateRoom(ctx context.Context, user1ID, user2ID uuid.UUID) (*domain.Room, error) {
	room := &domain.Room{
		ID:        1,
		User1ID:   user1ID,
		User2ID:   user2ID,
		IsActive:  true,
		CreatedAt: time.Now(),
	}
	m.CreatedRoom = room
	return room, nil
}

func (m *MockRoomRepo) GetByUser(ctx context.Context, userID uuid.UUID) ([]domain.Room, error) {
	return nil, nil
}

func (m *MockRoomRepo) GetRoomByUsers(ctx context.Context, user1ID, user2ID uuid.UUID) (*domain.Room, error) {
	return nil, nil // not found, creates a new one
}

func (m *MockRoomRepo) GetRoomByID(ctx context.Context, roomID int64) (*domain.Room, error) {
	return &domain.Room{ID: 1, User1ID: u1, User2ID: u2}, nil
}
func TestRoomHandler_CreateRoom(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	svc := service.NewMessageService(mockMsgRepo, mochRoomRepo)

	// Create request with body containing only user2_id
	req, _ := http.NewRequest(http.MethodPost, "/api/conversations", bytes.NewBufferString(`{"user2_id": "00000000-0000-0000-0000-000000000002"}`))

	// Inject user_id into context as AuthMiddleware would do
	ctx := context.WithValue(req.Context(), UserIDKey, u1)
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	hub := websocket.NewHub(svc)
	go hub.Run()
	handler := NewRoomHandler(svc, hub)
	handler.CreateRoom(rr, req)

	var createdRoom domain.Room
	err := json.NewDecoder(rr.Body).Decode(&createdRoom)
	require.NoError(t, err)
	require.Equal(t, int64(1), createdRoom.ID)
	require.Equal(t, u1, createdRoom.User1ID)
	require.Equal(t, u2, createdRoom.User2ID)
	require.True(t, createdRoom.IsActive)
	require.NotNil(t, createdRoom.CreatedAt)
	require.Equal(t, http.StatusCreated, rr.Code)
}
func TestRoomHandler_GetUserRooms(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	svc := service.NewMessageService(mockMsgRepo, mochRoomRepo)

	req, _ := http.NewRequest(http.MethodGet, "/api/rooms", nil)

	// Inject user_id into context
	ctx := context.WithValue(req.Context(), UserIDKey, u1)
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	hub := websocket.NewHub(svc)
	handler := NewRoomHandler(svc, hub)
	handler.GetUserRooms(rr, req)

	var rooms []domain.Room
	err := json.NewDecoder(rr.Body).Decode(&rooms)
	require.NoError(t, err)
	require.Equal(t, http.StatusOK, rr.Code)
	require.Len(t, rooms, 0)
}

func TestRoomHandler_GetRoomHistory(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	svc := service.NewMessageService(mockMsgRepo, mochRoomRepo)

	req, _ := http.NewRequest(http.MethodGet, "/api/rooms/1/history", nil)
	req.SetPathValue("id", "1")

	// Inject user_id into context
	ctx := context.WithValue(req.Context(), UserIDKey, u1)
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	hub := websocket.NewHub(svc)
	handler := NewRoomHandler(svc, hub)
	handler.GetRoomHistory(rr, req)

	require.Equal(t, http.StatusOK, rr.Code)
	var msgs []*domain.Message
	err := json.NewDecoder(rr.Body).Decode(&msgs)
	require.NoError(t, err)
	require.Len(t, msgs, 0)
}
