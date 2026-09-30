package app.bloom.android.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatRoomScreen(graph: AppGraph, roomId: Long, myId: String, targetMessageId: Long? = null, back: () -> Unit) {
    val connection = remember(graph, roomId) { ChatConnection(graph.http, graph.sessions, graph.baseUrl, roomId) }
    val state by connection.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    var partner by remember(roomId) { mutableStateOf("Собеседник") }
    var context by remember(roomId, targetMessageId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var contextNotice by remember { mutableStateOf<String?>(null) }
    var focused by remember(targetMessageId) { mutableStateOf(targetMessageId != null) }
    var jumped by remember(targetMessageId) { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matchIndex by remember(query) { mutableIntStateOf(0) }
    val messages = if (focused && context.isNotEmpty()) context else state.messages
    val hits =
        remember(messages, query) {
            if (query.isBlank()) emptyList()
            else messages.filter { it.content.orEmpty().contains(query.trim(), true) }.map { it.id }
        }
    val highlighted = if (showSearch) hits.getOrNull(matchIndex) else if (focused) targetMessageId else null
    LaunchedEffect(hits.size) { matchIndex = matchIndex.coerceIn(0, hits.lastIndex.coerceAtLeast(0)) }
    LaunchedEffect(roomId) {
        try {
            graph.chat
                .rooms()
                .orEmpty()
                .find { it.id == roomId }
                ?.let { partner = graph.users.profile(it.partner(myId)).nickname }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {}
    }
    LaunchedEffect(targetMessageId) {
        if (targetMessageId != null) {
            try {
                context = graph.chat.context(roomId, targetMessageId).items.orEmpty().filter { it.roomId == roomId }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val denied = exception is retrofit2.HttpException && exception.code() in listOf(401, 403)
                context = if (denied) emptyList() else graph.chatPreviews.history.value.filter { it.roomId == roomId }
                contextNotice =
                    if (denied) "Нет доступа к этому сообщению."
                    else "Показана загруженная история. Сервер пока не вернул фрагмент переписки."
            }
        }
    }
    LaunchedEffect(connection, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                connection.connect()
                while (true) {
                    delay(60_000)
                    if (
                        (graph.sessions.session.value?.expiresAt ?: Long.MAX_VALUE) <=
                            System.currentTimeMillis() + 60_000
                    )
                        connection.connect()
                }
            } finally {
                connection.disconnect()
            }
        }
    }
    DisposableEffect(connection) { onDispose { connection.disconnect() } }
    LaunchedEffect(state.messages) { graph.chatPreviews.recordHistory(state.messages) }
    LaunchedEffect(messages.lastOrNull()?.id, highlighted) {
        val index = messages.indexOfFirst { it.id == highlighted }
        if (index >= 0 && (showSearch || !jumped)) {
            list.scrollToItem(index)
            jumped = true
        } else if (!focused && !showSearch && messages.isNotEmpty()) {
            val nearBottom =
                (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: messages.lastIndex) >= messages.lastIndex - 3
            if (nearBottom || messages.last().senderId == myId) list.animateScrollToItem(messages.lastIndex)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
            PersonAvatar(partner, size = 40.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(partner, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    if (state.connected) "Чат" else if (state.connecting) "Подключаемся…" else "Нет соединения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    showSearch = !showSearch
                    query = ""
                }
            ) {
                Icon(if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search, "Поиск в чате")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (showSearch) {
            OutlinedTextField(
                query,
                { query = it.take(200) },
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                placeholder = { Text("В загруженных сообщениях") },
                singleLine = true,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hits.isEmpty()) "Нет совпадений" else "${matchIndex + 1} из ${hits.size}",
                    Modifier.weight(1f).padding(start = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                IconButton(onClick = { matchIndex-- }, enabled = matchIndex > 0) {
                    Icon(Icons.Outlined.KeyboardArrowUp, "Предыдущее совпадение")
                }
                IconButton(onClick = { matchIndex++ }, enabled = matchIndex < hits.lastIndex) {
                    Icon(Icons.Outlined.KeyboardArrowDown, "Следующее совпадение")
                }
            }
        }
        if (state.error != null) {
            ErrorMessage(state.error)
            TextButton(onClick = { scope.launch { connection.connect() } }, enabled = !state.connecting) {
                Text("Подключиться")
            }
        }
        if (focused) {
            if (contextNotice != null)
                Text(contextNotice!!, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            TextButton(
                onClick = {
                    focused = false
                    scope.launch { if (state.messages.isNotEmpty()) list.scrollToItem(state.messages.lastIndex) }
                }
            ) {
                Text("К новым сообщениям")
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty())
                EmptyBloom(
                    "Начните с простого «привет»",
                    "Можно написать сообщение или выбрать фото.",
                    Modifier.align(Alignment.Center),
                    Icons.Outlined.WavingHand,
                )
            else
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = list,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        if (index == 0 || message.createdAt.take(10) != messages[index - 1].createdAt.take(10))
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    message.createdAt.take(10),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        MessageBubble(graph, message, message.senderId == myId, message.id == highlighted)
                    }
                }
            if (list.canScrollForward && !showSearch)
                SmallFloatingActionButton(
                    onClick = { scope.launch { list.animateScrollToItem(messages.lastIndex.coerceAtLeast(0)) } },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(Icons.Outlined.KeyboardArrowDown, "Последние сообщения")
                }
        }
        ChatComposer(graph, roomId, myId, connection, state)
    }
}
