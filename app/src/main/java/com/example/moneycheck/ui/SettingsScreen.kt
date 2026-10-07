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
import androidx.compose.runtime.saveable.Saver
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

private val StringMapSaver = Saver<Map<String, String>, ArrayList<String>>(
    save = { map -> ArrayList(map.flatMap { listOf(it.key, it.value) }) },
    restore = { entries -> entries.chunked(2).associate { it[0] to it[1] } },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: SettingsSnapshot,
    openAiConnection: OpenAiConnectionState,
    installedApps: List<InstalledApp>,
    hasNotificationAccess: Boolean,
    canPostConfirmations: Boolean,
    canDrawOverlays: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onValidateApiKey: (String, String) -> Unit,
    onApiKeyChanged: () -> Unit,
    onSaveLocal: (String, Map<String, Set<String>>, Boolean) -> Unit,
    onSaveAi: (String, String, String) -> Boolean,
    onOpenTool: (SettingsDestination) -> Unit,
    onClearApiKey: () -> Unit,
    onExportDatabase: () -> Unit,
    onImportDatabase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiBaseUrl by rememberSaveable { mutableStateOf(settings.apiBaseUrl) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var prompt by rememberSaveable { mutableStateOf(settings.prompt) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var notificationRuleInputs by rememberSaveable(stateSaver = StringMapSaver) {
        mutableStateOf(settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") })
    }
    var overlayEnabled by rememberSaveable { mutableStateOf(settings.overlayEnabled) }
    var lastPersistedRules by rememberSaveable(stateSaver = StringMapSaver) {
        mutableStateOf(settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") })
    }
    var query by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var aiSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedRulePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var promptEditorTarget by remember { mutableStateOf<PromptEditorTarget?>(null) }
    var showImportConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showAddAppSheet by rememberSaveable { mutableStateOf(false) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openAiConnection.models) {
        if (openAiConnection.models.isNotEmpty() && model !in openAiConnection.models) model = openAiConnection.models.first()
    }

    fun reconcilePersistedMaps() {
        val persistedRules = settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") }
        notificationRuleInputs = reconcileDraftMap(notificationRuleInputs, lastPersistedRules, persistedRules)
        lastPersistedRules = persistedRules
    }

    LaunchedEffect(settings.notificationRules) {
        reconcilePersistedMaps()
    }

    val normalizedQuery = query.trim()
    val selectedApps = remember(installedApps, notificationRuleInputs) {
        installedApps.filter { it.packageName in notificationRuleInputs }
    }
    val filteredApps = remember(installedApps, normalizedQuery, notificationRuleInputs) {
        if (normalizedQuery.isBlank()) emptyList() else installedApps.filter {
            it.packageName !in notificationRuleInputs &&
                (it.label.contains(normalizedQuery, true) || it.packageName.contains(normalizedQuery, true))
        }
    }
    val canSave = model.isNotBlank() &&
        apiBaseUrl.isNotBlank() &&
        (settings.hasApiKey || apiKey.isNotBlank()) &&
        ((apiKey.isBlank() && apiBaseUrl.trim().trimEnd('/') == settings.apiBaseUrl) || openAiConnection.isVerified)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PageHeader(
                        eyebrow = "THEO CÁCH CỦA BẠN",
                        title = "Cài đặt",
                        subtitle = "",
                        modifier = Modifier.weight(1f),
                        eyebrowPill = false,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            reconcilePersistedMaps()
                            val rules = notificationRuleInputs.mapValues { (_, input) ->
                                input.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
                            }
                            onSaveLocal(prompt, rules, overlayEnabled)
                            saved = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006A47),
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (saved) "Đã lưu" else "Lưu tùy chọn",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (saved) item { InlineMessage("Đã lưu cấu hình", modifier = Modifier.padding(horizontal = 24.dp)) }

        item {
            SettingsSection(
                title = "Công cụ thông báo",
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                SettingsCard {
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.inbox_lucide,
                        title = "Hộp thư",
                        subtitle = "Thông báo trong 24 giờ",
                        onClick = { onOpenTool(SettingsDestination.INBOX) },
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.scan_line_lucide,
                        title = "Đã bắt",
                        subtitle = "Theo dõi thông báo khớp quy tắc",
                        onClick = { onOpenTool(SettingsDestination.AUTO_MATCHED) },
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.bookmark_lucide,
                        title = "Đã lưu",
                        subtitle = "Mẫu thông báo để kiểm thử",
                        onClick = { onOpenTool(SettingsDestination.SAVED) },
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "Quyền và hiển thị",
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                SettingsCard {
                    SettingsPermissionRow(
                        iconRes = com.example.moneycheck.R.drawable.bell_lucide,
                        title = "Đọc thông báo",
                        granted = hasNotificationAccess,
                        onAction = onOpenNotificationAccess,
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    SettingsPermissionRow(
                        iconRes = com.example.moneycheck.R.drawable.bell_ring_lucide,
                        title = "Thông báo xác nhận",
                        granted = canPostConfirmations,
                        onAction = onRequestPostNotifications,
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    SettingsToggleRow(
                        iconRes = com.example.moneycheck.R.drawable.sliders_horizontal_lucide,
                        title = "Popup xác nhận nổi",
                        subtitle = "Xác nhận khi Moneycheck ở nền",
                        checked = overlayEnabled,
                        onCheckedChange = { enabled ->
                            overlayEnabled = enabled
                            saved = false
                            if (enabled && !canDrawOverlays) onRequestOverlayPermission()
                        },
                    )
                    if (overlayEnabled && !canDrawOverlays) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .clickable(onClick = onRequestOverlayPermission),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Cần quyền hiển thị trên ứng dụng khác.",
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Cấp quyền",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF006A47),
                                )
                                Icon(
                                    painterResource(com.example.moneycheck.R.drawable.chevron_right_lucide),
                                    contentDescription = null,
                                    tint = Color(0xFF006A47),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "AI tương thích OpenAI",
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                SettingsCard {
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.sparkles_lucide,
                        title = "Cấu hình AI nâng cao",
                        subtitle = if (aiSettingsExpanded) "Thu gọn" else "Base URL, API key, model",
                        onClick = { aiSettingsExpanded = !aiSettingsExpanded },
                        endContent = {
                            Text(
                                if (aiSettingsExpanded) "⌄" else "›",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                            )
                        }
                    )
                }
                if (aiSettingsExpanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("API base URL", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                OutlinedTextField(
                                    value = apiBaseUrl,
                                    onValueChange = {
                                        apiBaseUrl = it
                                        saved = false
                                        onApiKeyChanged()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("https://api.openai.com/v1", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("API key", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = {
                                        apiKey = it
                                        saved = false
                                        onApiKeyChanged()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(if (settings.hasApiKey) "Đã lưu •••• (để trống giữ nguyên)" else "Nhập API key", fontSize = 12.sp) },
                                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Text(if (showApiKey) "Ẩn" else "Hiện", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { onValidateApiKey(apiBaseUrl, apiKey) },
                                    enabled = !openAiConnection.isChecking && apiBaseUrl.isNotBlank() && (apiKey.isNotBlank() || settings.hasApiKey),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp),
                                ) {
                                    if (openAiConnection.isChecking) {
                                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Đang kiểm tra", fontSize = 11.sp)
                                    } else {
                                        Icon(painterResource(com.example.moneycheck.R.drawable.refresh_cw_lucide), contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Kiểm tra kết nối", fontSize = 11.sp)
                                    }
                                }
                                if (settings.hasApiKey) {
                                    TextButton(
                                        onClick = { onClearApiKey(); apiKey = "" },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp),
                                    ) {
                                        Text("Xóa key", fontSize = 11.sp, color = Color(0xFFDC2626))
                                    }
                                }
                            }

                            openAiConnection.errorMessage?.let {
                                Text(it, fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                            if (openAiConnection.isVerified) {
                                Text(
                                    "✓ Kết nối thành công · ${openAiConnection.models.size} model khả dụng",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Medium,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Model phân tích", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                    Box(Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { modelMenuExpanded = true },
                                            modifier = Modifier.fillMaxWidth().height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        ) {
                                            Text(model, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                            Text("⌄", fontSize = 14.sp)
                                        }
                                        DropdownMenu(
                                            expanded = modelMenuExpanded,
                                            onDismissRequest = { modelMenuExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f),
                                        ) {
                                            openAiConnection.models.forEach { modelId ->
                                                DropdownMenuItem(
                                                    text = { Text(modelId, fontSize = 12.sp) },
                                                    onClick = {
                                                        model = modelId
                                                        modelMenuExpanded = false
                                                        saved = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (onSaveAi(apiBaseUrl, model, apiKey)) apiKey = ""
                                },
                                enabled = canSave,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006A47),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("Lưu kết nối AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Prompt notification
                            PromptPreviewCard(
                                title = "Prompt notification",
                                subtitle = "Dùng cho notification tự động và mẫu trong Hộp thư/Đã lưu.",
                                prompt = prompt,
                                isCustom = prompt.trim() != AppSettings.DEFAULT_PROMPT.trim(),
                                onEdit = {
                                    promptEditorTarget = PromptEditorTarget(
                                        title = "Prompt notification",
                                        subtitle = "Chỉ title, text và expanded_content được gửi đi.",
                                        prompt = prompt,
                                        defaultPrompt = AppSettings.DEFAULT_PROMPT,
                                    )
                                },
                                onReset = {
                                    prompt = AppSettings.DEFAULT_PROMPT
                                    saved = false
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "Ứng dụng tự động phân tích",
                action = {
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { showAddAppSheet = true },
                        color = Color(0xFFDCFCE7),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(com.example.moneycheck.R.drawable.plus_lucide),
                                contentDescription = "Thêm ứng dụng",
                                tint = Color(0xFF006A47),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                if (selectedApps.isEmpty()) {
                    Text(
                        "Chưa chọn ứng dụng nào.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedApps.forEach { app ->
                            AppRuleCard(
                                app = app,
                                expanded = expandedRulePackage == app.packageName,
                                onToggleExpanded = {
                                    expandedRulePackage = if (expandedRulePackage == app.packageName) null else app.packageName
                                },
                                titleInput = notificationRuleInputs[app.packageName].orEmpty(),
                                onTitleInputChanged = { titles ->
                                    notificationRuleInputs = notificationRuleInputs + (app.packageName to titles)
                                    saved = false
                                },
                                onDeleteRule = {
                                    notificationRuleInputs = notificationRuleInputs - app.packageName
                                    saved = false
                                },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showAddAppSheet = true },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Icon(
                        painterResource(com.example.moneycheck.R.drawable.plus_lucide),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1A202C),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Thêm ứng dụng", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A202C))
                }
            }
        }

        item {
            SettingsSection(
                title = "Dữ liệu & Sao lưu",
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                SettingsCard {
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.download_lucide,
                        title = "Xuất dữ liệu",
                        subtitle = "Gói sao lưu (.zip)",
                        onClick = onExportDatabase,
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    SettingsRow(
                        iconRes = com.example.moneycheck.R.drawable.download_lucide,
                        title = "Nhập dữ liệu",
                        subtitle = "Khôi phục từ file .zip hoặc .db",
                        onClick = { showImportConfirmDialog = true },
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("m", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006A47))
                    Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    Spacer(Modifier.width(6.dp))
                    Text("Moneycheck", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A202C))
                }
                Text("Phiên bản 1.0", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }

    if (showAddAppSheet) {
        SelectAppSheet(
            installedApps = installedApps,
            selectedPackages = notificationRuleInputs.keys,
            onDismiss = { showAddAppSheet = false },
            onSelectApp = { app ->
                notificationRuleInputs = notificationRuleInputs + (app.packageName to "")
                saved = false
            },
        )
    }

    promptEditorTarget?.let { target ->
        PromptEditorSheet(
            target = target,
            onDismiss = { promptEditorTarget = null },
            onApply = { updatedPrompt ->
                prompt = updatedPrompt
                saved = false
                promptEditorTarget = null
            },
        )
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Phục hồi dữ liệu sao lưu?") },
            text = {
                Text(
                    "Thao tác này sẽ phục hồi giao dịch và cấu hình từ file đã chọn (hỗ trợ cả gói sao lưu .zip và file .db cũ). Dữ liệu hiện tại sẽ được thay thế bằng bản sao lưu. Bạn có chắc chắn muốn tiếp tục?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmDialog = false
                        onImportDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Tiếp tục chọn file")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportConfirmDialog = false }) {
                    Text("Hủy")
                }
            },
        )
    }
}

internal data class PromptEditorTarget(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val defaultPrompt: String,
)

@Composable
internal fun PromptPreviewCard(
    title: String,
    subtitle: String,
    prompt: String,
    isCustom: Boolean,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        PromptStatusChip(isCustom = isCustom)
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit) { Text("Sửa") }
            }
            Text(
                prompt.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .take(4)
                    .joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReset, enabled = isCustom) { Text("Mặc định") }
            }
        }
    }
}

@Composable
internal fun PromptStatusChip(isCustom: Boolean) {
    Surface(
        color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape,
    ) {
        Text(
            if (isCustom) "Đã chỉnh" else "Mặc định",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCustom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PromptEditorSheet(
    target: PromptEditorTarget,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var draft by remember(target) { mutableStateOf(target.prompt) }
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
                .fillMaxHeight(0.9f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(target.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    target.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                label = { Text("Nội dung prompt") },
                minLines = 12,
                shape = MaterialTheme.shapes.medium,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = { draft = target.defaultPrompt }) { Text("Mặc định") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) { Text("Hủy") }
                Button(
                    onClick = { onApply(draft.trim()) },
                    enabled = draft.isNotBlank(),
                ) { Text("Áp dụng") }
            }
        }
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    subtitle: String = "",
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = Color(0xFF59635F),
                fontWeight = FontWeight.Bold,
            )
            action?.invoke()
        }
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            content = content,
        )
    }
}

@Composable
internal fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    endContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(33.dp)
                .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = Color(0xFF006A47),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A202C),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp,
                )
            }
        }
        if (endContent != null) {
            endContent()
        } else if (onClick != null) {
            Icon(
                painter = painterResource(com.example.moneycheck.R.drawable.chevron_right_lucide),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF94A3B8),
            )
        }
    }
}

@Composable
internal fun SettingsPermissionRow(
    iconRes: Int,
    title: String,
    granted: Boolean,
    onAction: () -> Unit,
) {
    SettingsRow(
        iconRes = iconRes,
        title = title,
        subtitle = if (granted) "Đã cấp quyền" else "Chưa cấp quyền",
        onClick = onAction,
        endContent = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (granted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                modifier = Modifier.clickable(onClick = onAction),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        if (granted) "Đã cấp" else "Cấp quyền",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (granted) Color(0xFF166534) else Color(0xFFDC2626),
                    )
                    Icon(
                        painter = painterResource(com.example.moneycheck.R.drawable.chevron_right_lucide),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (granted) Color(0xFF166534) else Color(0xFFDC2626),
                    )
                }
            }
        }
    )
}

@Composable
internal fun SettingsToggleRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(33.dp)
                .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = Color(0xFF006A47),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A202C),
            )
            Text(
                subtitle,
                fontSize = 10.5.sp,
                color = Color(0xFF64748B),
                lineHeight = 15.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun AppRuleCard(
    app: InstalledApp,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    titleInput: String,
    onTitleInputChanged: (String) -> Unit,
    onDeleteRule: () -> Unit,
) {
    val titleCount = titleInput.lineSequence().count { it.isNotBlank() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(app.label, app.packageName)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A202C))
                    Text(
                        app.packageName,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        if (titleCount == 0) "Mọi tiêu đề" else "$titleCount tiêu đề",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (expanded) "⌄" else "›",
                    fontSize = 16.sp,
                    color = Color(0xFF94A3B8),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tiêu đề notification (mỗi dòng một tiêu đề)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = onTitleInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Để trống để nhận mọi tiêu đề", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDeleteRule,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        painterResource(com.example.moneycheck.R.drawable.trash_2_lucide),
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Bỏ quy tắc ${app.label}", color = Color(0xFFDC2626), fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectAppSheet(
    installedApps: List<InstalledApp>,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSelectApp: (InstalledApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, installedApps) {
        val q = query.trim().lowercase()
        if (q.isBlank()) installedApps
        else installedApps.filter { (it.label + it.packageName).lowercase().contains(q) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Chọn ứng dụng", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", modifier = Modifier.size(16.dp))
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                placeholder = { Text("Tìm tên hoặc package", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = InstalledApp::packageName) { app ->
                    val isSelected = app.packageName in selectedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectApp(app)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppAvatar(app.label, app.packageName)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(app.packageName, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF006A47), modifier = Modifier.size(18.dp))
                        } else {
                            Icon(painterResource(com.example.moneycheck.R.drawable.plus_lucide), contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
