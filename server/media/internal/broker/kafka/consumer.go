package kafka

import (
	"context"
	"encoding/json"
	"log"
	"strings"

	"bloom.local/media/internal/service"
	"github.com/segmentio/kafka-go"
)

type MessageSavedEvent struct {
	MessageID      int64    `json:"message_id"`
	RoomID         int64    `json:"room_id"`
	AttachmentURLs []string `json:"attachment_urls"`
}
type Consumer struct {
	reader *kafka.Reader
	svc    *service.MediaService
}

func NewConsumer(brokers []string, topic string, groupID string, svc *service.MediaService) *Consumer {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers: brokers,
		Topic:   topic,
		GroupID: groupID,
	})
	return &Consumer{reader: reader, svc: svc}
}
func (c *Consumer) Start(ctx context.Context) {
	log.Println("Запуск Kafka Consumer для топика media.message_saved...")
	for {
		msg, err := c.reader.ReadMessage(ctx)
		if err != nil {
			log.Println("Ошибка чтения из Kafka (остановка):", err)
			break
		}
		var event MessageSavedEvent
		if err := json.Unmarshal(msg.Value, &event); err != nil {
			log.Println("Ошибка парсинга JSON:", err)
			continue
		}
		var ids []string
		for _, url := range event.AttachmentURLs {
			parts := strings.Split(url, "/")
			if len(parts) > 0 {
				filename := parts[len(parts)-1]
				id := strings.Split(filename, ".")[0]
				ids = append(ids, id)
			}
		}
		err = c.svc.MarkAsPermanent(ctx, ids)
		if err != nil {
			log.Println("Ошибка сохранения медиафайлов:", err)
			continue
		}
		log.Println("Файлы успешно стали постоянными!")
	}
}
