package com.dasariravi145.agrolynch.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.dasariravi145.agrolynch.data.local.dao.SubscriptionDao
import com.dasariravi145.agrolynch.data.local.entity.SubscriptionEntity
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumStateManager: PremiumStateManager,
    private val subscriptionDao: SubscriptionDao,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PurchasesUpdatedListener {

    private companion object {
        const val TAG = "BillingManager"

        const val PREMIUM_PRODUCT_ID = "premium"

        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_THREE_MONTHS = "3-months"
        const val BASE_PLAN_SIX_MONTHS = "6-months"
        const val BASE_PLAN_YEARLY = "yearly"

        const val STATUS_ACTIVE = "ACTIVE"

        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        const val MONTHLY_DURATION_DAYS = 30
        const val THREE_MONTH_DURATION_DAYS = 90
        const val SIX_MONTH_DURATION_DAYS = 180
        const val YEARLY_DURATION_DAYS = 365

        val SUPPORTED_BASE_PLAN_IDS = setOf(
            BASE_PLAN_MONTHLY,
            BASE_PLAN_THREE_MONTHS,
            BASE_PLAN_SIX_MONTHS,
            BASE_PLAN_YEARLY
        )
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    private val _productDetailsList =
        MutableStateFlow<List<ProductDetails>>(emptyList())

    val productDetailsList = _productDetailsList.asStateFlow()

    private val _subscriptionOffers =
        MutableStateFlow<List<ProductDetails.SubscriptionOfferDetails>>(emptyList())

    /**
     * The Premium screen should observe this list.
     *
     * One item is returned for each configured base plan:
     * monthly, 3-months, 6-months and yearly.
     */
    val subscriptionOffers = _subscriptionOffers.asStateFlow()

    private val _billingError = MutableSharedFlow<String>()

    val billingError = _billingError.asSharedFlow()

    private val _purchaseSuccess = MutableSharedFlow<Unit>()

    val purchaseSuccess = _purchaseSuccess.asSharedFlow()

    private var reconnectCount = 0

    private val maxReconnectCount = 5

    /**
     * Google Play Purchase.products contains "premium", not the base-plan ID.
     * Therefore, preserve the selected base plan before launching billing.
     */
    @Volatile
    private var selectedBasePlanId: String? = null

    @Volatile
    private var billingConnectionInProgress = false

    init {
        startBillingConnection()
    }

    fun startBillingConnection() {
        if (billingClient.isReady) {
            queryPremiumProducts()
            refreshSubscriptionStatus()
            return
        }

        if (billingConnectionInProgress) {
            Log.d(TAG, "Billing connection is already in progress")
            return
        }

        billingConnectionInProgress = true

        Log.d(TAG, "Connecting to Google Play Billing")

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {
                    billingConnectionInProgress = false

                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        Log.i(TAG, "BILLING_CONNECTED")

                        reconnectCount = 0

                        queryPremiumProducts()
                        refreshSubscriptionStatus()
                    } else {
                        Log.e(
                            TAG,
                            "Billing setup failed: " +
                                    "${billingResult.debugMessage}"
                        )

                        handleBillingError(billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    billingConnectionInProgress = false

                    Log.w(TAG, "Billing service disconnected")

                    if (reconnectCount < maxReconnectCount) {
                        reconnectCount++

                        Log.d(
                            TAG,
                            "Reconnecting to Billing: " +
                                    "$reconnectCount/$maxReconnectCount"
                        )

                        startBillingConnection()
                    } else {
                        emitBillingError(
                            "Unable to connect to Google Play Billing. " +
                                    "Please check your internet connection and try again."
                        )
                    }
                }
            }
        )
    }

    private fun queryPremiumProducts() {
        if (!billingClient.isReady) {
            Log.w(
                TAG,
                "Billing client is not ready for product query"
            )

            startBillingConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val queryParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

        billingClient.queryProductDetailsAsync(
            queryParams
        ) { billingResult, returnedProducts ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                Log.e(
                    TAG,
                    "Failed to query premium product: " +
                            billingResult.debugMessage
                )

                _productDetailsList.value = emptyList()
                _subscriptionOffers.value = emptyList()

                emitBillingError(
                    billingResult.debugMessage.ifBlank {
                        "Unable to load premium subscription plans."
                    }
                )

                return@queryProductDetailsAsync
            }

            val premiumProduct =
                returnedProducts.firstOrNull { productDetails ->
                    productDetails.productId == PREMIUM_PRODUCT_ID
                }

            if (premiumProduct == null) {
                Log.e(
                    TAG,
                    "PRODUCT_NOT_FOUND: $PREMIUM_PRODUCT_ID"
                )

                _productDetailsList.value = emptyList()
                _subscriptionOffers.value = emptyList()

                emitBillingError(
                    "Premium plans were not returned by Google Play. " +
                            "Install the app from the closed-testing Play Store link."
                )

                return@queryProductDetailsAsync
            }

            val availableOffers =
                premiumProduct.subscriptionOfferDetails
                    .orEmpty()
                    .filter { offerDetails ->
                        offerDetails.basePlanId in
                                SUPPORTED_BASE_PLAN_IDS
                    }
                    .distinctBy { offerDetails ->
                        offerDetails.basePlanId
                    }
                    .sortedBy { offerDetails ->
                        basePlanDisplayOrder(
                            offerDetails.basePlanId
                        )
                    }

            if (availableOffers.isEmpty()) {
                Log.e(
                    TAG,
                    "No supported base plans returned for " +
                            PREMIUM_PRODUCT_ID
                )

                _productDetailsList.value = emptyList()
                _subscriptionOffers.value = emptyList()

                emitBillingError(
                    "No active premium base plans are available."
                )

                return@queryProductDetailsAsync
            }

            _productDetailsList.value =
                listOf(premiumProduct)

            _subscriptionOffers.value =
                availableOffers

            Log.i(
                TAG,
                "PRODUCT_LOADED: product=$PREMIUM_PRODUCT_ID, " +
                        "basePlans=${
                            availableOffers.joinToString { offer ->
                                offer.basePlanId
                            }
                        }"
            )
        }
    }

    /**
     * Launches Google Play Billing for the exact base plan selected by the user.
     *
     * @param productDetails The ProductDetails for product ID "premium".
     * @param basePlanId One of monthly, 3-months, 6-months or yearly.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String
    ) {
        Log.i(
            TAG,
            "SUBSCRIBE_CLICKED: " +
                    "product=${productDetails.productId}, " +
                    "basePlan=$basePlanId"
        )

        if (!billingClient.isReady) {
            Log.e(
                TAG,
                "BillingClient is not ready"
            )

            startBillingConnection()

            emitBillingError(
                "Google Play Billing is not ready. " +
                        "Please try again in a moment."
            )

            return
        }

        if (productDetails.productId != PREMIUM_PRODUCT_ID) {
            Log.e(
                TAG,
                "Unexpected product ID: " +
                        productDetails.productId
            )

            emitBillingError(
                "Invalid premium subscription product."
            )

            return
        }

        if (basePlanId !in SUPPORTED_BASE_PLAN_IDS) {
            Log.e(
                TAG,
                "Unsupported base-plan ID: $basePlanId"
            )

            emitBillingError(
                "The selected premium plan is not supported."
            )

            return
        }

        val selectedOffer =
            productDetails.subscriptionOfferDetails
                .orEmpty()
                .firstOrNull { offerDetails ->
                    offerDetails.basePlanId == basePlanId
                }

        if (selectedOffer == null) {
            Log.e(
                TAG,
                "Offer not found for base plan: $basePlanId"
            )

            emitBillingError(
                "The selected premium plan is currently unavailable."
            )

            return
        }

        selectedBasePlanId = basePlanId

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(selectedOffer.offerToken)
                .build()

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(productDetailsParams)
                )
                .build()

        val launchResult =
            billingClient.launchBillingFlow(
                activity,
                billingFlowParams
            )

        if (
            launchResult.responseCode !=
            BillingClient.BillingResponseCode.OK
        ) {
            selectedBasePlanId = null

            Log.e(
                TAG,
                "Failed to launch billing flow: " +
                        launchResult.debugMessage
            )

            handleBillingError(launchResult)

            return
        }

        Log.i(
            TAG,
            "BILLING_FLOW_STARTED: " +
                    "product=${productDetails.productId}, " +
                    "basePlan=$basePlanId"
        )
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    Log.w(
                        TAG,
                        "Purchase result was OK but no purchase was returned"
                    )

                    emitBillingError(
                        "Google Play did not return purchase information."
                    )

                    return
                }

                purchases.forEach { purchase ->
                    handlePurchase(
                        purchase = purchase,
                        fromNewPurchaseFlow = true
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                selectedBasePlanId = null

                Log.i(
                    TAG,
                    "Purchase cancelled by user"
                )
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                selectedBasePlanId = null

                Log.i(
                    TAG,
                    "Subscription is already owned"
                )

                premiumStateManager.updatePremiumStatus(true)

                refreshSubscriptionStatus()

                emitBillingError(
                    "This Google Play account already has Premium."
                )
            }

            else -> {
                selectedBasePlanId = null

                Log.e(
                    TAG,
                    "PURCHASE_FAILED: " +
                            "${billingResult.debugMessage}; " +
                            "code=${billingResult.responseCode}"
                )

                handleBillingError(billingResult)
            }
        }
    }

    private fun handlePurchase(
        purchase: Purchase,
        fromNewPurchaseFlow: Boolean
    ) {
        if (PREMIUM_PRODUCT_ID !in purchase.products) {
            Log.w(
                TAG,
                "Ignoring unrelated purchase: ${purchase.products}"
            )

            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (purchase.isAcknowledged) {
                    processPurchasedSubscription(
                        purchase = purchase,
                        fromNewPurchaseFlow =
                            fromNewPurchaseFlow
                    )
                } else {
                    acknowledgePurchase(
                        purchase = purchase,
                        fromNewPurchaseFlow =
                            fromNewPurchaseFlow
                    )
                }
            }

            Purchase.PurchaseState.PENDING -> {
                Log.i(
                    TAG,
                    "Subscription purchase is pending"
                )

                emitBillingError(
                    "Your purchase is pending. Premium will activate " +
                            "after Google Play confirms the payment."
                )
            }

            else -> {
                Log.w(
                    TAG,
                    "Purchase is not active. State=" +
                            purchase.purchaseState
                )
            }
        }
    }

    private fun acknowledgePurchase(
        purchase: Purchase,
        fromNewPurchaseFlow: Boolean
    ) {
        val acknowledgeParams =
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

        billingClient.acknowledgePurchase(
            acknowledgeParams
        ) { billingResult ->

            if (
                billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {
                Log.i(
                    TAG,
                    "Purchase acknowledged successfully"
                )

                processPurchasedSubscription(
                    purchase = purchase,
                    fromNewPurchaseFlow =
                        fromNewPurchaseFlow
                )
            } else {
                Log.e(
                    TAG,
                    "Purchase acknowledgement failed: " +
                            billingResult.debugMessage
                )

                handleBillingError(billingResult)
            }
        }
    }

    private fun processPurchasedSubscription(
        purchase: Purchase,
        fromNewPurchaseFlow: Boolean
    ) {
        val basePlanId =
            if (fromNewPurchaseFlow) {
                selectedBasePlanId
            } else {
                null
            }

        if (fromNewPurchaseFlow && basePlanId == null) {
            Log.e(
                TAG,
                "Selected base-plan ID is unavailable for new purchase"
            )

            emitBillingError(
                "The purchased plan could not be identified. " +
                        "Please reopen the Premium screen."
            )

            premiumStateManager.updatePremiumStatus(true)

            return
        }

        if (fromNewPurchaseFlow) {
            saveNewSubscriptionDetails(
                purchase = purchase,
                basePlanId = requireNotNull(basePlanId)
            )
        } else {
            restoreExistingSubscription(purchase)
        }
    }

    private fun saveNewSubscriptionDetails(
        purchase: Purchase,
        basePlanId: String
    ) {
        scope.launch {
            val uid = auth.currentUser?.uid

            if (uid.isNullOrBlank()) {
                Log.e(
                    TAG,
                    "Cannot save subscription: user is not authenticated"
                )

                _billingError.emit(
                    "Your login session has expired. Please log in again."
                )

                return@launch
            }

            val productId =
                purchase.products.firstOrNull()

            if (productId != PREMIUM_PRODUCT_ID) {
                Log.e(
                    TAG,
                    "Unexpected purchased product: $productId"
                )

                _billingError.emit(
                    "The purchased subscription could not be verified."
                )

                return@launch
            }

            val premiumProduct =
                _productDetailsList.value.firstOrNull {
                    it.productId == PREMIUM_PRODUCT_ID
                }

            val selectedOffer =
                premiumProduct
                    ?.subscriptionOfferDetails
                    .orEmpty()
                    .firstOrNull { offerDetails ->
                        offerDetails.basePlanId == basePlanId
                    }

            val formattedPrice =
                selectedOffer
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
                    ?: "Price unavailable"

            val planName =
                getPlanName(basePlanId)

            /*
             * This is an estimated local expiry for the existing local model.
             * Production entitlement should later be verified using a secure
             * backend and the Google Play Developer API.
             */
            val estimatedExpiryTime =
                calculateEstimatedExpiryTime(
                    purchaseTime = purchase.purchaseTime,
                    basePlanId = basePlanId
                )

            val userDisplayName =
                auth.currentUser?.email
                    ?: auth.currentUser?.phoneNumber
                    ?: "User"

            val subscription =
                SubscriptionEntity(
                    transactionId = purchase.purchaseToken,
                    userId = uid,
                    userName = userDisplayName,
                    planName = planName,
                    amount = formattedPrice,
                    status = STATUS_ACTIVE,
                    purchaseDate = purchase.purchaseTime,
                    expiryDate = estimatedExpiryTime,
                    orderId = purchase.orderId.orEmpty(),
                    productId = productId
                )

            try {
                subscriptionDao.insertSubscription(subscription)
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to save subscription locally",
                    exception
                )

                _billingError.emit(
                    "Premium was purchased, but local subscription " +
                            "details could not be saved."
                )
            }

            premiumStateManager.updatePremiumStatus(
                true,
                estimatedExpiryTime
            )

            updateLocalPremiumProfile(
                planName = planName,
                purchase = purchase,
                productId = productId,
                expiryTime = estimatedExpiryTime
            )

            updateFirestorePremiumProfile(
                uid = uid,
                planName = planName,
                basePlanId = basePlanId,
                purchase = purchase,
                productId = productId,
                expiryTime = estimatedExpiryTime
            )

            syncSubscriptionRecord(
                uid = uid,
                subscription = subscription,
                basePlanId = basePlanId
            )

            selectedBasePlanId = null

            Log.i(
                TAG,
                "PURCHASE_SUCCESS: product=$productId, " +
                        "basePlan=$basePlanId"
            )

            _purchaseSuccess.emit(Unit)
        }
    }

    private fun restoreExistingSubscription(
        purchase: Purchase
    ) {
        scope.launch {
            /*
             * Purchase.products only returns "premium".
             * It does not provide the base-plan ID.
             *
             * Therefore, do not overwrite the saved plan name or expiry
             * during a normal restore/refresh.
             */
            premiumStateManager.updatePremiumStatus(true)

            val uid = auth.currentUser?.uid

            if (!uid.isNullOrBlank()) {
                try {
                    val localUser =
                        userRepository.getUserProfile().first()

                    localUser?.let { user ->
                        userRepository.saveProfile(
                            user.copy(
                                isPremium = true,
                                purchaseToken =
                                    purchase.purchaseToken,
                                productId =
                                    PREMIUM_PRODUCT_ID,
                                lastUpdatedAt =
                                    System.currentTimeMillis()
                            )
                        )
                    }
                } catch (exception: Exception) {
                    Log.e(
                        TAG,
                        "Failed to restore local premium state",
                        exception
                    )
                }

                try {
                    firestore
                        .collection("users")
                        .document(uid)
                        .update(
                            mapOf(
                                "isPremium" to true,
                                "purchaseToken" to
                                        purchase.purchaseToken,
                                "productId" to
                                        PREMIUM_PRODUCT_ID,
                                "lastUpdatedAt" to
                                        System.currentTimeMillis()
                            )
                        )
                        .await()
                } catch (exception: Exception) {
                    Log.e(
                        TAG,
                        "Failed to restore Firestore premium state",
                        exception
                    )
                }
            }

            Log.i(
                TAG,
                "Existing Premium subscription restored"
            )
        }
    }

    private suspend fun updateLocalPremiumProfile(
        planName: String,
        purchase: Purchase,
        productId: String,
        expiryTime: Long
    ) {
        try {
            val localUser =
                userRepository.getUserProfile().first()

            localUser?.let { user ->
                userRepository.saveProfile(
                    user.copy(
                        isPremium = true,
                        premiumPlan = planName,
                        premiumStartDate =
                            purchase.purchaseTime,
                        premiumExpiryDate =
                            expiryTime,
                        purchaseToken =
                            purchase.purchaseToken,
                        productId = productId,
                        lastUpdatedAt =
                            System.currentTimeMillis()
                    )
                )
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to update local user premium profile",
                exception
            )
        }
    }

    private suspend fun updateFirestorePremiumProfile(
        uid: String,
        planName: String,
        basePlanId: String,
        purchase: Purchase,
        productId: String,
        expiryTime: Long
    ) {
        try {
            val userReference =
                firestore.collection("users")
                    .document(uid)

            firestore.runTransaction { transaction ->
                transaction.update(
                    userReference,
                    mapOf(
                        "isPremium" to true,
                        "premiumPlan" to planName,
                        "premiumBasePlanId" to
                                basePlanId,
                        "premiumStartDate" to
                                purchase.purchaseTime,
                        "premiumExpiryDate" to
                                expiryTime,
                        "premiumExpiry" to
                                expiryTime,
                        "purchaseToken" to
                                purchase.purchaseToken,
                        "productId" to productId,
                        "lastUpdatedAt" to
                                System.currentTimeMillis()
                    )
                )
            }.await()
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to update Firestore premium profile",
                exception
            )
        }
    }

    private suspend fun syncSubscriptionRecord(
        uid: String,
        subscription: SubscriptionEntity,
        basePlanId: String
    ) {
        try {
            val subscriptionData =
                mapOf(
                    "transactionId" to
                            subscription.transactionId,
                    "userId" to subscription.userId,
                    "userName" to subscription.userName,
                    "planName" to subscription.planName,
                    "basePlanId" to basePlanId,
                    "amount" to subscription.amount,
                    "status" to subscription.status,
                    "purchaseDate" to
                            subscription.purchaseDate,
                    "expiryDate" to
                            subscription.expiryDate,
                    "orderId" to subscription.orderId,
                    "productId" to
                            subscription.productId
                )

            firestore
                .collection("users")
                .document(uid)
                .collection("subscriptions")
                .document(subscription.transactionId)
                .set(subscriptionData)
                .await()
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to sync subscription record",
                exception
            )
        }
    }

    fun refreshSubscriptionStatus() {
        if (!billingClient.isReady) {
            Log.w(
                TAG,
                "Billing client not ready during subscription refresh"
            )

            startBillingConnection()
            return
        }

        val queryParams =
            QueryPurchasesParams.newBuilder()
                .setProductType(
                    BillingClient.ProductType.SUBS
                )
                .build()

        billingClient.queryPurchasesAsync(
            queryParams
        ) { billingResult, purchases ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                Log.e(
                    TAG,
                    "Failed to refresh subscriptions: " +
                            billingResult.debugMessage
                )

                handleBillingError(billingResult)

                return@queryPurchasesAsync
            }

            val activePurchase =
                purchases.firstOrNull { purchase ->
                    PREMIUM_PRODUCT_ID in
                            purchase.products &&
                            purchase.purchaseState ==
                            Purchase.PurchaseState.PURCHASED
                }

            if (activePurchase != null) {
                Log.i(
                    TAG,
                    "Active Premium purchase found"
                )

                handlePurchase(
                    purchase = activePurchase,
                    fromNewPurchaseFlow = false
                )
            } else {
                Log.i(
                    TAG,
                    "No active Premium subscription returned by Google Play"
                )

                selectedBasePlanId = null

                premiumStateManager.updatePremiumStatus(false)

                scope.launch {
                    updateExpiredPremiumStatus()
                }
            }
        }
    }

    private suspend fun updateExpiredPremiumStatus() {
        val uid = auth.currentUser?.uid

        try {
            val localUser =
                userRepository.getUserProfile().first()

            localUser?.let { user ->
                userRepository.saveProfile(
                    user.copy(
                        isPremium = false,
                        lastUpdatedAt =
                            System.currentTimeMillis()
                    )
                )
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to disable local premium status",
                exception
            )
        }

        if (!uid.isNullOrBlank()) {
            try {
                firestore
                    .collection("users")
                    .document(uid)
                    .update(
                        mapOf(
                            "isPremium" to false,
                            "lastUpdatedAt" to
                                    System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to disable Firestore premium status",
                    exception
                )
            }
        }
    }

    private fun getPlanName(
        basePlanId: String
    ): String {
        return when (basePlanId) {
            BASE_PLAN_MONTHLY ->
                "Monthly Premium"

            BASE_PLAN_THREE_MONTHS ->
                "3 Months Premium"

            BASE_PLAN_SIX_MONTHS ->
                "6 Months Premium"

            BASE_PLAN_YEARLY ->
                "Yearly Premium"

            else ->
                "Premium"
        }
    }

    private fun calculateEstimatedExpiryTime(
        purchaseTime: Long,
        basePlanId: String
    ): Long {
        val durationDays =
            when (basePlanId) {
                BASE_PLAN_MONTHLY ->
                    MONTHLY_DURATION_DAYS

                BASE_PLAN_THREE_MONTHS ->
                    THREE_MONTH_DURATION_DAYS

                BASE_PLAN_SIX_MONTHS ->
                    SIX_MONTH_DURATION_DAYS

                BASE_PLAN_YEARLY ->
                    YEARLY_DURATION_DAYS

                else ->
                    MONTHLY_DURATION_DAYS
            }

        return purchaseTime +
                durationDays.toLong() *
                MILLIS_PER_DAY
    }

    private fun basePlanDisplayOrder(
        basePlanId: String
    ): Int {
        return when (basePlanId) {
            BASE_PLAN_MONTHLY -> 1
            BASE_PLAN_THREE_MONTHS -> 2
            BASE_PLAN_SIX_MONTHS -> 3
            BASE_PLAN_YEARLY -> 4
            else -> Int.MAX_VALUE
        }
    }

    private fun handleBillingError(
        billingResult: BillingResult
    ) {
        val message =
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode
                    .SERVICE_UNAVAILABLE -> {
                    "Google Play Billing service is unavailable."
                }

                BillingClient.BillingResponseCode
                    .BILLING_UNAVAILABLE -> {
                    "Google Play Billing is unavailable on this device or account."
                }

                BillingClient.BillingResponseCode
                    .DEVELOPER_ERROR -> {
                    "Google Play Billing configuration error. " +
                            "Verify product ID, base plans and app version."
                }

                BillingClient.BillingResponseCode
                    .ITEM_UNAVAILABLE -> {
                    "The selected subscription plan is unavailable."
                }

                BillingClient.BillingResponseCode
                    .ITEM_ALREADY_OWNED -> {
                    premiumStateManager.updatePremiumStatus(true)

                    "This Google Play account already owns Premium."
                }

                BillingClient.BillingResponseCode
                    .NETWORK_ERROR -> {
                    "Network error while contacting Google Play."
                }

                BillingClient.BillingResponseCode
                    .SERVICE_DISCONNECTED -> {
                    "Google Play Billing was disconnected."
                }

                else -> {
                    billingResult.debugMessage.ifBlank {
                        "Google Play Billing error."
                    }
                }
            }

        emitBillingError(message)
    }

    private fun emitBillingError(
        message: String
    ) {
        scope.launch {
            _billingError.emit(message)
        }
    }

    /**
     * Call only if BillingManager is no longer application-scoped.
     * The current @Singleton instance normally remains alive with the app.
     */
    fun release() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }

        scope.cancel()
    }
}