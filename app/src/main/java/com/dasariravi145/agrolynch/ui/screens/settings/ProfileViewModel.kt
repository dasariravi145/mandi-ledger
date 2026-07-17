package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
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
        viewModelScope.launch {
            userRepository.getUserProfile().collect { profile ->
                _user.value = profile
            }
        }
    }

    fun updateProfile(name: String, location: String) {
        val current = _user.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val updated = current.copy(name = name, location = location)
            val result = userRepository.saveProfile(updated)
            when (result) {
                is Resource.Success -> {
                    _message.emit("Profile updated successfully")
                }
                is Resource.Error -> {
                    _message.emit(result.message ?: "Failed to update profile")
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
