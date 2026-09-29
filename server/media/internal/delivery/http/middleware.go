package http

import (
	"context"
	"net/http"
	"strings"

	"bloom.local/media/internal/pkg/auth"
)

type contextKey string

const UserIDKey contextKey = "user_id"

func AuthMiddleware(validator auth.TokenValidator, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			http.Error(w, "Authorization header required", http.StatusUnauthorized)
			return
		}
		tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
		if tokenStr == "" {
			http.Error(w, "Token required", http.StatusUnauthorized)
			return
		}
		userId, err := validator.ValidateToken(tokenStr)
		if err != nil {
			http.Error(w, "Invalid token", http.StatusUnauthorized)
			return
		}
		ctx := context.WithValue(r.Context(), UserIDKey, userId)
		next.ServeHTTP(w, r.WithContext(ctx))
	}
}
