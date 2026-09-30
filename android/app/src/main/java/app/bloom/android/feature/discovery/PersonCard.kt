package app.bloom.android.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.GoalLabels
import app.bloom.android.core.ui.PersonAvatar

@Composable
fun PersonCard(profile: Profile, modifier: Modifier = Modifier, open: (() -> Unit)? = null) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF96788E), Color(0xFF594559), Color(0xFF281D2D))))
            .then(if (open != null) Modifier.clickable(onClick = open) else Modifier)
    ) {
        PersonAvatar(profile.nickname, Modifier.align(Alignment.Center).padding(bottom = 56.dp), size = 132.dp)
        Surface(
            Modifier.align(Alignment.TopStart).padding(18.dp),
            color = Color.Black.copy(alpha = 0.18f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                profile.searchModes.orEmpty().firstOrNull()?.let { GoalLabels[it] } ?: "Новые знакомства",
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.nickname + (profile.displayedAge()?.let { ", $it" } ?: ""),
                    Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                )
                if (profile.verified) Icon(Icons.Outlined.Verified, "Подтверждённый профиль", tint = Color(0xFF85CAFF))
                if (open != null)
                    Icon(
                        Icons.Outlined.Info,
                        "Подробнее",
                        Modifier.padding(start = 10.dp).size(24.dp),
                        tint = Color.White,
                    )
            }
            if (!profile.city.isNullOrBlank()) Text(profile.city, color = Color.White.copy(alpha = 0.9f))
            if (!profile.bio.isNullOrBlank())
                Text(
                    profile.bio,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
        }
    }
}
