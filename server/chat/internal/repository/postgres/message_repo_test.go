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
	"github.com/google/uuid"
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
	if err != nil {
		t.Skipf("Failed to start postgres container (is docker running?): %v", err)
	}

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

	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES (1, $1, $2)",
		uuid.MustParse("00000000-0000-0000-0000-000000000010"),
		uuid.MustParse("00000000-0000-0000-0000-000000000020"),
	)
	require.NoError(t, err)

	msg := &domain.Message{
		RoomID:   1,
		SenderID: uuid.MustParse("00000000-0000-0000-0000-000000000010"),
		Content:  "Hello",
	}
	err = repo.Save(ctx, msg)
	require.NoError(t, err)

	msg2 := &domain.Message{
		RoomID:   1,
		SenderID: uuid.MustParse("00000000-0000-0000-0000-000000000020"),
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
	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES (1, $1, $2)",
		uuid.MustParse("00000000-0000-0000-0000-000000000010"),
		uuid.MustParse("00000000-0000-0000-0000-000000000020"),
	)
	require.NoError(t, err)
	msg := &domain.Message{
		RoomID:   1,
		SenderID: uuid.MustParse("00000000-0000-0000-0000-000000000010"),
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

func TestMessageRepo_Statuses(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()
	repo := NewMessageRepo(pool)
	ctx := context.Background()

	u1 := uuid.MustParse("00000000-0000-0000-0000-000000000010")
	u2 := uuid.MustParse("00000000-0000-0000-0000-000000000020")

	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES (1, $1, $2)", u1, u2)
	require.NoError(t, err)

	msg := &domain.Message{
		RoomID:   1,
		SenderID: u1,
		Content:  "Initial",
	}
	err = repo.Save(ctx, msg)
	require.NoError(t, err)

	// 1. MarkAsRead by recipient
	err = repo.MarkAsRead(ctx, msg.ID, time.Now(), u2)
	require.NoError(t, err)

	// MarkAsRead by sender should fail
	err = repo.MarkAsRead(ctx, msg.ID, time.Now(), u1)
	require.Error(t, err)

	// 2. UpdateContent by sender
	err = repo.UpdateContent(ctx, msg.ID, "Edited", time.Now(), u1)
	require.NoError(t, err)

	// UpdateContent by recipient should fail
	err = repo.UpdateContent(ctx, msg.ID, "Hack", time.Now(), u2)
	require.Error(t, err)

	// 3. SoftDelete by sender
	err = repo.SoftDelete(ctx, msg.ID, time.Now(), u1)
	require.NoError(t, err)

	// Verify DB state
	msgs, err := repo.GetByRoomID(ctx, 1)
	require.NoError(t, err)
	require.Len(t, msgs, 1)
	require.NotNil(t, msgs[0].ReadAt)
	require.NotNil(t, msgs[0].EditedAt)
	require.Equal(t, "Edited", msgs[0].Content)
	require.NotNil(t, msgs[0].DeletedAt)
}

func TestMessageRepo_GetAround(t *testing.T) {
	ctx := context.Background()
	pool, terminate := setupTestDB(t)
	defer terminate()

	repo := NewMessageRepo(pool)

	u1 := uuid.New()
	u2 := uuid.New()

	_, err := pool.Exec(ctx, "INSERT INTO rooms (id, user1_id, user2_id) VALUES ($1, $2, $3)", 1, u1, u2)
	require.NoError(t, err)

	for i := 1; i <= 10; i++ {
		msg := &domain.Message{
			RoomID:   1,
			SenderID: u1,
			Content:  "msg",
		}
		err = repo.Save(ctx, msg)
		require.NoError(t, err)
	}

	page, err := repo.GetAround(ctx, 1, 5, 4)
	require.NoError(t, err)
	require.NotNil(t, page)

	// we want around 5 limit 4 (2 before + 5 + 2 after) -> so roughly 3-4-5-6 or 4-5-6-7
	// with my query: limit/2+1 before (incl 5) and limit/2 after
	// 4/2+1 = 3 before, 4/2 = 2 after -> 3,4,5 and 6,7 -> 5 items total
	require.Len(t, page.Items, 5)
	require.Equal(t, int64(3), page.Items[0].ID)
	require.Equal(t, int64(4), page.Items[1].ID)
	require.Equal(t, int64(5), page.Items[2].ID)
	require.Equal(t, int64(6), page.Items[3].ID)
	require.Equal(t, int64(7), page.Items[4].ID)
}
