package com.dasariravi145.agrolynch.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    val backupHistory: StateFlow<List<BackupEntity>> = repository.getBackupHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createLocalBackup(type: String = "MANUAL") {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.createLocalBackup(type)) {
                is Resource.Success -> {
                    _message.emit("Local backup saved! / లోకల్ బ్యాకప్ సేవ్ చేయబడింది!")
                }
                is Resource.Error -> _message.emit("Backup failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun uploadToCloud(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.uploadBackupToCloud(file)) {
                is Resource.Success -> _message.emit("Cloud backup successful! / క్లౌడ్ బ్యాకప్ విజయవంతమైంది!")
                is Resource.Error -> _message.emit("Upload failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun deleteBackup(id: String) {
        viewModelScope.launch {
            repository.deleteBackup(id)
        }
    }
}
