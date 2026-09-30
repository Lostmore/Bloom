package app.bloom.android.feature.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Match
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun MatchesScreen(
    graph: AppGraph,
    myId: String,
    openProfile: (String) -> Unit,
    openChat: (Long) -> Unit,
) {
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<Match?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    suspend fun load(after: String? = null) {
        loading = true
        error = null
        try {
            val page = graph.interactions.matches(after)
            matches = ((if (after == null) emptyList() else matches) + page.items).distinctBy { it.id }
            cursor = page.nextCursor
            page.items.forEach { match ->
                val partner = match.partner(myId)
                try {
                    names = names + (partner to graph.users.profile(partner).nickname)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    /* A now-hidden profile must not hide the match itself. */
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }
    LaunchedEffect(reload) { load() }
    MatchesContent(
        matches.map { MatchTileItem(it.id, names[it.partner(myId)] ?: "Bloom") },
        loading,
        busy,
        error,
        cursor != null,
        refresh = { if (!loading) reload++ },
        loadMore = { scope.launch { load(cursor) } },
        openProfile = { id -> matches.find { it.id == id }?.let { openProfile(it.partner(myId)) } },
        chat = { id ->
            matches
                .find { it.id == id }
                ?.let { match ->
                    scope.launch {
                        busy = true
                        try {
                            openChat(graph.chat.create(mapOf("user2_id" to match.partner(myId))).id)
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            error = exception.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                }
        },
        remove = { id -> remove = matches.find { it.id == id } },
    )
    remove?.let { match ->
        AlertDialog(
            onDismissRequest = { if (!busy) remove = null },
            title = { Text("Убрать совпадение?") },
            text = { Text("Оно исчезнет из списка. Сразу восстановить симпатию не получится.") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                graph.interactions.unmatch(match.id)
                                matches = matches.filterNot { it.id == match.id }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                busy = false
                                remove = null
                            }
                        }
                    },
                ) {
                    Text("Убрать")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { remove = null }) { Text("Отмена") }
            },
        )
    }
}
