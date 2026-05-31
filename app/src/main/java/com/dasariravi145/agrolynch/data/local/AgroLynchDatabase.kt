package com.dasariravi145.agrolynch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        DashboardSummaryEntity::class
    ],
    version = 26,
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
}
