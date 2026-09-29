package app.bloom.android.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Session(val accessToken: String, val refreshToken: String, val expiresAt: Long)

interface SessionStore {
    fun load(): Session?

    fun save(session: Session?)
}

/** Preferences contain only AES-GCM ciphertext. The key never leaves Android Keystore. */
@SuppressLint("ApplySharedPref", "UseKtx") // Token rotation must durably save before releasing waiting requests.
class KeystoreSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("bloom_session", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alias = "bloom.session.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    override fun load(): Session? {
        val encrypted = preferences.getString("value", null) ?: return null
        return try {
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            gson.fromJson(
                String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8),
                Session::class.java,
            )
        } catch (_: Exception) {
            preferences.edit().clear().commit()
            null
        }
    }

    override fun save(session: Session?) {
        if (session == null) {
            check(preferences.edit().clear().commit())
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("value", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit())
    }
}
