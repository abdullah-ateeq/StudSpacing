package com.example.studspacing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.studspacing.data.AppSettings
import com.example.studspacing.data.HistoryEntry
import com.example.studspacing.data.HistoryRepository
import com.example.studspacing.data.SettingsRepository
import com.example.studspacing.data.historyDataStore
import com.example.studspacing.data.settingsDataStore
import com.example.studspacing.domain.LayoutError
import com.example.studspacing.domain.LayoutOutcome
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.StudSpacingCalculator
import com.example.studspacing.domain.StudSpacingInput
import com.example.studspacing.domain.StudSpacingMode
import com.example.studspacing.domain.StudSpacingResult
import com.example.studspacing.domain.convert
import com.example.studspacing.domain.parseDecimal
import com.example.studspacing.domain.toInputText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/** Raw text the user typed plus unit choices. Offsets always share the spacing unit. */
data class CalculatorForm(
    val mode: StudSpacingMode = StudSpacingMode.MAX_SPACING,
    val wallLength: String = "12",
    val wallUnit: MeasurementUnit = MeasurementUnit.FOOT,
    val maximumSpacing: String = "16",
    val compareMaximum: String = "",
    val spacingUnit: MeasurementUnit = MeasurementUnit.INCH,
    val studCount: String = "10",
    val startOffset: String = "0",
    val endOffset: String = "0",
    val equalOffsets: Boolean = false
) {
    val offsetUnit: MeasurementUnit get() = spacingUnit

    companion object {
        fun fromSettings(s: AppSettings) = CalculatorForm(
            mode = s.defaultMode,
            wallLength = s.defaultWallLength,
            wallUnit = s.defaultWallUnit,
            maximumSpacing = s.defaultMaximumSpacing,
            spacingUnit = s.defaultSpacingUnit,
            startOffset = s.defaultStartOffset,
            endOffset = s.defaultEndOffset
        )
    }
}

enum class InputField { WALL, SPACING, STUD_COUNT, START_OFFSET, END_OFFSET, COMPARE_MAXIMUM }

data class CalculatorUiState(
    val form: CalculatorForm = CalculatorForm(),
    val result: StudSpacingResult? = null,
    val fieldErrors: Map<InputField, String> = emptyMap()
)

class StudSpacingViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<HistoryEntry>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val form = MutableStateFlow(CalculatorForm())

    val uiState: StateFlow<CalculatorUiState> = form
        .map(::evaluate)
        .stateIn(viewModelScope, SharingStarted.Eagerly, evaluate(form.value))

    init {
        viewModelScope.launch {
            val defaults = CalculatorForm.fromSettings(settingsRepository.settings.first())
            // Only apply saved defaults if the user has not already edited or restored a form.
            form.compareAndSet(CalculatorForm(), defaults)
        }
    }

    // region Input events

    fun setMode(mode: StudSpacingMode) = form.update { it.copy(mode = mode) }

    fun setWallLength(text: String) = form.update { it.copy(wallLength = sanitizeDecimal(text)) }

    fun setMaximumSpacing(text: String) = form.update { it.copy(maximumSpacing = sanitizeDecimal(text)) }

    fun setCompareMaximum(text: String) = form.update { it.copy(compareMaximum = sanitizeDecimal(text)) }

    fun setStudCount(text: String) = form.update { it.copy(studCount = text.filter(Char::isDigit).take(4)) }

    fun setStartOffset(text: String) = form.update { it.copy(startOffset = sanitizeDecimal(text)) }

    fun setEndOffset(text: String) = form.update {
        val value = sanitizeDecimal(text)
        it.copy(endOffset = value, startOffset = if (it.equalOffsets) value else it.startOffset)
    }

    fun setEqualOffsets(enabled: Boolean) = form.update {
        it.copy(equalOffsets = enabled, startOffset = if (enabled) it.endOffset else it.startOffset)
    }

    fun setQuickSpacing(value: String, unit: MeasurementUnit) = form.update { f ->
        // Quick values are given in [unit]; convert so the offsets keep their physical size.
        f.copy(
            maximumSpacing = value,
            spacingUnit = unit,
            startOffset = convertText(f.startOffset, f.spacingUnit, unit),
            endOffset = convertText(f.endOffset, f.spacingUnit, unit),
            compareMaximum = convertText(f.compareMaximum, f.spacingUnit, unit)
        )
    }

    /** Changing a unit converts the entered value so the physical length is preserved. */
    fun setWallUnit(unit: MeasurementUnit) = form.update {
        it.copy(wallLength = convertText(it.wallLength, it.wallUnit, unit), wallUnit = unit)
    }

    fun setSpacingUnit(unit: MeasurementUnit) = form.update {
        it.copy(
            maximumSpacing = convertText(it.maximumSpacing, it.spacingUnit, unit),
            compareMaximum = convertText(it.compareMaximum, it.spacingUnit, unit),
            startOffset = convertText(it.startOffset, it.spacingUnit, unit),
            endOffset = convertText(it.endOffset, it.spacingUnit, unit),
            spacingUnit = unit
        )
    }

    fun reset() {
        form.value = CalculatorForm.fromSettings(settings.value ?: AppSettings())
    }

    // endregion

    // region History

    /** Returns false when there is no valid result to save. */
    fun saveCurrent(): Boolean {
        val state = uiState.value
        val result = state.result ?: return false
        val f = state.form
        val now = System.currentTimeMillis()
        val entry = HistoryEntry(
            id = now,
            timestampMillis = now,
            mode = f.mode,
            wallLength = f.wallLength,
            wallUnit = f.wallUnit,
            maximumSpacing = if (f.mode == StudSpacingMode.MAX_SPACING) f.maximumSpacing else f.compareMaximum,
            spacingUnit = f.spacingUnit,
            studCount = if (f.mode == StudSpacingMode.STUD_COUNT) f.studCount else result.studCount.toString(),
            startOffset = f.startOffset,
            endOffset = f.endOffset,
            offsetUnit = f.offsetUnit,
            equalOffsets = f.equalOffsets,
            actualSpacingMm = result.actualSpacingMm.storageScale(),
            positionsMm = result.positions.map { it.positionMm.storageScale() }
        )
        viewModelScope.launch { historyRepository.add(entry) }
        return true
    }

    fun restore(entry: HistoryEntry) {
        form.value = CalculatorForm(
            mode = entry.mode,
            wallLength = entry.wallLength,
            wallUnit = entry.wallUnit,
            maximumSpacing = if (entry.mode == StudSpacingMode.MAX_SPACING) entry.maximumSpacing else form.value.maximumSpacing,
            compareMaximum = if (entry.mode == StudSpacingMode.STUD_COUNT) entry.maximumSpacing else "",
            spacingUnit = entry.spacingUnit,
            studCount = entry.studCount,
            startOffset = convertText(entry.startOffset, entry.offsetUnit, entry.spacingUnit),
            endOffset = convertText(entry.endOffset, entry.offsetUnit, entry.spacingUnit),
            equalOffsets = entry.equalOffsets
        )
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.reset() }
    }

    // endregion

    companion object {
        private const val MAX_INPUT_LENGTH = 12

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                StudSpacingViewModel(
                    SettingsRepository(app.settingsDataStore),
                    HistoryRepository(app.historyDataStore)
                )
            }
        }

        /** Keeps digits and a single decimal separator; never allows a minus sign. */
        fun sanitizeDecimal(text: String): String {
            val builder = StringBuilder()
            var seenSeparator = false
            for (c in text) {
                when {
                    c.isDigit() -> builder.append(c)
                    (c == '.' || c == ',') && !seenSeparator -> {
                        builder.append('.')
                        seenSeparator = true
                    }
                }
            }
            return builder.toString().take(MAX_INPUT_LENGTH)
        }

        fun convertText(text: String, from: MeasurementUnit, to: MeasurementUnit): String {
            if (from == to) return text
            val value = parseDecimal(text) ?: return text
            return toInputText(convert(value, from, to))
        }

        private fun BigDecimal.storageScale(): BigDecimal =
            setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()

        /** Turns the raw form into field errors and, when everything is valid, a result. */
        fun evaluate(f: CalculatorForm): CalculatorUiState {
            val errors = mutableMapOf<InputField, String>()

            val wall = parseDecimal(f.wallLength)
            when {
                wall == null -> errors[InputField.WALL] = "Enter a wall length."
                wall.signum() <= 0 -> errors[InputField.WALL] = LayoutError.WALL_LENGTH_INVALID.message
            }

            var maximum: BigDecimal? = null
            var studCount = 0
            if (f.mode == StudSpacingMode.MAX_SPACING) {
                maximum = parseDecimal(f.maximumSpacing)
                if (maximum == null || maximum.signum() <= 0) {
                    errors[InputField.SPACING] = LayoutError.SPACING_INVALID.message
                }
            } else {
                val count = f.studCount.toIntOrNull()
                when {
                    f.studCount.isBlank() -> errors[InputField.STUD_COUNT] = "Enter the number of studs."
                    count == null -> errors[InputField.STUD_COUNT] = "Enter a whole number of studs."
                    count < StudSpacingCalculator.MIN_STUD_POSITIONS ->
                        errors[InputField.STUD_COUNT] = LayoutError.STUD_COUNT_TOO_LOW.message
                    count > StudSpacingCalculator.MAX_STUD_POSITIONS ->
                        errors[InputField.STUD_COUNT] = LayoutError.STUD_COUNT_TOO_HIGH.message
                    else -> studCount = count
                }
                if (f.compareMaximum.isNotBlank()) {
                    maximum = parseDecimal(f.compareMaximum)
                    if (maximum == null || maximum.signum() <= 0) {
                        errors[InputField.COMPARE_MAXIMUM] = LayoutError.SPACING_INVALID.message
                        maximum = null
                    }
                }
            }

            val end = if (f.endOffset.isBlank()) BigDecimal.ZERO else parseDecimal(f.endOffset)
            val start = when {
                f.equalOffsets -> end
                f.startOffset.isBlank() -> BigDecimal.ZERO
                else -> parseDecimal(f.startOffset)
            }
            if (end == null) errors[InputField.END_OFFSET] = "Enter a valid offset."
            if (start == null) errors[InputField.START_OFFSET] = "Enter a valid offset."

            if (wall == null || start == null || end == null || errors.keys.any { it != InputField.COMPARE_MAXIMUM }) {
                return CalculatorUiState(f, null, errors)
            }

            val input = StudSpacingInput(
                mode = f.mode,
                wallLength = wall,
                wallUnit = f.wallUnit,
                maximumSpacing = maximum,
                spacingUnit = f.spacingUnit,
                studCount = studCount,
                startOffset = start,
                endOffset = end,
                offsetUnit = f.offsetUnit,
                equalOffsets = f.equalOffsets
            )
            return when (val outcome = StudSpacingCalculator.calculate(input)) {
                is LayoutOutcome.Success -> CalculatorUiState(f, outcome.result, errors)
                is LayoutOutcome.Invalid -> {
                    errors[fieldFor(outcome.error)] = outcome.error.message
                    CalculatorUiState(f, null, errors)
                }
            }
        }

        private fun fieldFor(error: LayoutError): InputField = when (error) {
            LayoutError.WALL_LENGTH_INVALID -> InputField.WALL
            LayoutError.SPACING_INVALID, LayoutError.TOO_MANY_STUDS -> InputField.SPACING
            LayoutError.STUD_COUNT_TOO_LOW, LayoutError.STUD_COUNT_TOO_HIGH -> InputField.STUD_COUNT
            LayoutError.OFFSET_NEGATIVE, LayoutError.OFFSETS_TOO_LARGE, LayoutError.USABLE_LENGTH_INVALID ->
                InputField.END_OFFSET
        }
    }
}
