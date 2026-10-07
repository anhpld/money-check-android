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

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TRANSACTIONS("Tổng quan", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PENDING("Duyệt", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    CHAT("Trợ lý", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    SETTINGS("Cài đặt", Icons.Filled.Settings, Icons.Outlined.Settings),
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == MainTab.PENDING && pendingConfirmations.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(pendingConfirmations.size.toString()) } }) {
                                    Icon(
                                        imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    )
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
