package postgres

import (
	"context"

	"bloom.local/chat/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
)

type MessageRepo struct {
	pool *pgxpool.Pool
}

func NewMessageRepo(pool *pgxpool.Pool) *MessageRepo {
	return &MessageRepo{pool}
}

func (r *MessageRepo) Save(ctx context.Context, msg *domain.Message) error {
	err := r.pool.QueryRow(ctx, "INSERT INTO messages (room_id,sender_id,content) VALUES($1,$2,$3) RETURNING id,created_at", msg.RoomID, msg.SenderID, msg.Content).Scan(&msg.ID, &msg.CreatedAt)
	if err != nil {
		return err
	}
	return nil
}
func (r *MessageRepo) GetByRoomID(ctx context.Context, roomID int64) ([]*domain.Message, error) {
	rows, err := r.pool.Query(ctx, "SELECT id, room_id, sender_id, content, created_at FROM messages WHERE room_id=$1 ORDER BY created_at ASC", roomID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	messages := make([]*domain.Message, 0)
	for rows.Next() {
		var m domain.Message
		err := rows.Scan(&m.ID, &m.RoomID, &m.SenderID, &m.Content, &m.CreatedAt)
		if err != nil {
			return nil, err
		}
		messages = append(messages, &m)
	}
	if err = rows.Err(); err != nil {
		return nil, err
	}
	return messages, nil
}
