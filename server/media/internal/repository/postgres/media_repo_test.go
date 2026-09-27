package postgres_test

import (
	"context"
	"testing"
	"time"

	"bloom.local/media/internal/domain"
	"bloom.local/media/internal/repository/postgres"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	postgresModules "github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

func setupTestDB(t *testing.T) (*pgxpool.Pool, func()) {
	ctx := context.Background()

	// 1. Поднимаем контейнер
	postgresContainer, err := postgresModules.RunContainer(ctx,
		testcontainers.WithImage("postgres:15-alpine"),
		postgresModules.WithDatabase("testdb"),
		postgresModules.WithUsername("testuser"),
		postgresModules.WithPassword("testpass"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).WithStartupTimeout(5*time.Second)),
	)
	require.NoError(t, err)

	// 2. Получаем строку подключения
	connStr, err := postgresContainer.ConnectionString(ctx, "sslmode=disable")
	require.NoError(t, err)

	// 3. Подключаемся
	pool, err := pgxpool.New(ctx, connStr)
	require.NoError(t, err)

	// 4. Создаем таблицу руками
	_, err = pool.Exec(ctx, `
		CREATE TABLE media_files (
			id UUID PRIMARY KEY,
			original_name VARCHAR(255) NOT NULL,
			mime_type VARCHAR(100) NOT NULL,
			size BIGINT NOT NULL,
			is_temporary BOOLEAN DEFAULT true,
			created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
		)
	`)
	require.NoError(t, err)

	cleanup := func() {
		pool.Close()
		postgresContainer.Terminate(ctx)
	}

	return pool, cleanup
}

func TestMediaRepo_SaveAndGet(t *testing.T) {
	pool, cleanup := setupTestDB(t)
	defer cleanup()

	repo := postgres.NewMediaRepo(pool)
	ctx := context.Background()

	file := &domain.MediaFile{
		ID:           "123e4567-e89b-12d3-a456-426614174000",
		OriginalName: "test.png",
		MimeType:     "image/png",
		Size:         1024,
	}

	err := repo.Save(ctx, file)
	require.NoError(t, err)

	savedFile, err := repo.GetByID(ctx, file.ID)
	require.NoError(t, err)
	assert.Equal(t, file.ID, savedFile.ID)
	assert.Equal(t, file.OriginalName, savedFile.OriginalName)
	assert.Equal(t, file.MimeType, savedFile.MimeType)
	assert.Equal(t, file.Size, savedFile.Size)
	assert.True(t, savedFile.IsTemporary) // По умолчанию должен быть true
}

func TestMediaRepo_MarkAsPermanent(t *testing.T) {
	pool, cleanup := setupTestDB(t)
	defer cleanup()

	repo := postgres.NewMediaRepo(pool)
	ctx := context.Background()

	file1 := &domain.MediaFile{
		ID:           "11111111-1111-1111-1111-111111111111",
		OriginalName: "test1.png",
		MimeType:     "image/png",
		Size:         100,
	}
	file2 := &domain.MediaFile{
		ID:           "22222222-2222-2222-2222-222222222222",
		OriginalName: "test2.png",
		MimeType:     "image/png",
		Size:         200,
	}
	
	require.NoError(t, repo.Save(ctx, file1))
	require.NoError(t, repo.Save(ctx, file2))

	// Mark file1 as permanent
	err := repo.MarkAsPermanent(ctx, []string{file1.ID})
	require.NoError(t, err)

	savedFile1, _ := repo.GetByID(ctx, file1.ID)
	savedFile2, _ := repo.GetByID(ctx, file2.ID)

	assert.False(t, savedFile1.IsTemporary, "file1 should be permanent")
	assert.True(t, savedFile2.IsTemporary, "file2 should still be temporary")
}

func TestMediaRepo_DeleteOldTemporary(t *testing.T) {
	pool, cleanup := setupTestDB(t)
	defer cleanup()

	repo := postgres.NewMediaRepo(pool)
	ctx := context.Background()

	// Вставляем файлы с разным created_at
	_, err := pool.Exec(ctx, `
		INSERT INTO media_files (id, original_name, mime_type, size, is_temporary, created_at)
		VALUES 
			('11111111-1111-1111-1111-111111111111', 'old_temp.png', 'image/png', 100, true, NOW() - INTERVAL '2 days'),
			('22222222-2222-2222-2222-222222222222', 'old_perm.png', 'image/png', 100, false, NOW() - INTERVAL '2 days'),
			('33333333-3333-3333-3333-333333333333', 'new_temp.png', 'image/png', 100, true, NOW())
	`)
	require.NoError(t, err)

	deletedFiles, err := repo.DeleteOldTemporary(ctx, time.Now().Add(-24*time.Hour))
	require.NoError(t, err)

	// Должен удалиться только old_temp.png (он старше 24ч и temporary=true)
	assert.Len(t, deletedFiles, 1)
	assert.Equal(t, "11111111-1111-1111-1111-111111111111", deletedFiles[0].ID)

	// Проверяем, что в БД осталось 2 файла
	var count int
	err = pool.QueryRow(ctx, "SELECT COUNT(*) FROM media_files").Scan(&count)
	require.NoError(t, err)
	assert.Equal(t, 2, count)
}
