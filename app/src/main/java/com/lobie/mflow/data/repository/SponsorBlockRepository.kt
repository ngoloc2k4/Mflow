package com.lobie.mflow.data.repository

import com.lobie.mflow.data.api.SponsorBlockApi
import com.lobie.mflow.player.model.SkipSegment
import java.util.concurrent.ConcurrentHashMap

class SponsorBlockRepository(private val api: SponsorBlockApi) {

    private val segmentCache = ConcurrentHashMap<String, List<SkipSegment>>()

    suspend fun getSegments(videoId: String): List<SkipSegment> {
        segmentCache[videoId]?.let { return it }

        val segments = api.getSkipSegments(videoId)
        segmentCache[videoId] = segments
        return segments
    }

    fun clearCache() {
        segmentCache.clear()
    }
}
