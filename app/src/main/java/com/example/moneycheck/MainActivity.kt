package com.example.moneycheck

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshNotificationState() }
    private val databaseExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { destination ->
        if (destination == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                contentResolver.openOutputStream(destination, "wt").use { output ->
                    checkNotNull(output) { "Không thể mở file đã chọn" }
                    MoneyCheckRepository.get(this@MainActivity).exportBackup(output)
                }
            }
            Toast.makeText(
                this@MainActivity,
                if (result.isSuccess) "Đã xuất gói sao lưu thành công (.zip)" else "Không thể xuất sao lưu: ${result.exceptionOrNull()?.message.orEmpty()}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    private val databaseImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { sourceUri ->
        if (sourceUri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                contentResolver.openInputStream(sourceUri).use { input ->
                    checkNotNull(input) { "Không thể mở file đã chọn" }
                    MoneyCheckRepository.get(this@MainActivity).importBackup(input)
                }
            }
            Toast.makeText(
                this@MainActivity,
                if (result.isSuccess) result.getOrNull().orEmpty() else "Không thể nhập sao lưu: ${result.exceptionOrNull()?.message.orEmpty()}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        ConfirmationNotifier.ensureChannel(this)
        handleIntent(intent)
        setContent {
            val listenerAccess = hasListenerAccess.collectAsStateWithLifecycle()
            val postConfirmationAccess = canPostConfirmations.collectAsStateWithLifecycle()
            val overlayAccess = canDrawOverlays.collectAsStateWithLifecycle()
            MoneyCheckTheme {
                MoneyCheckApp(
                    viewModel = viewModel,
                    hasNotificationAccess = listenerAccess.value,
                    canPostConfirmations = postConfirmationAccess.value,
                    canDrawOverlays = overlayAccess.value,
                    onOpenNotificationAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestPostNotifications = ::requestOrOpenNotificationSettings,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings,
                    onExportDatabase = {
                        databaseExportLauncher.launch("moneycheck-backup-${LocalDate.now()}.zip")
                    },
                    onImportDatabase = {
                        databaseImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/vnd.sqlite3", "*/*"))
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

    companion object {
        const val EXTRA_CONFIRM_NOTIFICATION_ID = "confirm_notification_id"
        private const val PERMISSION_PREFS = "permission_requests"
        private const val KEY_POST_NOTIFICATION_REQUESTED = "post_notifications_requested"
    }
}
