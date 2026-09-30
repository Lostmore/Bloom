package app.bloom.android.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import app.bloom.android.R

val MainTabs = listOf("feed", "explore", "matches", "chats", "me")

@Composable
fun BloomBottomBar(selected: String?, navigate: (String) -> Unit) {
    val chatIcon = ImageVector.vectorResource(R.drawable.ic_chats)
    val matchIcon = ImageVector.vectorResource(R.drawable.ic_matches)
    val labels = listOf("Люди", "Обзор", "Симпатии", "Чаты", "Профиль")
    val outline =
        listOf(
            Icons.Outlined.Style,
            Icons.Outlined.GridView,
            matchIcon,
            chatIcon,
            Icons.Outlined.PersonOutline,
        )
    val filled =
        listOf(
            Icons.Filled.Style,
            Icons.Filled.GridView,
            matchIcon,
            chatIcon,
            Icons.Filled.Person,
        )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0),
            modifier = Modifier.height(64.dp),
        ) {
            MainTabs.forEachIndexed { index, route ->
                NavigationBarItem(
                    selected == route,
                    { navigate(route) },
                    icon = {
                        Icon(
                            if (selected == route) filled[index] else outline[index],
                            labels[index],
                            Modifier.size(27.dp),
                        )
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}
