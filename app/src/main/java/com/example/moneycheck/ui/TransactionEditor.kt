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

data class TransactionInitialDraft(
    val direction: String = "expense",
    val amount: Long? = null,
    val recipient: String = "",
    val purpose: String = "",
    val appName: String = "Tiền mặt",
    val packageName: String = "",
    val transactionTime: Long = System.currentTimeMillis(),
    val rawModelJson: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionEditorSheet(
    transaction: TransactionEntity?,
    initialDraft: TransactionInitialDraft? = null,
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String, String, Long, String) -> Unit,
) {
    val initialTime = transaction?.transactionTime ?: initialDraft?.transactionTime ?: System.currentTimeMillis()
    val initialDraftKey = initialDraft?.let { "${it.direction}-${it.amount}-${it.recipient}-${it.purpose}-${it.appName}" }
    val stateKey = transaction?.id ?: initialDraftKey
    var direction by rememberSaveable(stateKey) { mutableStateOf(transaction?.direction ?: initialDraft?.direction ?: "expense") }
    var amount by rememberSaveable(stateKey) { mutableStateOf(transaction?.amount?.toString() ?: initialDraft?.amount?.toString().orEmpty()) }
    var recipient by rememberSaveable(stateKey) { mutableStateOf(transaction?.recipient ?: initialDraft?.recipient.orEmpty()) }
    var purpose by rememberSaveable(stateKey) { mutableStateOf(transaction?.purpose ?: initialDraft?.purpose.orEmpty()) }
    var selectedAppName by rememberSaveable(stateKey) { mutableStateOf(transaction?.appName ?: initialDraft?.appName ?: "Tiền mặt") }
    var selectedPackageName by rememberSaveable(stateKey) { mutableStateOf(transaction?.packageName ?: initialDraft?.packageName.orEmpty()) }
    var appQuery by rememberSaveable(stateKey) { mutableStateOf("") }
    var showAppSearch by rememberSaveable(stateKey) {
        mutableStateOf(false)
    }
    var selectedEpochDay by rememberSaveable(stateKey) { mutableStateOf(initialTime.toLocalDate().toEpochDay()) }
    val initialLocalTime = remember(stateKey) {
        Instant.ofEpochMilli(initialTime).atZone(ZoneId.systemDefault()).toLocalTime()
    }
    var selectedHour by rememberSaveable(stateKey) { mutableStateOf(initialLocalTime.hour) }
    var selectedMinute by rememberSaveable(stateKey) { mutableStateOf(initialLocalTime.minute) }
    var showDatePicker by rememberSaveable(stateKey) { mutableStateOf(false) }
    var showTimePicker by rememberSaveable(stateKey) { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()
    val validation = validateManualTransaction(
        amountInput = amount,
        direction = direction,
        recipient = recipient,
        purpose = purpose,
        appName = selectedAppName,
        packageName = selectedPackageName,
    )
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
                Text("Nguồn tiền", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        selectedAppName = "Tiền mặt"
                        selectedPackageName = ""
                        showAppSearch = false
                    }) { Text("Tiền mặt") }
                    TextButton(onClick = { showAppSearch = !showAppSearch }) { Text("Chọn ứng dụng (tùy chọn)") }
                }
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
                    isError = ManualTransactionField.AMOUNT in validation.errors,
                    supportingText = { validation.errors[ManualTransactionField.AMOUNT]?.let { Text(it) } },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Người nhận (tùy chọn)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mục đích / nội dung") },
                    minLines = 2,
                    isError = ManualTransactionField.PURPOSE in validation.errors,
                    supportingText = { validation.errors[ManualTransactionField.PURPOSE]?.let { Text(it) } },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(formatEpochDay(selectedEpochDay))
                    }
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute))
                    }
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
                        val llmInput = transaction?.llmInputJson?.ifBlank { null }
                            ?: initialDraft?.rawModelJson?.takeIf(String::isNotBlank)?.let { raw ->
                                JSONObject().apply {
                                    put("source", "image_vision")
                                    put("model_output", raw)
                                }.toString()
                            }.orEmpty()
                        onSave(
                            selectedAppName,
                            selectedPackageName,
                            direction,
                            requireNotNull(parsedAmount),
                            recipient.trim(),
                            purpose.trim(),
                            replaceTimeKeepingDate(
                                replaceDateKeepingTime(initialTime, selectedEpochDay),
                                selectedHour,
                                selectedMinute,
                            ),
                            llmInput,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = validation.isValid,
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

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ giao dịch") },
            text = { TimePicker(state = timePickerState) },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    },
                ) { Text("Chọn") }
            },
        )
    }
}
