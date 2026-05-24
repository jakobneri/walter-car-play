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
    val APP_LIST = stringPreferencesKey("app_list")
    val DELAY_SECONDS = intPreferencesKey("delay_seconds")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val SET_VOLUME = booleanPreferencesKey("set_volume")
    val VOLUME_LEVEL = intPreferencesKey("volume_level")
}

const val DEFAULT_APPS = ""
const val APP_SEPARATOR = "|"

class AppPreferences(private val ctx: Context) {

    val serviceEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.SERVICE_ENABLED] ?: true }
    val appList: Flow<List<String>> = ctx.dataStore.data.map {
        (it[Keys.APP_LIST] ?: DEFAULT_APPS).split(APP_SEPARATOR).filter { p -> p.isNotBlank() }
    }
    val delaySeconds: Flow<Int> = ctx.dataStore.data.map { it[Keys.DELAY_SECONDS] ?: 2 }
    val keepScreenOn: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: false }
    val setVolume: Flow<Boolean> = ctx.dataStore.data.map { it[Keys.SET_VOLUME] ?: false }
    val volumeLevel: Flow<Int> = ctx.dataStore.data.map { it[Keys.VOLUME_LEVEL] ?: 80 }

    suspend fun setServiceEnabled(v: Boolean) = ctx.dataStore.edit { it[Keys.SERVICE_ENABLED] = v }
    suspend fun setAppList(list: List<String>) =
        ctx.dataStore.edit { it[Keys.APP_LIST] = list.joinToString(APP_SEPARATOR) }
    suspend fun setDelaySeconds(v: Int) = ctx.dataStore.edit { it[Keys.DELAY_SECONDS] = v }
    suspend fun setKeepScreenOn(v: Boolean) = ctx.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = v }
    suspend fun setSetVolume(v: Boolean) = ctx.dataStore.edit { it[Keys.SET_VOLUME] = v }
    suspend fun setVolumeLevel(v: Int) = ctx.dataStore.edit { it[Keys.VOLUME_LEVEL] = v }
}
