package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/joho/godotenv"

	"bloom.local/media/internal/broker/kafka"
	httpDelivery "bloom.local/media/internal/delivery/http"
	"bloom.local/media/internal/repository/postgres"
	"bloom.local/media/internal/service"
	"bloom.local/media/internal/worker"
	"github.com/golang-migrate/migrate/v4"
	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	_ = godotenv.Load()
	brokers := []string{"localhost:9092"}
	ctx := context.Background()
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://testuser:testpass@127.0.0.1:5434/media-db?sslmode=disable"
	}
	port := os.Getenv("PORT")
	if port == "" {
		port = "8093"
	}
	hostUrl := os.Getenv("HOST_URL")
	if hostUrl == "" {
		hostUrl = "http://localhost:" + port
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

	repo := postgres.NewMediaRepo(pool)
	svc := service.NewMediaService(repo)
	handler := httpDelivery.NewMediaHandler(svc, hostUrl)

	mux := http.NewServeMux()
	mux.HandleFunc("/media/upload", handler.Upload)
	mux.HandleFunc("/media/", handler.ServeMedia)

	consumer := kafka.NewConsumer(brokers, "media.message_saved", "media-group", svc)
	go consumer.Start(ctx)
	cronWorker := worker.NewCronWorker(svc)
	go cronWorker.Start(ctx)
	log.Println("Сервис медиа запущен на порту :" + port + "...")
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatal("Ошибка запуска сервера: ", err)
	}
}
