package app.bloom.android.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.ChatMessage
import kotlinx.coroutines.delay

@Composable
fun EditMessageComposer(message: ChatMessage, connection: ChatConnection, state: ChatState, close: () -> Unit) {
    var draft by
        rememberSaveable(message.id, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(message.content.orEmpty(), TextRange(message.content.orEmpty().length)))
        }
    var pending by remember(message.id) { mutableStateOf<String?>(null) }
    var error by remember(message.id) { mutableStateOf<String?>(null) }
    BackHandler { close() }
    LaunchedEffect(state.messages, pending) {
        val current = state.messages.find { it.id == message.id }
        if (current?.deletedAt != null || (pending != null && current?.content == pending)) close()
    }
    LaunchedEffect(state.errorVersion) {
        if (pending != null && state.actionError != null) {
            error = "Не удалось сохранить. Текст оставлен в редакторе."
            pending = null
        }
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            delay(15_000)
            pending = null
            error = "Подтверждение не получено. Проверь соединение и повтори."
        }
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp)) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                    "Редактирование",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(message.content.orEmpty(), maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = close) { Icon(Icons.Outlined.Close, "Отменить редактирование") }
        }
        error?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error) }
        if (pending != null) LinearProgressIndicator(Modifier.fillMaxWidth())
        MessageComposer(
            draft,
            {
                draft = it
                error = null
            },
            pending == null,
            state.connected && draft.text.isNotBlank() && draft.text.trim() != message.content,
            send = {
                if (connection.edit(message.id, draft.text)) pending = draft.text.trim()
                else error = "Нет соединения. Текст сохранён в редакторе."
            },
            attach = {},
            editing = true,
        )
    }
}
