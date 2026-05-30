package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.work.*
import com.dasariravi145.agrolynch.data.worker.DailySummaryWorker
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val DAILY_SUMMARY_TAG = "daily_summary_work"

    fun scheduleDailySummary(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            24, TimeUnit.HOURS
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        )
        .addTag(DAILY_SUMMARY_TAG)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SUMMARY_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
