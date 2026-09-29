package app.bloom.android.core.network

import java.io.IOException
import retrofit2.HttpException

fun Throwable.userMessage(): String =
    when (this) {
        is HttpException ->
            when (code()) {
                400 -> "Проверьте заполненные поля."
                401 -> "Сессия истекла или данные для входа неверны. Войдите снова."
                403,
                404 -> "Сейчас это недоступно. Возможно, профиль скрыт или общение завершено."
                409 -> "Данные изменились. Обновите экран и попробуйте ещё раз."
                429 -> "Слишком много запросов. Попробуйте немного позже."
                else -> "Не удалось связаться с Bloom. Попробуйте ещё раз."
            }
        is IOException -> "Нет соединения с Bloom. Проверьте интернет и попробуйте снова."
        is IllegalArgumentException -> message ?: "Проверьте введённые данные."
        else -> "Что-то не получилось. Попробуйте ещё раз."
    }
