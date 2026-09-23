package com.example.moneycheck

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.data.CapturedNotificationEntity
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.llm.NotificationAnalysisScheduler
import com.example.moneycheck.llm.OpenAiClient
import com.example.moneycheck.llm.OpenAiCompatibleEndpoint
import com.example.moneycheck.llm.OpenAiModelsClient
import com.example.moneycheck.llm.TransactionChatClient
import com.example.moneycheck.notification.ConfirmationNotifier
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.loadLaunchableApps
import com.example.moneycheck.ui.TransactionInitialDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest

data class OpenAiConnectionState(
    val isChecking: Boolean = false,
    val isVerified: Boolean = false,
    val models: List<String> = emptyList(),
    val errorMessage: String? = null,
)

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val selectedModel: String = "",
)

data class RetestState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
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
    val autoMatchedNotifications = repository.autoMatchedNotifications.stateIn(
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

    private val _chatState = MutableStateFlow(ChatState(selectedModel = appSettings.chatModel()))
    val chatState = _chatState.asStateFlow()

    private val _isAnalyzingImage = MutableStateFlow(false)
    val isAnalyzingImage = _isAnalyzingImage.asStateFlow()

    private val _retestStates = MutableStateFlow<Map<Long, RetestState>>(emptyMap())
    val retestStates = _retestStates.asStateFlow()

    @Volatile
    private var verifiedConnectionHash: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = loadLaunchableApps(application)
        }
    }

    fun requestConfirmation(notificationId: Long?) {
        _confirmationId.value = notificationId
    }

    fun testNotification(notificationId: Long) {
        if (_retestStates.value[notificationId]?.isLoading == true) return
        updateRetestState(notificationId, RetestState(isLoading = true))
        viewModelScope.launch {
            try {
                val notification = withContext(Dispatchers.IO) {
                    repository.getNotification(notificationId)?.notification
                }
                if (notification == null) {
                    updateRetestState(notificationId, RetestState(message = "Không tìm thấy notification đã lưu", isError = true))
                    return@launch
                }

                val configuredTitles = appSettings.notificationRules()[notification.packageName]
                if (configuredTitles == null) {
                    updateRetestState(notificationId, RetestState(message = "Ứng dụng chưa được cấu hình tự động phân tích", isError = true))
                    return@launch
                }
                if (!appSettings.shouldAutoAnalyze(notification.packageName, notification.title)) {
                    updateRetestState(notificationId, RetestState(message = "Title notification chưa được cấu hình cho ứng dụng này", isError = true))
                    return@launch
                }

                val testRunId = withContext(Dispatchers.IO) {
                    repository.createTestRun(notificationId)
                }
                if (testRunId == null) {
                    updateRetestState(notificationId, RetestState(message = "Không thể tạo lần test mới", isError = true))
                    return@launch
                }
                val workId = NotificationAnalysisScheduler.enqueue(getApplication(), testRunId)
                withContext(Dispatchers.IO) {
                    val workManager = WorkManager.getInstance(getApplication())
                    while (true) {
                        val workInfo = workManager.getWorkInfoById(workId).get()
                        if (workInfo?.state?.isFinished == true) break
                        delay(250)
                    }
                    val result = repository.getNotification(testRunId)
                    if (result?.notification?.analysisStatus == AnalysisStatus.READY && result.draft != null) {
                        updateRetestState(notificationId, RetestState(message = "Phân tích thành công"))
                    } else {
                        updateRetestState(
                            notificationId,
                            RetestState(
                                message = result?.notification?.errorMessage ?: "Không thể phân tích notification",
                                isError = true,
                            ),
                        )
                    }
                }
            } catch (error: Exception) {
                updateRetestState(
                    notificationId,
                    RetestState(
                        message = error.message?.take(300) ?: "Không thể chạy lại notification",
                        isError = true,
                    ),
                )
            }
        }
    }

    private fun updateRetestState(notificationId: Long, state: RetestState) {
        _retestStates.value = _retestStates.value + (notificationId to state)
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

    fun validateOpenAiKey(apiBaseUrlInput: String, apiKeyInput: String) {
        val apiBaseUrl = runCatching { OpenAiCompatibleEndpoint.normalize(apiBaseUrlInput) }
            .getOrElse {
                _openAiConnection.value = OpenAiConnectionState(errorMessage = it.message)
                return
            }
        val apiKey = apiKeyInput.trim().ifBlank { appSettings.apiKey().orEmpty() }
        if (apiKey.isBlank()) {
            _openAiConnection.value = OpenAiConnectionState(errorMessage = "Hãy nhập API key")
            return
        }

        _openAiConnection.value = OpenAiConnectionState(isChecking = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { OpenAiModelsClient().validateAndList(apiBaseUrl, apiKey) }
                .onSuccess { models ->
                    verifiedConnectionHash = connectionHash(apiBaseUrl, apiKey)
                    _openAiConnection.value = OpenAiConnectionState(
                        isVerified = true,
                        models = models,
                    )
                }
                .onFailure { error ->
                    verifiedConnectionHash = null
                    _openAiConnection.value = OpenAiConnectionState(
                        errorMessage = error.message?.take(300) ?: "Không thể kiểm tra API key",
                    )
                }
        }
    }

    fun clearOpenAiValidation() {
        verifiedConnectionHash = null
        _openAiConnection.value = OpenAiConnectionState()
    }

    fun ensureOpenAiModelsLoaded() {
        if (!appSettings.snapshot().hasApiKey) return
        if (_openAiConnection.value.isChecking || _openAiConnection.value.isVerified) return
        validateOpenAiKey(appSettings.apiBaseUrl(), "")
    }

    fun saveLocalPreferences(
        prompt: String,
        notificationRules: Map<String, Set<String>>,
        screenPrompts: Map<String, String>,
        overlayEnabled: Boolean,
    ) {
        appSettings.savePrompt(prompt)
        appSettings.saveNotificationRules(notificationRules)
        appSettings.saveScreenPrompts(screenPrompts)
        appSettings.saveOverlayEnabled(overlayEnabled)
        _settings.value = appSettings.snapshot()
    }

    fun saveAiConnection(
        apiBaseUrl: String,
        model: String,
        apiKey: String,
    ): Boolean {
        val normalizedApiBaseUrl = runCatching { OpenAiCompatibleEndpoint.normalize(apiBaseUrl) }
            .getOrElse {
                _openAiConnection.value = OpenAiConnectionState(errorMessage = it.message)
                return false
            }
        val normalizedApiKey = apiKey.trim()
        val effectiveApiKey = normalizedApiKey.ifBlank { appSettings.apiKey().orEmpty() }
        val connectionChanged = normalizedApiKey.isNotEmpty() || normalizedApiBaseUrl != appSettings.apiBaseUrl()
        if (connectionChanged && connectionHash(normalizedApiBaseUrl, effectiveApiKey) != verifiedConnectionHash) {
            _openAiConnection.value = OpenAiConnectionState(
                errorMessage = "URL hoặc API key đã thay đổi. Hãy kiểm tra kết nối trước khi lưu.",
            )
            return false
        }
        if (normalizedApiKey.isEmpty() && !appSettings.snapshot().hasApiKey) {
            _openAiConnection.value = OpenAiConnectionState(errorMessage = "Chưa có API key hợp lệ")
            return false
        }
        appSettings.saveApiBaseUrl(normalizedApiBaseUrl)
        appSettings.saveModel(model)
        if (normalizedApiKey.isNotEmpty()) appSettings.saveApiKey(normalizedApiKey)
        _settings.value = appSettings.snapshot()
        return true
    }

    fun clearApiKey() {
        appSettings.saveApiKey("")
        clearOpenAiValidation()
        _settings.value = appSettings.snapshot()
    }

    fun selectChatModel(model: String) {
        val trimmed = model.trim()
        if (trimmed.isNotBlank()) {
            appSettings.saveChatModel(trimmed)
            _chatState.value = _chatState.value.copy(selectedModel = trimmed)
        }
    }

    fun sendChatMessage(input: String) {
        val question = input.trim()
        if (question.isEmpty() || _chatState.value.isSending) return

        val apiKey = appSettings.apiKey().orEmpty()
        if (apiKey.isBlank()) {
            _chatState.value = _chatState.value.copy(errorMessage = "Hãy cấu hình API URL và key trong Cài đặt")
            return
        }

        val currentModel = _chatState.value.selectedModel.ifBlank { appSettings.model() }
        val userMessage = ChatMessage(
            id = System.nanoTime(),
            role = "user",
            content = question,
        )
        val conversation = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(messages = conversation, isSending = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            val assistantMessageId = System.nanoTime()
            val streamedAnswer = StringBuilder()
            var lastUiUpdateNanos = 0L
            runCatching {
                TransactionChatClient().chat(
                    apiBaseUrl = appSettings.apiBaseUrl(),
                    apiKey = apiKey,
                    model = currentModel,
                    transactions = transactions.value,
                    conversation = conversation,
                    onDelta = { delta ->
                        streamedAnswer.append(delta)
                        val now = System.nanoTime()
                        if (now - lastUiUpdateNanos >= CHAT_STREAM_UI_INTERVAL_NANOS) {
                            lastUiUpdateNanos = now
                            _chatState.value = _chatState.value.copy(
                                messages = conversation + ChatMessage(
                                    id = assistantMessageId,
                                    role = "assistant",
                                    content = streamedAnswer.toString(),
                                ),
                                isSending = true,
                            )
                        }
                    },
                )
            }.onSuccess { answer ->
                _chatState.value = _chatState.value.copy(
                    messages = conversation + ChatMessage(
                        id = assistantMessageId,
                        role = "assistant",
                        content = answer,
                    ),
                    isSending = false,
                )
            }.onFailure { error ->
                _chatState.value = _chatState.value.copy(
                    messages = if (streamedAnswer.isEmpty()) conversation else {
                        conversation + ChatMessage(
                            id = assistantMessageId,
                            role = "assistant",
                            content = streamedAnswer.toString(),
                        )
                    },
                    isSending = false,
                    errorMessage = error.message?.take(300) ?: "Không thể gửi câu hỏi",
                )
            }
        }
    }

    fun clearChat() {
        if (!_chatState.value.isSending) _chatState.value = ChatState(selectedModel = _chatState.value.selectedModel)
    }

    fun clearInbox() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearInbox() }
    }

    fun clearSavedNotifications() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearSavedNotifications() }
    }

    fun removeAutoMatchedNotification(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeAutoMatched(id) }
    }

    fun clearAutoMatchedNotifications() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearAutoMatched() }
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
        llmInputJson: String = "",
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
                    llmInputJson,
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

    fun analyzeTransactionImageBytes(
        imageBytes: ByteArray,
        onSuccess: (TransactionInitialDraft) -> Unit,
    ) {
        if (_isAnalyzingImage.value) return
        val apiKey = appSettings.apiKey().orEmpty()
        if (apiKey.isBlank()) {
            showToast("Hãy cấu hình API URL và key trong Cài đặt")
            return
        }

        _isAnalyzingImage.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val (base64Image, mimeType) = compressImageBytes(imageBytes)
                val visionResult = OpenAiClient().analyzeImage(
                    base64Image = base64Image,
                    mimeType = mimeType,
                    apiBaseUrl = appSettings.apiBaseUrl(),
                    apiKey = apiKey,
                    model = appSettings.model(),
                )

                val candidateAppName = visionResult.appName.trim()
                val matchedApp = if (candidateAppName.isNotEmpty()) {
                    _installedApps.value.firstOrNull { app ->
                        app.label.contains(candidateAppName, ignoreCase = true) ||
                            candidateAppName.contains(app.label, ignoreCase = true)
                    }
                } else null

                TransactionInitialDraft(
                    direction = if (visionResult.direction in setOf("income", "expense")) visionResult.direction else "expense",
                    amount = visionResult.amount,
                    recipient = visionResult.recipient,
                    purpose = visionResult.purpose,
                    appName = matchedApp?.label ?: candidateAppName.ifBlank { "Tiền mặt" },
                    packageName = matchedApp?.packageName.orEmpty(),
                    transactionTime = visionResult.transactionTime ?: System.currentTimeMillis(),
                    rawModelJson = visionResult.rawModelJson,
                )
            }.onSuccess { draft ->
                withContext(Dispatchers.Main) {
                    _isAnalyzingImage.value = false
                    onSuccess(draft)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _isAnalyzingImage.value = false
                    showToast(error.message?.take(300) ?: "Không thể đọc thông tin giao dịch từ ảnh")
                }
            }
        }
    }

    private fun compressImageBytes(
        imageBytes: ByteArray,
    ): Pair<String, String> {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            throw IOException("Tệp được chọn không phải là hình ảnh hợp lệ")
        }

        val maxDimension = 1600
        var sampleSize = 1
        val (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
            ?: throw IOException("Không thể giải mã hình ảnh")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return base64 to "image/jpeg"
    }

    fun refreshSettings() {
        _settings.value = appSettings.snapshot()
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun connectionHash(apiBaseUrl: String, apiKey: String): String =
        "$apiBaseUrl\n$apiKey".sha256()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        private const val CHAT_STREAM_UI_INTERVAL_NANOS = 50_000_000L
    }
}
