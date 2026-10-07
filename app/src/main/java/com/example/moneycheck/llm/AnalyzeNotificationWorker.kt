package com.example.moneycheck.llm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.notification.AppVisibility
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.notification.OverlayConfirmationLauncher
import com.example.moneycheck.settings.AppSettings
import java.io.IOException

class AnalyzeNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val notificationId = inputData.getLong(KEY_NOTIFICATION_ID, -1L)
        if (notificationId <= 0) return Result.failure()

        val repository = MoneyCheckRepository.get(applicationContext)
        val notification = repository.getNotification(notificationId)?.notification ?: return Result.failure()
        val settings = AppSettings.get(applicationContext)
        val apiKey = settings.apiKey()
        if (apiKey.isNullOrBlank()) {
            repository.saveError(notificationId, "Chưa cấu hình API key")
            return Result.failure()
        }

        repository.markProcessing(notificationId)
        return try {
            val draft = OpenAiClient().analyze(
                notification = notification,
                apiBaseUrl = settings.apiBaseUrl(),
                apiKey = apiKey,
                model = settings.model(),
                prompt = settings.prompt(),
            )
            repository.saveAnalysis(notificationId, draft)
            repository.removeAutoMatched(notificationId)
            if (!AppVisibility.isForeground) {
                ConfirmationNotifier.show(
                    applicationContext,
                    notificationId,
                    draft.direction,
                    draft.amount,
                    draft.purpose,
                )
                if (settings.overlayEnabled()) {
                    OverlayConfirmationLauncher.show(applicationContext, notificationId)
                }
            }
            Result.success()
        } catch (error: Exception) {
            val message = error.message?.take(300) ?: "Không thể phân tích dữ liệu"
            repository.saveError(notificationId, message)
            if (error is IOException && runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
