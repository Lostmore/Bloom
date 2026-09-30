package app.bloom.android

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.bloom.android.core.model.Interest
import app.bloom.android.core.ui.BloomTheme
import app.bloom.android.feature.profile.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun interestsStaySelectedWhenGoingBackFromPhotos() {
        var draft by mutableStateOf(OnboardingDraft(step = 4))
        compose.setContent {
            BloomTheme("light") {
                Surface {
                    OnboardingContent(
                        draft,
                        listOf(Interest("travel", "Путешествия", "", "")),
                        false,
                        null,
                        null,
                        { draft = it },
                        {},
                        { draft = draft.copy(step = draft.step - 1) },
                        { if (draft.error() == null) draft = draft.copy(step = draft.step + 1) },
                    )
                }
            }
        }
        compose.onNodeWithText("Путешествия").performClick()
        compose.onNodeWithText("1 из 10").assertIsDisplayed()
        capture("onboarding-interests")
        compose.onNodeWithText("Продолжить").performClick()
        compose.onNodeWithText("Покажи себя").assertIsDisplayed()
        capture("onboarding-photos")
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("1 из 10").assertIsDisplayed()
        assertEquals(listOf("travel"), draft.interests)
    }

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = File("build/design-previews/$name.png")
            output.parentFile!!.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
