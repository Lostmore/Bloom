package postgres

import (
	"context"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

func setupDB(t *testing.T) (*pgxpool.Pool, func()) {
	ctx := context.Background()
	pgContainer, err := postgres.RunContainer(ctx,
		testcontainers.WithImage("postgres:15-alpine"),
		postgres.WithDatabase("chatdb"),
		postgres.WithUsername("testuser"),
		postgres.WithPassword("testpass"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").WithOccurrence(2).WithStartupTimeout(10*time.Second),
		),
	)
	require.NoError(t, err)
	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	require.NoError(t, err)
	pool, err := pgxpool.New(ctx, connStr)
	require.NoError(t, err)
	_, err = pool.Exec(ctx, `
			CREATE TABLE IF NOT EXISTS messages (
				id SERIAL PRIMARY KEY,
				room_id INTEGER NOT NULL,
				sender_id INTEGER NOT NULL,
				content TEXT NOT NULL,
				created_at TIMESTAMP DEFAULT NOW()
			);
		`)
	teardown := func() {
		pool.Close()
		pgContainer.Terminate(ctx)
	}
	return pool, teardown
}
func TestMessageRepo_SaveAndGet(t *testing.T) {
	pool, teardown := setupDB(t)
	defer teardown()
	repo := NewMessageRepo(pool)
	ctx := context.Background()
	msg := &domain.Message{
		RoomID:   1,
		SenderID: 1,
		Content:  "Hello",
	}
	err := repo.Save(ctx, msg)
	require.NoError(t, err)
	msg2 := &domain.Message{
		RoomID:   1,
		SenderID: 2,
		Content:  "Hi",
	}
	err = repo.Save(ctx, msg2)
	require.NoError(t, err)
	messages, err := repo.GetByRoomID(ctx, 1)
	require.NoError(t, err)
	require.Len(t, messages, 2)
	require.Equal(t, msg.Content, messages[0].Content)
	require.Equal(t, msg2.Content, messages[1].Content)
}
