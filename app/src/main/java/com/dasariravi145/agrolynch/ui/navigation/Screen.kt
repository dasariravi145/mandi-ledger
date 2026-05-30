package com.dasariravi145.agrolynch.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object LanguageSelection : Screen("language_selection")
    object Login : Screen("login")
    object Otp : Screen("otp")
    object Register : Screen("register")
    object ForgotPin : Screen("forgot_pin")
    object Dashboard : Screen("dashboard")
    object TransactionList : Screen("transaction_list")
    object AddTransaction : Screen("add_transaction/{transactionId}") {
        fun passId(id: String? = null) = if (id != null) "add_transaction/$id" else "add_transaction/new"
    }
    object NewArrival : Screen("new_arrival") {
        const val routeTemplate = "new_arrival?billNo={billNo}&amount={amount}&date={date}"
        fun passOcr(billNo: String = "", amount: Double = 0.0, date: Long = 0L) = 
            "new_arrival?billNo=$billNo&amount=$amount&date=$date"
    }
    object FarmerList : Screen("farmer_list")
    object AddEditFarmer : Screen("add_edit_farmer/{farmerId}") {
        fun passId(id: String? = null) = if (id != null) "add_edit_farmer/$id" else "add_edit_farmer/new"
    }
    object BuyerList : Screen("buyer_list")
    object AddEditBuyer : Screen("add_edit_buyer/{buyerId}") {
        fun passId(id: String? = null) = if (id != null) "add_edit_buyer/$id" else "add_edit_buyer/new"
    }
    object ProductList : Screen("product_list")
    object AddEditProduct : Screen("add_edit_product/{productId}") {
        fun passId(id: String? = null) = if (id != null) "add_edit_product/$id" else "add_edit_product/new"
    }
    object MarketRate : Screen("market_rate")
    object Sale : Screen("sale") {
        const val routeTemplate = "sale?billNo={billNo}&amount={amount}&date={date}"
        fun passOcr(billNo: String = "", amount: Double = 0.0, date: Long = 0L) = 
            "sale?billNo=$billNo&amount=$amount&date=$date"
    }
    object Payment : Screen("payment") {
        const val routeTemplate = "payment?billNo={billNo}&amount={amount}&date={date}"
        fun passOcr(billNo: String = "", amount: Double = 0.0, date: Long = 0L) = 
            "payment?billNo=$billNo&amount=$amount&date=$date"
    }
    object Ledger : Screen("ledger")
    object LedgerDetail : Screen("ledger_detail/{partyId}/{partyType}") {
        fun passArgs(id: String, type: String) = "ledger_detail/$id/$type"
    }
    object ReceiptPreview : Screen("receipt_preview")
    object Expense : Screen("expense")
    object Analytics : Screen("analytics")
    object BillScan : Screen("bill_scan")
    object VoiceEntry : Screen("voice_entry")
    object Security : Screen("security")
    object Backup : Screen("backup")
    object Premium : Screen("premium")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object ReportsDashboard : Screen("reports_dashboard")
    object StockReport : Screen("stock_report")
    object DailySalesReport : Screen("daily_sales_report")
    object MonthlySalesReport : Screen("monthly_sales_report")
    object CommissionReport : Screen("commission_report")
    object FarmerReport : Screen("farmer_report")
    object BuyerReport : Screen("buyer_report")
    object ExpenseReport : Screen("expense_report")
    object OutstandingReport : Screen("outstanding_report")
    object ProductPerformance : Screen("product_performance")
    object AgingReport : Screen("aging_report")
}
