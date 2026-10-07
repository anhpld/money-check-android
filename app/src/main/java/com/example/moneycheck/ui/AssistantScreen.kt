package com.example.moneycheck.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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

@Composable
internal fun ChatScreen(
    state: ChatState,
    transactionCount: Int,
    availableModels: List<String> = emptyList(),
    onSelectModel: (String) -> Unit = {},
    onRefreshModels: () -> Unit = {},
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var showModelDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (availableModels.isEmpty()) {
            onRefreshModels()
        }
    }

    fun send() {
        val question = input.trim()
        if (question.isEmpty() || state.isSending) return
        onSend(question)
        input = ""
        focusManager.clearFocus()
    }

    LaunchedEffect(state.messages.lastOrNull()?.content?.length, state.isSending) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    if (showModelDialog) {
        ModelSelectionDialog(
            selectedModel = state.selectedModel,
            availableModels = availableModels,
            onSelectModel = onSelectModel,
            onRefreshModels = onRefreshModels,
            onDismiss = { showModelDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteChatDialog(
            onConfirm = {
                onClear()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PageHeader(
                eyebrow = "HIỂU HƠN VỀ TIỀN CỦA BẠN",
                title = "Trợ lý",
                subtitle = "$transactionCount giao dịch đã lưu",
                modifier = Modifier.weight(1f),
                eyebrowPill = false,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = { showModelDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(com.example.moneycheck.R.drawable.nav_settings_2),
                        contentDescription = "Chọn model",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (state.messages.isNotEmpty()) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !state.isSending,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(com.example.moneycheck.R.drawable.trash_2_lucide),
                            contentDescription = "Xóa chat",
                            modifier = Modifier.size(19.dp),
                            tint = if (!state.isSending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    if (transactionCount == 0) {
                        EmptyStateCard(
                            symbol = "✦",
                            title = "Chưa có dữ liệu để trò chuyện",
                            description = "Thêm hoặc xác nhận một giao dịch trước khi hỏi trợ lý.",
                        )
                    } else {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Image(
                                painter = painterResource(com.example.moneycheck.R.drawable.assistant_spark),
                                contentDescription = null,
                                modifier = Modifier.size(108.dp),
                            )
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                Text("TRỢ LÝ TÀI CHÍNH", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Tiền của bạn,\nrõ ràng hơn.", fontSize = 27.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text("Hôm nay bạn muốn tìm hiểu điều gì?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.size(18.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "Dòng tiền của tôi thế nào?",
                                    "Tôi chi nhiều nhất cho khoản nào?",
                                    "Tổng chi tiêu đã lưu là bao nhiêu?",
                                ).forEach { suggestion ->
                                    OutlinedButton(
                                        onClick = { onSend(suggestion) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(43.dp)
                                            .shadow(2.dp, RoundedCornerShape(6.dp)),
                                        shape = RoundedCornerShape(6.dp),
                                        border = null,
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                                        contentPadding = PaddingValues(horizontal = 13.dp),
                                    ) {
                                        Text(
                                            suggestion,
                                            Modifier.weight(1f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Start,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Icon(
                                            painter = painterResource(com.example.moneycheck.R.drawable.arrow_up_right_lucide),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            items(state.messages, key = { it.id }) { message ->
                if (message.role == "user") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                            modifier = Modifier.fillMaxWidth(0.86f),
                        ) {
                            SelectionContainer {
                                Text(message.content, Modifier.padding(horizontal = 15.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, lineHeight = 23.sp)
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Surface(Modifier.size(25.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(painterResource(com.example.moneycheck.R.drawable.nav_sparkles), contentDescription = null, modifier = Modifier.padding(5.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        SelectionContainer {
                            ChatMarkdown(message.content, Modifier.weight(1f))
                        }
                    }
                }
            }
            if (state.isSending) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Đang phân tích…", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.errorMessage?.let { message ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(com.example.moneycheck.R.drawable.alert_circle_lucide),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                            )
                            val lastUserQuestion = state.messages.lastOrNull { it.role == "user" }?.content
                            if (!lastUserQuestion.isNullOrBlank()) {
                                TextButton(
                                    onClick = { onSend(lastUserQuestion) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                ) {
                                    Text("Thử lại", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 9.dp),
                        enabled = !state.isSending && transactionCount > 0,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (input.isEmpty()) Text("Hỏi về giao dịch của bạn…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                innerTextField()
                            }
                        },
                    )
                    Button(
                        onClick = { send() },
                        enabled = input.isNotBlank() && !state.isSending && transactionCount > 0,
                        modifier = Modifier.size(34.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(com.example.moneycheck.R.drawable.arrow_up_lucide),
                            contentDescription = "Gửi",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSelectionDialog(
    selectedModel: String,
    availableModels: List<String>,
    onSelectModel: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    var customEntry by rememberSaveable { mutableStateOf(false) }
    var customModelInput by rememberSaveable { mutableStateOf(selectedModel) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chọn model", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        },
        text = {
            if (customEntry) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Mã model",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = { customModelInput = it },
                        placeholder = { Text("Ví dụ: gpt-4o-mini") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Model phân tích",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { expandedDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        selectedModel.ifBlank { "Chọn model…" },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.75f),
                            ) {
                                availableModels.forEach { modelName ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                modelName,
                                                fontWeight = if (modelName == selectedModel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (modelName == selectedModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            onSelectModel(modelName)
                                            expandedDropdown = false
                                        },
                                    )
                                }
                                if (availableModels.isNotEmpty()) HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Nhập mã model…") },
                                    onClick = {
                                        expandedDropdown = false
                                        customEntry = true
                                    },
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshModels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(com.example.moneycheck.R.drawable.refresh_cw_lucide),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Làm mới danh sách model", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (customEntry) {
                Button(
                    onClick = {
                        val trimmed = customModelInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onSelectModel(trimmed)
                        }
                        onDismiss()
                    },
                    enabled = customModelInput.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Chọn model")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        },
        dismissButton = {
            if (customEntry) {
                OutlinedButton(
                    onClick = { customEntry = false },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Quay lại")
                }
            }
        },
    )
}

@Composable
private fun DeleteChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Xóa cuộc trò chuyện?", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        },
        text = {
            Text(
                "Toàn bộ tin nhắn trò chuyện sẽ bị xóa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Xác nhận xóa")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Giữ lại")
            }
        },
    )
}
