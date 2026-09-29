package app.bloom.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bloom.android.core.ui.BloomApp
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var profileLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readLink(intent)
        setContent {
            val application = application as BloomApplication
            BloomApp(application.graph, profileLink, { profileLink = null }) { url ->
                application.changeServer(url)
                recreate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readLink(intent)
    }

    private fun readLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "bloom" && uri.host == "profile") {
            profileLink = runCatching { UUID.fromString(uri.lastPathSegment).toString() }.getOrNull()
        }
    }
}
