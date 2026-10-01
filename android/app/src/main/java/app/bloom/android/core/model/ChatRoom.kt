package app.bloom.android.core.model

data class ChatRoom(
    val id: Long,
    @com.google.gson.annotations.SerializedName("user1_id") val user1Id: String,
    @com.google.gson.annotations.SerializedName("user2_id") val user2Id: String,
    @com.google.gson.annotations.SerializedName("is_active") val active: Boolean,
    @com.google.gson.annotations.SerializedName("last_message") val lastMessage: String? = null,
    @com.google.gson.annotations.SerializedName("last_message_at") val lastMessageAt: String? = null,
    @com.google.gson.annotations.SerializedName("created_at") val createdAt: String? = null,
) {
    fun partnerOrNull(user: String): String? =
        when {
            user.isBlank() -> null
            user1Id.equals(user, ignoreCase = true) -> user2Id
            user2Id.equals(user, ignoreCase = true) -> user1Id
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals(user, ignoreCase = true) }

    fun partner(user: String): String = requireNotNull(partnerOrNull(user)) { "User is not a room participant" }
}
