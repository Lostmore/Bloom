package service

import (
	"context"
	"errors"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
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
func (m *MockMessageRepo) MarkAsRead(ctx context.Context, messageID int64, readAt time.Time, userID uuid.UUID) error {
	return nil
}
func (m *MockMessageRepo) UpdateContent(ctx context.Context, messageID int64, newContent string, editedAt time.Time, userID uuid.UUID) error {
	return nil
}
func (m *MockMessageRepo) SoftDelete(ctx context.Context, messageID int64, deletedAt time.Time, userID uuid.UUID) error {
	return nil
}
func (m *MockMessageRepo) SearchByUser(ctx context.Context, userID uuid.UUID, query string, cursor string, limit int) (*domain.MessagePage, error) {
	return &domain.MessagePage{Items: []*domain.Message{m.SavedMessage}}, nil
}
func (m *MockMessageRepo) GetAround(ctx context.Context, roomID int64, aroundID int64, limit int) (*domain.MessagePage, error) {
	return &domain.MessagePage{Items: []*domain.Message{}}, nil
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
	u1 := uuid.MustParse("00000000-0000-0000-0000-000000000001")
	u3 := uuid.MustParse("00000000-0000-0000-0000-000000000003")
	if (user1ID == u1 && user2ID == u3) || (user1ID == u3 && user2ID == u1) {
		return &domain.Room{
			ID:      2,
			User1ID: u1,
			User2ID: u3,
		}, nil
	}
	return nil, nil
}

func (m *MockRoomRepo) GetRoomByID(ctx context.Context, roomID int64) (*domain.Room, error) {
	if roomID == 1 {
		return &domain.Room{
			ID:       1,
			User1ID:  uuid.MustParse("00000000-0000-0000-0000-000000000001"),
			User2ID:  uuid.MustParse("00000000-0000-0000-0000-000000000002"),
			IsActive: true,
		}, nil
	}
	return nil, errors.New("room not found")
}

func TestMessageService_SendMessage(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()
	validMsg := &domain.Message{
		RoomID:   1,
		SenderID: uuid.MustParse("00000000-0000-0000-0000-000000000001"),
		Content:  "Hello World",
	}
	err := service.SendMessage(ctx, validMsg)
	require.NoError(t, err)
	require.NotNil(t, mockMsgRepo.SavedMessage)
	require.Equal(t, int64(999), mockMsgRepo.SavedMessage.ID)
	invalidMsg := &domain.Message{RoomID: 1, SenderID: validMsg.SenderID, Content: "   "}
	err = service.SendMessage(ctx, invalidMsg)
	require.Error(t, err)
	require.Equal(t, "message cannot be empty", err.Error())
}
func TestMessageService_CreateRoom(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()

	// 1. Success: new room
	room, err := service.CreateRoom(ctx, u1, u2)
	require.NoError(t, err)
	require.NotNil(t, room)
	require.Equal(t, u1, room.User1ID)
	require.Equal(t, u2, room.User2ID)
	require.True(t, room.IsActive)
	require.NotNil(t, room.CreatedAt)

	// 2. Fail: chat with yourself
	_, err = service.CreateRoom(ctx, u1, u1)
	require.Error(t, err)
	require.Contains(t, err.Error(), "cannot create chat with yourself")

	// 3. Success: return existing room
	existingRoom, err := service.CreateRoom(ctx, u1, u3)
	require.NoError(t, err)
	require.Equal(t, int64(2), existingRoom.ID) // from MockRoomRepo
	require.Equal(t, u1, existingRoom.User1ID)
	require.Equal(t, u3, existingRoom.User2ID)
}
func TestMessageService_GetUserRooms(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()
	rooms, err := service.GetUserRooms(ctx, u1)
	require.NoError(t, err)
	require.Equal(t, 0, len(rooms))
}

func TestMessageService_GetRoomHistory(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mochRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mochRoomRepo)
	ctx := context.Background()

	// Access allowed for User1
	_, err := service.GetRoomHistory(ctx, 1, u1)
	require.NoError(t, err)

	// Access allowed for User2
	_, err = service.GetRoomHistory(ctx, 1, u2)
	require.NoError(t, err)

	// Access denied for User3
	_, err = service.GetRoomHistory(ctx, 1, u3)
	require.Error(t, err)
	require.Contains(t, err.Error(), "access denied")

	// Room not found
	_, err = service.GetRoomHistory(ctx, 99, u1)
	require.Error(t, err)
	require.Contains(t, err.Error(), "not found")
}

func TestMessageService_Statuses(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mockRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mockRoomRepo)
	ctx := context.Background()

	err := service.MarkAsRead(ctx, 1, u1)
	require.NoError(t, err)

	err = service.EditMessage(ctx, 1, "New Content", u1)
	require.NoError(t, err)

	err = service.EditMessage(ctx, 1, "   ", u1)
	require.Error(t, err)
	require.Contains(t, err.Error(), "cannot be empty")

	err = service.DeleteMessage(ctx, 1, u1)
	require.NoError(t, err)
}

func TestMessageService_SearchMessages(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{
		SavedMessage: &domain.Message{
			ID:      42,
			Content: "hello from test",
		},
	}
	mockRoomRepo := &MockRoomRepo{}
	service := NewMessageService(mockMsgRepo, mockRoomRepo)
	ctx := context.Background()

	page, err := service.SearchMessages(ctx, u1, "hello", "100", 30)
	require.NoError(t, err)
	require.NotNil(t, page)
	require.Len(t, page.Items, 1)
	require.Equal(t, int64(42), page.Items[0].ID)
}

func TestGetMessageContext_Success(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mockRoomRepo := &MockRoomRepo{}
	svc := NewMessageService(mockMsgRepo, mockRoomRepo)

	ctx := context.Background()
	page, err := svc.GetMessageContext(ctx, 1, 42, 50, uuid.MustParse("00000000-0000-0000-0000-000000000001"))
	require.NoError(t, err)
	require.NotNil(t, page)
}

func TestGetMessageContext_AccessDenied(t *testing.T) {
	mockMsgRepo := &MockMessageRepo{}
	mockRoomRepo := &MockRoomRepo{}
	svc := NewMessageService(mockMsgRepo, mockRoomRepo)

	ctx := context.Background()
	page, err := svc.GetMessageContext(ctx, 1, 42, 50, uuid.MustParse("00000000-0000-0000-0000-000000000004"))
	require.Error(t, err)
	require.Nil(t, page)
	require.Contains(t, err.Error(), "access denied")
}

