package com.lobie.mflow.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lobie.mflow.ui.theme.DarkSurfaceVariant

/**
 * Optimized Image Loader for Android 7 low-RAM devices:
 * Uses RGB_565 config for 50% memory saving on bitmap caches.
 */
@Composable
fun AsyncImageCompat(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    if (url.isNullOrEmpty()) {
        Box(modifier = modifier.background(DarkSurfaceVariant))
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .crossfade(200)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
