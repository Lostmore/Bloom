package app.bloom.android.feature.profile

import app.bloom.android.core.model.*
import app.bloom.android.core.network.UsersApi
import retrofit2.HttpException

suspend fun saveOnboarding(users: UsersApi, draft: OnboardingDraft, profileReady: (Profile) -> Unit = {}): Profile {
    val current =
        try {
            users.me()
        } catch (exception: HttpException) {
            if (exception.code() != 404) throw exception
            try {
                users.create(
                    CreateProfile(
                        draft.name.trim(),
                        draft.birthday,
                        draft.gender,
                        draft.goals.toSet(),
                    )
                )
            } catch (conflict: HttpException) {
                if (conflict.code() == 409) users.me() else throw conflict
            }
        }
    profileReady(current)
    users.update(
        ProfilePatch(
            current.version,
            draft.name.trim(),
            draft.bio.trim(),
            draft.city.trim(),
            draft.goals.toSet(),
        )
    )
    if (draft.interests.isNotEmpty() || !current.interests.isNullOrEmpty()) {
        users.interests(mapOf("interests" to draft.interests))
    }
    return users.me()
}
