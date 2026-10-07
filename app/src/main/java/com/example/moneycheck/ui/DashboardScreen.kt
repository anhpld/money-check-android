package com.example.moneycheck.ui

import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
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
import com.example.moneycheck.ChatState
import com.example.moneycheck.RetestState
import com.example.moneycheck.data.AnalysisStatus
import com.example.moneycheck.data.NotificationWithDraft
import com.example.moneycheck.data.TransactionEntity
import com.example.moneycheck.data.TransactionSource
import com.example.moneycheck.settings.AppSettings
import com.example.moneycheck.settings.InstalledApp
import com.example.moneycheck.settings.SettingsSnapshot
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun readBytesFromUri(context: android.content.Context, uri: Uri): ByteArray {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.isNotEmpty()) return bytes
        }
    } catch (_: Exception) {}

    try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            java.io.FileInputStream(pfd.fileDescriptor).use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        }
    } catch (_: Exception) {}

    try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            afd.createInputStream().use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        }
    } catch (_: Exception) {}

    throw java.io.IOException("Không thể mở tệp hình ảnh")
}

@Composable
internal fun TransactionScreen(
    transactions: List<TransactionEntity>,
    installedApps: List<InstalledApp>,
    isAnalyzingImage: Boolean = false,
    onAnalyzeImageBytes: (ByteArray, (TransactionInitialDraft) -> Unit) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit,
    onAdd: (String, String, String, Long, String, String, Long, String, () -> Unit) -> Unit,
    onUpdate: (Long, String, String, String, Long, String, String, Long, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var filterStartEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterEndEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDateFilter by rememberSaveable { mutableStateOf(false) }
    var showTransactionEditor by rememberSaveable { mutableStateOf(false) }
    var showAddMethodPicker by rememberSaveable { mutableStateOf(false) }
    var transactionToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var initialDraft by remember { mutableStateOf<TransactionInitialDraft?>(null) }
    val transactionToEdit = transactions.firstOrNull { it.id == transactionToEditId }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        readBytesFromUri(context, uri)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bytes != null) {
                    onAnalyzeImageBytes(bytes) { extractedDraft ->
                        transactionToEditId = null
                        initialDraft = extractedDraft
                        showTransactionEditor = true
                    }
                } else {
                    android.widget.Toast.makeText(context, "Không thể mở tệp hình ảnh đã chọn", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
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
        contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                PageHeader(
                    eyebrow = "MỘT CHÚT RÕ RÀNG, MỖI NGÀY",
                    title = "Tổng quan",
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
                        transactionToEditId = null
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
                transactionCount = filteredTransactions.size,
            )
        }
        item {
            val spentPercent = if (income > 0) (expense * 100 / income).coerceAtMost(999) else 0
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(painterResource(com.example.moneycheck.R.drawable.arrow_up_right_lucide), contentDescription = null, modifier = Modifier.padding(7.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (income >= expense) "Thu nhiều hơn chi" else "Chi nhiều hơn thu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("$spentPercent% thu nhập đã được chi tiêu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(14, 19, 13, 24, 18, 28, 22).forEachIndexed { index, height ->
                        Box(
                            Modifier.size(width = 4.dp, height = height.dp).background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (index >= 5) 0.9f else 0.6f),
                                RoundedCornerShape(2.dp),
                            ),
                        )
                    }
                }
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Giao dịch gần đây", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(filteredTransactions.size.toString(), Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showDateFilter = true }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (filterStartEpochDay == null) "Lọc ngày" else "Đã lọc", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (filterStartEpochDay != null && filterEndEpochDay != null) {
                TextButton(onClick = { filterStartEpochDay = null; filterEndEpochDay = null }) {
                    Text("${formatEpochDay(requireNotNull(filterStartEpochDay))} – ${formatEpochDay(requireNotNull(filterEndEpochDay))}  ×", style = MaterialTheme.typography.labelSmall)
                }
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
                            transactionToEditId = transaction.id
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
            initialDraft = initialDraft,
            installedApps = installedApps,
            onDismiss = {
                showTransactionEditor = false
                initialDraft = null
            },
            onSave = { appName, packageName, direction, amount, recipient, purpose, transactionTime, llmInputJson ->
                val editing = transactionToEdit
                if (editing == null) {
                    onAdd(appName, packageName, direction, amount, recipient, purpose, transactionTime, llmInputJson) {
                        showTransactionEditor = false
                        initialDraft = null
                    }
                } else {
                    onUpdate(editing.id, appName, packageName, direction, amount, recipient, purpose, transactionTime) {
                        showTransactionEditor = false
                        initialDraft = null
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
                transactionToEditId = null
                initialDraft = null
                showTransactionEditor = true
            },
            onPickImage = {
                showAddMethodPicker = false
                imagePickerLauncher.launch("image/*")
            },
        )
    }

    if (isAnalyzingImage) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.background,
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    CircularProgressIndicator()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Đang phân tích hình ảnh…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "AI đang đọc và trích xuất thông tin giao dịch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }
}
