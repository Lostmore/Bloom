package app.bloom.android.feature.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.*

data class MatchTileItem(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesContent(
    items: List<MatchTileItem>,
    loading: Boolean,
    busy: Boolean,
    error: String?,
    hasMore: Boolean,
    refresh: () -> Unit,
    loadMore: () -> Unit,
    openProfile: (String) -> Unit,
    chat: (String) -> Unit,
    remove: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Симпатии", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = refresh, enabled = !loading) { Icon(Icons.Outlined.Refresh, "Обновить совпадения") }
        }
        Text(
            "Ваши совпадения · ${items.size}",
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        PullToRefreshBox(loading, refresh, Modifier.weight(1f)) {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.fillMaxSize(),
                flingBehavior = app.bloom.android.core.ui.rememberBloomFling(),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (error != null) item(span = { GridItemSpan(2) }) { ErrorMessage(error) }
                if (items.isEmpty())
                    item(span = { GridItemSpan(2) }) {
                        if (loading) LoadingBloom(Modifier.height(300.dp))
                        else
                            EmptyBloom(
                                "Взаимно — это особенное",
                                "Когда вы понравитесь друг другу, совпадение появится здесь.",
                                Modifier.padding(top = 70.dp),
                                Icons.Outlined.FavoriteBorder,
                            )
                    }
                items(items, key = { it.id }) { match ->
                    var menu by remember { mutableStateOf(false) }
                    Card(
                        onClick = { openProfile(match.id) },
                        shape = RoundedCornerShape(22.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                    ) {
                        Box(Modifier.fillMaxWidth().height(145.dp), contentAlignment = Alignment.Center) {
                            PersonAvatar(match.name, size = 82.dp)
                            Box(Modifier.align(Alignment.TopEnd)) {
                                IconButton(onClick = { menu = true }, enabled = !busy) {
                                    Icon(Icons.Outlined.MoreHoriz, "Действия с совпадением")
                                }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Убрать совпадение") },
                                        onClick = {
                                            menu = false
                                            remove(match.id)
                                        },
                                    )
                                }
                            }
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                match.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Вы понравились друг другу",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { chat(match.id) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Написать")
                            }
                        }
                    }
                }
                if (hasMore)
                    item(span = { GridItemSpan(2) }) {
                        TextButton(onClick = loadMore, enabled = !loading) { Text("Показать ещё") }
                    }
            }
        }
    }
}
