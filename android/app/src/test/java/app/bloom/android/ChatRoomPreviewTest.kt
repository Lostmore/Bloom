package app.bloom.android

import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.model.ChatRoom
import app.bloom.android.feature.chat.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ChatRoomPreviewTest {
    @Test
    fun freshServerSummaryWinsOverOldLocalMessages() {
        val room =
            Gson()
                .fromJson(
                    """{"id":7,"user1_id":"me","user2_id":"other","is_active":true,"last_message":"new","last_message_at":"2026-09-30T12:00:00Z"}""",
                    ChatRoom::class.java,
                )
        val old = ChatMessage(9, 7, "me", "old", "2026-09-30T11:00:00Z")
        assertEquals("new", chatRoomPreview(room, old, "me", "Name").preview)
        assertEquals("new", chatRoomPreview(room, null, "me", "Name").preview)
        assertEquals(
            "Вы: live",
            chatRoomPreview(room, old.copy(content = "live", createdAt = "2026-09-30T13:00:00Z"), "me", "Name").preview,
        )
    }

    @Test
    fun roomsSortByActualMessageTimeAndEmptyRoomsRemainUsable() {
        val empty = ChatRoom(8, "me", "other", true)
        assertNull(chatRoomPreview(empty, null, "me", "Name").preview)
        val a =
            chatRoomPreview(
                empty.copy(id = 1, lastMessage = "later", lastMessageAt = "2026-09-30T13:00:00Z"),
                null,
                "me",
                "A",
            )
        val b =
            chatRoomPreview(
                empty.copy(id = 2, lastMessage = "earlier", lastMessageAt = "2026-09-30T11:00:00Z"),
                null,
                "me",
                "B",
            )
        assertEquals(listOf(1L, 2L), listOf(b, a).sortedByDescending { chatTimestamp(it.timestamp) }.map { it.id })
    }
}
