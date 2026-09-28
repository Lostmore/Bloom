// Package docs serves the generated public OpenAPI document.
package docs

import (
	_ "embed"
	"net/http"
)

//go:generate go run github.com/swaggo/swag/v2/cmd/swag@v2.0.0-rc6 init -g main.go -d ../cmd,../internal/delivery/http,../internal/domain --parseInternal --v3.1 --outputTypes json,yaml -o .

//go:embed swagger.json
var specification []byte

func Handler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Write(specification)
}
