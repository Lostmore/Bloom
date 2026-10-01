package app.bloom.android.core.model

data class ChatMessage(
    val id: Long,
    @com.google.gson.annotations.SerializedName("room_id") val roomId: Long,
    @com.google.gson.annotations.SerializedName("sender_id") val senderId: String,
    val content: String,
    @com.google.gson.annotations.SerializedName("created_at") val createdAt: String,
    val attachments: List<Attachment>? = null,
    @com.google.gson.annotations.SerializedName("client_message_id") val clientMessageId: String? = null,
    @com.google.gson.annotations.SerializedName("read_at") val readAt: String? = null,
    @com.google.gson.annotations.SerializedName("edited_at") val editedAt: String? = null,
    @com.google.gson.annotations.SerializedName("deleted_at") val deletedAt: String? = null,
)
