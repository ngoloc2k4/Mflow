package com.lobie.mflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lobie.mflow.data.local.dao.FavoriteDao
import com.lobie.mflow.data.local.dao.HistoryDao
import com.lobie.mflow.data.local.dao.SongDao
import com.lobie.mflow.data.local.entity.FavoriteEntity
import com.lobie.mflow.data.local.entity.HistoryEntity
import com.lobie.mflow.data.local.entity.SongEntity

@Database(
    entities = [SongEntity::class, HistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MflowDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
}
