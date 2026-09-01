package com.example.moneycheck.llm

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneycheck.accessibility.ScreenCaptureSessionStore
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.notification.AppVisibility
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.notification.OverlayConfirmationLauncher
import com.example.moneycheck.settings.AppSettings
import org.json.JSONObject
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
        val isManualScreen = ScreenCaptureSessionStore.isManualScreenEvent(notification.eventId)
        val settings = AppSettings.get(applicationContext)
        val apiKey = settings.apiKey()
        if (apiKey.isNullOrBlank()) {
            repository.saveError(notificationId, "Chưa cấu hình OpenAI API key")
            if (isManualScreen) showToast("Chưa cấu hình OpenAI API key")
            return Result.failure()
        }

        repository.markProcessing(notificationId)
        return try {
            val screenPayload = if (isManualScreen) {
                runCatching { JSONObject(notification.rawPayload) }.getOrNull()
            } else {
                null
            }
            val model = screenPayload?.optString("model")?.takeIf(String::isNotBlank) ?: settings.model()
            val draft = if (isManualScreen) {
                OpenAiClient().analyzeScreen(
                    notification = notification,
                    apiKey = apiKey,
                    model = model,
                    prompt = screenPayload?.optString("prompt")?.takeIf(String::isNotBlank)
                        ?: settings.screenPrompt(notification.packageName),
                    cleanedXml = notification.expandedContent,
                )
            } else {
                OpenAiClient().analyze(
                    notification = notification,
                    apiKey = apiKey,
                    model = model,
                    prompt = settings.prompt(),
                )
            }
            repository.saveAnalysis(notificationId, draft)
            if (isManualScreen || !AppVisibility.isForeground) {
                ConfirmationNotifier.show(
                    applicationContext,
                    notificationId,
                    draft.direction,
                    draft.amount,
                    draft.purpose,
                )
                if (isManualScreen || settings.overlayEnabled()) {
                    OverlayConfirmationLauncher.show(applicationContext, notificationId)
                }
            }
            Result.success()
        } catch (error: Exception) {
            val message = error.message?.take(300) ?: "Không thể phân tích dữ liệu"
            repository.saveError(notificationId, message)
            if (isManualScreen) showToast("Không thể phân tích màn hình: $message")
            if (error is IOException && runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun showToast(message: String) {
        applicationContext.mainExecutor.execute {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
