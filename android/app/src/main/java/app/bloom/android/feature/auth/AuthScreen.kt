package app.bloom.android.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.BuildConfig
import app.bloom.android.core.model.Credentials
import app.bloom.android.core.network.userMessage
import app.bloom.android.core.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    graph: AppGraph,
    theme: String,
    toggleTheme: () -> Unit,
    changeServer: (String) -> Unit,
) {
    var form by remember { mutableStateOf(false) }
    var register by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var serverDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BloomBrand(Modifier.weight(1f))
            IconButton(onClick = toggleTheme) {
                Icon(
                    if (theme == "dark") Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    "Переключить тему",
                )
            }
            if (BuildConfig.DEBUG)
                IconButton(onClick = { serverDialog = true }) {
                    Icon(Icons.Outlined.Settings, "Адрес сервера")
                }
        }
        if (!form) {
            WelcomeContent()
            BloomButton(
                "Найти своих людей →",
                {
                    form = true
                    register = true
                },
            )
            TextButton(
                onClick = {
                    form = true
                    register = false
                },
                Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("У меня уже есть аккаунт")
            }
        } else {
            TextButton(onClick = { form = false }) { Text("← Назад") }
            Text(
                if (register) "Хорошее начинается\nсо знакомства" else "Рады, что ты здесь",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                if (register) "Создай аккаунт, а дальше — будь собой." else "Войди, чтобы продолжить свои истории.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                phone,
                { phone = it },
                Modifier.fillMaxWidth(),
                label = { Text("Телефон") },
                placeholder = { Text("+7 900 000 00 00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(18.dp),
            )
            OutlinedTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                supportingText = {
                    Text(if (register) "Не меньше 12 символов" else "Пароль от аккаунта Bloom")
                },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(18.dp),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            "Показать или скрыть пароль",
                        )
                    }
                },
            )
            ErrorMessage(error)
            BloomButton(
                if (loading) "Секунду…" else if (register) "Создать аккаунт" else "Войти",
                {
                    val normalized = phone.filter { it.isDigit() || it == '+' }
                    if (!normalized.matches(Regex("^\\+[1-9]\\d{9,14}$"))) {
                        error = "Укажи телефон с кодом страны, например +79001234567."
                    } else if (password.length < (if (register) 12 else 1) || password.length > 128) {
                        error = "Проверь длину пароля."
                    } else {
                        loading = true
                        error = null
                        scope.launch {
                            try {
                                val request = Credentials(normalized, password)
                                val tokens =
                                    if (register) graph.identity.register(request) else graph.identity.login(request)
                                withContext(Dispatchers.IO) { graph.sessions.accept(tokens) }
                                password = ""
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.userMessage()
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                enabled = !loading,
            )
            TextButton(
                onClick = {
                    register = !register
                    error = null
                },
                enabled = !loading,
            ) {
                Text(if (register) "Уже с нами? Войти" else "Нет аккаунта? Присоединиться")
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (serverDialog) ServerDialog(graph.baseUrl.toString(), { serverDialog = false }, changeServer)
}
