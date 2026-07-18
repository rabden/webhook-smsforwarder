package com.rabden.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rabden.smsforwarder.data.AppDatabase
import com.rabden.smsforwarder.network.WebhookPayload
import com.rabden.smsforwarder.network.WebhookService
import com.rabden.smsforwarder.repository.SettingsRepository
import com.rabden.smsforwarder.util.parseHeaders
import kotlinx.coroutines.flow.first

class ForwardSmsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val settingsRepository = SettingsRepository(context)
    private val webhookService = WebhookService.create()
    private val database = AppDatabase.getInstance(context)

    override suspend fun doWork(): Result {
        val logId = inputData.getLong(LogSmsWorker.EXTRA_LOG_ID, -1L)
        if (logId == -1L) {
            Log.e("ForwardSmsWorker", "Missing log ID")
            return Result.failure()
        }

        Log.d("ForwardSmsWorker", "Forwarding log ID: $logId (Attempt $runAttemptCount)")

        val logEntry = database.messageLogDao().getLogById(logId) ?: run {
            Log.e("ForwardSmsWorker", "Log entry not found for ID: $logId")
            return Result.failure()
        }
        
        if (logEntry.status == "SUCCESS") {
            Log.d("ForwardSmsWorker", "Already forwarded successfully")
            return Result.success()
        }

        try {
            val payload = WebhookPayload(
                sender = logEntry.sender,
                message = logEntry.message,
                sim = logEntry.sim,
                device = logEntry.device
            )

            val customHeadersJson = settingsRepository.customHeaders.first()
            val headers = parseHeaders(customHeadersJson)

            Log.d("ForwardSmsWorker", "Posting to: ${logEntry.webhookUrl}")
            val response = try {
                webhookService.postRaw(logEntry.webhookUrl, payload, headers)
            } catch (e: Exception) {
                Log.e("ForwardSmsWorker", "Network exception", e)
                null
            }

            if (response?.isSuccessful == true) {
                Log.d("ForwardSmsWorker", "Webhook delivery successful")
                database.messageLogDao().updateStatus(logId, "SUCCESS")
                settingsRepository.updateLastForwardedTime(logEntry.timestamp)
                return Result.success()
            } else {
                val code = response?.code()
                Log.e("ForwardSmsWorker", "Delivery failed with code: $code")
                
                if (runAttemptCount < 3) {
                    Log.d("ForwardSmsWorker", "Retrying...")
                    return Result.retry()
                } else {
                    Log.e("ForwardSmsWorker", "Max retries reached")
                    database.messageLogDao().updateStatus(logId, "FAILED")
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("ForwardSmsWorker", "Fatal error", e)
            return Result.failure()
        }
    }

}
