package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.CreateProfile
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import java.time.LocalDate
import java.time.Period
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(graph: AppGraph, onCreated: (Profile) -> Unit, logout: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("OTHER") }
    var goals by remember { mutableStateOf(setOf("FRIENDS")) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BloomBrand()
        Spacer(Modifier.height(12.dp))
        Text("Давай знакомиться", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Немного о тебе — чтобы встретить своих людей.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            name,
            { name = it.take(40) },
            Modifier.fillMaxWidth(),
            label = { Text("Как тебя зовут?") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            birthday,
            { birthday = it.take(10) },
            Modifier.fillMaxWidth(),
            label = { Text("Дата рождения") },
            placeholder = { Text("2000-05-21") },
            supportingText = { Text("Год-месяц-день. Bloom доступен с 18 лет.") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        Text("Пол", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mapOf("FEMALE" to "Женский", "MALE" to "Мужской", "OTHER" to "Другой").forEach { (key, label) ->
                FilterChip(gender == key, { gender = key }, label = { Text(label) })
            }
        }
        Text("Что ты ищешь в Bloom?", style = MaterialTheme.typography.titleLarge)
        Text(
            "Можно выбрать несколько вариантов",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GoalPicker(goals) { goals = it }
        ErrorMessage(error)
        BloomButton(
            if (busy) "Сохраняем…" else "Начать свою историю →",
            {
                val date = runCatching { LocalDate.parse(birthday) }.getOrNull()
                when {
                    name.isBlank() -> error = "Напиши своё имя."
                    date == null || Period.between(date, LocalDate.now()).years !in 18..120 ->
                        error = "Проверь дату рождения. Возраст — от 18 лет."
                    goals.isEmpty() -> error = "Выбери хотя бы один вариант."
                    else ->
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                onCreated(
                                    graph.users.create(CreateProfile(name.trim(), date.toString(), gender, goals))
                                )
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                busy = false
                            }
                        }
                }
            },
            enabled = !busy,
        )
        TextButton(onClick = logout, enabled = !busy) { Text("Выйти из аккаунта") }
    }
}
