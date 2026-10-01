package app.bloom.android

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.bloom.android.core.ui.BloomTheme
import app.bloom.android.feature.discovery.DiscoveryScreen
import app.bloom.android.feature.discovery.demoProfile
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DiscoveryFeedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun demoCyclesAndHonorsSelectedGoal() {
        assertEquals(demoProfile(0, null), demoProfile(120, null))
        for (i in 0..1000) {
            val profile = demoProfile(i, "FRIENDS")
            assertTrue(profile.id.startsWith("demo-"))
            assertEquals(setOf("FRIENDS"), profile.searchModes)
            assertTrue(profile.age!! >= 18)
            assertTrue(profile.nickname.isNotBlank())
        }
    }

    @Test
    fun realFeedPaginatesWhileDemoNeverSendsReactionsOrOpensBackendProfiles() {
        MockWebServer().use { server ->
            val requests = CopyOnWriteArrayList<String>()
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        requests += "${request.method} ${request.path}"
                        return when (request.path) {
                            "/api/v1/users/feed" ->
                                MockResponse()
                                    .setBody(
                                        """{"items":[{"id":"real-a","nickname":"Real A","age":24,"searchModes":["DATING"]}],"nextCursor":"next"}"""
                                    )
                            "/api/v1/users/feed?cursor=next" ->
                                MockResponse()
                                    .setBody(
                                        """{"items":[{"id":"real-b","nickname":"Real B","age":25,"searchModes":["FRIENDS"]}],"nextCursor":null}"""
                                    )
                            "/api/v1/interactions/real-a/skip" -> MockResponse().setBody("""{"matchId":null}""")
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            compose.activity
                .getSharedPreferences("settings", 0)
                .edit()
                .putString("server", server.url("/api/v1/").toString())
                .commit()
            val graph = AppGraph(compose.activity)
            val opened = mutableListOf<String>()
            compose.setContent { BloomTheme("light") { Surface { DiscoveryScreen(graph, { opened += it }) } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Real A, 24").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Пропустить").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Real B, 25").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Тестовые анкеты").performClick()
            compose.onNodeWithText("Аня, 21").assertExists()
            compose.onNodeWithText("Аня, 21").performClick()
            compose.onNodeWithText("Понятно").performClick()
            compose.onNodeWithContentDescription("Нравится").performClick()
            compose.onNodeWithText("Марк, 22").assertExists()
            compose.onNodeWithContentDescription("Особый интерес").performClick()
            compose.onNodeWithContentDescription("Пропустить").performClick()
            assertTrue(opened.isEmpty())
            assertEquals(listOf("POST /api/v1/interactions/real-a/skip"), requests.filter { it.startsWith("POST") })
            assertTrue(requests.none { it.contains("demo-") || it.contains("discovery/") })
            compose.onNodeWithContentDescription("Показать реальные анкеты").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Real A, 24").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
