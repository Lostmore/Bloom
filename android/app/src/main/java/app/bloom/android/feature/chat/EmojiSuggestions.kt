package app.bloom.android.feature.chat

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal data class EmojiKeyword(val emoji: String, val label: String, val keywords: List<String>)

private object EmojiKeywords {
    private var cached: List<EmojiKeyword>? = null

    @Synchronized
    fun load(context: Context): List<EmojiKeyword> {
        cached?.let {
            return it
        }
        val json = context.assets.open("emoji/keywords.json").bufferedReader().use { JSONArray(it.readText()) }
        return List(json.length()) { index ->
                val entry = json.getJSONObject(index)
                val words = entry.getJSONArray("keywords")
                EmojiKeyword(
                    entry.getString("emoji"),
                    entry.getString("label"),
                    List(words.length()) { words.getString(it) },
                )
            }
            .also { cached = it }
    }
}

// Only the word ending at the cursor is replaced; never replace selected text or part of a URL.
internal fun emojiWord(value: TextFieldValue): TextRange? {
    if (!value.selection.collapsed) return null
    val end = value.selection.end
    if (end < value.text.length && value.text[end].isLetterOrDigit()) return null
    var start = end
    while (start > 0 && value.text[start - 1].isLetter()) start--
    if (end - start < 2) return null
    val tokenStart = value.text.substring(0, start).indexOfLast { it.isWhitespace() } + 1
    val prefix = value.text.substring(tokenStart, start)
    if (prefix.any { it.isLetterOrDigit() || it in "@#/:._" }) return null
    return TextRange(start, end)
}

@Composable
internal fun EmojiSuggestions(value: TextFieldValue, change: (TextFieldValue) -> Unit) {
    val context = LocalContext.current.applicationContext
    val catalog by
        produceState<List<EmojiKeyword>>(emptyList(), context) {
            this.value = withContext(Dispatchers.IO) { EmojiKeywords.load(context) }
        }
    val word = emojiWord(value)
    val query = word?.let { value.text.substring(it.start, it.end).lowercase(Locale.ROOT).replace('ё', 'е') }
    val suggestions =
        remember(query, catalog) {
            if (query == null) emptyList()
            else
                catalog
                    .filter { entry -> entry.keywords.any { it.startsWith(query) } }
                    .sortedBy { if (query in it.keywords) 0 else 1 }
                    .take(8)
        }
    if (word != null && suggestions.isNotEmpty()) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
        ) {
            LazyRow(contentPadding = PaddingValues(4.dp)) {
                items(suggestions, key = { it.emoji }) { suggestion ->
                    TextButton(
                        onClick = {
                            change(insertEmoji(value.copy(selection = word), suggestion.emoji))
                        },
                        modifier =
                            Modifier.size(48.dp).semantics {
                                contentDescription = "Заменить слово на ${suggestion.label}"
                            },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(suggestion.emoji, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}
