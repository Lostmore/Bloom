package main

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"log"
	"net/http"
	"os"
	"time"

	"bloom.local/chat/internal/broker/kafka"
	httpDelivery "bloom.local/chat/internal/delivery/http"
	"bloom.local/chat/internal/delivery/websocket"
	"bloom.local/chat/internal/pkg/auth"
	"bloom.local/chat/internal/repository/postgres"
	"bloom.local/chat/internal/service"
	"bloom.local/chat/internal/worker"
	"github.com/golang-jwt/jwt/v5"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

// @title           Bloom Chat API
// @version         1.0
// @description     Микросервис для работы с чатами в Bloom
// @BasePath        /api/v1
// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
func main() {
	privateKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		log.Fatal("Ошибка генерации RSA ключа:", err)
	}
	validator := auth.NewRSATokenValidator(&privateKey.PublicKey)
	ctx := context.Background()
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://testuser:testpass@localhost:5432/chatdb?sslmode=disable"
	}
	var pool *pgxpool.Pool
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
	kafkaBrokers := []string{os.Getenv("KAFKA_BROKER")}
	if kafkaBrokers[0] == "" {
		kafkaBrokers = []string{"localhost:9092"}
	}
	kafkaProducer := kafka.NewProducer(kafkaBrokers, "media.message_saved")
	defer kafkaProducer.Close()
	outboxRelay := worker.NewOutboxRelay(pool, kafkaProducer)
	go outboxRelay.Run(ctx)
	log.Println("Outbox Relay Worker запущен...")
	svc := service.NewMessageService(msgRepo, roomRepo)
	hub := websocket.NewHub(svc)
	go hub.Run()

	roomHandler := httpDelivery.NewRoomHandler(svc)

	roomsLogic := func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost {
			roomHandler.CreateRoom(w, r)
		} else if r.Method == http.MethodGet {
			roomHandler.GetUserRooms(w, r)
		} else {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	}
	http.HandleFunc("/api/rooms", httpDelivery.AuthMiddleware(validator, roomsLogic))
	http.HandleFunc("/api/test/token", func(w http.ResponseWriter, r *http.Request) {
		userIDStr := r.URL.Query().Get("user_id")
		if userIDStr == "" {
			http.Error(w, "user_id is required", http.StatusBadRequest)
			return
		}
		token := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.MapClaims{
			"user_id": userIDStr,
		})
		tokenString, err := token.SignedString(privateKey)
		if err != nil {
			http.Error(w, "Failed to generate token", http.StatusInternalServerError)
			return
		}
		w.Write([]byte(tokenString))
	})
	http.HandleFunc("/openapi.json", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		http.ServeFile(w, r, "docs/swagger.json")
	})
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		websocket.ServeWS(hub, validator, w, r)
	})
	http.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}
		if err := pool.Ping(r.Context()); err != nil {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		w.WriteHeader(http.StatusOK)
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "tools/test_client.html")
	})

	log.Println("Сервис чата запущен на порту :8091...")
	if err := http.ListenAndServe(":8091", nil); err != nil {

		log.Fatal("Ошибка запуска сервера: ", err)
	}
}
