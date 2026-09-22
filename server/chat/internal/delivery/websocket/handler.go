package websocket

import (
	"log"
	"net/http"
	"strconv"

	"bloom.local/chat/internal/domain"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

func ServeWS(hub *Hub, w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Ошибка апгрейда", err)
		return
	}
	roomIDStr := r.URL.Query().Get("room_id")
	userIDStr := r.URL.Query().Get("user_id")
	roomID, err := strconv.ParseInt(roomIDStr, 10, 64)
	if err != nil {
		log.Println("Ошибка парсинга room_id", err)
		conn.Close()
		return
	}
	userID, err := strconv.ParseInt(userIDStr, 10, 64)
	if err != nil {
		log.Println("Ошибка парсинга user_id", err)
		conn.Close()
		return
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
