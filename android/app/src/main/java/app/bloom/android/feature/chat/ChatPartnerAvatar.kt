package app.bloom.android.feature.chat

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    if (photoId == null) PersonAvatar(name, modifier, size)
    else
        ChatPhoto(
            graph,
            PhotoSource(photoId),
            modifier.size(size).clip(CircleShape),
            maxDimension = 256,
            fallback = {
                PersonAvatar(name, Modifier.matchParentSize(), size)
            },
        )
}
