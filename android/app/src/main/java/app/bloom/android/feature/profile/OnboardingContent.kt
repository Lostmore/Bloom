package app.bloom.android.feature.profile

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.Interest
import app.bloom.android.core.ui.*

private val titles =
    listOf(
        "Как тебя зовут?",
        "Когда твой день\nрождения?",
        "Расскажи о себе",
        "Что ты ищешь?",
        "Что тебя\nувлекает?",
        "Покажи себя",
        "Ещё немного тебя",
    )
private val subtitles =
    listOf(
        "Так тебя будут видеть в Bloom.",
        "В профиле будет только возраст, не дата рождения. 18+",
        "Выбери, как указать твой пол в анкете.",
        "Любовь, дружба или компания — можно выбрать несколько.",
        "Выбери до 10 интересов. С общего проще начать разговор.",
        "Выбери до 6 фотографий. Первую сделай своей главной.",
        "Пара деталей, за которые зацепится хороший разговор.",
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingContent(
    draft: OnboardingDraft,
    catalog: List<Interest>,
    busy: Boolean,
    error: String?,
    catalogError: String?,
    change: (OnboardingDraft) -> Unit,
    retryCatalog: () -> Unit,
    back: () -> Unit,
    next: () -> Unit,
) {
    BackHandler { if (!busy) back() }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, enabled = !busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { BloomMark(Modifier.size(28.dp)) }
            Text(
                "${draft.step + 1} / 7",
                Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        LinearProgressIndicator(progress = { (draft.step + 1) / 7f }, modifier = Modifier.fillMaxWidth().height(3.dp))
        key(draft.step) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(titles[draft.step], style = MaterialTheme.typography.headlineLarge)
                Text(subtitles[draft.step], color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                when (draft.step) {
                    0 ->
                        OutlinedTextField(
                            draft.name,
                            { change(draft.copy(name = it.take(40))) },
                            Modifier.fillMaxWidth().testTag("onboarding-name"),
                            label = { Text("Твоё имя") },
                            singleLine = true,
                            enabled = !busy,
                            shape = RoundedCornerShape(18.dp),
                        )
                    1 ->
                        if (draft.identitySaved) {
                            Text(draft.birthday, style = MaterialTheme.typography.titleLarge)
                            Text("Дата рождения уже сохранена в анкете.")
                        } else BirthdayStep(draft.birthday) { change(draft.copy(birthday = it)) }
                    2 ->
                        listOf("FEMALE" to "Женский", "MALE" to "Мужской", "OTHER" to "Другой").forEach { (value, label)
                            ->
                            OutlinedButton(
                                onClick = { change(draft.copy(gender = value)) },
                                enabled = !draft.identitySaved,
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                            if (draft.gender == value) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                    ),
                            ) {
                                Text(if (draft.gender == value) "$label  ✓" else label)
                            }
                        }
                    3 -> GoalPicker(draft.goals.toSet()) { change(draft.copy(goals = it.toList())) }
                    4 -> {
                        Text("${draft.interests.size} из 10", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            catalog.forEach { interest ->
                                FilterChip(
                                    interest.id in draft.interests,
                                    {
                                        change(
                                            draft.copy(
                                                interests =
                                                    if (interest.id in draft.interests) draft.interests - interest.id
                                                    else if (draft.interests.size < 10) draft.interests + interest.id
                                                    else draft.interests
                                            )
                                        )
                                    },
                                    label = { Text(interest.name) },
                                )
                            }
                        }
                        if (catalog.isEmpty() && catalogError == null) CircularProgressIndicator()
                        ErrorMessage(catalogError)
                        if (catalogError != null) TextButton(onClick = retryCatalog) { Text("Повторить") }
                    }
                    5 -> OnboardingPhotos(draft.photos) { change(draft.copy(photos = it)) }
                    6 -> {
                        OutlinedTextField(
                            draft.city,
                            { change(draft.copy(city = it.take(100))) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Город · необязательно") },
                            singleLine = true,
                            enabled = !busy,
                        )
                        OutlinedTextField(
                            draft.bio,
                            { change(draft.copy(bio = it.take(500))) },
                            Modifier.fillMaxWidth(),
                            label = { Text("О себе · необязательно") },
                            minLines = 4,
                            supportingText = { Text("${draft.bio.length}/500") },
                            enabled = !busy,
                        )
                        if (draft.photos.isNotEmpty())
                            Text(
                                "Фото сохранены как локальный черновик. Пока другие пользователи их не увидят.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ErrorMessage(error)
            BloomButton(
                if (busy) "Сохраняем…" else if (draft.step == 6) "Начать знакомиться" else "Продолжить",
                next,
                enabled = !busy && (draft.step != 4 || catalog.isNotEmpty()),
            )
        }
    }
}
