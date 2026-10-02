package app.bloom.android.feature.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bloom.android.core.model.ChatMessage
import kotlinx.coroutines.delay

@Composable
fun ReadReceiptsEffect(
    connection: ChatConnection,
    myId: String,
    messages: List<ChatMessage>,
    list: LazyListState,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestMessages by rememberUpdatedState(messages)
    LaunchedEffect(connection, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val sent = mutableSetOf<Long>()
            var errorVersion = connection.state.value.errorVersion
            var connectionVersion = connection.state.value.connectionVersion
            while (true) {
                delay(1000)
                val state = connection.state.value
                if (
                    !state.connected ||
                        state.errorVersion != errorVersion ||
                        state.connectionVersion != connectionVersion
                ) {
                    sent.clear()
                    errorVersion = state.errorVersion
                    connectionVersion = state.connectionVersion
                    // Do not immediately retry rejected actions every second.
                    if (state.actionError != null) delay(5000)
                }
                if (!state.connected) continue
                val layout = list.layoutInfo
                val visible =
                    layout.visibleItemsInfo.filter {
                        val visiblePixels =
                            minOf(it.offset + it.size, layout.viewportEndOffset) -
                                maxOf(it.offset, layout.viewportStartOffset)
                        visiblePixels > 0 &&
                            visiblePixels >=
                                minOf(
                                    it.size,
                                    layout.viewportEndOffset - layout.viewportStartOffset,
                                ) / 2
                    }
                for (item in visible) {
                    val message = latestMessages.getOrNull(item.index) ?: continue
                    if (
                        message.senderId == myId ||
                            message.readAt != null ||
                            message.deletedAt != null ||
                            message.id in sent
                    )
                        continue
                    if (connection.markRead(message.id)) sent.add(message.id)
                }
            }
        }
    }
}
