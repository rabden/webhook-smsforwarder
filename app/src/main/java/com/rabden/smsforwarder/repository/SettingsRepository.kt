package com.rabden.smsforwarder.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val WEBHOOK_URL = stringPreferencesKey("webhook_url")
    private val FORWARDING_ENABLED = booleanPreferencesKey("forwarding_enabled")
    private val LAST_FORWARDED_TIME = longPreferencesKey("last_forwarded_time")
    private val DEVICE_NAME = stringPreferencesKey("device_name")
    private val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")
    private val HIGH_RELIABILITY_MODE = booleanPreferencesKey("high_reliability_mode")

    val webhookUrl: Flow<String> = context.dataStore.data
        .map { it[WEBHOOK_URL] ?: "" }

    val isForwardingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[FORWARDING_ENABLED] ?: true }

    val lastForwardedTime: Flow<Long?> = context.dataStore.data
        .map { it[LAST_FORWARDED_TIME] }

    val deviceName: Flow<String> = context.dataStore.data
        .map { it[DEVICE_NAME] ?: android.os.Build.MODEL }

    val customHeaders: Flow<String> = context.dataStore.data
        .map { it[CUSTOM_HEADERS] ?: "{}" }

    val isHighReliabilityMode: Flow<Boolean?> = context.dataStore.data
        .map { it[HIGH_RELIABILITY_MODE] }

    suspend fun updateWebhookUrl(url: String) {
        context.dataStore.edit { it[WEBHOOK_URL] = url }
    }

    suspend fun setForwardingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[FORWARDING_ENABLED] = enabled }
    }

    suspend fun setHighReliabilityMode(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_RELIABILITY_MODE] = enabled }
    }

    suspend fun updateLastForwardedTime(timestamp: Long) {
        context.dataStore.edit { it[LAST_FORWARDED_TIME] = timestamp }
    }

    suspend fun updateDeviceName(name: String) {
        context.dataStore.edit { it[DEVICE_NAME] = name }
    }

    suspend fun updateCustomHeaders(json: String) {
        context.dataStore.edit { it[CUSTOM_HEADERS] = json }
    }
}