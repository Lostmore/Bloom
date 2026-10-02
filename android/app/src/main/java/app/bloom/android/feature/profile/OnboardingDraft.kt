package app.bloom.android.feature.profile

import java.time.LocalDate

data class OnboardingDraft(
    val step: Int = 0,
    val name: String = "",
    val birthday: String = "",
    val gender: String = "",
    val goals: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val bio: String = "",
    val city: String = "",
    val identitySaved: Boolean = false,
) {
    fun withSavedIdentity(profile: app.bloom.android.core.model.Profile) =
        copy(
            birthday = profile.birthDate ?: birthday,
            gender = profile.gender,
            identitySaved = true,
        )

    fun error(): String? =
        when (step) {
            0 -> if (name.isBlank()) "Напиши своё имя." else null
            1 -> {
                val date = runCatching { LocalDate.parse(birthday) }.getOrNull()
                if (
                    date == null ||
                        date.isAfter(LocalDate.now().minusYears(18)) ||
                        date.isBefore(LocalDate.now().minusYears(120))
                )
                    "Bloom доступен с 18 лет. Проверь дату рождения."
                else null
            }
            2 -> if (gender.isEmpty()) "Выбери вариант." else null
            3 -> if (goals.isEmpty()) "Выбери хотя бы одну цель." else null
            else -> null
        }
}
