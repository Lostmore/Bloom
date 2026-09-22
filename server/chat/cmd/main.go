package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	"bloom.local/chat/internal/delivery/websocket"
	"bloom.local/chat/internal/repository/postgres"
	"bloom.local/chat/internal/service"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	ctx := context.Background()
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://testuser:testpass@localhost:5432/chatdb?sslmode=disable"
	}
	var pool *pgxpool.Pool
	var err error
	for i := 0; i < 5; i++ {
		pool, err = pgxpool.New(ctx, dsn)
		if err == nil {
			err = pool.Ping(ctx)
			if err == nil {
				break
			}
		}
		log.Println("Ожидание старта PostgreSQL...")
		time.Sleep(2 * time.Second)
	}
	if err != nil {
		log.Fatal("Не удалось подключиться к БД после 5 попыток:", err)
	}
	defer pool.Close()
	m, err := migrate.New("file://migrations", dsn)
	if err != nil {
		log.Fatal("Ошибка инициализации миграций:", err)
	}
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		log.Fatal("Ошибка применения миграций:", err)
	}
	log.Println("Миграции успешно применены!")
	repo := postgres.NewMessageRepo(pool)
	svc := service.NewMessageService(repo)
	hub := websocket.NewHub(svc)
	go hub.Run()
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		websocket.ServeWS(hub, w, r)
	})
	log.Println("Сервис чата запущен на порту :8091...")
	if err := http.ListenAndServe(":8091", nil); err != nil {
		log.Fatal("Ошибка запуска сервера: ", err)
	}
}
