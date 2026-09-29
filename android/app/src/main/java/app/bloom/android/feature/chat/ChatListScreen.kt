package app.bloom.android.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatRoom
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException

@Composable
fun ChatListScreen(graph: AppGraph, myId: String, openChat: (Long) -> Unit) {
    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            rooms = graph.chat.rooms().orEmpty().filter { it.active }
            rooms.forEach { room ->
                val partner = room.partner(myId)
                try {
                    names = names + (partner to graph.users.profile(partner).nickname)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    /* The conversation remains usable if the profile is hidden. */
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
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row {
                Text(
                    "Твои разговоры",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(onClick = { reload++ }, enabled = !loading) {
                    Icon(Icons.Outlined.Refresh, "Обновить")
                }
            }
            Text(
                "Маленькое сообщение. Начало большой истории.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { ErrorMessage(error) }
        if (loading && rooms.isEmpty()) item { LoadingBloom(Modifier.height(280.dp)) }
        else if (rooms.isEmpty() && error == null)
            item {
                EmptyBloom(
                    "Кому скажем привет?",
                    "Начни разговор из ваших совпадений — и он появится здесь.",
                    icon = Icons.Outlined.ChatBubbleOutline,
                )
            }
        items(rooms, key = { it.id }) { room ->
            Card(Modifier.fillMaxWidth().clickable { openChat(room.id) }) {
                ListItem(
                    headlineContent = { Text(names[room.partner(myId)] ?: "Разговор #${room.id}") },
                    supportingContent = { Text("Открыть переписку") },
                    leadingContent = { BloomMark(Modifier.size(40.dp)) },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                )
            }
        }
        if (error != null) item { TextButton(onClick = { reload++ }) { Text("Повторить") } }
    }
}
