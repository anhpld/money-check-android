package com.example.moneycheck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class SettingsDestination(val label: String) {
    OVERVIEW("Cài đặt"), INBOX("Hộp thư"), AUTO_MATCHED("Đã bắt"), SAVED("Đã lưu"),
}

/** Each secondary destination owns its saved drafts and scroll, just like a primary tab. */
@Composable
internal fun SettingsDestinationScreen(
    destination: SettingsDestination,
    onDestinationSelected: (SettingsDestination) -> Unit,
    overview: @Composable () -> Unit,
    inbox: @Composable () -> Unit,
    autoMatched: @Composable () -> Unit,
    saved: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateHolder = rememberSaveableStateHolder()
    BackHandler(enabled = destination != SettingsDestination.OVERVIEW) {
        onDestinationSelected(SettingsDestination.OVERVIEW)
    }
    Column(modifier.fillMaxSize()) {
        if (destination != SettingsDestination.OVERVIEW) {
            TextButton(
                onClick = { onDestinationSelected(SettingsDestination.OVERVIEW) },
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Text("←  Cài đặt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SettingsDestination.entries.filter { it != SettingsDestination.OVERVIEW }.forEach { item ->
                    Surface(
                        modifier = Modifier.weight(1f).height(33.dp).clickable { onDestinationSelected(item) },
                        color = if (destination == item) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text(item.label, fontSize = 11.sp, color = if (destination == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        stateHolder.SaveableStateProvider(destination.name) {
            when (destination) {
                SettingsDestination.OVERVIEW -> overview()
                SettingsDestination.INBOX -> inbox()
                SettingsDestination.AUTO_MATCHED -> autoMatched()
                SettingsDestination.SAVED -> saved()
            }
        }
    }
}
