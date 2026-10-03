package app.bloom.android.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
fun ChatRoomScreen(
    graph: AppGraph,
    roomId: Long,
    myId: String,
    targetMessageId: Long? = null,
    openProfile: (String) -> Unit = {},
    back: () -> Unit,
) {
    val connection =
        remember(graph, roomId) {
            ChatConnection(graph.http, graph.sessions, graph.baseUrl, roomId)
        }
    val state by connection.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val composerState = rememberSaveableStateHolder()
    val searchFocus = remember { FocusRequester() }
    var partner by remember(roomId) { mutableStateOf("Собеседник") }
    var partnerId by remember(roomId, myId) { mutableStateOf<String?>(null) }
    var partnerProfile by remember(roomId) { mutableStateOf<app.bloom.android.core.model.Profile?>(null) }
    var partnerInterests by remember(roomId) { mutableStateOf<List<String>>(emptyList()) }
    var showPartner by remember(roomId) { mutableStateOf(false) }
    var context by remember(roomId, targetMessageId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var contextNotice by remember { mutableStateOf<String?>(null) }
    var focused by remember(targetMessageId) { mutableStateOf(targetMessageId != null) }
    var jumped by remember(targetMessageId) { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchList by remember { mutableStateOf(false) }
    LaunchedEffect(showSearch) {
        if (showSearch) searchFocus.requestFocus()
    }
    var query by remember { mutableStateOf("") }
    var matchIndex by remember(query) { mutableIntStateOf(0) }
    var loadingHistory by remember(roomId) { mutableStateOf(true) }
    var historyError by remember(roomId) { mutableStateOf<String?>(null) }
    var reloadHistory by remember(roomId) { mutableIntStateOf(0) }
    LaunchedEffect(roomId, state.connected, reloadHistory, state.historyVersion) {
        loadingHistory = true
        historyError = null
        try {
            val revision = state.historyVersion
            val history = graph.chat.history(roomId).orEmpty()
            connection.mergeHistory(history, revision)
            if (focused) context = history.filter { it.roomId == roomId }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            historyError = "Не удалось загрузить историю. Попробуй ещё раз."
        } finally {
            loadingHistory = false
        }
    }
    val messages = if (focused && context.isNotEmpty()) context else state.messages
    var selectedIds by remember(roomId) { mutableStateOf(setOf<Long>()) }
    var selectionMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var pendingDeletes by remember { mutableStateOf(setOf<Long>()) }
    val clipboard = LocalClipboardManager.current
    val selectedMessages = messages.filter { it.id in selectedIds && it.deletedAt == null }
    BackHandler(selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
        selectionMenu = false
    }
    LaunchedEffect(state.messages) {
        val deleted = state.messages.filter { it.deletedAt != null }.map { it.id }.toSet()
        selectedIds = selectedIds - deleted
        pendingDeletes = pendingDeletes - deleted
    }
    LaunchedEffect(state.errorVersion, state.connected) { pendingDeletes = emptySet() }
    LaunchedEffect(pendingDeletes) {
        if (pendingDeletes.isNotEmpty()) {
            delay(15_000)
            pendingDeletes = emptySet()
        }
    }
    if (confirmDelete)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить сообщения (${selectedMessages.size})?") },
            text = { Text("Они будут удалены для обоих участников переписки.") },
            confirmButton = {
                TextButton(
                    enabled = state.connected && pendingDeletes.isEmpty(),
                    onClick = {
                        pendingDeletes =
                            selectedMessages
                                .filter { it.senderId == myId && connection.delete(it.id) }
                                .map { it.id }
                                .toSet()
                        confirmDelete = false
                        selectionMenu = false
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    val hits =
        remember(state.messages, query) {
            state.messages
                .filter {
                    it.deletedAt == null && (query.isBlank() || it.content.orEmpty().contains(query.trim(), true))
                }
                .sortedByDescending { it.id }
                .map { it.id }
        }
    val messagesById = remember(state.messages) { state.messages.associateBy { it.id } }
    val highlighted = if (showSearch) hits.getOrNull(matchIndex) else if (focused) targetMessageId else null
    BackHandler(showSearch) {
        if (searchList) searchList = false
        else {
            showSearch = false
            query = ""
        }
    }
    LaunchedEffect(hits.size) {
        matchIndex = matchIndex.coerceIn(0, hits.lastIndex.coerceAtLeast(0))
    }
    var partnerState by remember(graph, roomId, myId) { mutableStateOf<ChatPartner?>(null) }
    var profileRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(graph, roomId, myId, lifecycle, state.connectionVersion) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var failures = 0
            while (true) {
                try {
                    val room = graph.chat.rooms().orEmpty().find { it.id == roomId && it.active }
                    val id = room?.partnerOrNull(myId)
                    partnerId = id
                    val loaded = if (id == null) ChatPartner(unavailable = true) else loadChatPartner(graph.users, id)
                    partnerState = loaded
                    partnerProfile = loaded.profile
                    partner = loaded.name
                    partnerInterests = emptyList()
                    if (loaded.profile == null) showPartner = false
                    profileRevision++
                    if (loaded.profile != null) {
                        try {
                            partnerInterests =
                                graph.users
                                    .interests()
                                    .filter { it.id in loaded.profile.interests.orEmpty() }
                                    .map { it.displayName }
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {}
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    val denied = exception is retrofit2.HttpException && exception.code() in setOf(403, 404, 410)
                    partnerState = ChatPartner(unavailable = denied)
                    partnerProfile = null
                    partner = partnerState!!.name
                    partnerInterests = emptyList()
                    showPartner = false
                }
                // Retry transient failures quietly, with a bounded backoff while the screen is visible.
                // Access denials still clear the profile and use the normal refresh interval.
                if (partnerState?.profile == null && partnerState?.unavailable != true) {
                    val retryDelays = longArrayOf(2_000, 5_000, 15_000, 60_000)
                    delay(retryDelays[failures.coerceAtMost(retryDelays.lastIndex)])
                    failures = (failures + 1).coerceAtMost(retryDelays.lastIndex)
                } else {
                    failures = 0
                    delay(60_000)
                }
            }
        }
    }
    LaunchedEffect(targetMessageId) {
        if (targetMessageId != null) {
            try {
                val history = graph.chat.context(roomId, targetMessageId).items.orEmpty().filter { it.roomId == roomId }
                val index = history.indexOfFirst { it.id == targetMessageId }
                context =
                    if (index < 0) emptyList()
                    else
                        history.subList(
                            (index - 25).coerceAtLeast(0),
                            (index + 26).coerceAtMost(history.size),
                        )
                if (index < 0) contextNotice = "Сообщение больше не доступно в истории."
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
                        !connection.state.value.connected ||
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
    if (!showSearch) ReadReceiptsEffect(connection, myId, messages, list)
    var typingNow by remember { mutableStateOf(false) }
    LaunchedEffect(state.typingUntil, partnerId) {
        val until = state.typingUntil.entries.firstOrNull { it.key.equals(partnerId, ignoreCase = true) }?.value ?: 0L
        typingNow = until > System.currentTimeMillis()
        if (typingNow) {
            delay((until - System.currentTimeMillis()).coerceAtLeast(0))
            typingNow = false
        }
    }
    if (showPartner)
        partnerProfile?.let { profile ->
            PartnerCard(
                profile,
                partnerInterests,
                { showPartner = false },
                avatar = {
                    key(profileRevision) {
                        ChatPartnerAvatar(
                            graph,
                            profile.nickname,
                            partnerState?.photoId,
                            size = 72.dp,
                        )
                    }
                },
            ) {
                showPartner = false
                openProfile(profile.id)
            }
        }
    LaunchedEffect(state.messages) { graph.chatPreviews.recordHistory(state.messages) }
    LaunchedEffect(messages.lastOrNull()?.id, highlighted, searchList) {
        if (searchList) return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == highlighted }
        if (index >= 0 && (showSearch || !jumped)) {
            list.scrollToItem(index)
            jumped = true
        } else if (!focused && !showSearch && messages.isNotEmpty()) {
            val nearBottom =
                (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: messages.lastIndex) >= messages.lastIndex - 3
            if (!list.isScrollInProgress && (nearBottom || messages.last().senderId == myId))
                list.scrollToItem(messages.lastIndex)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        if (showSearch) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (searchList) searchList = false
                        else {
                            showSearch = false
                            query = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад")
                }
                OutlinedTextField(
                    query,
                    { query = it.take(200) },
                    Modifier.weight(1f).focusRequester(searchFocus),
                    placeholder = { Text("Поиск в переписке") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    trailingIcon = {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить поиск") }
                    },
                )
            }
        } else
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
                key(roomId, profileRevision) {
                    ChatPartnerAvatar(
                        graph,
                        partner,
                        partnerState?.photoId,
                        Modifier.clickable(enabled = partnerProfile != null) { showPartner = true },
                        size = 40.dp,
                    )
                }
                Column(
                    Modifier.weight(1f)
                        .clickable(enabled = partnerProfile != null) { showPartner = true }
                        .padding(start = 12.dp)
                ) {
                    Text(partner, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (state.connected && typingNow) "Печатает…"
                        else if (state.connected)
                            partnerProfile?.let { chatActivityLabel(it, state.onlineUsers[it.id]) } ?: "Чат"
                        else if (state.connecting) "Подключаемся…" else "Нет соединения",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        showSearch = true
                        searchList = false
                        focused = false
                        selectedIds = emptySet()
                        editingId = null
                        query = ""
                    }
                ) {
                    Icon(
                        if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                        "Поиск в чате",
                    )
                }
            }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (selectedIds.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        selectedIds = emptySet()
                        selectionMenu = false
                    }
                ) {
                    Icon(Icons.Outlined.Close, "Снять выделение")
                }
                Text("Выбрано: ${selectedMessages.size}", Modifier.weight(1f))
                Box {
                    IconButton(onClick = { selectionMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, "Действия с сообщениями")
                    }
                    DropdownMenu(selectionMenu, onDismissRequest = { selectionMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Копировать") },
                            enabled = selectedMessages.any { !it.content.isNullOrBlank() },
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(selectedMessages.joinToString("\n") { it.content.orEmpty() })
                                )
                                selectedIds = emptySet()
                                selectionMenu = false
                            },
                        )
                        val single = selectedMessages.singleOrNull()
                        if (
                            single != null &&
                                single.senderId == myId &&
                                bloomSticker(single.content) == null &&
                                !single.content.isNullOrBlank()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Изменить") },
                                enabled = state.connected && pendingDeletes.isEmpty(),
                                onClick = {
                                    editingId = single.id
                                    selectedIds = emptySet()
                                    selectionMenu = false
                                },
                            )
                        }
                        if (selectedMessages.isNotEmpty() && selectedMessages.all { it.senderId == myId }) {
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                enabled = state.connected && pendingDeletes.isEmpty(),
                                onClick = {
                                    confirmDelete = true
                                    selectionMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
        if (loadingHistory) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (historyError != null) {
            ErrorMessage(historyError)
            TextButton(onClick = { reloadHistory++ }, enabled = !loadingHistory) {
                Text("Повторить загрузку истории")
            }
        }
        if (state.error != null) {
            ErrorMessage(state.error)
            TextButton(
                onClick = { scope.launch { connection.connect() } },
                enabled = !state.connecting,
            ) {
                Text("Подключиться")
            }
        }
        if (state.actionError != null) {
            ErrorMessage(state.actionError)
            TextButton(onClick = { connection.clearActionError() }) { Text("Закрыть") }
        }
        if (focused) {
            if (contextNotice != null)
                Text(
                    contextNotice!!,
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            TextButton(
                onClick = {
                    focused = false
                    scope.launch {
                        if (state.messages.isNotEmpty()) list.scrollToItem(state.messages.lastIndex)
                    }
                }
            ) {
                Text("К новым сообщениям")
            }
        }
        Box(
            Modifier.weight(1f)
                .fillMaxWidth()
                .bloomSwipeBack(enabled = selectedIds.isEmpty() && editingId == null && !showSearch, back = back)
        ) {
            if (showSearch && searchList) {
                LazyColumn(Modifier.fillMaxSize(), flingBehavior = rememberBloomFling()) {
                    itemsIndexed(hits, key = { _, id -> id }) { index, id ->
                        val message = messagesById.getValue(id)
                        ConversationRow(
                            ChatRowItem(
                                roomId,
                                if (message.senderId == myId) "Вы" else partner,
                                message.content,
                                message.createdAt,
                            ),
                            avatar = { PersonAvatar(it.name, size = 40.dp) },
                        ) {
                            matchIndex = index
                            searchList = false
                        }
                    }
                    if (hits.isEmpty() && !loadingHistory)
                        item {
                            Text(
                                if (query.isBlank()) "Введите текст для поиска" else "Ничего не найдено",
                                Modifier.padding(24.dp),
                            )
                        }
                }
            } else if (messages.isEmpty() && !loadingHistory && historyError == null)
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
                    flingBehavior = rememberBloomFling(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
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
                        MessageBubble(
                            graph,
                            message,
                            message.senderId == myId,
                            message.id == highlighted,
                            selected = message.id in selectedIds,
                            selectionActive = selectedIds.isNotEmpty(),
                            select = {
                                if (message.id in selectedIds) selectedIds = selectedIds - message.id
                                else selectedIds = selectedIds + message.id
                            },
                            startEditing = { editingId = message.id },
                            edit = if (state.connected) { text -> connection.edit(message.id, text) } else null,
                            delete =
                                if (state.connected) {
                                    { connection.delete(message.id) }
                                } else null,
                        )
                    }
                }
            if (showSearch && !searchList) {
                Column(
                    Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledIconButton(
                        onClick = { matchIndex++ },
                        enabled = matchIndex < hits.lastIndex,
                        modifier = Modifier.size(44.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowUp, "Предыдущее сообщение")
                    }
                    FilledIconButton(
                        onClick = { matchIndex-- },
                        enabled = matchIndex > 0,
                        modifier = Modifier.size(44.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, "Следующее сообщение")
                    }
                }
            }
            if (list.canScrollForward && !showSearch)
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            list.scrollToItem(messages.lastIndex.coerceAtLeast(0))
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(Icons.Outlined.KeyboardArrowDown, "Последние сообщения")
                }
        }
        if (showSearch) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (loadingHistory) "Загрузка истории…"
                        else if (hits.isEmpty()) "Нет совпадений" else "${matchIndex + 1} из ${hits.size}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(onClick = { searchList = !searchList }) { Text(if (searchList) "В чате" else "Списком") }
                }
            }
        } else
            composerState.SaveableStateProvider(roomId) {
                ChatComposer(
                    graph,
                    roomId,
                    myId,
                    connection,
                    state,
                    messages.find { it.id == editingId && it.deletedAt == null },
                    { editingId = null },
                )
            }
    }
}
