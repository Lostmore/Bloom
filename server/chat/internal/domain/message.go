package domain

import (
	"context"
	"time"
)

type Message struct {
	ID        int64     `json:"id"`
	RoomID    int64     `json:"room_id"`
	SenderID  int64     `json:"sender_id"`
	Content   string    `json:"content"`
	CreatedAt time.Time `json:"created_at"`
}
type MessageRepository interface {
	Save(ctx context.Context, msg *Message) error
	GetByRoomID(ctx context.Context, roomID int64) ([]*Message, error)
}
