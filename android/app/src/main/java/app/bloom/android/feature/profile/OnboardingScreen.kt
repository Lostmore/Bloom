package app.bloom.android.feature.profile

import androidx.compose.runtime.*
import app.bloom.android.AppGraph
import app.bloom.android.core.model.*
import app.bloom.android.core.network.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    graph: AppGraph,
    store: OnboardingStore,
    existing: Profile?,
    onCreated: (Profile) -> Unit,
    logout: () -> Unit,
) {
    var draft by remember {
        mutableStateOf(store.read().let { if (existing != null) it.withSavedIdentity(existing) else it })
    }
    var catalog by remember { mutableStateOf<List<Interest>>(emptyList()) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val restricted = graph.sessions.onboardingRequired()
    LaunchedEffect(draft) { store.save(draft) }
    LaunchedEffect(restricted) {
        if (restricted && draft.step == 4) draft = draft.copy(step = 5)
    }
    LaunchedEffect(retry, restricted) {
        if (restricted) return@LaunchedEffect
        catalogError = null
        try {
            catalog = graph.users.interests()
            if (catalog.isEmpty()) catalogError = "Увлечения пока недоступны. Попробуй загрузить список ещё раз."
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            catalogError = exception.userMessage()
        }
    }
    OnboardingContent(
        draft,
        catalog,
        busy,
        error,
        catalogError,
        change = {
            draft = it
            error = null
        },
        retryCatalog = { retry++ },
        back = {
            if (draft.step > 0) draft = draft.copy(step = if (restricted && draft.step == 5) 3 else draft.step - 1)
            else logout()
        },
        next = {
            error = draft.error()
            if (error == null && draft.step < 6)
                draft = draft.copy(step = if (restricted && draft.step == 3) 5 else draft.step + 1)
            else if (error == null)
                scope.launch {
                    busy = true
                    store.save(draft)
                    try {
                        val saved =
                            saveOnboarding(graph.users, draft, restricted, graph.sessions::awaitProfileActivation) {
                                draft = draft.withSavedIdentity(it)
                                store.save(draft)
                            }
                        store.complete(draft)
                        onCreated(saved)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        error =
                            if (exception is app.bloom.android.core.security.ProfileActivationPending)
                                "Анкета сохранена. Активация ещё идёт — попробуй продолжить чуть позже."
                            else exception.userMessage()
                    } finally {
                        busy = false
                    }
                }
        },
    )
}
