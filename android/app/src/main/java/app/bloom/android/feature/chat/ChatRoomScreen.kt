package app.bloom.android.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.bloom.android.AppGraph
import app.bloom.android.core.ui.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatRoomScreen(graph: AppGraph, roomId: Long, myId: String, back: () -> Unit) {
    val connection =
        remember(graph, roomId) {
            ChatConnection(graph.http, graph.sessions, graph.baseUrl, roomId)
        }
    val state by connection.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var sentAfter by remember { mutableLongStateOf(0) }
    var partnerName by remember(roomId) { mutableStateOf<String?>(null) }
    LaunchedEffect(roomId) {
        try {
            val room = graph.chat.rooms().orEmpty().find { it.id == roomId }
            if (room != null) partnerName = graph.users.profile(room.partner(myId)).nickname
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            /* Private or unavailable profile: keep the conversation open. */
        }
    }
    val list = rememberLazyListState()
    LaunchedEffect(connection, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                connection.connect()
                while (true) {
                    delay(60_000)
                    val session = graph.sessions.session.value
                    if (session != null && session.expiresAt <= System.currentTimeMillis() + 60_000)
                        connection.connect()
                }
            } finally {
                connection.disconnect()
            }
        }
    }
    DisposableEffect(connection) { onDispose { connection.disconnect() } }
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        state.messages.lastOrNull()?.let(graph.chatPreviews::record)
        if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex)
        if (
            pendingText != null &&
                state.messages.any {
                    it.id > sentAfter && it.senderId == myId && it.content == pendingText
                }
        ) {
            pendingText = null
        }
    }
    LaunchedEffect(pendingText) {
        if (pendingText != null) {
            delay(15_000)
            draft = pendingText.orEmpty()
            pendingText = null
            sendError = "Подтверждение не пришло. Текст возвращён в поле — проверь историю перед повторной отправкой."
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
            PersonAvatar(partnerName ?: "Bloom", size = 40.dp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(partnerName ?: "Ваш разговор", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.connected) "Можно говорить обо всём"
                    else if (state.connecting) "Подключаемся…" else "Нет соединения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (state.error != null) {
            ErrorMessage(state.error)
            TextButton(
                onClick = { scope.launch { connection.connect() } },
                enabled = !state.connecting,
            ) {
                Text("Подключиться")
            }
        }
        if (state.messages.isEmpty())
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyBloom(
                    "Начните с простого «привет»",
                    "Иногда одного сообщения достаточно для хорошего знакомства.",
                    icon = Icons.Outlined.WavingHand,
                )
            }
        else
            LazyColumn(
                Modifier.weight(1f),
                state = list,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    val own = message.senderId == myId
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            Modifier.widthIn(max = 300.dp),
                            shape =
                                RoundedCornerShape(
                                    20.dp,
                                    20.dp,
                                    if (own) 5.dp else 20.dp,
                                    if (own) 20.dp else 5.dp,
                                ),
                            color =
                                if (own) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (!message.content.isNullOrBlank()) Text(message.content)
                                if (!message.attachments.isNullOrEmpty())
                                    Text(
                                        "Вложение · просмотр пока недоступен",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                val time = runCatching {
                                    OffsetDateTime.parse(message.createdAt)
                                        .atZoneSameInstant(java.time.ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                }
                                    .getOrDefault("")
                                Text(
                                    time,
                                    Modifier.align(Alignment.End),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        ErrorMessage(sendError)
        if (pendingText != null)
            Text(
                "Отправляем сообщение…",
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                draft,
                {
                    draft = it.take(4000)
                    sendError = null
                },
                Modifier.weight(1f),
                enabled = pendingText == null,
                placeholder = { Text("Написать сообщение…") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
            )
            FilledIconButton(
                enabled = state.connected && draft.isNotBlank() && pendingText == null,
                onClick = {
                    sentAfter = state.messages.lastOrNull()?.id ?: 0
                    pendingText = draft.trim()
                    if (connection.send(draft)) draft = ""
                    else {
                        pendingText = null
                        sendError = "Не удалось отправить. Текст сохранён в поле ввода."
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, "Отправить сообщение")
            }
        }
    }
}
