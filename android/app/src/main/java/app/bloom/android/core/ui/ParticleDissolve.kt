package app.bloom.android.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import kotlin.random.Random

fun Modifier.particleDissolve(disappearing: Boolean, color: Color): Modifier =
    if (!disappearing) this
    else
        composed {
            val progress = remember { Animatable(0f) }
            val dust = remember {
                val random = Random(42)
                List(240) {
                    floatArrayOf(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat())
                }
            }
            val paint = remember { Paint() }
            LaunchedEffect(disappearing) {
                if (disappearing) progress.animateTo(1f, tween(650)) else progress.snapTo(0f)
            }
            drawWithContent {
                val t = progress.value
                if (t == 0f) drawContent()
                else if (t < 1f) {
                    drawIntoCanvas { canvas ->
                        paint.alpha = (1f - t * 2).coerceIn(0f, 1f)
                        canvas.saveLayer(Rect(Offset.Zero, size), paint)
                        drawContent()
                        canvas.restore()
                    }
                    val density = (size.width * size.height / 500f).toInt().coerceIn(24, dust.size)
                    for (i in 0 until density) {
                        val p = dust[i]
                        drawCircle(
                            color.copy(alpha = (1f - t) * (0.4f + p[2] * .6f)),
                            radius = (.6f + p[3]) * this.density,
                            center =
                                Offset(
                                    p[0] * size.width + t * (p[2] - .35f) * 70.dp.toPx(),
                                    p[1] * size.height - t * p[3] * 45.dp.toPx(),
                                ),
                        )
                    }
                }
            }
        }
