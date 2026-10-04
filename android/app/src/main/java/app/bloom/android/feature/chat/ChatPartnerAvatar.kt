package app.bloom.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import app.bloom.android.core.ui.PersonAvatar

@Composable
fun ChatPartnerAvatar(
    graph: AppGraph,
    name: String,
    photoId: String?,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    online: Boolean = false,
) {
    Box(modifier.size(size)) {
        if (photoId == null) PersonAvatar(name, Modifier, size)
        else
            ChatPhoto(
                graph,
                PhotoSource(photoId),
                Modifier.size(size).clip(CircleShape),
                maxDimension = 256,
                fallback = {
                    PersonAvatar(name, Modifier.matchParentSize(), size)
                },
            )
        if (online)
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .size(size * .25f)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .semantics { contentDescription = "В сети" }
            )
    }
}
