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

    fun dismissConfirmation(notificationId: Long) {
        _confirmationId.value = null
        viewModelScope.launch(Dispatchers.IO) { repository.markHandled(notificationId) }
    }

    fun confirmTransaction(
        notification: CapturedNotificationEntity,
        direction: String,
        amount: Long,
        currency: String,
        purpose: String,
        sender: String,
        recipient: String,
        reference: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.confirm(
                notification,
                direction,
                amount,
                currency,
                purpose,
                sender,
                recipient,
                reference,
            )
            _confirmationId.value = null
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

    fun saveSettings(model: String, apiKey: String, enabledPackages: Set<String>): Boolean {
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
        if (normalizedApiKey.isNotEmpty()) appSettings.saveApiKey(normalizedApiKey)
        appSettings.saveEnabledPackages(enabledPackages)
        _settings.value = appSettings.snapshot()
        return true
    }

    fun clearApiKey() {
        appSettings.saveApiKey("")
        clearOpenAiValidation()
        _settings.value = appSettings.snapshot()
    }

    fun clearNotifications() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearNotifications() }
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
