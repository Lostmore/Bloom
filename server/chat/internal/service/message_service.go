package service

import (
	"context"
	"errors"
	"strings"

	"bloom.local/chat/internal/domain"
)

type MessageService struct {
	repo domain.MessageRepository
}

func NewMessageService(repo domain.MessageRepository) *MessageService {
	return &MessageService{repo}
}
func (s *MessageService) SendMessage(ctx context.Context, msg *domain.Message) error {
	if strings.TrimSpace(msg.Content) == "" {
		return errors.New("message cannot be empty")
	}
	err := s.repo.Save(ctx, msg)
	if err != nil {
		return err
	}
	return nil
}
func (s *MessageService) GetRoomHistory(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	return s.repo.GetByRoomID(ctx, roomID)
}
