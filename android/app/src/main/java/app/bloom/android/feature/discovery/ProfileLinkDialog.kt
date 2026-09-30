package app.bloom.android.feature.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.ErrorMessage
import java.util.UUID

@Composable
fun ProfileLinkDialog(dismiss: () -> Unit, openProfile: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Открыть анкету") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Вставь ссылку Bloom, которой с тобой поделились.")
                OutlinedTextField(
                    value,
                    {
                        value = it
                        error = null
                    },
                    singleLine = true,
                    label = { Text("Ссылка на профиль") },
                    placeholder = { Text("bloom://profile/…") },
                )
                ErrorMessage(error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = runCatching {
                        UUID.fromString(value.trim().removePrefix("bloom://profile/")).toString()
                    }
                        .getOrNull()
                    if (id == null) error = "Проверь ссылку на профиль."
                    else {
                        dismiss()
                        openProfile(id)
                    }
                }
            ) {
                Text("Открыть")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } },
    )
}
