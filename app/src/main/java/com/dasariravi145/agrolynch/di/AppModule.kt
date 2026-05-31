package com.dasariravi145.agrolynch.di

import android.content.Context
import androidx.room.Room
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.dao.TransactionDao
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
        return Room.databaseBuilder(
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
            com.dasariravi145.agrolynch.data.local.DatabaseMigrations.MIGRATION_25_26
        )
        .build()
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
}
