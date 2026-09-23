package postgres

import (
	"context"

	"bloom.local/chat/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
)

// RoomRepo - реализация интерфейса RoomRepository для PostgreSQL.
type RoomRepo struct {
	pool *pgxpool.Pool
}

// NewRoomRepo создает новый экземпляр RoomRepo.
func NewRoomRepo(pool *pgxpool.Pool) *RoomRepo {
	return &RoomRepo{pool: pool}
}

// CreateRoom создает новую чат-комнату для двух пользователей.
func (r *RoomRepo) CreateRoom(ctx context.Context, user1ID, user2ID int64) (*domain.Room, error) {
	var room domain.Room
	err := r.pool.QueryRow(ctx, "INSERT INTO rooms (user1_id, user2_id) VALUES ($1, $2) RETURNING id, user1_id, user2_id, is_active, created_at", user1ID, user2ID).Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &room, nil
}

// GetByUser извлекает все чат-комнаты, в которых участвует указанный пользователь.
func (r *RoomRepo) GetByUser(ctx context.Context, userID int64) ([]domain.Room, error) {
	rows, err := r.pool.Query(ctx, "SELECT * FROM rooms WHERE user1_id=$1 OR user2_id=$1", userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var rooms []domain.Room
	for rows.Next() {
		var room domain.Room
		if err := rows.Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt); err != nil {
			return nil, err
		}
		rooms = append(rooms, room)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return rooms, nil
}
