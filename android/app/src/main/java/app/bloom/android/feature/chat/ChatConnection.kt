package app.bloom.android.feature.chat

import app.bloom.android.core.model.Attachment
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.security.SessionManager
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.*

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val error: String? = null,
    val roomsVersion: Long = 0,
)

/** Live events for all rooms on /ws?token=...; history is fetched separately over REST. */
class ChatConnection(
    private val http: OkHttpClient,
    private val sessions: SessionManager,
    private val baseUrl: HttpUrl,
    private val roomId: Long? = null,
) {
    private val json = Gson()
    private val socketClient =
        http
            .newBuilder()
            .authenticator(Authenticator.NONE)
            .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    private val mutable = MutableStateFlow(ChatState())
    val state = mutable.asStateFlow()
    private var socket: WebSocket? = null
    private var generation = 0

    suspend fun connect() =
        withContext(Dispatchers.IO) {
            val attempt =
                synchronized(this@ChatConnection) {
                    disconnect()
                    mutable.update { it.copy(connecting = true, error = null) }
                    generation
                }
            try {
                val token = sessions.freshToken() ?: throw IOException("Signed out")
                // Never log this URL: the Go handshake currently accepts its credential in a query
                // parameter.
                val url = baseUrl.resolve("ws")!!.newBuilder().addQueryParameter("token", token).build()
                synchronized(this@ChatConnection) {
                    if (attempt != generation) return@withContext
                    socket = socketClient.newWebSocket(Request.Builder().url(url).build(), listener(attempt))
                }
            } catch (_: IOException) {
                synchronized(this@ChatConnection) {
                    if (attempt == generation)
                        mutable.update {
                            it.copy(
                                connecting = false,
                                error = "Не удалось подключиться. Проверь интернет и попробуй ещё раз.",
                            )
                        }
                }
            }
        }

    @Synchronized
    fun disconnect() {
        generation++
        socket?.close(1000, "Screen inactive")
        socket = null
        mutable.update { it.copy(connected = false, connecting = false) }
    }

    @Synchronized
    fun send(text: String, attachments: List<Attachment> = emptyList(), clientMessageId: String? = null): Boolean {
        if (roomId == null || !mutable.value.connected || (text.isBlank() && attachments.isEmpty())) return false
        val payload = mutableMapOf<String, Any>("content" to text.trim(), "room_id" to roomId)
        if (attachments.isNotEmpty())
            payload["attachments"] = attachments.map { mapOf("media_id" to it.mediaId, "media_type" to it.mediaType) }
        if (clientMessageId != null) payload["client_message_id"] = clientMessageId
        return socket?.send(json.toJson(payload)) == true
    }

    @Synchronized
    fun mergeHistory(history: List<ChatMessage>) {
        val valid = history.filter {
            it.id > 0 && it.roomId == roomId && !it.senderId.isNullOrBlank() && !it.createdAt.isNullOrBlank()
        }
        mutable.update { current ->
            current.copy(
                messages = (valid + current.messages).associateBy { it.id }.values.sortedBy { it.id }.takeLast(500)
            )
        }
    }

    private fun listener(attempt: Int) =
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = update {
                it.copy(connected = true, connecting = false, error = null)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.length > 256_000) return
                val message = runCatching { json.fromJson(text, ChatMessage::class.java) }.getOrNull() ?: return
                if (message.roomId <= 0 || message.senderId.isNullOrBlank()) return
                if (message.id == 0L) {
                    update { it.copy(roomsVersion = it.roomsVersion + 1) }
                    return
                }
                if (message.id < 0 || (roomId != null && message.roomId != roomId) || message.createdAt.isNullOrBlank())
                    return
                update { current ->
                    current.copy(
                        messages =
                            (current.messages.filterNot { it.id == message.id } + message)
                                .sortedBy { it.id }
                                .takeLast(500)
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = update {
                it.copy(
                    connected = false,
                    connecting = false,
                    error = "Связь прервалась. Нажми «Подключиться», чтобы продолжить.",
                )
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
                update {
                    it.copy(
                        connected = false,
                        connecting = false,
                        error = "Соединение закрыто. Можно подключиться заново.",
                    )
                }
            }

            private fun update(transform: (ChatState) -> ChatState) {
                synchronized(this@ChatConnection) {
                    if (attempt == generation) mutable.update(transform)
                }
            }
        }
}
