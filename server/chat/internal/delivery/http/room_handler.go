package http

import (
	"encoding/json"
	"net/http"
	"strconv"

	"bloom.local/chat/internal/service"
)

// CreateRoomRequest - структура для входящих данных при создании комнаты.
type CreateRoomRequest struct {
	User1ID int64 `json:"user1_id"`
	User2ID int64 `json:"user2_id"`
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
	if req.User1ID <= 0 || req.User2ID <= 0 {
		http.Error(w, "Invalid user IDs", http.StatusBadRequest)
		return
	}
	room, err := h.Service.CreateRoom(r.Context(), req.User1ID, req.User2ID)
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
	r.ParseForm()
	userIDStr := r.Form.Get("user_id")
	userID, err := strconv.ParseInt(userIDStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid user ID", http.StatusBadRequest)
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
