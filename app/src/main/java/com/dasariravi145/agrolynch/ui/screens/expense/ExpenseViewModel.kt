package com.dasariravi145.agrolynch.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity
import com.dasariravi145.agrolynch.domain.repository.ExpenseRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    val expenses = repository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalToday = expenses.map { list ->
        val today = getStartOfDay(System.currentTimeMillis())
        list.filter { it.date >= today }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun addExpense(type: String, amount: Double, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                amount = amount,
                description = description
            )
            val result = repository.addExpense(expense)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to save expense")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateExpense(expense)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to update expense")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteExpense(id)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to delete expense")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
