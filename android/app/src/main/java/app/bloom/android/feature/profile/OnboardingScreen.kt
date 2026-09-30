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
    LaunchedEffect(draft) { store.save(draft) }
    LaunchedEffect(retry) {
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
        back = { if (draft.step > 0) draft = draft.copy(step = draft.step - 1) else logout() },
        next = {
            error = draft.error()
            if (error == null && draft.step < 6) draft = draft.copy(step = draft.step + 1)
            else if (error == null)
                scope.launch {
                    busy = true
                    store.save(draft)
                    try {
                        val saved =
                            saveOnboarding(graph.users, draft) {
                                draft = draft.withSavedIdentity(it)
                                store.save(draft)
                            }
                        store.complete(draft)
                        onCreated(saved)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        error = exception.userMessage()
                    } finally {
                        busy = false
                    }
                }
        },
    )
}
