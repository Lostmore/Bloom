package app.bloom.android.core.model

data class Match(
    val id: String,
    val userA: String,
    val userB: String,
    val status: String,
    val createdAt: String,
) {
    fun partner(user: String) = if (userA == user) userB else userA
}
