package dev.walter.carplay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object Keys {
    val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    val LAUNCH_YOUTUBE_MUSIC = booleanPreferencesKey("launch_youtube_music")
    val LAUNCH_BLITZER = booleanPreferencesKey("launch_blitzer")
    val BLITZER_PACKAGE = stringPreferencesKey("blitzer_package")
    val DELAY_SECONDS = intPreferencesKey("delay_seconds")
}

class AppPreferences(private val ctx: Context) {

    val serviceEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.SERVICE_ENABLED] ?: true }
    val launchYoutubeMusic: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.LAUNCH_YOUTUBE_MUSIC] ?: true }
    val launchBlitzer: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.LAUNCH_BLITZER] ?: true }
    val blitzerPackage: Flow<String> = ctx.dataStore.data.map { it[Keys.BLITZER_PACKAGE] ?: "com.sygic.aura" }
    val delaySeconds: Flow<Int> = ctx.dataStore.data.map { it[Keys.DELAY_SECONDS] ?: 2 }

    suspend fun setServiceEnabled(v: Boolean) = ctx.dataStore.edit { it[Keys.SERVICE_ENABLED] = v }
    suspend fun setLaunchYoutubeMusic(v: Boolean) = ctx.dataStore.edit { it[Keys.LAUNCH_YOUTUBE_MUSIC] = v }
    suspend fun setLaunchBlitzer(v: Boolean) = ctx.dataStore.edit { it[Keys.LAUNCH_BLITZER] = v }
    suspend fun setBlitzerPackage(v: String) = ctx.dataStore.edit { it[Keys.BLITZER_PACKAGE] = v }
    suspend fun setDelaySeconds(v: Int) = ctx.dataStore.edit { it[Keys.DELAY_SECONDS] = v }
}
