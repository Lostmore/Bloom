package service

import (
	"context"
	"os"
	"path/filepath"
	"time"

	"bloom.local/media/internal/domain"
	"github.com/google/uuid"
)

type MediaService struct {
	mediaRepo domain.MediaRepository
}

func NewMediaService(mediaRepo domain.MediaRepository) *MediaService {
	return &MediaService{
		mediaRepo: mediaRepo,
	}
}

// Upload принимает файл, сохраняет его на диск и пишет в БД
func (s *MediaService) Upload(ctx context.Context, fileContent []byte, originalName, mimeType string) (*domain.MediaFile, error) {
	id := uuid.New()
	extension := filepath.Ext(originalName)
	fileName := id.String() + extension
	dirName := "uploads"
	filePath := filepath.Join(dirName, fileName)
	os.MkdirAll(dirName, 0755)
	err := os.WriteFile(filePath, fileContent, 0644)
	if err != nil {
		return nil, err
	}
	mediaFile := &domain.MediaFile{
		ID:           id.String(),
		OriginalName: originalName,
		MimeType:     mimeType,
		Size:         int64(len(fileContent)),
	}
	err = s.mediaRepo.Save(ctx, mediaFile)
	if err != nil {
		os.Remove(filePath)
		return nil, err
	}
	return mediaFile, nil
}

// GetMedia ищет файл в БД и возвращает его метаданные + полный путь к файлу на диске
func (s *MediaService) GetMedia(ctx context.Context, id string) (*domain.MediaFile, string, error) {
	mediaFile, err := s.mediaRepo.GetByID(ctx, id)
	if err != nil {
		return nil, "", err
	}
	extension := filepath.Ext(mediaFile.OriginalName)
	filePath := filepath.Join("uploads", id+extension)
	return mediaFile, filePath, nil
}

func (s *MediaService) MarkAsPermanent(ctx context.Context, ids []string) error {
	return s.mediaRepo.MarkAsPermanent(ctx, ids)
}

// DeleteOldTemporary удаляет записи из БД и физические файлы с диска
func (s *MediaService) DeleteOldTemporary(ctx context.Context, olderThan time.Time) error {
	files, err := s.mediaRepo.DeleteOldTemporary(ctx, olderThan)
	if err != nil {
		return err
	}
	for _, f := range files {
		extension := filepath.Ext(f.OriginalName)
		path := filepath.Join("uploads", f.ID+extension)
		os.Remove(path)
	}
	return nil
}
