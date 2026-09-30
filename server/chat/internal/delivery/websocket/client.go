package websocket

import (
	"context"
	"log"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
	"github.com/gorilla/websocket"
)

type JoinRoomRequest struct {
	UserID uuid.UUID
	RoomID int64
}

type Client struct {
	Hub     *Hub
	Conn    *websocket.Conn
	RoomsID []int64
	UserID  uuid.UUID
	Send    chan *domain.Message
}
type Hub struct {
	Rooms      map[int64]map[*Client]bool
	Users      map[uuid.UUID]map[*Client]bool
	JoinRoom   chan *JoinRoomRequest
	Broadcast  chan *domain.Message
	Register   chan *Client
	Unregister chan *Client
	Service    *service.MessageService
}

func (c *Client) ReadPump() {
	defer func() {
		c.Hub.Unregister <- c
		c.Conn.Close()
	}()
	for {
		var msg domain.Message
		err := c.Conn.ReadJSON(&msg)
		if err != nil {
			break
		}
		msg.SenderID = c.UserID
		err = c.Hub.Service.SendMessage(context.Background(), &msg)
		if err != nil {
			log.Println("Ошибка сохранения сообщения:", err)
		}
		c.Hub.Broadcast <- &msg
	}
}
func (c *Client) WritePump() {
	defer func() {
		c.Conn.Close()
	}()
	for msg := range c.Send {
		err := c.Conn.WriteJSON(msg)
		if err != nil {
			break
		}
	}
}
func NewHub(svc *service.MessageService) *Hub {
	return &Hub{
		Rooms:      make(map[int64]map[*Client]bool),
		Users:      make(map[uuid.UUID]map[*Client]bool),
		Broadcast:  make(chan *domain.Message),
		JoinRoom:   make(chan *JoinRoomRequest),
		Register:   make(chan *Client),
		Unregister: make(chan *Client),
		Service:    svc,
	}
}
func (h *Hub) Run() {
	for {
		select {
		case client := <-h.Register:
			for _, roomID := range client.RoomsID {
				if _, ok := h.Rooms[roomID]; !ok {
					h.Rooms[roomID] = make(map[*Client]bool)
				}
				h.Rooms[roomID][client] = true
			}
			if _, ok := h.Users[client.UserID]; !ok {
				h.Users[client.UserID] = make(map[*Client]bool)
			}
			h.Users[client.UserID][client] = true

		case client := <-h.Unregister:
			for _, roomID := range client.RoomsID {
				if _, ok := h.Rooms[roomID]; ok {
					delete(h.Rooms[roomID], client)
					if len(h.Rooms[roomID]) == 0 {
						delete(h.Rooms, roomID)
					}
				}
			}
			if _, ok := h.Users[client.UserID]; ok {
				delete(h.Users[client.UserID], client)
				if len(h.Users[client.UserID]) == 0 {
					delete(h.Users, client.UserID)
				}
			}
			close(client.Send)
		case req := <-h.JoinRoom:
			if clients, ok := h.Users[req.UserID]; ok {
				if _, ok := h.Rooms[req.RoomID]; !ok {
					h.Rooms[req.RoomID] = make(map[*Client]bool)
				}
				for client := range clients {
					h.Rooms[req.RoomID][client] = true
					client.RoomsID = append(client.RoomsID, req.RoomID)
				}
			}
		case msg := <-h.Broadcast:
			if clients, ok := h.Rooms[msg.RoomID]; ok {
				for client := range clients {
					select {
					case client.Send <- msg:
					default:
						close(client.Send)
						delete(clients, client)
					}
				}
			}
		}
	}
}
