package com.rabden.smsforwarder.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabden.smsforwarder.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactRepository(application)

    val customContacts: StateFlow<Set<String>> = repository.customNumbers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val blacklistContacts: StateFlow<Set<String>> = repository.blacklistNumbers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val filterMode: StateFlow<String> = repository.filterMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "whitelist")

    private val _selectedContacts = MutableStateFlow<Set<String>>(emptySet())
    val selectedContacts: StateFlow<Set<String>> = _selectedContacts

    private val _selectedBlacklistContacts = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlacklistContacts: StateFlow<Set<String>> = _selectedBlacklistContacts

    val isSelectionMode: StateFlow<Boolean> = _selectedContacts
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBlacklistSelectionMode: StateFlow<Boolean> = _selectedBlacklistContacts
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleSelection(number: String) {
        _selectedContacts.update { if (number in it) it - number else it + number }
    }

    fun toggleBlacklistSelection(number: String) {
        _selectedBlacklistContacts.update { if (number in it) it - number else it + number }
    }

    fun selectAll() {
        _selectedContacts.value = customContacts.value.toSet()
    }

    fun selectAllBlacklist() {
        _selectedBlacklistContacts.value = blacklistContacts.value.toSet()
    }

    fun clearSelection() {
        _selectedContacts.value = emptySet()
    }

    fun clearBlacklistSelection() {
        _selectedBlacklistContacts.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedContacts.value.forEach { repository.removeCustomNumber(it) }
            _selectedContacts.value = emptySet()
        }
    }

    fun deleteSelectedBlacklist() {
        viewModelScope.launch {
            _selectedBlacklistContacts.value.forEach { repository.removeBlacklistNumber(it) }
            _selectedBlacklistContacts.value = emptySet()
        }
    }

    fun addContact(phoneNumber: String) {
        viewModelScope.launch {
            repository.addCustomNumber(phoneNumber)
        }
    }

    fun removeContact(phoneNumber: String) {
        viewModelScope.launch {
            repository.removeCustomNumber(phoneNumber)
        }
    }

    fun addBlacklistContact(phoneNumber: String) {
        viewModelScope.launch {
            repository.addBlacklistNumber(phoneNumber)
        }
    }

    fun removeBlacklistContact(phoneNumber: String) {
        viewModelScope.launch {
            repository.removeBlacklistNumber(phoneNumber)
        }
    }

    fun setFilterMode(mode: String) {
        viewModelScope.launch {
            repository.setFilterMode(mode)
        }
    }
}
