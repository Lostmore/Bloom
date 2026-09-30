package app.bloom.android

import app.bloom.android.core.model.Profile
import app.bloom.android.feature.chat.*
import org.junit.Assert.*
import org.junit.Test

class ConversationDetailsTest {
    @Test
    fun hiddenActivityDoesNotInventAnExactTime() {
        val user = Profile("id", "Name")
        assertEquals("Активность скрыта", activityLabel(user))
        assertEquals("Был(а) на этой неделе", activityLabel(user.copy(activityStatus = "WITHIN_WEEK")))
        assertEquals("Активность скрыта", activityLabel(user.copy(lastSeen = "invalid")))
        assertEquals("В сети", activityLabel(user.copy(online = true)))
    }

    @Test
    fun onlyKnownCompleteStickerEnvelopesRenderAsStickers() {
        assertEquals(12, BloomStickers.size)
        for (sticker in BloomStickers) assertEquals(sticker, bloomSticker(sticker.wire))
        assertNull(bloomSticker("[bloom-sticker:v1:unknown]"))
        assertNull(bloomSticker("hello " + BloomStickers.first().wire))
        assertNull(bloomSticker("https://untrusted.example/sticker"))
    }
}
