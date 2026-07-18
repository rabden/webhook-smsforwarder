package com.rabden.smsforwarder.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabden.smsforwarder.data.AppDatabase
import com.rabden.smsforwarder.data.MessageLog
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
}
