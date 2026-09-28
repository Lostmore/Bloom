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
		log.Printf("Outbox: Error starting tx: %v", err)
		return
	}
	defer tx.Rollback(ctx)

	rows, err := tx.Query(ctx, "SELECT id, payload FROM outbox_events WHERE processed = false AND topic = $1 LIMIT 50 FOR UPDATE SKIP LOCKED", topic)
	if err != nil {
		log.Printf("Outbox: Error querying outbox_events: %v", err)
		return
	}
	defer rows.Close()

	var count int
	var processedIDs []int64

	for rows.Next() {
		var id int64
		var payload []byte
		err := rows.Scan(&id, &payload)
		if err != nil {
			log.Printf("Outbox scan error: %v", err)
			continue
		}
		var message domain.MessageSavedEvent
		if err := json.Unmarshal(payload, &message); err != nil {
			log.Printf("Outbox unmarshal error for ID %d: %v", id, err)
			continue
		}
		if err := r.producer.PublishMessageSaved(ctx, &message); err != nil {
			log.Printf("Outbox publish error for ID %d: %v", id, err)
			continue
		}

		processedIDs = append(processedIDs, id)
	}
	rows.Close()

	for _, id := range processedIDs {
		_, err = tx.Exec(ctx, "UPDATE outbox_events SET processed = true WHERE id = $1", id)
		if err != nil {
			log.Printf("Outbox: Error updating outbox_events for ID %d: %v", id, err)
			continue
		}
		count++
	}

	if count > 0 {
		log.Printf("Outbox: successfully processed %d events", count)
	}
	if err := tx.Commit(ctx); err != nil {
		log.Printf("Outbox: Error committing tx: %v", err)
	}
}
