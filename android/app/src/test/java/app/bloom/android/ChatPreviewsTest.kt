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
        val edited = ChatMessage(10, 7, "sender", "edited", "2026-09-30T10:00:00Z", editedAt = "2026-09-30T10:01:00Z")
        previews.recordHistory(listOf(edited))
        assertEquals("edited", previews.messages.value[7]?.content)
        previews.recordHistory(listOf(edited.copy(deletedAt = "2026-09-30T10:02:00Z")))
        assertEquals("2026-09-30T10:02:00Z", previews.messages.value[7]?.deletedAt)
        previews.clear()
        assertTrue(previews.messages.value.isEmpty())
    }

    @Test
    fun rawPresenceCannotOverrideHiddenActivity() {
        val hidden = app.bloom.android.core.model.Profile("partner", "Anna")
        assertEquals("Активность скрыта", app.bloom.android.feature.chat.chatActivityLabel(hidden, true))
        val visible = hidden.copy(lastSeen = "2026-10-01T10:00:00Z")
        assertEquals("В сети", app.bloom.android.feature.chat.chatActivityLabel(visible, true))
    }
}
