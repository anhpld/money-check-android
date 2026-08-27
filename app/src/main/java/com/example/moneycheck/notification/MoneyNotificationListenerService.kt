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
        if (sbn.packageName !in AppSettings.get(this).enabledPackages()) return
        val captured = NotificationContentExtractor.extract(this, sbn) ?: return

        serviceScope.launch {
            val id = MoneyCheckRepository.get(this@MoneyNotificationListenerService).capture(captured)
            if (id > 0) {
                NotificationAnalysisScheduler.enqueue(this@MoneyNotificationListenerService, id)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
