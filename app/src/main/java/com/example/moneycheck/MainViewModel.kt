package com.example.moneycheck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneycheck.data.CapturedNotificationEntity
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.llm.NotificationAnalysisScheduler
import com.example.moneycheck.llm.OpenAiModelsClient
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
        _confirmationId.value = notificationId
        NotificationAnalysisScheduler.enqueue(getApplication(), notificationId)
    }

    fun saveNotification(notificationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.saveNotification(notificationId) }
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

    fun confirmTransaction(
        notification: CapturedNotificationEntity,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
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
                )
            }
            _confirmationId.value = null
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

    fun refreshSettings() {
        _settings.value = appSettings.snapshot()
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
