package app.bloom.android.feature.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.*
import app.bloom.android.feature.profile.ProfileHero
import java.util.UUID
import kotlinx.coroutines.CancellationException

@Composable
fun DiscoveryScreen(graph: AppGraph, openProfile: (String) -> Unit) {
    var people by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var unavailable by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refresh) {
        loading = true
        try {
            val feed = graph.discovery.feed()
            require(feed.items.orEmpty().all { !it.id.isNullOrBlank() && !it.nickname.isNullOrBlank() })
            people = feed.items ?: emptyList()
            unavailable = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            unavailable = true
        } finally {
            loading = false
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BloomBrand()
            IconButton(onClick = { refresh++ }) { Icon(Icons.Outlined.Refresh, "Обновить") }
        }
        Text("Твои люди где-то рядом", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Для любви, дружбы и всего, что между.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            loading -> LoadingBloom(Modifier.height(240.dp))
            people.isNotEmpty() ->
                people.forEach { profile ->
                    Card(onClick = { openProfile(profile.id) }, shape = RoundedCornerShape(28.dp)) {
                        ProfileHero(profile)
                    }
                }
            else ->
                EmptyBloom(
                    if (unavailable) "Знакомства скоро расцветут" else "Пока здесь тихо",
                    if (unavailable)
                        "Подбор людей пока недоступен. Уже можно открыть анкету по ссылке и начать знакомство."
                    else "Новые люди появятся здесь. А пока можно обменяться ссылками на анкеты.",
                    action = "Проверить ещё раз",
                    onAction = { refresh++ },
                )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Уже нашли друг друга?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Вставь ссылку на анкету Bloom, которой с тобой поделились.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    link,
                    {
                        link = it
                        linkError = null
                    },
                    Modifier.fillMaxWidth(),
                    label = { Text("bloom://profile/…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                ErrorMessage(linkError)
                TextButton(
                    onClick = {
                        val value = link.trim().removePrefix("bloom://profile/")
                        val id = runCatching { UUID.fromString(value).toString() }.getOrNull()
                        if (id == null) linkError = "Проверь ссылку на профиль." else openProfile(id)
                    }
                ) {
                    Text("Открыть профиль →")
                }
            }
        }
    }
}
