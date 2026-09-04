package com.lobie.mflow.player.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

@Immutable
data class MflowMediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val thumbnailUrl: String? = null,
    val streamUrl: String? = null,
    val isFavorite: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl ?: "") // Empty URI if not available - will trigger error handling
            .setMediaMetadata(metadata)
            .setCustomCacheKey(id) // Enable caching with video ID as key
            .build()
    }

    fun copyWithStreamUrl(url: String): MflowMediaItem {
        return copy(streamUrl = url, isError = false, errorMessage = null, isLoading = false)
    }

    fun copyWithError(message: String): MflowMediaItem {
        return copy(isError = true, errorMessage = message, isLoading = false)
    }

    fun copyWithLoading(): MflowMediaItem {
        return copy(isLoading = true, isError = false, errorMessage = null)
    }

    companion object {
        fun fromMediaItem(item: MediaItem, streamUrl: String? = null): MflowMediaItem {
            val meta = item.mediaMetadata
            return MflowMediaItem(
                id = item.mediaId,
                title = meta.title?.toString() ?: "Unknown Title",
                artist = meta.artist?.toString() ?: "Unknown Artist",
                album = meta.albumTitle?.toString(),
                thumbnailUrl = meta.artworkUri?.toString(),
                streamUrl = streamUrl ?: item.localConfiguration?.uri?.toString()
            )
        }

        fun fromSearchItem(
            id: String,
            title: String,
            artist: String,
            album: String? = null,
            durationSeconds: Long = 0,
            thumbnailUrl: String? = null
        ): MflowMediaItem {
            return MflowMediaItem(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationSeconds * 1000,
                thumbnailUrl = thumbnailUrl
            )
        }
    }
}
