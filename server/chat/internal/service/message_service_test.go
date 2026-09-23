package service

import (
	"context"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
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

func TestMessageService_SendMessage(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()
	validMsg := &domain.Message{Content: "Hello World"}
	err := service.SendMessage(ctx, validMsg)
	require.NoError(t, err)
	require.NotNil(t, mockMsgRepo.SavedMessage)
	require.Equal(t, int64(999), mockMsgRepo.SavedMessage.ID)
	invalidMsg := &domain.Message{Content: "   "}
	err = service.SendMessage(ctx, invalidMsg)
	require.Error(t, err)
	require.Equal(t, "message cannot be empty", err.Error())
}
func TestMessageService_CreateRoom(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()
	room, err := service.CreateRoom(ctx, int64(1), int64(2))
	require.NoError(t, err)
	require.NotNil(t, room)
	require.Equal(t, int64(1), room.User1ID)
	require.Equal(t, int64(2), room.User2ID)
	require.True(t, room.IsActive)
	require.NotNil(t, room.CreatedAt)
}
func TestMessageService_GetUserRooms(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()
	rooms, err := service.GetUserRooms(ctx, 1)
	require.NoError(t, err)
	require.Equal(t, 0, len(rooms))
}
