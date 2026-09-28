package domain

import (
	"context"
	"time"

	"github.com/google/uuid"
)

// Room представляет собой сущность чат-комнаты между двумя пользователями.
type Room struct {
	ID        int64     `json:"id"`
	User1ID   uuid.UUID `json:"user1_id"`
	User2ID   uuid.UUID `json:"user2_id"`
	IsActive  bool      `json:"is_active"`
	CreatedAt time.Time `json:"created_at"`
}

// RoomRepository описывает методы для работы с чат-комнатами в базе данных.
type RoomRepository interface {
	// CreateRoom создаёт чат-комнату между двумя пользователями.
	CreateRoom(ctx context.Context, user1ID, user2ID uuid.UUID) (*Room, error)
	// GetByUser возвращает все чат-комнаты, в которых участвует указанный пользователь.
	GetByUser(ctx context.Context, userID uuid.UUID) ([]Room, error)
	// GetRoomByID возвращает чат-комнату по её уникальному идентификатору.
	GetRoomByID(ctx context.Context, roomID int64) (*Room, error)
	// GetRoomByUsers ищет существующую комнату между двумя пользователями.
	// Возвращает nil, nil если комната не найдена.
	GetRoomByUsers(ctx context.Context, user1ID, user2ID uuid.UUID) (*Room, error)
}
