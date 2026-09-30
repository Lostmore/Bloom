package app.bloom.android.feature.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnboardingPhotos(photos: List<String>, change: (List<String>) -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
            val kept = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    uri.toString()
                } catch (_: SecurityException) {
                    error = "Не удалось сохранить доступ к фото. Выбери другое изображение."
                    null
                }
            }
            change((photos + kept).distinct().take(6))
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    Box(
                        Modifier.weight(1f)
                            .aspectRatio(0.76f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (index < photos.size) {
                            LocalPhoto(photos[index], Modifier.fillMaxSize())
                            IconButton(
                                onClick = { change(photos.filterIndexed { i, _ -> i != index }) },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    "Удалить фото ${index + 1}",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (index == 0)
                                Surface(
                                    Modifier.align(Alignment.BottomStart),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text("Главное", Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                                }
                        } else
                            IconButton(
                                onClick = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.align(Alignment.Center),
                            ) {
                                Icon(Icons.Outlined.Add, "Добавить фото")
                            }
                    }
                }
            }
        }
        Text(
            "Фото пока сохраняются только на этом устройстве. Другие пользователи их не увидят. Можно продолжить без фото.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        app.bloom.android.core.ui.ErrorMessage(error)
    }
}

@Composable
private fun LocalPhoto(uriValue: String, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by
        produceState<ImageBitmap?>(null, uriValue) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val uri = Uri.parse(uriValue)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        val options =
                            BitmapFactory.Options().apply {
                                inSampleSize = 1
                                while (
                                    bounds.outWidth / inSampleSize > 600 || bounds.outHeight / inSampleSize > 600
                                ) inSampleSize *= 2
                            }
                        resolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
                        }
                    }
                        .getOrNull()
                }
        }
    if (bitmap != null) Image(bitmap!!, "Выбранное фото", modifier, contentScale = ContentScale.Crop)
    else
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Фото недоступно", style = MaterialTheme.typography.labelSmall)
        }
}
