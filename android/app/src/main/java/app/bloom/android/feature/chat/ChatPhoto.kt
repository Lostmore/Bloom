package app.bloom.android.feature.chat

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bloom.android.AppGraph
import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class PhotoSource(val value: String, val local: Boolean = false)

private val decoding = Semaphore(2)

@Composable
fun ChatPhoto(graph: AppGraph, source: PhotoSource, modifier: Modifier, fit: Boolean = false) {
    val resolver = LocalContext.current.contentResolver
    var bitmap by remember(source) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(source) { mutableStateOf(false) }
    var retry by remember(source) { mutableIntStateOf(0) }
    LaunchedEffect(source, retry) {
        failed = false
        try {
            bitmap = decoding.withPermit {
                withContext(Dispatchers.IO) {
                    val bytes =
                        if (source.local)
                            resolver.openInputStream(Uri.parse(source.value))?.use { it.readImageBytes() }
                                ?: error("Фото недоступно")
                        else
                            graph.media.image(source.value).use { response ->
                                require(response.contentType()?.type == "image")
                                response.byteStream().use { it.readImageBytes() }
                            }
                    if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _
                                ->
                                val scale = (maxOf(info.size.width, info.size.height) / 1200f).coerceAtLeast(1f)
                                decoder.setTargetSize(
                                    (info.size.width / scale).toInt().coerceAtLeast(1),
                                    (info.size.height / scale).toInt().coerceAtLeast(1),
                                )
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                            .asImageBitmap()
                    } else {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        options.inJustDecodeBounds = false
                        options.inSampleSize = 1
                        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1200) options
                            .inSampleSize *= 2
                        checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)).asImageBitmap()
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            failed = true
        }
    }
    if (bitmap != null)
        Image(bitmap!!, "Фото", modifier, contentScale = if (fit) ContentScale.Fit else ContentScale.Crop)
    else
        Box(
            modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (failed) Modifier.clickable { retry++ } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (failed)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.BrokenImage, "Фото недоступно")
                    Text("Повторить", style = MaterialTheme.typography.labelSmall)
                }
            else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
}
