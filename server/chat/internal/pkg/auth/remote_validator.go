package auth

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
)

// TokenValidator описывает интерфейс проверки токенов.
type TokenValidator interface {
	ValidateToken(tokenStr string) (uuid.UUID, error)
}

// RemoteTokenValidator реализует TokenValidator через вызов Identity микросервиса.
type RemoteTokenValidator struct {
	identityURL   string
	internalToken string
	client        *http.Client
}

// NewRemoteTokenValidator создает новый валидатор токенов, обращающийся к Identity.
func NewRemoteTokenValidator(identityURL, internalToken string) *RemoteTokenValidator {
	return &RemoteTokenValidator{
		identityURL:   identityURL,
		internalToken: internalToken,
		client: &http.Client{
			Timeout: 5 * time.Second,
		},
	}
}

type tokenRequest struct {
	Token string `json:"token"`
}

type tokenStatus struct {
	Active    bool      `json:"active"`
	AccountId uuid.UUID `json:"accountId"`
	FamilyId  uuid.UUID `json:"familyId"`
}

// ValidateToken проверяет токен через Identity микросервис.
func (v *RemoteTokenValidator) ValidateToken(tokenStr string) (uuid.UUID, error) {
	reqBody, err := json.Marshal(tokenRequest{Token: tokenStr})
	if err != nil {
		return uuid.Nil, fmt.Errorf("failed to marshal token request: %w", err)
	}

	req, err := http.NewRequest(http.MethodPost, v.identityURL+"/internal/identity/introspect", bytes.NewBuffer(reqBody))
	if err != nil {
		return uuid.Nil, fmt.Errorf("failed to create request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	if v.internalToken != "" {
		req.Header.Set("X-Internal-Token", v.internalToken)
	}

	resp, err := v.client.Do(req)
	if err != nil {
		return uuid.Nil, fmt.Errorf("failed to execute request to identity: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return uuid.Nil, fmt.Errorf("identity service returned status: %d", resp.StatusCode)
	}

	var status tokenStatus
	if err := json.NewDecoder(resp.Body).Decode(&status); err != nil {
		return uuid.Nil, fmt.Errorf("failed to decode identity response: %w", err)
	}

	if !status.Active {
		return uuid.Nil, fmt.Errorf("token is not active or invalid")
	}
	if status.AccountId == uuid.Nil {
		return uuid.Nil, fmt.Errorf("active token does not contain accountId")
	}

	return status.AccountId, nil
}
