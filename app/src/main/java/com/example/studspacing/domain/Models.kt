package com.example.studspacing.domain

import java.math.BigDecimal

enum class StudSpacingMode(val label: String) {
    MAX_SPACING("Max spacing"),
    STUD_COUNT("Stud count")
}

enum class MeasurementUnit(val symbol: String, val spokenName: String, val millimeters: BigDecimal) {
    MILLIMETER("mm", "millimeters", BigDecimal.ONE),
    CENTIMETER("cm", "centimeters", BigDecimal.TEN),
    METER("m", "meters", BigDecimal("1000")),
    INCH("in", "inches", BigDecimal("25.4")),
    FOOT("ft", "feet", BigDecimal("304.8")),
    YARD("yd", "yards", BigDecimal("914.4"));

    companion object {
        val wallUnits: List<MeasurementUnit> = entries
        val spacingUnits: List<MeasurementUnit> = listOf(MILLIMETER, CENTIMETER, METER, INCH, FOOT)
    }
}

enum class Orientation(val label: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical")
}

data class StudSpacingInput(
    val mode: StudSpacingMode,
    val wallLength: BigDecimal,
    val wallUnit: MeasurementUnit,
    val maximumSpacing: BigDecimal?,
    val spacingUnit: MeasurementUnit,
    val studCount: Int,
    val startOffset: BigDecimal,
    val endOffset: BigDecimal,
    val offsetUnit: MeasurementUnit,
    val equalOffsets: Boolean = false
)

data class StudPosition(
    val index: Int,
    val positionMm: BigDecimal
)

data class StudSpacingResult(
    val wallLengthMm: BigDecimal,
    val startOffsetMm: BigDecimal,
    val endOffsetMm: BigDecimal,
    val usableLengthMm: BigDecimal,
    val intervals: Int,
    val studCount: Int,
    val actualSpacingMm: BigDecimal,
    val maximumSpacingMm: BigDecimal?,
    val positions: List<StudPosition>
) {
    /** True when a maximum is known and the unrounded actual spacing is larger than it. */
    val exceedsMaximum: Boolean
        get() = maximumSpacingMm != null && actualSpacingMm > maximumSpacingMm
}

enum class LayoutError(val message: String) {
    WALL_LENGTH_INVALID("Wall length must be greater than zero."),
    SPACING_INVALID("Maximum spacing must be greater than zero."),
    STUD_COUNT_TOO_LOW("Enter at least 2 stud positions."),
    STUD_COUNT_TOO_HIGH("Enter no more than ${StudSpacingCalculator.MAX_STUD_POSITIONS} stud positions."),
    OFFSET_NEGATIVE("Offsets cannot be negative."),
    OFFSETS_TOO_LARGE("Offsets must be smaller than the wall length."),
    USABLE_LENGTH_INVALID("Usable wall length must be greater than zero."),
    TOO_MANY_STUDS(
        "This layout needs more than ${StudSpacingCalculator.MAX_STUD_POSITIONS} stud positions. " +
            "Increase the maximum spacing."
    )
}

sealed interface LayoutOutcome {
    data class Success(val result: StudSpacingResult) : LayoutOutcome
    data class Invalid(val error: LayoutError) : LayoutOutcome
}
