package app.bloom.android

import app.bloom.android.core.model.ChatMessage
import app.bloom.android.feature.chat.ChatPreviews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPreviewsTest {
    @Test
    fun replayedHistoryCannotReplaceLastMessageAndLogoutRemovesIt() {
        val previews = ChatPreviews()
        previews.record(ChatMessage(10, 7, "sender", "latest", "2026-09-30T10:00:00Z"))
        previews.record(ChatMessage(9, 7, "sender", "older", "2026-09-30T09:00:00Z"))
        assertEquals("latest", previews.messages.value[7]?.content)
        previews.clear()
        assertTrue(previews.messages.value.isEmpty())
    }
}
