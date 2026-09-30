package app.bloom.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val palette =
        listOf(
            listOf(Color(0xFFEE8B9D), Color(0xFFAC426F)),
            listOf(Color(0xFFAF9FD3), Color(0xFF68518F)),
            listOf(Color(0xFF83B6AC), Color(0xFF38675F)),
            listOf(Color(0xFFD9AB7B), Color(0xFF986A57)),
        )
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors =
        palette[(name.hashCode() and Int.MAX_VALUE) % palette.size].map {
            if (dark) lerp(it, Color(0xFF30363C), 0.65f) else it
        }
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifBlank { "B" },
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
