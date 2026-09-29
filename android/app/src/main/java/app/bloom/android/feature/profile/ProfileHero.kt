package app.bloom.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.BloomMark
import app.bloom.android.core.ui.GoalLabels

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileHero(profile: Profile, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFBE79A9), Color(0xFF67518D), Color(0xFF322A4E))),
                    RoundedCornerShape(28.dp),
                )
        ) {
            BloomMark(Modifier.size(160.dp).align(Alignment.TopEnd).padding(24.dp))
            Text(
                profile.nickname.take(1).uppercase(),
                Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 84.sp,
                fontWeight = FontWeight.Light,
            )
            Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        profile.nickname + (profile.displayedAge()?.let { ", $it" } ?: ""),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    if (profile.verified)
                        Icon(
                            Icons.Outlined.Verified,
                            "Профиль подтверждён",
                            tint = Color(0xFFAAE3FF),
                        )
                }
                Text(
                    profile.city?.takeIf { it.isNotBlank() } ?: "Своя история начинается здесь",
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        Column(
            Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!profile.bio.isNullOrBlank()) Text(profile.bio!!, style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                profile.searchModes.orEmpty().forEach { mode ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            GoalLabels[mode] ?: mode,
                            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
