package main

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"log"
	"net/http"
	"os"
	"strconv"
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

	enableCORS := func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Access-Control-Allow-Origin", "*")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
			if r.Method == "OPTIONS" {
				w.WriteHeader(http.StatusOK)
				return
			}
			next(w, r)
		}
	}
	roomsLogic := func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost {
			roomHandler.CreateRoom(w, r)
		} else if r.Method == http.MethodGet {
			roomHandler.GetUserRooms(w, r)
		} else {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	}
	http.HandleFunc("/api/rooms", enableCORS(httpDelivery.AuthMiddleware(validator, roomsLogic)))
	http.HandleFunc("/api/test/token", enableCORS(func(w http.ResponseWriter, r *http.Request) {
		userIDStr := r.URL.Query().Get("user_id")
		if userIDStr == "" {
			http.Error(w, "user_id is required", http.StatusBadRequest)
			return
		}
		userID, err := strconv.ParseInt(userIDStr, 10, 64)
		if err != nil {
			http.Error(w, "Invalid user ID", http.StatusBadRequest)
			return
		}
		token := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.MapClaims{
			"user_id": userID,
		})
		tokenString, err := token.SignedString(privateKey)
		if err != nil {
			http.Error(w, "Failed to generate token", http.StatusInternalServerError)
			return
		}
		w.Write([]byte(tokenString))
	}))
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		websocket.ServeWS(hub, validator, w, r)
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "tools/test_client.html")
	})

	log.Println("Сервис чата запущен на порту :8091...")
	if err := http.ListenAndServe(":8091", nil); err != nil {

		log.Fatal("Ошибка запуска сервера: ", err)
	}
}
