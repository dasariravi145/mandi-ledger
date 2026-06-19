package com.dasariravi145.agrolynch.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val repository: CompanyRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _profile = repository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val profile = _profile

    val isPremium = premiumStateManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    fun updateProfile(updated: CompanyProfileEntity) {
        viewModelScope.launch {
            when (val result = repository.updateProfile(updated)) {
                is Resource.Success -> _message.emit("profile_updated_success")
                is Resource.Error -> _message.emit("Update failed: ${result.message}")
                else -> {}
            }
        }
    }

    fun saveAssetLocally(context: Context, uri: Uri, assetType: String) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "${assetType}_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                
                val current = _profile.value ?: CompanyProfileEntity()
                val updated = when (assetType) {
                    "logo" -> current.copy(logoPath = file.absolutePath)
                    "signature" -> current.copy(signaturePath = file.absolutePath)
                    "stamp" -> current.copy(stampPath = file.absolutePath)
                    "god" -> current.copy(godImagePath = file.absolutePath)
                    "upi_qr" -> current.copy(upiQrPath = file.absolutePath)
                    "fruit" -> current.copy(fruitImagePath = file.absolutePath)
                    "custom_template" -> current.copy(customTemplatePath = file.absolutePath)
                    else -> current
                }
                repository.updateProfile(updated)
                _message.emit("${assetType.uppercase()} saved!")
            } catch (e: Exception) {
                _message.emit("Failed to save image")
            }
        }
    }
}
