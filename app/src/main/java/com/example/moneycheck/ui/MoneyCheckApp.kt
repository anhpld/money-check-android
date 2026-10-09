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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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

private enum class MainTab(
    val label: String,
    val iconRes: Int,
) {
    TRANSACTIONS("Tổng quan", com.example.moneycheck.R.drawable.nav_layout_grid),
    PENDING("Duyệt", com.example.moneycheck.R.drawable.nav_list_checks),
    CHAT("Trợ lý", com.example.moneycheck.R.drawable.nav_sparkles),
    SETTINGS("Cài đặt", com.example.moneycheck.R.drawable.nav_settings_2),
}



@Composable
fun MoneyCheckApp(
    viewModel: MainViewModel,
    hasNotificationAccess: Boolean,
    canPostConfirmations: Boolean,
    canDrawOverlays: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onExportDatabase: () -> Unit,
    onImportDatabase: () -> Unit,
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val inboxNotifications by viewModel.inboxNotifications.collectAsStateWithLifecycle()
    val savedNotifications by viewModel.savedNotifications.collectAsStateWithLifecycle()
    val autoMatchedNotifications by viewModel.autoMatchedNotifications.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val confirmationId by viewModel.confirmationId.collectAsStateWithLifecycle()
    val openAiConnection by viewModel.openAiConnection.collectAsStateWithLifecycle()
    val isAnalyzingImage by viewModel.isAnalyzingImage.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val retestStates by viewModel.retestStates.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TRANSACTIONS) }
    val saveableStateHolder = rememberSaveableStateHolder()
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsDestination.OVERVIEW) }
    val pendingConfirmations = remember(notifications) {
        notifications.filter {
            it.notification.analysisStatus == AnalysisStatus.READY && it.draft != null
        }
    }

    LaunchedEffect(notifications, confirmationId) {
        if (confirmationId == null) {
            notifications.firstOrNull {
                it.notification.analysisStatus == AnalysisStatus.READY && !it.notification.handled
            }?.let { viewModel.requestConfirmation(it.notification.id) }
        } else {
            val current = notifications.firstOrNull { it.notification.id == confirmationId }
            if (current == null || current.notification.analysisStatus in
                setOf(AnalysisStatus.ERROR, AnalysisStatus.IGNORED)
            ) viewModel.requestConfirmation(null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(com.example.moneycheck.R.drawable.moneycheck_brand),
                        contentDescription = "Logo Moneycheck",
                        modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    )
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                            append("moneycheck")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary))
                            append(".")
                            pop()
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)).navigationBarsPadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 2.dp),
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val itemColor = if (selected) Color(0xFF006A47) else MaterialTheme.colorScheme.onSurfaceVariant
                        val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(0xFFDCFCE7) else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp),
                                    tint = itemColor,
                                )
                                if (tab == MainTab.PENDING && pendingConfirmations.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-7).dp)
                                            .size(17.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF9F4E6)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            pendingConfirmations.size.toString(),
                                            fontSize = 8.sp,
                                            color = Color(0xFF9E7525),
                                            lineHeight = 10.sp,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tab.label,
                                color = itemColor,
                                fontSize = 9.sp,
                                fontWeight = fontWeight,
                                lineHeight = 11.sp,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        saveableStateHolder.SaveableStateProvider(selectedTab) {
            when (selectedTab) {
            MainTab.TRANSACTIONS -> TransactionScreen(
                transactions = transactions,
                installedApps = installedApps,
                isAnalyzingImage = isAnalyzingImage,
                onAnalyzeImageBytes = viewModel::analyzeTransactionImageBytes,
                onDelete = viewModel::deleteTransaction,
                onAdd = { appName, packageName, direction, amount, recipient, purpose, transactionTime, llmInputJson, onComplete ->
                    viewModel.addManualTransaction(
                        appName = appName,
                        packageName = packageName,
                        direction = direction,
                        amount = amount,
                        recipient = recipient,
                        purpose = purpose,
                        transactionTime = transactionTime,
                        llmInputJson = llmInputJson,
                        onComplete = onComplete,
                    )
                },
                onUpdate = viewModel::updateTransaction,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.CHAT -> ChatScreen(
                state = chatState,
                transactionCount = transactions.size,
                availableModels = openAiConnection.models,
                onSelectModel = viewModel::selectChatModel,
                onRefreshModels = viewModel::ensureOpenAiModelsLoaded,
                onSend = viewModel::sendChatMessage,
                onClear = viewModel::clearChat,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.PENDING -> PendingConfirmationScreen(
                notifications = pendingConfirmations,
                onReview = viewModel::requestConfirmation,
                onQuickConfirm = { item ->
                    item.draft?.let { draft ->
                        viewModel.confirmTransaction(
                            notification = item.notification,
                            direction = draft.direction,
                            amount = draft.amount ?: 0L,
                            recipient = draft.recipient,
                            purpose = draft.purpose,
                            transactionTime = draft.transactionTime ?: item.notification.postedAt,
                        )
                    }
                },
                onCancel = viewModel::cancelPendingConfirmation,
                onCancelAll = viewModel::cancelAllPendingConfirmations,
                modifier = Modifier.padding(innerPadding),
            )

            MainTab.SETTINGS -> SettingsDestinationScreen(
                modifier = Modifier.padding(innerPadding),
                destination = settingsDestination,
                onDestinationSelected = { settingsDestination = it },
                overview = {
                    SettingsScreen(
                settings = settings,
                openAiConnection = openAiConnection,
                installedApps = installedApps,
                hasNotificationAccess = hasNotificationAccess,
                canPostConfirmations = canPostConfirmations,
                canDrawOverlays = canDrawOverlays,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestPostNotifications = onRequestPostNotifications,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onValidateApiKey = viewModel::validateOpenAiKey,
                onApiKeyChanged = viewModel::clearOpenAiValidation,
                onSaveLocal = viewModel::saveLocalPreferences,
                onSaveAi = viewModel::saveAiConnection,
                onOpenTool = { settingsDestination = it },
                onClearApiKey = viewModel::clearApiKey,
                onExportDatabase = onExportDatabase,
                onImportDatabase = onImportDatabase,
                    )
                },
                inbox = {
                    NotificationInboxScreen(
                        notifications = inboxNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onOpenNotificationAccess = onOpenNotificationAccess,
                        onSave = viewModel::saveNotification,
                        onAddConfig = viewModel::addNotificationConfig,
                        onClear = viewModel::clearInbox,
                            )
                },
                autoMatched = {
                    AutoMatchedNotificationScreen(
                        notifications = autoMatchedNotifications,
                        onReview = viewModel::requestConfirmation,
                        onRemove = viewModel::removeAutoMatchedNotification,
                        onClear = viewModel::clearAutoMatchedNotifications,
                            )
                },
                saved = {
                    SavedNotificationScreen(
                        notifications = savedNotifications,
                        retestStates = retestStates,
                        onTest = viewModel::testNotification,
                        onDelete = viewModel::deleteNotification,
                        onClear = viewModel::clearSavedNotifications,
                            )
                },
            )
            }
        }
    }

    val confirmation = notifications.firstOrNull { it.notification.id == confirmationId }
    if (confirmation?.draft != null && confirmation.notification.analysisStatus == AnalysisStatus.READY) {
        TransactionConfirmationDialog(
            item = confirmation,
            onDismiss = { viewModel.dismissConfirmation(confirmation.notification.id) },
            onConfirm = { direction, amount, recipient, purpose, transactionTime ->
                viewModel.confirmTransaction(
                    confirmation.notification,
                    direction,
                    amount,
                    recipient,
                    purpose,
                    transactionTime,
                )
            },
        )
    }
}
