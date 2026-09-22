package websocket

import (
	"context"
	"log"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/gorilla/websocket"
)

type Client struct {
	Hub    *Hub
	Conn   *websocket.Conn
	RoomID int64
	UserID int64
	Send   chan *domain.Message
}
type Hub struct {
	Rooms      map[int64]map[*Client]bool
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
		msg.RoomID = c.RoomID
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
		Broadcast:  make(chan *domain.Message),
		Register:   make(chan *Client),
		Unregister: make(chan *Client),
		Service:    svc,
	}
}
func (h *Hub) Run() {
	for {
		select {
		case client := <-h.Register:
			{
				if _, ok := h.Rooms[client.RoomID]; !ok {
					h.Rooms[client.RoomID] = make(map[*Client]bool)
				}
				h.Rooms[client.RoomID][client] = true
			}
		case client := <-h.Unregister:
			if _, ok := h.Rooms[client.RoomID]; ok {
				delete(h.Rooms[client.RoomID], client)
				if len(h.Rooms[client.RoomID]) == 0 {
					delete(h.Rooms, client.RoomID)
				}
				close(client.Send)
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
