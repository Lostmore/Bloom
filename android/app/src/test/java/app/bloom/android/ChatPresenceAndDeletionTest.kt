package app.bloom.android

import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.model.Profile
import app.bloom.android.feature.chat.chatPartnerOnline
import app.bloom.android.feature.chat.messagesForDeletionAnimation
import org.junit.Assert.*
import org.junit.Test

class ChatPresenceAndDeletionTest {
    @Test
    fun hiddenPresenceIsVisibleOnlyWhileTypingAndDeletionDoesNotReappear() {
        val hidden = Profile("partner", "Анна", activityStatus = "RECENTLY")
        assertFalse(chatPartnerOnline(hidden, true, false))
        assertTrue(chatPartnerOnline(hidden, true, true))
        assertFalse(chatPartnerOnline(hidden, true, false))
        val visible = hidden.copy(lastSeen = "2026-10-04T00:00:00Z")
        assertTrue(chatPartnerOnline(visible, true, false))
        assertFalse(chatPartnerOnline(visible, false, false))

        val message =
            ChatMessage(
                id = 1,
                roomId = 2,
                senderId = "partner",
                content = "Привет",
                createdAt = "2026-10-04T00:00:00Z",
            )
        val removed = message.copy(content = "", deletedAt = "2026-10-04T00:01:00Z")
        val frame = messagesForDeletionAnimation(listOf(message), listOf(removed)).single()
        assertEquals("Привет", frame.content)
        assertNotNull(frame.deletedAt)
        assertTrue(messagesForDeletionAnimation(emptyList(), listOf(removed)).isEmpty())
        assertEquals(listOf(message), messagesForDeletionAnimation(emptyList(), listOf(message)))
    }
}
