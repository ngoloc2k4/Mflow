package com.lobie.mflow.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "mflow_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val SPONSOR_BLOCK_ENABLED = booleanPreferencesKey("sponsor_block_enabled")
        val SKIP_INTRO_OUTRO = booleanPreferencesKey("skip_intro_outro")
        val SKIP_SPONSORS = booleanPreferencesKey("skip_sponsors")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality") // "high", "medium", "low"
    }

    val sponsorBlockEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[SPONSOR_BLOCK_ENABLED] ?: true
    }

    val skipSponsors: Flow<Boolean> = context.dataStore.data.map {
        it[SKIP_SPONSORS] ?: true
    }

    val skipIntroOutro: Flow<Boolean> = context.dataStore.data.map {
        it[SKIP_INTRO_OUTRO] ?: true
    }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SPONSOR_BLOCK_ENABLED] = enabled }
    }
}
