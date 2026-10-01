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
	EditedAt    *time.Time   `json:"edited_at,omitempty"`
	DeletedAt   *time.Time   `json:"deleted_at,omitempty"`
	ReadAt      *time.Time   `json:"read_at,omitempty"`
}

// MessageSavedEvent — событие, которое мы будем отправлять в брокер (Kafka)
type MessageSavedEvent struct {
	MessageID      int64    `json:"message_id"`
	RoomID         int64    `json:"room_id"`
	AttachmentURLs []string `json:"attachment_urls"`
}

type MessagePage struct {
	Items      []*Message `json:"items"`
	NextCursor *string    `json:"nextCursor"`
}

// MessageRepository описывает методы для сохранения и получения сообщений.
type MessageRepository interface {
	Save(ctx context.Context, msg *Message) error
	GetByRoomID(ctx context.Context, roomID int64) ([]*Message, error)
	MarkAsRead(ctx context.Context, messageID int64, readAt time.Time, userID uuid.UUID) error
	UpdateContent(ctx context.Context, messageID int64, newContent string, editedAt time.Time, userID uuid.UUID) error
	SoftDelete(ctx context.Context, messageID int64, deletedAt time.Time, userID uuid.UUID) error
	SearchByUser(ctx context.Context, userID uuid.UUID, query string, cursor string, limit int) (*MessagePage, error)
}

type MessageEventProducer interface {
	PublishMessageSaved(ctx context.Context, event *MessageSavedEvent) error
}
