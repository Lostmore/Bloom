package app.bloom.android.feature.profile

import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import java.security.MessageDigest
import org.json.JSONObject

class OnboardingStore(private val preferences: SharedPreferences, server: String, accessToken: String) {
    // Subject scopes local drafts only; authorization remains entirely on the server.
    private val subject =
        JSONObject(String(Base64.decode(accessToken.split('.')[1], Base64.URL_SAFE), Charsets.UTF_8)).getString("sub")
    private val key =
        "onboarding." +
            MessageDigest.getInstance("SHA-256").digest("$server|$subject".toByteArray()).joinToString("") {
                "%02x".format(it)
            }
    private val gson = Gson()

    fun pending() = preferences.contains(key)

    fun read(): OnboardingDraft =
        runCatching { gson.fromJson(preferences.getString(key, null), OnboardingDraft::class.java) }.getOrNull()
            ?: OnboardingDraft()

    fun save(draft: OnboardingDraft) {
        preferences.edit().putString(key, gson.toJson(draft)).apply()
    }

    fun photos(): List<String> = runCatching {
        gson.fromJson(preferences.getString("$key.photos", "[]"), Array<String>::class.java).toList()
    }
        .getOrDefault(emptyList())

    fun savePhotos(photos: List<String>) {
        preferences.edit().putString("$key.photos", gson.toJson(photos)).apply()
    }

    fun complete(draft: OnboardingDraft) {
        preferences.edit().remove(key).putString("$key.photos", gson.toJson(draft.photos)).apply()
    }
}
