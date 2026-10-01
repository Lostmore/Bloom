package app.bloom.android.feature.discovery

import app.bloom.android.core.model.Profile

/** Local fictional cards. Their IDs must never be sent to backend APIs. */
fun demoProfile(index: Int, mode: String?): Profile {
    val names = listOf("Аня", "Марк", "Соня", "Артём", "Лера", "Данил", "Маша", "Никита")
    val cities = listOf("Ижевск", "Казань", "Москва", "Санкт-Петербург")
    val bios =
        listOf(
            "Ищу компанию для прогулок, кофе и спонтанных поездок.",
            "Люблю живую музыку, уютные места и разговоры до ночи.",
            "В выходные выбираюсь на природу. Покажешь свой любимый маршрут?",
            "Можно начать с мемов, а потом сходить на выставку вместе.",
        )
    val position = Math.floorMod(index, 120)
    return Profile(
        id = "demo-$position",
        nickname = names[position % names.size],
        age = 21 + position % 12,
        city = cities[(position / 2) % cities.size],
        bio = bios[position % bios.size],
        searchModes = setOf(mode ?: listOf("DATING", "FRIENDS", "ACTIVITIES", "ANY")[position % 4]),
    )
}
