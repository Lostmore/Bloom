package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Interest
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.ErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsDialog(
    graph: AppGraph,
    initial: List<String>,
    dismiss: () -> Unit,
    saved: (Profile) -> Unit,
) {
    var catalog by remember { mutableStateOf<List<Interest>>(emptyList()) }
    var selected by remember { mutableStateOf(initial.toSet()) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(reload) {
        busy = true
        try {
            catalog = graph.users.interests()
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
        title = { Text("Что тебя увлекает?") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Выбери до 10 интересов — так проще найти общее.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    catalog.forEach { interest ->
                        FilterChip(
                            interest.id in selected,
                            {
                                selected =
                                    if (interest.id in selected) selected - interest.id
                                    else if (selected.size < 10) selected + interest.id else selected
                            },
                            label = { Text(interest.name) },
                        )
                    }
                }
                ErrorMessage(error)
                if (catalog.isEmpty() && !busy) TextButton(onClick = { reload++ }) { Text("Повторить") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && catalog.isNotEmpty(),
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            graph.users.interests(mapOf("interests" to selected.toList()))
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
                Text(if (busy) "Загрузка…" else "Готово")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Закрыть") } },
    )
}
