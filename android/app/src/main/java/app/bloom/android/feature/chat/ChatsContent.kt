package app.bloom.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatRowItem(val id: Long, val name: String, val preview: String? = null, val timestamp: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsContent(
    items: List<ChatRowItem>,
    refreshing: Boolean,
    error: String?,
    refresh: () -> Unit,
    openChat: (Long) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background =
        if (dark) listOf(Color(0xFF35091F), Color(0xFF1C101B), Color(0xFF140D15))
        else listOf(Color(0xFFFFF1F6), Color(0xFFFFFCFD), Color(0xFFFFFCFD))
    val filtered = items.filter {
        it.name.contains(query.trim(), true) || it.preview.orEmpty().contains(query.trim(), true)
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(background))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Чаты", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
        }
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("chat-search"),
            placeholder = { Text("Поиск сообщений") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty())
                    IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить поиск") }
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.White,
                    focusedContainerColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
        )
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = refresh,
            modifier = Modifier.weight(1f).testTag("chat-refresh"),
        ) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("chat-list"),
                contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Сообщения", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (query.isEmpty()) "${items.size}" else "${filtered.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (error != null) item { Box(Modifier.padding(20.dp)) { ErrorMessage(error) } }
                items(filtered, key = { it.id }) { chat -> ConversationRow(chat) { openChat(chat.id) } }
                if (filtered.isEmpty() && !refreshing)
                    item {
                        EmptyBloom(
                            if (query.isBlank()) "Всё начинается с приветствия" else "Ничего не найдено",
                            if (query.isBlank()) "Открой совпадение и напиши первым. Ваш разговор появится здесь."
                            else "Попробуй другое имя или фразу.",
                            Modifier.padding(top = 60.dp),
                            Icons.Outlined.ChatBubbleOutline,
                        )
                    }
                if (refreshing && items.isEmpty()) item { LoadingBloom(Modifier.height(300.dp)) }
            }
        }
    }
}

@Composable
private fun ConversationRow(chat: ChatRowItem, open: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = open).padding(horizontal = 22.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PersonAvatar(chat.name, size = 58.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        chat.name,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    chat.timestamp?.let { timestamp ->
                        Text(
                            messageTime(timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    chat.preview ?: "Начните ваш разговор",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(
            Modifier.padding(start = 94.dp, end = 22.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        )
    }
}

private fun messageTime(value: String): String = runCatching {
    val time = OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
    val pattern = if (time.toLocalDate() == java.time.LocalDate.now()) "HH:mm" else "dd.MM"
    time.format(DateTimeFormatter.ofPattern(pattern))
}
    .getOrDefault("")
