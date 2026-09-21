package com.example.moneycheck

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.notification.AppVisibility
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.accessibility.ScreenTransactionCaptureService
import com.example.moneycheck.accessibility.ScreenCaptureSessionStore
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.ui.MoneyCheckApp
import com.example.moneycheck.ui.theme.MoneyCheckTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val hasListenerAccess = MutableStateFlow(false)
    private val canPostConfirmations = MutableStateFlow(false)
    private val canDrawOverlays = MutableStateFlow(false)
    private val hasScreenCaptureAccess = MutableStateFlow(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshNotificationState() }
    private val databaseExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
    ) { destination ->
        if (destination == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                contentResolver.openOutputStream(destination, "wt").use { output ->
                    checkNotNull(output) { "Không thể mở file đã chọn" }
                    MoneyCheckRepository.get(this@MainActivity).exportDatabase(output)
                }
            }
            Toast.makeText(
                this@MainActivity,
                if (result.isSuccess) "Đã xuất database" else "Không thể xuất database: ${result.exceptionOrNull()?.message.orEmpty()}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ConfirmationNotifier.ensureChannel(this)
        handleIntent(intent)
        setContent {
            val listenerAccess = hasListenerAccess.collectAsStateWithLifecycle()
            val postConfirmationAccess = canPostConfirmations.collectAsStateWithLifecycle()
            val overlayAccess = canDrawOverlays.collectAsStateWithLifecycle()
            val screenCaptureAccess = hasScreenCaptureAccess.collectAsStateWithLifecycle()
            MoneyCheckTheme {
                MoneyCheckApp(
                    viewModel = viewModel,
                    hasNotificationAccess = listenerAccess.value,
                    canPostConfirmations = postConfirmationAccess.value,
                    canDrawOverlays = overlayAccess.value,
                    hasScreenCaptureAccess = screenCaptureAccess.value,
                    onOpenNotificationAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestPostNotifications = ::requestOrOpenNotificationSettings,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings,
                    onOpenScreenCaptureAccess = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onStartScreenRead = ::startManualScreenRead,
                    onExportDatabase = {
                        databaseExportLauncher.launch("moneycheck-${LocalDate.now()}.db")
                    },
                )
            }
        }
        requestPostNotificationsOnce()
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
        refreshNotificationState()
        canDrawOverlays.value = Settings.canDrawOverlays(this)
        hasScreenCaptureAccess.value = isScreenCaptureServiceEnabled()
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

    private fun requestPostNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) return
        val preferences = getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_POST_NOTIFICATION_REQUESTED, false)) return
        preferences.edit().putBoolean(KEY_POST_NOTIFICATION_REQUESTED, true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestOrOpenNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_POST_NOTIFICATION_REQUESTED, false)
        ) {
            requestPostNotificationsOnce()
            return
        }
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
        )
    }

    private fun refreshNotificationState() {
        canPostConfirmations.value = ConfirmationNotifier.canNotify(this)
    }

    private fun openOverlayPermissionSettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun startManualScreenRead() {
        if (!isScreenCaptureServiceEnabled()) {
            Toast.makeText(this, "Hãy bật quyền Đọc màn hình giao dịch", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Hãy cấp quyền hiển thị trên ứng dụng khác", Toast.LENGTH_LONG).show()
            openOverlayPermissionSettings()
            return
        }
        val appSettings = AppSettings.get(this)
        if (appSettings.apiKey().isNullOrBlank()) {
            Toast.makeText(this, "Chưa cấu hình OpenAI API key", Toast.LENGTH_LONG).show()
            return
        }
        ScreenCaptureSessionStore(this).start()
        ScreenTransactionCaptureService.refreshManualSession()
        viewModel.refreshSettings()
        Toast.makeText(
            this,
            "Mở app cần đọc, vào chi tiết giao dịch rồi nhấn nút Lưu thủ công",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun isScreenCaptureServiceEnabled(): Boolean {
        val expected = ComponentName(this, ScreenTransactionCaptureService::class.java)
        val manager = getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expected
            }
    }

    companion object {
        const val EXTRA_CONFIRM_NOTIFICATION_ID = "confirm_notification_id"
        private const val PERMISSION_PREFS = "permission_requests"
        private const val KEY_POST_NOTIFICATION_REQUESTED = "post_notifications_requested"
    }
}
