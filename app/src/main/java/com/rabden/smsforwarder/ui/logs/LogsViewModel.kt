package com.rabden.smsforwarder.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabden.smsforwarder.data.AppDatabase
import com.rabden.smsforwarder.data.MessageLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).messageLogDao()

    val logs: StateFlow<List<MessageLog>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun clearAllLogs() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}