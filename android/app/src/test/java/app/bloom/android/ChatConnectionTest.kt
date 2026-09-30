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
            assertEquals("{\"content\":\"Привет\",\"room_id\":7}", received.poll(5, TimeUnit.SECONDS))
            withTimeout(5000) { connection.state.first { it.messages.size == 2 } }
            assertEquals(listOf(1L, 3L), connection.state.value.messages.map { it.id })
            connection.mergeHistory(
                listOf(
                    app.bloom.android.core.model.ChatMessage(1, 7, "partner", "old snapshot", "2026-09-29T12:00:00Z")
                )
            )
            assertEquals(listOf(1L, 3L), connection.state.value.messages.map { it.id })
            assertEquals("hello", connection.state.value.messages.first().content)
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
            ChatConnection(client, SessionManager(store, client, server.url("/api/v1/")), server.url("/api/v1/"))
        try {
            connection.connect()
            withTimeout(5000) { connection.state.first { it.messages.size == 2 && it.roomsVersion == 1L } }
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
        """{"id":$id,"room_id":$room,"sender_id":"partner","content":"hello","created_at":"2026-09-29T12:00:00Z"}"""
}
