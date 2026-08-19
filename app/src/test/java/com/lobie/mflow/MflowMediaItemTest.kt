package com.lobie.mflow

import com.lobie.mflow.player.model.MflowMediaItem
import com.lobie.mflow.player.model.SkipSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MflowMediaItemTest {

    @Test
    fun mediaItem_conversion_preservesMetadata() {
        val song = MflowMediaItem(
            id = "test_123",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180000,
            thumbnailUrl = "https://example.com/thumb.jpg",
            streamUrl = "https://example.com/audio.m4a"
        )

        val mediaItem = song.toMediaItem()

        assertEquals("test_123", mediaItem.mediaId)
        assertEquals("Test Song", mediaItem.mediaMetadata.title.toString())
        assertEquals("Test Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals("Test Album", mediaItem.mediaMetadata.albumTitle.toString())
    }

    @Test
    fun skipSegment_intervals_validateCorrectly() {
        val segment = SkipSegment(
            category = "sponsor",
            startMs = 30000L,
            endMs = 45000L
        )

        val positionInside = 35000L
        val isInside = positionInside in segment.startMs..(segment.endMs - 100)

        assertTrue(isInside)
    }
}
