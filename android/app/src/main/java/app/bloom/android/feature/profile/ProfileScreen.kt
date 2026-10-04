package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    graph: AppGraph,
    id: String,
    myId: String,
    back: () -> Unit,
    openChat: (Long) -> Unit,
) {
    var profile by remember(id) { mutableStateOf<Profile?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var matched by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var blockDialog by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    // Keep a failed request's key: a network timeout may occur after the server commits the
    // reaction.
    val pendingKeys = rememberSaveable(id) { hashMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(id, retry) {
        loading = true
        error = null
        try {
            profile = graph.users.profile(id)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }
    val react: (String) -> Unit = { action ->
        scope.launch {
            busy = true
            error = null
            try {
                val result =
                    graph.interactions.react(
                        id,
                        action,
                        pendingKeys.getOrPut(action) { UUID.randomUUID().toString() },
                    )
                pendingKeys.remove(action)
                matched = result.matchId != null
                feedback =
                    when (action) {
                        "skip" -> "Анкета пропущена"
                        "super-interest" -> "Особый интерес отправлен ✨"
                        else -> "Симпатия отправлена ♥"
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                error = exception.userMessage()
            } finally {
                busy = false
            }
        }
    }
    val startChat: () -> Unit = {
        scope.launch {
            busy = true
            error = null
            try {
                val room = graph.chat.create(mapOf("user2_id" to id))
                if (room.active) openChat(room.id) else error = "Эта переписка недоступна."
                matched = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                error = exception.userMessage()
                matched = false
            } finally {
                busy = false
            }
        }
    }
    ProfileCoverHost(profile) { coverModifier, openCover ->
        Column(
            coverModifier
                .fillMaxSize()
                .bloomSwipeBack(enabled = !busy, back = back)
                .verticalScroll(rememberScrollState(), flingBehavior = rememberBloomFling())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
                Text("Знакомство", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (id != myId)
                    IconButton(onClick = { blockDialog = true }) {
                        Icon(Icons.Outlined.Block, "Заблокировать")
                    }
            }
            if (loading) LoadingBloom(Modifier.height(320.dp))
            else if (profile != null) {
                ProfileHero(profile!!, openCover = openCover)
                if (id != myId) {
                    Button(onClick = startChat, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Подождите…" else "Написать")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("skip", "super-interest", "like").forEachIndexed { index, action ->
                            FilledTonalIconButton(
                                onClick = { react(action) },
                                enabled = !busy,
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    listOf(
                                        Icons.Outlined.Close,
                                        Icons.Outlined.StarOutline,
                                        Icons.Outlined.FavoriteBorder,
                                    )[index],
                                    listOf("Пропустить", "Особый интерес", "Нравится")[index],
                                    Modifier.size(30.dp),
                                )
                            }
                        }
                    }
                }
                feedback?.let {
                    Text(
                        it,
                        Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ErrorMessage(error)
            if (!loading && profile == null) TextButton(onClick = { retry++ }) { Text("Повторить") }
        }
    }
    if (matched)
        AlertDialog(
            onDismissRequest = { matched = false },
            icon = { BloomMark(Modifier.size(64.dp)) },
            title = { Text("Это совпадение!") },
            text = { Text("Вы понравились друг другу. Самое время сказать привет.") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = startChat,
                ) {
                    Text("Написать сообщение")
                }
            },
            dismissButton = { TextButton(onClick = { matched = false }) { Text("Позже") } },
        )
    if (blockDialog)
        AlertDialog(
            onDismissRequest = { blockDialog = false },
            title = { Text("Заблокировать профиль?") },
            text = {
                Text("Вы больше не будете видеть анкеты друг друга. Совпадение будет закрыто.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                graph.users.block(id)
                                back()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                busy = false
                                blockDialog = false
                            }
                        }
                    },
                ) {
                    Text("Заблокировать")
                }
            },
            dismissButton = { TextButton(onClick = { blockDialog = false }) { Text("Отмена") } },
        )
}
