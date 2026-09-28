package auth

import (
	"crypto/rsa"
	"fmt"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

// RSATokenValidator реализует TokenValidator с использованием алгоритма RS256.
type RSATokenValidator struct {
	publicKey *rsa.PublicKey
}

// TokenValidator описывает интерфейс проверки токенов.
type TokenValidator interface {
	ValidateToken(tokenStr string) (uuid.UUID, error)
}

// NewRSATokenValidator создает новый валидатор токенов.
func NewRSATokenValidator(publicKey *rsa.PublicKey) *RSATokenValidator {
	return &RSATokenValidator{publicKey: publicKey}
}

// ValidateToken парсит JWT токен, проверяет подпись с помощью публичного RSA-ключа
// и извлекает ID пользователя из поля "user_id".
func (v *RSATokenValidator) ValidateToken(tokenStr string) (uuid.UUID, error) {
	parsedToken, err := jwt.Parse(tokenStr, jwt.Keyfunc(func(token *jwt.Token) (any, error) {
		if _, ok := token.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return v.publicKey, nil
	}))
	if err != nil {
		return uuid.Nil, fmt.Errorf("invalid token: %v", err)
	}
	if !parsedToken.Valid {
		return uuid.Nil, fmt.Errorf("invalid token")
	}

	claims, ok := parsedToken.Claims.(jwt.MapClaims)
	if !ok {
		return uuid.Nil, fmt.Errorf("invalid token claims")
	}
	if userIdStr, ok := claims["user_id"].(string); ok {
		parsedUUID, err := uuid.Parse(userIdStr)
		if err == nil {
			return parsedUUID, nil
		}
	}
	return uuid.Nil, fmt.Errorf("invalid user ID in token")

}
