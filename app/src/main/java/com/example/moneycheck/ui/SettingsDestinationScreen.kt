package com.example.moneycheck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            TextButton(onClick = { onDestinationSelected(SettingsDestination.OVERVIEW) }) {
                Text("Quay lại Cài đặt")
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsDestination.entries.filter { it != SettingsDestination.OVERVIEW }.forEach { item ->
                    FilterChip(
                        selected = destination == item,
                        onClick = { onDestinationSelected(item) },
                        label = { Text(item.label) },
                    )
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
