package app.bloom.android.feature.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import app.bloom.android.AppGraph
import app.bloom.android.core.model.Profile
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    graph: AppGraph,
    me: Profile,
    update: (Profile) -> Unit,
    theme: String,
    setTheme: (String) -> Unit,
    logout: () -> Unit,
) {
    var settings by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var interests by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ProfileOverview(
        me,
        edit = { edit = true },
        interests = { interests = true },
        privacy = { privacy = true },
        share = {
            val intent =
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "bloom://profile/${me.id}")
            context.startActivity(Intent.createChooser(intent, null))
        },
        settings = { settings = true },
    )
    if (settings)
        ProfileSettingsSheet(
            theme,
            setTheme,
            busy,
            error,
            { settings = false },
            logout,
            {
                settings = false
                delete = true
            },
        ) {
            scope.launch {
                busy = true
                try {
                    update(graph.users.me())
                    error = null
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    error = exception.userMessage()
                } finally {
                    busy = false
                }
            }
        }
    if (edit)
        EditProfileDialog(
            graph,
            me,
            { edit = false },
            {
                update(it)
                edit = false
            },
        )
    if (error != null && !settings)
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Не удалось выполнить действие") },
            text = { Text(error.orEmpty()) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Понятно") } },
        )
    if (interests)
        InterestsDialog(graph, me.interests.orEmpty(), { interests = false }) { updated ->
            update(updated)
            interests = false
        }
    if (privacy)
        PrivacyDialog(graph, { privacy = false }) { updated ->
            update(updated)
            privacy = false
        }
    if (delete)
        AlertDialog(
            onDismissRequest = { if (!busy) delete = false },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Анкета будет удалена. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                graph.users.delete()
                                logout()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                busy = false
                                delete = false
                            }
                        }
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { delete = false }) { Text("Оставить") }
            },
        )
}
