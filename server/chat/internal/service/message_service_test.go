package service

import (
	"context"
	"testing"

	"bloom.local/chat/internal/domain"
	"github.com/stretchr/testify/require"
)

type MockMessageRepo struct {
	SavedMessage *domain.Message
}

func (m *MockMessageRepo) Save(ctx context.Context, msg *domain.Message) error {
	msg.ID = 999
	m.SavedMessage = msg
	return nil
}
func (m *MockMessageRepo) GetByRoomID(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	return nil, nil
}
func TestMessageService_SendMessage(t *testing.T) {
	mockRepo := &MockMessageRepo{}
	service := NewMessageService(mockRepo)
	ctx := context.Background()
	validMsg := &domain.Message{Content: "Hello World"}
	err := service.SendMessage(ctx, validMsg)
	require.NoError(t, err)
	require.NotNil(t, mockRepo.SavedMessage)
	require.Equal(t, int64(999), mockRepo.SavedMessage.ID)
	invalidMsg := &domain.Message{Content: "   "}
	err = service.SendMessage(ctx, invalidMsg)
	require.Error(t, err)
	require.Equal(t, "message cannot be empty", err.Error())
}
