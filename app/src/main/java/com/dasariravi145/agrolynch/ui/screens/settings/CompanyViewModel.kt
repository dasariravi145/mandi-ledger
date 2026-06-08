package com.dasariravi145.agrolynch.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val repository: CompanyRepository
) : ViewModel() {

    private val _profile = repository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val profile = _profile

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    fun updateProfile(updated: CompanyProfileEntity) {
        viewModelScope.launch {
            when (val result = repository.updateProfile(updated)) {
                is Resource.Success -> _message.emit("Profile updated successfully! / ప్రొఫైల్ అప్‌డేట్ చేయబడింది!")
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
