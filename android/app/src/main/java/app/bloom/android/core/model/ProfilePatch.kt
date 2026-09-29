package app.bloom.android.core.model

data class ProfilePatch(
    val version: Long,
    val nickname: String,
    val bio: String,
    val city: String,
    val searchModes: Set<String>,
)
