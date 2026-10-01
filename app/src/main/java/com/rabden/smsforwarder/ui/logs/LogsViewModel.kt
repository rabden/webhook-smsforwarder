package com.rabden.smsforwarder.ui.logs

import android.app.Application
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabden.smsforwarder.data.AppDatabase
import com.rabden.smsforwarder.data.MessageLog
import com.rabden.smsforwarder.worker.ForwardSmsWorker
import com.rabden.smsforwarder.worker.LogSmsWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).messageLogDao()

    val logs: StateFlow<List<MessageLog>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    val isSelectionMode: StateFlow<Boolean> = _selectedIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleSelection(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun selectAll() {
        _selectedIds.value = logs.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            dao.deleteByIds(_selectedIds.value)
            _selectedIds.value = emptySet()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun retry(log: MessageLog) {
        viewModelScope.launch {
            dao.updateStatus(log.id, "PENDING")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val inputData = Data.Builder()
                .putLong(LogSmsWorker.EXTRA_LOG_ID, log.id)
                .build()
            val request = OneTimeWorkRequestBuilder<ForwardSmsWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(getApplication<Application>())
                .enqueueUniqueWork(
                    "manual_retry_${log.id}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}
