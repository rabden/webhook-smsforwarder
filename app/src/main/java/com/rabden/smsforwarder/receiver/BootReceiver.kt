package com.rabden.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rabden.smsforwarder.repository.SettingsRepository
import com.rabden.smsforwarder.service.SmsForwardingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Checking service status.")
            val settingsRepository = SettingsRepository(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = settingsRepository.isForwardingEnabled.first()
                val isHighRel = settingsRepository.isHighReliabilityMode.first() ?: com.rabden.smsforwarder.util.BrandHelper.isAggressiveBrand()
                
                if (isEnabled && isHighRel) {
                    Log.d("BootReceiver", "Restarting Foreground Service")
                    SmsForwardingService.start(context)
                }
            }
        }
    }
}
