package app.bloom.android.core.model

data class Interest(val id: String, val name: String, val icon: String, val category: String) {
    val displayName: String
        get() =
            when (id) {
                "animals" -> "Животные"
                "art" -> "Искусство"
                "books" -> "Книги"
                "coffee" -> "Кофе"
                "concerts" -> "Концерты"
                "cooking" -> "Кулинария"
                "fitness" -> "Фитнес"
                "food" -> "Еда"
                "gaming" -> "Видеоигры"
                "movies" -> "Кино"
                "music" -> "Музыка"
                "nature" -> "Природа"
                "nightlife" -> "Ночная жизнь"
                "photography" -> "Фотография"
                "sport" -> "Спорт"
                "technology" -> "Технологии"
                "travel" -> "Путешествия"
                "walking" -> "Прогулки"
                else -> name
            }
}
