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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.painterResource
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
    var showTransactionTimePicker by rememberSaveable(item.notification.id, draft.analyzedAt) {
        mutableStateOf(false)
    }
    var showLlmInput by rememberSaveable(item.notification.id, draft.analyzedAt) { mutableStateOf(false) }
    val llmInput = remember(item.notification.rawPayload, item.notification.expandedContent, draft.rawModelJson) {
        LlmInputSnapshot(
            source = "notification",
            title = item.notification.title,
            text = item.notification.text,
            expandedContent = item.notification.expandedContent,
            modelOutput = draft.rawModelJson,
        )
    }
    val parsedAmount = amount.toLongOrNull()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        sheetGesturesEnabled = false,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                    .align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Xác nhận giao dịch",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painterResource(com.example.moneycheck.R.drawable.shield_check_lucide),
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            "Tự động",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534),
                        )
                    }
                }
                Text(
                    "Chờ bạn kiểm tra",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    formatMoney(parsedAmount ?: 0),
                    fontSize = 33.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${item.notification.appName} · ${formatDateTime(transactionTime)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Loại giao dịch", style = MaterialTheme.typography.titleSmall)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DirectionSegment("expense", direction, "Tiền ra", Modifier.weight(1f)) { direction = "expense" }
                        DirectionSegment("income", direction, "Tiền vào", Modifier.weight(1f)) { direction = "income" }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showTransactionDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(formatEpochDay(transactionTime.toLocalDate().toEpochDay()))
                    }
                    OutlinedButton(
                        onClick = { showTransactionTimePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(formatTime(transactionTime))
                    }
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
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Xem chi tiết đầu vào LLM")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("Bỏ qua", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
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
                    modifier = Modifier.weight(1.65f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = parsedAmount != null && parsedAmount > 0 &&
                        direction in setOf("income", "expense") && recipient.isNotBlank() && purpose.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF006A47),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF006A47).copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f),
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lưu giao dịch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
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

    if (showTransactionTimePicker) {
        val localDateTime = remember(transactionTime) {
            Instant.ofEpochMilli(transactionTime).atZone(ZoneId.systemDefault())
        }
        val timePickerState = rememberTimePickerState(
            initialHour = localDateTime.hour,
            initialMinute = localDateTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTransactionTimePicker = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Chọn giờ thanh toán") },
            text = { TimePicker(state = timePickerState) },
            dismissButton = {
                TextButton(onClick = { showTransactionTimePicker = false }) { Text("Hủy") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionTime = replaceTimeKeepingDate(
                            transactionTime,
                            timePickerState.hour,
                            timePickerState.minute,
                        )
                        showTransactionTimePicker = false
                    },
                ) { Text("Chọn") }
            },
        )
    }

    if (showLlmInput) {
        LlmInputDialog(
            input = llmInput,
            onDismiss = { showLlmInput = false },
        )
    }
}
