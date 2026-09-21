package com.example.moneycheck.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.drawable.toBitmap
import com.example.moneycheck.MainViewModel
import com.example.moneycheck.OpenAiConnectionState
import com.example.moneycheck.accessibility.ScreenCaptureSessionStore
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.data.NotificationWithDraft
import com.example.moneycheck.data.TransactionEntity
import com.example.moneycheck.data.TransactionSource
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.SettingsSnapshot
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TRANSACTIONS("Tổng quan", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PENDING("Xác nhận", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    INBOX("Hộp thư", Icons.Filled.Inbox, Icons.Outlined.Inbox),
    SAVED("Đã lưu", Icons.Filled.Star, Icons.Outlined.StarBorder),
    SETTINGS("Cài đặt", Icons.Filled.Settings, Icons.Outlined.Settings),
}

private val IncomeStrong = Color(0xFF087A55)
private val AppIconCache = object : LruCache<String, ImageBitmap>(64) {}
private val KnownScreenPromptPackages = listOf("com.shopee.vn", "vn.com.vng.zalopay")

@Composable
fun MoneyCheckApp(
    viewModel: MainViewModel,
    hasNotificationAccess: Boolean,
    canPostConfirmations: Boolean,
    canDrawOverlays: Boolean,
    hasScreenCaptureAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenScreenCaptureAccess: () -> Unit,
    onStartScreenRead: () -> Unit,
    onExportDatabase: () -> Unit,
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
    val pendingConfirmations = remember(notifications) {
        notifications.filter {
            it.notification.analysisStatus == AnalysisStatus.READY && it.draft != null
        }
    }

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
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.TRANSACTIONS -> TransactionScreen(
                transactions = transactions,
                installedApps = installedApps,
                onDelete = viewModel::deleteTransaction,
                onAdd = viewModel::addManualTransaction,
                onUpdate = viewModel::updateTransaction,
                onStartScreenRead = onStartScreenRead,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.PENDING -> PendingConfirmationScreen(
                notifications = pendingConfirmations,
                onReview = viewModel::requestConfirmation,
                onCancel = viewModel::cancelPendingConfirmation,
                onCancelAll = viewModel::cancelAllPendingConfirmations,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.INBOX -> NotificationInboxScreen(
                notifications = inboxNotifications,
                hasNotificationAccess = hasNotificationAccess,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onSave = viewModel::saveNotification,
                onAddConfig = viewModel::addNotificationConfig,
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
                hasScreenCaptureAccess = hasScreenCaptureAccess,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestPostNotifications = onRequestPostNotifications,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onOpenScreenCaptureAccess = onOpenScreenCaptureAccess,
                onValidateApiKey = viewModel::validateOpenAiKey,
                onApiKeyChanged = viewModel::clearOpenAiValidation,
                onEnsureModelsLoaded = viewModel::ensureOpenAiModelsLoaded,
                onSave = viewModel::saveSettings,
                onClearApiKey = viewModel::clearApiKey,
                onExportDatabase = onExportDatabase,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    val confirmation = notifications.firstOrNull { it.notification.id == confirmationId }
    if (confirmation?.draft != null && confirmation.notification.analysisStatus == AnalysisStatus.READY) {
        TransactionConfirmationDialog(
            item = confirmation,
            onDismiss = { viewModel.dismissConfirmation(confirmation.notification.id) },
            onConfirm = { direction, amount, recipient, purpose, transactionTime ->
                viewModel.confirmTransaction(
                    confirmation.notification,
                    direction,
                    amount,
                    recipient,
                    purpose,
                    transactionTime,
                )
            },
        )
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrowPill: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (eyebrowPill) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                Text(
                    eyebrow.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransactionScreen(
    transactions: List<TransactionEntity>,
    installedApps: List<InstalledApp>,
    onDelete: (Long) -> Unit,
    onAdd: (String, String, String, Long, String, String, Long, () -> Unit) -> Unit,
    onUpdate: (Long, String, String, String, Long, String, String, Long, () -> Unit) -> Unit,
    onStartScreenRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterStartEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterEndEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDateFilter by rememberSaveable { mutableStateOf(false) }
    var showTransactionEditor by rememberSaveable { mutableStateOf(false) }
    var showAddMethodPicker by rememberSaveable { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    val filteredTransactions = remember(transactions, filterStartEpochDay, filterEndEpochDay) {
        if (filterStartEpochDay == null || filterEndEpochDay == null) transactions else {
            transactions.filter { transaction ->
                transaction.transactionTime.toLocalDate().toEpochDay() in
                    requireNotNull(filterStartEpochDay)..requireNotNull(filterEndEpochDay)
            }
        }
    }
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { it.transactionTime.toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
    }
    val income = filteredTransactions.filter { it.direction == "income" }.sumOf { it.amount }
    val expense = filteredTransactions.filter { it.direction == "expense" }.sumOf { it.amount }
    val net = income - expense
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                PageHeader(
                    eyebrow = "Moneycheck",
                    title = "Tổng quan tài chính",
                    subtitle = if (filterStartEpochDay != null && filterEndEpochDay != null) {
                        "${formatEpochDay(requireNotNull(filterStartEpochDay))} – ${formatEpochDay(requireNotNull(filterEndEpochDay))}"
                    } else {
                        "Tất cả thời gian"
                    },
                    modifier = Modifier.weight(1f),
                    eyebrowPill = false,
                )
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    IconButton(onClick = {
                        transactionToEdit = null
                        showAddMethodPicker = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Thêm giao dịch", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        item {
            BalanceHero(
                net = net,
                income = income,
                expense = expense,
                periodLabel = if (filterStartEpochDay == null) "Tất cả" else "Đã lọc",
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Giao dịch gần đây",
                    trailing = "${filteredTransactions.size} giao dịch",
                    subtitle = if (filterStartEpochDay == null) "Các khoản đã được xác nhận" else "Trong khoảng ngày đã chọn",
                )
                DateFilterCard(
                    startEpochDay = filterStartEpochDay,
                    endEpochDay = filterEndEpochDay,
                    resultCount = filteredTransactions.size,
                    onOpen = { showDateFilter = true },
                    onClear = {
                        filterStartEpochDay = null
                        filterEndEpochDay = null
                    },
                )
            }
        }
        if (filteredTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "₫",
                    title = if (transactions.isEmpty()) "Chưa có giao dịch" else "Không có giao dịch trong khoảng này",
                    description = if (transactions.isEmpty()) {
                        "Thêm thủ công hoặc xác nhận một giao dịch từ notification."
                    } else {
                        "Hãy chọn một khoảng ngày khác, tối đa 3 tháng."
                    },
                )
            }
        } else {
            groupedTransactions.forEach { (date, dayTransactions) ->
                item(key = "day:${date.toEpochDay()}") {
                    TransactionDayHeader(date = date, transactions = dayTransactions)
                }
                items(dayTransactions, key = TransactionEntity::id) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onEdit = {
                            transactionToEdit = transaction
                            showTransactionEditor = true
                        },
                        onDelete = { transactionToDelete = transaction },
                    )
                }
            }
        }
    }

    transactionToDelete?.let { transaction ->
        DeleteConfirmationDialog(
            title = "Xóa giao dịch?",
            message = "Giao dịch “${transaction.purpose.ifBlank { "Không có nội dung" }}” sẽ bị xóa khỏi Tổng quan.",
            onDismiss = { transactionToDelete = null },
            onConfirm = {
                onDelete(transaction.id)
                transactionToDelete = null
            },
        )
    }

    if (showDateFilter) {
        TransactionDateRangeDialog(
            initialStartEpochDay = filterStartEpochDay,
            initialEndEpochDay = filterEndEpochDay,
            onDismiss = { showDateFilter = false },
            onApply = { start, end ->
                filterStartEpochDay = start
                filterEndEpochDay = end
                showDateFilter = false
            },
        )
    }

    if (showTransactionEditor) {
        TransactionEditorSheet(
            transaction = transactionToEdit,
            installedApps = installedApps,
            onDismiss = { showTransactionEditor = false },
            onSave = { appName, packageName, direction, amount, recipient, purpose, transactionTime ->
                val editing = transactionToEdit
                if (editing == null) {
                    onAdd(appName, packageName, direction, amount, recipient, purpose, transactionTime) {
                        showTransactionEditor = false
                    }
                } else {
                    onUpdate(editing.id, appName, packageName, direction, amount, recipient, purpose, transactionTime) {
                        showTransactionEditor = false
                    }
                }
            },
        )
    }

    if (showAddMethodPicker) {
        ManualAddMethodDialog(
            onDismiss = { showAddMethodPicker = false },
            onManualInput = {
                showAddMethodPicker = false
                transactionToEdit = null
                showTransactionEditor = true
            },
            onReadScreen = {
                showAddMethodPicker = false
                onStartScreenRead()
            },
        )
    }
}

@Composable
private fun ManualAddMethodDialog(
    onDismiss: () -> Unit,
    onManualInput: () -> Unit,
    onReadScreen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm giao dịch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onManualInput),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nhập thủ công", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Chọn ứng dụng và tự nhập thông tin giao dịch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onReadScreen),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Đọc từ màn hình", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tự nhận diện ứng dụng và điền thông tin từ nội dung đang mở.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenReadSetupSheet(
    installedApps: List<InstalledApp>,
    savedPrompts: Map<String, String>,
    onDismiss: () -> Unit,
    onStart: (InstalledApp, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var prompt by rememberSaveable { mutableStateOf("") }
    val results = remember(installedApps, query, selectedApp) {
        val normalized = query.trim()
        if (normalized.isBlank()) emptyList() else installedApps.asSequence()
            .filter { it.packageName != selectedApp?.packageName }
            .filter { it.label.contains(normalized, true) || it.packageName.contains(normalized, true) }
            .take(8)
            .toList()
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Đọc giao dịch từ màn hình", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Chọn app và kiểm tra prompt trước khi bắt đầu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                selectedApp?.let { app ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppAvatar(app.label, app.packageName, size = 42)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {
                                selectedApp = null
                                prompt = ""
                                query = ""
                            }) { Text("Đổi") }
                        }
                    }
                }

                if (selectedApp == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tìm ứng dụng") },
                        placeholder = { Text("Tên app hoặc package") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                    )
                    when {
                        query.isBlank() -> Text(
                            "Nhập tên ứng dụng để bắt đầu tìm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        results.isEmpty() -> InlineMessage("Không tìm thấy ứng dụng phù hợp", error = true)
                        else -> results.forEach { app ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                                    selectedApp = app
                                    prompt = savedPrompts[app.packageName]
                                        ?: AppSettings.defaultScreenPrompt(app.packageName)
                                    query = ""
                                },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppAvatar(app.label, app.packageName, size = 40)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(app.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Prompt riêng cho ứng dụng") },
                        minLines = 9,
                        maxLines = 16,
                        shape = MaterialTheme.shapes.medium,
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Prompt được lưu theo package và có thể sửa lại ở lần sau.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            prompt = AppSettings.defaultScreenPrompt(requireNotNull(selectedApp).packageName)
                        }) { Text("Mặc định") }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Hủy") }
                Button(
                    onClick = { onStart(requireNotNull(selectedApp), prompt.trim()) },
                    modifier = Modifier.weight(1f),
                    enabled = selectedApp != null && prompt.isNotBlank(),
                ) { Text("Mở ứng dụng") }
            }
        }
    }
}

@Composable
private fun BalanceHero(net: Long, income: Long, expense: Long, periodLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Dòng tiền ròng",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        formatMoney(net),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                    shape = CircleShape,
                ) {
                    Text(
                        periodLabel,
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroMetric("↓", "Tiền vào", income, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                HeroMetric("↑", "Tiền ra", expense, Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun HeroMetric(symbol: String, label: String, value: Long, modifier: Modifier, accent: Color) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            formatMoney(value),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Text(it, Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TransactionDayHeader(date: LocalDate, transactions: List<TransactionEntity>) {
    val income = transactions.filter { it.direction == "income" }.sumOf { it.amount }
    val expense = transactions.filter { it.direction == "expense" }.sumOf { it.amount }
    val net = income - expense
    val netColor = when {
        net > 0 -> IncomeStrong
        net < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                modifier = Modifier.padding(8.dp).size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                formatTransactionDayTitle(date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatEpochDay(date.toEpochDay())} · ${transactions.size} giao dịch",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = netColor.copy(alpha = 0.12f),
            shape = CircleShape,
        ) {
            Text(
                formatSignedMoney(net),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = netColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val income = transaction.direction == "income"
    val accent = if (income) IncomeStrong else MaterialTheme.colorScheme.error
    var menuExpanded by remember { mutableStateOf(false) }
    var showLlmInput by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val llmInput = remember(transaction.llmInputJson) { parseLlmInput(transaction.llmInputJson) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppAvatar(transaction.appName, transaction.packageName, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    transaction.purpose.ifBlank { "Không có nội dung" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${transaction.appName} · ${formatTransactionDateTime(transaction.transactionTime)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = if (transaction.sourceType == TransactionSource.MANUAL) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = CircleShape,
                    ) {
                        Text(
                            if (transaction.sourceType == TransactionSource.MANUAL) "Thủ công" else "Tự động",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        transaction.recipient.ifBlank { transaction.appName },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        (if (income) "+" else "−") + formatMoney(transaction.amount),
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Tùy chọn giao dịch",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa giao dịch") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    if (llmInput != null) {
                        DropdownMenuItem(
                            text = { Text("Dữ liệu trích xuất") },
                            leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showLlmInput = true
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Xóa giao dịch", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }

    if (showLlmInput) {
        llmInput?.let { input ->
            LlmInputDialog(
                input = input,
                onDismiss = { showLlmInput = false },
            )
        }
    }
}

@Composable
private fun DateFilterCard(
    startEpochDay: Long?,
    endEpochDay: Long?,
    resultCount: Int,
    onOpen: () -> Unit,
    onClear: () -> Unit,
) {
    val active = startEpochDay != null && endEpochDay != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) {
                        "${formatEpochDay(requireNotNull(startEpochDay))} – ${formatEpochDay(requireNotNull(endEpochDay))}"
                    } else {
                        "Tất cả thời gian"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) {
                    Text(
                        "$resultCount giao dịch",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (active) {
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Bỏ lọc")
                }
            }
            Text(
                if (active) "Đổi" else "Lọc",
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CompactListAction(
    title: String,
    description: String,
    actionLabel: String,
    danger: Boolean,
    onClick: () -> Unit,
) {
    val actionColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor = if (danger) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, actionColor.copy(alpha = 0.32f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = actionColor,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                actionLabel,
                color = actionColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDateRangeDialog(
    initialStartEpochDay: Long?,
    initialEndEpochDay: Long?,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartEpochDay?.times(MILLIS_PER_DAY),
        initialSelectedEndDateMillis = initialEndEpochDay?.times(MILLIS_PER_DAY),
    )
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis?.floorDiv(MILLIS_PER_DAY)
                    val end = state.selectedEndDateMillis?.floorDiv(MILLIS_PER_DAY)
                    when {
                        start == null || end == null -> errorMessage = "Hãy chọn đủ ngày bắt đầu và kết thúc"
                        LocalDate.ofEpochDay(end).isAfter(LocalDate.ofEpochDay(start).plusMonths(3)) -> {
                            errorMessage = "Khoảng lọc không được dài quá 3 tháng"
                        }
                        else -> onApply(start, end)
                    }
                },
            ) { Text("Áp dụng") }
        },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 610.dp)) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = {
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                        Text("Chọn khoảng ngày", style = MaterialTheme.typography.titleLarge)
                        Text("Khoảng tối đa 3 tháng", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                headline = {
                    val start = state.selectedStartDateMillis?.floorDiv(MILLIS_PER_DAY)
                    val end = state.selectedEndDateMillis?.floorDiv(MILLIS_PER_DAY)
                    Text(
                        when {
                            start != null && end != null -> "${formatEpochDay(start)} – ${formatEpochDay(end)}"
                            start != null -> "${formatEpochDay(start)} – Chọn ngày kết thúc"
                            else -> "Ngày bắt đầu – Ngày kết thúc"
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                showModeToggle = false,
            )
            errorMessage?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditorSheet(
    transaction: TransactionEntity?,
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String, String, Long) -> Unit,
) {
    val initialTime = transaction?.transactionTime ?: System.currentTimeMillis()
    var direction by remember(transaction?.id) { mutableStateOf(transaction?.direction ?: "expense") }
    var amount by remember(transaction?.id) { mutableStateOf(transaction?.amount?.toString().orEmpty()) }
    var recipient by remember(transaction?.id) { mutableStateOf(transaction?.recipient.orEmpty()) }
    var purpose by remember(transaction?.id) { mutableStateOf(transaction?.purpose.orEmpty()) }
    var selectedAppName by remember(transaction?.id) { mutableStateOf(transaction?.appName.orEmpty()) }
    var selectedPackageName by remember(transaction?.id) { mutableStateOf(transaction?.packageName.orEmpty()) }
    var appQuery by rememberSaveable(transaction?.id) { mutableStateOf("") }
    var showAppSearch by rememberSaveable(transaction?.id) {
        mutableStateOf(transaction?.packageName.isNullOrBlank())
    }
    var selectedEpochDay by remember(transaction?.id) { mutableStateOf(initialTime.toLocalDate().toEpochDay()) }
    var showDatePicker by rememberSaveable(transaction?.id) { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()
    val appResults = remember(installedApps, appQuery, selectedPackageName) {
        val query = appQuery.trim()
        if (query.isBlank()) emptyList() else installedApps.asSequence()
            .filter { app ->
                app.packageName != selectedPackageName &&
                    (app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true))
            }
            .take(8)
            .toList()
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    if (transaction == null) "Thêm giao dịch" else "Sửa giao dịch",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when {
                        transaction == null -> "Nguồn: Thủ công"
                        transaction.sourceType == TransactionSource.MANUAL -> "Nguồn: Thủ công"
                        else -> "Nguồn: Tự động"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Ứng dụng", style = MaterialTheme.typography.titleSmall)
                if (selectedPackageName.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppAvatar(selectedAppName, selectedPackageName, size = 42)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(selectedAppName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    selectedPackageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { showAppSearch = !showAppSearch }) {
                                Text(if (showAppSearch) "Đóng" else "Đổi")
                            }
                        }
                    }
                }
                if (showAppSearch) {
                    OutlinedTextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tìm ứng dụng") },
                        placeholder = { Text("Tên app hoặc package") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                    )
                    if (appQuery.isBlank()) {
                        Text(
                            "Nhập tên ứng dụng để tìm trong các app đã cài.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (appResults.isEmpty()) {
                        InlineMessage("Không tìm thấy ứng dụng phù hợp", error = true)
                    } else {
                        appResults.forEach { app ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                                    selectedAppName = app.label
                                    selectedPackageName = app.packageName
                                    appQuery = ""
                                    showAppSearch = false
                                },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Row(
                                    Modifier.padding(11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppAvatar(app.label, app.packageName, size = 38)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(app.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Loại giao dịch", style = MaterialTheme.typography.titleSmall)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DirectionSegment("income", direction, "Tiền vào", Modifier.weight(1f)) { direction = "income" }
                        DirectionSegment("expense", direction, "Tiền ra", Modifier.weight(1f)) { direction = "expense" }
                    }
                }
                OutlinedTextField(
                    value = formatAmountInput(amount),
                    onValueChange = { input -> amount = input.filter(Char::isDigit).trimStart('0').take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số tiền") },
                    suffix = { Text("đ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Người nhận") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mục đích / nội dung") },
                    minLines = 2,
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ngày giao dịch: ${formatEpochDay(selectedEpochDay)}")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text("Hủy") }
                Button(
                    onClick = {
                        onSave(
                            selectedAppName,
                            selectedPackageName,
                            direction,
                            requireNotNull(parsedAmount),
                            recipient.trim(),
                            purpose.trim(),
                            replaceDateKeepingTime(initialTime, selectedEpochDay),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = parsedAmount != null && parsedAmount > 0 &&
                        selectedPackageName.isNotBlank() && recipient.isNotBlank() && purpose.isNotBlank(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text(if (transaction == null) "Thêm giao dịch" else "Lưu thay đổi") }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEpochDay * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedEpochDay = it.floorDiv(MILLIS_PER_DAY)
                        }
                        showDatePicker = false
                    },
                ) { Text("Chọn") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PendingConfirmationScreen(
    notifications: List<NotificationWithDraft>,
    onReview: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onCancelAll: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var itemToCancel by remember { mutableStateOf<NotificationWithDraft?>(null) }
    var showCancelAllConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Moneycheck",
                title = "Cần xác nhận",
                subtitle = "Kiểm tra các giao dịch đã phân tích trước khi lưu vào Tổng quan.",
                eyebrowPill = false,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Chờ xử lý",
                    trailing = "${notifications.size} giao dịch",
                    subtitle = "Soát lại số tiền, người nhận và nội dung",
                )
                if (notifications.isNotEmpty()) {
                    CompactListAction(
                        title = "Hủy toàn bộ hàng chờ",
                        description = "Xóa ${notifications.size} kết quả phân tích chưa lưu",
                        actionLabel = "Hủy tất cả",
                        danger = true,
                        onClick = { showCancelAllConfirmation = true },
                    )
                }
            }
        }
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "✓",
                    title = "Không có giao dịch đang chờ",
                    description = "Kết quả phân tích mới cần bạn kiểm tra sẽ xuất hiện tại đây.",
                )
            }
        } else {
            items(notifications, key = { "pending:${it.notification.id}" }) { item ->
                PendingConfirmationCard(
                    item = item,
                    onReview = { onReview(item.notification.id) },
                    onCancel = { itemToCancel = item },
                )
            }
        }
    }

    itemToCancel?.let { item ->
        DeleteConfirmationDialog(
            title = "Hủy giao dịch đang chờ?",
            message = "Kết quả phân tích này sẽ bị hủy và không được thêm vào Tổng quan.",
            confirmLabel = "Hủy giao dịch",
            onDismiss = { itemToCancel = null },
            onConfirm = {
                onCancel(item.notification.id)
                itemToCancel = null
            },
        )
    }

    if (showCancelAllConfirmation) {
        DeleteConfirmationDialog(
            title = "Hủy tất cả giao dịch đang chờ?",
            message = "Toàn bộ ${notifications.size} kết quả đang chờ sẽ bị hủy và không được thêm vào Tổng quan. Thao tác này không thể hoàn tác.",
            confirmLabel = "Hủy tất cả",
            onDismiss = { showCancelAllConfirmation = false },
            onConfirm = {
                onCancelAll(notifications.map { it.notification.id })
                showCancelAllConfirmation = false
            },
        )
    }
}

@Composable
private fun PendingConfirmationCard(
    item: NotificationWithDraft,
    onReview: () -> Unit,
    onCancel: () -> Unit,
) {
    val draft = requireNotNull(item.draft)
    val income = draft.direction == "income"
    val expense = draft.direction == "expense"
    val amountColor = when {
        income -> IncomeStrong
        expense -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val directionLabel = when (draft.direction) {
        "income" -> "Tiền vào"
        "expense" -> "Tiền ra"
        else -> "Chưa rõ chiều giao dịch"
    }
    val amountPrefix = when {
        income -> "+"
        expense -> "−"
        else -> ""
    }
    val transactionTime = draft.transactionTime ?: item.notification.postedAt

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AppAvatar(item.notification.appName, item.notification.packageName, size = 42)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        draft.purpose.ifBlank { "Chưa xác định mục đích" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${item.notification.appName} · ${formatTransactionDateTime(transactionTime)}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = when {
                                income -> MaterialTheme.colorScheme.primaryContainer
                                expense -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        ) {
                            Text(
                                directionLabel,
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = amountColor,
                                maxLines = 1,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            draft.recipient.ifBlank { "Chưa xác định người nhận" },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            draft.amount?.let { amountPrefix + formatMoney(it) } ?: "Chưa rõ",
                            style = MaterialTheme.typography.titleMedium,
                            color = amountColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Hủy")
                }
                Button(onClick = onReview, modifier = Modifier.weight(1f)) {
                    Text("Xem và lưu")
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
    onAddConfig: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim()
    val filteredNotifications = remember(notifications, normalizedQuery) {
        if (normalizedQuery.isBlank()) notifications else notifications.filter { item ->
            item.notification.appName.contains(normalizedQuery, ignoreCase = true) ||
                item.notification.packageName.contains(normalizedQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Moneycheck",
                title = "Hộp thư",
                subtitle = "Notification mới nhận được giữ tạm trong 24 giờ.",
                eyebrowPill = false,
            )
        }
        if (!hasNotificationAccess) item { PermissionBanner(onOpenNotificationAccess) }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tìm ứng dụng") },
                placeholder = { Text("Tên app hoặc package") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Mới nhận",
                    trailing = if (normalizedQuery.isBlank()) "${notifications.size} mục"
                    else "${filteredNotifications.size}/${notifications.size} mục",
                    subtitle = "Lưu các mẫu cần giữ lại hoặc thêm app vào cấu hình",
                )
                if (notifications.isNotEmpty()) {
                    CompactListAction(
                        title = "Dọn hộp thư tạm",
                        description = "Xóa các notification đang hiển thị trong Hộp thư",
                        actionLabel = "Xóa",
                        danger = true,
                        onClick = onClear,
                    )
                }
            }
        }
        if (filteredNotifications.isEmpty()) {
            item {
                EmptyStateCard(
                    symbol = "◉",
                    title = if (notifications.isEmpty()) "Chưa có notification" else "Không tìm thấy ứng dụng",
                    description = if (notifications.isEmpty()) {
                        "Notification từ mọi ứng dụng sẽ xuất hiện tại đây và tự xóa sau 24 giờ."
                    } else {
                        "Thử tìm bằng tên ứng dụng hoặc package khác."
                    },
                )
            }
        } else {
            items(filteredNotifications, key = { it.notification.id }) { item ->
                NotificationCard(
                    item = item,
                    actionLabel = "Lưu",
                    onAction = { onSave(item.notification.id) },
                    onAddConfig = { onAddConfig(item.notification.id) },
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
    var notificationToDelete by remember { mutableStateOf<NotificationWithDraft?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Moneycheck",
                title = "Đã lưu",
                subtitle = "Các mẫu notification được giữ lại để test phân tích.",
                eyebrowPill = false,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Thư viện mẫu",
                    trailing = "${notifications.size} mục",
                    subtitle = "Chạy lại mẫu đã lưu để kiểm tra prompt và parser",
                )
                if (notifications.isNotEmpty()) {
                    CompactListAction(
                        title = "Xóa toàn bộ mẫu",
                        description = "Dọn các notification đã lưu khỏi thư viện test",
                        actionLabel = "Xóa tất cả",
                        danger = true,
                        onClick = { showClearConfirmation = true },
                    )
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
                    actionLabel = "Test lại",
                    onAction = { onTest(item.notification.id) },
                    onDelete = { notificationToDelete = item },
                    showStatus = false,
                )
            }
        }
    }

    notificationToDelete?.let { item ->
        DeleteConfirmationDialog(
            title = "Xóa notification đã lưu?",
            message = "Mẫu “${item.notification.title.ifBlank { "Không có tiêu đề" }}” sẽ bị xóa và không thể test lại.",
            onDismiss = { notificationToDelete = null },
            onConfirm = {
                onDelete(item.notification.id)
                notificationToDelete = null
            },
        )
    }

    if (showClearConfirmation) {
        DeleteConfirmationDialog(
            title = "Xóa tất cả notification đã lưu?",
            message = "Toàn bộ ${notifications.size} mẫu test đã lưu sẽ bị xóa.",
            confirmLabel = "Xóa tất cả",
            onDismiss = { showClearConfirmation = false },
            onConfirm = {
                onClear()
                showClearConfirmation = false
            },
        )
    }
}

@Composable
private fun NotificationCard(
    item: NotificationWithDraft,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onAddConfig: (() -> Unit)? = null,
    temporary: Boolean = false,
    showStatus: Boolean = true,
) {
    var showRaw by rememberSaveable(item.notification.id) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val notification = item.notification
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AppAvatar(notification.appName, notification.packageName, size = 42)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        notification.title.ifBlank { "Thông báo không có tiêu đề" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${notification.appName} · ${formatDateTime(notification.postedAt)}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (showStatus) {
                            Spacer(Modifier.width(6.dp))
                            StatusLabel(if (temporary) "temporary" else notification.analysisStatus)
                        }
                    }
                    Text(
                        notification.expandedContent.ifBlank { notification.text }.ifBlank { "(Không có nội dung)" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (showRaw) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Tùy chọn notification",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (showRaw) "Thu gọn chi tiết" else "Xem chi tiết") },
                            leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                            onClick = {
                                showRaw = !showRaw
                                menuExpanded = false
                            },
                        )
                        if (onAddConfig != null) {
                            DropdownMenuItem(
                                text = { Text("Thêm cấu hình") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddConfig()
                                },
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Xóa notification", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
            if (showStatus) {
                notification.errorMessage?.let { InlineMessage(it, error = true) }
            }
            if (showRaw) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(notification.rawPayload, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.packageName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun AppAvatar(name: String, packageName: String = "", size: Int = 40) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = AppIconCache.get(packageName), packageName) {
        if (packageName.isBlank() || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = 96, height = 96)
                    .asImageBitmap()
                    .also { AppIconCache.put(packageName, it) }
            }.getOrNull()
        }
    }
    Surface(
        modifier = Modifier.size(size.dp),
        shape = RoundedCornerShape((size * 0.28f).dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (icon != null) {
            Image(
                bitmap = requireNotNull(icon),
                contentDescription = "Icon $name",
                modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape((size * 0.24f).dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "A",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
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
    hasScreenCaptureAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenScreenCaptureAccess: () -> Unit,
    onValidateApiKey: (String) -> Unit,
    onApiKeyChanged: () -> Unit,
    onEnsureModelsLoaded: () -> Unit,
    onSave: (String, String, String, Map<String, Set<String>>, Map<String, String>, Boolean) -> Boolean,
    onClearApiKey: () -> Unit,
    onExportDatabase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var prompt by rememberSaveable { mutableStateOf(settings.prompt) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var notificationRuleInputs by remember {
        mutableStateOf(settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") })
    }
    var screenPromptInputs by remember { mutableStateOf(settings.screenPrompts) }
    var overlayEnabled by rememberSaveable { mutableStateOf(settings.overlayEnabled) }
    var query by rememberSaveable { mutableStateOf("") }
    var screenPromptQuery by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var promptEditorTarget by remember { mutableStateOf<PromptEditorTarget?>(null) }

    LaunchedEffect(settings) {
        model = settings.model
        prompt = settings.prompt
        notificationRuleInputs = settings.notificationRules
            .mapValues { (_, titles) -> titles.sorted().joinToString("\n") }
        screenPromptInputs = settings.screenPrompts
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
    val normalizedScreenPromptQuery = screenPromptQuery.trim()
    val installedAppByPackage = remember(installedApps) { installedApps.associateBy(InstalledApp::packageName) }
    val screenPromptApps = remember(installedAppByPackage, screenPromptInputs) {
        (KnownScreenPromptPackages + screenPromptInputs.keys)
            .distinct()
            .map { packageName -> installedAppByPackage[packageName] ?: knownScreenPromptApp(packageName) }
    }
    val screenPromptPackages = remember(screenPromptApps) { screenPromptApps.map(InstalledApp::packageName).toSet() }
    val filteredScreenPromptApps = remember(installedApps, normalizedScreenPromptQuery, screenPromptPackages) {
        if (normalizedScreenPromptQuery.isBlank()) emptyList() else installedApps
            .filter { app ->
                app.packageName !in screenPromptPackages &&
                    (app.label.contains(normalizedScreenPromptQuery, true) ||
                        app.packageName.contains(normalizedScreenPromptQuery, true))
            }
            .take(8)
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
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    PageHeader(
                        eyebrow = "Moneycheck",
                        title = "Cài đặt",
                        subtitle = "Quyền, OpenAI và các prompt phân tích",
                        modifier = Modifier.weight(1f),
                        eyebrowPill = false,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val rules = notificationRuleInputs.mapValues { (_, input) ->
                                input.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
                            }
                            saved = onSave(model, prompt, apiKey, rules, screenPromptInputs, overlayEnabled)
                            if (saved) apiKey = ""
                        },
                        enabled = canSave,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    ) { Text("Lưu") }
                }
            }
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
                    title = "Đọc màn hình giao dịch",
                    description = if (hasScreenCaptureAccess) {
                        "Sẵn sàng hiển thị nút nổi trên ứng dụng bạn chọn"
                    } else {
                        "Bật Trợ năng để đọc màn chi tiết khi bạn chủ động yêu cầu"
                    },
                    granted = hasScreenCaptureAccess,
                    actionLabel = if (hasScreenCaptureAccess) "Đã bật" else "Thiết lập",
                    onAction = onOpenScreenCaptureAccess,
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
                    description = "Hiện bảng xác nhận trượt từ dưới lên trên ứng dụng đang sử dụng",
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
                PromptPreviewCard(
                    title = "Prompt notification",
                    subtitle = "Dùng cho notification tự động và mẫu trong Hộp thư/Đã lưu.",
                    prompt = prompt,
                    isCustom = prompt.trim() != AppSettings.DEFAULT_PROMPT.trim(),
                    onEdit = {
                        promptEditorTarget = PromptEditorTarget(
                            title = "Prompt notification",
                            subtitle = "Chỉ title, text và expanded_content được gửi đi.",
                            prompt = prompt,
                            defaultPrompt = AppSettings.DEFAULT_PROMPT,
                        )
                    },
                    onReset = {
                        prompt = AppSettings.DEFAULT_PROMPT
                        saved = false
                    },
                )
            }
        }

        item {
            SettingsSection(
                title = "Prompt đọc màn hình",
                subtitle = "Prompt riêng theo app khi bấm nút nổi để phân tích màn chi tiết giao dịch.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                Text(
                    "Moneycheck tự nhận diện package app đang mở, rồi dùng prompt tương ứng ở đây. Nếu chưa có prompt riêng, app sẽ dùng prompt mặc định.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = screenPromptQuery,
                    onValueChange = { screenPromptQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm app để thêm/sửa prompt") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "Gợi ý & đã cấu hình",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                screenPromptApps.forEach { app ->
                    val defaultPrompt = AppSettings.defaultScreenPrompt(app.packageName)
                    val currentPrompt = screenPromptInputs[app.packageName] ?: defaultPrompt
                    ScreenPromptAppRow(
                        app = app,
                        prompt = currentPrompt,
                        isCustom = currentPrompt.trim() != defaultPrompt.trim(),
                        onEdit = {
                            promptEditorTarget = PromptEditorTarget(
                                packageName = app.packageName,
                                title = "Prompt ${app.label}",
                                subtitle = app.packageName,
                                prompt = currentPrompt,
                                defaultPrompt = defaultPrompt,
                            )
                        },
                    )
                }
                if (normalizedScreenPromptQuery.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(
                        "Kết quả tìm kiếm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (filteredScreenPromptApps.isEmpty()) {
                        Text(
                            "Không tìm thấy app phù hợp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        filteredScreenPromptApps.forEach { app ->
                            val defaultPrompt = AppSettings.defaultScreenPrompt(app.packageName)
                            ScreenPromptAppRow(
                                app = app,
                                prompt = defaultPrompt,
                                isCustom = false,
                                onEdit = {
                                    promptEditorTarget = PromptEditorTarget(
                                        packageName = app.packageName,
                                        title = "Prompt ${app.label}",
                                        subtitle = app.packageName,
                                        prompt = defaultPrompt,
                                        defaultPrompt = defaultPrompt,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "Ứng dụng tự động phân tích",
                subtitle = "Đã chọn ${notificationRuleInputs.size} ứng dụng. Chỉ notification khớp title mới gọi LLM và hiện bảng xác nhận.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm tên app hoặc package") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "App không được chọn vẫn xuất hiện trong Hộp thư 24 giờ, nhưng không tự gọi OpenAI và không bật bảng xác nhận.",
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

        item {
            SettingsSection(
                title = "Dữ liệu",
                subtitle = "Sao lưu database SQLite để mở bằng ứng dụng đọc SQLite hoặc lưu trữ ở nơi khác.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                Text(
                    "File xuất ra chứa dữ liệu giao dịch và notification. API key cùng cấu hình ứng dụng không được đưa vào file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onExportDatabase,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Xuất file database (.db)")
                }
            }
        }

    }

    promptEditorTarget?.let { target ->
        PromptEditorSheet(
            target = target,
            onDismiss = { promptEditorTarget = null },
            onApply = { updatedPrompt ->
                val packageName = target.packageName
                if (packageName == null) {
                    prompt = updatedPrompt
                } else {
                    screenPromptInputs = screenPromptInputs + (packageName to updatedPrompt)
                    screenPromptQuery = ""
                }
                saved = false
                promptEditorTarget = null
            },
        )
    }
}

private data class PromptEditorTarget(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val defaultPrompt: String,
    val packageName: String? = null,
)

private fun knownScreenPromptApp(packageName: String): InstalledApp = InstalledApp(
    packageName = packageName,
    label = when (packageName) {
        "com.shopee.vn" -> "Shopee"
        "vn.com.vng.zalopay" -> "ZaloPay"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    },
)

@Composable
private fun PromptPreviewCard(
    title: String,
    subtitle: String,
    prompt: String,
    isCustom: Boolean,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        PromptStatusChip(isCustom = isCustom)
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit) { Text("Sửa") }
            }
            Text(
                prompt.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .take(4)
                    .joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReset, enabled = isCustom) { Text("Mặc định") }
            }
        }
    }
}

@Composable
private fun ScreenPromptAppRow(
    app: InstalledApp,
    prompt: String,
    isCustom: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(app.label, app.packageName, size = 38)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        PromptStatusChip(isCustom = isCustom)
                    }
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onEdit) { Text("Sửa") }
            }
            Text(
                prompt.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .take(3)
                    .joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PromptStatusChip(isCustom: Boolean) {
    Surface(
        color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape,
    ) {
        Text(
            if (isCustom) "Đã chỉnh" else "Mặc định",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCustom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptEditorSheet(
    target: PromptEditorTarget,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var draft by remember(target) { mutableStateOf(target.prompt) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(target.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    target.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                label = { Text("Nội dung prompt") },
                minLines = 12,
                shape = MaterialTheme.shapes.medium,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = { draft = target.defaultPrompt }) { Text("Mặc định") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) { Text("Hủy") }
                Button(
                    onClick = { onApply(draft.trim()) },
                    enabled = draft.isNotBlank(),
                ) { Text("Áp dụng") }
            }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(start = 13.dp, top = 11.dp, bottom = 11.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(app.label, app.packageName)
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
private fun DeleteConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Xóa",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Không") } },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(confirmLabel)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionConfirmationDialog(
    item: NotificationWithDraft,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, String, Long) -> Unit,
) {
    val draft = requireNotNull(item.draft)
    var direction by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.direction) }
    var amount by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.amount?.toString().orEmpty()) }
    var purpose by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.purpose) }
    var recipient by remember(item.notification.id, draft.analyzedAt) { mutableStateOf(draft.recipient) }
    var transactionTime by remember(item.notification.id, draft.analyzedAt) {
        mutableStateOf(draft.transactionTime ?: item.notification.postedAt)
    }
    var showTransactionDatePicker by rememberSaveable(item.notification.id, draft.analyzedAt) {
        mutableStateOf(false)
    }
    var showLlmInput by rememberSaveable(item.notification.id, draft.analyzedAt) { mutableStateOf(false) }
    val isManualScreen = ScreenCaptureSessionStore.isManualScreenEvent(item.notification.eventId)
    val llmInput = remember(item.notification.rawPayload, item.notification.expandedContent, draft.rawModelJson) {
        if (isManualScreen) {
            parseLlmInput(item.notification.rawPayload)?.copy(modelOutput = draft.rawModelJson)
        } else {
            LlmInputSnapshot(
                source = "notification",
                title = item.notification.title,
                text = item.notification.text,
                expandedContent = item.notification.expandedContent,
            )
        }
    }
    val parsedAmount = amount.toLongOrNull()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("Xác nhận thông tin", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Từ ${item.notification.appName} · ${formatDateTime(transactionTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isManualScreen) {
                    Text(
                        "Nguồn: Đọc từ màn hình",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
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
                    onClick = { showTransactionDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ngày thanh toán: ${formatDateTime(transactionTime)}")
                }
                if (draft.transactionTime == null) {
                    Text(
                        "Không nhận diện được ngày thanh toán; đang dùng thời điểm ghi nhận. Bạn có thể chọn lại.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { showLlmInput = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = llmInput != null,
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Xem chi tiết đầu vào LLM")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text("Bỏ qua") }
                Button(
                    onClick = {
                        onConfirm(
                            direction,
                            requireNotNull(parsedAmount),
                            recipient.trim(),
                            purpose.trim(),
                            transactionTime,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = parsedAmount != null && parsedAmount > 0 &&
                        direction in setOf("income", "expense") && recipient.isNotBlank() && purpose.isNotBlank(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text("Lưu giao dịch") }
            }
        }
    }

    if (showTransactionDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = transactionTime.toLocalDate().toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showTransactionDatePicker = false },
            dismissButton = {
                TextButton(onClick = { showTransactionDatePicker = false }) { Text("Hủy") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            transactionTime = replaceDateKeepingTime(
                                transactionTime,
                                selectedMillis.floorDiv(MILLIS_PER_DAY),
                            )
                        }
                        showTransactionDatePicker = false
                    },
                ) { Text("Chọn") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLlmInput) {
        llmInput?.let { input ->
            LlmInputDialog(
                input = input,
                onDismiss = { showLlmInput = false },
            )
        }
    }
}

private data class LlmInputSnapshot(
    val source: String = "notification",
    val title: String = "",
    val text: String = "",
    val expandedContent: String = "",
    val prompt: String = "",
    val model: String = "",
    val modelOutput: String = "",
)

private fun parseLlmInput(json: String): LlmInputSnapshot? {
    if (json.isBlank()) return null
    return runCatching {
        val value = JSONObject(json)
        if (value.optString("source") == ScreenCaptureSessionStore.SOURCE) {
            LlmInputSnapshot(
                source = ScreenCaptureSessionStore.SOURCE,
                expandedContent = value.optString("screen_xml"),
                prompt = value.optString("prompt"),
                model = value.optString("model"),
                modelOutput = value.optString("model_output"),
            )
        } else {
            LlmInputSnapshot(
                title = value.optString("title"),
                text = value.optString("text"),
                expandedContent = value.optString("expanded_content"),
            )
        }
    }.getOrNull()
}

@Composable
private fun LlmInputDialog(
    input: LlmInputSnapshot,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dữ liệu trích xuất", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Nội dung nguồn dùng để tạo giao dịch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Đóng") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    LlmInputDetails(input)
                }
            }
        }
    }
}

@Composable
private fun LlmInputDetails(input: LlmInputSnapshot) {
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
                if (input.source == ScreenCaptureSessionStore.SOURCE) {
                    LlmInputField("source", input.source)
                    LlmInputField("model", input.model)
                    LlmInputField("prompt", input.prompt)
                    LlmInputField("screen_xml", input.expandedContent)
                    if (input.modelOutput.isNotBlank()) LlmInputField("model_output", input.modelOutput)
                } else {
                    LlmInputField("title", input.title)
                    LlmInputField("text", input.text)
                    LlmInputField("expanded_content", input.expandedContent)
                }
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

private val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
private val transactionDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
private val dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("vi-VN"))

private fun formatSignedMoney(amount: Long): String = when {
    amount > 0 -> "+${formatMoney(amount)}"
    amount < 0 -> "−${formatMoney(-amount)}"
    else -> formatMoney(0)
}

private fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

private fun formatTransactionDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(transactionDateTimeFormatter)

private fun formatTime(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(fullDateFormatter)

private fun formatTransactionDayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hôm nay"
        today.minusDays(1) -> "Hôm qua"
        else -> date.format(dayNameFormatter).capitalizeFirst()
    }
}

private fun String.capitalizeFirst(): String =
    if (isBlank()) this else take(1).uppercase(Locale.forLanguageTag("vi-VN")) + drop(1)

private fun replaceDateKeepingTime(originalTimestamp: Long, epochDay: Long): Long {
    val zone = ZoneId.systemDefault()
    val originalTime = Instant.ofEpochMilli(originalTimestamp).atZone(zone).toLocalTime()
    return LocalDate.ofEpochDay(epochDay)
        .atTime(originalTime)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
