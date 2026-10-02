package app.bloom.android.feature.chat

import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.UsersApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class ChatPartner(val profile: Profile? = null, val unavailable: Boolean = false) {
    val name: String
        get() = profile?.nickname ?: "Собеседник"

    val photoId: String?
        get() =
            profile?.photos.orEmpty().firstOrNull()?.takeIf {
                runCatching { java.util.UUID.fromString(it).toString() == it.lowercase() }.getOrDefault(false)
            }
}

// Always revalidate with Users; never retain a previous profile after access is denied.
suspend fun loadChatPartner(users: UsersApi, id: String): ChatPartner =
    try {
        val profile = users.profile(id)
        require(profile.id.equals(id, ignoreCase = true) && !profile.nickname.isNullOrBlank())
        ChatPartner(profile)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        ChatPartner(unavailable = exception is HttpException && exception.code() in setOf(403, 404, 410))
    }
