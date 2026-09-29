package app.bloom.android.core.model

data class CreateProfile(
    val nickname: String,
    val birthDate: String,
    val gender: String,
    val searchModes: Set<String>,
)
