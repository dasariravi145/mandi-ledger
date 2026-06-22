package com.dasariravi145.agrolynch.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.dasariravi145.agrolynch.ui.screens.*
import com.dasariravi145.agrolynch.ui.screens.analytics.*
import com.dasariravi145.agrolynch.ui.screens.arrival.ArrivalViewModel
import com.dasariravi145.agrolynch.ui.screens.arrival.NewArrivalScreen
import com.dasariravi145.agrolynch.ui.screens.auth.*
import com.dasariravi145.agrolynch.ui.screens.backup.BackupScreen
import com.dasariravi145.agrolynch.ui.screens.backup.BackupViewModel
import com.dasariravi145.agrolynch.ui.screens.buyer.*
import com.dasariravi145.agrolynch.ui.screens.dashboard.*
import com.dasariravi145.agrolynch.ui.screens.expense.ExpenseScreen
import com.dasariravi145.agrolynch.ui.screens.expense.ExpenseViewModel
import com.dasariravi145.agrolynch.ui.screens.farmer.*
import com.dasariravi145.agrolynch.ui.screens.ledger.*
import com.dasariravi145.agrolynch.ui.screens.marketrate.MarketRateScreen
import com.dasariravi145.agrolynch.ui.screens.marketrate.MarketRateViewModel
import com.dasariravi145.agrolynch.ui.screens.payment.*
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumScreen
import com.dasariravi145.agrolynch.ui.screens.premium.PremiumViewModel
import com.dasariravi145.agrolynch.ui.screens.product.*
import com.dasariravi145.agrolynch.ui.screens.receipt.*
import com.dasariravi145.agrolynch.ui.screens.report.*
import com.dasariravi145.agrolynch.ui.screens.scan.BillScanScreen
import com.dasariravi145.agrolynch.ui.screens.scan.BillScanViewModel
import com.dasariravi145.agrolynch.ui.screens.scan.ScanTarget
import com.dasariravi145.agrolynch.ui.screens.scanner.FarmerBillScannerScreen
import com.dasariravi145.agrolynch.ui.screens.scanner.ScannerViewModel
import com.dasariravi145.agrolynch.ui.screens.developer.DeveloperOptionsScreen
import com.dasariravi145.agrolynch.ui.screens.security.SecurityScreen
import com.dasariravi145.agrolynch.ui.screens.security.SecurityViewModel
import com.dasariravi145.agrolynch.ui.screens.settings.*
import com.dasariravi145.agrolynch.ui.screens.voice.VoiceEntryScreen
import com.dasariravi145.agrolynch.ui.screens.voice.VoiceNavigationEvent
import com.dasariravi145.agrolynch.ui.screens.voice.VoiceViewModel
import com.dasariravi145.agrolynch.ui.screens.template.InvoiceProfileScreen
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillData
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillItem
import com.dasariravi145.agrolynch.ui.screens.template.InvoiceProfileViewModel
import timber.log.Timber

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val isPremium by dashboardViewModel.isPremium.collectAsState(initial = false)
    
    // Store receipt data for preview
    var currentReceiptData by remember { mutableStateOf<com.dasariravi145.agrolynch.domain.model.ReceiptData?>(null) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Splash.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SplashScreen(
                viewModel = authViewModel,
                onNavigate = { isLoggedIn, isLangSelected, hasProfile ->
                    val route = when {
                        !isLangSelected -> Screen.LanguageSelection.route
                        !isLoggedIn -> Screen.Login.route
                        !hasProfile -> Screen.Register.route
                        else -> Screen.Security.route
                    }
                    Timber.d("APP_START_NAVIGATION: $route")
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.LanguageSelection.route) {
            LanguageSelectionScreen(onLanguageSelected = {
                navController.navigate(Screen.Login.route)
            })
        }
        composable(route = Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onOtpSent = {
                    val verificationId = authViewModel.state.value.verificationId ?: ""
                    val phone = authViewModel.state.value.phoneNumber ?: ""
                    navController.navigate("otp/$verificationId/$phone")
                }
            )
        }
        composable(
            route = "otp/{verificationId}/{phone}",
            arguments = listOf(
                navArgument("verificationId") { type = NavType.StringType },
                navArgument("phone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val authViewModel: AuthViewModel = hiltViewModel()
            OtpScreen(
                viewModel = authViewModel,
                onVerified = {
                    val isForgotPin = authViewModel.state.value.isForgotPinFlow
                    val pinExists = authViewModel.state.value.isRegistered
                    
                    val route = if (isForgotPin) {
                        Screen.Register.route
                    } else if (pinExists) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Register.route
                    }

                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
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
            DashboardScreen(
                viewModel = dashboardViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.navigate(Screen.Premium.route) },
                onAddTransaction = { navController.navigate(Screen.NewArrival.route) },
                onViewTransactions = { navController.navigate(Screen.TransactionList.route) },
                onViewFarmers = { navController.navigate(Screen.FarmerList.route) },
                onViewBuyers = { navController.navigate(Screen.BuyerList.route) },
                onViewProducts = { navController.navigate(Screen.ProductList.route) },
                onViewMarketRates = { navController.navigate(Screen.MarketRate.route) },
                onViewSales = { navController.navigate(Screen.Sale.route) },
                onViewPayments = { navController.navigate(Screen.Payment.route) },
                onViewLedger = { navController.navigate(Screen.Ledger.route) },
                onViewExpenses = { navController.navigate(Screen.Expense.route) },
                onViewAnalytics = { navController.navigate(Screen.Analytics.route) },
                onViewReports = { navController.navigate(Screen.ReportsDashboard.route) },
                onViewSecurity = { navController.navigate(Screen.Security.route) },
                onViewBackup = { navController.navigate(Screen.Backup.route) },
                onViewSettings = { navController.navigate(Screen.Settings.route) },
                onViewCompanyProfile = { navController.navigate(Screen.CompanyProfile.route) },
                onLogout = {
                    dashboardViewModel.onLogout() 
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.TransactionList.route) {
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            TransactionListScreen(
                viewModel = transactionViewModel,
                onAddClick = { navController.navigate(Screen.AddTransaction.passId()) },
                onTransactionClick = { id ->
                    navController.navigate(Screen.AddTransaction.passId(id))
                }
            )
        }
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            AddTransactionScreen(
                transactionId = if (transactionId == "new") null else transactionId,
                onBack = { navController.popBackStack() },
                viewModel = transactionViewModel
            )
        }
        composable(
            route = Screen.NewArrival.routeTemplate,
            arguments = listOf(
                navArgument("billNo") { defaultValue = ""; type = NavType.StringType },
                navArgument("amount") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("date") { defaultValue = 0L; type = NavType.LongType },
                navArgument("farmer") { defaultValue = ""; type = NavType.StringType },
                navArgument("phone") { defaultValue = ""; type = NavType.StringType },
                navArgument("village") { defaultValue = ""; type = NavType.StringType },
                navArgument("product") { defaultValue = ""; type = NavType.StringType },
                navArgument("category") { defaultValue = ""; type = NavType.StringType },
                navArgument("grade") { defaultValue = ""; type = NavType.StringType },
                navArgument("qty") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("rate") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("unit") { defaultValue = "KG"; type = NavType.StringType },
                navArgument("boxes") { defaultValue = 0; type = NavType.IntType },
                navArgument("weightTon") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("emptyWtBox") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("spoilage") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("comm") { defaultValue = 5f; type = NavType.FloatType },
                navArgument("labor") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("transport") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("deductions") { defaultValue = ""; type = NavType.StringType },
                navArgument("autoSave") { defaultValue = false; type = NavType.BoolType },
                navArgument("ocrItems") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            val arrivalViewModel: ArrivalViewModel = hiltViewModel()
            val billNo = backStackEntry.arguments?.getString("billNo") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val farmer = backStackEntry.arguments?.getString("farmer") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val village = backStackEntry.arguments?.getString("village") ?: ""
            val product = backStackEntry.arguments?.getString("product") ?: ""
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val grade = backStackEntry.arguments?.getString("grade") ?: ""
            val qty = backStackEntry.arguments?.getFloat("qty")?.toDouble() ?: 0.0
            val rate = backStackEntry.arguments?.getFloat("rate")?.toDouble() ?: 0.0
            val unit = backStackEntry.arguments?.getString("unit") ?: "KG"
            val boxes = backStackEntry.arguments?.getInt("boxes") ?: 0
            val weightTon = backStackEntry.arguments?.getFloat("weightTon")?.toDouble() ?: 0.0
            val emptyWtBox = backStackEntry.arguments?.getFloat("emptyWtBox")?.toDouble() ?: 0.0
            val spoilage = backStackEntry.arguments?.getFloat("spoilage")?.toDouble() ?: 0.0
            val comm = backStackEntry.arguments?.getFloat("comm")?.toDouble() ?: 5.0
            val labor = backStackEntry.arguments?.getFloat("labor")?.toDouble() ?: 0.0
            val transport = backStackEntry.arguments?.getFloat("transport")?.toDouble() ?: 0.0
            val deductionsStr = backStackEntry.arguments?.getString("deductions") ?: ""
            val autoSave = backStackEntry.arguments?.getBoolean("autoSave") ?: false
            val ocrItems = backStackEntry.arguments?.getString("ocrItems") ?: ""

            NewArrivalScreen(
                viewModel = arrivalViewModel,
                ocrBillNo = billNo,
                ocrAmount = amount,
                ocrDate = date,
                ocrFarmer = farmer,
                ocrPhone = phone,
                ocrVillage = village,
                ocrProduct = product,
                ocrCategory = category,
                ocrGrade = grade,
                ocrQty = qty,
                ocrRate = rate,
                ocrUnit = unit,
                ocrNumBoxes = boxes,
                ocrWeightTon = weightTon,
                ocrEmptyBoxWeight = emptyWtBox,
                ocrSpoilagePercent = spoilage,
                ocrComm = comm,
                ocrLabor = labor,
                ocrTransport = transport,
                ocrDeductions = deductionsStr,
                ocrAutoSave = autoSave,
                ocrItems = ocrItems,
                onBack = { navController.popBackStack() },
                onVoiceEntryClick = { navController.navigate(Screen.VoiceEntry.route) }
            )
        }
        composable(route = Screen.FarmerList.route) {
            val farmerViewModel: FarmerViewModel = hiltViewModel()
            FarmerListScreen(
                viewModel = farmerViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.navigate(Screen.Premium.route) },
                onAddClick = { navController.navigate(Screen.AddEditFarmer.passId()) },
                onFarmerClick = { id ->
                    navController.navigate(Screen.AddEditFarmer.passId(id))
                },
                onBackClick = { navController.popBackStack() }
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
                isPremium = isPremium,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.BuyerList.route) {
            val buyerViewModel: BuyerViewModel = hiltViewModel()
            BuyerListScreen(
                viewModel = buyerViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.navigate(Screen.Premium.route) },
                onAddClick = { navController.navigate(Screen.AddEditBuyer.passId()) },
                onBuyerClick = { id ->
                    navController.navigate(Screen.AddEditBuyer.passId(id))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEditBuyer.route,
            arguments = listOf(navArgument("buyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val buyerViewModel: BuyerViewModel = hiltViewModel()
            val buyerId = backStackEntry.arguments?.getString("buyerId")
            AddEditBuyerScreen(
                viewModel = buyerViewModel,
                buyerId = if (buyerId == "new") null else buyerId,
                isPremium = isPremium,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.ProductList.route) {
            val productViewModel: ProductViewModel = hiltViewModel()
            ProductListScreen(
                viewModel = productViewModel,
                onAddClick = { navController.navigate(Screen.AddEditProduct.passId()) },
                onProductClick = { id ->
                    navController.navigate(Screen.AddEditProduct.passId(id))
                },
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
                navArgument("rate") { defaultValue = 0f; type = NavType.FloatType },
                navArgument("deductions") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            val saleViewModel: com.dasariravi145.agrolynch.ui.screens.sale.SaleViewModel = hiltViewModel()
            val billNo = backStackEntry.arguments?.getString("billNo") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            val buyer = backStackEntry.arguments?.getString("buyer") ?: ""
            val product = backStackEntry.arguments?.getString("product") ?: ""
            val qty = backStackEntry.arguments?.getFloat("qty")?.toDouble() ?: 0.0
            val rate = backStackEntry.arguments?.getFloat("rate")?.toDouble() ?: 0.0
            val deductions = backStackEntry.arguments?.getString("deductions") ?: ""

            com.dasariravi145.agrolynch.ui.screens.sale.SaleScreen(
                viewModel = saleViewModel,
                ocrBillNo = billNo,
                ocrAmount = amount,
                ocrDate = date,
                ocrBuyer = buyer,
                ocrProduct = product,
                ocrQty = qty,
                ocrRate = rate,
                ocrDeductions = deductions,
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
            val ledgerViewModel: LedgerViewModel = hiltViewModel()
            LedgerScreen(
                viewModel = ledgerViewModel,
                onSummaryClick = { id, type ->
                    navController.navigate(Screen.LedgerDetail.passArgs(id, type))
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
            val receiptViewModel: ReceiptViewModel = hiltViewModel()
            if (currentReceiptData != null) {
                ReceiptPreviewScreen(
                    data = currentReceiptData!!,
                    onBack = { navController.popBackStack() },
                    onUpgradeClick = { navController.navigate(Screen.Premium.route) },
                    viewModel = receiptViewModel
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
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
        composable(route = Screen.VoiceEntry.route) {
            val voiceViewModel: VoiceViewModel = hiltViewModel()
            VoiceEntryScreen(
                viewModel = voiceViewModel,
                onNavigateToArrival = { draft, autoSave ->
                    val route = Screen.NewArrival.passOcr(
                        farmer = draft.farmerName, 
                        phone = draft.phone,
                        village = draft.village,
                        product = draft.productName, 
                        category = draft.category,
                        grade = draft.grade, 
                        qty = draft.quantity, 
                        unit = draft.unitType, 
                        rate = draft.rate, 
                        spoilage = if (draft.unitType == "Boxes") draft.spoilagePercent else draft.waste,
                        boxes = draft.numBoxes,
                        weightTon = draft.totalWeightTon,
                        emptyWtBox = draft.emptyWeightPerBox,
                        comm = draft.commissionPercent,
                        labor = draft.laborCharges,
                        transport = draft.transportCharges,
                        deductions = "Other:${draft.otherDeductions}",
                        autoSave = autoSave
                    )
                    navController.navigate(route) {
                        popUpTo(Screen.VoiceEntry.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Security.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SecurityScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Security.route) { inclusive = true }
                    }
                },
                onForgotPin = { navController.navigate(Screen.ForgotPin.route) }
            )
        }
        composable(route = Screen.ForgotPin.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ForgotPinScreen(
                viewModel = authViewModel,
                onOtpSent = {
                    val verificationId = authViewModel.state.value.verificationId ?: ""
                    val phone = authViewModel.state.value.phoneNumber ?: ""
                    navController.navigate("otp/$verificationId/$phone")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Backup.route) {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                viewModel = backupViewModel,
                isPremium = isPremium,
                onUpgradeClick = { navController.navigate(Screen.Premium.route) },
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
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onViewProfile = { navController.navigate(Screen.Profile.route) },
                onViewCompanyProfile = { navController.navigate(Screen.CompanyProfile.route) },
                onViewBillSettings = { navController.navigate(Screen.BillSettings.route) },
                onViewBackup = { navController.navigate(Screen.Backup.route) },
                onViewSubscription = { navController.navigate(Screen.Premium.route) },
                onViewDeveloperOptions = { navController.navigate(Screen.DeveloperOptions.route) },
                onLanguageChanged = { /* Restart activity if needed */ },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.BillSettings.route) {
            val billSettingsViewModel: com.dasariravi145.agrolynch.ui.screens.settings.BillSettingsViewModel = hiltViewModel()
            BillSettingsScreen(
                viewModel = billSettingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Profile.route) {
            val profileViewModel = hiltViewModel<com.dasariravi145.agrolynch.ui.screens.settings.ProfileViewModel>()
            ProfileScreen(viewModel = profileViewModel, onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.CompanyProfile.route) {
            val companyViewModel: com.dasariravi145.agrolynch.ui.screens.settings.CompanyViewModel = hiltViewModel()
            CompanyProfileScreen(
                viewModel = companyViewModel,
                onBack = { navController.popBackStack() },
                onEditTemplate = { navController.navigate(Screen.TemplateEditor.route) },
                onDesignTemplate = { _ ->
                    navController.navigate(Screen.InvoiceProfileSetup.route)
                }
            )
        }
        composable(route = Screen.TemplateEditor.route) {
            val editorViewModel: com.dasariravi145.agrolynch.ui.screens.settings.TemplateEditorViewModel = hiltViewModel()
            val companyViewModel: com.dasariravi145.agrolynch.ui.screens.settings.CompanyViewModel = hiltViewModel()
            val profile by companyViewModel.profile.collectAsState()
            TemplateEditorScreen(
                viewModel = editorViewModel,
                templateImageUrl = profile?.customTemplatePath,
                onBack = { navController.popBackStack() }
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
                onNavigateToOutstandingReport = { navController.navigate(Screen.OutstandingReport.route) },
                onNavigateToProductPerformance = { navController.navigate(Screen.ProductPerformance.route) }
            )
        }
        composable(route = Screen.StockReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            StockReportScreen(viewModel = reportViewModel, onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.DailySalesReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            DailySalesReportScreen(viewModel = reportViewModel, onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.MonthlySalesReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            MonthlySalesReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.CommissionReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            CommissionReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.FarmerReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            FarmerReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.BuyerReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            BuyerReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.ExpenseReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            ExpenseReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.OutstandingReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            OutstandingAgingScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.ProductPerformance.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            ProductReportScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.AgingReport.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            OutstandingAgingScreen(viewModel = reportViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.DeveloperOptions.route) {
            val developerViewModel: com.dasariravi145.agrolynch.ui.screens.developer.DeveloperViewModel = hiltViewModel()
            DeveloperOptionsScreen(viewModel = developerViewModel, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.InvoiceProfileSetup.route) {
            val profileViewModel: InvoiceProfileViewModel = hiltViewModel()
            InvoiceProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
