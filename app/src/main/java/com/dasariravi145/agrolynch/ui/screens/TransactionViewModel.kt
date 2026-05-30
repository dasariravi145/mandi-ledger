package com.dasariravi145.agrolynch.ui.screens

import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity
import com.dasariravi145.agrolynch.domain.repository.TransactionRepository
import com.dasariravi145.agrolynch.util.BaseViewModel
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class TransactionListState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val productRepository: com.dasariravi145.agrolynch.domain.repository.ProductRepository,
    private val farmerRepository: com.dasariravi145.agrolynch.domain.repository.FarmerRepository
) : BaseViewModel<TransactionListState>(TransactionListState()) {

    val products = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmers = farmerRepository.getFarmers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        repository.getTransactions()
            .onStart { updateState { it.copy(isLoading = true) } }
            .onEach { list ->
                Timber.d("TransactionViewModel: Loaded ${list.size} transactions")
                updateState { it.copy(transactions = list, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "TransactionViewModel: Error loading transactions")
                handleError(e)
                updateState { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun addTransaction(transaction: TransactionEntity, category: String, grades: List<String>) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            
            // 1. Ensure product exists or update it
            val product = com.dasariravi145.agrolynch.data.local.entity.ProductEntity(
                id = transaction.productId.ifEmpty { UUID.randomUUID().toString() },
                name = transaction.productName,
                category = category,
                availableGrades = grades
            )
            productRepository.addProduct(product, null)

            // 2. Add the transaction
            val result = repository.addTransaction(transaction.copy(productId = product.id))
            if (result is Resource.Success) {
                Timber.i("TransactionViewModel: Transaction added successfully")
                syncTransactions()
                _saveSuccess.emit(Unit)
            } else if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.updateTransaction(transaction)
            if (result is Resource.Success) {
                _saveSuccess.emit(Unit)
            } else if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun deleteTransaction(id: String) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.deleteTransaction(id)
            if (result is Resource.Success) {
                _saveSuccess.emit(Unit)
            } else if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun syncTransactions() {
        launchSafe {
            Timber.d("TransactionViewModel: Syncing transactions...")
            repository.syncTransactions()
        }
    }
}
