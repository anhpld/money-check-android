package com.example.moneycheck.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.llm.NotificationAnalysisScheduler
import com.example.moneycheck.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MoneyNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val captured = NotificationContentExtractor.extract(this, sbn) ?: return

        serviceScope.launch {
            val repository = MoneyCheckRepository.get(this@MoneyNotificationListenerService)
            val id = repository.capture(captured)
            val shouldAutoAnalyze = id > 0 && AppSettings.get(this@MoneyNotificationListenerService).shouldAutoAnalyze(
                    packageName = sbn.packageName,
                    notificationTitle = captured.title,
                )
            if (shouldAutoAnalyze) {
                repository.markAutoMatched(id)
                NotificationAnalysisScheduler.enqueue(this@MoneyNotificationListenerService, id)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
