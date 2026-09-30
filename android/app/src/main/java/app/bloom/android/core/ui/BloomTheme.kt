package app.bloom.android.core.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors =
    lightColorScheme(
        primary = Color(0xFFE62D70),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE7F2),
        onPrimaryContainer = Color(0xFF9C185B),
        secondary = Color(0xFF8550C7),
        secondaryContainer = Color(0xFFF1E8FF),
        background = Color(0xFFFFFCFD),
        onBackground = Color(0xFF1D1932),
        surface = Color(0xFFFFFCFD),
        onSurface = Color(0xFF1D1932),
        surfaceVariant = Color(0xFFF3EDF5),
        onSurfaceVariant = Color(0xFF71667D),
        outlineVariant = Color(0xFFEADFEA),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFF8BC2),
        onPrimary = Color(0xFF501333),
        primaryContainer = Color(0xFF422137),
        onPrimaryContainer = Color(0xFFFFC5E1),
        secondary = Color(0xFFD5B0FF),
        secondaryContainer = Color(0xFF342642),
        background = Color(0xFF140D15),
        onBackground = Color(0xFFF7EFF8),
        surface = Color(0xFF20141F),
        onSurface = Color(0xFFF7EFF8),
        surfaceVariant = Color(0xFF2D202D),
        onSurfaceVariant = Color(0xFFBEB0C8),
        outlineVariant = Color(0xFF393040),
    )

@Composable
fun BloomTheme(mode: String = "light", content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity =
                generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()
            activity?.let {
                WindowCompat.getInsetsController(it.window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography =
            Typography(
                headlineLarge =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                    ),
                headlineMedium =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    ),
                titleLarge =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
                bodyLarge =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    ),
            ),
        content = content,
    )
}
