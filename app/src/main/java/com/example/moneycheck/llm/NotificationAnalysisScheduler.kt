package com.example.moneycheck.llm

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

object NotificationAnalysisScheduler {
    fun enqueue(context: Context, notificationId: Long): UUID {
        val request = OneTimeWorkRequestBuilder<AnalyzeNotificationWorker>()
            .setInputData(Data.Builder().putLong(AnalyzeNotificationWorker.KEY_NOTIFICATION_ID, notificationId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "analyze-notification-$notificationId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return request.id
    }
}
