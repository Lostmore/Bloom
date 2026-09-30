package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.model.ProfilePatch
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(graph: AppGraph, profile: Profile, dismiss: () -> Unit, saved: (Profile) -> Unit) {
    var name by remember { mutableStateOf(profile.nickname) }
    var bio by remember { mutableStateOf(profile.bio.orEmpty()) }
    var city by remember { mutableStateOf(profile.city.orEmpty()) }
    var goals by remember { mutableStateOf(profile.searchModes.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = { if (!busy) dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = dismiss, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад")
                    }
                    Text("Редактировать", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        enabled = !busy && name.isNotBlank() && goals.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                busy = true
                                error = null
                                try {
                                    saved(
                                        graph.users.update(ProfilePatch(profile.version, name.trim(), bio, city, goals))
                                    )
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
                        Text(if (busy) "Сохраняем…" else "Готово")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    PersonAvatar(name, Modifier.align(Alignment.CenterHorizontally), size = 88.dp)
                    Text("Пусть увидят тебя", style = MaterialTheme.typography.headlineMedium)
                    Text("Детали помогают начать хороший разговор.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        name,
                        { name = it.take(40) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Имя") },
                        enabled = !busy,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    OutlinedTextField(
                        bio,
                        { bio = it.take(500) },
                        Modifier.fillMaxWidth(),
                        label = { Text("О себе") },
                        enabled = !busy,
                        minLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        supportingText = { Text("${bio.length}/500") },
                    )
                    OutlinedTextField(
                        city,
                        { city = it.take(100) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Город") },
                        enabled = !busy,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Text("Я здесь, чтобы…", style = MaterialTheme.typography.titleLarge)
                    GoalPicker(goals) { if (!busy) goals = it }
                    ErrorMessage(error)
                }
            }
        }
    }
}
