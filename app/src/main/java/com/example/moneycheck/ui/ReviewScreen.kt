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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
internal fun PendingConfirmationScreen(
    notifications: List<NotificationWithDraft>,
    onReview: (Long) -> Unit,
    onQuickConfirm: (NotificationWithDraft) -> Unit,
    onCancel: (Long) -> Unit,
    onCancelAll: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var itemToCancel by remember { mutableStateOf<NotificationWithDraft?>(null) }
    var showCancelAllConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                PageHeader(
                    eyebrow = "KIỂM TRA TRƯỚC KHI LƯU",
                    title = "Cần xác nhận",
                    subtitle = "${notifications.size} bản nháp đang chờ",
                    modifier = Modifier.weight(1f),
                    eyebrowPill = false,
                )
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(com.example.moneycheck.R.drawable.nav_list_checks),
                            contentDescription = null,
                            tint = Color(0xFF006A47),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        item {
            if (notifications.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = { showCancelAllConfirmation = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        ),
                    ) {
                        Text("Hủy toàn bộ hàng chờ", style = MaterialTheme.typography.labelSmall)
                    }
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
                    onQuickConfirm = { onQuickConfirm(item) },
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
internal fun PendingConfirmationCard(
    item: NotificationWithDraft,
    onReview: () -> Unit,
    onQuickConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val draft = requireNotNull(item.draft)
    val income = draft.direction == "income"
    val expense = draft.direction == "expense"
    val amountColor = when {
        income -> IncomeStrong
        expense -> ExpenseStrong
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
    val canQuickConfirm = draft.direction in setOf("income", "expense") &&
        (draft.amount ?: 0L) > 0L && draft.recipient.isNotBlank() && draft.purpose.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(4.dp)) {
                    Text("CHỜ XÁC NHẬN", Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color(0xFFD97706), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Hủy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                AppAvatar(item.notification.appName, item.notification.packageName, size = 36)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        draft.purpose.ifBlank { "Chưa xác định mục đích" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${item.notification.appName} · ${formatEpochDay(transactionTime.toLocalDate().toEpochDay())}, ${formatTime(transactionTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (income) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                    Icon(
                        painterResource(if (income) com.example.moneycheck.R.drawable.arrow_down_left_lucide else com.example.moneycheck.R.drawable.arrow_up_right_lucide),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(19.dp),
                        tint = if (income) IncomeStrong else MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(directionLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(draft.amount?.let { amountPrefix + formatMoney(it) } ?: "Chưa rõ", style = MaterialTheme.typography.titleLarge, color = amountColor, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Người nhận", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(draft.recipient.ifBlank { "Chưa xác định" }, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReview,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text("Xem và sửa", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(com.example.moneycheck.R.drawable.arrow_right_lucide),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Button(
                    onClick = onQuickConfirm,
                    enabled = canQuickConfirm,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Duyệt nhanh", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}
