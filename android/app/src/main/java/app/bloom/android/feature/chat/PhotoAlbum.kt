package app.bloom.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bloom.android.AppGraph

@Composable
fun PhotoAlbum(graph: AppGraph, photos: List<PhotoSource>, modifier: Modifier = Modifier) {
    var opened by remember(photos) { mutableStateOf<Int?>(null) }
    val rows =
        when (photos.size) {
            1 -> listOf(listOf(0))
            3 -> listOf(listOf(0), listOf(1, 2))
            else -> photos.indices.toList().chunked(if (photos.size > 4) 3 else 2)
        }
    Column(modifier.clip(RoundedCornerShape(16.dp)), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { index ->
                    ChatPhoto(
                        graph,
                        photos[index],
                        Modifier.weight(1f).aspectRatio(if (row.size == 1) 1.35f else 1f).clickable(
                            onClickLabel = "Открыть фото ${index + 1}"
                        ) {
                            opened = index
                        },
                    )
                }
            }
        }
    }
    opened?.let { index -> PhotoViewer(graph, photos, index) { opened = null } }
}

@Composable
fun PhotoViewer(graph: AppGraph, photos: List<PhotoSource>, initial: Int, dismiss: () -> Unit) {
    val pager = rememberPagerState(initialPage = initial) { photos.size }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
                ChatPhoto(graph, photos[index], Modifier.fillMaxSize(), fit = true)
            }
            Row(
                Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${pager.currentPage + 1} / ${photos.size}", Modifier.weight(1f), color = Color.White)
                IconButton(onClick = dismiss) { Icon(Icons.Outlined.Close, "Закрыть фото", tint = Color.White) }
            }
        }
    }
}
