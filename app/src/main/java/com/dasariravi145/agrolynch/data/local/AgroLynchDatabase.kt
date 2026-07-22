package com.dasariravi145.agrolynch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.dao.BuyerDao
import com.dasariravi145.agrolynch.data.local.dao.FarmerDao
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        FarmerEntity::class,
        BuyerEntity::class,
        ProductEntity::class,
        MarketRateEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        ArrivalEntity::class,
        SaleItemEntity::class,
        OcrScanEntity::class,
        UserEntity::class,
        SubscriptionEntity::class,
        BackupEntity::class,
        DashboardSummaryEntity::class,
        CompanyProfileEntity::class,
        BoxWeightItemEntity::class,
        BillNumberSeriesEntity::class,
        EntryDeductionEntity::class,
        InvoiceTemplatePositionEntity::class,
        InvoiceLayoutEntity::class,
        InvoiceWizardConfigEntity::class,
        ProductTypeEntity::class,
        AccountBookArchiveEntity::class
    ],
    version = 56,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AgroLynchDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun farmerDao(): FarmerDao
    abstract fun buyerDao(): BuyerDao
    abstract fun productDao(): ProductDao
    abstract fun marketRateDao(): MarketRateDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun arrivalDao(): ArrivalDao
    abstract fun ocrScanDao(): OcrScanDao
    abstract fun userDao(): UserDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun backupDao(): BackupDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun reportDao(): ReportDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun boxWeightDao(): BoxWeightDao
    abstract fun billNumberSeriesDao(): BillNumberSeriesDao
    abstract fun entryDeductionDao(): EntryDeductionDao
    abstract fun templatePositionDao(): TemplatePositionDao
    abstract fun invoiceLayoutDao(): InvoiceLayoutDao
    abstract fun invoiceWizardDao(): InvoiceWizardDao
    abstract fun productTypeDao(): ProductTypeDao
    abstract fun accountBookArchiveDao(): AccountBookArchiveDao

    suspend fun clearSupportedTables() {
        withTransaction {
            query("DELETE FROM arrivals", null).close()
            query("DELETE FROM sales", null).close()
            query("DELETE FROM sale_items", null).close()
            query("DELETE FROM payments", null).close()
            query("DELETE FROM transactions", null).close()
            query("DELETE FROM expenses", null).close()
            query("DELETE FROM farmers", null).close()
            query("DELETE FROM buyers", null).close()
            query("DELETE FROM products", null).close()
            query("DELETE FROM product_types", null).close()
            query("DELETE FROM market_rates", null).close()
            query("DELETE FROM ocr_scans", null).close()
            query("DELETE FROM box_weight_items", null).close()
            query("DELETE FROM bill_number_series", null).close()
            query("DELETE FROM entry_deductions", null).close()
            query("DELETE FROM template_positions", null).close()
            query("DELETE FROM invoice_layouts", null).close()
            query("DELETE FROM invoice_wizard_configs", null).close()
            query("DELETE FROM account_book_archives", null).close()
            query("DELETE FROM company_profile", null).close()
        }
    }
}
