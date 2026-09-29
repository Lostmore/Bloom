package app.bloom.android.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Petals =
    listOf(
        "M10 13C8 13 7 15 8 18C9 36 21 48 42 48C44 29 33 13 10 13Z",
        "M84 5C63 6 48 21 48 48C71 47 88 32 88 9C88 6 87 5 84 5Z",
        "M42 54C25 54 13 62 12 75C11 86 26 87 34 78C40 72 43 63 42 54Z",
        "M49 54C47 77 59 99 82 99C86 99 88 98 87 94C86 72 74 56 49 54Z",
    )

@Composable
fun BloomMark(modifier: Modifier = Modifier, loading: Boolean = false) {
    val paths = remember { Petals.map { PathParser().parsePathString(it).toPath() } }
    val progress =
        if (loading) {
            val transition = rememberInfiniteTransition(label = "Bloom loading")
            val value by
                transition.animateFloat(
                    0.65f,
                    1f,
                    infiniteRepeatable(tween(850), RepeatMode.Reverse),
                    label = "Petal pulse",
                )
            value
        } else 1f
    Canvas(modifier.semantics { contentDescription = if (loading) "Загрузка" else "Bloom" }) {
        scale(size.width / 96f, size.height / 104f, Offset.Zero) {
            paths.forEachIndexed { index, path ->
                drawPath(
                    path,
                    Brush.linearGradient(
                        listOf(Color(0xFFFFAD9F), Color(0xFFFF6AB1), Color(0xFFD958E9)),
                        Offset.Zero,
                        Offset(90f, 104f),
                    ),
                    alpha = if (!loading) 1f else if (index % 2 == 0) progress else 1.65f - progress,
                )
            }
        }
    }
}

@Composable
fun BloomBrand(modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BloomMark(Modifier.size(if (large) 54.dp else 30.dp))
        Spacer(Modifier.width(if (large) 14.dp else 9.dp))
        Text(
            "Bloom",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = if (large) 42.sp else 25.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
        )
    }
}
