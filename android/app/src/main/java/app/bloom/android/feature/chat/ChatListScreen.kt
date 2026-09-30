package app.bloom.android.feature.chat

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatRoom
import app.bloom.android.core.network.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun ChatListScreen(graph: AppGraph, myId: String, openChat: (Long) -> Unit) {
    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val previews by graph.chatPreviews.messages.collectAsStateWithLifecycle()
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
    ChatsContent(items, loading, error, { if (!loading) reload++ }, openChat)
}
