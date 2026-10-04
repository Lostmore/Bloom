package app.bloom.android.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.PersonAvatar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun activityLabel(profile: Profile): String {
    if (profile.online) return "В сети"
    profile.lastSeen?.let { value ->
        runCatching {
            Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM, HH:mm"))
        }
            .getOrNull()
            ?.let {
                return "Последний визит: $it"
            }
    }
    return when (profile.activityStatus) {
        "RECENTLY" -> "Был(а) недавно"
        "WITHIN_WEEK" -> "Был(а) на этой неделе"
        "WITHIN_MONTH" -> "Был(а) в этом месяце"
        "LONG_AGO" -> "Был(а) давно"
        else -> "Активность скрыта"
    }
}

/** Users controls visibility; raw Chat presence must not reveal hidden activity. */
fun chatPartnerOnline(profile: Profile?, presence: Boolean?, typing: Boolean): Boolean {
    if (typing) return true
    if (profile == null) return false
    return if (profile.lastSeen != null) presence ?: profile.online else profile.online
}

fun chatActivityLabel(profile: Profile, connected: Boolean?): String {
    if (profile.lastSeen == null) return activityLabel(profile)
    return when (connected) {
        true -> "В сети"
        false -> activityLabel(profile.copy(online = false))
        null -> activityLabel(profile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerCard(
    profile: Profile,
    interests: List<String>,
    dismiss: () -> Unit,
    avatar: @Composable () -> Unit = { PersonAvatar(profile.nickname, size = 72.dp) },
    openProfile: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            avatar()
            Text(
                profile.nickname + (profile.displayedAge()?.let { ", $it" } ?: ""),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(activityLabel(profile), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Повод познакомиться", style = MaterialTheme.typography.titleLarge)
            Text(profile.bio?.takeIf { it.isNotBlank() } ?: "Спроси, какой момент сегодня вызвал улыбку.")
            if (interests.isNotEmpty()) Text("Можно начать с этого: " + interests.joinToString(" · "))
            Button(onClick = openProfile, modifier = Modifier.fillMaxWidth()) { Text("Посмотреть анкету") }
            TextButton(onClick = dismiss) { Text("Вернуться к разговору") }
        }
    }
}
