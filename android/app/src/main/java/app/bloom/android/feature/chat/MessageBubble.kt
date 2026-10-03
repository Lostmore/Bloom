package app.bloom.android.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
fun MessageBubble(
    graph: AppGraph,
    message: ChatMessage,
    own: Boolean,
    highlighted: Boolean,
    edit: ((String) -> Boolean)? = null,
    delete: (() -> Boolean)? = null,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    select: (() -> Unit)? = null,
    startEditing: () -> Unit = {},
) {
    var details by remember(message.id) { mutableStateOf(false) }
    var menu by remember(message.id) { mutableStateOf(false) }
    var editing by remember(message.id) { mutableStateOf(false) }
    var deleting by remember(message.id) { mutableStateOf(false) }
    var replacement by remember(message.id, message.content) { mutableStateOf(message.content.orEmpty()) }
    var actionError by remember(message.id) { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val readTime = message.readAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
    fun closeDialog() {
        details = false
        menu = false
        editing = false
        deleting = false
        actionError = null
    }
    LaunchedEffect(message.deletedAt) {
        if (message.deletedAt != null) closeDialog()
    }
    if (details || deleting)
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(if (deleting) "Удалить сообщение?" else "О сообщении") },
            text = {
                Column {
                    if (deleting) Text("Сообщение будет помечено удалённым для участников чата.")
                    else {
                        Text(
                            if (readTime != null)
                                "Прочитано: " +
                                    readTime
                                        .atZoneSameInstant(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
                            else "Сохранено в чате. Подтверждение прочтения пока не получено."
                        )
                        if (own && message.deletedAt == null) {
                            if (edit != null)
                                TextButton(
                                    onClick = {
                                        replacement = message.content.orEmpty()
                                        details = false
                                        startEditing()
                                    }
                                ) {
                                    Text("Редактировать")
                                }
                            if (delete != null) TextButton(onClick = { deleting = true }) { Text("Удалить") }
                        }
                    }
                    actionError?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!deleting || delete?.invoke() == true) closeDialog()
                        else actionError = "Нет соединения. Попробуй ещё раз."
                    }
                ) {
                    Text(if (deleting) "Удалить" else "Понятно")
                }
            },
            dismissButton = {
                if (deleting) TextButton(onClick = { closeDialog() }) { Text("Отмена") }
                else if (message.deletedAt == null)
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(message.content.orEmpty()))
                            closeDialog()
                        }
                    ) {
                        Text("Копировать")
                    }
            },
        )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            Modifier.widthIn(max = 310.dp)
                .combinedClickable(
                    enabled = !editing && message.deletedAt == null,
                    onClick = {
                        if (selectionActive) select?.invoke() else menu = true
                    },
                    onLongClick = { if (select != null) select() else details = true },
                )
                .pointerInput(message.id, select, editing) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (
                                !editing &&
                                    message.deletedAt == null &&
                                    event.type == PointerEventType.Press &&
                                    event.buttons.isSecondaryPressed
                            ) {
                                menu = true
                            }
                        }
                    }
                },
            shape =
                RoundedCornerShape(
                    18.dp,
                    18.dp,
                    if (own) 5.dp else 18.dp,
                    if (own) 18.dp else 5.dp,
                ),
            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border =
                if (highlighted || selected)
                    BorderStroke(if (selected) 3.dp else 1.dp, MaterialTheme.colorScheme.primary)
                else null,
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box {
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        if (!message.content.isNullOrBlank())
                            DropdownMenuItem(
                                text = { Text("Копировать") },
                                onClick = {
                                    clipboard.setText(AnnotatedString(message.content.orEmpty()))
                                    menu = false
                                },
                            )
                        if (
                            own &&
                                edit != null &&
                                !message.content.isNullOrBlank() &&
                                bloomSticker(message.content) == null
                        )
                            DropdownMenuItem(
                                text = { Text("Изменить") },
                                onClick = {
                                    replacement = message.content.orEmpty()
                                    menu = false
                                    startEditing()
                                },
                            )
                        if (own && delete != null)
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = {
                                    menu = false
                                    deleting = true
                                },
                            )
                        if (select != null)
                            DropdownMenuItem(
                                text = { Text("Выбрать") },
                                onClick = {
                                    menu = false
                                    select()
                                },
                            )
                        if (own && readTime != null) {
                            val local = readTime.atZoneSameInstant(ZoneId.systemDefault())
                            val day = local.toLocalDate()
                            val today = java.time.LocalDate.now()
                            val date =
                                when (day) {
                                    today -> "сегодня"
                                    today.minusDays(1) -> "вчера"
                                    else ->
                                        local.format(
                                            DateTimeFormatter.ofPattern(
                                                "d MMMM yyyy",
                                                java.util.Locale.forLanguageTag("ru"),
                                            )
                                        )
                                }
                            Text(
                                "Прочитано $date в ${local.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (selected) Text("✓ Выбрано", style = MaterialTheme.typography.labelSmall)
                if (message.deletedAt != null) {
                    Text("Сообщение удалено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val photos =
                        message.attachments
                            .orEmpty()
                            .filter {
                                it.mediaType.orEmpty().startsWith("image/") || it.mediaType == "image"
                            }
                            .mapNotNull { mediaId(it)?.let(::PhotoSource) }
                            .take(6)
                    if (photos.isNotEmpty()) PhotoAlbum(graph, photos, Modifier.width(294.dp))
                    val sticker = bloomSticker(message.content)
                    if (sticker != null) BloomStickerArt(sticker)
                    else if (!message.content.isNullOrBlank())
                        Text(message.content, Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                    if (message.attachments.orEmpty().size > photos.size)
                        Text(
                            "Вложение не поддерживается",
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (message.editedAt != null && message.deletedAt == null)
                        Text("изменено", style = MaterialTheme.typography.labelSmall)
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
                            Modifier.size(24.dp)
                                .clickable(enabled = readTime != null && !selectionActive) {
                                    menu = true
                                }
                                .padding(4.dp),
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
