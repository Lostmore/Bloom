package app.bloom.android

import android.app.Application
import app.bloom.android.core.network.*

class BloomApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }

    fun changeServer(url: String) {
        check(BuildConfig.DEBUG)
        val parsed = ApiAddress.parse(url, true)
        graph.sessions.clear()
        graph.http.dispatcher.cancelAll()
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString("server", parsed.toString()).apply()
        graph = AppGraph(this)
    }
}
