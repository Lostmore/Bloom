package app.bloom.android.core.model

data class Profile(
    val id: String,
    val nickname: String,
    val birthDate: String? = null,
    val age: Int? = null,
    val gender: String = "OTHER",
    val bio: String? = null,
    val city: String? = null,
    val searchModes: Set<String> = emptySet(),
    val interests: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val version: Long = 0,
    val verified: Boolean = false,
    val online: Boolean = false,
    val distanceKm: Int? = null,
) {
    fun displayedAge(): Int? =
        age
            ?: birthDate?.let {
                runCatching {
                    java.time.Period.between(
                            java.time.LocalDate.parse(it),
                            java.time.LocalDate.now(),
                        )
                        .years
                }
                    .getOrNull()
            }
}
