package app.bloom.android

import android.content.Context
import app.bloom.android.core.network.*
import app.bloom.android.core.security.KeystoreSessionStore
import app.bloom.android.core.security.SessionManager
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppGraph(context: Context) {
    val chatPreviews = app.bloom.android.feature.chat.ChatPreviews()
    val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val baseUrl =
        ApiAddress.parse(
            if (BuildConfig.DEBUG) preferences.getString("server", BuildConfig.API_URL)!! else BuildConfig.API_URL,
            BuildConfig.DEBUG,
        )
    private val transport =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .build()
    val sessions = SessionManager(KeystoreSessionStore(context), transport, baseUrl)
    val http: OkHttpClient =
        transport
            .newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val token = sessions.session.value?.accessToken
                val publicAuth = original.url.pathSegments.last() in setOf("login", "register", "refresh")
                val request =
                    if (token != null && !publicAuth) {
                        original.newBuilder().header("Authorization", "Bearer $token").build()
                    } else original
                chain.proceed(request)
            }
            .authenticator(sessions)
            .build()
    private val retrofit =
        Retrofit.Builder().baseUrl(baseUrl).client(http).addConverterFactory(GsonConverterFactory.create()).build()
    val identity: IdentityApi = retrofit.create(IdentityApi::class.java)
    val users: UsersApi = retrofit.create(UsersApi::class.java)
    val interactions: InteractionsApi = retrofit.create(InteractionsApi::class.java)
    val chat: ChatApi = retrofit.create(ChatApi::class.java)
    val discovery: DiscoveryApi = retrofit.create(DiscoveryApi::class.java)
}
