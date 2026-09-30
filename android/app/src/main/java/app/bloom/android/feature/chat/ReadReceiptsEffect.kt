package app.bloom.android.feature.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bloom.android.AppGraph
import app.bloom.android.core.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun ReadReceiptsEffect(graph: AppGraph, roomId: Long, myId: String, messages: List<ChatMessage>, list: LazyListState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestMessages by rememberUpdatedState(messages)
    LaunchedEffect(graph, roomId, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                if (!graph.chat.capabilities().readReceipts) return@repeatOnLifecycle
                var lastSent = 0L
                while (true) {
                    delay(1000)
                    val layout = list.layoutInfo
                    val visible =
                        layout.visibleItemsInfo.filter {
                            val visiblePixels =
                                minOf(it.offset + it.size, layout.viewportEndOffset) -
                                    maxOf(it.offset, layout.viewportStartOffset)
                            visiblePixels > 0 &&
                                visiblePixels >=
                                    minOf(it.size, layout.viewportEndOffset - layout.viewportStartOffset) / 2
                        }
                    val id =
                        visible
                            .mapNotNull { item ->
                                latestMessages.getOrNull(item.index)?.takeIf { it.senderId != myId }?.id
                            }
                            .maxOrNull()
                    if (id != null && id > lastSent) {
                        graph.chat.markRead(roomId, mapOf("messageId" to id))
                        lastSent = id
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                /* Unsupported/offline: never manufacture a read receipt. Retry on resume. */
            }
        }
    }
}
