package com.dasariravi145.agrolynch.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.AccountBookArchiveEntity
import com.dasariravi145.agrolynch.domain.model.BackupData
import com.dasariravi145.agrolynch.domain.repository.ArchiveRepository
import com.dasariravi145.agrolynch.util.Resource
import com.dasariravi145.agrolynch.util.PdfActionManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ArchiveListViewModel @Inject constructor(
    private val repository: ArchiveRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    private val _filterType = MutableStateFlow("ALL") // ALL, FARMER, BUYER
    val filterType = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedArchive = MutableStateFlow<AccountBookArchiveEntity?>(null)
    val selectedArchive = _selectedArchive.asStateFlow()

    private val _selectedSnapshot = MutableStateFlow<BackupData?>(null)
    val selectedSnapshot = _selectedSnapshot.asStateFlow()

    val archives: StateFlow<List<AccountBookArchiveEntity>> = combine(
        repository.getArchives(),
        _filterType,
        _searchQuery
    ) { list, type, query ->
        list.filter { 
            (type == "ALL" || it.partyType == type) && 
            (query.isBlank() || it.partyName.contains(query, ignoreCase = true) || it.partyPhone.contains(query) || it.archiveId.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadArchiveDetails(archiveId: String, gson: Gson) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val archive = repository.getArchiveById(archiveId)
                if (archive != null) {
                    _selectedArchive.value = archive
                    _selectedSnapshot.value = gson.fromJson(archive.snapshotJson, BackupData::class.java)
                } else {
                    _message.emit("Archive details not found.")
                }
            } catch (e: Exception) {
                _message.emit("Unable to load archive details: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedArchive() {
        _selectedArchive.value = null
        _selectedSnapshot.value = null
    }

    fun setFilter(type: String) {
        _filterType.value = type
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun restoreArchive(archiveId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.restoreArchive(archiveId)) {
                is Resource.Success -> _message.emit("Archived history restored successfully.")
                is Resource.Error -> _message.emit("Restore failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun permanentDelete(archiveId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.permanentDeleteArchive(archiveId)) {
                is Resource.Success -> _message.emit("Archive permanently removed.")
                is Resource.Error -> _message.emit("Delete failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun exportPdf(context: android.content.Context, archiveId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.exportArchivePdf(context, archiveId)) {
                is Resource.Success -> PdfActionManager.openPdf(context, com.dasariravi145.agrolynch.util.PdfGenerator.getUriFromFile(context, result.data!!))
                is Resource.Error -> _message.emit("Export failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun exportExcel(context: android.content.Context, archiveId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.exportArchiveExcel(context, archiveId)) {
                is Resource.Success -> _message.emit("Excel exported to ${result.data?.absolutePath}")
                is Resource.Error -> _message.emit("Export failed: ${result.message}")
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
