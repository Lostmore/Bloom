package app.bloom.android.feature.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyProfileScreen(
    graph: AppGraph,
    me: Profile,
    update: (Profile) -> Unit,
    theme: String,
    setTheme: (String) -> Unit,
    logout: () -> Unit,
) {
    var edit by remember { mutableStateOf(false) }
    var interests by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Мой Bloom", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            IconButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            update(graph.users.me())
                            error = null
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            error = exception.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Icon(Icons.Outlined.Refresh, "Обновить анкету")
            }
            IconButton(
                onClick = {
                    val share =
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                "bloom://profile/${me.id}",
                            )
                    context.startActivity(Intent.createChooser(share, "Поделиться анкетой"))
                }
            ) {
                Icon(Icons.Outlined.IosShare, "Поделиться анкетой")
            }
        }
        ProfileHero(me)
        BloomButton("Редактировать профиль", { edit = true })
        OutlinedButton(onClick = { interests = true }, Modifier.fillMaxWidth()) {
            Text("Мои интересы")
        }
        OutlinedButton(onClick = { privacy = true }, Modifier.fillMaxWidth()) {
            Text("Приватность")
        }
        Text("Твой комфорт", style = MaterialTheme.typography.titleLarge)
        Text("Оформление", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mapOf("light" to "Светлое", "dark" to "Тёмное", "system" to "Системное").forEach { (key, label) ->
                FilterChip(theme == key, { setTheme(key) }, label = { Text(label) })
            }
        }
        Text(
            "Фотографии и вложения появятся после подключения защищённого хранения.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorMessage(error)
        TextButton(onClick = logout, enabled = !busy) { Text("Выйти из аккаунта") }
        TextButton(onClick = { delete = true }, enabled = !busy) {
            Text("Удалить аккаунт", color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Bloom · больше, чем знакомства",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (edit)
        EditProfileDialog(
            graph,
            me,
            { edit = false },
            {
                update(it)
                edit = false
            },
        )
    if (interests)
        InterestsDialog(graph, me.interests.orEmpty(), { interests = false }) { updated ->
            update(updated)
            interests = false
        }
    if (privacy)
        PrivacyDialog(graph, { privacy = false }) { updated ->
            update(updated)
            privacy = false
        }
    if (delete)
        AlertDialog(
            onDismissRequest = { if (!busy) delete = false },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Анкета будет удалена. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                graph.users.delete()
                                logout()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                busy = false
                                delete = false
                            }
                        }
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { delete = false }) { Text("Оставить") }
            },
        )
}
