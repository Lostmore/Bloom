package app.bloom.android.feature.chat

import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.model.ChatRoom
import java.time.Instant

fun chatTimestamp(value: String?): Instant = runCatching {
    Instant.parse(value)
}
    .getOrDefault(Instant.EPOCH)

fun chatRoomPreview(room: ChatRoom, live: ChatMessage?, myId: String, name: String): ChatRowItem {
    val useLive =
        live != null &&
            (room.lastMessageAt == null || chatTimestamp(live.createdAt) >= chatTimestamp(room.lastMessageAt))
    val content =
        if (useLive) {
            if (live!!.deletedAt != null) "Сообщение удалено" else live.content
        } else room.lastMessage
    val timestamp = if (useLive) live!!.createdAt else room.lastMessageAt
    val preview =
        content?.let {
            (if (useLive && live!!.senderId == myId) "Вы: " else "") +
                (bloomSticker(it)?.let { sticker -> "Стикер · ${sticker.caption}" }
                    ?: it.takeIf(String::isNotBlank)
                    ?: "Вложение")
        } ?: if (timestamp != null) "Вложение" else null
    return ChatRowItem(room.id, name, preview, timestamp ?: room.createdAt)
}
