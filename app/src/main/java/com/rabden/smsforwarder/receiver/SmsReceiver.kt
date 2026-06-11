package com.rabden.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rabden.smsforwarder.worker.ForwardSmsWorker
import com.rabden.smsforwarder.worker.LogSmsWorker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SmsReceiver", "Received intent: $action")
        
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) return

        val pendingResult = goAsync()

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.e("SmsReceiver", "No messages found")
                pendingResult.finish()
                return
            }

            val sb = StringBuilder()
            var sender: String? = null
            val timestamp: Long = System.currentTimeMillis()

            for (message in messages) {
                val msg = message.displayMessageBody ?: continue
                if (sender == null) {
                    sender = message.originatingAddress
                }
                sb.append(msg)
            }

            if (sender == null) {
                Log.e("SmsReceiver", "Sender is null")
                pendingResult.finish()
                return
            }

            val simLabel = getSimNumber(context, intent)
            
            Log.d("SmsReceiver", "Handing off to WorkManager: $sender (SIM: $simLabel)")

            val inputData = Data.Builder()
                .putString(LogSmsWorker.EXTRA_SENDER, sender)
                .putString(LogSmsWorker.EXTRA_BODY, sb.toString())
                .putLong(LogSmsWorker.EXTRA_TIMESTAMP, timestamp)
                .putString(LogSmsWorker.EXTRA_SIM, simLabel)
                .build()

            val logRequest = OneTimeWorkRequestBuilder<LogSmsWorker>()
                .setInputData(inputData)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val forwardRequest = OneTimeWorkRequestBuilder<ForwardSmsWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .beginWith(logRequest)
                .then(forwardRequest)
                .enqueue()
            
            Log.d("SmsReceiver", "Work enqueued successfully for sender: $sender")
            
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        } finally {
            pendingResult.finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun getSimNumber(context: Context, intent: Intent): String {
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
            if (sm != null) {
                // 1. Try to get by subscription ID first
                val subId = intent.getIntExtra("subscription", -1)
                if (subId != -1) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val number = sm.getPhoneNumber(subId)
                        if (!number.isNullOrBlank()) return number
                    }
                    val info = sm.getActiveSubscriptionInfo(subId)
                    if (info != null && !info.number.isNullOrBlank()) {
                        return info.number
                    }
                }

                // 2. Try to get by slot index from other common extras
                val slotIndex = when {
                    intent.hasExtra("simId") -> intent.getIntExtra("simId", -1)
                    intent.hasExtra("slot") -> intent.getIntExtra("slot", -1)
                    intent.hasExtra("phone") -> intent.getIntExtra("phone", -1)
                    else -> -1
                }
                if (slotIndex >= 0) {
                    val info = sm.getActiveSubscriptionInfoForSimSlotIndex(slotIndex)
                    if (info != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val number = sm.getPhoneNumber(info.subscriptionId)
                            if (!number.isNullOrBlank()) return number
                        }
                        if (!info.number.isNullOrBlank()) {
                            return info.number
                        }
                    }
                }
                
                // 3. Fallback to active subscriptions list if subId and slotIndex are not resolved
                val activeList = sm.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    if (activeList.size == 1) {
                        val info = activeList[0]
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val number = sm.getPhoneNumber(info.subscriptionId)
                            if (!number.isNullOrBlank()) return number
                        }
                        if (!info.number.isNullOrBlank()) {
                            return info.number
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("SmsReceiver", "SecurityException reading SIM number", e)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error reading SIM number", e)
        }
        
        // Final fallback if we couldn't resolve the number
        val subId = intent.getIntExtra("subscription", -1)
        return if (subId != -1) "SIM-$subId" else "Primary"
    }
}
