package app.bloom.android.feature.chat

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView

/** AndroidX owns the Unicode catalog, categories, variants and recent selections. */
@Composable
fun AndroidEmojiPicker(enabled: Boolean, onEmoji: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val background = MaterialTheme.colorScheme.surface.toArgb()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnEmoji by rememberUpdatedState(onEmoji)
    val themedContext =
        remember(context, dark) {
            val configuration =
                Configuration(context.resources.configuration).apply {
                    uiMode =
                        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            ContextThemeWrapper(
                context.createConfigurationContext(configuration),
                if (dark) android.R.style.Theme_Material_NoActionBar
                else android.R.style.Theme_Material_Light_NoActionBar,
            )
        }
    key(themedContext) {
        AndroidView(
            modifier = modifier,
            factory = {
                EmojiPickerView(themedContext).apply {
                    emojiGridColumns = 8
                    setOnEmojiPickedListener { item ->
                        if (currentEnabled) currentOnEmoji(item.emoji)
                    }
                }
            },
            update = { view ->
                view.setBackgroundColor(background)
                view.alpha = if (enabled) 1f else 0.5f
            },
            onRelease = { it.setOnEmojiPickedListener(null) },
        )
    }
}
