package com.rabden.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.rabden.smsforwarder.data.AppDatabase
import com.rabden.smsforwarder.data.MessageLog
import com.rabden.smsforwarder.repository.ContactRepository
import com.rabden.smsforwarder.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class LogSmsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val contactRepository = ContactRepository(context)
    private val settingsRepository = SettingsRepository(context)
    private val database = AppDatabase.getInstance(context)

    override suspend fun doWork(): Result {
        val sender = inputData.getString(EXTRA_SENDER) ?: return Result.failure()
        val body = inputData.getString(EXTRA_BODY) ?: return Result.failure()
        val timestamp = inputData.getLong(EXTRA_TIMESTAMP, System.currentTimeMillis())
        val sim = inputData.getString(EXTRA_SIM) ?: "Primary"

        Log.d("LogSmsWorker", "Worker started for sender: $sender")

        try {
            val isEnabled = settingsRepository.isForwardingEnabled.first()
            if (!isEnabled) {
                Log.d("LogSmsWorker", "Forwarding disabled in settings")
                return Result.success()
            }

            val customContacts = contactRepository.customNumbers.first()
            
            if (!shouldForward(sender, customContacts)) {
                Log.d("LogSmsWorker", "Sender $sender not in whitelist or disabled")
                return Result.success()
            }

            val webhookUrl = settingsRepository.webhookUrl.first()
            if (webhookUrl.isBlank()) {
                Log.e("LogSmsWorker", "Webhook URL is empty")
                return Result.failure()
            }

            val deviceName = settingsRepository.deviceName.first()
            val normalizedSender = normalizePhoneNumber(sender)
            
            Log.d("LogSmsWorker", "Logging message from $normalizedSender")
            
            val logId = database.messageLogDao().insert(
                MessageLog(
                    sender = normalizedSender,
                    message = body,
                    sim = sim,
                    device = deviceName,
                    timestamp = timestamp,
                    webhookUrl = webhookUrl,
                    status = "PENDING"
                )
            )

            val outputData = Data.Builder()
                .putLong(EXTRA_LOG_ID, logId)
                .build()

            Log.d("LogSmsWorker", "Log created with ID: $logId. Passing to ForwardSmsWorker.")
            return Result.success(outputData)
        } catch (e: Exception) {
            Log.e("LogSmsWorker", "Process failed", e)
            return Result.failure()
        }
    }

    private fun shouldForward(
        sender: String, 
        customContacts: Set<String>
    ): Boolean {
        val normalizedSender = normalizePhoneNumber(sender)
        
        // Check custom whitelist
        return customContacts.any { watched ->
            normalizePhoneNumber(watched) == normalizedSender
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return ""
        
        // Handle alphanumeric sender IDs (e.g., "GOOGLE", "GP-SMS")
        if (trimmed.any { it.isLetter() }) {
            // Remove common symbols but keep letters and numbers
            return trimmed.uppercase().replace(Regex("[^A-Z0-9]"), "")
        }

        // Handle numeric phone numbers
        // 1. Remove everything except digits and the plus sign
        val clean = trimmed.replace(Regex("[^0-9+]"), "")
        
        // 2. If it's already an international format, keep as is
        if (clean.startsWith("+")) return clean
        
        // 3. Handle local formats (specifically Bangladesh as per existing logic)
        val digits = clean.replace(Regex("[^0-9]"), "")
        return when {
            // Bangladesh: 11 digits starting with 01 -> +8801...
            digits.length == 11 && digits.startsWith("01") -> "+88$digits"
            // Bangladesh: 13 digits starting with 880 -> +880...
            digits.length == 13 && digits.startsWith("880") -> "+$digits"
            // Generic: prefix with + if not present
            digits.isNotEmpty() -> "+$digits"
            else -> trimmed
        }
    }

    companion object {
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_LOG_ID = "log_id"
        const val EXTRA_SIM = "sim"
    }
}
