package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayStep(value: String, change: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text(
            date?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Выбрать дату рождения",
            style = MaterialTheme.typography.titleLarge,
        )
    }
    if (show) {
        val today = LocalDate.now()
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    (date ?: today.minusYears(18)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                yearRange = (today.year - 120)..(today.year - 18),
                initialDisplayMode = DisplayMode.Input,
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val candidate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            return !candidate.isAfter(today.minusYears(18)) &&
                                !candidate.isBefore(today.minusYears(120))
                        }
                    },
            )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        state.selectedDateMillis?.let {
                            change(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        }
                        show = false
                    },
                ) {
                    Text("Готово")
                }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Отмена") } },
        ) {
            DatePicker(state)
        }
    }
}
