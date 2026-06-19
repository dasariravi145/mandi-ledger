package com.dasariravi145.agrolynch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dasariravi145.agrolynch.MainActivity
import com.dasariravi145.agrolynch.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_DAILY_SUMMARY = "daily_summary"
        const val CHANNEL_PAYMENT_ALERTS = "payment_alerts"
        
        const val NOTIFICATION_ID_SUMMARY = 1001
        const val NOTIFICATION_ID_PAYMENT = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val summaryChannel = NotificationChannel(
                CHANNEL_DAILY_SUMMARY,
                context.getString(R.string.daily_summary),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily business performance and expense summary"
            }

            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENT_ALERTS,
                context.getString(R.string.payment_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for new payments and receipts"
            }

            notificationManager.createNotificationChannels(
                listOf(summaryChannel, paymentChannel)
            )
        }
    }

    fun showSimpleNotification(
        channelId: String,
        id: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }
}
