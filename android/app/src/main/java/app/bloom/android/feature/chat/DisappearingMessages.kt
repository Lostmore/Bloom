package app.bloom.android.feature.chat

import androidx.compose.runtime.*
import app.bloom.android.core.model.ChatMessage
import kotlinx.coroutines.delay

/** Keep the original content briefly only for a deletion observed on this screen. */
@Composable
internal fun rememberDisappearingMessages(messages: List<ChatMessage>, roomId: Long): List<ChatMessage> {
    var visible by remember(roomId) { mutableStateOf(messages.filter { it.deletedAt == null }) }
    LaunchedEffect(messages) {
        visible = messagesForDeletionAnimation(visible, messages)
    }
    val deleting = visible.filter { it.deletedAt != null }.map { it.id }.toSet()
    LaunchedEffect(deleting) {
        if (deleting.isNotEmpty()) {
            delay(700)
            visible = visible.filterNot { it.id in deleting }
        }
    }
    return visible
}

internal fun messagesForDeletionAnimation(previous: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
    val known = previous.associateBy { it.id }
    return incoming.mapNotNull { message ->
        if (message.deletedAt == null) message else known[message.id]?.copy(deletedAt = message.deletedAt)
    }
}
