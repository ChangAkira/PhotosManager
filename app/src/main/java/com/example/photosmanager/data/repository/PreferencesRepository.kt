package com.example.photosmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        
        // 默认值
        const val DEFAULT_PHOTOS_PER_SESSION = 5
        const val DEFAULT_NEARBY_PHOTOS_COUNT = 10
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
}


