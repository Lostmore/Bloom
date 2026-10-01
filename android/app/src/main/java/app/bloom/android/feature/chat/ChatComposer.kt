package app.bloom.android.feature.chat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Attachment
import app.bloom.android.core.model.ChatMessage
import app.bloom.android.core.model.MediaCapabilities
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.ErrorMessage
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun confirmsSend(
    message: ChatMessage,
    userId: String,
    clientId: String,
    text: String,
    hasPhotos: Boolean,
    after: Long,
): Boolean =
    message.senderId == userId &&
        (message.clientMessageId == clientId ||
            (!hasPhotos && message.clientMessageId == null && message.id > after && message.content == text.trim()))

@Composable
fun ChatComposer(
    graph: AppGraph,
    roomId: Long,
    myId: String,
    connection: ChatConnection,
    state: ChatState,
) {
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var photos by rememberSaveable { mutableStateOf(listOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var sentAfter by remember { mutableLongStateOf(0) }
    var attemptKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var signature by rememberSaveable { mutableStateOf("") }
    val uploadKeys = rememberSaveable { hashMapOf<String, String>() }
    val uploaded = remember { hashMapOf<String, Attachment>() }
    var capabilities by remember { mutableStateOf<MediaCapabilities?>(null) }
    var checking by remember { mutableStateOf(true) }
    var checkAgain by remember { mutableIntStateOf(0) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    LaunchedEffect(draft.text, state.connected) {
        if (!state.connected) return@LaunchedEffect
        delay(500)
        connection.typing(draft.text.isNotBlank())
        if (draft.text.isNotBlank()) {
            delay(3000)
            connection.typing(false)
        }
    }
    LaunchedEffect(state.errorVersion) {
        if (state.actionError != null && pending != null) {
            pending = null
            busy = false
            error = "Сервер отклонил действие. Черновик сохранён."
        }
    }
    LaunchedEffect(roomId, checkAgain) {
        checking = true
        try {
            val media = graph.media.capabilities()
            val chat = graph.chat.capabilities()
            capabilities = media.takeIf {
                it.privateChatAttachments && chat.attachmentMessages && it.maxUploadBytes > 0 && it.maxAttachments > 0
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            capabilities = null
        } finally {
            checking = false
        }
    }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { selected ->
            val accepted = selected.mapNotNull { uri ->
                try {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    uri.toString()
                } catch (_: SecurityException) {
                    error = "Не удалось открыть фото. Выбери его ещё раз."
                    null
                }
            }
            photos = (photos + accepted).distinct().take(capabilities?.maxAttachments?.coerceIn(1, 6) ?: 6)
        }
    LaunchedEffect(state.messages, pending) {
        val key = pending ?: return@LaunchedEffect
        if (
            state.messages.any {
                confirmsSend(it, myId, key, draft.text, photos.isNotEmpty(), sentAfter)
            }
        ) {
            draft = TextFieldValue()
            photos = emptyList()
            pending = null
            busy = false
            signature = ""
            uploaded.clear()
            uploadKeys.clear()
        }
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            delay(15_000)
            pending = null
            busy = false
            error = "Подтверждение не пришло. Черновик сохранён — проверь историю перед повтором."
        }
    }
    Column {
        if (photos.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(photos, key = { _, uri -> uri }) { index, uri ->
                    Box(Modifier.size(84.dp)) {
                        ChatPhoto(
                            graph,
                            PhotoSource(uri, true),
                            Modifier.fillMaxSize().clickable { viewing = index },
                        )
                        IconButton(
                            onClick = { photos = photos - uri },
                            enabled = !busy,
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                "Убрать фото ${index + 1}",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (capabilities == null) {
                Text(
                    if (checking) "Проверяем возможность отправки фото…"
                    else "Отправка фото пока недоступна на сервере. Фото не отправлены.",
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!checking) TextButton(onClick = { checkAgain++ }) { Text("Проверить доступность") }
            }
        }
        ErrorMessage(error)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        MessageComposer(
            draft,
            {
                draft = it
                error = null
            },
            !busy,
            state.connected &&
                (draft.text.isNotBlank() || photos.isNotEmpty()) &&
                (photos.isEmpty() || capabilities != null),
            send = {
                scope.launch {
                    if (busy) return@launch
                    busy = true
                    error = null
                    val nextSignature = draft.text.trim() + "\u0000" + photos.joinToString("\u0000")
                    if (signature != nextSignature) {
                        signature = nextSignature
                        attemptKey = UUID.randomUUID().toString()
                    }
                    try {
                        val attachments = photos.map { uri ->
                            uploaded[uri]
                                ?: uploadChatPhoto(
                                        graph,
                                        resolver,
                                        uri,
                                        roomId,
                                        uploadKeys.getOrPut(uri) { UUID.randomUUID().toString() },
                                        checkNotNull(capabilities),
                                    )
                                    .also { uploaded[uri] = it }
                        }
                        sentAfter = state.messages.lastOrNull()?.id ?: 0
                        if (connection.send(draft.text, attachments)) pending = attemptKey
                        else {
                            busy = false
                            error = "Связь прервалась. Черновик сохранён."
                        }
                    } catch (exception: CancellationException) {
                        busy = false
                        throw exception
                    } catch (exception: Exception) {
                        busy = false
                        error =
                            if (exception is IllegalArgumentException) exception.message else exception.userMessage()
                    }
                }
            },
            attach = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
    viewing
        ?.takeIf { it < photos.size }
        ?.let {
            PhotoViewer(graph, photos.map { uri -> PhotoSource(uri, true) }, it) { viewing = null }
        }
}
