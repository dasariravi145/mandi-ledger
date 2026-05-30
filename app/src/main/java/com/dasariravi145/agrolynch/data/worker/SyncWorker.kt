package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.domain.repository.*
import timber.log.Timber
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val farmerRepository: FarmerRepository,
    private val buyerRepository: BuyerRepository,
    private val transactionRepository: TransactionRepository,
    private val arrivalRepository: ArrivalRepository,
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
    private val marketRateRepository: MarketRateRepository,
    private val productRepository: ProductRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: Starting sync...")
        return try {
            Timber.d("SyncWorker: Syncing farmers...")
            farmerRepository.syncFarmers()
            Timber.d("SyncWorker: Syncing buyers...")
            buyerRepository.syncBuyers()
            Timber.d("SyncWorker: Syncing transactions...")
            transactionRepository.syncTransactions()
            Timber.d("SyncWorker: Syncing arrivals...")
            arrivalRepository.syncArrivals()
            Timber.d("SyncWorker: Syncing sales...")
            saleRepository.syncSales()
            Timber.d("SyncWorker: Syncing payments...")
            paymentRepository.syncPayments()
            Timber.d("SyncWorker: Syncing expenses...")
            expenseRepository.syncExpenses()
            Timber.d("SyncWorker: Syncing market rates...")
            marketRateRepository.syncRates()
            Timber.d("SyncWorker: Syncing products...")
            productRepository.syncProducts()
            
            Timber.d("SyncWorker: Sync successful")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Sync failed")
            Result.retry()
        }
    }
}
