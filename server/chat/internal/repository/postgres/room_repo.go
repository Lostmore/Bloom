package postgres

import (
	"context"

	"bloom.local/chat/internal/domain"
	"github.com/google/uuid"
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
func (r *RoomRepo) CreateRoom(ctx context.Context, user1ID, user2ID uuid.UUID) (*domain.Room, error) {
	var room domain.Room
	err := r.pool.QueryRow(ctx, "INSERT INTO rooms (user1_id, user2_id) VALUES ($1, $2) RETURNING id, user1_id, user2_id, is_active, created_at", user1ID, user2ID).Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &room, nil
}

// GetByUser извлекает все чат-комнаты, в которых участвует указанный пользователь.
func (r *RoomRepo) GetByUser(ctx context.Context, userID uuid.UUID) ([]domain.Room, error) {
	query := `
		SELECT 
			r.id, r.user1_id, r.user2_id, r.is_active, r.created_at,
			m.content, m.created_at
		FROM rooms r
		LEFT JOIN (
			SELECT room_id, MAX(created_at) AS max_time 
			FROM messages 
			GROUP BY room_id
		) fm ON r.id = fm.room_id
		LEFT JOIN messages m ON m.room_id = r.id AND m.created_at = fm.max_time
		WHERE r.user1_id = $1 OR r.user2_id = $1
		ORDER BY COALESCE(m.created_at, r.created_at) DESC
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var rooms []domain.Room
	for rows.Next() {
		var room domain.Room
		if err := rows.Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt, &room.LastMessage, &room.LastMessageAt); err != nil {
			return nil, err
		}
		rooms = append(rooms, room)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return rooms, nil
}

// GetRoomByID возвращает чат-комнату по её идентификатору.
// Используется для проверки существования комнаты и проверки прав доступа.
func (r *RoomRepo) GetRoomByID(ctx context.Context, roomID int64) (*domain.Room, error) {
	var room domain.Room
	err := r.pool.QueryRow(ctx, "SELECT * FROM rooms WHERE id=$1", roomID).Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &room, nil
}

// GetRoomByUsers ищет чат-комнату между двумя пользователями (в любом порядке).
// Возвращает nil, nil если комната не найдена, чтобы избежать дублирования чатов.
func (r *RoomRepo) GetRoomByUsers(ctx context.Context, user1ID, user2ID uuid.UUID) (*domain.Room, error) {
	var room domain.Room
	err := r.pool.QueryRow(ctx, "SELECT * FROM rooms WHERE (user1_id= $1 AND user2_id=$2) OR (user1_id=$2 AND user2_id=$1)", user1ID, user2ID).Scan(&room.ID, &room.User1ID, &room.User2ID, &room.IsActive, &room.CreatedAt)
	if err != nil {
		if err.Error() == "no rows in result set" {
			return nil, nil
		}
		return nil, err
	}
	return &room, nil
}
