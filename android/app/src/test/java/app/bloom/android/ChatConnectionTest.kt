package app.bloom.android

import app.bloom.android.core.security.Session
import app.bloom.android.core.security.SessionManager
import app.bloom.android.core.security.SessionStore
import app.bloom.android.feature.chat.ChatConnection
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class ChatConnectionTest {
    @Test
    fun currentGoProtocolConnectsSendsAndDeduplicatesHistory() = runBlocking {
        val server = MockWebServer()
        val received = LinkedBlockingQueue<String>()
        server.enqueue(
            MockResponse()
                .withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            webSocket.send(message(1, 7))
                            webSocket.send(message(1, 7))
                            webSocket.send(message(2, 999))
                            webSocket.send("{\"error\":\"not a message\"}")
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            received.add(text)
                            webSocket.send(message(3, 7))
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, null)
                        }
                    }
                )
        )
        server.start()
        val client = OkHttpClient()
        val store =
            object : SessionStore {
                override fun load() = Session("user-access", "user-refresh", System.currentTimeMillis() + 900_000)

                override fun save(session: Session?) = Unit
            }
        val session = SessionManager(store, client, server.url("/api/v1/"))
        val connection = ChatConnection(client, session, server.url("/api/v1/"), 7)
        try {
            connection.connect()
            withTimeout(5000) { connection.state.first { it.connected && it.messages.size == 1 } }
            assertTrue(connection.send("Привет"))
            assertEquals(
                "{\"type\":\"new_message\",\"payload\":{\"content\":\"Привет\",\"room_id\":7}}",
                received.poll(5, TimeUnit.SECONDS),
            )
            withTimeout(5000) { connection.state.first { it.messages.size == 2 } }
            assertEquals(listOf(1L, 3L), connection.state.value.messages.map { it.id })
            connection.mergeHistory(
                listOf(
                    app.bloom.android.core.model.ChatMessage(
                        1,
                        7,
                        "partner",
                        "old snapshot",
                        "2026-09-29T12:00:00Z",
                    )
                )
            )
            assertEquals(listOf(1L, 3L), connection.state.value.messages.map { it.id })
            assertEquals("old snapshot", connection.state.value.messages.first().content)
            val request = server.takeRequest()
            assertEquals("/api/v1/ws", request.requestUrl!!.encodedPath)
            assertEquals("user-access", request.requestUrl!!.queryParameter("token"))
            assertNull(request.requestUrl!!.queryParameter("room_id"))
            assertNull(request.getHeader("X-Internal-Token"))
            connection.disconnect()
            assertFalse(connection.send("После выхода"))
        } finally {
            connection.disconnect()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    @Test
    fun listConnectionReceivesMultipleRoomsAndRoomCreationWithoutFakeMessage() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            webSocket.send(message(1, 7))
                            webSocket.send(message(2, 8))
                            webSocket.send(message(0, 9))
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, null)
                        }
                    }
                )
        )
        server.start()
        val client = OkHttpClient()
        val store =
            object : SessionStore {
                override fun load() = Session("access", "refresh", System.currentTimeMillis() + 900_000)

                override fun save(session: Session?) = Unit
            }
        val connection =
            ChatConnection(
                client,
                SessionManager(store, client, server.url("/api/v1/")),
                server.url("/api/v1/"),
            )
        try {
            connection.connect()
            withTimeout(5000) {
                connection.state.first { it.messages.size == 2 && it.roomsVersion == 1L }
            }
            val previews = app.bloom.android.feature.chat.ChatPreviews()
            previews.recordHistory(connection.state.value.messages)
            assertEquals(setOf(7L, 8L), previews.messages.value.keys)
            assertFalse(connection.send("No destination"))
        } finally {
            connection.disconnect()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    private fun message(id: Int, room: Int) =
        """{"type":"new_message","payload":{"id":$id,"room_id":$room,"sender_id":"partner","content":"hello","created_at":"2026-09-29T12:00:00Z"}}"""

    @Test
    fun actionsUseEnvelopesAndReloadAuthoritativeHistoryWithoutInventingTimes() = runBlocking {
        val server = MockWebServer()
        val sockets = LinkedBlockingQueue<WebSocket>()
        val received = LinkedBlockingQueue<String>()
        server.enqueue(
            MockResponse()
                .withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            sockets.add(webSocket)
                            webSocket.send(message(1, 7))
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            received.add(text)
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, null)
                        }
                    }
                )
        )
        server.start()
        val client = OkHttpClient()
        val store =
            object : SessionStore {
                override fun load() = Session("access", "refresh", System.currentTimeMillis() + 900_000)

                override fun save(session: Session?) = Unit
            }
        val connection =
            ChatConnection(
                client,
                SessionManager(store, client, server.url("/api/v1/")),
                server.url("/api/v1/"),
                7,
            )
        try {
            connection.connect()
            withTimeout(5000) { connection.state.first { it.messages.size == 1 } }
            val peer = sockets.poll(5, TimeUnit.SECONDS)!!
            assertTrue(connection.markRead(1))
            assertEquals(
                """{"type":"mark_as_read","payload":{"room_id":7,"message_id":1}}""",
                received.poll(5, TimeUnit.SECONDS),
            )
            assertTrue(connection.edit(1, "changed"))
            assertEquals(
                """{"type":"edit_message","payload":{"room_id":7,"message_id":1,"content":"changed"}}""",
                received.poll(5, TimeUnit.SECONDS),
            )
            assertTrue(connection.delete(1))
            assertEquals(
                """{"type":"delete_message","payload":{"room_id":7,"message_id":1}}""",
                received.poll(5, TimeUnit.SECONDS),
            )
            assertTrue(connection.typing(true))
            assertEquals(
                """{"type":"typing","payload":{"room_id":7,"is_typing":true}}""",
                received.poll(5, TimeUnit.SECONDS),
            )
            peer.send("""{"type":"typing","payload":{"room_id":999,"user_id":"outsider","is_typing":true}}""")
            peer.send("""{"type":"typing","payload":{"room_id":7,"user_id":"partner","is_typing":true}}""")
            peer.send("""{"type":"presence","payload":{"room_id":7,"user_id":"partner","status":"online"}}""")
            peer.send("""{"type":"mark_as_read","payload":{"room_id":7,"message_id":1}}""")
            peer.send("""{"type":"edit_message","payload":{"room_id":7,"message_id":1,"content":"changed"}}""")
            peer.send("""{"type":"delete_message","payload":{"room_id":7,"message_id":1}}""")
            withTimeout(5000) { connection.state.first { it.historyVersion == 3L } }
            val state = connection.state.value
            assertEquals(setOf(7L), state.roomsNeedingHistory)
            assertEquals(setOf("partner"), state.typingUntil.keys)
            assertEquals(true, state.onlineUsers["partner"])
            assertNull(state.messages.single().readAt)
            val updated =
                state.messages
                    .single()
                    .copy(
                        content = "changed",
                        readAt = "2026-10-01T12:00:00Z",
                        deletedAt = "2026-10-01T12:01:00Z",
                    )
            connection.mergeHistory(listOf(updated), revision = 0)
            assertNull(connection.state.value.messages.single().deletedAt)
            connection.mergeHistory(listOf(updated), revision = 3)
            assertEquals(updated, connection.state.value.messages.single())
            assertTrue(connection.state.value.roomsNeedingHistory.isEmpty())
            peer.send("""{"type":"error","error":"room is not active"}""")
            withTimeout(5000) { connection.state.first { it.errorVersion == 1L } }
            assertTrue(connection.state.value.actionError!!.contains("room is not active"))
            assertTrue(connection.state.value.connected)
            connection.disconnect()
            assertTrue(connection.state.value.onlineUsers.isEmpty())
        } finally {
            connection.disconnect()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }
}
