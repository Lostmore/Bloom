package http

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"

	"bloom.local/media/internal/service"
)

type MediaHandler struct {
	svc     *service.MediaService
	hostUrl string
}

func NewMediaHandler(svc *service.MediaService, hostUrl string) *MediaHandler {
	return &MediaHandler{svc: svc, hostUrl: hostUrl}
}

// Upload принимает загрузку файла
func (h *MediaHandler) Upload(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	r.ParseMultipartForm(10 << 20)
	file, header, err := r.FormFile("file")
	if err != nil {
		http.Error(w, "Invalid file", http.StatusBadRequest)
		return
	}
	defer file.Close()
	fileContent, err := io.ReadAll(file)
	if err != nil {
		http.Error(w, "Failed to read file", http.StatusInternalServerError)
		return
	}
	mediaFile, err := h.svc.Upload(r.Context(), fileContent, header.Filename, header.Header.Get("Content-Type"))
	if err != nil {
		http.Error(w, "Failed to upload file", http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(map[string]string{
		"url": h.hostUrl + "/media/" + mediaFile.ID,
	})
}

// ServeMedia отдает сам файл для просмотра (например, когда браузер запрашивает картинку)
func (h *MediaHandler) ServeMedia(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/media/")
	if id == "" {
		http.Error(w, "ID is required", http.StatusBadRequest)
		return
	}
	mediaFile, filePath, err := h.svc.GetMedia(r.Context(), id)
	if err != nil {
		http.Error(w, "Failed to get media", http.StatusInternalServerError)
		return
	}
	if mediaFile == nil {
		http.NotFound(w, r)
		return
	}
	http.ServeFile(w, r, filePath)
}
