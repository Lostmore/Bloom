package app.bloom.android

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.*
import app.bloom.android.feature.auth.WelcomeContent
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WelcomeDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun lightWelcomeIsReadableAndScrollable() = render("light")

    @Test fun darkWelcomeIsReadableAndScrollable() = render("dark")

    private fun render(theme: String) {
        compose.setContent {
            BloomTheme(theme) {
                Surface(Modifier.fillMaxSize().testTag("preview")) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        BloomBrand()
                        WelcomeContent()
                        BloomButton("Найти своих людей →", {})
                    }
                }
            }
        }
        compose.onNodeWithText("Люди. Моменты.\nВозможности.").assertIsDisplayed()
        val output = File("build/design-previews/bloom-$theme.png")
        output.parentFile!!.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.onNodeWithText("Найти своих людей →").performScrollTo().assertIsDisplayed().performClick()
    }
}
