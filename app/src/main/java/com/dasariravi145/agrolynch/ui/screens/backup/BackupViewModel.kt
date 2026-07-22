package com.dasariravi145.agrolynch.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BackupEntity
import com.dasariravi145.agrolynch.domain.repository.BackupRepository
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.util.Resource
import com.dasariravi145.agrolynch.util.DeduplicationManager
import com.dasariravi145.agrolynch.util.Formatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject
import android.content.Context
import timber.log.Timber

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val deduplicationManager: DeduplicationManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    private val _restorePreview = MutableStateFlow<RestorePreviewData?>(null)
    val restorePreview: StateFlow<RestorePreviewData?> = _restorePreview.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow("All")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    val latestBackupId: StateFlow<String?> = repository.getBackupHistory()
        .map { history ->
            history.filter { it.status == "SUCCESS" && !it.isDeleted }
                .maxByOrNull { it.timestamp }?.id
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val backupHistory: StateFlow<List<BackupEntity>> = repository.getBackupHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cloudBackups = MutableStateFlow<List<String>>(emptyList())
    val cloudBackups: StateFlow<List<String>> = _cloudBackups.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun permanentDelete(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteBackupPermanently(id)
            _message.emit("Backup slot cleared")
            _isLoading.value = false
        }
    }

    fun createLocalBackup(type: String = "MANUAL") {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.createLocalBackup(type)) {
                is Resource.Success -> {
                    if (type == "MANUAL_EXPORT") {
                        _message.emit("local_backup_ready")
                    } else {
                        _message.emit("Local backup saved successfully")
                    }
                }
                is Resource.Error -> _message.emit("Backup failed: ${result.message ?: "Unknown error"}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun performManualBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            syncRepository.syncAllData()
            when (val localResult = repository.createLocalBackup("MANUAL")) {
                is Resource.Success -> {
                    val file = localResult.data
                    if (file != null && file.exists()) {
                        when (val cloudResult = repository.uploadBackupToCloud(file, "MANUAL", "SLOT_LOCAL")) {
                            is Resource.Success -> _message.emit("Cloud backup successful!")
                            is Resource.Error -> _message.emit(cloudResult.message ?: "Cloud upload failed")
                            else -> {}
                        }
                    }
                }
                is Resource.Error -> _message.emit("Backup failed. Please try again.")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun restoreLocalBackup(id: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val safetyResult = repository.createLocalBackup("PRE_RESTORE_SAFETY")
                if (safetyResult is Resource.Error) {
                    _message.emit("Safety backup failed. Restore cancelled to protect your data.")
                    return@launch
                }

                val backup = backupHistory.value.find { it.id == id }
                if (backup != null) {
                    val file = File(backup.filePath)
                    if (file.exists()) {
                        when (val result = repository.restoreLocalBackup(file)) {
                            is Resource.Success -> {
                                settingsRepository.updateLastRestoreInfo("${backup.type}|${System.currentTimeMillis()}")
                                _message.emit("restore_success")
                            }
                            is Resource.Error -> _message.emit("Restore failed. Your current data was not changed.")
                            else -> {}
                        }
                    } else {
                        _message.emit("Backup file not found in storage.")
                    }
                }
            } catch (e: Exception) {
                _message.emit("An unexpected error occurred during restore.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreLatestCloud() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val safetyResult = repository.createLocalBackup("PRE_RESTORE_SAFETY")
                if (safetyResult is Resource.Error) {
                    _message.emit("Safety backup failed. Restore cancelled.")
                    return@launch
                }

                when (val result = repository.restoreLatestCloudBackup()) {
                    is Resource.Success -> {
                        settingsRepository.updateLastRestoreInfo("CLOUD|${System.currentTimeMillis()}")
                        _message.emit("restore_success")
                    }
                    is Resource.Error -> _message.emit("Restore failed: Unable to download cloud backup.")
                    else -> {}
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadToCloud(file: File, reportType: String, localId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.uploadBackupToCloud(file, reportType, localId)) {
                is Resource.Success -> _message.emit("backup_complete_success")
                is Resource.Error -> _message.emit(result.message ?: "Upload failed")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun fetchCloudBackups() {
        viewModelScope.launch {
            // Remove global isLoading to prevent blocking the entire screen
            when (val result = repository.listCloudBackups()) {
                is Resource.Success -> _cloudBackups.value = result.data ?: emptyList()
                is Resource.Error -> {
                    Timber.e("Cloud list failed: ${result.message}")
                    // Don't emit message for every background refresh to avoid UX noise
                }
                else -> {}
            }
        }
    }

    fun recalculateStock() {
        if (_isLoading.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _isLoading.value = true
            try {
                deduplicationManager.reconcileStockBalances()
                _message.emit("Stock reconciliation completed.")
            } catch (e: Exception) {
                _message.emit("Reconciliation failed.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deduplicateData() {
        if (_isLoading.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _isLoading.value = true
            try {
                deduplicationManager.mergeDuplicates()
                _message.emit("Cleanup complete.")
            } catch (e: Exception) {
                _message.emit("Cleanup failed.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreFromStoragePath(storagePath: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val safetyResult = repository.createLocalBackup("PRE_RESTORE_SAFETY")
                if (safetyResult is Resource.Error) {
                    _message.emit("Safety backup failed. Restore cancelled.")
                    return@launch
                }

                when (val result = repository.restoreFromStoragePath(storagePath)) {
                    is Resource.Success -> {
                        settingsRepository.updateLastRestoreInfo("CLOUD|${System.currentTimeMillis()}")
                        _message.emit("restore_success")
                    }
                    is Resource.Error -> _message.emit("Restore failed: Unable to download backup.")
                    else -> {}
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreFromUri(uri: android.net.Uri, context: android.content.Context) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            val tempFile = File(context.cacheDir, "temp_restore.json")
            try {
                // 1. Mandatory safety backup
                val safetyResult = repository.createLocalBackup("PRE_RESTORE_SAFETY")
                if (safetyResult is Resource.Error) {
                    _message.emit("Safety backup failed. Restore cancelled.")
                    return@launch
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                when (val result = repository.restoreLocalBackup(tempFile)) {
                    is Resource.Success -> {
                        settingsRepository.updateLastRestoreInfo("EXTERNAL|${System.currentTimeMillis()}")
                        _message.emit("restore_success")
                    }
                    is Resource.Error -> _message.emit("The selected backup file is invalid or incomplete.")
                    else -> {}
                }
            } catch (e: Exception) {
                _message.emit("Failed to read backup file")
            } finally {
                if (tempFile.exists()) tempFile.delete()
                _isLoading.value = false
            }
        }
    }

    private val _showRestoreDialog = MutableStateFlow<RestorePreviewData?>(null)
    val showRestoreDialog = _showRestoreDialog.asStateFlow()

    fun onRestoreSelected(action: RestoreAction, context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _isLoading.value = true
            try {
                val preview = when (action) {
                    is RestoreAction.LocalId -> getPreviewFromLocalId(action.id)
                    is RestoreAction.LocalUri -> getPreviewFromUri(action.uri, context)
                    is RestoreAction.CloudPath -> getPreviewFromCloudPath(action.path)
                    is RestoreAction.LatestCloud -> getPreviewFromLatestCloud()
                }
                
                if (preview is Resource.Success) {
                    val lastLocalMod = repository.getLastDataModificationTime()
                    _showRestoreDialog.value = preview.data!!.copy(
                        action = action,
                        isOlderThanLocal = preview.data.createdAt < lastLocalMod
                    )
                } else if (preview is Resource.Error) {
                    _message.emit(preview.message ?: "Failed to read backup preview")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissRestoreDialog() {
        _showRestoreDialog.value = null
    }

    fun confirmRestore(preview: RestorePreviewData, context: Context) {
        _showRestoreDialog.value = null
        val action = preview.action ?: return
        
        viewModelScope.launch {
            when (action) {
                is RestoreAction.LocalId -> restoreLocalBackup(action.id)
                is RestoreAction.LocalUri -> restoreFromUri(action.uri, context)
                is RestoreAction.CloudPath -> restoreFromStoragePath(action.path)
                is RestoreAction.LatestCloud -> restoreLatestCloud()
            }
        }
    }

    private suspend fun getPreviewFromLocalId(id: String): Resource<RestorePreviewData> {
        val backup = backupHistory.value.find { it.id == id } ?: return Resource.Error("Backup not found")
        val file = File(backup.filePath)
        if (!file.exists()) return Resource.Error("Backup file missing")
        return parsePreviewFromFile(file, backup.type)
    }

    private suspend fun getPreviewFromUri(uri: android.net.Uri, context: Context): Resource<RestorePreviewData> {
        val tempFile = File(context.cacheDir, "preview_${System.currentTimeMillis()}.json")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            parsePreviewFromFile(tempFile, "EXTERNAL")
        } catch (e: Exception) {
            Resource.Error("Unreadable backup file")
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun getPreviewFromCloudPath(path: String): Resource<RestorePreviewData> {
        val result = repository.downloadToTempFile(path)
        return if (result is Resource.Success) {
            val preview = parsePreviewFromFile(result.data!!, "CLOUD")
            result.data.delete() // Clean up after parsing preview
            preview
        } else {
            Resource.Error(result.message ?: "Failed to download cloud backup for preview")
        }
    }

    private suspend fun getPreviewFromLatestCloud(): Resource<RestorePreviewData> {
        val listResult = repository.listCloudBackups()
        if (listResult is Resource.Success) {
            val latest = listResult.data?.sortedDescending()?.firstOrNull()
            return if (latest != null) {
                getPreviewFromCloudPath(latest)
            } else {
                Resource.Error("No cloud backups found")
            }
        }
        return Resource.Error(listResult.message ?: "Failed to list cloud backups")
    }

    private fun parsePreviewFromFile(file: File, type: String): Resource<RestorePreviewData> {
        return try {
            val reader = com.google.gson.stream.JsonReader(java.io.FileReader(file))
            var farmerCount = 0
            var buyerCount = 0
            var arrivalCount = 0
            var saleCount = 0
            var paymentCount = 0
            var createdAt = file.lastModified()

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                when (name) {
                    "farmers" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.skipValue()
                            farmerCount++
                        }
                        reader.endArray()
                    }
                    "buyers" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.skipValue()
                            buyerCount++
                        }
                        reader.endArray()
                    }
                    "arrivals" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.skipValue()
                            arrivalCount++
                        }
                        reader.endArray()
                    }
                    "sales" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.skipValue()
                            saleCount++
                        }
                        reader.endArray()
                    }
                    "payments" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.skipValue()
                            paymentCount++
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            reader.close()
            
            Resource.Success(RestorePreviewData(
                fileName = file.name,
                type = type,
                createdAt = createdAt,
                size = file.length(),
                farmerCount = farmerCount,
                buyerCount = buyerCount,
                arrivalCount = arrivalCount,
                saleCount = saleCount,
                paymentCount = paymentCount
            ))
        } catch (e: Exception) {
            Timber.e(e, "Preview parse failed")
            Resource.Error("Invalid Mandi Ledger backup format")
        }
    }
}

data class RestorePreviewData(
    val fileName: String,
    val type: String,
    val createdAt: Long,
    val size: Long,
    val farmerCount: Int,
    val buyerCount: Int,
    val arrivalCount: Int,
    val saleCount: Int,
    val paymentCount: Int,
    val isOlderThanLocal: Boolean = false,
    val action: RestoreAction? = null
)
