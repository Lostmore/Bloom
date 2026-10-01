package http

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
)

type MessageHandler struct {
	messageService *service.MessageService
}

func NewMessageHandler(messageService *service.MessageService) *MessageHandler {
	return &MessageHandler{messageService: messageService}
}

func (h *MessageHandler) SearchMessages(w http.ResponseWriter, r *http.Request) {
	userID, ok := r.Context().Value(UserIDKey).(uuid.UUID)
	if !ok {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	query := r.URL.Query().Get("q")
	query = strings.TrimSpace(query)
	if len(query) == 0 || len(query) > 200 {
		http.Error(w, "Query is empty", http.StatusBadRequest)
		return
	}
	cursor := r.URL.Query().Get("cursor")
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	if limit <= 0 {
		limit = 30
	}
	if limit > 100 {
		limit = 100
	}
	page, err := h.messageService.SearchMessages(r.Context(), userID, query, cursor, limit)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(page)
}
