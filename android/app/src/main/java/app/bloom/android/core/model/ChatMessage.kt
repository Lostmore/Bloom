package app.bloom.android.core.model

data class ChatMessage(
    val id: Long,
    @com.google.gson.annotations.SerializedName("room_id") val roomId: Long,
    @com.google.gson.annotations.SerializedName("sender_id") val senderId: String,
    val content: String,
    @com.google.gson.annotations.SerializedName("created_at") val createdAt: String,
    val attachments: List<Attachment>? = null,
)
