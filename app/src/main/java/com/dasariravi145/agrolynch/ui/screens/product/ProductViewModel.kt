package com.dasariravi145.agrolynch.ui.screens.product

import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.domain.repository.ProductRepository
import com.dasariravi145.agrolynch.util.BaseViewModel
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

data class ProductListState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : BaseViewModel<ProductListState>(ProductListState()) {

    init {
        observeProducts()
    }

    private fun observeProducts() {
        repository.getProducts()
            .onStart { updateState { it.copy(isLoading = true) } }
            .onEach { list ->
                Timber.d("ProductViewModel: Loaded ${list.size} products")
                updateState { it.copy(products = list, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "ProductViewModel: Error loading products")
                handleError(e)
                updateState { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        Timber.v("ProductViewModel: Search query changed to: $query")
        updateState { it.copy(searchQuery = query) }
    }

    val filteredProducts = uiState.map { state ->
        if (state.searchQuery.isBlank()) {
            state.products
        } else {
            state.products.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) || 
                it.category.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun addProduct(name: String, category: String, grades: List<String>, imageUri: android.net.Uri?) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val product = ProductEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                category = category,
                availableGrades = grades
            )
            val result = repository.addProduct(product, imageUri)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun updateProduct(product: ProductEntity, imageUri: android.net.Uri?) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.updateProduct(product, imageUri)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun deleteProduct(id: String) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.deleteProduct(id)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }
}
