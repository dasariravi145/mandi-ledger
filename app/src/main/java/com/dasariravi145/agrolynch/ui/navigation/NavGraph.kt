package com.dasariravi145.agrolynch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dasariravi145.agrolynch.ui.screens.TransactionListScreen
import com.dasariravi145.agrolynch.ui.screens.AddTransactionScreen
import com.dasariravi145.agrolynch.ui.screens.SplashScreen
import com.dasariravi145.agrolynch.ui.screens.auth.AuthEvent
import com.dasariravi145.agrolynch.ui.screens.auth.AuthViewModel
import com.dasariravi145.agrolynch.ui.screens.auth.ForgotPinScreen
import com.dasariravi145.agrolynch.ui.screens.auth.LoginScreen
import com.dasariravi145.agrolynch.ui.screens.auth.OtpScreen
import com.dasariravi145.agrolynch.ui.screens.auth.RegistrationScreen
import com.dasariravi145.agrolynch.ui.screens.arrival.NewArrivalScreen
import com.dasariravi145.agrolynch.ui.screens.arrival.ArrivalViewModel
import com.dasariravi145.agrolynch.ui.screens.dashboard.DashboardScreen
import com.dasariravi145.agrolynch.ui.screens.dashboard.DashboardViewModel
import com.dasariravi145.agrolynch.ui.screens.farmer.AddEditFarmerScreen
import com.dasariravi145.agrolynch.ui.screens.farmer.FarmerListScreen
import com.dasariravi145.agrolynch.ui.screens.farmer.FarmerViewModel
import com.dasariravi145.agrolynch.ui.screens.buyer.AddEditBuyerScreen
import com.dasariravi145.agrolynch.ui.screens.buyer.BuyerListScreen
import com.dasariravi145.agrolynch.ui.screens.buyer.BuyerViewModel
import com.dasariravi145.agrolynch.ui.screens.product.AddEditProductScreen
import com.dasariravi145.agrolynch.ui.screens.product.ProductListScreen
import com.dasariravi145.agrolynch.ui.screens.product.ProductViewModel
import com.dasariravi145.agrolynch.ui.screens.marketrate.MarketRateScreen
import com.dasariravi145.agrolynch.ui.screens.marketrate.MarketRateViewModel
import com.dasariravi145.agrolynch.ui.screens.LanguageSelectionScreen
import com.dasariravi145.agrolynch.ui.screens.sale.SaleScreen
import com.dasariravi145.agrolynch.ui.screens.sale.SaleViewModel
import com.dasariravi145.agrolynch.ui.screens.payment.PaymentScreen
import com.dasariravi145.agrolynch.ui.screens.payment.PaymentViewModel
import com.dasariravi145.agrolynch.ui.screens.ledger.LedgerScreen
import com.dasariravi145.agrolynch.ui.screens.ledger.LedgerDetailScreen
import com.dasariravi145.agrolynch.ui.screens.ledger.LedgerViewModel
import com.dasariravi145.agrolynch.ui.screens.receipt.ReceiptPreviewScreen
import com.dasariravi145.agrolynch.ui.screens.receipt.ReceiptViewModel
import com.dasariravi145.agrolynch.ui.screens.expense.ExpenseScreen
import com.dasariravi145.agrolynch.ui.screens.expense.ExpenseViewModel
import com.dasariravi145.agrolynch.ui.screens.analytics.AnalyticsScreen
import com.dasariravi145.agrolynch.ui.screens.analytics.AnalyticsViewModel
import com.dasariravi145.agrolynch.ui.screens.scan.*
import com.dasariravi145.agrolynch.ui.screens.voice.VoiceEntryScreen
import com.dasariravi145.agrolynch.ui.screens.voice.VoiceViewModel
import com.dasariravi145.agrolynch.ui.screens.security.SecurityScreen
import com.dasariravi145.agrolynch.ui.screens.security.SecurityViewModel
import com.dasariravi145.agrolynch.ui.screens.backup.BackupScreen
import com.dasariravi145.agrolynch.ui.screens.backup.BackupViewModel
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumScreen
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumViewModel
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumUpgradePopup
import com.dasariravi145.agrolynch.ui.screens.settings.*
import com.dasariravi145.agrolynch.ui.screens.report.*
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import androidx.navigation.NavType
import androidx.navigation.navArgument
import timber.log.Timber

@Composable
fun SetupNavGraph(navController: NavHostController) {
    Timber.d("NavGraph: SetupNavGraph initialized")
    // Store receipt data for preview
    var currentReceiptData by remember { mutableStateOf<ReceiptData?>(null) }
    
    // Share AuthViewModel across Splash, Login, and Otp screens
    val authViewModel: AuthViewModel = hiltViewModel()
    val premiumViewModel: PremiumViewModel = hiltViewModel()
    val isPremium by premiumViewModel.isPremium.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            Timber.d("NavGraph: Screen.Splash")
            val state by authViewModel.state.collectAsStateWithLifecycle()
            SplashScreen(
                viewModel = authViewModel,
                onNavigate = { isLoggedIn, isLangSelected, hasPin ->
                    if (isLoggedIn) {
                        navController.safeNavigate(Screen.Security.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else if (!isLangSelected) {
                        navController.safeNavigate(Screen.LanguageSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else if (hasPin) {
                        navController.safeNavigate(Screen.Security.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.safeNavigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(route = Screen.LanguageSelection.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            LanguageSelectionScreen(
                onLanguageSelected = { code ->
                    // Restart activity to apply language changes
                    val intent = android.content.Intent(context, com.dasariravi145.agrolynch.MainActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                }
            )
        }
        composable(route = Screen.Security.route) {
            Timber.d("NavGraph: Screen.Security")
            SecurityScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.safeNavigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Security.route) { inclusive = true }
                    }
                },
                onForgotPin = {
                    navController.safeNavigate(Screen.ForgotPin.route)
                }
            )
        }
        composable(route = Screen.ForgotPin.route) {
            ForgotPinScreen(
                viewModel = authViewModel,
                onOtpSent = {
                    navController.navigate(Screen.Otp.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onOtpSent = {
                    navController.navigate(Screen.Otp.route)
                }
            )
        }
        composable(route = Screen.Otp.route) {
            val state by authViewModel.state.collectAsStateWithLifecycle()
            OtpScreen(
                viewModel = authViewModel,
                onVerified = {
                    if (state.isRegistered) {
                        navController.safeNavigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.safeNavigate(Screen.Register.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(route = Screen.Register.route) {
            RegistrationScreen(
                viewModel = authViewModel,
                onRegistered = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Dashboard.route) {
            Timber.d("NavGraph: Screen.Dashboard")
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            
            // Show popup immediately after registration if required
            LaunchedEffect(Unit) {
                if (dashboardViewModel.shouldShowPopupAfterRegistration()) {
                    dashboardViewModel.markPopupSeenAfterRegistration()
                    // The dashboardViewModel.checkPremiumPopup() in init will handle setting _showPremiumPopup to true
                }
            }

            DashboardScreen(
                viewModel = dashboardViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.safeNavigate(Screen.Premium.route) },
                onAddTransaction = {
                    navController.safeNavigate(Screen.NewArrival.route)
                },
                onViewTransactions = {
                    navController.safeNavigate(Screen.TransactionList.route)
                },
                onViewFarmers = {
                    navController.safeNavigate(Screen.FarmerList.route)
                },
                onViewBuyers = {
                    navController.safeNavigate(Screen.BuyerList.route)
                },
                onViewProducts = {
                    navController.safeNavigate(Screen.ProductList.route)
                },
                onViewMarketRates = {
                    navController.safeNavigate(Screen.MarketRate.route)
                },
                onViewSales = {
                    navController.safeNavigate(Screen.Sale.route)
                },
                onViewPayments = {
                    navController.safeNavigate(Screen.Payment.route)
                },
                onViewLedger = {
                    navController.safeNavigate(Screen.Ledger.route)
                },
                onViewExpenses = {
                    navController.safeNavigate(Screen.Expense.route)
                },
                onViewAnalytics = {
                    navController.safeNavigate(Screen.Analytics.route)
                },
                onViewReports = {
                    navController.safeNavigate(Screen.ReportsDashboard.route)
                },
                onViewBillScan = {
                    navController.safeNavigate(Screen.BillScan.route)
                },
                onViewVoiceEntry = {
                    navController.safeNavigate(Screen.VoiceEntry.route)
                },
                onViewSecurity = {
                    navController.safeNavigate(Screen.Security.route)
                },
                onViewBackup = {
                    navController.safeNavigate(Screen.Backup.route)
                },
                onViewSettings = {
                    navController.safeNavigate(Screen.Settings.route)
                },
                onLogout = {
                    authViewModel.onEvent(AuthEvent.Logout)
                    navController.safeNavigate(Screen.Security.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.TransactionList.route) {
            Timber.d("NavGraph: Screen.TransactionList")
            TransactionListScreen(
                onAddClick = {
                    navController.safeNavigate(Screen.AddTransaction.passId())
                },
                onTransactionClick = { id ->
                    navController.safeNavigate(Screen.AddTransaction.passId(id))
                }
            )
        }
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            AddTransactionScreen(
                transactionId = if (transactionId == "new") null else transactionId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.NewArrival.routeTemplate,
            arguments = listOf(
                navArgument("billNo") { defaultValue = ""; type = NavType.StringType },
                navArgument("amount") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("date") { defaultValue = 0L; type = NavType.LongType },
                navArgument("farmer") { defaultValue = ""; type = NavType.StringType },
                navArgument("product") { defaultValue = ""; type = NavType.StringType },
                navArgument("qty") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("rate") { defaultValue = 0f; type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val arrivalViewModel: ArrivalViewModel = hiltViewModel()
            val billNo = backStackEntry.arguments?.getString("billNo") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val farmer = backStackEntry.arguments?.getString("farmer") ?: ""
            val product = backStackEntry.arguments?.getString("product") ?: ""
            val qty = backStackEntry.arguments?.getFloat("qty")?.toDouble() ?: 0.0
            val rate = backStackEntry.arguments?.getFloat("rate")?.toDouble() ?: 0.0

            NewArrivalScreen(
                viewModel = arrivalViewModel,
                ocrBillNo = billNo,
                ocrAmount = amount,
                ocrDate = date,
                ocrFarmer = farmer,
                ocrProduct = product,
                ocrQty = qty,
                ocrRate = rate,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.FarmerList.route) {
            Timber.d("NavGraph: Navigating to FarmerList")
            val farmerViewModel: FarmerViewModel = hiltViewModel()
            FarmerListScreen(
                viewModel = farmerViewModel,
                onAddClick = { 
                    Timber.d("NavGraph: FarmerList -> AddFarmer")
                    navController.safeNavigate(Screen.AddEditFarmer.passId()) 
                },
                onFarmerClick = { id -> 
                    Timber.d("NavGraph: FarmerList -> EditFarmer($id)")
                    navController.safeNavigate(Screen.AddEditFarmer.passId(id)) 
                },
                onBackClick = { 
                    Timber.d("NavGraph: FarmerList -> Back")
                    navController.popBackStack() 
                }
            )
        }
        composable(
            route = Screen.AddEditFarmer.route,
            arguments = listOf(navArgument("farmerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val farmerViewModel: FarmerViewModel = hiltViewModel()
            val farmerId = backStackEntry.arguments?.getString("farmerId")
            AddEditFarmerScreen(
                viewModel = farmerViewModel,
                farmerId = if (farmerId == "new") null else farmerId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.BuyerList.route) {
            Timber.i("NavGraph: Navigating to BuyerList")
            val buyerViewModel: BuyerViewModel = hiltViewModel()
            BuyerListScreen(
                viewModel = buyerViewModel,
                onAddClick = { navController.safeNavigate(Screen.AddEditBuyer.passId()) },
                onBuyerClick = { id -> navController.safeNavigate(Screen.AddEditBuyer.passId(id)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEditBuyer.route,
            arguments = listOf(navArgument("buyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            Timber.i("NavGraph: Navigating to AddEditBuyer")
            val buyerViewModel: BuyerViewModel = hiltViewModel()
            val buyerId = backStackEntry.arguments?.getString("buyerId")
            AddEditBuyerScreen(
                viewModel = buyerViewModel,
                buyerId = if (buyerId == "new") null else buyerId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.ProductList.route) {
            Timber.i("NavGraph: Navigating to ProductList")
            val productViewModel: ProductViewModel = hiltViewModel()
            ProductListScreen(
                viewModel = productViewModel,
                onAddClick = { navController.safeNavigate(Screen.AddEditProduct.passId()) },
                onProductClick = { id -> navController.safeNavigate(Screen.AddEditProduct.passId(id)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productViewModel: ProductViewModel = hiltViewModel()
            val productId = backStackEntry.arguments?.getString("productId")
            AddEditProductScreen(
                viewModel = productViewModel,
                productId = if (productId == "new") null else productId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.MarketRate.route) {
            val marketRateViewModel: MarketRateViewModel = hiltViewModel()
            MarketRateScreen(
                viewModel = marketRateViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Sale.routeTemplate,
            arguments = listOf(
                navArgument("billNo") { defaultValue = ""; type = NavType.StringType },
                navArgument("amount") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("date") { defaultValue = 0L; type = NavType.LongType },
                navArgument("buyer") { defaultValue = ""; type = NavType.StringType },
                navArgument("product") { defaultValue = ""; type = NavType.StringType },
                navArgument("qty") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("rate") { defaultValue = 0f; type = NavType.FloatType }
            )
        ) { backStackEntry ->
            Timber.i("NavGraph: Navigating to Sale")
            val saleViewModel: SaleViewModel = hiltViewModel()
            val billNo = backStackEntry.arguments?.getString("billNo") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val buyer = backStackEntry.arguments?.getString("buyer") ?: ""
            val product = backStackEntry.arguments?.getString("product") ?: ""
            val qty = backStackEntry.arguments?.getFloat("qty")?.toDouble() ?: 0.0
            val rate = backStackEntry.arguments?.getFloat("rate")?.toDouble() ?: 0.0

            SaleScreen(
                viewModel = saleViewModel,
                ocrBillNo = billNo,
                ocrAmount = amount,
                ocrDate = date,
                ocrBuyer = buyer,
                ocrProduct = product,
                ocrQty = qty,
                ocrRate = rate,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Payment.routeTemplate,
            arguments = listOf(
                navArgument("billNo") { defaultValue = ""; type = NavType.StringType },
                navArgument("amount") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("date") { defaultValue = 0L; type = NavType.LongType },
                navArgument("party") { defaultValue = ""; type = NavType.StringType },
                navArgument("mode") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            Timber.i("NavGraph: Navigating to Payment")
            val paymentViewModel: PaymentViewModel = hiltViewModel()
            val billNo = backStackEntry.arguments?.getString("billNo") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val party = backStackEntry.arguments?.getString("party") ?: ""
            val mode = backStackEntry.arguments?.getString("mode") ?: ""

            PaymentScreen(
                viewModel = paymentViewModel,
                ocrBillNo = billNo,
                ocrAmount = amount,
                ocrDate = date,
                ocrPartyName = party,
                ocrMode = mode,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Ledger.route) {
            Timber.i("NavGraph: Navigating to Ledger")
            val ledgerViewModel: LedgerViewModel = hiltViewModel()
            LedgerScreen(
                viewModel = ledgerViewModel,
                onSummaryClick = { id, type ->
                    Timber.d("NavGraph: Ledger -> Detail($id, $type)")
                    navController.safeNavigate(Screen.LedgerDetail.passArgs(id, type))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.LedgerDetail.route,
            arguments = listOf(
                navArgument("partyId") { type = NavType.StringType },
                navArgument("partyType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ledgerViewModel: LedgerViewModel = hiltViewModel()
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            val partyType = backStackEntry.arguments?.getString("partyType") ?: ""
            LedgerDetailScreen(
                viewModel = ledgerViewModel,
                partyId = partyId,
                partyType = partyType,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.ReceiptPreview.route) {
            Timber.d("NavGraph: Screen.ReceiptPreview")
            val receiptViewModel: ReceiptViewModel = hiltViewModel()
            if (currentReceiptData != null) {
                ReceiptPreviewScreen(
                    data = currentReceiptData!!,
                    viewModel = receiptViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Timber.w("NavGraph: currentReceiptData is null in ReceiptPreview. Popping back.")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        composable(route = Screen.Expense.route) {
            val expenseViewModel: ExpenseViewModel = hiltViewModel()
            ExpenseScreen(
                viewModel = expenseViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Analytics.route) {
            val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.BillScan.route) {
            val billScanViewModel: BillScanViewModel = hiltViewModel()
            BillScanScreen(
                viewModel = billScanViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.navigate(Screen.Premium.route) },
                onNavigateToEntry = { target, billNo, amount, date, farmer, buyer, party, product, qty, rate, mode ->
                    val route = when(target) {
                        ScanTarget.STOCK_ENTRY -> Screen.NewArrival.passOcr(billNo, amount, date, farmer, product, qty, rate)
                        ScanTarget.SALE_ENTRY -> Screen.Sale.passOcr(billNo, amount, date, buyer, product, qty, rate)
                        ScanTarget.PAYMENT, ScanTarget.CHEQUE -> Screen.Payment.passOcr(billNo, amount, date, party, mode)
                        ScanTarget.EXPENSE -> Screen.Expense.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.BillScan.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.VoiceEntry.route) {
            val voiceViewModel: VoiceViewModel = hiltViewModel()
            VoiceEntryScreen(
                viewModel = voiceViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Backup.route) {
            Timber.d("NavGraph: Screen.Backup")
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                viewModel = backupViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.safeNavigate(Screen.Premium.route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Premium.route) {
            val premiumViewModel: PremiumViewModel = hiltViewModel()
            PremiumScreen(
                viewModel = premiumViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onViewProfile = { navController.navigate(Screen.Profile.route) },
                onViewCompanyProfile = { navController.navigate(Screen.CompanyProfile.route) },
                onViewBackup = { navController.navigate(Screen.Backup.route) },
                onViewSubscription = { navController.navigate(Screen.Premium.route) },
                onLanguageChanged = {
                    val intent = android.content.Intent(context, com.dasariravi145.agrolynch.MainActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                },
                onLogout = {
                    authViewModel.onEvent(AuthEvent.Logout)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.CompanyProfile.route) {
            val companyViewModel: CompanyViewModel = hiltViewModel()
            CompanyProfileScreen(
                viewModel = companyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ReportsDashboard.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            ReportsDashboardScreen(
                viewModel = reportViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToStockReport = { navController.navigate(Screen.StockReport.route) },
                onNavigateToDailySalesReport = { navController.navigate(Screen.DailySalesReport.route) },
                onNavigateToMonthlySalesReport = { navController.navigate(Screen.MonthlySalesReport.route) },
                onNavigateToCommissionReport = { navController.navigate(Screen.CommissionReport.route) },
                onNavigateToFarmerReport = { navController.navigate(Screen.FarmerReport.route) },
                onNavigateToBuyerReport = { navController.navigate(Screen.BuyerReport.route) },
                onNavigateToExpenseReport = { navController.navigate(Screen.ExpenseReport.route) },
                onNavigateToOutstandingReport = { navController.navigate(Screen.AgingReport.route) },
                onNavigateToProductPerformance = { navController.navigate(Screen.ProductPerformance.route) }
            )
        }

        composable(route = Screen.StockReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            StockReportScreen(
                viewModel = reportViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.DailySalesReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            DailySalesReportScreen(
                viewModel = reportViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ProductPerformance.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            ProductReportScreen(
                viewModel = reportViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AgingReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            OutstandingAgingScreen(
                viewModel = reportViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.OutstandingReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            OutstandingReportScreen(
                viewModel = reportViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.MonthlySalesReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            MonthlySalesReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = Screen.CommissionReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            CommissionReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = Screen.FarmerReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            FarmerReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = Screen.BuyerReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            BuyerReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = Screen.ExpenseReport.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.ReportsDashboard.route) }
            val reportViewModel: ReportViewModel = hiltViewModel(parentEntry)
            ExpenseReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
    }
}
