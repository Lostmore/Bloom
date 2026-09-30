package app.bloom.android.feature.chat

import android.content.ContentResolver
import android.net.Uri
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Attachment
import app.bloom.android.core.model.MediaCapabilities
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun mediaId(attachment: Attachment): String? {
    val candidate = attachment.mediaId ?: attachment.url?.substringBefore('?')?.substringAfterLast('/')
    return candidate?.takeIf { runCatching { UUID.fromString(it).toString() == it.lowercase() }.getOrDefault(false) }
}

fun InputStream.readImageBytes(limit: Int = 10_485_760): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "Файл слишком большой. Максимум — ${limit / 1_048_576} МБ." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

suspend fun uploadChatPhoto(
    graph: AppGraph,
    resolver: ContentResolver,
    uri: String,
    roomId: Long,
    key: String,
    capabilities: MediaCapabilities,
): Attachment =
    withContext(Dispatchers.IO) {
        check(capabilities.privateChatAttachments) { "Отправка фото пока недоступна на сервере." }
        val source = Uri.parse(uri)
        require(source.scheme == "content")
        val mime = resolver.getType(source) ?: ""
        require(mime in setOf("image/jpeg", "image/png", "image/webp")) { "Выбери фотографию JPEG, PNG или WebP." }
        val bytes =
            resolver.openInputStream(source)?.use {
                it.readImageBytes(capabilities.maxUploadBytes.coerceIn(1, 10_485_760).toInt())
            } ?: error("Не удалось открыть фото.")
        val result =
            graph.media.uploadChatPhoto(
                key,
                roomId.toString().toRequestBody("text/plain".toMediaType()),
                MultipartBody.Part.createFormData("file", "photo", bytes.toRequestBody(mime.toMediaType())),
            )
        val attachment = Attachment(mediaType = result.mimeType, mediaId = result.mediaId)
        require(mediaId(attachment) != null && result.mimeType.startsWith("image/")) {
            "Сервер вернул некорректное фото."
        }
        attachment
    }
