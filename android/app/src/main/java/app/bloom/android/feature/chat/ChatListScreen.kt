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
    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var nameFailures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
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
            results = ((if (cursor == null) emptyList() else results) + page.items.orEmpty()).distinctBy { it.id }
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
    LaunchedEffect(reload, live.connected, live.roomsVersion) {
        loading = true
        error = null
        try {
            rooms = graph.chat.rooms().orEmpty().filter { it.active }.distinctBy { it.id }
            val limit = Semaphore(4)
            coroutineScope {
                rooms
                    .map { room ->
                        async {
                            limit.withPermit {
                                val id = room.partner(myId)
                                try {
                                    names = names + (id to graph.users.profile(id).nickname)
                                    nameFailures = nameFailures - id
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    names = names - id
                                    nameFailures =
                                        nameFailures +
                                            (id to
                                                (exception is retrofit2.HttpException &&
                                                    exception.code() in listOf(403, 404)))
                                }
                            }
                        }
                    }
                    .awaitAll()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }
    val items =
        rooms
            .map { room ->
                val id = room.partner(myId)
                val name =
                    names[id]
                        ?: when (nameFailures[id]) {
                            true -> "Профиль недоступен"
                            false -> "Имя не загрузилось"
                            null -> "Собеседник"
                        }
                chatRoomPreview(room, previews[room.id], myId, name)
            }
            .sortedWith(compareByDescending<ChatRowItem> { chatTimestamp(it.timestamp) }.thenByDescending { it.id })
    ChatsContent(
        items,
        loading,
        error
            ?: if (nameFailures.values.any { !it })
                "Не удалось получить часть имён из Users. Потяни список вниз, чтобы повторить."
            else null,
        { if (!loading) reload++ },
        openChat,
        searchResults =
            if (cachedSearch)
                history
                    .filter {
                        it.content.orEmpty().contains(query.trim(), true) && rooms.any { room -> room.id == it.roomId }
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
    )
}
