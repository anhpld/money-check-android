package com.example.moneycheck

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneycheck.data.CapturedNotificationEntity
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.llm.NotificationAnalysisScheduler
import com.example.moneycheck.llm.OpenAiModelsClient
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class OpenAiConnectionState(
    val isChecking: Boolean = false,
    val isVerified: Boolean = false,
    val models: List<String> = emptyList(),
    val errorMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MoneyCheckRepository.get(application)
    private val appSettings = AppSettings.get(application)

    val notifications = repository.notifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val inboxNotifications = repository.inboxNotifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val savedNotifications = repository.savedNotifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val transactions = repository.transactions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _settings = MutableStateFlow(appSettings.snapshot())
    val settings = _settings.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _confirmationId = MutableStateFlow<Long?>(null)
    val confirmationId = _confirmationId.asStateFlow()

    private val _openAiConnection = MutableStateFlow(OpenAiConnectionState())
    val openAiConnection = _openAiConnection.asStateFlow()

    @Volatile
    private var verifiedApiKeyHash: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = loadLaunchableApps(application)
        }
    }

    fun requestConfirmation(notificationId: Long?) {
        _confirmationId.value = notificationId
    }

    fun testNotification(notificationId: Long) {
        viewModelScope.launch {
            val notification = withContext(Dispatchers.IO) {
                repository.getNotification(notificationId)?.notification
            }
            if (notification == null) {
                showToast("Không tìm thấy notification đã lưu")
                return@launch
            }

            val configuredTitles = appSettings.notificationRules()[notification.packageName]
            if (configuredTitles == null) {
                showToast("Ứng dụng chưa được cấu hình tự động phân tích")
                return@launch
            }
            if (!appSettings.shouldAutoAnalyze(notification.packageName, notification.title)) {
                showToast("Title notification chưa được cấu hình cho ứng dụng này")
                return@launch
            }

            val testRunId = withContext(Dispatchers.IO) {
                repository.createTestRun(notificationId)
            }
            if (testRunId == null) {
                showToast("Không thể tạo lần test mới")
                return@launch
            }
            NotificationAnalysisScheduler.enqueue(getApplication(), testRunId)
        }
    }

    fun saveNotification(notificationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.saveNotification(notificationId) }
    }

    fun addNotificationConfig(notificationId: Long) {
        viewModelScope.launch {
            val notification = withContext(Dispatchers.IO) {
                repository.getNotification(notificationId)?.notification
            }
            if (notification == null) {
                showToast("Không tìm thấy notification")
                return@launch
            }

            val packageName = notification.packageName
            val title = notification.title.trim()
            val rules = appSettings.notificationRules()
            val configuredTitles = rules[packageName]

            when {
                configuredTitles == null -> {
                    appSettings.saveNotificationRules(
                        rules + (packageName to title.takeIf(String::isNotEmpty)?.let(::setOf).orEmpty()),
                    )
                    _settings.value = appSettings.snapshot()
                    showToast(
                        if (title.isEmpty()) "Đã thêm app, áp dụng cho mọi title"
                        else "Đã thêm app và title vào cấu hình",
                    )
                }

                configuredTitles.isEmpty() -> showToast("Đã có cấu hình cho app này")

                title.isEmpty() -> {
                    appSettings.saveNotificationRules(rules + (packageName to emptySet()))
                    _settings.value = appSettings.snapshot()
                    showToast("Đã cập nhật app, áp dụng cho mọi title")
                }

                configuredTitles.any { it.trim().equals(title, ignoreCase = true) } -> {
                    showToast("Đã có cấu hình app và title này")
                }

                else -> {
                    appSettings.saveNotificationRules(rules + (packageName to (configuredTitles + title)))
                    _settings.value = appSettings.snapshot()
                    showToast("Đã thêm title vào cấu hình hiện có")
                }
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteNotification(notificationId) }
    }

    fun dismissConfirmation(notificationId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.markHandled(notificationId) }
            _confirmationId.value = null
            onComplete()
        }
    }

    fun cancelPendingConfirmation(notificationId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.cancelPendingConfirmation(notificationId) }
            ConfirmationNotifier.cancel(getApplication(), notificationId)
            if (_confirmationId.value == notificationId) _confirmationId.value = null
            showToast("Đã hủy giao dịch")
        }
    }

    fun cancelAllPendingConfirmations(notificationIds: List<Long>) {
        val ids = notificationIds.distinct()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.cancelPendingConfirmations(ids) }
            ids.forEach { id -> ConfirmationNotifier.cancel(getApplication(), id) }
            if (_confirmationId.value in ids) _confirmationId.value = null
            showToast("Đã hủy ${ids.size} giao dịch đang chờ")
        }
    }

    fun confirmTransaction(
        notification: CapturedNotificationEntity,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.confirm(
                    notification,
                    direction,
                    amount,
                    recipient,
                    purpose,
                    transactionTime,
                )
            }
            _confirmationId.value = null
            showToast("Đã lưu giao dịch thành công")
            onComplete()
        }
    }

    fun validateOpenAiKey(apiKeyInput: String) {
        val apiKey = apiKeyInput.trim().ifBlank { appSettings.apiKey().orEmpty() }
        if (apiKey.isBlank()) {
            _openAiConnection.value = OpenAiConnectionState(errorMessage = "Hãy nhập OpenAI API key")
            return
        }

        _openAiConnection.value = OpenAiConnectionState(isChecking = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { OpenAiModelsClient().validateAndList(apiKey) }
                .onSuccess { models ->
                    verifiedApiKeyHash = apiKey.sha256()
                    _openAiConnection.value = OpenAiConnectionState(
                        isVerified = true,
                        models = models,
                    )
                }
                .onFailure { error ->
                    verifiedApiKeyHash = null
                    _openAiConnection.value = OpenAiConnectionState(
                        errorMessage = error.message?.take(300) ?: "Không thể kiểm tra API key",
                    )
                }
        }
    }

    fun clearOpenAiValidation() {
        verifiedApiKeyHash = null
        _openAiConnection.value = OpenAiConnectionState()
    }

    fun ensureOpenAiModelsLoaded() {
        if (!appSettings.snapshot().hasApiKey) return
        if (_openAiConnection.value.isChecking || _openAiConnection.value.isVerified) return
        validateOpenAiKey("")
    }

    fun saveSettings(
        model: String,
        prompt: String,
        apiKey: String,
        notificationRules: Map<String, Set<String>>,
        overlayEnabled: Boolean,
    ): Boolean {
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isNotEmpty() && normalizedApiKey.sha256() != verifiedApiKeyHash) {
            _openAiConnection.value = OpenAiConnectionState(
                errorMessage = "API key đã thay đổi. Hãy kiểm tra lại trước khi lưu.",
            )
            return false
        }
        if (normalizedApiKey.isEmpty() && !appSettings.snapshot().hasApiKey) {
            _openAiConnection.value = OpenAiConnectionState(errorMessage = "Chưa có API key hợp lệ")
            return false
        }
        appSettings.saveModel(model)
        appSettings.savePrompt(prompt)
        if (normalizedApiKey.isNotEmpty()) appSettings.saveApiKey(normalizedApiKey)
        appSettings.saveNotificationRules(notificationRules)
        appSettings.saveOverlayEnabled(overlayEnabled)
        _settings.value = appSettings.snapshot()
        return true
    }

    fun clearApiKey() {
        appSettings.saveApiKey("")
        clearOpenAiValidation()
        _settings.value = appSettings.snapshot()
    }

    fun clearInbox() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearInbox() }
    }

    fun clearSavedNotifications() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearSavedNotifications() }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTransaction(id) }
    }

    fun addManualTransaction(
        appName: String,
        packageName: String,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addManualTransaction(
                    appName,
                    packageName,
                    direction,
                    amount,
                    recipient,
                    purpose,
                    transactionTime,
                )
            }
            showToast("Đã thêm giao dịch")
            onComplete()
        }
    }

    fun updateTransaction(
        id: Long,
        appName: String,
        packageName: String,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateTransaction(
                    id,
                    appName,
                    packageName,
                    direction,
                    amount,
                    recipient,
                    purpose,
                    transactionTime,
                )
            }
            showToast("Đã cập nhật giao dịch")
            onComplete()
        }
    }

    fun refreshSettings() {
        _settings.value = appSettings.snapshot()
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
