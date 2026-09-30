package app.bloom.android.feature.chat

import app.bloom.android.core.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Only messages actually received in this session; no invented read counts or presence. */
class ChatPreviews {
    private val mutable = MutableStateFlow<Map<Long, ChatMessage>>(emptyMap())
    val messages = mutable.asStateFlow()

    fun record(message: ChatMessage) {
        mutable.update { current ->
            if ((current[message.roomId]?.id ?: 0) >= message.id) current else current + (message.roomId to message)
        }
    }

    fun clear() {
        mutable.value = emptyMap()
    }
}
