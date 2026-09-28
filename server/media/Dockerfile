FROM golang:alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
# Собираем бинарник из папки cmd
RUN go generate ./docs
RUN go build -o media-server ./cmd/main.go 
FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/media-server .
COPY --from=builder /app/migrations ./migrations
# Открываем порт
EXPOSE 8093
CMD ["./media-server"]
