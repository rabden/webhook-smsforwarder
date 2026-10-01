package com.rabden.smsforwarder.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "contacts")

class ContactRepository(private val context: Context) {
    private val CUSTOM_NUMBERS_KEY = stringSetPreferencesKey("custom_numbers_set")
    private val BLACKLIST_NUMBERS_KEY = stringSetPreferencesKey("blacklist_numbers_set")
    private val FILTER_MODE_KEY = stringPreferencesKey("filter_mode")

    val customNumbers: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_NUMBERS_KEY] ?: emptySet()
        }

    val blacklistNumbers: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[BLACKLIST_NUMBERS_KEY] ?: emptySet()
        }

    val filterMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[FILTER_MODE_KEY] ?: "whitelist"
        }

    suspend fun addCustomNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_NUMBERS_KEY] ?: emptySet()
            preferences[CUSTOM_NUMBERS_KEY] = current + number
        }
    }

    suspend fun removeCustomNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_NUMBERS_KEY] ?: emptySet()
            preferences[CUSTOM_NUMBERS_KEY] = current - number
        }
    }

    suspend fun addBlacklistNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLACKLIST_NUMBERS_KEY] ?: emptySet()
            preferences[BLACKLIST_NUMBERS_KEY] = current + number
        }
    }

    suspend fun removeBlacklistNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLACKLIST_NUMBERS_KEY] ?: emptySet()
            preferences[BLACKLIST_NUMBERS_KEY] = current - number
        }
    }

    suspend fun setFilterMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[FILTER_MODE_KEY] = mode
        }
    }
}
