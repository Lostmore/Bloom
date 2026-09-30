package app.bloom.android.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatMessage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(graph: AppGraph, message: ChatMessage, own: Boolean, highlighted: Boolean) {
    var details by remember(message.id) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val readTime = message.readAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
    if (details)
        AlertDialog(
            onDismissRequest = { details = false },
            title = { Text("О сообщении") },
            text = {
                Text(
                    if (readTime != null)
                        "Прочитано: " +
                            readTime
                                .atZoneSameInstant(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
                    else "Сохранено в чате. Подтверждение прочтения пока не получено."
                )
            },
            confirmButton = { TextButton(onClick = { details = false }) { Text("Понятно") } },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(message.content.orEmpty()))
                        details = false
                    }
                ) {
                    Text("Копировать")
                }
            },
        )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
        Surface(
            Modifier.widthIn(max = 310.dp)
                .combinedClickable(onClick = {}, onLongClick = { details = true })
                .pointerInput(message.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) details = true
                        }
                    }
                },
            shape = RoundedCornerShape(18.dp, 18.dp, if (own) 5.dp else 18.dp, if (own) 18.dp else 5.dp),
            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = if (highlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val photos =
                    message.attachments
                        .orEmpty()
                        .filter { it.mediaType.orEmpty().startsWith("image/") || it.mediaType == "image" }
                        .mapNotNull { mediaId(it)?.let(::PhotoSource) }
                        .take(6)
                if (photos.isNotEmpty()) PhotoAlbum(graph, photos, Modifier.width(294.dp))
                val sticker = bloomSticker(message.content)
                if (sticker != null) BloomStickerArt(sticker)
                else if (!message.content.isNullOrBlank())
                    Text(message.content, Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                if (message.attachments.orEmpty().size > photos.size)
                    Text("Вложение не поддерживается", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        messageTime(message.createdAt),
                        Modifier.padding(horizontal = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (own)
                        Icon(
                            if (readTime != null) Icons.Outlined.DoneAll else Icons.Outlined.Done,
                            if (readTime != null) "Прочитано" else "Отправлено",
                            Modifier.size(16.dp),
                            tint =
                                if (readTime != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }
    }
}

fun messageTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}
    .getOrDefault("")
