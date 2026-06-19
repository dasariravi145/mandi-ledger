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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumStateManager: PremiumStateManager,
    private val subscriptionDao: SubscriptionDao,
    private val userRepository: com.dasariravi145.agrolynch.domain.repository.UserRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList = _productDetailsList.asStateFlow()

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
        Log.d("BillingManager", "Connecting to Google Play...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i("BillingManager", "BILLING_CONNECTED")
                    reconnectCount = 0
                    queryPremiumProducts()
                    refreshSubscriptionStatus()
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                    handleBillingError(billingResult)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected")
                if (reconnectCount < maxReconnectCount) {
                    reconnectCount++
                    startBillingConnection()
                }
            }
        })
    }

    private fun queryPremiumProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_1_month")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_3_months")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_6_months")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_1_year")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isEmpty()) {
                    Log.e("BillingManager", "PRODUCT_NOT_FOUND: No products returned from Play Store")
                } else {
                    Log.i("BillingManager", "PRODUCTS_LOADED: ${productDetailsList.size} plans found")
                }
                _productDetailsList.value = productDetailsList
            } else {
                Log.e("BillingManager", "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        Log.i("BillingManager", "SUBSCRIBE_CLICKED: ${productDetails.productId}")
        
        if (!billingClient.isReady) {
            Log.e("BillingManager", "BillingClient not ready. Reconnecting...")
            startBillingConnection()
            scope.launch { _billingError.emit("Google Play Billing not ready. Please try again in a moment.") }
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
        if (offerToken.isEmpty() && productDetails.productType == BillingClient.ProductType.SUBS) {
            Log.e("BillingManager", "PRODUCT_NOT_FOUND: No offer token found for subscription ${productDetails.productId}")
            scope.launch { _billingError.emit("Premium plan not available. Please check Play Console product setup.") }
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        Log.i("BillingManager", "BILLING_FLOW_STARTED: ${productDetails.productId}")
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w("BillingManager", "PURCHASE_FAILED: User cancelled")
        } else {
            Log.e("BillingManager", "PURCHASE_FAILED: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
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
            
            val productId = purchase.products.firstOrNull() ?: ""
            val plan = com.dasariravi145.agrolynch.domain.model.PREMIUM_PLANS.find { it.productId == productId }
            
            val details = _productDetailsList.value.find { it.productId == productId }
            val formattedPrice = details?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice 
                ?: plan?.formattedPrice ?: "₹0"
            
            val durationDays = plan?.durationDays ?: 30
            val expiryTime = purchase.purchaseTime + (durationDays.toLong() * 24 * 60 * 60 * 1000)

            val subscription = SubscriptionEntity(
                transactionId = purchase.purchaseToken,
                userId = uid,
                userName = uEmail,
                planName = plan?.name ?: "Premium Plan",
                amount = formattedPrice,
                status = "ACTIVE",
                purchaseDate = purchase.purchaseTime,
                expiryDate = expiryTime,
                orderId = purchase.orderId ?: "",
                productId = productId
            )
            
            subscriptionDao.insertSubscription(subscription)
            premiumStateManager.updatePremiumStatus(true, expiryTime)
            
            // Update local user profile
            try {
                val localUser = userRepository.getUserProfile().first()
                localUser?.let {
                    userRepository.saveProfile(it.copy(
                        isPremium = true,
                        premiumPlan = plan?.name ?: "Premium",
                        premiumStartDate = purchase.purchaseTime,
                        premiumExpiryDate = expiryTime,
                        purchaseToken = purchase.purchaseToken,
                        productId = productId,
                        lastUpdatedAt = System.currentTimeMillis()
                    ))
                }
            } catch (e: Exception) {
                Log.e("BillingManager", "Failed to update local user profile: ${e.message}")
            }

            // Update User Profile with extensive details in Firestore
            try {
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(uid)
                    transaction.update(userRef, mapOf(
                        "isPremium" to true,
                        "premiumPlan" to (plan?.name ?: "Premium"),
                        "premiumStartDate" to purchase.purchaseTime,
                        "premiumExpiryDate" to expiryTime,
                        "premiumExpiry" to expiryTime, // compatibility
                        "purchaseToken" to purchase.purchaseToken,
                        "productId" to productId,
                        "lastUpdatedAt" to System.currentTimeMillis()
                    ))
                }.await()
            } catch (e: Exception) {
                Log.e("BillingManager", "Failed to sync user premium status: ${e.message}")
            }

            // Sync to Firestore
            try {
                firestore.collection("users").document(uid)
                    .collection("subscriptions").document(subscription.transactionId)
                    .set(subscription)
            } catch (e: Exception) {
                Log.e("BillingManager", "Failed to sync subscription: ${e.message}")
            }
            
            Log.i("BillingManager", "PURCHASE_SUCCESS: $productId")
            _purchaseSuccess.emit(Unit)
        }
    }

    fun refreshSubscriptionStatus() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val validProductIds = listOf("premium_1_month", "premium_3_months", "premium_6_months", "premium_1_year")
                val activePurchases = purchases.filter { purchase ->
                    purchase.products.any { it in validProductIds } && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                val hasPremium = activePurchases.isNotEmpty()
                
                if (hasPremium) {
                    val purchase = activePurchases.first()
                    // Re-calculate expiry if needed or just trust the latest purchase
                    // For simplicity, we just mark as premium. handlePurchase will update details if not already done.
                    premiumStateManager.updatePremiumStatus(true)
                    handlePurchase(purchase)
                } else {
                    // Check if locally expired
                    val expiry = premiumStateManager.getExpiryTime()
                    if (expiry > 0 && System.currentTimeMillis() > expiry) {
                        premiumStateManager.updatePremiumStatus(false)
                    }
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
