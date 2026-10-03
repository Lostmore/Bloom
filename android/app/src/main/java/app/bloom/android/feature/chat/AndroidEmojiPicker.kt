package app.bloom.android.feature.chat

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

private fun EmojiPickerView.configureScrolling(columns: Int) {
    findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)?.apply {
        // The default two cached cells cause bitmap/text preparation on almost every return scroll.
        setItemViewCacheSize((columns * 8).coerceAtMost(120))
        isNestedScrollingEnabled = false
        itemAnimator = null
        (layoutManager as? GridLayoutManager)?.apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = columns * 2
            spanSizeLookup.isSpanIndexCacheEnabled = true
            spanSizeLookup.isSpanGroupIndexCacheEnabled = true
        }
    }
    findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_header)?.itemAnimator = null
}

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
    BoxWithConstraints(modifier) {
        // Keep cells compact on wide emulators/tablets instead of stretching eight columns.
        val columns = (maxWidth.value / 44f).toInt().coerceAtLeast(6)
        key(themedContext, columns) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    EmojiPickerView(themedContext).apply {
                        emojiGridColumns = columns
                        // AndroidX installs/replaces its RecyclerViews asynchronously after catalog loading.
                        setOnHierarchyChangeListener(
                            object : ViewGroup.OnHierarchyChangeListener {
                                override fun onChildViewAdded(parent: View?, child: View?) {
                                    post { configureScrolling(columns) }
                                }

                                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
                            }
                        )
                        setOnEmojiPickedListener { item ->
                            if (currentEnabled) currentOnEmoji(item.emoji)
                        }
                    }
                },
                update = { view ->
                    if ((view.background as? android.graphics.drawable.ColorDrawable)?.color != background)
                        view.setBackgroundColor(background)
                    val alpha = if (enabled) 1f else 0.5f
                    if (view.alpha != alpha) view.alpha = alpha
                },
                onRelease = {
                    it.setOnEmojiPickedListener(null)
                    it.setOnHierarchyChangeListener(null)
                },
            )
        }
    }
}
