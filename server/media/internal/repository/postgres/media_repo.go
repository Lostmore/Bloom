package postgres

import (
	"context"
	"time"

	"bloom.local/media/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
)

type MediaRepo struct {
	pool *pgxpool.Pool
}

func NewMediaRepo(pool *pgxpool.Pool) *MediaRepo {
	return &MediaRepo{pool: pool}
}

// Save сохраняет метаданные файла в БД
func (r *MediaRepo) Save(ctx context.Context, file *domain.MediaFile) error {
	_, err := r.pool.Exec(ctx, "INSERT INTO media_files (id, original_name, mime_type, size) VALUES($1, $2, $3, $4)", file.ID, file.OriginalName, file.MimeType, file.Size)
	if err != nil {
		return err
	}
	return nil
}

// GetByID достает файл по его UUID
func (r *MediaRepo) GetByID(ctx context.Context, id string) (*domain.MediaFile, error) {
	var file domain.MediaFile
	err := r.pool.QueryRow(ctx, "SELECT id, original_name, mime_type, size, is_temporary, created_at FROM media_files WHERE id=$1", id).Scan(&file.ID, &file.OriginalName, &file.MimeType, &file.Size, &file.IsTemporary, &file.CreatedAt)
	if err != nil {
		return nil, err
	}
	return &file, nil
}

func (r *MediaRepo) MarkAsPermanent(ctx context.Context, ids []string) error {
	_, err := r.pool.Exec(ctx, "UPDATE media_files SET is_temporary=false WHERE id=ANY($1)", ids)
	if err != nil {
		return err
	}
	return nil
}

func (r *MediaRepo) DeleteOldTemporary(ctx context.Context, olderThan time.Time) ([]*domain.MediaFile, error) {
	var files []*domain.MediaFile
	rows, err := r.pool.Query(ctx, `DELETE FROM media_files 
WHERE is_temporary = true AND created_at < $1 
RETURNING id, original_name, mime_type, size, is_temporary, created_at
`, olderThan)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var file domain.MediaFile
		if err := rows.Scan(&file.ID, &file.OriginalName, &file.MimeType, &file.Size, &file.IsTemporary, &file.CreatedAt); err != nil {
			return nil, err
		}
		files = append(files, &file)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return files, nil
}
