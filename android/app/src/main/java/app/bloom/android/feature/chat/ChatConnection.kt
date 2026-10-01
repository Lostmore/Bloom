package app.bloom.android.feature.chat

import app.bloom.android.core.model.Attachment
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.security.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
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
    val historyVersion: Long = 0,
    val connectionVersion: Long = 0,
    val roomsNeedingHistory: Set<Long> = emptySet(),
    val typingUntil: Map<String, Long> = emptyMap(),
    val onlineUsers: Map<String, Boolean> = emptyMap(),
    val actionError: String? = null,
    val errorVersion: Long = 0,
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
                    socket =
                        socketClient.newWebSocket(
                            Request.Builder().url(url).build(),
                            listener(attempt),
                        )
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
        mutable.update {
            it.copy(
                connected = false,
                connecting = false,
                typingUntil = emptyMap(),
                onlineUsers = emptyMap(),
            )
        }
    }

    @Synchronized
    fun send(text: String, attachments: List<Attachment> = emptyList()): Boolean {
        if (roomId == null || !mutable.value.connected || (text.isBlank() && attachments.isEmpty())) return false
        val payload = mutableMapOf<String, Any>("content" to text.trim(), "room_id" to roomId)
        if (attachments.any { it.url.isNullOrBlank() }) return false
        if (attachments.isNotEmpty())
            payload["attachments"] = attachments.map {
                mapOf("url" to it.url, "media_type" to it.mediaType)
            }
        return sendEvent("new_message", payload)
    }

    @Synchronized
    private fun sendEvent(type: String, payload: Map<String, Any>): Boolean {
        if (!mutable.value.connected) return false
        return socket?.send(json.toJson(mapOf("type" to type, "payload" to payload))) == true
    }

    fun typing(active: Boolean): Boolean =
        roomId?.let {
            sendEvent("typing", mapOf("room_id" to it, "is_typing" to active))
        } ?: false

    fun markRead(messageId: Long): Boolean = action("mark_as_read", messageId)

    fun edit(messageId: Long, text: String): Boolean =
        text.isNotBlank() && action("edit_message", messageId, text.trim())

    fun delete(messageId: Long): Boolean = action("delete_message", messageId)

    private fun action(type: String, messageId: Long, text: String? = null): Boolean {
        val room = roomId ?: return false
        if (messageId <= 0) return false
        val payload = mutableMapOf<String, Any>("room_id" to room, "message_id" to messageId)
        if (text != null) payload["content"] = text
        return sendEvent(type, payload)
    }

    fun clearActionError() {
        mutable.update { it.copy(actionError = null) }
    }

    @Synchronized
    fun mergeHistory(
        history: List<ChatMessage>,
        revision: Long = mutable.value.historyVersion,
        loadedRoom: Long? = roomId,
    ) {
        if (revision != mutable.value.historyVersion) return // A newer action needs a fresh HTTP snapshot.
        val valid = history.filter {
            it.id > 0 &&
                (loadedRoom == null || it.roomId == loadedRoom) &&
                !it.senderId.isNullOrBlank() &&
                !it.createdAt.isNullOrBlank()
        }
        mutable.update { current ->
            current.copy(
                messages = (current.messages + valid).associateBy { it.id }.values.sortedBy { it.id }.takeLast(500),
                roomsNeedingHistory = current.roomsNeedingHistory - setOfNotNull(loadedRoom),
            )
        }
    }

    private fun listener(attempt: Int) =
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = update {
                it.copy(
                    connected = true,
                    connecting = false,
                    error = null,
                    connectionVersion = it.connectionVersion + 1,
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.length > 256_000) return
                // Go sends an envelope, not a bare Message. Ignore malformed/unknown events.
                runCatching { handleEvent(json.fromJson(text, JsonObject::class.java)) }
            }

            private fun handleEvent(event: JsonObject) {
                val type = event.get("type")?.asString ?: return
                if (type == "error") {
                    val reason = event.get("error")?.asString ?: return
                    update {
                        it.copy(
                            actionError = "Действие не выполнено: ${reason.take(300)}",
                            errorVersion = it.errorVersion + 1,
                        )
                    }
                    return
                }
                val payload = event.getAsJsonObject("payload") ?: return
                val targetRoom = payload.get("room_id")?.asLong ?: return
                if (targetRoom <= 0 || (roomId != null && targetRoom != roomId)) return
                if (type in setOf("edit_message", "delete_message", "mark_as_read")) {
                    if ((payload.get("message_id")?.asLong ?: 0) <= 0) return
                    update {
                        it.copy(
                            historyVersion = it.historyVersion + 1,
                            roomsVersion = it.roomsVersion + 1,
                            roomsNeedingHistory = it.roomsNeedingHistory + targetRoom,
                        )
                    }
                    return
                }
                if (type == "typing" || type == "presence") {
                    val user = payload.get("user_id")?.asString?.takeIf { it.isNotBlank() } ?: return
                    if (type == "typing") {
                        val active = payload.get("is_typing")?.asBoolean ?: return
                        update {
                            it.copy(
                                typingUntil =
                                    if (active) it.typingUntil + (user to System.currentTimeMillis() + 5000)
                                    else it.typingUntil - user
                            )
                        }
                    } else {
                        val status = payload.get("status")?.asString ?: return
                        if (status !in setOf("online", "offline")) return
                        update {
                            it.copy(onlineUsers = it.onlineUsers + (user to (status == "online")))
                        }
                    }
                    return
                }
                if (type != "new_message") return
                val message = json.fromJson(payload, ChatMessage::class.java) ?: return
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
                    typingUntil = emptyMap(),
                    onlineUsers = emptyMap(),
                    error = "Связь прервалась. Нажми «Подключиться», чтобы продолжить.",
                )
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
                update {
                    it.copy(
                        connected = false,
                        connecting = false,
                        typingUntil = emptyMap(),
                        onlineUsers = emptyMap(),
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
