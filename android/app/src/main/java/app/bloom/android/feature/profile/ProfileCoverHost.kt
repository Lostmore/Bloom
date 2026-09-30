package app.bloom.android.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bloom.android.core.model.Profile

/** Receives only unconsumed downward scrolling; regular profile scrolling stays intact. */
@Composable
fun ProfileCoverHost(profile: Profile?, content: @Composable (Modifier, () -> Unit) -> Unit) {
    var expanded by remember(profile?.id) { mutableStateOf(false) }
    var pull by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 88.dp.toPx() }
    val stretch by animateFloatAsState(pull * 0.18f, spring(), label = "cover-stretch")
    val connection =
        remember(profile?.id, threshold) {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (profile == null || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y > 0 || pull > 0) {
                        pull = (pull + available.y).coerceIn(0f, threshold * 1.5f)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull >= threshold) expanded = true
                    pull = 0f
                    return Velocity.Zero
                }
            }
        }
    content(Modifier.nestedScroll(connection).graphicsLayer { translationY = stretch }) {
        if (profile != null) expanded = true
    }
    if (expanded && profile != null) ProfileCoverViewer(profile) { expanded = false }
}

@Composable
private fun ProfileCoverViewer(profile: Profile, dismiss: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    var drag by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, spring(), label = "cover-reveal")
    LaunchedEffect(Unit) { entered = true }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().testTag("profile-cover-viewer").background(Color(0xFF180F1B)).pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (drag > threshold) dismiss() else drag = 0f },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag = (drag + amount).coerceAtLeast(0f)
                }
            }
        ) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX = 0.94f + reveal * 0.06f
                        scaleY = scaleX
                        translationY = drag * 0.35f
                        alpha = reveal
                    }
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFBA7295), Color(0xFF674366), Color(0xFF211A2B)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                app.bloom.android.core.ui.PersonAvatar(profile.nickname, size = 220.dp)
            }
            FilledTonalIconButton(
                onClick = dismiss,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(16.dp),
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
            ) {
                Icon(Icons.Outlined.Close, "Закрыть обложку")
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        profile.nickname + (profile.displayedAge()?.let { ", $it" } ?: ""),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (!profile.city.isNullOrBlank()) Text(profile.city)
                    Text(
                        "Фото пока недоступно · потяни вниз, чтобы закрыть",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
