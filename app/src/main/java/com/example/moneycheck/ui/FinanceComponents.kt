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

internal val IncomeStrong = Color(0xFF087A55)
private val AppIconCache = object : LruCache<String, ImageBitmap>(64) {}

@Composable
internal fun PageHeader(
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
internal fun ManualAddMethodDialog(
    onDismiss: () -> Unit,
    onManualInput: () -> Unit,
    onPickImage: () -> Unit,
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
                            "Nhập khoản thu hoặc chi tiền mặt; ứng dụng và người nhận là tùy chọn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onPickImage),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Đọc từ ảnh", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Chọn ảnh chụp màn hình hoặc biên lai để AI tự động trích xuất thông tin giao dịch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}

@Composable
internal fun BalanceHero(net: Long, income: Long, expense: Long, periodLabel: String) {
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
internal fun HeroMetric(symbol: String, label: String, value: Long, modifier: Modifier, accent: Color) {
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
internal fun SectionHeader(title: String, trailing: String? = null, subtitle: String? = null) {
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
internal fun TransactionDayHeader(date: LocalDate, transactions: List<TransactionEntity>) {
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
internal fun TransactionCard(
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                showLlmInput = true
            },
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
                    if (llmInput?.modelOutput?.isNotBlank() == true || transaction.llmInputJson.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showLlmInput = true },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Code,
                                    contentDescription = "Log LLM",
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Log LLM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
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
                    DropdownMenuItem(
                        text = { Text("Dữ liệu trích xuất & Log") },
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showLlmInput = true
                        },
                    )
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
        val displayInput = llmInput ?: LlmInputSnapshot(
            source = if (transaction.sourceType == TransactionSource.MANUAL) "manual" else "automatic",
            title = transaction.purpose.ifBlank { "Giao dịch ${transaction.appName}" },
            text = "Số tiền: ${formatMoney(transaction.amount)} | Bên nhận: ${transaction.recipient.ifBlank { "Không có" }} | Ứng dụng: ${transaction.appName} | Thời gian: ${formatTransactionDateTime(transaction.transactionTime)}",
            expandedContent = "",
            modelOutput = "",
        )
        LlmInputDialog(
            input = displayInput,
            onDismiss = { showLlmInput = false },
            onEdit = {
                showLlmInput = false
                onEdit()
            },
        )
    }
}

@Composable
internal fun DateFilterCard(
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
internal fun CompactListAction(
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
internal fun TransactionDateRangeDialog(
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

@Composable
internal fun AppAvatar(name: String, packageName: String = "", size: Int = 40) {
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
internal fun StatusLabel(status: String) {
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

@Composable
internal fun InlineMessage(message: String, modifier: Modifier = Modifier, error: Boolean = false) {
    val foreground = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val background = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    Surface(modifier = modifier.fillMaxWidth(), color = background, shape = MaterialTheme.shapes.small) {
        Text(message, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = foreground, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun ListCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun AppSelectionRow(
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
internal fun PermissionBanner(onOpenNotificationAccess: () -> Unit) {
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
internal fun EmptyStateCard(symbol: String, title: String, description: String) {
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
internal fun DeleteConfirmationDialog(
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

internal data class LlmInputSnapshot(
    val source: String = "notification",
    val title: String = "",
    val text: String = "",
    val expandedContent: String = "",
    val prompt: String = "",
    val model: String = "",
    val modelOutput: String = "",
)

internal fun parseLlmInput(json: String): LlmInputSnapshot? {
    if (json.isBlank()) return null
    return runCatching {
        val value = JSONObject(json)
        val source = value.optString("source").ifBlank { "notification" }
        val rawModel = value.optString("model_output").ifBlank { value.optString("raw_model_json") }
        val finalModelOutput = if (rawModel.isNotBlank()) {
            rawModel
        } else if (value.has("direction") || value.has("amount")) {
            json
        } else {
            ""
        }
        LlmInputSnapshot(
            source = source,
            title = value.optString("title"),
            text = value.optString("text"),
            expandedContent = value.optString("screen_xml").ifBlank { value.optString("expanded_content") },
            prompt = value.optString("prompt"),
            model = value.optString("model"),
            modelOutput = finalModelOutput,
        )
    }.getOrNull()
}

@Composable
internal fun LlmInputDialog(
    input: LlmInputSnapshot,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
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
                        Text("Dữ liệu trích xuất & Log LLM", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Chi tiết nguồn và kết quả phân tích AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onEdit != null) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sửa")
                        }
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
internal fun LlmInputDetails(input: LlmInputSnapshot) {
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
                if (input.modelOutput.isNotBlank()) {
                    val formatted = remember(input.modelOutput) {
                        runCatching {
                            val trimmed = input.modelOutput.trim()
                            when {
                                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                                trimmed.startsWith("[") -> org.json.JSONArray(trimmed).toString(2)
                                else -> input.modelOutput
                            }
                        }.getOrDefault(input.modelOutput)
                    }
                    LlmInputField("Log kết quả LLM trả ra (model_output)", formatted)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                } else {
                    LlmInputField(
                        "Log kết quả LLM trả ra (model_output)",
                        "(Không có log phản hồi của LLM cho giao dịch này)",
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (input.source.isNotBlank() && input.source != "notification") {
                    LlmInputField("source", input.source)
                }
                if (input.model.isNotBlank()) LlmInputField("model", input.model)
                if (input.prompt.isNotBlank()) LlmInputField("prompt", input.prompt)
                if (input.title.isNotBlank()) LlmInputField("title", input.title)
                if (input.text.isNotBlank()) LlmInputField("text", input.text)
                if (input.expandedContent.isNotBlank()) {
                    LlmInputField("expanded_content", input.expandedContent)
                }
            }
        }
    }
}

@Composable
internal fun LlmInputField(name: String, value: String) {
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
internal fun DirectionSegment(
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

internal fun formatAmountInput(digits: String): String = digits.reversed().chunked(3).joinToString(".").reversed()

internal fun formatMoney(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ"

internal val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
internal val transactionDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN"))
internal val dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("vi-VN"))

internal fun formatSignedMoney(amount: Long): String = when {
    amount > 0 -> "+${formatMoney(amount)}"
    amount < 0 -> "−${formatMoney(-amount)}"
    else -> formatMoney(0)
}

internal fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

internal fun formatTransactionDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(transactionDateTimeFormatter)

internal fun formatTime(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))

internal fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(fullDateFormatter)

internal fun formatTransactionDayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hôm nay"
        today.minusDays(1) -> "Hôm qua"
        else -> date.format(dayNameFormatter).capitalizeFirst()
    }
}

internal fun String.capitalizeFirst(): String =
    if (isBlank()) this else take(1).uppercase(Locale.forLanguageTag("vi-VN")) + drop(1)

internal fun replaceDateKeepingTime(originalTimestamp: Long, epochDay: Long): Long {
    val zone = ZoneId.systemDefault()
    val originalTime = Instant.ofEpochMilli(originalTimestamp).atZone(zone).toLocalTime()
    return LocalDate.ofEpochDay(epochDay)
        .atTime(originalTime)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

internal fun replaceTimeKeepingDate(originalTimestamp: Long, hour: Int, minute: Int): Long {
    val zone = ZoneId.systemDefault()
    val originalDate = Instant.ofEpochMilli(originalTimestamp).atZone(zone).toLocalDate()
    return originalDate
        .atTime(hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

internal const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
