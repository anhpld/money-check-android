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
    hasScreenCaptureAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenScreenCaptureAccess: () -> Unit,
    onValidateApiKey: (String, String) -> Unit,
    onApiKeyChanged: () -> Unit,
    onSaveLocal: (String, Map<String, Set<String>>, Map<String, String>, Boolean) -> Unit,
    onSaveAi: (String, String, String) -> Boolean,
    onOpenTool: (SettingsDestination) -> Unit,
    onClearApiKey: () -> Unit,
    onExportDatabase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiBaseUrl by rememberSaveable { mutableStateOf(settings.apiBaseUrl) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var prompt by rememberSaveable { mutableStateOf(settings.prompt) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var notificationRuleInputs by rememberSaveable(stateSaver = StringMapSaver) {
        mutableStateOf(settings.notificationRules.mapValues { (_, titles) -> titles.sorted().joinToString("\n") })
    }
    var screenPromptInputs by rememberSaveable(stateSaver = StringMapSaver) { mutableStateOf(settings.screenPrompts.toMap()) }
    var overlayEnabled by rememberSaveable { mutableStateOf(settings.overlayEnabled) }
    var query by rememberSaveable { mutableStateOf("") }
    var screenPromptQuery by rememberSaveable { mutableStateOf("") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var promptEditorTarget by remember { mutableStateOf<PromptEditorTarget?>(null) }

    LaunchedEffect(openAiConnection.models) {
        if (openAiConnection.models.isNotEmpty() && model !in openAiConnection.models) model = openAiConnection.models.first()
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
    val normalizedScreenPromptQuery = screenPromptQuery.trim()
    val installedAppByPackage = remember(installedApps) { installedApps.associateBy(InstalledApp::packageName) }
    val screenPromptApps = remember(installedAppByPackage, screenPromptInputs) {
        (KnownScreenPromptPackages + screenPromptInputs.keys)
            .distinct()
            .map { packageName -> installedAppByPackage[packageName] ?: knownScreenPromptApp(packageName) }
    }
    val screenPromptPackages = remember(screenPromptApps) { screenPromptApps.map(InstalledApp::packageName).toSet() }
    val filteredScreenPromptApps = remember(installedApps, normalizedScreenPromptQuery, screenPromptPackages) {
        if (normalizedScreenPromptQuery.isBlank()) emptyList() else installedApps
            .filter { app ->
                app.packageName !in screenPromptPackages &&
                    (app.label.contains(normalizedScreenPromptQuery, true) ||
                        app.packageName.contains(normalizedScreenPromptQuery, true))
            }
            .take(8)
    }
    val canSave = model.isNotBlank() &&
        apiBaseUrl.isNotBlank() &&
        (settings.hasApiKey || apiKey.isNotBlank()) &&
        ((apiKey.isBlank() && apiBaseUrl.trim().trimEnd('/') == settings.apiBaseUrl) || openAiConnection.isVerified)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    PageHeader(
                        eyebrow = "Moneycheck",
                        title = "Cài đặt",
                        subtitle = "Quyền, AI và các prompt phân tích",
                        modifier = Modifier.weight(1f),
                        eyebrowPill = false,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val rules = notificationRuleInputs.mapValues { (_, input) ->
                                input.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
                            }
                            onSaveLocal(prompt, rules, screenPromptInputs, overlayEnabled)
                            saved = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) { Text("Lưu tùy chọn") }
                }
            }
        }

        if (saved) item { InlineMessage("Đã lưu cấu hình", modifier = Modifier.padding(horizontal = 18.dp)) }

        item {
            SettingsSection(
                title = "Công cụ thông báo",
                subtitle = "Hộp thư, nhật ký và mẫu kiểm thử — không phải sổ giao dịch.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                TextButton(onClick = { onOpenTool(SettingsDestination.INBOX) }) { Text("Hộp thư") }
                TextButton(onClick = { onOpenTool(SettingsDestination.AUTO_MATCHED) }) { Text("Đã bắt") }
                TextButton(onClick = { onOpenTool(SettingsDestination.SAVED) }) { Text("Đã lưu") }
            }
        }
        item {
            SettingsSection(
                title = "Quyền và hiển thị",
                subtitle = "Kiểm soát việc đọc và xác nhận notification.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                PermissionRow(
                    title = "Đọc notification",
                    description = if (hasNotificationAccess) "Đang hứng notification từ mọi ứng dụng" else "Cần cấp quyền để mở hộp thư 24 giờ",
                    granted = hasNotificationAccess,
                    actionLabel = "Thiết lập",
                    onAction = onOpenNotificationAccess,
                )
                PermissionRow(
                    title = "Đọc màn hình giao dịch",
                    description = if (hasScreenCaptureAccess) {
                        "Sẵn sàng hiển thị nút nổi trên ứng dụng bạn chọn"
                    } else {
                        "Bật Trợ năng để đọc màn chi tiết khi bạn chủ động yêu cầu"
                    },
                    granted = hasScreenCaptureAccess,
                    actionLabel = if (hasScreenCaptureAccess) "Đã bật" else "Thiết lập",
                    onAction = onOpenScreenCaptureAccess,
                )
                PermissionRow(
                    title = "Notification xác nhận",
                    description = if (canPostConfirmations) "Heads-up notification đang hoạt động" else "Đang bị tắt trong Android",
                    granted = canPostConfirmations,
                    actionLabel = "Mở cài đặt",
                    onAction = onRequestPostNotifications,
                )
                SettingToggleRow(
                    title = "Popup xác nhận nổi",
                    description = "Hiện bảng xác nhận trượt từ dưới lên trên ứng dụng đang sử dụng",
                    checked = overlayEnabled,
                    onCheckedChange = { enabled ->
                        overlayEnabled = enabled
                        saved = false
                        if (enabled && !canDrawOverlays) onRequestOverlayPermission()
                    },
                )
                if (overlayEnabled && !canDrawOverlays) {
                    InlineMessage("Cần cấp quyền “Hiển thị trên ứng dụng khác”. Notification vẫn được dùng làm dự phòng.", error = true)
                    OutlinedButton(onClick = onRequestOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("Cấp quyền popup nổi") }
                } else if (overlayEnabled) {
                    InlineMessage("Popup nổi đã sẵn sàng")
                }
            }
        }

        item {
            SettingsSection(
                title = "AI tương thích OpenAI",
                subtitle = "Dùng OpenAI, 9router hoặc dịch vụ có API tương thích.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = {
                        apiBaseUrl = it
                        saved = false
                        onApiKeyChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API base URL") },
                    placeholder = { Text("http://localhost:20128") },
                    supportingText = { Text("Chấp nhận URL gốc hoặc URL có /v1; ứng dụng tự nối endpoint") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        saved = false
                        onApiKeyChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    placeholder = { Text(if (settings.hasApiKey) "Đã lưu •••• · để trống để giữ nguyên" else "API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onValidateApiKey(apiBaseUrl, apiKey) },
                        enabled = !openAiConnection.isChecking && apiBaseUrl.isNotBlank() && (apiKey.isNotBlank() || settings.hasApiKey),
                    ) {
                        if (openAiConnection.isChecking) {
                            CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang kiểm tra")
                        } else Text("Kiểm tra kết nối")
                    }
                    if (settings.hasApiKey) TextButton(onClick = { onClearApiKey(); apiKey = "" }) { Text("Xóa key") }
                }
                Button(onClick = {
                    if (onSaveAi(apiBaseUrl, model, apiKey)) apiKey = ""
                }, enabled = canSave) { Text("Lưu kết nối AI") }
                Text("Tùy chọn cục bộ được lưu riêng, không cần API key hoặc mạng.", style = MaterialTheme.typography.bodySmall)
                openAiConnection.errorMessage?.let { InlineMessage(it, error = true) }
                if (openAiConnection.isVerified) {
                    InlineMessage("Kết nối thành công · ${openAiConnection.models.size} model khả dụng")
                    Text("Model đang dùng", style = MaterialTheme.typography.titleSmall)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { modelMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(model, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("⌄")
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f),
                        ) {
                            openAiConnection.models.forEach { modelId ->
                                DropdownMenuItem(
                                    text = { Text(modelId) },
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

        item {
            SettingsSection(
                title = "Prompt đọc màn hình",
                subtitle = "Prompt riêng theo app khi bấm nút nổi để phân tích màn chi tiết giao dịch.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                Text(
                    "Moneycheck tự nhận diện package app đang mở, rồi dùng prompt tương ứng ở đây. Nếu chưa có prompt riêng, app sẽ dùng prompt mặc định.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = screenPromptQuery,
                    onValueChange = { screenPromptQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm app để thêm/sửa prompt") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "Gợi ý & đã cấu hình",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                screenPromptApps.forEach { app ->
                    val defaultPrompt = AppSettings.defaultScreenPrompt(app.packageName)
                    val currentPrompt = screenPromptInputs[app.packageName] ?: defaultPrompt
                    ScreenPromptAppRow(
                        app = app,
                        prompt = currentPrompt,
                        isCustom = currentPrompt.trim() != defaultPrompt.trim(),
                        onEdit = {
                            promptEditorTarget = PromptEditorTarget(
                                packageName = app.packageName,
                                title = "Prompt ${app.label}",
                                subtitle = app.packageName,
                                prompt = currentPrompt,
                                defaultPrompt = defaultPrompt,
                            )
                        },
                    )
                }
                if (normalizedScreenPromptQuery.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(
                        "Kết quả tìm kiếm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (filteredScreenPromptApps.isEmpty()) {
                        Text(
                            "Không tìm thấy app phù hợp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        filteredScreenPromptApps.forEach { app ->
                            val defaultPrompt = AppSettings.defaultScreenPrompt(app.packageName)
                            ScreenPromptAppRow(
                                app = app,
                                prompt = defaultPrompt,
                                isCustom = false,
                                onEdit = {
                                    promptEditorTarget = PromptEditorTarget(
                                        packageName = app.packageName,
                                        title = "Prompt ${app.label}",
                                        subtitle = app.packageName,
                                        prompt = defaultPrompt,
                                        defaultPrompt = defaultPrompt,
                                    )
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
                subtitle = "Đã chọn ${notificationRuleInputs.size} ứng dụng. Chỉ notification khớp title mới gọi LLM và hiện bảng xác nhận.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm tên app hoặc package") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    "App không được chọn vẫn xuất hiện trong Hộp thư 24 giờ, nhưng không tự gọi AI và không bật bảng xác nhận.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (normalizedQuery.isNotBlank() && filteredApps.isEmpty()) {
                    Text(
                        "Không tìm thấy ứng dụng phù hợp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (selectedApps.isNotEmpty()) {
            item { ListCaption("Đang tự động phân tích", modifier = Modifier.padding(horizontal = 20.dp)) }
            items(selectedApps, key = { "selected:${it.packageName}" }) { app ->
                AppSelectionRow(
                    app = app,
                    checked = true,
                    onCheckedChange = {
                        notificationRuleInputs = notificationRuleInputs - app.packageName
                        saved = false
                    },
                    titleInput = notificationRuleInputs[app.packageName].orEmpty(),
                    onTitleInputChanged = { titles ->
                        notificationRuleInputs = notificationRuleInputs + (app.packageName to titles)
                        saved = false
                    },
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        if (filteredApps.isNotEmpty()) {
            item { ListCaption("Kết quả tìm kiếm", modifier = Modifier.padding(horizontal = 20.dp)) }
        }
        items(filteredApps, key = InstalledApp::packageName) { app ->
            AppSelectionRow(
                app = app,
                checked = false,
                onCheckedChange = {
                    notificationRuleInputs = notificationRuleInputs + (app.packageName to "")
                    saved = false
                },
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

        item {
            SettingsSection(
                title = "Dữ liệu",
                subtitle = "Sao lưu database SQLite để mở bằng ứng dụng đọc SQLite hoặc lưu trữ ở nơi khác.",
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                Text(
                    "File xuất ra chứa dữ liệu giao dịch và notification. API key cùng cấu hình ứng dụng không được đưa vào file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onExportDatabase,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Xuất file database (.db)")
                }
            }
        }

    }

    promptEditorTarget?.let { target ->
        PromptEditorSheet(
            target = target,
            onDismiss = { promptEditorTarget = null },
            onApply = { updatedPrompt ->
                val packageName = target.packageName
                if (packageName == null) {
                    prompt = updatedPrompt
                } else {
                    screenPromptInputs = screenPromptInputs + (packageName to updatedPrompt)
                    screenPromptQuery = ""
                }
                saved = false
                promptEditorTarget = null
            },
        )
    }
}

internal data class PromptEditorTarget(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val defaultPrompt: String,
    val packageName: String? = null,
)

internal fun knownScreenPromptApp(packageName: String): InstalledApp = InstalledApp(
    packageName = packageName,
    label = when (packageName) {
        "com.shopee.vn" -> "Shopee"
        "vn.com.vng.zalopay" -> "ZaloPay"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    },
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
        shape = RoundedCornerShape(16.dp),
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
internal fun ScreenPromptAppRow(
    app: InstalledApp,
    prompt: String,
    isCustom: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(app.label, app.packageName, size = 38)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        PromptStatusChip(isCustom = isCustom)
                    }
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onEdit) { Text("Sửa") }
            }
            Text(
                prompt.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .take(3)
                    .joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
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
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
internal fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(start = 13.dp, top = 11.dp, bottom = 11.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun SettingToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
