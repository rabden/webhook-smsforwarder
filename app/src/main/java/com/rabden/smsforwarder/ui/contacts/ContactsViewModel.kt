package com.rabden.smsforwarder.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabden.smsforwarder.repository.ContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactRepository(application)

    val customContacts: StateFlow<Set<String>> = repository.customNumbers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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
}
