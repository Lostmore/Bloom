package http

import (
	"encoding/json"
	"net/http"

	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
)

// CreateRoomRequest - структура для входящих данных при создании комнаты.
type CreateRoomRequest struct {
	User2ID uuid.UUID `json:"user2_id"`
}

// RoomHandler обрабатывает HTTP-запросы (REST API), связанные с комнатами.
type RoomHandler struct {
	Service *service.MessageService
}

// NewRoomHandler создает новый обработчик HTTP запросов для комнат.
func NewRoomHandler(svc *service.MessageService) *RoomHandler {
	return &RoomHandler{Service: svc}
}

// Обработчик для POST /api/rooms
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
	w.WriteHeader(http.StatusCreated)
	if err := json.NewEncoder(w).Encode(room); err != nil {
		http.Error(w, "Failed to encode room", http.StatusInternalServerError)
	}
}

// Обработчик для POST /api/rooms?user_id
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
