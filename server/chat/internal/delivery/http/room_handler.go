package http

import (
	"encoding/json"
	"net/http"
	"strconv"

	"bloom.local/chat/internal/delivery/websocket"
	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
)

// CreateRoomRequest - структура для входящих данных при создании комнаты.
type CreateRoomRequest struct {
	User2ID uuid.UUID `json:"user2_id" swaggertype:"string" format:"uuid" binding:"required"`
}

// RoomHandler обрабатывает HTTP-запросы (REST API), связанные с комнатами.
type RoomHandler struct {
	Service *service.MessageService
	Hub     *websocket.Hub
}

// NewRoomHandler создает новый обработчик HTTP запросов для комнат.
func NewRoomHandler(svc *service.MessageService, hub *websocket.Hub) *RoomHandler {
	return &RoomHandler{Service: svc, Hub: hub}
}

// CreateRoom godoc
// @Summary      Создать комнату
// @Description  Создает новую комнату чата с выбранным пользователем
// @Tags         rooms
// @Accept       json
// @Produce      json
// @Param        request body CreateRoomRequest true "Данные для создания комнаты"
// @Success      201  {object}  domain.Room "Созданная комната"
// @Failure      400  {string}  string "Invalid request body"
// @Failure      401  {string}  string "Authorization token invalid"
// @Failure      500  {string}  string "Failed to create room"
// @Security     BearerAuth
// @Router       /api/rooms [post]
func (h *RoomHandler) CreateRoom(w http.ResponseWriter, r *http.Request) {
	var req CreateRoomRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}
	userID, ok := r.Context().Value(UserIDKey).(uuid.UUID)
	if !ok {
		http.Error(w, "Authorization token invalid", http.StatusUnauthorized)
		return
	}
	if req.User2ID == uuid.Nil {
		http.Error(w, "Invalid user ID", http.StatusBadRequest)
		return
	}
	room, err := h.Service.CreateRoom(r.Context(), userID, req.User2ID)
	if err != nil {
		http.Error(w, "Failed to create room", http.StatusInternalServerError)
		return
	}
	h.Hub.JoinRoom <- &websocket.JoinRoomRequest{UserID: userID, RoomID: room.ID}
	h.Hub.JoinRoom <- &websocket.JoinRoomRequest{UserID: req.User2ID, RoomID: room.ID}
	sysMsg := &domain.Message{
		RoomID:   room.ID,
		SenderID: userID,
		Content:  "Чат создан",
	}
	sysMsgBytes, _ := json.Marshal(sysMsg)
	h.Hub.Broadcast <- &websocket.BroadcastMessage{
		Client: nil,
		Message: &websocket.WSMessage{
			Type:    "new_message",
			Payload: json.RawMessage(sysMsgBytes),
		},
	}
	w.WriteHeader(http.StatusCreated)
	if err := json.NewEncoder(w).Encode(room); err != nil {
		http.Error(w, "Failed to encode room", http.StatusInternalServerError)
	}
}

// GetUserRooms godoc
// @Summary      Получить список комнат
// @Description  Возвращает список комнат, в которых состоит текущий пользователь
// @Tags         rooms
// @Produce      json
// @Success      200  {array}   domain.Room "Список комнат"
// @Failure      401  {string}  string "Authorization token invalid"
// @Failure      500  {string}  string "Failed to get user rooms"
// @Security     BearerAuth
// @Router       /api/rooms [get]
func (h *RoomHandler) GetUserRooms(w http.ResponseWriter, r *http.Request) {
	userID, ok := r.Context().Value(UserIDKey).(uuid.UUID)
	if !ok {
		http.Error(w, "Authorization token invalid", http.StatusUnauthorized)
		return
	}
	rooms, err := h.Service.GetUserRooms(r.Context(), userID)
	if err != nil {
		http.Error(w, "Failed to get user rooms", http.StatusInternalServerError)
		return
	}
	if err := json.NewEncoder(w).Encode(rooms); err != nil {
		http.Error(w, "Failed to encode rooms", http.StatusInternalServerError)
	}
}

// GetRoomHistory возвращает историю сообщений чата
func (h *RoomHandler) GetRoomHistory(w http.ResponseWriter, r *http.Request) {
	userID, ok := r.Context().Value(UserIDKey).(uuid.UUID)
	if !ok {
		http.Error(w, "Authorization token invalid", http.StatusUnauthorized)
		return
	}

	roomIDStr := r.PathValue("id")
	if roomIDStr == "" {
		http.Error(w, "room_id is required", http.StatusBadRequest)
		return
	}
	roomID, err := strconv.ParseInt(roomIDStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid room_id", http.StatusBadRequest)
		return
	}
	msgs, err := h.Service.GetRoomHistory(r.Context(), roomID, userID)
	if err != nil {
		http.Error(w, "Failed to get room history", http.StatusInternalServerError)
		return
	}

	if msgs == nil {
		msgs = []*domain.Message{}
	}
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(msgs); err != nil {
		http.Error(w, "Failed to encode messages", http.StatusInternalServerError)
	}
}
