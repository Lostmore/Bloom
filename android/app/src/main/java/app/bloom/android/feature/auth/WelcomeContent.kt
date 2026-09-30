package app.bloom.android.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bloom.android.R
import app.bloom.android.core.ui.BloomButton
import app.bloom.android.core.ui.BloomMark

@Composable
fun WelcomeContent(register: () -> Unit = {}, login: () -> Unit = {}, serverSettings: (() -> Unit)? = null) {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity =
            generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
                .filterIsInstance<android.app.Activity>()
                .firstOrNull()
        val controller = activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, view) }
        val oldStatus = controller?.isAppearanceLightStatusBars
        val oldNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (oldStatus != null) controller.isAppearanceLightStatusBars = oldStatus
            if (oldNavigation != null) controller.isAppearanceLightNavigationBars = oldNavigation
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF17151D))) {
        Image(
            painterResource(R.drawable.welcome_sunset),
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color(0xEE111017))
                    )
                )
        )
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 26.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    BloomMark(Modifier.size(40.dp))
                    Text(
                        "Bloom",
                        Modifier.padding(start = 10.dp),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (serverSettings != null)
                    IconButton(onClick = serverSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Outlined.Settings, "Адрес сервера", tint = Color.White)
                    }
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Больше, чем\nзнакомства",
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Твои люди. Ваши моменты.\nИстории, которые начинаются здесь.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                BloomButton("Создать аккаунт", register)
                OutlinedButton(
                    onClick = login,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("Войти", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "Знакомства, дружба и всё, что между ними. 18+",
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
