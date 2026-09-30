package app.bloom.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bloom.android.core.model.Profile
import app.bloom.android.core.ui.GoalLabels

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileHero(profile: Profile, modifier: Modifier = Modifier, openCover: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        app.bloom.android.feature.discovery.PersonCard(profile, Modifier.fillMaxWidth().height(360.dp), openCover)
        Column(
            Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!profile.bio.isNullOrBlank()) {
                Text("О себе", style = MaterialTheme.typography.titleMedium)
                Text(profile.bio, style = MaterialTheme.typography.bodyLarge)
            }
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
