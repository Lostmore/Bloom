package app.bloom.android

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.*
import app.bloom.android.feature.chat.ChatRowItem
import app.bloom.android.feature.chat.ChatsContent
import app.bloom.android.feature.profile.ProfileOverview
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainScreensDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun darkChatsSupportSearchRefreshAndNavigation() = chats("dark")

    @Test fun lightChatsSupportSearchRefreshAndNavigation() = chats("light")

    @Test fun lightProfileOffersEditAndPrivacy() = profile("light")

    @Test fun darkProfileOffersEditAndPrivacy() = profile("dark")

    private fun chats(theme: String) {
        var refreshed = 0
        var opened = 0L
        var destination = "chats"
        // Design-only fixtures. Production reads actual rooms and received messages.
        val people =
            listOf(
                ChatRowItem(1, "Анна", "Давай в субботу за кофе? ☕", "2026-09-30T09:18:00Z"),
                ChatRowItem(2, "Марк", "Вы: Отлично, тогда до встречи!", "2026-09-30T08:42:00Z"),
                ChatRowItem(3, "София", "Тоже люблю эти маленькие поездки", "2026-09-30T07:30:00Z"),
                ChatRowItem(4, "Александр", "А какой твой любимый маршрут?", "2026-09-29T17:03:00Z"),
                ChatRowItem(5, "Вика", "Привет! Рада знакомству 🌸", "2026-09-29T12:40:00Z"),
            )
        screen(theme, "chats", { destination = it }) {
            ChatsContent(people, false, null, { refreshed++ }, { opened = it })
        }
        compose.onNodeWithText("Анна").assertIsDisplayed()
        compose.onNodeWithContentDescription("Обновить чаты").assertDoesNotExist()
        capture("chats-$theme")
        compose.onNodeWithTag("chat-search").performTextInput("София")
        compose.onNodeWithText("Анна").assertDoesNotExist()
        compose.onNode(hasText("София") and !hasTestTag("chat-search")).performClick()
        assertEquals(3L, opened)
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.onNodeWithTag("chat-list").performTouchInput {
            swipeDown(startY = 20f, endY = height * 0.8f, durationMillis = 600)
        }
        compose.waitForIdle()
        assertTrue("Pull-down must refresh conversations", refreshed > 0)
        compose.onNodeWithContentDescription("Обзор").performClick()
        assertEquals("explore", destination)
        listOf("Люди", "Обзор", "Симпатии", "Профиль").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
    }

    private fun profile(theme: String) {
        var edited = false
        var privacy = false
        val person =
            Profile(
                id = "design-fixture",
                nickname = "Алина",
                age = 24,
                city = "Санкт-Петербург",
                bio = "За спонтанные поездки, хороший кофе и людей, с которыми легко быть собой.",
                searchModes = setOf("DATING", "FRIENDS"),
                interests = listOf("coffee", "travel"),
            )
        screen(theme, "me", {}) {
            ProfileOverview(person, { edited = true }, {}, {}, { privacy = true })
        }
        compose.onNodeWithText("Алина, 24").assertIsDisplayed()
        capture("profile-$theme")
        compose.onNodeWithTag("profile-scroll").performTouchInput {
            swipeDown(startY = 30f, endY = height * 0.7f, durationMillis = 600)
        }
        compose.onNodeWithTag("profile-cover-viewer").assertIsDisplayed()
        compose.onNodeWithContentDescription("Закрыть обложку").performClick()
        compose.onNodeWithTag("profile-cover-viewer").assertDoesNotExist()
        compose.onNodeWithContentDescription("Редактировать анкету").performClick()
        assertTrue(edited)
        compose.onNodeWithContentDescription("Настройки").performClick()
        assertTrue(privacy)
    }

    private fun screen(theme: String, route: String, navigate: (String) -> Unit, content: @Composable () -> Unit) {
        compose.setContent {
            BloomTheme(theme) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = { BloomBottomBar(route, navigate) },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) { content() }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/design-previews/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
