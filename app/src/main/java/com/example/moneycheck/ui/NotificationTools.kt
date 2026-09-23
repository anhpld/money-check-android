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
internal fun NotificationInboxScreen(
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
internal fun SavedNotificationScreen(
    notifications: List<NotificationWithDraft>,
    retestStates: Map<Long, RetestState>,
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
                val retestState = retestStates[item.notification.id] ?: RetestState()
                NotificationCard(
                    item = item,
                    actionLabel = "Test lại",
                    onAction = { onTest(item.notification.id) },
                    actionEnabled = !retestState.isLoading,
                    actionLoading = retestState.isLoading,
                    actionMessage = retestState.message,
                    actionMessageIsError = retestState.isError,
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
internal fun AutoMatchedNotificationScreen(
    notifications: List<NotificationWithDraft>,
    onReview: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var notificationToRemove by remember { mutableStateOf<NotificationWithDraft?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "Moneycheck",
                title = "Đã bắt tự động",
                subtitle = "Notification đã match rule, được giữ lại trước khi gọi AI.",
                eyebrowPill = false,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Nhật ký đầu vào",
                    trailing = "${notifications.size} mục",
                    subtitle = "Bản gốc vẫn còn kể cả khi API hoặc model phân tích lỗi",
                )
                if (notifications.isNotEmpty()) {
                    CompactListAction(
                        title = "Dọn nhật ký tự động",
                        description = "Bỏ toàn bộ notification khỏi danh sách Đã bắt",
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
                    symbol = "◎",
                    title = "Chưa bắt được notification nào",
                    description = "Notification match package và title trong Cài đặt sẽ tự xuất hiện tại đây trước khi gọi AI.",
                )
            }
        } else {
            items(notifications, key = { it.notification.id }) { item ->
                val canReview = item.notification.analysisStatus == AnalysisStatus.READY && item.draft != null
                NotificationCard(
                    item = item,
                    actionLabel = if (canReview) "Xem kết quả" else "Đã lưu",
                    onAction = { if (canReview) onReview(item.notification.id) },
                    actionEnabled = canReview,
                    onDelete = { notificationToRemove = item },
                    deleteLabel = "Bỏ khỏi Đã bắt",
                    timestamp = item.notification.autoMatchedAt,
                )
            }
        }
    }

    notificationToRemove?.let { item ->
        DeleteConfirmationDialog(
            title = "Bỏ khỏi nhật ký Đã bắt?",
            message = "Notification này sẽ không còn trong tab Đã bắt. Giao dịch và kết quả đã tạo không bị xóa.",
            confirmLabel = "Bỏ khỏi danh sách",
            onDismiss = { notificationToRemove = null },
            onConfirm = {
                onRemove(item.notification.id)
                notificationToRemove = null
            },
        )
    }
    if (showClearConfirmation) {
        DeleteConfirmationDialog(
            title = "Dọn toàn bộ nhật ký Đã bắt?",
            message = "Các giao dịch và kết quả phân tích đã tạo vẫn được giữ nguyên.",
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
internal fun NotificationCard(
    item: NotificationWithDraft,
    actionLabel: String,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
    actionLoading: Boolean = false,
    actionMessage: String? = null,
    actionMessageIsError: Boolean = false,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Xóa notification",
    onAddConfig: (() -> Unit)? = null,
    temporary: Boolean = false,
    showStatus: Boolean = true,
    timestamp: Long? = null,
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
                            "${notification.appName} · ${formatDateTime(timestamp ?: notification.postedAt)}",
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
                            text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
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
            actionMessage?.let { InlineMessage(it, error = actionMessageIsError) }
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
                    enabled = actionEnabled && !actionLoading,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    if (actionLoading) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang test")
                    } else {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
