package com.example.moneycheck.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.sp
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

internal val IncomeStrong = Color(0xFF006D49)
internal val ExpenseStrong = Color(0xFFCB473F)
private val AppIconCache = object : LruCache<String, ImageBitmap>(64) {}

@Composable
internal fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrowPill: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            eyebrow.uppercase(),
            fontSize = 8.sp,
            lineHeight = 11.sp,
            letterSpacing = 1.25.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        containerColor = MaterialTheme.colorScheme.background,
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
internal fun BalanceHero(net: Long, income: Long, expense: Long, transactionCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.moneycheck.ui.theme.SummaryGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width - 45.dp.toPx(), 28.dp.toPx())
                listOf(67.dp, 47.dp, 27.dp).forEach { radius ->
                    drawCircle(
                        color = com.example.moneycheck.ui.theme.SummaryMuted.copy(alpha = 0.35f),
                        radius = radius.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 23.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Dòng tiền ròng", Modifier.weight(1f), color = com.example.moneycheck.ui.theme.SummaryMuted, style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Filled.BarChart, contentDescription = null, tint = com.example.moneycheck.ui.theme.SummaryMuted, modifier = Modifier.size(19.dp))
            }
            Text(formatMoney(net), color = Color.White, fontSize = 31.sp, lineHeight = 46.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("•  Đã ghi nhận $transactionCount giao dịch", color = com.example.moneycheck.ui.theme.SummaryMuted, fontSize = 9.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroMetric(com.example.moneycheck.R.drawable.arrow_down_left_lucide, "Tiền vào", income, Modifier.weight(1f), com.example.moneycheck.ui.theme.SummaryIncome)
                HeroMetric(com.example.moneycheck.R.drawable.arrow_up_right_lucide, "Tiền ra", expense, Modifier.weight(1f), com.example.moneycheck.ui.theme.SummaryExpense)
            }
            Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
internal fun HeroMetric(iconRes: Int, label: String, value: Long, modifier: Modifier, accent: Color) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(17.dp), tint = accent)
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = com.example.moneycheck.ui.theme.SummaryMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        Text(
            formatMoney(value),
            color = accent,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionHeader(title: String, trailing: String? = null, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, fontSize = 9.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Text(it, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun TransactionDayHeader(date: LocalDate, transactions: List<TransactionEntity>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF006A47), CircleShape),
                )
                Text(
                    formatTransactionDayTitle(date),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            formatEpochDay(date.toEpochDay()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TransactionCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val income = transaction.direction == "income"
    val accent = if (income) IncomeStrong else ExpenseStrong
    var menuExpanded by remember { mutableStateOf(false) }
    var showLlmInput by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val llmInput = remember(transaction.llmInputJson) { parseLlmInput(transaction.llmInputJson) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                showLlmInput = true
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AppAvatar(transaction.appName, transaction.packageName, size = 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    transaction.purpose.ifBlank { "Không có nội dung" },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                    Text("${transaction.appName} · ${formatTime(transaction.transactionTime)}", fontSize = 9.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(color = if (transaction.sourceType == TransactionSource.MANUAL) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                    Text(if (transaction.sourceType == TransactionSource.MANUAL) "Thủ công" else "Tự động", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 8.sp, lineHeight = 11.sp, color = if (transaction.sourceType == TransactionSource.MANUAL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                }
                if (llmInput?.modelOutput?.isNotBlank() == true || transaction.llmInputJson.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Text("Log LLM", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 8.sp, lineHeight = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Người nhận: " + transaction.recipient.ifBlank { "chưa có" }, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Text((if (income) "+" else "−") + formatMoney(transaction.amount), color = accent, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
internal fun TransactionDateFilterSheet(
    initialStartEpochDay: Long?,
    initialEndEpochDay: Long?,
    onDismiss: () -> Unit,
    onApply: (Long?, Long?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }
    var startDay by remember { mutableStateOf(initialStartEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today.withDayOfMonth(1)) }
    var endDay by remember { mutableStateOf(initialEndEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Lọc theo ngày",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Chọn khoảng thời gian tối đa 3 tháng.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val presets = listOf(
                    "Tháng này" to (today.withDayOfMonth(1) to today),
                    "Tháng trước" to (today.minusMonths(1).withDayOfMonth(1) to today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth())),
                    "3 tháng gần đây" to (today.minusMonths(3) to today),
                )
                presets.forEach { (label, range) ->
                    OutlinedButton(
                        onClick = {
                            startDay = range.first
                            endDay = range.second
                            errorMessage = null
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Field: Từ ngày
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Từ ngày", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            formatEpochDay(startDay.toEpochDay()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Field: Đến ngày
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Đến ngày", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            formatEpochDay(endDay.toEpochDay()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Text(
                    error,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Modal Actions: [Tất cả thời gian] [Áp dụng]
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onApply(null, null)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text("Tất cả thời gian", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (endDay.isBefore(startDay)) {
                            errorMessage = "Ngày kết thúc phải sau ngày bắt đầu."
                            return@Button
                        }
                        if (endDay.isAfter(startDay.plusMonths(3))) {
                            errorMessage = "Khoảng lọc không được vượt quá 3 tháng."
                            return@Button
                        }
                        onApply(startDay.toEpochDay(), endDay.toEpochDay())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.5f).height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A47)),
                ) {
                    Text("Áp dụng", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        startDay = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        errorMessage = null
                    }
                    showStartDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        endDay = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        errorMessage = null
                    }
                    showEndDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = pickerState)
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
    expanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
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
                if (checked) {
                    val titleCount = titleInput.orEmpty().lineSequence().count { it.isNotBlank() }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                        Text(
                            if (titleCount == 0) "Mọi tiêu đề" else "$titleCount tiêu đề",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onToggleExpanded, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (expanded) "⌄" else "›", style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Checkbox(checked = false, onCheckedChange = { onCheckedChange() })
                }
            }
            if (checked && expanded && titleInput != null) {
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
                TextButton(onClick = onCheckedChange, modifier = Modifier.align(Alignment.End)) {
                    Text("Bỏ quy tắc", color = MaterialTheme.colorScheme.error)
                }
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 65.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Surface(modifier = Modifier.size(67.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Text(symbol, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall) }
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(description, fontSize = 12.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
        containerColor = MaterialTheme.colorScheme.background,
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
            color = MaterialTheme.colorScheme.background,
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
