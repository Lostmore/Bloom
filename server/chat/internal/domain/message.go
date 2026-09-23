package domain

import (
	"context"
	"time"
)

// Message представляет собой единичное текстовое сообщение в чат-комнате.
type Message struct {
	ID        int64     `json:"id"`
	RoomID    int64     `json:"room_id"`
	SenderID  int64     `json:"sender_id"`
	Content   string    `json:"content"`
	CreatedAt time.Time `json:"created_at"`
}

// MessageRepository описывает методы для сохранения и получения сообщений.
type MessageRepository interface {
	Save(ctx context.Context, msg *Message) error
	GetByRoomID(ctx context.Context, roomID int64) ([]*Message, error)
}
