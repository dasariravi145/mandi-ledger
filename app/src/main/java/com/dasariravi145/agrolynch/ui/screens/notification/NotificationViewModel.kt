package com.dasariravi145.agrolynch.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    fun updateFCMToken() {
        viewModelScope.launch {
            val token = repository.getFCMToken()
            token?.let {
                repository.updateTokenInFirestore(it)
            }
        }
    }

    fun subscribeToMarketUpdates() {
        viewModelScope.launch {
            repository.subscribeToTopic("market_updates")
        }
    }
}
