package domain

import (
	"context"
	"time"
)

type MediaFile struct {
	ID           string    `json:"id"`
	OriginalName string    `json:"original_name"`
	MimeType     string    `json:"mime_type"`
	Size         int64     `json:"size"`
	IsTemporary  bool      `json:"is_temporary"`
	CreatedAt    time.Time `json:"created_at"`
}

// MediaRepository описывает методы работы с базой данных
type MediaRepository interface {
	Save(ctx context.Context, file *MediaFile) error
	GetByID(ctx context.Context, id string) (*MediaFile, error)
	MarkAsPermanent(ctx context.Context, ids []string) error
	DeleteOldTemporary(ctx context.Context, olderThan time.Time) ([]*MediaFile, error)
}
