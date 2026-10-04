package app.bloom.android.core.security

import android.util.Base64
import app.bloom.android.core.model.Tokens
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class SessionManager(
    private val store: SessionStore,
    private val refreshClient: OkHttpClient,
    private val baseUrl: HttpUrl,
) : Authenticator {
    private val json = Gson()
    private val mutable = MutableStateFlow(store.load())
    val session = mutable.asStateFlow()

    // UI routing only. Access authorization is always checked by the server.
    fun onboardingRequired(): Boolean = tokenStatus() == "ONBOARDING"

    fun tokenStatus(): String? = runCatching {
        val encoded = session.value?.accessToken?.split('.')?.getOrNull(1) ?: return null
        val payload = Base64.decode(encoded, Base64.URL_SAFE)
        json.fromJson(String(payload, Charsets.UTF_8), JsonObject::class.java).get("status")?.asString
    }.getOrNull()

    suspend fun awaitProfileActivation() {
        repeat(15) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                freshToken(session.value?.accessToken)
            }
            if (tokenStatus() == "ACTIVE") return
            kotlinx.coroutines.delay(1000)
        }
        throw ProfileActivationPending()
    }

    @Synchronized
    fun accept(tokens: Tokens) {
        require(tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank() && tokens.expiresIn > 0)
        val next =
            Session(
                tokens.accessToken,
                tokens.refreshToken,
                System.currentTimeMillis() + tokens.expiresIn * 1000,
            )
        store.save(next)
        mutable.value = next
    }

    @Synchronized
    fun clear() {
        mutable.value = null
        store.save(null)
    }

    @Synchronized
    fun freshToken(forceFor: String? = null): String? {
        val current = mutable.value ?: return null
        if (forceFor != null && current.accessToken != forceFor) return current.accessToken
        if (forceFor == null && current.expiresAt > System.currentTimeMillis() + 60_000) return current.accessToken
        val body =
            json.toJson(mapOf("refreshToken" to current.refreshToken)).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl.resolve("auth/refresh")!!).post(body).build()
        refreshClient.newCall(request).execute().use { response ->
            if (response.code == 400 || response.code == 401) {
                clear()
                return null
            }
            if (!response.isSuccessful) throw IOException("Refresh temporarily unavailable")
            try {
                val tokens =
                    json.fromJson(response.body?.string(), Tokens::class.java)
                        ?: throw IOException("Invalid refresh response")
                accept(tokens)
                return tokens.accessToken
            } catch (error: RuntimeException) {
                throw IOException("Invalid refresh response", error)
            }
        }
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        if (response.request.url.pathSegments.last() in setOf("login", "register", "refresh")) return null
        val supplied = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        return try {
            val refreshed = freshToken(supplied) ?: return null
            response.request.newBuilder().header("Authorization", "Bearer $refreshed").build()
        } catch (_: IOException) {
            null
        }
    }
}

class ProfileActivationPending : Exception()
