package app.bloom.android.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bloom.android.core.ui.BloomMark

@Composable
fun ExploreScreen(chooseMode: (String) -> Unit, openProfile: (String) -> Unit) {
    var link by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Обзор", style = MaterialTheme.typography.headlineLarge)
        Text("Знакомься по-своему", style = MaterialTheme.typography.titleLarge)
        Text(
            "Сегодня ищешь любовь, компанию или просто хороший разговор?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExploreTile(
            "Всё начинается с тебя",
            "Откройся новым знакомствам",
            Icons.Outlined.AutoAwesome,
            listOf(Color(0xFFC4497A), Color(0xFF653152)),
            Modifier.fillMaxWidth(),
            { chooseMode("ANY") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExploreTile(
                "Влюбиться",
                "Та самая искра",
                Icons.Outlined.FavoriteBorder,
                listOf(Color(0xFFF77986), Color(0xFFC93760)),
                Modifier.weight(1f),
                { chooseMode("DATING") },
            )
            ExploreTile(
                "Найти друзей",
                "На одной волне",
                Icons.Outlined.PeopleOutline,
                listOf(Color(0xFF9A8CD5), Color(0xFF5A467F)),
                Modifier.weight(1f),
                { chooseMode("FRIENDS") },
            )
        }
        ExploreTile(
            "Планы на двоих и больше",
            "Ищи компанию для своих увлечений",
            Icons.Outlined.LocalActivity,
            listOf(Color(0xFF659D92), Color(0xFF28584F)),
            Modifier.fillMaxWidth(),
            { chooseMode("ACTIVITIES") },
        )
        OutlinedButton(onClick = { link = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Link, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Открыть анкету по ссылке")
        }
    }
    if (link) ProfileLinkDialog({ link = false }, openProfile)
}

@Composable
private fun ExploreTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier,
    action: () -> Unit,
) {
    Box(
        modifier
            .heightIn(min = 172.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = action)
    ) {
        BloomMark(Modifier.align(Alignment.TopEnd).padding(12.dp).size(56.dp))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = Color.White)
            Spacer(Modifier.height(20.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
