package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.InvoiceTemplatePositionEntity
import com.dasariravi145.agrolynch.domain.repository.TemplatePositionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val repository: TemplatePositionRepository
) : ViewModel() {

    private val _templateType = MutableStateFlow("CUSTOM_TEMPLATE")
    val templateType = _templateType.asStateFlow()

    val positions: StateFlow<List<InvoiceTemplatePositionEntity>> = _templateType
        .flatMapLatest { type -> repository.getPositionsByTemplate(type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTemplateType(type: String) {
        _templateType.value = type
    }

    fun updatePosition(updated: InvoiceTemplatePositionEntity) {
        viewModelScope.launch {
            val current = positions.value.toMutableList()
            val index = current.indexOfFirst { it.fieldKey == updated.fieldKey }
            if (index != -1) {
                current[index] = updated
            } else {
                current.add(updated)
            }
            repository.savePositions(_templateType.value, current)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            repository.resetToDefault(_templateType.value)
        }
    }
}
