package app.bloom.android.core.model

data class MessagePage(val items: List<ChatMessage> = emptyList(), val nextCursor: String? = null)

data class ChatCapabilities(val attachmentMessages: Boolean = false, val readReceipts: Boolean = false)
