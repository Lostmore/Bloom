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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bloom.android.AppGraph
import app.bloom.android.BuildConfig
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    var demo by rememberSaveable { mutableStateOf(false) }
    var demoIndex by rememberSaveable { mutableIntStateOf(0) }
    var demoDetails by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    val seen = remember { mutableSetOf<String>() }
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(demo, lifecycle) {
        if (!demo)
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(30_000)
                    if (people.isEmpty() && !loading && !busy) {
                        cursor = null
                        refresh++
                    }
                }
            }
    }
    LaunchedEffect(refresh, mode, demo, cursor) {
        if (demo) {
            loading = false
            error = null
            unavailable = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            // Users supplies the basic feed while the Discovery service is still a stub.
            var pageCursor = cursor
            var pages = 0
            do {
                val feed = graph.users.feed(pageCursor)
                require(feed.items.orEmpty().all { !it.id.isNullOrBlank() && !it.nickname.isNullOrBlank() })
                people =
                    feed.items.orEmpty().filter {
                        it.id !in seen &&
                            (mode == null ||
                                mode == "ANY" ||
                                mode in it.searchModes.orEmpty() ||
                                "ANY" in it.searchModes.orEmpty())
                    }
                nextCursor = feed.nextCursor
                require(nextCursor == null || nextCursor != pageCursor)
                pageCursor = nextCursor
                pages++
            } while (people.isEmpty() && pageCursor != null && pages < 5)
            unavailable = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            unavailable = true
            people = emptyList()
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }
    val person =
        if (demo) demoProfile(demoIndex, mode)
        else
            people.firstOrNull {
                mode == null || mode == "ANY" || mode in it.searchModes.orEmpty() || "ANY" in it.searchModes.orEmpty()
            }
    fun react(action: String) {
        val target = person ?: return
        if (busy) return
        if (demo) {
            demoIndex = (demoIndex + 1) % 120
            return
        }
        busy = true
        error = null
        scope.launch {
            val request = "${target.id}/$action"
            try {
                val result =
                    graph.interactions.react(target.id, action, keys.getOrPut(request) { UUID.randomUUID().toString() })
                keys.remove(request)
                seen.add(target.id)
                people = people.filterNot { it.id == target.id }
                if (people.isEmpty() && nextCursor != null) cursor = nextCursor
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
            if (!demo)
                IconButton(
                    onClick = {
                        cursor = null
                        seen.clear()
                        refresh++
                    },
                    enabled = !loading && !busy,
                ) {
                    Icon(Icons.Outlined.Refresh, "Обновить анкеты")
                }
            if (BuildConfig.DEBUG)
                IconButton(
                    onClick = {
                        if (!busy) {
                            demo = !demo
                            cursor = null
                            nextCursor = null
                            people = emptyList()
                            seen.clear()
                            matchProfile = null
                        }
                    },
                    enabled = !busy,
                ) {
                    Icon(
                        Icons.Outlined.Science,
                        if (demo) "Показать реальные анкеты" else "Тестовые анкеты",
                        tint =
                            if (demo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            IconButton(onClick = { link = true }) { Icon(Icons.Outlined.Link, "Открыть анкету по ссылке") }
            IconButton(onClick = { filters = true }) { Icon(Icons.Outlined.Tune, "Цель знакомства") }
        }
        if (demo)
            Text(
                "Демо · вымышленные анкеты · свайпы не отправляются",
                Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        if (mode != null)
            InputChip(
                true,
                {
                    setMode(null)
                    cursor = null
                    nextCursor = null
                    seen.clear()
                },
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
                        if (demo) demoDetails = true else openProfile(person.id)
                    }
                }
                else ->
                    EmptyBloom(
                        if (unavailable) "Пока ищем твоих людей" else "На сегодня это все",
                        if (unavailable)
                            "Подбор пока недоступен. Можно открыть анкету по ссылке или заглянуть чуть позже."
                        else "Попробуй другую цель знакомства или вернись позже.",
                        icon = Icons.Outlined.TravelExplore,
                        action = if (nextCursor != null && !unavailable) "Показать ещё" else "Обновить",
                        onAction = {
                            if (nextCursor != null && !unavailable) cursor = nextCursor
                            else {
                                cursor = null
                                seen.clear()
                                refresh++
                            }
                        },
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
                            cursor = null
                            nextCursor = null
                            seen.clear()
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
    if (demo && demoDetails && person != null)
        AlertDialog(
            onDismissRequest = { demoDetails = false },
            title = { Text("${person.nickname}, ${person.age}") },
            text = {
                Text("${person.city}\n\n${person.bio}\n\nЭто вымышленная тестовая анкета. Чат и совпадения недоступны.")
            },
            confirmButton = { TextButton(onClick = { demoDetails = false }) { Text("Понятно") } },
        )
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
