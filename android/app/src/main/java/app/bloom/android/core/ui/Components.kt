package app.bloom.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BloomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFF657C), Color(0xFFE92B82))),
                    RoundedCornerShape(22.dp),
                ),
        shape = RoundedCornerShape(22.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun LoadingBloom(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BloomMark(Modifier.size(62.dp), loading = true)
            Text("Пусть хорошее случится", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyBloom(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.AutoAwesome,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                icon,
                null,
                Modifier.padding(22.dp).size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun ErrorMessage(message: String?) {
    if (message != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                message,
                Modifier.fillMaxWidth().padding(14.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

val GoalLabels =
    linkedMapOf(
        "DATING" to "Любовь",
        "FRIENDS" to "Друзья",
        "ACTIVITIES" to "Активности",
        "ANY" to "Всё сразу",
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalPicker(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalLabels.forEach { (key, label) ->
            FilterChip(
                selected = key in selected,
                onClick = {
                    onChange(if (key in selected) selected - key else selected + key)
                },
                label = { Text(label) },
            )
        }
    }
}
