package auth

import (
	"crypto/rsa"
	"fmt"

	"github.com/golang-jwt/jwt/v5"
)

// RSATokenValidator реализует TokenValidator с использованием алгоритма RS256.
type RSATokenValidator struct {
	publicKey *rsa.PublicKey
}

// TokenValidator описывает интерфейс проверки токенов.
type TokenValidator interface {
	ValidateToken(tokenStr string) (int64, error)
}

// NewRSATokenValidator создает новый валидатор токенов.
func NewRSATokenValidator(publicKey *rsa.PublicKey) *RSATokenValidator {
	return &RSATokenValidator{publicKey: publicKey}
}

// ValidateToken парсит JWT токен, проверяет подпись с помощью публичного RSA-ключа
// и извлекает ID пользователя из поля "user_id".
func (v *RSATokenValidator) ValidateToken(tokenStr string) (int64, error) {
	parsedToken, err := jwt.Parse(tokenStr, jwt.Keyfunc(func(token *jwt.Token) (any, error) {
		if _, ok := token.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return v.publicKey, nil
	}))
	if err != nil {
		return 0, fmt.Errorf("invalid token: %v", err)
	}
	if !parsedToken.Valid {
		return 0, fmt.Errorf("invalid token")
	}

	claims, ok := parsedToken.Claims.(jwt.MapClaims)
	if !ok {
		return 0, fmt.Errorf("invalid token claims")
	}
	if userIdFloat, ok := claims["user_id"].(float64); ok {
		return int64(userIdFloat), nil
	}
	return 0, fmt.Errorf("invalid user ID in token")

}
