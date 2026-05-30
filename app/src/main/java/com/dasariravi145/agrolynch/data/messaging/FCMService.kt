package com.dasariravi145.agrolynch.data.messaging

import com.dasariravi145.agrolynch.domain.repository.NotificationRepository
import com.dasariravi145.agrolynch.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: NotificationRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            repository.updateTokenInFirestore(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "AgroLynch Update"
        val body = message.notification?.body ?: message.data["body"] ?: "New market notification"
        val channelId = message.data["channelId"] ?: NotificationHelper.CHANNEL_DAILY_SUMMARY

        NotificationHelper(applicationContext).showSimpleNotification(
            channelId = channelId,
            id = System.currentTimeMillis().toInt(),
            title = title,
            message = body
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
