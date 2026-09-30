package app.bloom.android

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.bloom.android.core.model.Attachment
import app.bloom.android.core.model.MediaCapabilities
import app.bloom.android.core.network.ChatApi
import app.bloom.android.feature.chat.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatMediaContractTest {
    @Test
    fun retryAcknowledgesAnAlreadySavedMessageByClientKey() {
        val message =
            app.bloom.android.core.model.ChatMessage(
                42,
                7,
                "me",
                "hello",
                "2026-09-30T10:00:00Z",
                clientMessageId = "same-key",
            )
        assertTrue(confirmsSend(message, "me", "same-key", "hello", true, 42))
        assertFalse(confirmsSend(message, "other", "same-key", "hello", true, 42))
        assertFalse(confirmsSend(message.copy(clientMessageId = null), "me", "same-key", "hello", false, 42))
    }

    @Test
    fun emojiReplacesSelectionWithoutMovingToEnd() {
        val inserted = insertEmoji(TextFieldValue("hello there", TextRange(0, 5)), "😊")
        assertEquals("😊 there", inserted.text)
        assertEquals(TextRange(2), inserted.selection)
        assertEquals(4000, insertEmoji(TextFieldValue("x".repeat(4000), TextRange(4000)), "😊").text.length)
    }

    @Test
    fun mediaNeverUsesAnArbitraryRemoteUrlAndUploadIsOptIn() {
        val id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        assertEquals(id, mediaId(Attachment(url = "https://outside.example/media/$id", mediaType = "image/jpeg")))
        assertNull(mediaId(Attachment(url = "https://outside.example/photo.jpg", mediaType = "image/jpeg")))
        assertNull(mediaId(Attachment(mediaId = "../../secret", mediaType = "image/jpeg")))
        assertFalse(Gson().fromJson("{}", MediaCapabilities::class.java).privateChatAttachments)
        try {
            byteArrayOf(1, 2, 3).inputStream().readImageBytes(2)
            fail("Oversize body must be rejected")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun searchAndHistoryUseGatewayRoutesAndCurrentGoArrayResponse() = runBlocking {
        MockWebServer().use { server ->
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/api/v1/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ChatApi::class.java)
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"items":[],"nextCursor":"next/+="}""")
            )
            assertEquals("next/+=", api.search("кофе", "cursor/+=").nextCursor)
            val request = server.takeRequest()
            assertEquals("/api/v1/conversations/search", request.requestUrl!!.encodedPath)
            assertEquals("кофе", request.requestUrl!!.queryParameter("q"))
            assertEquals("cursor/+=", request.requestUrl!!.queryParameter("cursor"))
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """[{"id":19,"room_id":7,"sender_id":"partner","content":"hello","created_at":"2026-09-30T10:00:00Z"}]"""
                    )
            )
            assertEquals(19L, api.history(7)!!.single().id)
            val context = server.takeRequest()
            assertEquals("/api/v1/conversations/7/messages", context.requestUrl!!.encodedPath)
            assertNull(context.requestUrl!!.queryParameter("around"))
        }
    }
}
