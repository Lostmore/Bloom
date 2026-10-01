package http

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"bloom.local/chat/internal/domain"
	"bloom.local/chat/internal/service"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
)

// A specialized mock that extends MockMessageRepo to allow customizing SearchByUser
type SearchMockMessageRepo struct {
	MockMessageRepo
	SearchByUserFunc func(ctx context.Context, userID uuid.UUID, query string, cursor string, limit int) (*domain.MessagePage, error)
}

func (m *SearchMockMessageRepo) SearchByUser(ctx context.Context, userID uuid.UUID, query string, cursor string, limit int) (*domain.MessagePage, error) {
	if m.SearchByUserFunc != nil {
		return m.SearchByUserFunc(ctx, userID, query, cursor, limit)
	}
	return nil, nil
}

func TestSearchMessages_Success(t *testing.T) {
	userID := uuid.New()
	mockRepo := &SearchMockMessageRepo{
		SearchByUserFunc: func(ctx context.Context, uid uuid.UUID, query string, cursor string, limit int) (*domain.MessagePage, error) {
			assert.Equal(t, userID, uid)
			assert.Equal(t, "hello", query)
			assert.Equal(t, 30, limit)

			nextCursor := "42"
			return &domain.MessagePage{
				Items: []*domain.Message{
					{ID: 43, Content: "hello world", CreatedAt: time.Now()},
				},
				NextCursor: &nextCursor,
			}, nil
		},
	}
	svc := service.NewMessageService(mockRepo, &MockRoomRepo{})
	handler := NewMessageHandler(svc)

	req := httptest.NewRequest(http.MethodGet, "/conversations/search?q=hello", nil)
	ctx := context.WithValue(req.Context(), UserIDKey, userID)
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	handler.SearchMessages(rr, req)

	assert.Equal(t, http.StatusOK, rr.Code)

	var page domain.MessagePage
	err := json.NewDecoder(rr.Body).Decode(&page)
	assert.NoError(t, err)
	assert.Len(t, page.Items, 1)
	assert.Equal(t, "42", *page.NextCursor)
}

func TestSearchMessages_EmptyQuery(t *testing.T) {
	svc := service.NewMessageService(&MockMessageRepo{}, &MockRoomRepo{})
	handler := NewMessageHandler(svc)

	req := httptest.NewRequest(http.MethodGet, "/conversations/search?q=%20%20", nil)
	ctx := context.WithValue(req.Context(), UserIDKey, uuid.New())
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	handler.SearchMessages(rr, req)

	assert.Equal(t, http.StatusBadRequest, rr.Code)
}

func TestSearchMessages_LongQuery(t *testing.T) {
	svc := service.NewMessageService(&MockMessageRepo{}, &MockRoomRepo{})
	handler := NewMessageHandler(svc)

	longQuery := ""
	for i := 0; i < 201; i++ {
		longQuery += "a"
	}

	req := httptest.NewRequest(http.MethodGet, "/conversations/search?q="+longQuery, nil)
	ctx := context.WithValue(req.Context(), UserIDKey, uuid.New())
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	handler.SearchMessages(rr, req)

	assert.Equal(t, http.StatusBadRequest, rr.Code)
}
