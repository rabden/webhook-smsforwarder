package com.rabden.smsforwarder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.rabden.smsforwarder.repository.SettingsRepository
import com.rabden.smsforwarder.service.SmsForwardingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsForwarderApp : Application() {
    companion object {
        const val CHANNEL_ID = "sms_forwarder_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Forwarder Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for SMS forwarding events"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}