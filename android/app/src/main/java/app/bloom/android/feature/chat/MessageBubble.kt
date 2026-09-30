package app.bloom.android.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatMessage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageBubble(graph: AppGraph, message: ChatMessage, own: Boolean, highlighted: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
        Surface(
            Modifier.widthIn(max = 310.dp),
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
                if (!message.content.isNullOrBlank())
                    SelectionContainer { Text(message.content, Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) }
                if (message.attachments.orEmpty().size > photos.size)
                    Text("Вложение не поддерживается", style = MaterialTheme.typography.bodySmall)
                Text(
                    messageTime(message.createdAt),
                    Modifier.align(Alignment.End).padding(horizontal = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun messageTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}
    .getOrDefault("")
