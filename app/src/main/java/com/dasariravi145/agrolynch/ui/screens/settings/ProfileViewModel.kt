package com.dasariravi145.agrolynch.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()
    
    val isPremium = premiumStateManager.isPremium

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _user.value = authRepository.getUserProfile(uid)
        }
    }

    fun updateProfile(name: String, location: String) {
        val current = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val updated = current.copy(name = name, location = location)
            val result = authRepository.registerUser(updated) // Re-uses same DAO method for simplicity
            if (result.isSuccess) {
                _user.value = updated
                _message.emit("Profile updated successfully")
            } else {
                _message.emit("Failed to update profile")
            }
            _isLoading.value = false
        }
    }
}
