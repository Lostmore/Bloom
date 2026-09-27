package worker

import (
	"context"
	"log"
	"time"

	"bloom.local/media/internal/service"
)

type CronWorker struct {
	svc *service.MediaService
}

func NewCronWorker(svc *service.MediaService) *CronWorker {
	return &CronWorker{svc: svc}
}
func (w *CronWorker) Start(ctx context.Context) {
	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()
	log.Println("Запуск Cron Worker (очистка временных файлов)...")
	for {
		select {
		case <-ctx.Done():
			log.Println("Cron Worker остановлен.")
			return
		case <-ticker.C:
			olderThan := time.Now().Add(-24 * time.Hour)
			err := w.svc.DeleteOldTemporary(ctx, olderThan)
			if err != nil {
				log.Println("Ошибка очистки старых файлов:", err)
			} else {
				log.Println("Очистка старых файлов завершена")
			}
		}
	}
}
