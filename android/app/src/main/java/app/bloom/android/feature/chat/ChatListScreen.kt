package app.bloom.android.feature.chat

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.model.ChatRoom
import app.bloom.android.core.network.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun ChatListScreen(
    graph: AppGraph,
    myId: String,
    openChat: (Long) -> Unit,
    openMessage: (Long, Long) -> Unit = { room, _ -> openChat(room) },
) {
    var rooms by remember(graph, myId) { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var partners by remember(graph, myId) { mutableStateOf<Map<String, ChatPartner>>(emptyMap()) }
    var profileRevision by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val connection = remember(graph) { ChatConnection(graph.http, graph.sessions, graph.baseUrl) }
    val live by connection.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(connection, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                connection.connect()
                while (true) {
                    delay(30_000)
                    if (
                        !connection.state.value.connected ||
                            (graph.sessions.session.value?.expiresAt ?: Long.MAX_VALUE) <
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
    LaunchedEffect(live.historyVersion, live.connected, reload) {
        for (id in live.roomsNeedingHistory) {
            try {
                val revision = live.historyVersion
                connection.mergeHistory(graph.chat.history(id).orEmpty(), revision, id)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                error = "Не удалось обновить изменённые сообщения. Потяни список вниз для повтора."
            }
        }
    }
    LaunchedEffect(live.messages) {
        graph.chatPreviews.recordHistory(live.messages)
        if (live.messages.lastOrNull()?.roomId?.let { id -> rooms.none { it.id == id } } == true) reload++
    }
    val previews by graph.chatPreviews.messages.collectAsStateWithLifecycle()
    val history by graph.chatPreviews.history.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchNotice by remember { mutableStateOf<String?>(null) }
    var cachedSearch by remember { mutableStateOf(false) }
    LaunchedEffect(query, cursor) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            searchNotice = null
            cachedSearch = false
            return@LaunchedEffect
        }
        searching = true
        try {
            if (cursor == null) delay(350)
            val page = graph.chat.search(query.trim(), cursor)
            results =
                ((if (cursor == null) emptyList() else results) + page.items.orEmpty()).distinctBy {
                    it.id
                }
            nextCursor = page.nextCursor
            searchNotice = null
            cachedSearch = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            cachedSearch = cursor == null
            nextCursor = null
            searchNotice =
                if (cachedSearch) "Поиск по всей истории недоступен. Показаны совпадения в загруженных сообщениях."
                else "Не удалось загрузить следующую страницу. Измени запрос, чтобы повторить поиск."
        } finally {
            searching = false
        }
    }
    LaunchedEffect(graph, myId, reload, live.connected, live.roomsVersion) {
        loading = true
        error = null
        try {
            rooms =
                graph.chat.rooms().orEmpty().filter { it.active && it.partnerOrNull(myId) != null }.distinctBy { it.id }
            val ids = rooms.map { it.partner(myId) }.distinct()
            partners = partners.filterKeys { it in ids }
            val limit = Semaphore(4)
            coroutineScope {
                ids.map { id ->
                        async {
                            limit.withPermit {
                                val loaded = loadChatPartner(graph.users, id)
                                partners = partners + (id to loaded)
                            }
                        }
                    }
                    .awaitAll()
            }
            profileRevision++
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            error = exception.userMessage()
            if (exception is retrofit2.HttpException && exception.code() in setOf(401, 403)) {
                rooms = emptyList()
                partners = emptyMap()
            }
        } finally {
            loading = false
        }
    }
    LaunchedEffect(partners, loading, lifecycle) {
        if (!loading && partners.values.any { it.profile == null }) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(60_000)
                reload++
            }
        }
    }
    val items =
        rooms
            .map { room ->
                val id = room.partner(myId)
                val partner = partners[id]
                chatRoomPreview(room, previews[room.id], myId, partner?.name ?: "Собеседник")
                    .copy(photoId = partner?.photoId)
            }
            .sortedWith(compareByDescending<ChatRowItem> { chatTimestamp(it.timestamp) }.thenByDescending { it.id })
    ChatsContent(
        items,
        loading,
        error,
        { if (!loading) reload++ },
        openChat,
        searchResults =
            if (cachedSearch)
                history
                    .filter {
                        it.deletedAt == null &&
                            it.content.orEmpty().contains(query.trim(), true) &&
                            rooms.any { room -> room.id == it.roomId }
                    }
                    .sortedByDescending { it.id }
            else results,
        searching = searching,
        searchNotice = searchNotice,
        queryChanged = { value ->
            if (query != value) {
                query = value
                cursor = null
                nextCursor = null
                results = emptyList()
                cachedSearch = false
            }
        },
        openMessage = openMessage,
        moreResults = nextCursor?.let { next -> { cursor = next } },
        avatar = { item ->
            key(item.id, profileRevision) {
                ChatPartnerAvatar(graph, item.name, item.photoId)
            }
        },
    )
}
