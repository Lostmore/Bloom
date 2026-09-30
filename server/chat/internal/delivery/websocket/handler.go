package websocket

import (
	"context"
	"log"
	"net/http"

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
	rooms, err := hub.Service.GetUserRooms(context.Background(), userID)
	if err != nil {
		log.Println("Ошибка получения комнат", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	var roomIDs []int64
	for _, room := range rooms {
		roomIDs = append(roomIDs, room.ID)
	}
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Ошибка апгрейда", err)
		return
	}
	client := &Client{
		Hub:     hub,
		Conn:    conn,
		RoomsID: roomIDs,
		UserID:  userID,
		Send:    make(chan *domain.Message, 256),
	}
	client.Hub.Register <- client
	go client.WritePump()
	go client.ReadPump()
}
