package com.example.studspacing.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.studspacing.data.AppSettings
import com.example.studspacing.data.ThemeMode
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.Orientation
import com.example.studspacing.domain.StudSpacingMode
import com.example.studspacing.domain.parseDecimal
import com.example.studspacing.ui.StudSpacingViewModel
import com.example.studspacing.ui.components.NumberField
import com.example.studspacing.ui.components.SectionLabel
import com.example.studspacing.ui.components.SegmentedControl
import com.example.studspacing.ui.components.UnitSelector

@Composable
fun SettingsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClearHistory: () -> Unit
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

            Section("Appearance")
            Labeled("Theme") {
                SegmentedControl(ThemeMode.entries, settings.themeMode, { it.label }, { mode ->
                    onUpdate { it.copy(themeMode = mode) }
                })
            }
            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = "Dynamic color",
                subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or later",
                checked = settings.dynamicColor && dynamicSupported,
                enabled = dynamicSupported,
                onChange = { value -> onUpdate { it.copy(dynamicColor = value) } }
            )

            Section("Calculator defaults")
            Labeled("Default mode") {
                SegmentedControl(StudSpacingMode.entries, settings.defaultMode, { it.label }, { mode ->
                    onUpdate { it.copy(defaultMode = mode) }
                })
            }
            UnitRow("Default wall unit", settings.defaultWallUnit, MeasurementUnit.wallUnits) { unit ->
                onUpdate { it.copy(defaultWallUnit = unit) }
            }
            UnitRow("Default spacing unit", settings.defaultSpacingUnit, MeasurementUnit.spacingUnits) { unit ->
                onUpdate { it.copy(defaultSpacingUnit = unit) }
            }
            DefaultValueField(
                label = "Default wall length",
                stored = settings.defaultWallLength,
                suffix = settings.defaultWallUnit.symbol,
                allowZero = false
            ) { v -> onUpdate { it.copy(defaultWallLength = v) } }
            DefaultValueField(
                label = "Default maximum spacing",
                stored = settings.defaultMaximumSpacing,
                suffix = settings.defaultSpacingUnit.symbol,
                allowZero = false
            ) { v -> onUpdate { it.copy(defaultMaximumSpacing = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DefaultValueField(
                    label = "Default start offset",
                    stored = settings.defaultStartOffset,
                    suffix = settings.defaultSpacingUnit.symbol,
                    allowZero = true,
                    modifier = Modifier.weight(1f)
                ) { v -> onUpdate { it.copy(defaultStartOffset = v) } }
                DefaultValueField(
                    label = "Default end offset",
                    stored = settings.defaultEndOffset,
                    suffix = settings.defaultSpacingUnit.symbol,
                    allowZero = true,
                    modifier = Modifier.weight(1f)
                ) { v -> onUpdate { it.copy(defaultEndOffset = v) } }
            }
            Text(
                "Defaults apply to new sessions and to Reset on the calculator.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Section("Display")
            Labeled("Diagram orientation") {
                SegmentedControl(Orientation.entries, settings.orientation, { it.label }, { o ->
                    onUpdate { it.copy(orientation = o) }
                })
            }
            Labeled("Decimal places") {
                SegmentedControl(AppSettings.DECIMAL_OPTIONS, settings.decimalPlaces, { it.toString() }, { d ->
                    onUpdate { it.copy(decimalPlaces = d) }
                })
            }
            SwitchRow("Show visualization", "Wall diagram on the calculator", settings.showVisualization) { v ->
                onUpdate { it.copy(showVisualization = v) }
            }
            SwitchRow("Show formula", "Step-by-step calculation in the breakdown", settings.showFormula) { v ->
                onUpdate { it.copy(showFormula = v) }
            }
            SwitchRow("Show position list", "Every stud position from the wall start", settings.showPositionList) { v ->
                onUpdate { it.copy(showPositionList = v) }
            }
            SwitchRow("Haptic feedback", "Light feedback on mode change, calculate, save and reset", settings.hapticFeedback) { v ->
                onUpdate { it.copy(hapticFeedback = v) }
            }

            Section("Data")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.weight(1f)) {
                    Text("Reset settings")
                }
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) {
                    Text("Clear history")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults. Saved history is kept.",
            confirm = "Reset",
            onConfirm = onResetSettings,
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear history?",
            text = "All saved calculations will be removed. This cannot be undone.",
            confirm = "Clear",
            onConfirm = onClearHistory,
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun Section(title: String) {
    Column(Modifier.padding(top = 10.dp)) {
        SectionLabel(title, Modifier.semantics { heading() })
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun UnitRow(
    label: String,
    selected: MeasurementUnit,
    units: List<MeasurementUnit>,
    onSelect: (MeasurementUnit) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        UnitSelector(selected, units, onSelect, label)
    }
}

/** Edits a stored default; only valid values are persisted. */
@Composable
private fun DefaultValueField(
    label: String,
    stored: String,
    suffix: String,
    allowZero: Boolean,
    modifier: Modifier = Modifier,
    onValid: (String) -> Unit
) {
    var text by rememberSaveable(stored) { mutableStateOf(stored) }
    val value = parseDecimal(text)
    val valid = value != null && (if (allowZero) value.signum() >= 0 else value.signum() > 0)
    NumberField(
        value = text,
        onValueChange = {
            text = StudSpacingViewModel.sanitizeDecimal(it)
            val parsed = parseDecimal(text)
            if (parsed != null && (if (allowZero) parsed.signum() >= 0 else parsed.signum() > 0)) onValid(text)
        },
        label = label,
        suffix = suffix,
        error = if (valid) null else if (allowZero) "Enter 0 or more." else "Enter a value greater than zero.",
        modifier = modifier
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
