package com.example.photosmanager.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthRepository(private val context: Context) {
    companion object {
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_REDEEM_CODE = stringPreferencesKey("redeem_code")
    }

    val username: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_USERNAME]
    }

    val userId: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }

    suspend fun login(username: String, password: String): Boolean {
        // 暂时不使用，返回false
        return false
    }

    suspend fun register(username: String, password: String): Boolean {
        // 暂时不使用，返回false
        return false
    }

    suspend fun redeemCode(code: String): Boolean {
        // 暂时不使用，返回false
        return false
    }

    suspend fun logout() {
        // 暂时不使用
    }
}


