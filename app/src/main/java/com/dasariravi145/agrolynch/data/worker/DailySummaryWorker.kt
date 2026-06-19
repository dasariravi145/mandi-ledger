package com.dasariravi145.agrolynch.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dasariravi145.agrolynch.data.local.dao.ExpenseDao
import com.dasariravi145.agrolynch.data.local.dao.TransactionDao
import com.dasariravi145.agrolynch.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

import java.util.*
import com.dasariravi145.agrolynch.R

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val transactions = transactionDao.getAllTransactions().first()
        val expenses = expenseDao.getAllExpenses().first()

        val todaySales = transactions.filter { it.date >= todayStart }.sumOf { it.totalAmount }
        val todayExpenses = expenses.filter { it.date >= todayStart }.sumOf { it.amount }

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showSimpleNotification(
            NotificationHelper.CHANNEL_DAILY_SUMMARY,
            NotificationHelper.NOTIFICATION_ID_SUMMARY,
            applicationContext.getString(R.string.daily_business_summary),
            "Sales: ₹$todaySales | Expenses: ₹$todayExpenses"
        )

        return Result.success()
    }
}
