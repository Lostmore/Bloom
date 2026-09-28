package domain

import (
	"context"
	"time"

	"github.com/google/uuid"
)

// Attachment представляет собой медиа-вложение к сообщению
type Attachment struct {
	ID        int64     `json:"id"`
	MessageID int64     `json:"message_id"`
	URL       string    `json:"url"`
	MediaType string    `json:"media_type"`
	CreatedAt time.Time `json:"created_at"`
}

// Message представляет собой единичное текстовое сообщение в чат-комнате.
type Message struct {
	ID          int64        `json:"id"`
	RoomID      int64        `json:"room_id"`
	SenderID    uuid.UUID    `json:"sender_id"`
	Content     string       `json:"content"`
	Attachments []Attachment `json:"attachments,omitempty"`
	CreatedAt   time.Time    `json:"created_at"`
}

// MessageSavedEvent — событие, которое мы будем отправлять в брокер (Kafka)
type MessageSavedEvent struct {
	MessageID      int64    `json:"message_id"`
	RoomID         int64    `json:"room_id"`
	AttachmentURLs []string `json:"attachment_urls"`
}

// MessageRepository описывает методы для сохранения и получения сообщений.
type MessageRepository interface {
	Save(ctx context.Context, msg *Message) error
	GetByRoomID(ctx context.Context, roomID int64) ([]*Message, error)
}

type MessageEventProducer interface {
	PublishMessageSaved(ctx context.Context, event *MessageSavedEvent) error
}
