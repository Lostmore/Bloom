package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	httpDelivery "bloom.local/chat/internal/delivery/http"
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
	msgRepo := postgres.NewMessageRepo(pool)
	roomRepo := postgres.NewRoomRepo(pool)
	svc := service.NewMessageService(msgRepo, roomRepo)
	hub := websocket.NewHub(svc)
	go hub.Run()

	roomHandler := httpDelivery.NewRoomHandler(svc)

	enableCORS := func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Access-Control-Allow-Origin", "*")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			if r.Method == "OPTIONS" {
				w.WriteHeader(http.StatusOK)
				return
			}
			next(w, r)
		}
	}

	http.HandleFunc("/api/rooms", enableCORS(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost {
			roomHandler.CreateRoom(w, r)
		} else if r.Method == http.MethodGet {
			roomHandler.GetUserRooms(w, r)
		} else {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	}))

	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		websocket.ServeWS(hub, w, r)
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "tools/test_client.html")
	})

	log.Println("Сервис чата запущен на порту :8091...")
	if err := http.ListenAndServe(":8091", nil); err != nil {

		log.Fatal("Ошибка запуска сервера: ", err)
	}
}
