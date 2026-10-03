package app.bloom.android.core.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Preserve direct dragging, shorten the inertial tail after the finger is released. */
@Composable
fun rememberBloomFling(): FlingBehavior {
    val platform = ScrollableDefaults.flingBehavior()
    return remember(platform) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
                with(platform) { performFling(initialVelocity * 0.8f) } / 0.8f
        }
    }
}

/** Left swipe on a detail screen. Children (pagers, text selection) get first refusal. */
@Composable
fun Modifier.bloomSwipeBack(enabled: Boolean = true, back: () -> Unit): Modifier {
    val currentBack = rememberUpdatedState(back)
    return if (!enabled) this
    else
        pointerInput(Unit) {
            val threshold = 96.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var locked = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.size != 1 || change.isConsumed) break
                    val delta = change.position - down.position
                    if (!locked) {
                        if (abs(delta.y) > viewConfiguration.touchSlop || delta.x > viewConfiguration.touchSlop) break
                        if (-delta.x > viewConfiguration.touchSlop && -delta.x > abs(delta.y) * 1.5f) locked = true
                    }
                    if (locked) change.consume()
                    if (!change.pressed) {
                        if (locked && -delta.x >= threshold && -delta.x > abs(delta.y) * 1.5f) currentBack.value()
                        break
                    }
                }
            }
        }
}
