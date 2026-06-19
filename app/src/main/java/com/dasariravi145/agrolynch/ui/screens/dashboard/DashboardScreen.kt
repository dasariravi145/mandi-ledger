package com.dasariravi145.agrolynch.ui.screens.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasariravi145.agrolynch.R
import com.dasariravi145.agrolynch.ads.BannerAdView
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumFeatureLockedDialog
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumUpgradePopup
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.util.Formatter
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewTransactions: () -> Unit,
    onViewFarmers: () -> Unit,
    onViewBuyers: () -> Unit,
    onViewProducts: () -> Unit,
    onViewMarketRates: () -> Unit,
    onViewSales: () -> Unit,
    onViewPayments: () -> Unit,
    onViewLedger: () -> Unit,
    onViewExpenses: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewReports: () -> Unit,
    onViewBillScan: () -> Unit,
    onViewSecurity: () -> Unit,
    onViewBackup: () -> Unit,
    onViewSettings: () -> Unit,
    onViewCompanyProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Timber.d("DashboardScreen: Initializing...")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsState(initial = "")
    val showPremiumPopup by viewModel.showPremiumPopup.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPremiumLockedDialog by remember { mutableStateOf(false) }

    if (showPremiumPopup) {
        PremiumUpgradePopup(
            onUpgradeClick = {
                viewModel.onPremiumUpgradeClick()
                onUpgradeClick()
            },
            onSkipClick = { doNotShowAgain -> viewModel.onPremiumPopupDismiss(doNotShowAgain) }
        )
    }

    LaunchedEffect(exportStatus) {
        if (exportStatus == "PREMIUM_REQUIRED") {
            showPremiumLockedDialog = true
        } else if (exportStatus.startsWith("SUCCESS:")) {
            val file = java.io.File(exportStatus.removePrefix("SUCCESS:"))
            PdfGenerator.sharePdf(context, file)
        } else if (exportStatus.startsWith("FAILED:")) {
            snackbarHostState.showSnackbar(exportStatus.removePrefix("FAILED:"))
        }
    }

    // Optimization: remember expensive string formatting
    val todaySalesStr = remember(state.summary.todaySales) { 
        "₹${Formatter.formatCurrency(state.summary.todaySales)}" 
    }
    val todayCommStr = remember(state.summary.todayCommission) { 
        "₹${Formatter.formatCurrency(state.summary.todayCommission)}" 
    }
    val totalCommStr = remember(state.summary.commissionEarned) { 
        "₹${Formatter.formatCurrency(state.summary.commissionEarned)}" 
    }
    val buyerPendingStr = remember(state.summary.buyerPending) { 
        Formatter.formatCurrency(state.summary.buyerPending) 
    }
    val farmerPendingStr = remember(state.summary.farmerPending) { 
        Formatter.formatCurrency(state.summary.farmerPending)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.mandi_ledger),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPremium) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFFD700),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    stringResource(R.string.premium_member).uppercase(),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isPremium) {
                        IconButton(onClick = onUpgradeClick) {
                            Icon(Icons.Default.Star, contentDescription = "Upgrade", tint = Color(0xFFFFD700))
                        }
                    }
                    IconButton(onClick = { viewModel.exportSummary(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Summary", tint = if(isPremium) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    IconButton(onClick = onViewSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewLedger,
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.account_book)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewPayments,
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    label = { Text(stringResource(R.string.payments)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showMoreMenu = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                    label = { Text(stringResource(R.string.more)) }
                )
            }
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        if (state.isLoading && state.summary.todaySales == 0.0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF16A34A))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: TOP SUMMARY CARDS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.today_sales),
                        amount = todaySalesStr,
                        containerColor = Color(0xFF1B5E20)
                    )
                    SummaryCard(
                        title = stringResource(R.string.today_commission),
                        amount = todayCommStr,
                        containerColor = Color(0xFF0D47A1)
                    )
                    SummaryCard(
                        title = stringResource(R.string.total_commission),
                        amount = totalCommStr,
                        containerColor = Color(0xFFE65100)
                    )
                }

                // SECTION 2: DUES CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.account_summary),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PendingMiniSummary(
                            buyerPending = buyerPendingStr,
                            farmerPending = farmerPendingStr,
                            onClick = onViewLedger
                        )
                    }
                }

                // SECTION 3: MAIN ACTION BUTTONS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardBoxItem(
                            text = stringResource(R.string.farmer_arrival),
                            icon = Icons.Default.AddBusiness,
                            onClick = onAddTransaction,
                            modifier = Modifier.weight(1f),
                            contentColor = Color(0xFF2E7D32)
                        )
                        DashboardBoxItem(
                            text = stringResource(R.string.buyer_sale),
                            icon = Icons.Default.ShoppingCart,
                            onClick = onViewSales,
                            modifier = Modifier.weight(1f),
                            contentColor = Color(0xFF1565C0)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardBoxItem(
                            text = stringResource(R.string.business_analytics),
                            icon = Icons.Default.Analytics,
                            onClick = onViewAnalytics,
                            modifier = Modifier.weight(1f),
                            contentColor = Color(0xFF7C3AED)
                        )
                        DashboardBoxItem(
                            text = stringResource(R.string.read_bill),
                            icon = Icons.Default.DocumentScanner,
                            onClick = {
                                if (isPremium) onViewBillScan() else showPremiumLockedDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            contentColor = Color(0xFFEF6C00)
                        )
                    }
                }

                // SECTION 4: QUICK ACCESS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickAccessItem(text = stringResource(R.string.farmers), icon = Icons.Default.Person, onClick = onViewFarmers)
                        QuickAccessItem(text = stringResource(R.string.traders), icon = Icons.Default.Store, onClick = onViewBuyers)
                        QuickAccessItem(text = stringResource(R.string.expenses), icon = Icons.Default.ReceiptLong, onClick = onViewExpenses)
                        QuickAccessItem(text = stringResource(R.string.products), icon = Icons.Default.Inventory, onClick = onViewProducts)
                    }
                }

                if (!isPremium) {
                    BannerAdView()
                }
            }
        }
    }

    if (showMoreMenu) {
        MoreFeaturesDialog(
            isPremium = isPremium,
            onDismiss = { showMoreMenu = false },
            onViewAnalytics = onViewAnalytics,
            onViewReports = onViewReports,
            onViewBackup = onViewBackup,
            onViewBillScan = onViewBillScan,
            onViewCompanyProfile = onViewCompanyProfile,
            onUpgradeClick = onUpgradeClick,
            onViewSettings = onViewSettings,
            showPremiumLockedDialog = { showPremiumLockedDialog = true }
        )
    }

    if (showPremiumLockedDialog) {
        PremiumFeatureLockedDialog(
            onDismiss = { showPremiumLockedDialog = false },
            onUpgradeClick = {
                showPremiumLockedDialog = false
                onUpgradeClick()
            }
        )
    }
}

@Composable
fun MoreFeaturesDialog(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onViewAnalytics: () -> Unit,
    onViewReports: () -> Unit,
    onViewBackup: () -> Unit,
    onViewBillScan: () -> Unit,
    onViewCompanyProfile: () -> Unit,
    onUpgradeClick: () -> Unit,
    onViewSettings: () -> Unit,
    showPremiumLockedDialog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.more)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoreMenuItem(
                    text = stringResource(R.string.business_analytics),
                    icon = Icons.Default.Analytics,
                    onClick = onViewAnalytics,
                    onDismiss = onDismiss
                )
                MoreMenuItem(
                    text = stringResource(R.string.business_reports),
                    icon = Icons.Default.Assessment,
                    onClick = onViewReports,
                    onDismiss = onDismiss
                )
                MoreMenuItem(
                    text = stringResource(R.string.backup_reports),
                    icon = Icons.Default.CloudUpload,
                    onClick = onViewBackup,
                    onDismiss = onDismiss
                )
                MoreMenuItem(
                    text = stringResource(R.string.read_bill), 
                    icon = Icons.Default.DocumentScanner, 
                    onClick = {
                        if (isPremium) onViewBillScan() else showPremiumLockedDialog()
                    }, 
                    onDismiss = onDismiss,
                    isPremiumFeature = true,
                    isPremium = isPremium
                )
                MoreMenuItem(
                    text = stringResource(R.string.company_profile_branding),
                    icon = Icons.Default.Business,
                    onClick = onViewCompanyProfile,
                    onDismiss = onDismiss
                )
                MoreMenuItem(
                    text = stringResource(R.string.subscription),
                    icon = Icons.Default.Star,
                    onClick = onUpgradeClick,
                    onDismiss = onDismiss
                )
                MoreMenuItem(
                    text = stringResource(R.string.settings),
                    icon = Icons.Default.Settings,
                    onClick = onViewSettings,
                    onDismiss = onDismiss
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun MoreMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    isPremiumFeature: Boolean = false,
    isPremium: Boolean = false
) {
    Surface(
        onClick = { onClick(); onDismiss() },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontWeight = FontWeight.Medium)
            if (isPremiumFeature && !isPremium) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Lock, contentDescription = "Premium", modifier = Modifier.size(16.dp), tint = Color.Gray)
            }
        }
    }
}
