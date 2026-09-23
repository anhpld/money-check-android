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
internal fun PendingConfirmationScreen(
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
internal fun PendingConfirmationCard(
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
