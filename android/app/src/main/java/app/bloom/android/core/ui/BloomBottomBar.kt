package app.bloom.android.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bloom.android.R

val MainTabs = listOf("feed", "explore", "matches", "chats", "me")

@Composable
fun BloomBottomBar(selected: String?, navigate: (String) -> Unit) {
    val chatIcon = ImageVector.vectorResource(R.drawable.ic_chats)
    val labels = listOf("Люди", "Обзор", "Симпатии", "Чаты", "Профиль")
    val outline =
        listOf(
            Icons.Outlined.Style,
            Icons.Outlined.GridView,
            Icons.Outlined.FavoriteBorder,
            chatIcon,
            Icons.Outlined.PersonOutline,
        )
    val filled =
        listOf(
            Icons.Filled.Style,
            Icons.Filled.GridView,
            Icons.Filled.Favorite,
            chatIcon,
            Icons.Filled.Person,
        )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0),
            modifier = Modifier.height(72.dp),
        ) {
            MainTabs.forEachIndexed { index, route ->
                NavigationBarItem(
                    selected == route,
                    { navigate(route) },
                    icon = {
                        Icon(if (selected == route) filled[index] else outline[index], null, Modifier.size(25.dp))
                    },
                    label = { Text(labels[index], fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}
