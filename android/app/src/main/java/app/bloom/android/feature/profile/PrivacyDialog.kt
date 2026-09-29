package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Privacy
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.ErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PrivacyDialog(graph: AppGraph, dismiss: () -> Unit, saved: (Profile) -> Unit) {
    var privacy by remember { mutableStateOf<Privacy?>(null) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(reload) {
        busy = true
        try {
            privacy = graph.users.privacy()
            error = null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            busy = false
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) dismiss() },
        title = { Text("Приватность") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                privacy?.let { current ->
                    PrivacySwitch("Показывать мою анкету", current.discoverable) {
                        privacy = current.copy(discoverable = it)
                    }
                    PrivacySwitch("Показывать расстояние", current.showDistance) {
                        privacy = current.copy(showDistance = it)
                    }
                    PrivacySwitch("Показывать время активности", current.showLastSeen) {
                        privacy = current.copy(showLastSeen = it)
                    }
                }
                ErrorMessage(error)
                if (privacy == null && !busy) TextButton(onClick = { reload++ }) { Text("Повторить") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && privacy != null,
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            graph.users.privacy(mapOf("privacy" to privacy!!))
                            saved(graph.users.me())
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
                Text(if (busy) "Загрузка…" else "Сохранить")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun PrivacySwitch(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, change)
    }
}
