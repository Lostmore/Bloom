package postgres

import (
	"context"
	"log"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

func setupTestDB(t *testing.T) (*pgxpool.Pool, func()) {
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
	m, err := migrate.New("file://../../../migrations", connStr)
	if err != nil {
		log.Fatal("Ошибка инициализации миграций:", err)
	}

	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		log.Fatal("Ошибка применения миграций:", err)
	}
	log.Println("Миграции успешно применены!")

	teardown := func() {
		pool.Close()
		pgContainer.Terminate(ctx)
	}
	return pool, teardown
}

func TestMessageRepo_SaveAndGet(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()

	repo := NewMessageRepo(pool)
	ctx := context.Background()

	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES (1, 10, 20)")
	require.NoError(t, err)

	msg := &domain.Message{
		RoomID:   1,
		SenderID: 10,
		Content:  "Hello",
	}
	err = repo.Save(ctx, msg)
	require.NoError(t, err)

	msg2 := &domain.Message{
		RoomID:   1,
		SenderID: 20,
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
func TestMessageRepo_SaveWithAttachments(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()
	repo := NewMessageRepo(pool)
	ctx := context.Background()
	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES (1, 10, 20)")
	require.NoError(t, err)
	msg := &domain.Message{
		RoomID:   1,
		SenderID: 10,
		Content:  "Look at these files!",
		Attachments: []domain.Attachment{
			{URL: "http://example.com/1.jpg", MediaType: "image/jpeg"},
			{URL: "http://example.com/2.mp4", MediaType: "video/mp4"},
		},
	}
	err = repo.Save(ctx, msg)
	require.NoError(t, err)
	require.NotZero(t, msg.ID)
	testMsg, err := repo.GetByRoomID(ctx, msg.RoomID)
	require.NoError(t, err)
	require.Len(t, testMsg, 1)
	require.Equal(t, msg.Content, testMsg[0].Content)
	require.NoError(t, err)
	require.Equal(t, "http://example.com/1.jpg", testMsg[0].Attachments[0].URL)
	require.Equal(t, "video/mp4", testMsg[0].Attachments[1].MediaType)
	var count int
	err = pool.QueryRow(ctx, "SELECT count(*) FROM outbox_events").Scan(&count)
	require.NoError(t, err)
	require.Equal(t, 1, count)
}
