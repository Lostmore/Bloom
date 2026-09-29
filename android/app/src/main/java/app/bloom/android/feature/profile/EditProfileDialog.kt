package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.model.ProfilePatch
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.ErrorMessage
import app.bloom.android.core.ui.GoalPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    graph: AppGraph,
    profile: Profile,
    dismiss: () -> Unit,
    saved: (Profile) -> Unit,
) {
    var name by remember { mutableStateOf(profile.nickname) }
    var bio by remember { mutableStateOf(profile.bio.orEmpty()) }
    var city by remember { mutableStateOf(profile.city.orEmpty()) }
    var goals by remember { mutableStateOf(profile.searchModes.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) dismiss() },
        title = { Text("О тебе") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    name,
                    { name = it.take(40) },
                    label = { Text("Имя") },
                    singleLine = true,
                )
                OutlinedTextField(
                    bio,
                    { bio = it.take(500) },
                    label = { Text("О себе") },
                    minLines = 3,
                )
                OutlinedTextField(
                    city,
                    { city = it.take(100) },
                    label = { Text("Город") },
                    singleLine = true,
                )
                GoalPicker(goals) { goals = it }
                ErrorMessage(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && name.isNotBlank() && goals.isNotEmpty(),
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            saved(graph.users.update(ProfilePatch(profile.version, name.trim(), bio, city, goals)))
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
                Text(if (busy) "Сохраняем…" else "Сохранить")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Отмена") } },
    )
}
