package http

import (
	"encoding/json"
	"fmt"
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

// Upload godoc
// @Summary      Загрузить медиа-файл
// @Description  Загружает файл на сервер и делает его временным до подтверждения
// @Tags         media
// @Accept       multipart/form-data
// @Produce      json
// @Param        file formData file true "Файл для загрузки"
// @Success      200  {object}  map[string]string "Пример: {\"url\": \"/api/v1/media/uuid\"}"
// @Failure      400  {string}  string "Invalid file"
// @Failure      500  {string}  string "Internal server error"
// @Router       /media/upload [post]
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

// ServeMedia godoc
// @Summary      Скачать/Просмотреть медиа-файл
// @Description  Отдает содержимое файла по его ID
// @Tags         media
// @Produce      application/octet-stream
// @Param        id path string true "ID медиа-файла"
// @Success      200  {file}    file "Содержимое файла"
// @Failure      400  {string}  string "ID is required"
// @Failure      404  {string}  string "File not found"
// @Failure      500  {string}  string "Internal server error"
// @Router       /media/{id} [get]
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

	w.Header().Set("Content-Type", mediaFile.MimeType)
	w.Header().Set("Content-Disposition", fmt.Sprintf(`inline; filename="%s"`, mediaFile.OriginalName))

	http.ServeFile(w, r, filePath)
}
