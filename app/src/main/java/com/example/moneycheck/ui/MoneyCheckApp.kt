package com.example.moneycheck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneycheck.MainViewModel
import com.example.moneycheck.OpenAiConnectionState
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.data.ExtractedDraftEntity
import com.example.moneycheck.data.NotificationWithDraft
import com.example.moneycheck.data.TransactionEntity
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.SettingsSnapshot
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private enum class MainTab(val label: String, val symbol: String) {
    TRANSACTIONS("Giao dịch", "₫"),
    NOTIFICATIONS("Thông báo", "N"),
    SETTINGS("Cài đặt", "⚙"),
}

@Composable
fun MoneyCheckApp(
    viewModel: MainViewModel,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val confirmationId by viewModel.confirmationId.collectAsStateWithLifecycle()
    val openAiConnection by viewModel.openAiConnection.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TRANSACTIONS) }

    LaunchedEffect(notifications, confirmationId) {
        if (confirmationId == null) {
            notifications.firstOrNull {
                it.notification.analysisStatus == AnalysisStatus.READY && !it.notification.handled
            }?.let { viewModel.requestConfirmation(it.notification.id) }
        } else {
            val current = notifications.firstOrNull { it.notification.id == confirmationId }
            if (current != null && current.notification.analysisStatus in setOf(AnalysisStatus.ERROR, AnalysisStatus.IGNORED)) {
                viewModel.requestConfirmation(null)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol, fontWeight = FontWeight.Bold) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.TRANSACTIONS -> TransactionScreen(
                transactions = transactions,
                onDelete = viewModel::deleteTransaction,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.NOTIFICATIONS -> NotificationHistoryScreen(
                notifications = notifications,
                hasNotificationAccess = hasNotificationAccess,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onTest = viewModel::testNotification,
                onClear = viewModel::clearNotifications,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.SETTINGS -> SettingsScreen(
                settings = settings,
                openAiConnection = openAiConnection,
                installedApps = installedApps,
                hasNotificationAccess = hasNotificationAccess,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestPostNotifications = onRequestPostNotifications,
                onValidateApiKey = viewModel::validateOpenAiKey,
                onApiKeyChanged = viewModel::clearOpenAiValidation,
                onSave = viewModel::saveSettings,
                onClearApiKey = viewModel::clearApiKey,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    val confirmation = notifications.firstOrNull { it.notification.id == confirmationId }
    if (confirmation?.draft != null && confirmation.notification.analysisStatus == AnalysisStatus.READY) {
        TransactionConfirmationDialog(
            item = confirmation,
            onDismiss = { viewModel.dismissConfirmation(confirmation.notification.id) },
            onConfirm = { direction, amount, currency, purpose, sender, recipient, reference ->
                viewModel.confirmTransaction(
                    confirmation.notification,
                    direction,
                    amount,
                    currency,
                    purpose,
                    sender,
                    recipient,
                    reference,
                )
            },
        )
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

@Composable
private fun TransactionScreen(
    transactions: List<TransactionEntity>,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle("Quản lý tài chính", "${transactions.size} giao dịch đã xác nhận")
        val income = transactions.filter { it.direction == "income" }.sumOf { it.amount }
        val expense = transactions.filter { it.direction == "expense" }.sumOf { it.amount }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard("Tiền vào", income, Modifier.weight(1f))
            SummaryCard("Tiền ra", expense, Modifier.weight(1f))
        }
        if (transactions.isEmpty()) {
            EmptyState("Chưa có giao dịch", "Giao dịch chỉ được lưu sau khi bạn xác nhận.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(transactions, key = TransactionEntity::id) { transaction ->
                    TransactionCard(transaction, onDelete)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Long, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionCard(transaction: TransactionEntity, onDelete: (Long) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (transaction.direction == "income") "Tiền vào" else "Tiền ra",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(formatMoney(transaction.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(transaction.purpose.ifBlank { "Không có nội dung" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val party = if (transaction.direction == "income") transaction.sender else transaction.recipient
            if (party.isNotBlank()) Text(party, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDateTime(transaction.transactionTime), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { onDelete(transaction.id) }) { Text("Xóa") }
            }
        }
    }
}

@Composable
private fun NotificationHistoryScreen(
    notifications: List<NotificationWithDraft>,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onTest: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle("Notification đã lưu", "Dùng Test để bắn lại qua OpenAI")
        if (!hasNotificationAccess) {
            PermissionBanner(onOpenNotificationAccess)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${notifications.size} notification", modifier = Modifier.weight(1f))
            if (notifications.isNotEmpty()) TextButton(onClick = onClear) { Text("Xóa tất cả") }
        }
        if (notifications.isEmpty()) {
            EmptyState("Chưa bắt được notification", "Chọn package trong Cài đặt và cấp quyền đọc thông báo.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(notifications, key = { it.notification.id }) { item ->
                    NotificationCard(item, onTest)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationWithDraft, onTest: (Long) -> Unit) {
    var showRaw by rememberSaveable(item.notification.id) { mutableStateOf(false) }
    val notification = item.notification
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(notification.appName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(notification.packageName, style = MaterialTheme.typography.labelSmall)
                }
                StatusLabel(notification.analysisStatus)
            }
            Spacer(Modifier.height(8.dp))
            Text(notification.title, fontWeight = FontWeight.SemiBold)
            Text(
                notification.expandedContent.ifBlank { notification.text }.ifBlank { "(Không có nội dung)" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (showRaw) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            notification.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (showRaw) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(notification.rawPayload, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDateTime(notification.postedAt), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Thu gọn" else "Xem gốc") }
                Button(
                    onClick = { onTest(notification.id) },
                    enabled = notification.analysisStatus != AnalysisStatus.PROCESSING,
                ) { Text("Test") }
            }
        }
    }
}

@Composable
private fun StatusLabel(status: String) {
    val text = when (status) {
        AnalysisStatus.PENDING -> "Chờ"
        AnalysisStatus.PROCESSING -> "Đang phân tích"
        AnalysisStatus.READY -> "Chờ xác nhận"
        AnalysisStatus.IGNORED -> "Không phải giao dịch"
        AnalysisStatus.CONFIRMED -> "Đã lưu"
        AnalysisStatus.ERROR -> "Lỗi"
        else -> status
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingsScreen(
    settings: SettingsSnapshot,
    openAiConnection: OpenAiConnectionState,
    installedApps: List<InstalledApp>,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onValidateApiKey: (String) -> Unit,
    onApiKeyChanged: () -> Unit,
    onSave: (String, String, Set<String>) -> Boolean,
    onClearApiKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var enabledPackages by remember { mutableStateOf(settings.enabledPackages) }
    var query by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings) {
        model = settings.model
        enabledPackages = settings.enabledPackages
    }
    LaunchedEffect(openAiConnection.models) {
        if (openAiConnection.models.isNotEmpty() && model !in openAiConnection.models) {
            model = openAiConnection.models.first()
        }
    }
    val filteredApps = remember(installedApps, query) {
        installedApps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item { ScreenTitle("Cài đặt", "OpenAI và nguồn notification") }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quyền Android", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (hasNotificationAccess) "Đã cấp quyền đọc notification" else "Chưa cấp quyền đọc notification")
                OutlinedButton(onClick = onOpenNotificationAccess) { Text("Mở cài đặt quyền đọc") }
                OutlinedButton(onClick = onRequestPostNotifications) { Text("Cho phép heads-up notification") }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("OpenAI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        saved = false
                        onApiKeyChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    placeholder = { Text(if (settings.hasApiKey) "Đã lưu •••• (để trống để giữ nguyên)" else "sk-...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onValidateApiKey(apiKey) },
                        enabled = !openAiConnection.isChecking && (apiKey.isNotBlank() || settings.hasApiKey),
                    ) {
                        if (openAiConnection.isChecking) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Đang kiểm tra")
                        } else {
                            Text("Kiểm tra API key")
                        }
                    }
                    if (settings.hasApiKey) {
                        TextButton(onClick = { onClearApiKey(); apiKey = "" }) { Text("Xóa key") }
                    }
                }
                openAiConnection.errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (openAiConnection.isVerified) {
                    Text("API key hợp lệ · ${openAiConnection.models.size} model khả dụng", color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { modelMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(model, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("▼")
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                        ) {
                            openAiConnection.models.forEach { modelId ->
                                DropdownMenuItem(
                                    text = { Text(modelId) },
                                    onClick = {
                                        model = modelId
                                        modelMenuExpanded = false
                                        saved = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    "Nội dung notification tài chính sẽ được gửi tới OpenAI khi package được chọn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("Package cần theo dõi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Đã chọn ${enabledPackages.size} app")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm app hoặc package") },
                    singleLine = true,
                )
            }
        }
        items(filteredApps, key = InstalledApp::packageName) { app ->
            val checked = app.packageName in enabledPackages
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { selected ->
                        enabledPackages = if (selected) enabledPackages + app.packageName else enabledPackages - app.packageName
                        saved = false
                    },
                )
                Column(Modifier.weight(1f)) {
                    Text(app.label, fontWeight = FontWeight.Medium)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        saved = onSave(model, apiKey, enabledPackages)
                        if (saved) apiKey = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = model.isNotBlank() &&
                        (settings.hasApiKey || apiKey.isNotBlank()) &&
                        (apiKey.isBlank() || openAiConnection.isVerified),
                ) { Text("Lưu cài đặt") }
                if (saved) Text("Đã lưu cấu hình", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PermissionBanner(onOpenNotificationAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Cần quyền đọc notification", fontWeight = FontWeight.Bold)
            Text("Money check chưa thể bắt thông báo từ các app đã chọn.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenNotificationAccess) { Text("Cấp quyền") }
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionConfirmationDialog(
    item: NotificationWithDraft,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, String, String, String, String) -> Unit,
) {
    val draft = requireNotNull(item.draft)
    var direction by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.direction) }
    var amount by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.amount?.toString().orEmpty()) }
    var currency by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.currency) }
    var purpose by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.purpose.orEmpty()) }
    var sender by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.sender.orEmpty()) }
    var recipient by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.recipient.orEmpty()) }
    var reference by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.reference.orEmpty()) }
    val parsedAmount = amount.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận giao dịch") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "OpenAI tự tin ${(draft.confidence * 100).toInt()}%. Hãy kiểm tra trước khi lưu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = direction == "income", onClick = { direction = "income" }, label = { Text("Tiền vào") })
                    FilterChip(selected = direction == "expense", onClick = { direction = "expense" }, label = { Text("Tiền ra") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amount.isNotBlank() && parsedAmount == null,
                    singleLine = true,
                )
                OutlinedTextField(currency, { currency = it }, Modifier.fillMaxWidth(), label = { Text("Tiền tệ") }, singleLine = true)
                OutlinedTextField(purpose, { purpose = it }, Modifier.fillMaxWidth(), label = { Text("Mục đích/nội dung") }, minLines = 2)
                OutlinedTextField(sender, { sender = it }, Modifier.fillMaxWidth(), label = { Text("Người gửi") })
                OutlinedTextField(recipient, { recipient = it }, Modifier.fillMaxWidth(), label = { Text("Người nhận") })
                OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text("Mã tham chiếu") })
                HorizontalDivider()
                Text("Notification gốc", fontWeight = FontWeight.SemiBold)
                Text(item.notification.expandedContent.ifBlank { item.notification.text }, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(direction, requireNotNull(parsedAmount), currency, purpose, sender, recipient, reference)
                },
                enabled = parsedAmount != null && parsedAmount > 0 && direction in setOf("income", "expense"),
            ) { Text("Lưu giao dịch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bỏ qua") } },
    )
}

private fun formatMoney(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ"

private fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
