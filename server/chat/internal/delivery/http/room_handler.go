package http

import (
	"encoding/json"
	"net/http"

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
}

// NewRoomHandler создает новый обработчик HTTP запросов для комнат.
func NewRoomHandler(svc *service.MessageService) *RoomHandler {
	return &RoomHandler{Service: svc}
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
