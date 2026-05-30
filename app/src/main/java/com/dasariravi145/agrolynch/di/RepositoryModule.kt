package com.dasariravi145.agrolynch.di

import com.dasariravi145.agrolynch.data.repository.TransactionRepositoryImpl
import com.dasariravi145.agrolynch.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.dasariravi145.agrolynch.data.repository.AuthRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: com.dasariravi145.agrolynch.data.repository.DashboardRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.DashboardRepository

    @Binds
    @Singleton
    abstract fun bindFarmerRepository(
        farmerRepositoryImpl: com.dasariravi145.agrolynch.data.repository.FarmerRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.FarmerRepository

    @Binds
    @Singleton
    abstract fun bindBuyerRepository(
        buyerRepositoryImpl: com.dasariravi145.agrolynch.data.repository.BuyerRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.BuyerRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: com.dasariravi145.agrolynch.data.repository.ProductRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.ProductRepository

    @Binds
    @Singleton
    abstract fun bindMarketRateRepository(
        marketRateRepositoryImpl: com.dasariravi145.agrolynch.data.repository.MarketRateRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.MarketRateRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(
        saleRepositoryImpl: com.dasariravi145.agrolynch.data.repository.SaleRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.SaleRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: com.dasariravi145.agrolynch.data.repository.PaymentRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.PaymentRepository

    @Binds
    @Singleton
    abstract fun bindLedgerRepository(
        ledgerRepositoryImpl: com.dasariravi145.agrolynch.data.repository.LedgerRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.LedgerRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: com.dasariravi145.agrolynch.data.repository.ExpenseRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: com.dasariravi145.agrolynch.data.repository.NotificationRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: com.dasariravi145.agrolynch.data.repository.AnalyticsRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: com.dasariravi145.agrolynch.data.repository.BackupRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.BackupRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: com.dasariravi145.agrolynch.data.repository.SettingsRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.SettingsRepository

    @Binds
    @Singleton
    abstract fun bindArrivalRepository(
        arrivalRepositoryImpl: com.dasariravi145.agrolynch.data.repository.ArrivalRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.ArrivalRepository

    @Binds
    @Singleton
    abstract fun bindOcrRepository(
        ocrRepositoryImpl: com.dasariravi145.agrolynch.data.repository.OcrRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.OcrRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(
        reportRepositoryImpl: com.dasariravi145.agrolynch.data.repository.ReportRepositoryImpl
    ): com.dasariravi145.agrolynch.domain.repository.ReportRepository
}
