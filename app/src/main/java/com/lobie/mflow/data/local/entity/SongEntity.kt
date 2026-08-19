package com.lobie.mflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val cachedStreamUrl: String?,
    val streamExpireAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val addedAt: Long = System.currentTimeMillis()
)
