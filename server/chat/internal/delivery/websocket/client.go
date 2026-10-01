package websocket

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
	"github.com/gorilla/websocket"
)

type WSMessage struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload,omitempty"`
	Error   string          `json:"error,omitempty"`
}

type TypingPayload struct {
	RoomID   int64     `json:"room_id"`
	UserID   uuid.UUID `json:"user_id"`
	IsTyping bool      `json:"is_typing"`
}

type PresencePayload struct {
	UserID uuid.UUID `json:"user_id"`
	Status string    `json:"status"`
}

type BroadcastMessage struct {
	Client  *Client
	Message *WSMessage
}

type JoinRoomRequest struct {
	UserID uuid.UUID
	RoomID int64
}

type Client struct {
	Hub     *Hub
	Conn    *websocket.Conn
	RoomsID []int64
	UserID  uuid.UUID
	Send    chan *WSMessage
}
type Hub struct {
	Rooms      map[int64]map[*Client]bool
	Users      map[uuid.UUID]map[*Client]bool
	JoinRoom   chan *JoinRoomRequest
	Broadcast  chan *BroadcastMessage
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
		var req WSMessage
		err := c.Conn.ReadJSON(&req)
		if err != nil {
			break
		}
		switch req.Type {
		case "new_message":
			var msg domain.Message
			err = json.Unmarshal(req.Payload, &msg)
			if err != nil {
				continue
			}
			msg.SenderID = c.UserID
			err = c.Hub.Service.SendMessage(context.Background(), &msg)
			if err != nil {
				log.Println("Ошибка сохранения сообщения:", err)
				c.Send <- &WSMessage{
					Type:  "error",
					Error: err.Error(),
				}
				continue
			}
			msgBytes, _ := json.Marshal(&msg)
			c.Hub.Broadcast <- &BroadcastMessage{
				Client: c,
				Message: &WSMessage{
					Type:    "new_message",
					Payload: json.RawMessage(msgBytes),
				},
			}
		case "typing":
			var typing TypingPayload
			err := json.Unmarshal(req.Payload, &typing)
			if err != nil {
				continue
			}
			typing.UserID = c.UserID
			typingBytes, _ := json.Marshal(&typing)
			c.Hub.Broadcast <- &BroadcastMessage{
				Client: c,
				Message: &WSMessage{
					Type:    "typing",
					Payload: json.RawMessage(typingBytes),
				},
			}
		}
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
		Broadcast:  make(chan *BroadcastMessage),
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
			// 1. Фиксируем, первое ли это подключение
			isFirstConnection := len(h.Users[client.UserID]) == 0
			if isFirstConnection {
				h.Users[client.UserID] = make(map[*Client]bool)
			}
			h.Users[client.UserID][client] = true
			// 2. Обрабатываем каждую комнату клиента
			for _, roomID := range client.RoomsID {
				if _, ok := h.Rooms[roomID]; !ok {
					h.Rooms[roomID] = make(map[*Client]bool)
				}
				h.Rooms[roomID][client] = true
				// 🎯 ОТВЕТ НА ТВОЙ ВОПРОС:
				// Сразу сообщаем нашему НОВОМУ клиенту обо всех собеседниках, кто УЖЕ онлайн в этой комнате
				for otherClient := range h.Rooms[roomID] {
					if otherClient.UserID != client.UserID {
						payload := fmt.Sprintf(`{"room_id":%d,"user_id":"%s","status":"online"}`, roomID, otherClient.UserID)
						client.Send <- &WSMessage{Type: "presence", Payload: json.RawMessage(payload)}
					}
				}
				// Если это первое подключение юзера — оповещаем всех остальных в комнате, что он вошел
				if isFirstConnection {
					payload := fmt.Sprintf(`{"room_id":%d,"user_id":"%s","status":"online"}`, roomID, client.UserID)
					msg := &WSMessage{Type: "presence", Payload: json.RawMessage(payload)}

					for otherClient := range h.Rooms[roomID] {
						if otherClient.UserID != client.UserID {
							otherClient.Send <- msg
						}
					}
				}
			}

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
					for _, roomID := range client.RoomsID {
						payload := fmt.Sprintf(`{"room_id":%d,"user_id":"%s","status":"offline"}`, roomID, client.UserID)
						msg := &WSMessage{
							Type:    "presence",
							Payload: json.RawMessage(payload),
						}
						if clients, ok := h.Rooms[roomID]; ok {
							for otherClient := range clients {
								otherClient.Send <- msg
							}
						}
					}
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
		case bm := <-h.Broadcast:
			var extract struct {
				RoomID int64 `json:"room_id"`
			}
			if err := json.Unmarshal(bm.Message.Payload, &extract); err != nil {
				continue
			}
			if bm.Client != nil {
				if _, ok := h.Rooms[extract.RoomID][bm.Client]; !ok {
					continue
				}
			}
			if clients, ok := h.Rooms[extract.RoomID]; ok {
				for client := range clients {
					if client == bm.Client && bm.Message.Type == "typing" {
						continue
					}
					select {
					case client.Send <- bm.Message:
					default:
						close(client.Send)
						delete(clients, client)
					}
				}
			}
		}
	}
}
