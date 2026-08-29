package com.example.moneycheck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneycheck.MainViewModel
import com.example.moneycheck.OpenAiConnectionState
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.data.NotificationWithDraft
import com.example.moneycheck.data.TransactionEntity
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.SettingsSnapshot
import org.json.JSONObject
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainTab(val label: String, val symbol: String) {
    TRANSACTIONS("Tổng quan", "₫"),
    INBOX("Hộp thư", "◉"),
    SAVED("Đã lưu", "★"),
    SETTINGS("Cài đặt", "⚙"),
}

private val IncomeStrong = Color(0xFF087A55)
private val HeroStart = Color(0xFF0C6155)
private val HeroEnd = Color(0xFF183D49)

@Composable
fun MoneyCheckApp(
    viewModel: MainViewModel,
    hasNotificationAccess: Boolean,
    canPostConfirmations: Boolean,
    canDrawOverlays: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val inboxNotifications by viewModel.inboxNotifications.collectAsStateWithLifecycle()
    val savedNotifications by viewModel.savedNotifications.collectAsStateWithLifecycle()
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
            if (current == null || current.notification.analysisStatus in
                setOf(AnalysisStatus.ERROR, AnalysisStatus.IGNORED)
            ) viewModel.requestConfirmation(null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { NavSymbol(tab.symbol, selectedTab == tab) },
                        label = { Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium) },
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

            MainTab.INBOX -> NotificationInboxScreen(
                notifications = inboxNotifications,
                hasNotificationAccess = hasNotificationAccess,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onSave = viewModel::saveNotification,
                onClear = viewModel::clearInbox,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.SAVED -> SavedNotificationScreen(
                notifications = savedNotifications,
                onTest = viewModel::testNotification,
                onDelete = viewModel::deleteNotification,
                onClear = viewModel::clearSavedNotifications,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.SETTINGS -> SettingsScreen(
                settings = settings,
                openAiConnection = openAiConnection,
                installedApps = installedApps,
                hasNotificationAccess = hasNotificationAccess,
                canPostConfirmations = canPostConfirmations,
                canDrawOverlays = canDrawOverlays,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestPostNotifications = onRequestPostNotifications,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onValidateApiKey = viewModel::validateOpenAiKey,
                onApiKeyChanged = viewModel::clearOpenAiValidation,
                onEnsureModelsLoaded = viewModel::ensureOpenAiModelsLoaded,
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
            onConfirm = { direction, amount, recipient, purpose ->
                viewModel.confirmTransaction(
                    confirmation.notification,
                    direction,
                    amount,
                    recipient,
                    purpose,
                )
            },
        )
    }
}

@Composable
private fun NavSymbol(symbol: String, selected: Boolean) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                symbol,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PageHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransactionScreen(
    transactions: List<TransactionEntity>,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val income = transactions.filter { it.direction == "income" }.sumOf { it.amount }
    val expense = transactions.filter { it.direction == "expense" }.sumOf { it.amount }
    val net = income - expense

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Money Check",
                title = "Tổng quan tài chính",
                subtitle = currentMonthLabel(),
            )
        }
        item { BalanceHero(net = net, income = income, expense = expense) }
        item {
            SectionHeader(
                title = "Giao dịch gần đây",
                trailing = "${transactions.size} giao dịch",
            )
        }
        if (transactions.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "₫",
                    title = "Chưa có giao dịch",
                    description = "Giao dịch từ notification sẽ xuất hiện sau khi bạn xác nhận.",
                )
            }
        } else {
            items(transactions, key = TransactionEntity::id) { transaction ->
                TransactionCard(transaction, onDelete)
            }
        }
    }
}

@Composable
private fun BalanceHero(net: Long, income: Long, expense: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dòng tiền ròng", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    formatMoney(net),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroMetric("↓", "Tiền vào", income, Modifier.weight(1f), Color(0xFF9BE4C8))
                HeroMetric("↑", "Tiền ra", expense, Modifier.weight(1f), Color(0xFFFFB4AB))
            }
        }
    }
}

@Composable
private fun HeroMetric(symbol: String, label: String, value: Long, modifier: Modifier, accent: Color) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(symbol, color = accent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(5.dp))
                Text(label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            }
            Text(formatMoney(value), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        trailing?.let {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Text(it, Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TransactionCard(transaction: TransactionEntity, onDelete: (Long) -> Unit) {
    val income = transaction.direction == "income"
    val accent = if (income) IncomeStrong else MaterialTheme.colorScheme.error
    val container = if (income) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    var showLlmInput by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val llmInput = remember(transaction.llmInputJson) { parseLlmInput(transaction.llmInputJson) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = container) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (income) "↓" else "↑", color = accent, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        transaction.purpose.ifBlank { "Không có nội dung" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        transaction.recipient.ifBlank { transaction.appName },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${transaction.appName}  ·  ${formatDateTime(transaction.transactionTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        (if (income) "+" else "−") + formatMoney(transaction.amount),
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    TextButton(
                        onClick = { onDelete(transaction.id) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Xóa", style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (llmInput != null) {
                TextButton(
                    onClick = { showLlmInput = !showLlmInput },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(if (showLlmInput) "Ẩn chi tiết LLM  ⌃" else "Chi tiết LLM  ⌄")
                }
                if (showLlmInput) {
                    LlmInputDetails(llmInput.title, llmInput.text, llmInput.expandedContent)
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxScreen(
    notifications: List<NotificationWithDraft>,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onSave: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Tạm thời · 24 giờ",
                title = "Hộp thư notification",
                subtitle = "Hứng notification từ mọi ứng dụng. Chỉ mục bạn bấm Lưu mới được giữ lâu dài.",
            )
        }
        if (!hasNotificationAccess) item { PermissionBanner(onOpenNotificationAccess) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mới nhận", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Text("${notifications.size} mục", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
                if (notifications.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onClear) { Text("Xóa tất cả") }
                }
            }
        }
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "◉",
                    title = "Chưa có notification",
                    description = "Notification từ mọi ứng dụng sẽ xuất hiện tại đây và tự xóa sau 24 giờ.",
                )
            }
        } else {
            items(notifications, key = { it.notification.id }) { item ->
                NotificationCard(
                    item = item,
                    actionLabel = "Lưu",
                    onAction = { onSave(item.notification.id) },
                    temporary = true,
                )
            }
        }
    }
}

@Composable
private fun SavedNotificationScreen(
    notifications: List<NotificationWithDraft>,
    onTest: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Thư viện test",
                title = "Notification đã lưu",
                subtitle = "Các mẫu được giữ trên máy để bạn chạy lại phân tích khi cần.",
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Đã lưu", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Text("${notifications.size} mục", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
                if (notifications.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onClear) { Text("Xóa tất cả") }
                }
            }
        }
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "★",
                    title = "Chưa lưu notification nào",
                    description = "Mở Hộp thư và bấm Lưu ở notification bạn muốn dùng để test lại.",
                )
            }
        } else {
            items(notifications, key = { it.notification.id }) { item ->
                NotificationCard(
                    item = item,
                    actionLabel = if (item.notification.analysisStatus == AnalysisStatus.PROCESSING) "Đang chạy" else "Test lại",
                    onAction = { onTest(item.notification.id) },
                    actionEnabled = item.notification.analysisStatus != AnalysisStatus.PROCESSING,
                    onDelete = { onDelete(item.notification.id) },
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationWithDraft,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    temporary: Boolean = false,
) {
    var showRaw by rememberSaveable(item.notification.id) { mutableStateOf(false) }
    val notification = item.notification
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(notification.appName)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(notification.appName, style = MaterialTheme.typography.titleSmall)
                    Text(notification.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusLabel(if (temporary) "temporary" else notification.analysisStatus)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(notification.title.ifBlank { "Thông báo không có tiêu đề" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    notification.expandedContent.ifBlank { notification.text }.ifBlank { "(Không có nội dung)" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (showRaw) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            notification.errorMessage?.let { InlineMessage(it, error = true) }
            if (showRaw) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(notification.rawPayload, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDateTime(notification.postedAt),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Thu gọn" else "Chi tiết") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Xóa") }
                OutlinedButton(
                    onClick = onAction,
                    enabled = actionEnabled,
                ) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun AppAvatar(name: String) {
    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercase() ?: "A", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusLabel(status: String) {
    val (text, foreground, background) = when (status) {
        "temporary" -> Triple("Còn tối đa 24h", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
        AnalysisStatus.PENDING -> Triple("Đang chờ", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        AnalysisStatus.PROCESSING -> Triple("Phân tích", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
        AnalysisStatus.READY -> Triple("Cần xác nhận", Color(0xFF8A5700), Color(0xFFFFE0A6))
        AnalysisStatus.IGNORED -> Triple("Đã bỏ qua", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        AnalysisStatus.CONFIRMED -> Triple("Đã lưu", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        AnalysisStatus.ERROR -> Triple("Có lỗi", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
        else -> Triple(status, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }
    Surface(color = background, shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = foreground, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: SettingsSnapshot,
    openAiConnection: OpenAiConnectionState,
    installedApps: List<InstalledApp>,
    hasNotificationAccess: Boolean,
    canPostConfirmations: Boolean,
    canDrawOverlays: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onValidateApiKey: (String) -> Unit,
    onApiKeyChanged: () -> Unit,
    onEnsureModelsLoaded: () -> Unit,
    onSave: (String, String, String, Map<String, Set<String>>, Boolean) -> Boolean,
    onClearApiKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var prompt by rememberSaveable { mutableStateOf(settings.prompt) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var notificationRuleInputs by remember {
        mutableStateOf(settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") })
    }
    var overlayEnabled by rememberSaveable { mutableStateOf(settings.overlayEnabled) }
    var query by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings) {
        model = settings.model
        prompt = settings.prompt
        notificationRuleInputs = settings.notificationRules
            .mapValues { (_, titles) -> titles.sorted().joinToString("\n") }
        overlayEnabled = settings.overlayEnabled
    }
    LaunchedEffect(Unit) { onEnsureModelsLoaded() }
    LaunchedEffect(openAiConnection.models) {
        if (openAiConnection.models.isNotEmpty() && model !in openAiConnection.models) model = openAiConnection.models.first()
    }

    val normalizedQuery = query.trim()
    val selectedApps = remember(installedApps, notificationRuleInputs) {
        installedApps.filter { it.packageName in notificationRuleInputs }
    }
    val filteredApps = remember(installedApps, normalizedQuery, notificationRuleInputs) {
        if (normalizedQuery.isBlank()) emptyList() else installedApps.filter {
            it.packageName !in notificationRuleInputs &&
                (it.label.contains(normalizedQuery, true) || it.packageName.contains(normalizedQuery, true))
        }
    }
    val canSave = model.isNotBlank() &&
        (settings.hasApiKey || apiKey.isNotBlank()) &&
        (apiKey.isBlank() || openAiConnection.isVerified)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        stickyHeader {
            TopAppBar(
                title = {
                    Column {
                        Text("Cài đặt", style = MaterialTheme.typography.titleLarge)
                        Text("Cấu hình cách Moneycheck hoạt động", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val rules = notificationRuleInputs.mapValues { (_, input) ->
                                input.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
                            }
                            saved = onSave(model, prompt, apiKey, rules, overlayEnabled)
                            if (saved) apiKey = ""
                        },
                        enabled = canSave,
                        modifier = Modifier.padding(end = 12.dp),
                    ) { Text("Lưu") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }

        if (saved) item { InlineMessage("Đã lưu cấu hình", modifier = Modifier.padding(horizontal = 18.dp)) }

        item {
            SettingsSection(
                title = "Quyền và hiển thị",
                subtitle = "Kiểm soát việc đọc và xác nhận notification.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                PermissionRow(
                    title = "Đọc notification",
                    description = if (hasNotificationAccess) "Đang hứng notification từ mọi ứng dụng" else "Cần cấp quyền để mở hộp thư 24 giờ",
                    granted = hasNotificationAccess,
                    actionLabel = "Thiết lập",
                    onAction = onOpenNotificationAccess,
                )
                PermissionRow(
                    title = "Notification xác nhận",
                    description = if (canPostConfirmations) "Heads-up notification đang hoạt động" else "Đang bị tắt trong Android",
                    granted = canPostConfirmations,
                    actionLabel = "Mở cài đặt",
                    onAction = onRequestPostNotifications,
                )
                SettingToggleRow(
                    title = "Popup xác nhận nổi",
                    description = "Hiện dialog trên ứng dụng đang sử dụng",
                    checked = overlayEnabled,
                    onCheckedChange = { enabled ->
                        overlayEnabled = enabled
                        saved = false
                        if (enabled && !canDrawOverlays) onRequestOverlayPermission()
                    },
                )
                if (overlayEnabled && !canDrawOverlays) {
                    InlineMessage("Cần cấp quyền “Hiển thị trên ứng dụng khác”. Notification vẫn được dùng làm dự phòng.", error = true)
                    OutlinedButton(onClick = onRequestOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("Cấp quyền popup nổi") }
                } else if (overlayEnabled) {
                    InlineMessage("Popup nổi đã sẵn sàng")
                }
            }
        }

        item {
            SettingsSection(
                title = "OpenAI",
                subtitle = "Kết nối model và điều chỉnh cách phân tích.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        saved = false
                        onApiKeyChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    placeholder = { Text(if (settings.hasApiKey) "Đã lưu •••• · để trống để giữ nguyên" else "sk-...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onValidateApiKey(apiKey) },
                        enabled = !openAiConnection.isChecking && (apiKey.isNotBlank() || settings.hasApiKey),
                    ) {
                        if (openAiConnection.isChecking) {
                            CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang kiểm tra")
                        } else Text("Kiểm tra kết nối")
                    }
                    if (settings.hasApiKey) TextButton(onClick = { onClearApiKey(); apiKey = "" }) { Text("Xóa key") }
                }
                openAiConnection.errorMessage?.let { InlineMessage(it, error = true) }
                if (openAiConnection.isVerified) {
                    InlineMessage("Kết nối thành công · ${openAiConnection.models.size} model khả dụng")
                    Text("Model đang dùng", style = MaterialTheme.typography.titleSmall)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { modelMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(model, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("⌄")
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f),
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
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Prompt phân tích") },
                    minLines = 5,
                    maxLines = 10,
                    shape = MaterialTheme.shapes.medium,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Chỉ title, text và expanded_content được gửi đi.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { prompt = AppSettings.DEFAULT_PROMPT; saved = false }) { Text("Mặc định") }
                }
            }
        }

        item {
            SettingsSection(
                title = "Ứng dụng tự động phân tích",
                subtitle = "Đã chọn ${notificationRuleInputs.size} ứng dụng. Chỉ notification khớp title mới gọi LLM và hiện dialog.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm tên app hoặc package") },
                    leadingIcon = { Text("⌕", style = MaterialTheme.typography.titleLarge) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "App không được chọn vẫn xuất hiện trong Hộp thư 24 giờ, nhưng không tự gọi OpenAI và không bật dialog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (normalizedQuery.isNotBlank() && filteredApps.isEmpty()) {
                    Text(
                        "Không tìm thấy ứng dụng phù hợp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (selectedApps.isNotEmpty()) {
            item { ListCaption("Đang tự động phân tích", modifier = Modifier.padding(horizontal = 20.dp)) }
            items(selectedApps, key = { "selected:${it.packageName}" }) { app ->
                AppSelectionRow(
                    app = app,
                    checked = true,
                    onCheckedChange = {
                        notificationRuleInputs = notificationRuleInputs - app.packageName
                        saved = false
                    },
                    titleInput = notificationRuleInputs[app.packageName].orEmpty(),
                    onTitleInputChanged = { titles ->
                        notificationRuleInputs = notificationRuleInputs + (app.packageName to titles)
                        saved = false
                    },
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        if (filteredApps.isNotEmpty()) {
            item { ListCaption("Kết quả tìm kiếm", modifier = Modifier.padding(horizontal = 20.dp)) }
        }
        items(filteredApps, key = InstalledApp::packageName) { app ->
            AppSelectionRow(
                app = app,
                checked = false,
                onCheckedChange = {
                    notificationRuleInputs = notificationRuleInputs + (app.packageName to "")
                    saved = false
                },
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(start = 13.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InlineMessage(message: String, modifier: Modifier = Modifier, error: Boolean = false) {
    val foreground = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val background = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    Surface(modifier = modifier.fillMaxWidth(), color = background, shape = MaterialTheme.shapes.small) {
        Text(message, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = foreground, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ListCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun AppSelectionRow(
    app: InstalledApp,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    titleInput: String? = null,
    onTitleInputChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(app.label)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
            }
            if (checked && titleInput != null) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = onTitleInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title được phép") },
                    placeholder = { Text("Mỗi title một dòng") },
                    supportingText = {
                        Text("Bỏ trống để tự động xử lý mọi notification của app này.")
                    },
                    minLines = 2,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(onOpenNotificationAccess: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.error) {
                Box(contentAlignment = Alignment.Center) { Text("!", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Cần quyền đọc notification", fontWeight = FontWeight.Bold)
                Text("Moneycheck chưa thể hứng notification từ các ứng dụng.", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpenNotificationAccess) { Text("Cấp quyền") }
        }
    }
}

@Composable
private fun EmptyStateCard(symbol: String, title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text(symbol, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall) }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun TransactionConfirmationDialog(
    item: NotificationWithDraft,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, String) -> Unit,
) {
    val draft = requireNotNull(item.draft)
    var direction by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.direction) }
    var amount by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.amount?.toString().orEmpty()) }
    var purpose by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.purpose) }
    var recipient by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.recipient) }
    var showLlmInput by rememberSaveable(item.notification.id, draft.analyzedAt) { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("GIAO DỊCH MỚI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Xác nhận thông tin", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppAvatar(item.notification.appName)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.notification.appName, style = MaterialTheme.typography.titleSmall)
                            Text(formatDateTime(item.notification.postedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text("Loại giao dịch", style = MaterialTheme.typography.titleSmall)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DirectionSegment("income", direction, "Tiền vào", Modifier.weight(1f)) { direction = "income" }
                        DirectionSegment("expense", direction, "Tiền ra", Modifier.weight(1f)) { direction = "expense" }
                    }
                }

                OutlinedTextField(
                    value = formatAmountInput(amount),
                    onValueChange = { input -> amount = input.filter { it in '0'..'9' }.trimStart('0').take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số tiền") },
                    suffix = { Text("đ", fontWeight = FontWeight.Bold) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amount.isNotBlank() && parsedAmount == null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Người nhận") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mục đích / nội dung") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedButton(
                    onClick = { showLlmInput = !showLlmInput },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (showLlmInput) "Ẩn chi tiết đầu vào LLM" else "Xem chi tiết đầu vào LLM",
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (showLlmInput) "⌃" else "⌄")
                }
                if (showLlmInput) {
                    LlmInputDetails(
                        title = item.notification.title,
                        text = item.notification.text,
                        expandedContent = item.notification.expandedContent,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(direction, requireNotNull(parsedAmount), recipient.trim(), purpose.trim()) },
                enabled = parsedAmount != null && parsedAmount > 0 &&
                    direction in setOf("income", "expense") && recipient.isNotBlank() && purpose.isNotBlank(),
            ) { Text("Lưu giao dịch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bỏ qua") } },
    )
}

private data class LlmInputSnapshot(
    val title: String,
    val text: String,
    val expandedContent: String,
)

private fun parseLlmInput(json: String): LlmInputSnapshot? {
    if (json.isBlank()) return null
    return runCatching {
        val value = JSONObject(json)
        LlmInputSnapshot(
            title = value.optString("title"),
            text = value.optString("text"),
            expandedContent = value.optString("expanded_content"),
        )
    }.getOrNull()
}

@Composable
private fun LlmInputDetails(title: String, text: String, expandedContent: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        SelectionContainer {
            Column(
                Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LlmInputField("title", title)
                LlmInputField("text", text)
                LlmInputField("expanded_content", expandedContent)
            }
        }
    }
}

@Composable
private fun LlmInputField(name: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            value.ifBlank { "(trống)" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DirectionSegment(
    value: String,
    selectedValue: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val selected = value == selectedValue
    val income = value == "income"
    val accent = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun formatAmountInput(digits: String): String = digits.reversed().chunked(3).joinToString(".").reversed()

private fun formatMoney(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ"

private fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

private fun currentMonthLabel(): String =
    SimpleDateFormat("'Tháng' M, yyyy", Locale.forLanguageTag("vi-VN")).format(Date())
