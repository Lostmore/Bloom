package app.bloom.android.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

fun insertEmoji(value: TextFieldValue, emoji: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val text = value.text.replaceRange(start, end, emoji)
    return if (text.length <= 4000) TextFieldValue(text, TextRange(start + emoji.length)) else value
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageComposer(
    value: TextFieldValue,
    change: (TextFieldValue) -> Unit,
    enabled: Boolean,
    canSend: Boolean,
    send: () -> Unit,
    attach: () -> Unit,
) {
    var emojiPanel by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var inputFocused by remember { mutableStateOf(false) }
    BackHandler(enabled = emojiPanel) { emojiPanel = false }
    Column {
        val selectedSticker = bloomSticker(value.text)
        if (enabled && inputFocused && !emojiPanel && selectedSticker == null) {
            EmojiSuggestions(value, change)
        }
        if (selectedSticker != null)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BloomStickerArt(selectedSticker, Modifier.weight(1f))
                IconButton(onClick = { change(TextFieldValue()) }, enabled = enabled) {
                    Icon(Icons.Outlined.Close, "Убрать стикер")
                }
            }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                if (selectedSticker == null) value else TextFieldValue(),
                { if (it.text.length <= 4000) change(it) },
                Modifier.weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { inputFocused = it.isFocused }
                    .testTag("message-input"),
                enabled = enabled,
                placeholder = { Text("Сообщение") },
                maxLines = 5,
                shape = RoundedCornerShape(26.dp),
                leadingIcon = {
                    IconButton(
                        onClick = {
                            emojiPanel = !emojiPanel
                            if (emojiPanel) keyboard?.hide()
                            else {
                                focus.requestFocus()
                                keyboard?.show()
                            }
                        },
                        enabled = enabled,
                    ) {
                        Icon(if (emojiPanel) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions, "Эмодзи")
                    }
                },
                trailingIcon = {
                    IconButton(onClick = attach, enabled = enabled) {
                        Icon(Icons.Outlined.AttachFile, "Прикрепить фото")
                    }
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )
            FilledIconButton(
                onClick = send,
                enabled = enabled && canSend,
                modifier = Modifier.padding(bottom = 4.dp).size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, "Отправить сообщение")
            }
        }
        if (emojiPanel) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                ExpressionPicker(
                    enabled,
                    value.text.isBlank() || selectedSticker != null,
                    emoji = { change(insertEmoji(if (selectedSticker == null) value else TextFieldValue(), it)) },
                    sticker = {
                        change(TextFieldValue(it.wire, TextRange(it.wire.length)))
                        emojiPanel = false
                    },
                )
            }
        }
    }
}
