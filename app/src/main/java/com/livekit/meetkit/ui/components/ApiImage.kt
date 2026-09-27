/**
 * Компонент загрузки и кэширования медиа-файлов и аватаров через API.
 */
package com.livekit.meetkit.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.livekit.meetkit.data.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cache = LruCache<String, ImageBitmap>(32)

@Composable
fun rememberApiImage(apiClient: ApiClient, path: String?): ImageBitmap? {
    var image by remember(path) { mutableStateOf(path?.let { cache.get(it) }) }
    LaunchedEffect(path) {
        if (path == null || image != null) return@LaunchedEffect
        val bytes = apiClient.fetchApiBlob(path).getOrNull() ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } ?: return@LaunchedEffect
        cache.put(path, bitmap)
        image = bitmap
    }
    return image
}
