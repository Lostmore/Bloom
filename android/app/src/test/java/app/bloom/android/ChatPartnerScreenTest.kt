package app.bloom.android

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.bloom.android.core.ui.BloomTheme
import app.bloom.android.feature.chat.ChatListScreen
import app.bloom.android.feature.chat.ChatPartnerAvatar
import app.bloom.android.feature.chat.ChatRoomScreen
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatPartnerScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun graph(server: MockWebServer): AppGraph {
        compose.activity
            .getSharedPreferences("settings", 0)
            .edit()
            .putString("server", server.url("/api/v1/").toString())
            .commit()
        return AppGraph(compose.activity)
    }

    @Test
    fun roomRecoversFromFirstProfileFailureWithoutReopening() {
        MockWebServer().use { server ->
            val available = AtomicBoolean(false)
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        when (request.path) {
                            "/api/v1/conversations" ->
                                MockResponse().setBody("""[{"id":7,"user1_id":"me","user2_id":"a","is_active":true}]""")
                            "/api/v1/users/a" ->
                                if (available.get())
                                    MockResponse().setBody("""{"id":"a","nickname":"Anna","photos":[]}""")
                                else MockResponse().setResponseCode(503)
                            "/api/v1/interests",
                            "/api/v1/conversations/7/messages" -> MockResponse().setBody("[]")
                            else -> MockResponse().setResponseCode(404)
                        }
                }
            val graph = graph(server)
            compose.setContent { BloomTheme("light") { Surface { ChatRoomScreen(graph, 7, "me", back = {}) } } }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Имя не загрузилось").fetchSemanticsNodes().isNotEmpty()
            }
            available.set(true)
            compose.onNodeWithText("Не удалось загрузить собеседника · Повторить").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Anna").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Имя не загрузилось").assertDoesNotExist()
        }
    }

    @Test
    fun listShowsBothParticipantsAndRefreshesNamesAndAccess() {
        MockWebServer().use { server ->
            val refreshed = AtomicBoolean(false)
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        when (request.path) {
                            "/api/v1/conversations" ->
                                MockResponse()
                                    .setBody(
                                        """[
                        {"id":7,"user1_id":"me","user2_id":"a","is_active":true},
                        {"id":8,"user1_id":"b","user2_id":"me","is_active":true},
                        {"id":9,"user1_id":"other","user2_id":"stranger","is_active":true}]
                    """
                                    )
                            "/api/v1/users/a" ->
                                MockResponse()
                                    .setBodyDelay(150, TimeUnit.MILLISECONDS)
                                    .setBody(
                                        """{"id":"a","nickname":"${if (refreshed.get()) "Updated" else "Anna"}","photos":[]}"""
                                    )
                            "/api/v1/users/b" ->
                                if (refreshed.get()) MockResponse().setResponseCode(404)
                                else MockResponse().setBody("""{"id":"b","nickname":"Bob","photos":[]}""")
                            else -> MockResponse().setResponseCode(404)
                        }
                }
            val graph = graph(server)
            compose.setContent { BloomTheme("light") { Surface { ChatListScreen(graph, "me", {}) } } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bob").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Anna").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Bob").assertExists()
            refreshed.set(true)
            compose.onNodeWithTag("chat-refresh").performTouchInput { swipeDown() }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Updated").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Профиль недоступен").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Bob").assertDoesNotExist()
        }
    }

    @Test
    fun avatarLoadsProfilePhotoFromMedia() {
        MockWebServer().use { server ->
            val photo = "8cc89777-f073-419b-8d05-d99c2576297a"
            val bitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.BLUE)
            val bytes =
                java.io.ByteArrayOutputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    it.toByteArray()
                }
            bitmap.recycle()
            server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody(okio.Buffer().write(bytes)))
            val graph = graph(server)
            compose.setContent { BloomTheme("light") { ChatPartnerAvatar(graph, "Anna", photo) } }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Фото").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("/api/v1/media/$photo", server.takeRequest(5, TimeUnit.SECONDS)?.path)
        }
    }
}
