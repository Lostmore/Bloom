FROM golang:alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
# Собираем бинарник из папки cmd
RUN go build -o chat-server ./cmd/main.go 
FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/chat-server .
COPY --from=builder /app/migrations ./migrations
COPY --from=builder /app/tools ./tools
COPY --from=builder /app/docs ./docs
# Открываем порт
EXPOSE 8091
CMD ["./chat-server"]
