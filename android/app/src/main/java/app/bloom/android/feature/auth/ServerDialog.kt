package app.bloom.android.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.*

@Composable
internal fun ServerDialog(current: String, dismiss: () -> Unit, apply: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Сервер разработки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Телефон: адрес компьютера в одной сети. Эмулятор: 10.0.2.2. В конце — /api/v1/.")
                OutlinedTextField(
                    value,
                    { value = it },
                    label = { Text("URL Gateway") },
                    singleLine = true,
                )
                ErrorMessage(error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        apply(value)
                        dismiss()
                    } catch (exception: IllegalArgumentException) {
                        error = exception.message
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } },
    )
}
