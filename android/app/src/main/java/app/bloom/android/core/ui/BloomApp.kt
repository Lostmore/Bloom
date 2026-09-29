package app.bloom.android.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.feature.auth.AuthScreen
import app.bloom.android.feature.chat.ChatListScreen
import app.bloom.android.feature.chat.ChatRoomScreen
import app.bloom.android.feature.discovery.DiscoveryScreen
import app.bloom.android.feature.matches.MatchesScreen
import app.bloom.android.feature.profile.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
fun BloomApp(
    graph: AppGraph,
    profileLink: String?,
    consumeLink: () -> Unit,
    changeServer: (String) -> Unit,
) {
    var theme by remember {
        mutableStateOf(graph.preferences.getString("theme", "light") ?: "light")
    }
    val session by graph.sessions.session.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val logout: () -> Unit = {
        scope.launch {
            val refresh = graph.sessions.session.value?.refreshToken
            withContext(Dispatchers.IO) { graph.sessions.clear() }
            if (refresh != null) {
                try {
                    graph.identity.logout(app.bloom.android.core.model.RefreshRequest(refresh))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    /* Local credentials are already removed, even when offline. */
                }
            }
        }
    }
    BloomTheme(theme) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.safeDrawingPadding()) {
                if (session == null)
                    AuthScreen(
                        graph,
                        theme,
                        {
                            theme = if (theme == "dark") "light" else "dark"
                            graph.preferences.edit().putString("theme", theme).apply()
                        },
                        changeServer,
                    )
                else
                    AuthenticatedApp(
                        graph,
                        profileLink,
                        consumeLink,
                        theme,
                        { next ->
                            theme = next
                            graph.preferences.edit().putString("theme", next).apply()
                        },
                        logout,
                    )
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(
    graph: AppGraph,
    profileLink: String?,
    consumeLink: () -> Unit,
    theme: String,
    setTheme: (String) -> Unit,
    logout: () -> Unit,
) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var missing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            profile = graph.users.me()
            missing = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (exception is HttpException && exception.code() == 404) missing = true
            else error = exception.userMessage()
        } finally {
            loading = false
        }
    }
    when {
        loading -> LoadingBloom()
        missing ->
            OnboardingScreen(
                graph,
                {
                    profile = it
                    missing = false
                },
                logout,
            )
        profile != null ->
            MainNavigation(
                graph,
                profile!!,
                { profile = it },
                profileLink,
                consumeLink,
                theme,
                setTheme,
                logout,
            )
        else ->
            Column {
                EmptyBloom(
                    "Не удалось открыть профиль",
                    error ?: "Попробуй ещё раз.",
                    action = "Повторить",
                    onAction = { reload++ },
                )
                TextButton(onClick = logout) { Text("Выйти") }
            }
    }
}

@Composable
private fun MainNavigation(
    graph: AppGraph,
    me: Profile,
    updateMe: (Profile) -> Unit,
    profileLink: String?,
    consumeLink: () -> Unit,
    theme: String,
    setTheme: (String) -> Unit,
    logout: () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf("feed", "matches", "chats", "me")
    val labels = listOf("Для тебя", "Совпадения", "Чаты", "Профиль")
    val icons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.FavoriteBorder,
            Icons.Outlined.ChatBubbleOutline,
            Icons.Outlined.PersonOutline,
        )
    LaunchedEffect(profileLink) {
        if (profileLink != null) {
            nav.navigate("profile/$profileLink")
            consumeLink()
        }
    }
    LaunchedEffect(Unit) {
        try {
            graph.users.heartbeat()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            /* Presence is best-effort and must not block navigation. */
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (route in tabs)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            route == tab,
                            onClick = {
                                nav.navigate(tab) {
                                    popUpTo("feed") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icons[index], null) },
                            label = { Text(labels[index], maxLines = 1) },
                        )
                    }
                }
        },
    ) { padding ->
        NavHost(nav, "feed", Modifier.padding(padding)) {
            composable("feed") { DiscoveryScreen(graph, { nav.navigate("profile/$it") }) }
            composable("matches") {
                MatchesScreen(
                    graph,
                    me.id,
                    { nav.navigate("profile/$it") },
                    { nav.navigate("room/$it") },
                )
            }
            composable("chats") { ChatListScreen(graph, me.id) { nav.navigate("room/$it") } }
            composable("me") { MyProfileScreen(graph, me, updateMe, theme, setTheme, logout) }
            composable("profile/{id}") { stack ->
                ProfileScreen(
                    graph,
                    stack.arguments!!.getString("id")!!,
                    me.id,
                    { nav.popBackStack() },
                    { nav.navigate("room/$it") },
                )
            }
            composable("room/{id}") { stack ->
                ChatRoomScreen(graph, stack.arguments!!.getString("id")!!.toLong(), me.id) {
                    nav.popBackStack()
                }
            }
        }
    }
}
