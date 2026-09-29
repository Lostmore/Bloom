package app.bloom.android.feature.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Взаимная симпатия",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(onClick = { reload++ }, enabled = !loading) {
                    Icon(Icons.Outlined.Refresh, "Обновить")
                }
            }
            Text(
                "Новые истории начинаются с «привет».",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { ErrorMessage(error) }
        if (loading && matches.isEmpty()) item { LoadingBloom(Modifier.height(260.dp)) }
        else if (matches.isEmpty() && error == null)
            item {
                EmptyBloom(
                    "Пока без совпадений",
                    "Когда симпатия окажется взаимной, человек появится здесь.",
                    icon = Icons.Outlined.FavoriteBorder,
                )
            }
        items(matches, key = { it.id }) { match ->
            val partner = match.partner(myId)
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { openProfile(partner) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BloomMark(Modifier.size(38.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                names[partner] ?: "Ваше совпадение",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Открыть анкету →",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { remove = match }, enabled = !busy) {
                            Icon(Icons.Outlined.Close, "Убрать совпадение")
                        }
                    }
                    BloomButton(
                        "Сказать привет",
                        {
                            scope.launch {
                                busy = true
                                try {
                                    openChat(graph.chat.create(mapOf("user2_id" to partner)).id)
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    error = exception.userMessage()
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                    )
                }
            }
        }
        if (cursor != null)
            item {
                TextButton(enabled = !loading, onClick = { scope.launch { load(cursor) } }) {
                    Text("Показать ещё")
                }
            }
        if (error != null) item { TextButton(onClick = { reload++ }) { Text("Повторить") } }
    }
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
