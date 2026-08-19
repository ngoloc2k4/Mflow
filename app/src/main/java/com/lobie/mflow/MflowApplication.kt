package com.lobie.mflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.lobie.mflow.data.api.InnerTubeApi
import com.lobie.mflow.data.api.KtorClientFactory
import com.lobie.mflow.data.api.SponsorBlockApi
import com.lobie.mflow.data.local.MflowDatabase
import com.lobie.mflow.data.local.preferences.AppPreferences
import com.lobie.mflow.data.repository.MusicRepository
import com.lobie.mflow.data.repository.SponsorBlockRepository
import org.conscrypt.Conscrypt
import java.security.Security

class MflowApplication : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "mflow_playback_channel"
        lateinit var instance: MflowApplication
            private set
    }

    val database: MflowDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            MflowDatabase::class.java,
            "mflow.db"
        ).fallbackToDestructiveMigration().build()
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(applicationContext)
    }

    private val httpClient by lazy {
        KtorClientFactory.create()
    }

    val innerTubeApi: InnerTubeApi by lazy {
        InnerTubeApi(httpClient)
    }

    val sponsorBlockApi: SponsorBlockApi by lazy {
        SponsorBlockApi(httpClient)
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepository(innerTubeApi, database.songDao(), database.historyDao(), database.favoriteDao())
    }

    val sponsorBlockRepository: SponsorBlockRepository by lazy {
        SponsorBlockRepository(sponsorBlockApi)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Conscrypt for modern TLS 1.3 & Root CA support on Android 7 (API 24)
        setupConscrypt()

        // Create Notification Channel for Android 8+ (Safe on Android 7)
        createNotificationChannel()
    }

    private fun setupConscrypt() {
        try {
            val conscryptProvider = Conscrypt.newProvider()
            Security.insertProviderAt(conscryptProvider, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
