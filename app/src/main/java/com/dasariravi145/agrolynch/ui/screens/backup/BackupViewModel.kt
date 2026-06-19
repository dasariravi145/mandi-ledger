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
import timber.log.Timber

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
                    _message.emit("local_backup_saved")
                }
                is Resource.Error -> _message.emit("Backup failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun performManualBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            // 1. Create Local Backup
            when (val localResult = repository.createLocalBackup("MANUAL")) {
                is Resource.Success -> {
                    val file = localResult.data
                    if (file != null && file.exists()) {
                        // Find the local record we just created to get its ID
                        val lastLocal = repository.getBackupHistory().first().find { it.fileName == file.name && it.type == "LOCAL" }
                        // 2. Upload to Cloud
                        when (val cloudResult = repository.uploadBackupToCloud(file, "MANUAL", lastLocal?.id)) {
                            is Resource.Success -> _message.emit("backup_complete_success")
                            is Resource.Error -> _message.emit("Cloud upload failed: ${cloudResult.message}")
                            else -> {}
                        }
                    } else {
                        _message.emit("Local backup file error")
                    }
                }
                is Resource.Error -> _message.emit("Local backup failed: ${localResult.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun restoreBackup(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.restoreFromCloud(id)) {
                is Resource.Success -> _message.emit("restore_success")
                is Resource.Error -> _message.emit("Restore failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun uploadToCloud(file: File, reportType: String, localId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.uploadBackupToCloud(file, reportType, localId)) {
                is Resource.Success -> _message.emit("backup_complete_success")
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

    private val _cloudBackups = MutableStateFlow<List<String>>(emptyList())
    val cloudBackups: StateFlow<List<String>> = _cloudBackups.asStateFlow()

    fun fetchCloudBackups() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.listCloudBackups()) {
                is Resource.Success -> _cloudBackups.value = result.data ?: emptyList()
                is Resource.Error -> _message.emit("Failed to fetch cloud backups: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun restoreFromStoragePath(storagePath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.restoreFromStoragePath(storagePath)) {
                is Resource.Success -> _message.emit("restore_success")
                is Resource.Error -> _message.emit("Restore failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun restoreLatestCloud() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.restoreLatestCloudBackup()) {
                is Resource.Success -> _message.emit("restore_success")
                is Resource.Error -> _message.emit("Restore failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
