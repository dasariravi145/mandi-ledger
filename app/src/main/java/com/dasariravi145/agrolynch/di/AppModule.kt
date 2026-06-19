package com.dasariravi145.agrolynch.di

import android.content.Context
import androidx.room.Room
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.repository.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.dasariravi145.agrolynch.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgroLynchDatabase {
        val builder = Room.databaseBuilder(
            context,
            AgroLynchDatabase::class.java,
            "agrolynch_db"
        )
        .addMigrations(
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_14_15,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_15_16,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_16_17,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_17_18,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_18_19,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_19_20,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_20_21,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_21_22,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_22_23,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_23_24,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_24_25,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_25_26,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_26_27,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_27_28,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_28_29,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_29_30,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_30_31,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_31_32,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_32_33,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_33_34,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_34_35,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_35_36,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_36_37,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_37_38,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_38_39,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_39_40,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_40_41,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_41_42,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_42_43,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_43_44,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_44_45,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_45_46,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_46_47,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_47_48,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_48_49,
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_49_50
        )

        if (com.dasariravi145.agrolynch.BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(true)
            android.util.Log.d("ROOM_DB", "Database initializing with fallbackToDestructiveMigration in DEBUG mode")
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: AgroLynchDatabase): TransactionDao {
        return db.transactionDao()
    }

    @Provides
    @Singleton
    fun provideFarmerDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.FarmerDao {
        return db.farmerDao()
    }

    @Provides
    @Singleton
    fun provideBuyerDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.BuyerDao {
        return db.buyerDao()
    }

    @Provides
    @Singleton
    fun provideProductDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.ProductDao {
        return db.productDao()
    }

    @Provides
    @Singleton
    fun provideMarketRateDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.MarketRateDao {
        return db.marketRateDao()
    }

    @Provides
    @Singleton
    fun provideSaleDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.SaleDao {
        return db.saleDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.PaymentDao {
        return db.paymentDao()
    }

    @Provides
    @Singleton
    fun provideExpenseDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.ExpenseDao {
        return db.expenseDao()
    }

    @Provides
    @Singleton
    fun provideArrivalDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.ArrivalDao {
        return db.arrivalDao()
    }

    @Provides
    @Singleton
    fun provideOcrScanDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.OcrScanDao {
        return db.ocrScanDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.UserDao {
        return db.userDao()
    }

    @Provides
    @Singleton
    fun provideSubscriptionDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.SubscriptionDao {
        return db.subscriptionDao()
    }

    @Provides
    @Singleton
    fun provideBackupDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.BackupDao {
        return db.backupDao()
    }

    @Provides
    @Singleton
    fun provideDashboardDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.DashboardDao {
        return db.dashboardDao()
    }

    @Provides
    @Singleton
    fun provideReportDao(db: AgroLynchDatabase): com.dasariravi145.agrolynch.data.local.dao.ReportDao {
        return db.reportDao()
    }

    @Provides
    @Singleton
    fun provideCompanyProfileDao(db: AgroLynchDatabase): CompanyProfileDao {
        return db.companyProfileDao()
    }

    @Provides
    @Singleton
    fun provideBillNumberSeriesDao(db: AgroLynchDatabase): BillNumberSeriesDao {
        return db.billNumberSeriesDao()
    }

    @Provides
    @Singleton
    fun provideEntryDeductionDao(db: AgroLynchDatabase): EntryDeductionDao {
        return db.entryDeductionDao()
    }

    @Provides
    @Singleton
    fun provideTemplatePositionDao(db: AgroLynchDatabase): TemplatePositionDao {
        return db.templatePositionDao()
    }

    @Provides
    @Singleton
    fun provideInvoiceLayoutDao(db: AgroLynchDatabase): InvoiceLayoutDao {
        return db.invoiceLayoutDao()
    }

    @Provides
    @Singleton
    fun provideInvoiceWizardDao(db: AgroLynchDatabase): InvoiceWizardDao {
        return db.invoiceWizardDao()
    }

    @Provides
    @Singleton
    fun provideBillNumberRepository(
        seriesDao: BillNumberSeriesDao,
        deductionDao: EntryDeductionDao
    ): BillNumberRepository {
        return BillNumberRepositoryImpl(seriesDao, deductionDao)
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .create()
    }
}
