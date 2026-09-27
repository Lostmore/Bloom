package postgres

import (
	"context"
	"encoding/json"

	"bloom.local/chat/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
)

// MessageRepo - реализация интерфейса MessageRepository для PostgreSQL.
type MessageRepo struct {
	pool *pgxpool.Pool
}

// NewMessageRepo создает новый экземпляр MessageRepo.
func NewMessageRepo(pool *pgxpool.Pool) *MessageRepo {
	return &MessageRepo{pool}
}

// Save сохраняет новое сообщение в базе данных.
func (r *MessageRepo) Save(ctx context.Context, msg *domain.Message) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	err = tx.QueryRow(ctx, "INSERT INTO messages (room_id,sender_id,content) VALUES($1,$2,$3) RETURNING id,created_at", msg.RoomID, msg.SenderID, msg.Content).Scan(&msg.ID, &msg.CreatedAt)
	if err != nil {
		return err
	}
	for id, attachment := range msg.Attachments {
		err = tx.QueryRow(ctx, "INSERT INTO message_attachments (message_id,url,media_type) VALUES ($1,$2,$3) RETURNING id,created_at", msg.ID, attachment.URL, attachment.MediaType).Scan(&msg.Attachments[id].ID, &msg.Attachments[id].CreatedAt)
		if err != nil {
			return err
		}
	}
	if len(msg.Attachments) > 0 {
		var attachment []string
		for _, attach := range msg.Attachments {
			attachment = append(attachment, attach.URL)
		}
		messageSavedEvent := &domain.MessageSavedEvent{
			MessageID:      msg.ID,
			RoomID:         msg.RoomID,
			AttachmentURLs: attachment,
		}
		event, err := json.Marshal(messageSavedEvent)
		if err != nil {
			return err
		}
		_, err = tx.Exec(ctx, "INSERT INTO outbox_events (topic,payload) VALUES($1,$2)", "media.message_saved", event)
		if err != nil {
			return err
		}
	}
	if err = tx.Commit(ctx); err != nil {
		return err
	}
	return nil
}

// GetByRoomID извлекает все сообщения для указанной комнаты в хронологическом порядке.
func (r *MessageRepo) GetByRoomID(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	var attachmentsJSON []byte
	rows, err := r.pool.Query(ctx, `SELECT 
    m.id, m.room_id, m.sender_id, m.content, m.created_at,
    COALESCE(
        json_agg(
            json_build_object(
                'id', ma.id,
                'message_id', ma.message_id,
                'url', ma.url, 
                'media_type', ma.media_type,
                'created_at', ma.created_at
            )
        ) FILTER (WHERE ma.id IS NOT NULL), 
        '[]'
    ) AS attachments
FROM messages m
LEFT JOIN message_attachments ma ON ma.message_id = m.id 
WHERE m.room_id = $1
GROUP BY m.id
ORDER BY m.created_at ASC`, roomID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	messages := make([]*domain.Message, 0)
	for rows.Next() {
		var m domain.Message
		err := rows.Scan(&m.ID, &m.RoomID, &m.SenderID, &m.Content, &m.CreatedAt, &attachmentsJSON)
		if err != nil {
			return nil, err
		}
		if attachmentsJSON != nil {
			err = json.Unmarshal(attachmentsJSON, &m.Attachments)
			if err != nil {
				return nil, err
			}
		}
		messages = append(messages, &m)
	}
	if err = rows.Err(); err != nil {
		return nil, err
	}
	return messages, nil
}
