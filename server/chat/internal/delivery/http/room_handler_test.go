package http

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/stretchr/testify/require"
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
func (m *MockRoomRepo) CreateRoom(ctx context.Context, user1ID, user2ID int64) (*domain.Room, error) {
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

func (m *MockRoomRepo) GetByUser(ctx context.Context, userID int64) ([]domain.Room, error) {
	return nil, nil
}
func TestRoomHandler_CreateRoom(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	svc := service.NewMessageService(mockMsgRepo, mochRoomRepo)
	req, _ := http.NewRequest(http.MethodPost, "/api/rooms", bytes.NewBufferString(`{"user1_id": 1, "user2_id": 2}`))
	rr := httptest.NewRecorder()
	handler := NewRoomHandler(svc)
	handler.CreateRoom(rr, req)
	var createdRoom domain.Room
	err := json.NewDecoder(rr.Body).Decode(&createdRoom)
	require.NoError(t, err)
	require.Equal(t, int64(1), createdRoom.ID)
	require.Equal(t, int64(1), createdRoom.User1ID)
	require.Equal(t, int64(2), createdRoom.User2ID)
	require.True(t, createdRoom.IsActive)
	require.NotNil(t, createdRoom.CreatedAt)
	require.Equal(t, http.StatusCreated, rr.Code)
}
func TestRoomHandler_GetUserRooms(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	svc := service.NewMessageService(mockMsgRepo, mochRoomRepo)
	rr := httptest.NewRecorder()
	req, _ := http.NewRequest(http.MethodGet, "/api/rooms?user_id=1", nil)
	handler := NewRoomHandler(svc)
	handler.GetUserRooms(rr, req)
	var rooms []domain.Room
	err := json.NewDecoder(rr.Body).Decode(&rooms)
	require.NoError(t, err)
	require.Equal(t, http.StatusOK, rr.Code)
	require.Len(t, rooms, 0)
}
