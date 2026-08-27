package com.example.moneycheck

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneycheck.notification.AppVisibility
import com.example.moneycheck.ui.MoneyCheckApp
import com.example.moneycheck.ui.theme.MoneyCheckTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val hasListenerAccess = MutableStateFlow(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val listenerAccess = hasListenerAccess.collectAsStateWithLifecycle()
            MoneyCheckTheme {
                MoneyCheckApp(
                    viewModel = viewModel,
                    hasNotificationAccess = listenerAccess.value,
                    onOpenNotificationAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
    }

    override fun onResume() {
        super.onResume()
        hasListenerAccess.value = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        viewModel.refreshSettings()
    }

    override fun onStop() {
        AppVisibility.isForeground = false
        super.onStop()
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_CONFIRM_NOTIFICATION_ID, -1L) ?: -1L
        if (id > 0) viewModel.requestConfirmation(id)
    }

    companion object {
        const val EXTRA_CONFIRM_NOTIFICATION_ID = "confirm_notification_id"
    }
}
