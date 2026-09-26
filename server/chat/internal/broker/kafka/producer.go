package kafka

import (
	"context"
	"encoding/json"

	"bloom.local/chat/internal/domain"
	"github.com/segmentio/kafka-go"
)

type Producer struct {
	writer *kafka.Writer
}

func NewProducer(brokers []string, topic string) *Producer {
	w := &kafka.Writer{
		Addr:                   kafka.TCP(brokers...),
		Topic:                  topic,
		AllowAutoTopicCreation: true,
	}
	return &Producer{writer: w}
}

func (p *Producer) Close() error {
	return p.writer.Close()

}

func (p *Producer) PublishMessageSaved(ctx context.Context, event *domain.MessageSavedEvent) error {
	bytes, err := json.Marshal(event)
	if err != nil {
		return err
	}
	return p.writer.WriteMessages(ctx, kafka.Message{Value: bytes})
}
