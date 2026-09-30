package app.bloom.android.feature.chat

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
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
    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            rooms = graph.chat.rooms().orEmpty().filter { it.active }
            val limit = Semaphore(4)
            coroutineScope {
                rooms
                    .map { room ->
                        async {
                            limit.withPermit {
                                val id = room.partner(myId)
                                try {
                                    names = names + (id to graph.users.profile(id).nickname)
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (_: Exception) {
                                    /* Hidden profiles must not hide conversations. */
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
        rooms.sortedWith(compareByDescending<ChatRoom> { previews[it.id]?.id ?: 0 }.thenByDescending { it.id }).map {
            room ->
            val last = previews[room.id]
            ChatRowItem(
                room.id,
                names[room.partner(myId)] ?: "Собеседник",
                last?.let {
                    (if (it.senderId == myId) "Вы: " else "") + (it.content?.takeIf(String::isNotBlank) ?: "Вложение")
                },
                last?.createdAt,
            )
        }
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
