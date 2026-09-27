package worker

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"bloom.local/chat/internal/domain"
	"github.com/jackc/pgx/v5/pgxpool"
)

type OutboxRelay struct {
	pool     *pgxpool.Pool
	producer domain.MessageEventProducer
}

func NewOutboxRelay(pool *pgxpool.Pool, producer domain.MessageEventProducer) *OutboxRelay {
	return &OutboxRelay{pool: pool, producer: producer}
}

// Run запускает бесконечный цикл обработки.
// Мы будем вызывать его в отдельной горутине при старте приложения (в main.go).
func (r *OutboxRelay) Run(ctx context.Context) {
	ticker := time.NewTicker(2 * time.Second) // Проверяем базу каждые 2 секунды
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			log.Println("OutboxRelay stopping...")
			return
		case <-ticker.C:
			r.processEvents(ctx, "media.message_saved")
		}
	}
}

// processEvents ищет неотправленные события и шлет их
func (r *OutboxRelay) processEvents(ctx context.Context, topic string) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return
	}
	defer tx.Rollback(ctx)

	rows, err := tx.Query(ctx, "SELECT id, payload FROM outbox_events WHERE processed = false AND topic = $1 LIMIT 50 FOR UPDATE SKIP LOCKED", topic)
	if err != nil {
		log.Printf("Error querying outbox_events: %v", err)
		return
	}
	defer rows.Close()

	for rows.Next() {
		var id int64
		var payload []byte
		err := rows.Scan(&id, &payload)
		if err != nil {
			continue
		}
		var message domain.MessageSavedEvent
		if err := json.Unmarshal(payload, &message); err != nil {
			continue
		}
		if err := r.producer.PublishMessageSaved(ctx, &message); err != nil {
			continue
		}

		tx.Exec(ctx, "UPDATE outbox_events SET processed = true WHERE id = $1", id)
	}
	tx.Commit(ctx)
}
