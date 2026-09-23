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

@Composable
internal fun TransactionScreen(
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
    var transactionToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    val transactionToEdit = transactions.firstOrNull { it.id == transactionToEditId }
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
                transactionToEditId = null
                showTransactionEditor = true
            },
            onReadScreen = {
                showAddMethodPicker = false
                onStartScreenRead()
            },
        )
    }
}
