package app.bloom.android.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileOverview(
    me: Profile,
    edit: () -> Unit,
    interests: () -> Unit,
    share: () -> Unit,
    settings: () -> Unit,
    interestNames: List<String> = emptyList(),
) {
    val completed =
        listOf(
                me.nickname.isNotBlank(),
                !me.bio.isNullOrBlank(),
                !me.city.isNullOrBlank(),
                me.interests.orEmpty().isNotEmpty(),
                me.searchModes.orEmpty().isNotEmpty(),
            )
            .count { it }
    ProfileCoverHost(me) { coverModifier, openCover ->
        Column(
            coverModifier.fillMaxSize().testTag("profile-scroll").verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BloomBrand(Modifier.weight(1f))
                IconButton(onClick = settings) { Icon(Icons.Outlined.Settings, "Настройки") }
            }
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.BottomEnd) {
                PersonAvatar(
                    me.nickname,
                    Modifier.clickable(onClickLabel = "Открыть обложку", onClick = openCover)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                        .padding(7.dp),
                    size = 118.dp,
                )
                FilledIconButton(onClick = edit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Edit, "Редактировать анкету", Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    me.nickname + (me.displayedAge()?.let { ", $it" } ?: ""),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (me.verified) Icon(Icons.Outlined.Verified, "Профиль подтверждён", tint = Color(0xFF60A9E7))
            }
            if (!me.city.isNullOrBlank())
                Text(me.city, Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileShortcut("Изменить", Icons.Outlined.Edit, edit)
                ProfileShortcut("Интересы", Icons.Outlined.AutoAwesome, interests)
                ProfileShortcut("Поделиться", Icons.Outlined.IosShare, share)
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (completed < 5)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row {
                                Text("Твоя анкета", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${completed * 20}%",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { completed / 5f },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            )
                            Text(
                                if (completed < 5) "Добавь детали — так легче найти общее."
                                else "Теперь в анкете чуть больше тебя.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = edit, contentPadding = PaddingValues(0.dp)) {
                                Text("Редактировать профиль →")
                            }
                        }
                    }
                Text("Обо мне", style = MaterialTheme.typography.titleMedium)
                Text(
                    me.bio?.takeIf { it.isNotBlank() } ?: "Расскажи, что тебя увлекает и чему ты улыбаешься.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("Здесь для", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    me.searchModes
                        .orEmpty()
                        .mapNotNull { GoalLabels[it] }
                        .forEach { goal ->
                            SuggestionChip(onClick = edit, label = { Text(goal) })
                        }
                }
                Text("То, что меня увлекает", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    interestNames.forEach { name -> SuggestionChip(onClick = interests, label = { Text(name) }) }
                }
                TextButton(onClick = interests) {
                    Text(if (interestNames.isEmpty()) "Добавить интересы" else "Изменить интересы")
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ProfileShortcut(label: String, icon: ImageVector, action: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = action,
            modifier = Modifier.size(52.dp),
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
        ) {
            Icon(icon, label, Modifier.size(22.dp))
        }
        Text(
            label,
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ProfileMenuRow(title: String, subtitle: String, icon: ImageVector, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
