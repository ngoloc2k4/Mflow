package com.lobie.mflow.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.regex.Pattern

@Serializable
data class SearchItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String? = null,
    val isExplicit: Boolean = false
)

@Serializable
data class StreamInfo(
    val videoId: String,
    val audioUrl: String,
    val format: String, // hls, m4a, opus
    val bitrate: Int,
    val expireAtTimestamp: Long = 0
)

class InnerTubeApi(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun search(query: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val requestBody = """
        {
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20240722.01.00",
                    "hl": "vi",
                    "gl": "VN"
                }
            },
            "query": "$query"
        }
        """.trimIndent()

        try {
            val responseText = client.post("https://music.youtube.com/youtubei/v1/search") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("X-YouTube-Client-Name", "67")
                header("X-YouTube-Client-Version", "1.20240722.01.00")
                setBody(requestBody)
            }.bodyAsText()

            val results = parseSearchResponse(responseText)
            if (results.isNotEmpty()) {
                return@withContext results
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to WEB search
        try {
            val webBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB",
                        "clientVersion": "2.20240501.01.00",
                        "hl": "vi",
                        "gl": "VN"
                    }
                },
                "query": "$query"
            }
            """.trimIndent()
            val webResponse = client.post("https://www.youtube.com/youtubei/v1/search") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setBody(webBody)
            }.bodyAsText()

            return@withContext parseSearchResponse(webResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getHomeFeed(): List<SearchItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchItem>()

        // 1. Fetch FEmusic_home & FEmusic_explore
        val browseIds = listOf("FEmusic_home", "FEmusic_explore")
        for (browseId in browseIds) {
            try {
                val requestBody = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240722.01.00",
                            "hl": "vi",
                            "gl": "VN"
                        }
                    },
                    "browseId": "$browseId"
                }
                """.trimIndent()

                val responseText = client.post("https://music.youtube.com/youtubei/v1/browse") {
                    contentType(ContentType.Application.Json)
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    header("Origin", "https://music.youtube.com")
                    header("Referer", "https://music.youtube.com/")
                    header("X-YouTube-Client-Name", "67")
                    header("X-YouTube-Client-Version", "1.20240722.01.00")
                    setBody(requestBody)
                }.bodyAsText()

                val parsed = parseSearchResponse(responseText)
                results.addAll(parsed)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback if empty: Load trending chart hits
        if (results.size < 10) {
            val trendingQueries = listOf("nhạc việt thịnh hành", "top hits vietnam", "v-pop hot")
            for (q in trendingQueries) {
                try {
                    val searchItems = search(q)
                    results.addAll(searchItems)
                    if (results.size >= 25) break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return@withContext results.distinctBy { it.id }
    }

    suspend fun getRecommendationsFromLastPlayed(seedVideoId: String): List<SearchItem> = withContext(Dispatchers.IO) {
        try {
            val requestBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240722.01.00",
                        "hl": "vi",
                        "gl": "VN"
                    }
                },
                "videoId": "$seedVideoId",
                "playlistId": "RDAMVM$seedVideoId",
                "isAudioOnly": true
            }
            """.trimIndent()

            val responseText = client.post("https://music.youtube.com/youtubei/v1/next") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("X-YouTube-Client-Name", "67")
                header("X-YouTube-Client-Version", "1.20240722.01.00")
                setBody(requestBody)
            }.bodyAsText()

            val results = mutableListOf<SearchItem>()
            val root = json.parseToJsonElement(responseText).jsonObject
            findPanelTracks(root, results)
            return@withContext results.distinctBy { it.id }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getStreamInfo(videoId: String, retryCount: Int = 0): StreamInfo? = withContext(Dispatchers.IO) {
        val maxRetries = 2
        val currentRetry = retryCount.coerceAtMost(maxRetries)
        
        // Method 1: Extract HLS stream URL from mobile web watch page (Plays without 403 / Signature issues)
        try {
            val html = client.get("https://m.youtube.com/watch?v=$videoId") {
                header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
            }.bodyAsText()

            val pattern = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});")
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonStr = matcher.group(1)
                if (jsonStr != null) {
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val streamingData = root["streamingData"]?.jsonObject
                    if (streamingData != null) {
                        val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
                        if (!hlsUrl.isNullOrEmpty()) {
                            Log.d("MflowStream", "Resolved HLS Stream for $videoId: $hlsUrl")
                            return@withContext StreamInfo(
                                videoId = videoId,
                                audioUrl = hlsUrl,
                                format = "hls",
                                bitrate = 160000,
                                expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                            )
                        }

                        // Try direct audio formats - prefer higher bitrate
                        val formats = (streamingData["adaptiveFormats"]?.jsonArray ?: JsonArray(emptyList())) +
                                (streamingData["formats"]?.jsonArray ?: JsonArray(emptyList()))
                        
                        var bestFormat: JsonObject? = null
                        var bestBitrate = 0
                        
                        for (fmt in formats) {
                            val fmtObj = fmt.jsonObject
                            val mime = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            if (mime.startsWith("audio/")) {
                                val bitrate = fmtObj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                                if (bitrate > bestBitrate) {
                                    bestBitrate = bitrate
                                    bestFormat = fmtObj
                                }
                            }
                        }
                        
                        if (bestFormat != null) {
                            val url = bestFormat["url"]?.jsonPrimitive?.contentOrNull
                            val mime = bestFormat["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            if (!url.isNullOrEmpty()) {
                                Log.d("MflowStream", "Resolved Direct Audio for $videoId (bitrate: $bestBitrate)")
                                return@withContext StreamInfo(
                                    videoId = videoId,
                                    audioUrl = url,
                                    format = if (mime.contains("opus")) "opus" else "m4a",
                                    bitrate = bestBitrate,
                                    expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MflowStream", "Method 1 (mobile) failed for $videoId: ${e.message}")
        }

        // Method 2: Desktop Watch Page Extraction
        try {
            val desktopHtml = client.get("https://www.youtube.com/watch?v=$videoId") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }.bodyAsText()

            val idx = desktopHtml.indexOf("ytInitialPlayerResponse = ")
            if (idx != -1) {
                val start = idx + "ytInitialPlayerResponse = ".length
                val end = desktopHtml.indexOf(";</script>", start)
                if (end != -1) {
                    val rawJson = desktopHtml.substring(start, end)
                    val info = parsePlayerResponse(videoId, rawJson)
                    if (info != null) {
                        Log.d("MflowStream", "Resolved Desktop Stream for $videoId: ${info.audioUrl}")
                        return@withContext info
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MflowStream", "Method 2 (desktop) failed for $videoId: ${e.message}")
        }

        // Method 3: Try with different client (ANDROID_MUSIC) if previous methods failed
        if (currentRetry < maxRetries) {
            try {
                Log.d("MflowStream", "Attempting method 3 (ANDROID_MUSIC client) for $videoId (retry $currentRetry/$maxRetries)")
                val requestBody = """
                {
                    "context": {
                        "client": {
                            "clientName": "ANDROID_MUSIC",
                            "clientVersion": "6.35.52",
                            "androidSdkVersion": 30,
                            "hl": "vi",
                            "gl": "VN"
                        }
                    },
                    "videoId": "$videoId",
                    "contentCheckOk": true,
                    "racyCheckOk": true
                }
                """.trimIndent()
                
                val responseText = client.post("https://music.youtube.com/youtubei/v1/player") {
                    contentType(ContentType.Application.Json)
                    header("User-Agent", "com.google.android.apps.youtube.music/6.35.52 (Linux; U; Android 11)")
                    setBody(requestBody)
                }.bodyAsText()
                
                val root = json.parseToJsonElement(responseText).jsonObject
                val streamingData = root["streamingData"]?.jsonObject
                if (streamingData != null) {
                    val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
                    if (!hlsUrl.isNullOrEmpty()) {
                        Log.d("MflowStream", "Resolved ANDROID_MUSIC HLS Stream for $videoId")
                        return@withContext StreamInfo(
                            videoId = videoId,
                            audioUrl = hlsUrl,
                            format = "hls",
                            bitrate = 160000,
                            expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                        )
                    }
                    
                    val formats = (streamingData["adaptiveFormats"]?.jsonArray ?: JsonArray(emptyList()))
                    for (fmt in formats) {
                        val fmtObj = fmt.jsonObject
                        val mime = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull
                        if (mime.startsWith("audio/") && !url.isNullOrEmpty()) {
                            Log.d("MflowStream", "Resolved ANDROID_MUSIC Direct Audio for $videoId")
                            return@withContext StreamInfo(
                                videoId = videoId,
                                audioUrl = url,
                                format = if (mime.contains("opus")) "opus" else "m4a",
                                bitrate = fmtObj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 128000,
                                expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MflowStream", "Method 3 (ANDROID_MUSIC) failed for $videoId: ${e.message}")
            }
        }

        // Method 4: Fallback to WEB_REMIX client as last resort
        try {
            Log.d("MflowStream", "Attempting method 4 (WEB_REMIX client) for $videoId")
            val requestBody = """
            {
                "context": {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20240722.01.00",
                        "hl": "vi",
                        "gl": "VN"
                    }
                },
                "videoId": "$videoId",
                "contentCheckOk": true,
                "racyCheckOk": true
            }
            """.trimIndent()
            
            val responseText = client.post("https://music.youtube.com/youtubei/v1/player") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("X-YouTube-Client-Name", "67")
                header("X-YouTube-Client-Version", "1.20240722.01.00")
                setBody(requestBody)
            }.bodyAsText()
            
            val root = json.parseToJsonElement(responseText).jsonObject
            val streamingData = root["streamingData"]?.jsonObject
            if (streamingData != null) {
                val hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
                if (!hlsUrl.isNullOrEmpty()) {
                    Log.d("MflowStream", "Resolved WEB_REMIX HLS Stream for $videoId")
                    return@withContext StreamInfo(
                        videoId = videoId,
                        audioUrl = hlsUrl,
                        format = "hls",
                        bitrate = 160000,
                        expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                    )
                }
                
                val formats = (streamingData["adaptiveFormats"]?.jsonArray ?: JsonArray(emptyList()))
                for (fmt in formats) {
                    val fmtObj = fmt.jsonObject
                    val mime = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull
                    if (mime.startsWith("audio/") && !url.isNullOrEmpty()) {
                        Log.d("MflowStream", "Resolved WEB_REMIX Direct Audio for $videoId")
                        return@withContext StreamInfo(
                            videoId = videoId,
                            audioUrl = url,
                            format = if (mime.contains("opus")) "opus" else "m4a",
                            bitrate = fmtObj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 128000,
                            expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MflowStream", "Method 4 (WEB_REMIX) failed for $videoId: ${e.message}")
        }

        Log.e("MflowStream", "Failed to resolve stream for $videoId after all methods")
        return@withContext null
    }

    private fun parseSearchResponse(rawJson: String): List<SearchItem> {
        val results = mutableListOf<SearchItem>()
        try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            findItemRenderers(root, results)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun findItemRenderers(element: JsonElement, results: MutableList<SearchItem>) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("musicCardShelfRenderer")) {
                    val item = parseMusicCardItem(element["musicCardShelfRenderer"]!!.jsonObject)
                    if (item != null) results.add(item)
                } else if (element.containsKey("musicResponsiveListItemRenderer")) {
                    val item = parseMusicItem(element["musicResponsiveListItemRenderer"]!!.jsonObject)
                    if (item != null) results.add(item)
                } else if (element.containsKey("musicTwoRowItemRenderer")) {
                    val item = parseTwoRowItem(element["musicTwoRowItemRenderer"]!!.jsonObject)
                    if (item != null) results.add(item)
                } else if (element.containsKey("videoRenderer")) {
                    val item = parseVideoItem(element["videoRenderer"]!!.jsonObject)
                    if (item != null) results.add(item)
                } else if (element.containsKey("compactVideoRenderer")) {
                    val item = parseVideoItem(element["compactVideoRenderer"]!!.jsonObject)
                    if (item != null) results.add(item)
                } else {
                    for (value in element.values) {
                        findItemRenderers(value, results)
                    }
                }
            }
            is JsonArray -> {
                for (item in element) {
                    findItemRenderers(item, results)
                }
            }
            else -> {}
        }
    }

    private fun parseVideoId(renderer: JsonObject): String? {
        // 1. Check in root/playlistItemData
        renderer["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull?.let { return it }

        // 2. Check in navigationEndpoint.watchEndpoint
        renderer["navigationEndpoint"]?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull?.let { return it }
        renderer["navigationEndpoint"]?.jsonObject?.get("watchPlaylistEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull?.let { return it }

        // 3. Check in thumbnailOverlay / musicPlayButtonRenderer
        val overlayEndpoint = renderer["thumbnailOverlay"]?.jsonObject
            ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("musicPlayButtonRenderer")?.jsonObject
            ?.get("playNavigationEndpoint")?.jsonObject
            ?.get("watchEndpoint")?.jsonObject
            ?.get("videoId")?.jsonPrimitive?.contentOrNull
        if (!overlayEndpoint.isNullOrEmpty()) return overlayEndpoint

        // 4. Check in menuRenderer items
        val menuItems = renderer["menu"]?.jsonObject?.get("menuRenderer")?.jsonObject?.get("items")?.jsonArray
        if (menuItems != null) {
            for (item in menuItems) {
                val menuNavVid = item.jsonObject["menuNavigationItemRenderer"]?.jsonObject
                    ?.get("navigationEndpoint")?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                if (!menuNavVid.isNullOrEmpty()) return menuNavVid
            }
        }

        // 5. Check in title runs navigationEndpoint
        val titleRuns = renderer["title"]?.jsonObject?.get("runs")?.jsonArray
        val titleVid = titleRuns?.getOrNull(0)?.jsonObject?.get("navigationEndpoint")?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
        if (!titleVid.isNullOrEmpty()) return titleVid

        // 6. Direct videoId
        return renderer["videoId"]?.jsonPrimitive?.contentOrNull
    }

    private fun parseMusicCardItem(itemObj: JsonObject): SearchItem? {
        return try {
            val titleObj = itemObj["title"]?.jsonObject
            val titleRuns = titleObj?.get("runs")?.jsonArray
            val title = titleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Title"

            val subtitleRuns = itemObj["subtitle"]?.jsonObject?.get("runs")?.jsonArray
            val artist = subtitleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "YouTube Music"

            val videoId = parseVideoId(itemObj) ?: return null

            val thumbs = itemObj["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
            val thumbUrl = thumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            SearchItem(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTwoRowItem(itemObj: JsonObject): SearchItem? {
        return try {
            val titleRuns = itemObj["title"]?.jsonObject?.get("runs")?.jsonArray
            val title = titleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val subtitleRuns = itemObj["subtitle"]?.jsonObject?.get("runs")?.jsonArray
            val artist = subtitleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "YouTube Music"

            val thumbs = itemObj["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
            val thumbUrl = thumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            val videoId = parseVideoId(itemObj) ?: return null

            SearchItem(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMusicItem(itemObj: JsonObject): SearchItem? {
        return try {
            val flexColumns = itemObj["flexColumns"]?.jsonArray ?: return null
            val titleCol = flexColumns.getOrNull(0)?.jsonObject
            val titleRuns = titleCol?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject?.get("runs")?.jsonArray

            val title = titleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Title"

            val artistCol = flexColumns.getOrNull(1)?.jsonObject
            val artistRuns = artistCol?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject?.get("runs")?.jsonArray
            val artist = artistRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
            val album = artistRuns?.getOrNull(2)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull

            val thumbnails = itemObj["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
            val thumbUrl = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            val videoId = parseVideoId(itemObj) ?: return null

            SearchItem(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVideoItem(itemObj: JsonObject): SearchItem? {
        return try {
            val videoId = parseVideoId(itemObj) ?: return null
            val title = itemObj["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?: itemObj["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull ?: "Unknown Title"
            val artist = itemObj["ownerText"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "YouTube"
            val thumbs = itemObj["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
            val thumbUrl = thumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            SearchItem(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findPanelTracks(element: JsonElement, results: MutableList<SearchItem>) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("playlistPanelVideoRenderer")) {
                    val r = element["playlistPanelVideoRenderer"]!!.jsonObject
                    val vid = r["videoId"]?.jsonPrimitive?.contentOrNull
                    val title = r["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    val artist = r["longBylineText"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "YouTube Music"
                    val thumbs = r["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                    val thumbUrl = thumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!vid.isNullOrEmpty() && !title.isNullOrEmpty()) {
                        results.add(
                            SearchItem(
                                id = vid,
                                title = title,
                                artist = artist,
                                thumbnailUrl = thumbUrl
                            )
                        )
                    }
                } else {
                    for (v in element.values) {
                        findPanelTracks(v, results)
                    }
                }
            }
            is JsonArray -> {
                for (item in element) {
                    findPanelTracks(item, results)
                }
            }
            else -> {}
        }
    }

    private fun parsePlayerResponse(videoId: String, rawJson: String): StreamInfo? {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val streamingData = root["streamingData"]?.jsonObject ?: return null

            val hls = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull
            if (!hls.isNullOrEmpty()) {
                return StreamInfo(
                    videoId = videoId,
                    audioUrl = hls,
                    format = "hls",
                    bitrate = 160000,
                    expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                )
            }

            val formats = (streamingData["adaptiveFormats"]?.jsonArray ?: JsonArray(emptyList())) +
                    (streamingData["formats"]?.jsonArray ?: JsonArray(emptyList()))

            var bestUrl: String? = null
            var bestBitrate = 0
            var formatType = "m4a"

            for (fmt in formats) {
                val fmtObj = fmt.jsonObject
                val mimeType = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                if (mimeType.startsWith("audio/")) {
                    val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull
                    val bitrate = fmtObj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                    if (url != null && bitrate > bestBitrate) {
                        bestUrl = url
                        bestBitrate = bitrate
                        formatType = if (mimeType.contains("opus")) "opus" else "m4a"
                    }
                }
            }

            if (bestUrl != null) {
                StreamInfo(
                    videoId = videoId,
                    audioUrl = bestUrl,
                    format = formatType,
                    bitrate = bestBitrate,
                    expireAtTimestamp = System.currentTimeMillis() + (5 * 3600 * 1000)
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
