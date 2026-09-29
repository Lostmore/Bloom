package app.bloom.android.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.*

@Composable
fun WelcomeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .height(172.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background,
                        )
                    ),
                    RoundedCornerShape(36.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BloomMark(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(100.dp))
            Surface(
                Modifier.align(Alignment.BottomEnd).padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "Больше, чем знакомства",
                    Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text("Люди. Моменты.\nВозможности.", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Влюбиться, найти друзей или просто поговорить. Здесь можно быть собой — и встретить своих.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCard(
                    "Любовь",
                    "Твоя особенная история",
                    Icons.Outlined.FavoriteBorder,
                    Modifier.weight(1f),
                    Color(0xFFFF719F),
                )
                GoalCard(
                    "Друзья",
                    "На одной волне",
                    Icons.Outlined.PeopleOutline,
                    Modifier.weight(1f),
                    Color(0xFF42BE91),
                )
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCard(
                    "Активности",
                    "Вместе интереснее",
                    Icons.Outlined.LocalActivity,
                    Modifier.weight(1f),
                    Color(0xFFFFAC54),
                )
                GoalCard(
                    "Всё сразу",
                    "Без лишних границ",
                    Icons.Outlined.AllInclusive,
                    Modifier.weight(1f),
                    Color(0xFFAF70F3),
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    accent: Color,
) {
    Surface(modifier.fillMaxHeight(), shape = RoundedCornerShape(24.dp), color = accent.copy(alpha = 0.1f)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(30.dp), tint = accent)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
