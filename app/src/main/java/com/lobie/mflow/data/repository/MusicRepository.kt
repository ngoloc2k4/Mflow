package com.lobie.mflow.data.repository

import com.lobie.mflow.data.api.InnerTubeApi
import com.lobie.mflow.data.api.SearchItem
import com.lobie.mflow.data.api.StreamInfo
import com.lobie.mflow.data.local.dao.FavoriteDao
import com.lobie.mflow.data.local.dao.HistoryDao
import com.lobie.mflow.data.local.dao.SongDao
import com.lobie.mflow.data.local.entity.FavoriteEntity
import com.lobie.mflow.data.local.entity.HistoryEntity
import com.lobie.mflow.data.local.entity.SongEntity
import com.lobie.mflow.player.model.MflowMediaItem
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val api: InnerTubeApi,
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao
) {
    suspend fun search(query: String): List<SearchItem> {
        return api.search(query)
    }

    suspend fun getHomeFeed(): List<SearchItem> {
        return api.getHomeFeed()
    }

    suspend fun getRecommendations(seedVideoId: String): List<SearchItem> {
        return api.getRecommendationsFromLastPlayed(seedVideoId)
    }

    suspend fun getStreamInfo(videoId: String, forceRefresh: Boolean = false): StreamInfo? {
        if (!forceRefresh) {
            val cachedSong = songDao.getSong(videoId)
            if (cachedSong != null && cachedSong.cachedStreamUrl != null &&
                cachedSong.streamExpireAt > System.currentTimeMillis() + (60 * 1000)
            ) {
                return StreamInfo(
                    videoId = cachedSong.id,
                    audioUrl = cachedSong.cachedStreamUrl,
                    format = "m4a",
                    bitrate = 128000,
                    expireAtTimestamp = cachedSong.streamExpireAt
                )
            }
        }

        val freshInfo = api.getStreamInfo(videoId) ?: return null

        songDao.insertSong(
            SongEntity(
                id = videoId,
                title = "",
                artist = "",
                album = null,
                durationMs = 0,
                thumbnailUrl = null,
                cachedStreamUrl = freshInfo.audioUrl,
                streamExpireAt = freshInfo.expireAtTimestamp
            )
        )

        return freshInfo
    }

    suspend fun recordHistory(item: MflowMediaItem) {
        historyDao.insertHistory(
            HistoryEntity(
                songId = item.id,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl
            )
        )
        historyDao.trimHistory()
    }

    fun getRecentHistory(): Flow<List<HistoryEntity>> = historyDao.getRecentHistory()

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    fun isFavorite(songId: String): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(item: MflowMediaItem, isFav: Boolean) {
        if (isFav) {
            favoriteDao.removeFavorite(item.id)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    songId = item.id,
                    title = item.title,
                    artist = item.artist,
                    album = item.album,
                    durationMs = item.durationMs,
                    thumbnailUrl = item.thumbnailUrl
                )
            )
        }
    }
}
