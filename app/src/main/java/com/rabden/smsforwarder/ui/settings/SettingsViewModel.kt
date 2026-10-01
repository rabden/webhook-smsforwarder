package com.rabden.smsforwarder.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rabden.smsforwarder.repository.ContactRepository
import com.rabden.smsforwarder.repository.SettingsRepository
import com.rabden.smsforwarder.util.PermissionHelper
import com.rabden.smsforwarder.util.parseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val webhookUrl: String = "",
    val isForwardingEnabled: Boolean = true,
    val isHighReliabilityMode: Boolean = false,
    val lastForwardedTime: Long = 0L,
    val isBatteryOptimizationIgnored: Boolean = false,
    val deviceName: String = "",
    val customHeaders: Map<String, String> = emptyMap(),
    val filterMode: String = "whitelist"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val contactRepository = ContactRepository(application)
    private val gson = Gson()
    
    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)

    init {
        // Move expensive system call to background thread during init
        viewModelScope.launch(Dispatchers.IO) {
            val ignored = PermissionHelper.isBatteryOptimizationIgnored(application)
            _isBatteryOptimizationIgnored.value = ignored
        }
    }

    // Combine flows in groups to stay within standard 'combine' overloads (up to 5)
    // Use SharingStarted.Eagerly to keep state 'warm' even when tab is not active
    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.webhookUrl,
            repository.isForwardingEnabled,
            repository.isHighReliabilityMode,
            repository.lastForwardedTime,
            repository.deviceName
        ) { url, enabled, highRel, lastTime, device ->
            // Determine default for high reliability if null
            val isHighRel = highRel ?: com.rabden.smsforwarder.util.BrandHelper.isAggressiveBrand()
            Quint(url, enabled, isHighRel, lastTime, device)
        },
        repository.customHeaders,
        _isBatteryOptimizationIgnored,
        contactRepository.filterMode
    ) { quint, headersJson, batteryIgnored, mode ->
        SettingsUiState(
            webhookUrl = quint.url,
            isForwardingEnabled = quint.enabled,
            isHighReliabilityMode = quint.highRel,
            lastForwardedTime = quint.lastTime ?: 0L,
            isBatteryOptimizationIgnored = batteryIgnored,
            deviceName = quint.device,
            customHeaders = parseHeaders(headersJson),
            filterMode = mode
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private data class Quint(
        val url: String,
        val enabled: Boolean,
        val highRel: Boolean,
        val lastTime: Long?,
        val device: String
    )

    private fun serializeHeaders(headers: Map<String, String>): String {
        return gson.toJson(headers)
    }

    fun refreshBatteryStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val ignored = PermissionHelper.isBatteryOptimizationIgnored(getApplication())
            _isBatteryOptimizationIgnored.value = ignored
        }
    }

    fun updateWebhookUrl(url: String) {
        viewModelScope.launch {
            repository.updateWebhookUrl(url)
        }
    }

    fun setForwardingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setForwardingEnabled(enabled)
            updateServiceState(enabled, uiState.value.isHighReliabilityMode)
        }
    }

    fun setHighReliabilityMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHighReliabilityMode(enabled)
            updateServiceState(uiState.value.isForwardingEnabled, enabled)
        }
    }

    private fun updateServiceState(forwardingEnabled: Boolean, highRelEnabled: Boolean) {
        if (forwardingEnabled && highRelEnabled) {
            com.rabden.smsforwarder.service.SmsForwardingService.start(getApplication())
        } else {
            com.rabden.smsforwarder.service.SmsForwardingService.stop(getApplication())
        }
    }

    fun updateDeviceName(name: String) {
        viewModelScope.launch {
            repository.updateDeviceName(name)
        }
    }

    fun addHeader(key: String, value: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            val current = uiState.value.customHeaders.toMutableMap()
            current[key] = value
            repository.updateCustomHeaders(serializeHeaders(current))
        }
    }

    fun removeHeader(key: String) {
        viewModelScope.launch {
            val current = uiState.value.customHeaders.toMutableMap()
            current.remove(key)
            repository.updateCustomHeaders(serializeHeaders(current))
        }
    }

    fun setFilterMode(mode: String) {
        viewModelScope.launch {
            contactRepository.setFilterMode(mode)
        }
    }
}
