package domain

import (
	"context"
	"time"
)

// Room представляет собой сущность чат-комнаты между двумя пользователями.
type Room struct {
	ID        int64     `json:"id"`
	User1ID   int64     `json:"user1_id"`
	User2ID   int64     `json:"user2_id"`
	IsActive  bool      `json:"is_active"`
	CreatedAt time.Time `json:"created_at"`
}

// RoomRepository описывает методы для работы с чат-комнатами в базе данных.
type RoomRepository interface {
	CreateRoom(ctx context.Context, user1ID, user2ID int64) (*Room, error)
	GetByUser(ctx context.Context, userID int64) ([]Room, error)
}
