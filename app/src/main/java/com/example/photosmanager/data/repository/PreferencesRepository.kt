package com.example.photosmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photos_manager_prefs")

class PreferencesRepository(private val context: Context) {
    companion object {
        private val KEY_PHOTOS_PER_SESSION = intPreferencesKey("photos_per_session")
        private val KEY_NEARBY_PHOTOS_COUNT = intPreferencesKey("nearby_photos_count")
        private val KEY_ENABLED_ALBUMS = stringSetPreferencesKey("enabled_albums")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val KEY_ACTIVE_SESSION = stringPreferencesKey("active_session")
        private val KEY_THEME_PALETTE = stringPreferencesKey("theme_palette")
        private val KEY_DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        private val KEY_DND_START = intPreferencesKey("dnd_start_minute")
        private val KEY_DND_END = intPreferencesKey("dnd_end_minute")
        
        // 默认值
        const val DEFAULT_PHOTOS_PER_SESSION = 5
        const val DEFAULT_NEARBY_PHOTOS_COUNT = 10
        /** 免打扰默认 22:00-07:00，跨过午夜 */
        const val DEFAULT_DND_START = 22 * 60
        const val DEFAULT_DND_END = 7 * 60
        const val DEFAULT_THEME_PALETTE = "beige"
    }

    val photosPerSession: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PHOTOS_PER_SESSION] ?: DEFAULT_PHOTOS_PER_SESSION
    }

    val nearbyPhotosCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_NEARBY_PHOTOS_COUNT] ?: DEFAULT_NEARBY_PHOTOS_COUNT
    }

    val enabledAlbums: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_ENABLED_ALBUMS] ?: emptySet()
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH] ?: true
    }

    val isServiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVICE_ENABLED] ?: false
    }

    /** 未完成的一轮询问，JSON 序列化后存这里，进程被杀也能恢复 */
    val activeSessionJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_SESSION]
    }

    val themePaletteId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_PALETTE] ?: DEFAULT_THEME_PALETTE
    }

    /** 免打扰开关。关掉的时候亮屏照常弹悬浮窗 */
    val dndEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DND_ENABLED] ?: false
    }

    /** 免打扰起止时间，都是"当天第几分钟"，0 到 1439。结束小于开始表示跨午夜 */
    val dndStartMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DND_START] ?: DEFAULT_DND_START
    }

    val dndEndMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DND_END] ?: DEFAULT_DND_END
    }

    suspend fun setPhotosPerSession(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PHOTOS_PER_SESSION] = count
        }
    }

    suspend fun setNearbyPhotosCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NEARBY_PHOTOS_COUNT] = count
        }
    }

    suspend fun setEnabledAlbums(albums: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLED_ALBUMS] = albums
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setActiveSession(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SESSION] = json
        }
    }

    suspend fun clearActiveSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ACTIVE_SESSION)
        }
    }

    suspend fun setThemePalette(id: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PALETTE] = id
        }
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DND_ENABLED] = enabled
        }
    }

    suspend fun setDndStartMinute(minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DND_START] = minute.coerceIn(0, 24 * 60 - 1)
        }
    }

    suspend fun setDndEndMinute(minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DND_END] = minute.coerceIn(0, 24 * 60 - 1)
        }
    }

    /** 当前时刻是否落在免打扰时段内。结束时间小于开始时间表示这段跨了午夜 */
    fun inDoNotDisturb(startMinute: Int, endMinute: Int, nowMinute: Int): Boolean =
        if (startMinute <= endMinute) nowMinute in startMinute until endMinute
        else nowMinute >= startMinute || nowMinute < endMinute
}


