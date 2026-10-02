package app.bloom.android.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BloomSticker(val id: String, val emoji: String, val caption: String) {
    // A versioned text envelope works with the existing text-only Chat service.
    val wire: String
        get() = "[bloom-sticker:v1:$id]"
}

val BloomStickers =
    listOf(
        BloomSticker("hello", "🌸", "Привет, это я"),
        BloomSticker("coffee", "☕", "По чашечке?"),
        BloomSticker("spark", "✨", "Есть искра"),
        BloomSticker("hug", "🫂", "Обнимаю"),
        BloomSticker("walk", "🌿", "Пойдём гулять"),
        BloomSticker("smile", "😊", "Ты — улыбка"),
        BloomSticker("night", "🌙", "До завтра"),
        BloomSticker("yes", "🙌", "Я за!"),
        BloomSticker("miss", "🥹", "Жду встречи"),
        BloomSticker("wow", "🤩", "Вот это да"),
        BloomSticker("thanks", "💐", "Спасибо тебе"),
        BloomSticker("heart", "🫶", "На одной волне"),
    )

fun bloomSticker(text: String?) = BloomStickers.find { it.wire == text }

@Composable
fun BloomStickerArt(sticker: BloomSticker, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(sticker.emoji, fontSize = 48.sp)
        Text(sticker.caption, style = MaterialTheme.typography.labelLarge)
        Text("bloom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ExpressionPicker(
    enabled: Boolean,
    stickersEnabled: Boolean,
    emoji: (String) -> Unit,
    sticker: (BloomSticker) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf("Emoji") }
    Column(Modifier.fillMaxWidth().height(280.dp)) {
        LazyRow(
            modifier = Modifier.testTag("expression-categories"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(listOf("Emoji", "Bloom")) { name ->
                FilterChip(
                    selected == name,
                    { selected = name },
                    label = { Text(if (name == "Bloom") "Стикеры Bloom" else "Эмодзи") },
                )
            }
        }
        if (selected == "Bloom") {
            if (!stickersEnabled)
                Text(
                    "Отправь или очисти текст, чтобы выбрать стикер.",
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f)) {
                items(BloomStickers) { item ->
                    TextButton(
                        onClick = { sticker(item) },
                        enabled = enabled && stickersEnabled,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        BloomStickerArt(item)
                    }
                }
            }
        } else {
            AndroidEmojiPicker(enabled, emoji, Modifier.fillMaxWidth().weight(1f))
        }
    }
}
