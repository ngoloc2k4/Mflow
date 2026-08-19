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
    val isFavorite: Boolean = false
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
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
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
    }
}
