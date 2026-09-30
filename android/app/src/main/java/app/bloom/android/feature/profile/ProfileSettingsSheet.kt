package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsSheet(
    theme: String,
    setTheme: (String) -> Unit,
    busy: Boolean,
    error: String?,
    dismiss: () -> Unit,
    logout: () -> Unit,
    delete: () -> Unit,
    photos: () -> Unit,
    privacy: () -> Unit,
    refresh: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineMedium)
            Text("Оформление", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mapOf("light" to "Светлое", "dark" to "Тёмное", "system" to "Системное").forEach { (key, label) ->
                    FilterChip(theme == key, { setTheme(key) }, label = { Text(label) })
                }
            }
            ErrorMessage(error)
            OutlinedButton(onClick = privacy, modifier = Modifier.fillMaxWidth()) { Text("Приватность") }
            OutlinedButton(onClick = refresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Обновить анкету")
            }
            OutlinedButton(onClick = photos, modifier = Modifier.fillMaxWidth()) { Text("Фото · локальный черновик") }
            HorizontalDivider()
            TextButton(onClick = logout, enabled = !busy) { Text("Выйти из аккаунта") }
            TextButton(onClick = delete, enabled = !busy) {
                Text("Удалить аккаунт", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
