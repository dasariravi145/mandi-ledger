package com.dasariravi145.agrolynch.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.dasariravi145.agrolynch.data.local.dao.SubscriptionDao
import com.dasariravi145.agrolynch.data.local.entity.SubscriptionEntity
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumStateManager: PremiumStateManager,
    private val subscriptionDao: SubscriptionDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _billingError = MutableSharedFlow<String>()
    val billingError = _billingError.asSharedFlow()

    private val _purchaseSuccess = MutableSharedFlow<Unit>()
    val purchaseSuccess = _purchaseSuccess.asSharedFlow()

    private var reconnectCount = 0
    private val maxReconnectCount = 5

    init {
        startBillingConnection()
    }

    fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectCount = 0
                    queryPremiumProduct()
                    refreshSubscriptionStatus()
                } else {
                    handleBillingError(billingResult)
                }
            }

            override fun onBillingServiceDisconnected() {
                if (reconnectCount < maxReconnectCount) {
                    reconnectCount++
                    startBillingConnection()
                }
            }
        })
    }

    private fun queryPremiumProduct() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList.find { it.productId == "premium_yearly" }
                Log.d("BillingManager", "Product details loaded: ${_productDetails.value}")
            } else {
                Log.e("BillingManager", "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val details = _productDetails.value
        if (details == null) {
            scope.launch { _billingError.emit("Product details not available. Please try again.") }
            queryPremiumProduct() // Try fetching again
            return
        }
        
        val offerToken = details.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User cancelled
        } else {
            handleBillingError(billingResult)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        saveSubscriptionDetails(purchase)
                    }
                }
            } else {
                saveSubscriptionDetails(purchase)
            }
        }
    }

    private fun saveSubscriptionDetails(purchase: Purchase) {
        scope.launch {
            val uid = auth.currentUser?.uid ?: "unknown"
            val uEmail = auth.currentUser?.email ?: auth.currentUser?.phoneNumber ?: "User"
            
            val subscription = SubscriptionEntity(
                transactionId = purchase.purchaseToken, // Using purchaseToken as unique ID
                userId = uid,
                userName = uEmail,
                planName = "Premium Yearly",
                amount = _productDetails.value?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice ?: "₹999",
                status = "ACTIVE",
                purchaseDate = purchase.purchaseTime,
                expiryDate = purchase.purchaseTime + (365L * 24 * 60 * 60 * 1000), // Approx 1 year
                orderId = purchase.orderId ?: ""
            )
            
            subscriptionDao.insertSubscription(subscription)
            premiumStateManager.updatePremiumStatus(true)
            
            // Sync to Firestore
            try {
                firestore.collection("users").document(uid)
                    .collection("subscriptions").document(subscription.transactionId)
                    .set(subscription)
            } catch (e: Exception) {
                Log.e("BillingManager", "Failed to sync subscription: ${e.message}")
            }
            
            _purchaseSuccess.emit(Unit)
        }
    }

    fun refreshSubscriptionStatus() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { purchase ->
                    purchase.products.contains("premium_yearly") && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                premiumStateManager.updatePremiumStatus(hasPremium)
                
                // If they have premium, ensure it's in local DB
                if (hasPremium) {
                    val purchase = purchases.find { it.products.contains("premium_yearly") }
                    purchase?.let { handlePurchase(it) }
                }
            }
        }
    }

    private fun handleBillingError(billingResult: BillingResult) {
        val message = when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Billing service unavailable"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing unavailable"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer Error: Check Product ID"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                premiumStateManager.updatePremiumStatus(true)
                "Item already owned"
            }
            else -> "Billing error: ${billingResult.debugMessage}"
        }
        scope.launch { _billingError.emit(message) }
    }
}
