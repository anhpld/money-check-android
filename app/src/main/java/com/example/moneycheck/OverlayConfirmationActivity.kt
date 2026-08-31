package com.example.moneycheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.ui.TransactionConfirmationDialog
import com.example.moneycheck.ui.theme.MoneyCheckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayConfirmationActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val notificationId = MutableStateFlow(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateNotification(intent)
        setContent {
            val currentId by notificationId.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val item = notifications.firstOrNull { it.notification.id == currentId }

            LaunchedEffect(currentId, item?.notification?.analysisStatus) {
                if (currentId <= 0 ||
                    (item != null && item.notification.analysisStatus != AnalysisStatus.READY)
                ) finish()
                if (item?.draft != null && item.notification.analysisStatus == AnalysisStatus.READY) {
                    ConfirmationNotifier.cancel(this@OverlayConfirmationActivity, currentId)
                }
            }
            LaunchedEffect(currentId) {
                if (currentId > 0) {
                    delay(2_500)
                    if (viewModel.notifications.value.none { it.notification.id == currentId }) finish()
                }
            }

            MoneyCheckTheme {
                if (item?.draft != null && item.notification.analysisStatus == AnalysisStatus.READY) {
                    TransactionConfirmationDialog(
                        item = item,
                        onDismiss = {
                            viewModel.dismissConfirmation(item.notification.id) { finish() }
                        },
                        onConfirm = { direction, amount, recipient, purpose ->
                            viewModel.confirmTransaction(
                                notification = item.notification,
                                direction = direction,
                                amount = amount,
                                recipient = recipient,
                                purpose = purpose,
                                onComplete = { finish() },
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateNotification(intent)
    }

    private fun updateNotification(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_NOTIFICATION_ID, -1L) ?: -1L
        notificationId.value = id
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "overlay_notification_id"
    }
}
