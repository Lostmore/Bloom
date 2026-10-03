package app.bloom.android.feature.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

/** Put the timestamp after the last text line when it fits, otherwise below it. */
@Composable
internal fun CompactMessageText(text: String, footer: @Composable () -> Unit) {
    val style = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    Layout(
        content = {
            Text(text, style = style)
            footer()
        }
    ) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val body = children[0].measure(loose)
        val meta = children[1].measure(loose)
        val measured = measurer.measure(text, style = style, constraints = loose)
        val last = measured.lineCount - 1
        val gap = 6.dp.roundToPx()
        val end = ceil(measured.getLineRight(last)).toInt()
        val rtl = measured.getParagraphDirection(measured.getLineStart(last)) == ResolvedTextDirection.Rtl
        val inline =
            !rtl &&
                end + gap + meta.width <= constraints.maxWidth &&
                (last == 0 || meta.height <= measured.getLineBottom(last) - measured.getLineTop(last))
        val width =
            constraints.constrainWidth(
                if (inline) max(body.width, end + gap + meta.width) else max(body.width, meta.width)
            )
        val height =
            constraints.constrainHeight(if (inline) max(body.height, meta.height) else body.height + meta.height)
        layout(width, height) {
            body.placeRelative(0, 0)
            meta.placeRelative(width - meta.width, height - meta.height)
        }
    }
}
