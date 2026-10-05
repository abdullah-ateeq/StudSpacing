package com.example.studspacing.ui.calculator

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studspacing.data.AppSettings
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.StudSpacingMode
import com.example.studspacing.domain.StudSpacingResult
import com.example.studspacing.domain.formatMeasurement
import com.example.studspacing.domain.formatNumber
import com.example.studspacing.domain.formatSpacing
import com.example.studspacing.domain.isExactAt
import com.example.studspacing.domain.spokenMeasurement
import com.example.studspacing.ui.CalculatorForm
import com.example.studspacing.ui.CalculatorUiState
import com.example.studspacing.ui.InputField
import com.example.studspacing.ui.StudSpacingViewModel
import com.example.studspacing.ui.components.DisclaimerCard
import com.example.studspacing.ui.components.NumberField
import com.example.studspacing.ui.components.SectionLabel
import com.example.studspacing.ui.components.SegmentedControl
import com.example.studspacing.ui.components.UnitSelector
import com.example.studspacing.ui.theme.NumericStyle
import com.example.studspacing.ui.theme.OverlineStyle
import kotlinx.coroutines.launch

private val QUICK_SPACINGS = listOf("12", "16", "24")

@Composable
fun CalculatorScreen(
    viewModel: StudSpacingViewModel,
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    haptic: (HapticFeedbackType) -> Unit,
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    fun copy(text: String, message: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("StudSpacing", text)))
            snackbarHostState.showSnackbar(message)
        }
    }

    val actions = ResultActions(
        onCopyResult = { result ->
            copy(buildResultText(state.form, result, settings.decimalPlaces), "Result copied")
        },
        onCopyPositions = { result ->
            copy(buildPositionsText(state.form, result, settings.decimalPlaces), "Positions copied")
        },
        onSave = {
            if (viewModel.saveCurrent()) {
                haptic(HapticFeedbackType.Confirm)
                scope.launch { snackbarHostState.showSnackbar("Calculation saved") }
            }
        },
        onReset = {
            haptic(HapticFeedbackType.ContextClick)
            focusManager.clearFocus()
            viewModel.reset()
            scope.launch { snackbarHostState.showSnackbar("Calculator reset") }
        }
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    Modifier
                        .weight(0.44f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Header()
                    CalculatorInputs(state, settings, viewModel, haptic, onCalculate = {
                        haptic(HapticFeedbackType.ContextClick)
                        keyboard?.hide()
                        focusManager.clearFocus()
                    })
                    DisclaimerCard()
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(0.56f),
                    contentPadding = PaddingValues(start = 8.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    resultSection(state, settings, actions)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "header") { Header() }
                item(key = "inputs") {
                    CalculatorInputs(state, settings, viewModel, haptic, onCalculate = {
                        haptic(HapticFeedbackType.ContextClick)
                        keyboard?.hide()
                        focusManager.clearFocus()
                        scope.launch { listState.animateScrollToItem(2) }
                    })
                }
                resultSection(state, settings, actions)
                item(key = "disclaimer") { DisclaimerCard() }
            }
        }
    }
}

private class ResultActions(
    val onCopyResult: (StudSpacingResult) -> Unit,
    val onCopyPositions: (StudSpacingResult) -> Unit,
    val onSave: () -> Unit,
    val onReset: () -> Unit
)

@Composable
private fun Header() {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "STUDSPACING",
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "Calculate stud count and actual spacing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Inputs

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculatorInputs(
    state: CalculatorUiState,
    settings: AppSettings,
    viewModel: StudSpacingViewModel,
    haptic: (HapticFeedbackType) -> Unit,
    onCalculate: () -> Unit
) {
    val form = state.form
    val errors = state.fieldErrors
    val unit = form.spacingUnit

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedControl(
            options = StudSpacingMode.entries,
            selected = form.mode,
            label = { it.label },
            onSelect = {
                if (it != form.mode) haptic(HapticFeedbackType.SegmentTick)
                viewModel.setMode(it)
            }
        )

        NumberField(
            value = form.wallLength,
            onValueChange = viewModel::setWallLength,
            label = "Wall length",
            error = errors[InputField.WALL],
            trailing = {
                UnitSelector(form.wallUnit, MeasurementUnit.wallUnits, viewModel::setWallUnit, "Wall length unit")
            }
        )

        when (form.mode) {
            StudSpacingMode.MAX_SPACING -> {
                NumberField(
                    value = form.maximumSpacing,
                    onValueChange = viewModel::setMaximumSpacing,
                    label = "Maximum stud spacing",
                    error = errors[InputField.SPACING],
                    supportingText = "Layout limit. Actual spacing will not exceed this value.",
                    trailing = {
                        UnitSelector(unit, MeasurementUnit.spacingUnits, viewModel::setSpacingUnit, "Spacing unit")
                    }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.Center,
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick entry",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    QUICK_SPACINGS.forEach { value ->
                        FilterChip(
                            selected = form.maximumSpacing == value && unit == MeasurementUnit.INCH,
                            onClick = { viewModel.setQuickSpacing(value, MeasurementUnit.INCH) },
                            label = { Text("$value in") }
                        )
                    }
                }
            }
            StudSpacingMode.STUD_COUNT -> {
                NumberField(
                    value = form.studCount,
                    onValueChange = viewModel::setStudCount,
                    label = "Number of studs",
                    decimal = false,
                    error = errors[InputField.STUD_COUNT],
                    supportingText = "Stud positions including both ends. 10 studs = 9 spaces."
                )
                NumberField(
                    value = form.compareMaximum,
                    onValueChange = viewModel::setCompareMaximum,
                    label = "Maximum spacing (optional)",
                    error = errors[InputField.COMPARE_MAXIMUM],
                    supportingText = "Compare the result against a spacing limit.",
                    trailing = {
                        UnitSelector(unit, MeasurementUnit.spacingUnits, viewModel::setSpacingUnit, "Spacing unit")
                    }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = form.equalOffsets,
                    role = Role.Switch,
                    onValueChange = viewModel::setEqualOffsets
                )
                .padding(vertical = 4.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Equal end offsets", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Apply one offset to both ends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = form.equalOffsets, onCheckedChange = null)
        }

        val offsetError = errors[InputField.END_OFFSET] ?: errors[InputField.START_OFFSET]
        if (form.equalOffsets) {
            NumberField(
                value = form.endOffset,
                onValueChange = viewModel::setEndOffset,
                label = "End offset (each end)",
                error = offsetError,
                suffix = unit.symbol,
                imeAction = ImeAction.Done
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = form.startOffset,
                    onValueChange = viewModel::setStartOffset,
                    label = "Start offset",
                    error = errors[InputField.START_OFFSET],
                    suffix = unit.symbol,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = form.endOffset,
                    onValueChange = viewModel::setEndOffset,
                    label = "End offset",
                    error = errors[InputField.END_OFFSET],
                    suffix = unit.symbol,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            if (form.startOffset.isNotBlank() && form.startOffset.toBigDecimalOrNull()?.signum() == 1) {
                "Stud positions are measured from the wall start."
            } else {
                "Positions are measured from the start of the wall."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onCalculate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Calculate", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// endregion

// region Results

private fun LazyListScope.resultSection(state: CalculatorUiState, settings: AppSettings, actions: ResultActions) {
    val result = state.result
    if (result == null) {
        item(key = "empty") { EmptyResult(state) }
        return
    }
    val form = state.form
    val decimals = settings.decimalPlaces

    item(key = "primary") { PrimaryResult(form, result, decimals) }
    item(key = "actions") { ActionRow(result, actions) }
    if (settings.showVisualization) {
        item(key = "diagram") {
            ResultCard {
                SectionLabel("Wall layout")
                Spacer(Modifier.height(12.dp))
                WallDiagram(
                    result = result,
                    wallLengthText = form.wallLength,
                    wallUnit = form.wallUnit,
                    spacingUnit = form.spacingUnit,
                    decimals = decimals,
                    orientation = settings.orientation
                )
            }
        }
    }
    item(key = "breakdown") { Breakdown(form, result, decimals, settings.showFormula) }
    if (settings.showPositionList) {
        item(key = "positions-header") {
            Column(Modifier.padding(top = 4.dp)) {
                SectionLabel("Stud positions", Modifier.semantics { heading() })
                Text(
                    "Positions are measured from the start of the wall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(result.positions, key = { "stud-${it.index}" }) { position ->
            val value = formatMeasurement(position.positionMm, form.spacingUnit, decimals)
            val spoken = spokenMeasurement(position.positionMm, form.spacingUnit, decimals)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "Stud ${position.index} at $spoken" }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "STUD ${position.index}",
                        style = OverlineStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        value,
                        style = NumericStyle.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun EmptyResult(state: CalculatorUiState) {
    val message = state.fieldErrors.values.firstOrNull() ?: "Enter wall dimensions to see the layout."
    ResultCard {
        SectionLabel("Result")
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun PrimaryResult(form: CalculatorForm, result: StudSpacingResult, decimals: Int) {
    val unit = form.spacingUnit
    val maximum = result.maximumSpacingMm
    val spacingText = formatSpacing(result.actualSpacingMm, maximum, unit, decimals)
    val exact = isExactAt(result.actualSpacingMm, unit, decimals)
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Column(Modifier.clearAndSetSemantics {
                contentDescription = "${result.studCount} stud positions"
            }) {
                Text("STUD POSITIONS", style = OverlineStyle)
                Text(
                    result.studCount.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Normal)
                )
            }
            Text(
                "${result.intervals} intervals + 1 = ${result.studCount} studs, one at each end of the layout",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Intervals", result.intervals.toString(), "${result.intervals} intervals")
                Metric(
                    if (exact) "Exact spacing" else "Actual spacing",
                    spacingText,
                    "Actual spacing ${spokenMeasurement(result.actualSpacingMm, unit, decimals)}"
                )
            }

            if (maximum != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Metric("Maximum", formatMeasurement(maximum, unit, decimals, trimZeros = true), null, small = true)
                    Metric("Actual", spacingText, null, small = true)
                }
                Spacer(Modifier.height(10.dp))
                val exceeds = result.exceedsMaximum
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (exceeds) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (exceeds) colors.error else colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        if (exceeds) "Selected stud count creates spacing greater than the maximum."
                        else "Status: within maximum spacing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (exceeds) colors.error else colors.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, spoken: String?, small: Boolean = false) {
    Column(
        Modifier
            .widthIn(min = 72.dp)
            .then(if (spoken != null) Modifier.clearAndSetSemantics { contentDescription = spoken } else Modifier)
    ) {
        Text(label.uppercase(), style = OverlineStyle)
        Text(
            value,
            style = if (small) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(result: StudSpacingResult, actions: ResultActions) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalButton(
            onClick = actions.onSave,
            modifier = Modifier.semantics { contentDescription = "Save calculation" }
        ) { Text("Save") }
        OutlinedButton(
            onClick = { actions.onCopyResult(result) },
            modifier = Modifier.semantics { contentDescription = "Copy result" }
        ) { Text("Copy result") }
        OutlinedButton(
            onClick = { actions.onCopyPositions(result) },
            modifier = Modifier.semantics { contentDescription = "Copy positions" }
        ) { Text("Copy positions") }
        TextButton(
            onClick = actions.onReset,
            modifier = Modifier.semantics { contentDescription = "Reset calculator" }
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Reset")
        }
    }
}

@Composable
private fun Breakdown(form: CalculatorForm, result: StudSpacingResult, decimals: Int, showFormula: Boolean) {
    val unit = form.spacingUnit
    fun m(valueMm: java.math.BigDecimal) = formatMeasurement(valueMm, unit, decimals, trimZeros = true)
    fun n(valueMm: java.math.BigDecimal) = formatNumber(valueMm, unit, decimals, trimZeros = true)
    val spacingText = formatSpacing(result.actualSpacingMm, result.maximumSpacingMm, unit, decimals)

    val rows = buildList {
        add("Wall length" to "${form.wallLength} ${form.wallUnit.symbol}")
        add("Start offset" to m(result.startOffsetMm))
        add("End offset" to m(result.endOffsetMm))
        add("Usable length" to m(result.usableLengthMm))
        when (form.mode) {
            StudSpacingMode.MAX_SPACING -> result.maximumSpacingMm?.let { add("Maximum spacing" to m(it)) }
            StudSpacingMode.STUD_COUNT -> {
                add("Requested studs" to result.studCount.toString())
                result.maximumSpacingMm?.let { add("Maximum spacing" to m(it)) }
            }
        }
        add("Stud positions" to result.studCount.toString())
        add("Intervals" to result.intervals.toString())
        add("Actual spacing" to spacingText)
    }

    ResultCard {
        SectionLabel("Breakdown")
        Spacer(Modifier.height(8.dp))
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    label.uppercase(),
                    style = OverlineStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(value, style = NumericStyle)
            }
        }

        if (showFormula) {
            val usable = n(result.usableLengthMm)
            val lines = buildList {
                add("usable = ${n(result.wallLengthMm)} − ${n(result.startOffsetMm)} − ${n(result.endOffsetMm)} = $usable ${unit.symbol}")
                if (form.mode == StudSpacingMode.MAX_SPACING && result.maximumSpacingMm != null) {
                    add("intervals = ⌈$usable ÷ ${n(result.maximumSpacingMm)}⌉ = ${result.intervals}")
                    add("studs = ${result.intervals} + 1 = ${result.studCount}")
                } else {
                    add("intervals = ${result.studCount} − 1 = ${result.intervals}")
                }
                add("spacing = $usable ÷ ${result.intervals} = $spacingText")
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel("Formula")
                    lines.forEach {
                        Text(it, style = NumericStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize))
                    }
                }
            }
        }
    }
}

// endregion
