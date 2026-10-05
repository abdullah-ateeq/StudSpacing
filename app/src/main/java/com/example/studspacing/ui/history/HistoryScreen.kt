package com.example.studspacing.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studspacing.data.HistoryEntry
import com.example.studspacing.data.HistoryRepository
import com.example.studspacing.domain.StudSpacingMode
import com.example.studspacing.domain.formatSpacing
import com.example.studspacing.domain.parseDecimal
import com.example.studspacing.domain.toMillimeters
import com.example.studspacing.ui.theme.NumericStyle
import com.example.studspacing.ui.theme.OverlineStyle
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    decimals: Int,
    contentPadding: PaddingValues,
    onOpen: (HistoryEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(
                        "${history.size} of ${HistoryRepository.MAX_RECORDS} saved calculations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                }
            }
        }
        if (history.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No saved calculations.\nUse Save on the calculator to keep a layout here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        items(history, key = { it.id }) { entry ->
            HistoryItem(entry, decimals, onOpen = { onOpen(entry) }, onDelete = { onDelete(entry.id) })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All saved calculations will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryItem(entry: HistoryEntry, decimals: Int, onOpen: () -> Unit, onDelete: () -> Unit) {
    val unit = entry.spacingUnit
    val maxMm = parseDecimal(entry.maximumSpacing)?.takeIf { it.signum() > 0 }?.let { toMillimeters(it, unit) }
    val actual = formatSpacing(entry.actualSpacingMm, maxMm, unit, decimals, trimZeros = true)
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.timestampMillis))
    val lines = buildList {
        add("${entry.wallLength} ${entry.wallUnit.symbol} wall")
        if (entry.mode == StudSpacingMode.MAX_SPACING) add("${entry.maximumSpacing} ${unit.symbol} maximum")
        add("${entry.resultStudCount} studs")
        add("$actual actual")
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .clickable(onClickLabel = "Open in calculator", role = Role.Button, onClick = onOpen)
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.mode.label.uppercase(),
                    style = OverlineStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${entry.resultStudCount} studs · $actual",
                    style = NumericStyle.copy(fontWeight = FontWeight.SemiBold),
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                Text(
                    lines.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete saved calculation")
            }
        }
    }
}
