package com.dasariravi145.agrolynch.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
        const val routeTemplate = "new_arrival?billNo={billNo}&amount={amount}&date={date}&farmer={farmer}&phone={phone}&village={village}&product={product}&category={category}&grade={grade}&qty={qty}&rate={rate}&unit={unit}&boxes={boxes}&weightTon={weightTon}&emptyWtBox={emptyWtBox}&spoilage={spoilage}&comm={comm}&deductions={deductions}"
        fun passOcr(
            billNo: String = "", amount: Double = 0.0, date: Long = 0L, 
            farmer: String = "", phone: String = "", village: String = "", product: String = "", 
            category: String = "", grade: String = "", qty: Double = 0.0, rate: Double = 0.0,
            unit: String = "KG", boxes: Int = 0, weightTon: Double = 0.0, emptyWtBox: Double = 0.0, spoilage: Double = 0.0,
            comm: Double = 5.0, deductions: String = ""
        ) = "new_arrival?billNo=${enc(billNo)}&amount=$amount&date=$date&farmer=${enc(farmer)}&phone=${enc(phone)}&village=${enc(village)}&product=${enc(product)}&category=${enc(category)}&grade=${enc(grade)}&qty=$qty&rate=$rate&unit=$unit&boxes=$boxes&weightTon=$weightTon&emptyWtBox=$emptyWtBox&spoilage=$spoilage&comm=$comm&deductions=${enc(deductions)}"
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
        const val routeTemplate = "sale?billNo={billNo}&amount={amount}&date={date}&buyer={buyer}&product={product}&qty={qty}&rate={rate}&deductions={deductions}"
        fun passOcr(billNo: String = "", amount: Double = 0.0, date: Long = 0L, buyer: String = "", product: String = "", qty: Double = 0.0, rate: Double = 0.0, deductions: String = "") = 
            "sale?billNo=${enc(billNo)}&amount=$amount&date=$date&buyer=${enc(buyer)}&product=${enc(product)}&qty=$qty&rate=$rate&deductions=${enc(deductions)}"
    }
    object Payment : Screen("payment") {
        const val routeTemplate = "payment?billNo={billNo}&amount={amount}&date={date}&party={party}&mode={mode}"
        fun passOcr(billNo: String = "", amount: Double = 0.0, date: Long = 0L, party: String = "", mode: String = "") = 
            "payment?billNo=${enc(billNo)}&amount=$amount&date=$date&party=${enc(party)}&mode=${enc(mode)}"
    }
    object Ledger : Screen("ledger")
    object LedgerDetail : Screen("ledger_detail/{partyId}/{partyType}") {
        fun passArgs(id: String, type: String) = "ledger_detail/$id/$type"
    }
    object ReceiptPreview : Screen("receipt_preview")
    object Expense : Screen("expense")
    object Analytics : Screen("analytics")
    object BillScan : Screen("bill_scan")
    object FarmerBillScanner : Screen("farmer_bill_scanner")
    object VoiceEntry : Screen("voice_entry")
    object Security : Screen("security")
    object Backup : Screen("backup")
    object Premium : Screen("premium")
    object Settings : Screen("settings")
    object BillSettings : Screen("bill_settings")
    object Profile : Screen("profile")
    object CompanyProfile : Screen("company_profile")
    object TemplateEditor : Screen("template_editor")
    object InvoiceProfileSetup : Screen("invoice_profile_setup")
    object InvoiceMapper : Screen("invoice_mapper/{templateId}/{bg}") {
        fun passArgs(id: String, bg: String) = "invoice_mapper/$id/$bg"
    }
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
    object DeveloperOptions : Screen("developer_options")

    protected fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
}
