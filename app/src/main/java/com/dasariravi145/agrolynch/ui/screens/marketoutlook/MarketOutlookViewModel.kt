package com.dasariravi145.agrolynch.ui.screens.marketoutlook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductTypeEntity
import com.dasariravi145.agrolynch.domain.engine.MarketForecastEngine
import com.dasariravi145.agrolynch.domain.model.MarketForecastResult
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.domain.repository.MarketDataRepository
import com.dasariravi145.agrolynch.domain.repository.ProductRepository
import com.dasariravi145.agrolynch.util.MarketOutlookAiService
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MarketOutlookViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val productRepository: ProductRepository,
    private val companyRepository: CompanyRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedProductName = MutableStateFlow("Mango")
    val selectedProductName: StateFlow<String> = _selectedProductName.asStateFlow()

    private val _selectedVarietyName = MutableStateFlow("Neelam")
    val selectedVarietyName: StateFlow<String> = _selectedVarietyName.asStateFlow()

    private val _selectedGradeName = MutableStateFlow("Grade A")
    val selectedGradeName: StateFlow<String> = _selectedGradeName.asStateFlow()

    private val _selectedMarketName = MutableStateFlow("Chittoor Mandi")
    val selectedMarketName: StateFlow<String> = _selectedMarketName.asStateFlow()

    private val _forecastResult = MutableStateFlow<MarketForecastResult?>(null)
    val forecastResult: StateFlow<MarketForecastResult?> = _forecastResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val availableMarkets = listOf(
        "Chittoor Mandi",
        "Madanapalle Market",
        "Tirupati Mandi",
        "Kolar Market",
        "Bengaluru APMC",
        "Anantapur Mandi"
    )

    val products = productRepository.getProducts()
        .map { list -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val productTypes = _selectedProductName.flatMapLatest { prodName ->
        val matched = products.value.find { it.name.equals(prodName, ignoreCase = true) }
        if (matched != null) {
            productRepository.getProductTypes(matched.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUserDefaultMarket()
    }

    private fun loadUserDefaultMarket() {
        viewModelScope.launch {
            try {
                val profile = companyRepository.getProfile().firstOrNull()
                if (profile != null && profile.marketName.isNotBlank()) {
                    _selectedMarketName.value = profile.marketName
                }
            } catch (e: Exception) {
                Timber.e(e, "LOAD_DEFAULT_MARKET_FAILED")
            }
        }
    }

    fun onProductSelected(name: String) {
        _selectedProductName.value = name
    }

    fun onVarietySelected(variety: String) {
        _selectedVarietyName.value = variety
    }

    fun onGradeSelected(grade: String) {
        _selectedGradeName.value = grade
    }

    fun onMarketSelected(market: String) {
        _selectedMarketName.value = market
    }

    fun analyzeMarket() {
        if (!isPremium.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val prod = _selectedProductName.value
            val variety = _selectedVarietyName.value
            val grade = _selectedGradeName.value
            val market = _selectedMarketName.value

            try {
                // Check cache first
                val cached = marketDataRepository.getCachedForecast(market, prod, variety, grade)
                if (cached != null) {
                    _forecastResult.value = cached
                    _isLoading.value = false
                    return@launch
                }

                // Fetch historical and nearby data
                val history = marketDataRepository.getHistoricalPrices(prod, variety, grade, market)
                val nearby = marketDataRepository.getNearbyMarketPrices(prod, variety, grade, market)

                // Run forecasting engine
                val baseForecast = MarketForecastEngine.generateForecast(
                    productName = prod,
                    varietyName = variety,
                    gradeName = grade,
                    marketName = market,
                    historicalPrices = history,
                    nearbyMarkets = nearby
                )

                // Generate AI prose explanation
                val aiProse = MarketOutlookAiService.generateExplanation(baseForecast)

                val finalForecast = baseForecast.copy(aiExplanation = aiProse)

                // Cache result
                marketDataRepository.saveCachedForecast(finalForecast)

                _forecastResult.value = finalForecast
            } catch (e: Exception) {
                Timber.e(e, "ANALYZE_MARKET_FAILED")
                _errorMessage.value = "Failed to generate market outlook: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
