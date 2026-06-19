package com.dasariravi145.agrolynch.ui.screens.premium

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.data.local.entity.SubscriptionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val subscriptionHistory by viewModel.subscriptionHistory.collectAsState()
    val activity = LocalActivity.current as Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFFFD700)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isPremium) stringResource(R.string.premium_member) else stringResource(R.string.upgrade_sub),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPremium) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PremiumFeatureItem(stringResource(R.string.ad_free_experience))
            PremiumFeatureItem(stringResource(R.string.unlimited_ocr_scan))
            PremiumFeatureItem(stringResource(R.string.cloud_backup_restore))
            PremiumFeatureItem(stringResource(R.string.export_reports))
            PremiumFeatureItem("One-Tap Farmer Calling")
            PremiumFeatureItem("One-Tap Buyer Calling")
            PremiumFeatureItem("One-Tap WhatsApp Chat")
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isPremium) {
                val billingProductDetails = viewModel.productDetailsList.collectAsState().value
                
                if (billingProductDetails.isEmpty() && uiState !is PremiumUiState.Loading) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Premium plan not available. Please check Play Console product setup.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                com.dasariravi145.agrolynch.domain.model.PREMIUM_PLANS.forEach { plan ->
                    val details = billingProductDetails.find { it.productId == plan.productId }
                    val formattedPrice = details?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice 
                        ?: plan.formattedPrice

                    PremiumPlanCard(
                        plan = plan,
                        formattedPrice = formattedPrice,
                        isLoading = uiState is PremiumUiState.Loading,
                        onSubscribe = { 
                            details?.let { viewModel.subscribe(activity, it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                TextButton(onClick = { viewModel.restorePurchases() }) {
                    Text("Restore Purchases")
                }
            }
            
            if (subscriptionHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.payment_details), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                subscriptionHistory.forEach { sub ->
                    SubscriptionItem(sub)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            if (uiState is PremiumUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as PremiumUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PremiumPlanCard(
    plan: com.dasariravi145.agrolynch.domain.model.PremiumPlan,
    formattedPrice: String,
    isLoading: Boolean,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = plan.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = plan.durationText, fontSize = 12.sp, color = Color.Gray)
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = plan.badge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = formattedPrice, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                if (plan.productId != "premium_1_month") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "(${plan.monthlyPrice})", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Subscribe Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubscriptionItem(sub: SubscriptionEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = sub.planName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = sub.amount, fontWeight = FontWeight.ExtraBold)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            DetailRow("Subscriber:", sub.userName)
            DetailRow("Status:", sub.status)
            DetailRow("Transaction ID:", sub.transactionId.take(15) + "...")
            DetailRow("Order ID:", sub.orderId)
            DetailRow("Date:", dateFormat.format(Date(sub.purchaseDate)))
            DetailRow("Received By:", sub.accountReceived)
            DetailRow("Expires On:", dateFormat.format(Date(sub.expiryDate)))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 15.sp)
    }
}
