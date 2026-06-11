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

    // Flow for user-added custom numbers (original behavior)
    val customNumbers: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_NUMBERS_KEY] ?: emptySet()
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
}
