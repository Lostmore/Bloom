package app.bloom.android.feature.discovery

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    graph: AppGraph,
    openProfile: (String) -> Unit,
    mode: String? = null,
    setMode: (String?) -> Unit = {},
) {
    var people by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var unavailable by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var matchProfile by remember { mutableStateOf<String?>(null) }
    val keys = rememberSaveable { hashMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh) {
        loading = true
        try {
            val feed = graph.discovery.feed()
            require(feed.items.orEmpty().all { !it.id.isNullOrBlank() && !it.nickname.isNullOrBlank() })
            people = feed.items.orEmpty()
            unavailable = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            unavailable = true
        } finally {
            loading = false
        }
    }
    val person = people.firstOrNull {
        mode == null || mode == "ANY" || mode in it.searchModes.orEmpty() || "ANY" in it.searchModes.orEmpty()
    }
    fun react(action: String) {
        val target = person ?: return
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val request = "${target.id}/$action"
            try {
                val result =
                    graph.interactions.react(target.id, action, keys.getOrPut(request) { UUID.randomUUID().toString() })
                keys.remove(request)
                people = people.filterNot { it.id == target.id }
                if (result.matchId != null) matchProfile = target.id
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                error = exception.userMessage()
            } finally {
                busy = false
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BloomBrand(Modifier.weight(1f))
            IconButton(onClick = { link = true }) { Icon(Icons.Outlined.Link, "Открыть анкету по ссылке") }
            IconButton(onClick = { filters = true }) { Icon(Icons.Outlined.Tune, "Цель знакомства") }
        }
        if (mode != null)
            InputChip(
                true,
                { setMode(null) },
                label = { Text(GoalLabels[mode] ?: "Все") },
                trailingIcon = { Icon(Icons.Outlined.Close, "Сбросить фильтр", Modifier.size(16.dp)) },
            )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                loading -> LoadingBloom()
                person != null -> {
                    var drag by remember(person.id) { mutableFloatStateOf(0f) }
                    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
                    PersonCard(
                        person,
                        Modifier.fillMaxSize()
                            .graphicsLayer {
                                translationX = drag
                                rotationZ = (drag / 45f).coerceIn(-9f, 9f)
                            }
                            .pointerInput(person.id, busy) {
                                if (!busy)
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, amount ->
                                            change.consume()
                                            drag += amount
                                        },
                                        onDragCancel = { drag = 0f },
                                        onDragEnd = {
                                            if (drag > threshold) react("like")
                                            else if (drag < -threshold) react("skip")
                                            drag = 0f
                                        },
                                    )
                            },
                    ) {
                        openProfile(person.id)
                    }
                }
                else ->
                    EmptyBloom(
                        if (unavailable) "Пока ищем твоих людей" else "На сегодня это все",
                        if (unavailable)
                            "Подбор пока недоступен. Можно открыть анкету по ссылке или заглянуть чуть позже."
                        else "Попробуй другую цель знакомства или вернись позже.",
                        icon = Icons.Outlined.TravelExplore,
                        action = "Обновить",
                        onAction = { refresh++ },
                    )
            }
        }
        if (error != null) Box(Modifier.padding(top = 8.dp)) { ErrorMessage(error) }
        ReactionButtons(person != null && !busy && !loading, ::react)
    }
    if (link) ProfileLinkDialog({ link = false }, openProfile)
    if (filters)
        ModalBottomSheet(onDismissRequest = { filters = false }) {
            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Кого хочется встретить?", style = MaterialTheme.typography.titleLarge)
                (mapOf<String?, String>(null to "Все знакомства") + GoalLabels).forEach { (key, title) ->
                    TextButton(
                        onClick = {
                            setMode(key)
                            filters = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(title, Modifier.weight(1f))
                        if (mode == key) Icon(Icons.Outlined.Check, null)
                    }
                }
            }
        }
    matchProfile?.let { id ->
        AlertDialog(
            onDismissRequest = { matchProfile = null },
            icon = { BloomMark(Modifier.size(56.dp)) },
            title = { Text("Это взаимно!") },
            text = { Text("Вы понравились друг другу. Начните свою историю.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        matchProfile = null
                        openProfile(id)
                    }
                ) {
                    Text("Открыть анкету")
                }
            },
            dismissButton = { TextButton(onClick = { matchProfile = null }) { Text("Продолжить") } },
        )
    }
}

@Composable
fun ReactionButtons(enabled: Boolean, react: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("skip", "super-interest", "like").forEachIndexed { index, action ->
            val color = listOf(Color(0xFFFF5A76), Color(0xFF60ACEE), Color(0xFF39BC94))[index]
            OutlinedIconButton(
                onClick = { react(action) },
                enabled = enabled,
                shape = CircleShape,
                modifier = Modifier.size(if (index == 1) 50.dp else 62.dp),
                colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = color),
            ) {
                Icon(
                    listOf(Icons.Outlined.Close, Icons.Outlined.StarOutline, Icons.Outlined.FavoriteBorder)[index],
                    listOf("Пропустить", "Особый интерес", "Нравится")[index],
                    Modifier.size(if (index == 1) 25.dp else 32.dp),
                )
            }
        }
    }
}
