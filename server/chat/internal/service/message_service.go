package service

import (
	"context"
	"errors"
	"strings"

	"bloom.local/chat/internal/domain"
)

// MessageService содержит бизнес-логику для работы с сообщениями и комнатами.
type MessageService struct {
	msgRepo  domain.MessageRepository
	roomRepo domain.RoomRepository
}

// NewMessageService создает новый сервис для чата.
func NewMessageService(msgRepo domain.MessageRepository, roomRepo domain.RoomRepository) *MessageService {
	return &MessageService{msgRepo: msgRepo, roomRepo: roomRepo}
}

// SendMessage отправляет новое сообщение в чат, проверяя его содержимое на пустоту.
func (s *MessageService) SendMessage(ctx context.Context, msg *domain.Message) error {
	if strings.TrimSpace(msg.Content) == "" {
		return errors.New("message cannot be empty")
	}
	err := s.msgRepo.Save(ctx, msg)
	if err != nil {
		return err
	}
	return nil
}

// GetRoomHistory извлекает историю сообщений для указанной комнаты.
func (s *MessageService) GetRoomHistory(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	return s.msgRepo.GetByRoomID(ctx, roomID)
}

// CreateRoom создает новый чат между пользователями.
func (s *MessageService) CreateRoom(ctx context.Context, user1ID, user2ID int64) (*domain.Room, error) {
	return s.roomRepo.CreateRoom(ctx, user1ID, user2ID)
}

// GetUserRooms получает список всех чатов для конкретного пользователя.
func (s *MessageService) GetUserRooms(ctx context.Context, userID int64) ([]domain.Room, error) {
	return s.roomRepo.GetByUser(ctx, userID)
}
