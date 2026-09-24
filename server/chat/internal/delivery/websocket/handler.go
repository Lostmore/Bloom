package websocket

import (
	"context"
	"log"
	"net/http"
	"strconv"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/pkg/auth"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

func ServeWS(hub *Hub, validator auth.TokenValidator, w http.ResponseWriter, r *http.Request) {
	tokenStr := r.URL.Query().Get("token")
	if tokenStr == "" {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	userID, err := validator.ValidateToken(tokenStr)
	if err != nil {
		log.Println("Ошибка валидации токена", err)
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	roomIDStr := r.URL.Query().Get("room_id")
	if roomIDStr == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	roomID, err := strconv.ParseInt(roomIDStr, 10, 64)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Ошибка апгрейда", err)
		return
	}
	msgs, err := hub.Service.GetRoomHistory(context.Background(), roomID, userID)
	if err != nil {
		log.Println("Ошибка получения истории сообщений", err)
		conn.Close()
		return
	}
	for _, msg := range msgs {
		err := conn.WriteJSON(msg)
		if err != nil {
			log.Println("Ошибка отправки истории сообщений", err)
			conn.Close()
			return
		}
	}
	client := &Client{
		Hub:    hub,
		Conn:   conn,
		RoomID: roomID,
		UserID: userID,
		Send:   make(chan *domain.Message, 256),
	}
	client.Hub.Register <- client
	go client.WritePump()
	go client.ReadPump()
}
