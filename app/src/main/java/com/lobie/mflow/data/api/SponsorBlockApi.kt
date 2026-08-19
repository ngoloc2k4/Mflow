package com.lobie.mflow.data.api

import com.lobie.mflow.player.model.SkipSegment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class SponsorSegmentResponse(
    val category: String,
    val segment: List<Double>,
    val actionType: String? = null,
    val UUID: String? = null
)

class SponsorBlockApi(private val client: HttpClient) {

    suspend fun getSkipSegments(
        videoId: String,
        categories: List<String> = listOf("sponsor", "intro", "outro", "music_offtopic", "preview", "filler")
    ): List<SkipSegment> {
        return try {
            val categoriesJson = categories.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            val response: List<SponsorSegmentResponse> = client.get("https://sponsor.ajay.app/api/skipSegments") {
                parameter("videoID", videoId)
                parameter("categories", categoriesJson)
            }.body()

            response.mapNotNull {
                if (it.segment.size >= 2) {
                    val startMs = (it.segment[0] * 1000).toLong()
                    val endMs = (it.segment[1] * 1000).toLong()
                    SkipSegment(
                        category = it.category,
                        startMs = startMs,
                        endMs = endMs
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
