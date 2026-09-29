package app.bloom.android.feature.chat

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
)

/** Go's current protocol: JSON messages/history on /ws?room_id=...&token=.... */
class ChatConnection(
    private val http: OkHttpClient,
    private val sessions: SessionManager,
    private val baseUrl: HttpUrl,
    private val roomId: Long,
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
                val url =
                    baseUrl
                        .resolve("ws")!!
                        .newBuilder()
                        .addQueryParameter("room_id", roomId.toString())
                        .addQueryParameter("token", token)
                        .build()
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
    fun send(text: String): Boolean {
        if (!mutable.value.connected || text.isBlank()) return false
        return socket?.send(json.toJson(mapOf("content" to text.trim()))) == true
    }

    private fun listener(attempt: Int) =
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = update {
                it.copy(connected = true, connecting = false, error = null)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.length > 256_000) return
                val message = runCatching { json.fromJson(text, ChatMessage::class.java) }.getOrNull() ?: return
                if (message.id <= 0 || message.roomId != roomId || message.senderId.isNullOrBlank()) return
                update { current ->
                    current.copy(
                        messages = (current.messages + message).distinctBy { it.id }.sortedBy { it.id }.takeLast(500)
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
