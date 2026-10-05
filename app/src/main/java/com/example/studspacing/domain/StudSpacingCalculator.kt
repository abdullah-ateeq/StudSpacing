package com.example.studspacing.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure layout math. Every value is in millimeters and kept at full precision;
 * rounding happens only when values are formatted for display.
 */
object StudSpacingCalculator {

    const val MIN_STUD_POSITIONS = 2
    const val MAX_STUD_POSITIONS = 1000

    fun calculate(input: StudSpacingInput): LayoutOutcome {
        val wallMm = toMillimeters(input.wallLength, input.wallUnit)
        val endMm = toMillimeters(input.endOffset, input.offsetUnit)
        val startMm = if (input.equalOffsets) endMm else toMillimeters(input.startOffset, input.offsetUnit)
        val maxMm = input.maximumSpacing?.let { toMillimeters(it, input.spacingUnit) }
        return when (input.mode) {
            StudSpacingMode.MAX_SPACING -> {
                if (maxMm == null) LayoutOutcome.Invalid(LayoutError.SPACING_INVALID)
                else calculateMaxSpacingLayout(wallMm, maxMm, startMm, endMm)
            }
            StudSpacingMode.STUD_COUNT ->
                calculateStudCountLayout(wallMm, input.studCount, startMm, endMm, maxMm)
        }
    }

    fun calculateMaxSpacingLayout(
        wallLengthMm: BigDecimal,
        maximumSpacingMm: BigDecimal,
        startOffsetMm: BigDecimal,
        endOffsetMm: BigDecimal
    ): LayoutOutcome {
        validateWallAndOffsets(wallLengthMm, startOffsetMm, endOffsetMm)?.let { return LayoutOutcome.Invalid(it) }
        if (maximumSpacingMm.signum() <= 0) return LayoutOutcome.Invalid(LayoutError.SPACING_INVALID)

        val usable = calculateUsableLength(wallLengthMm, startOffsetMm, endOffsetMm)
        val intervalsDecimal = usable.divide(maximumSpacingMm, 0, RoundingMode.CEILING)
        if (intervalsDecimal >= BigDecimal(MAX_STUD_POSITIONS)) {
            return LayoutOutcome.Invalid(LayoutError.TOO_MANY_STUDS)
        }
        val intervals = intervalsDecimal.toInt().coerceAtLeast(1)
        return LayoutOutcome.Success(
            buildResult(wallLengthMm, startOffsetMm, endOffsetMm, usable, intervals, maximumSpacingMm)
        )
    }

    fun calculateStudCountLayout(
        wallLengthMm: BigDecimal,
        studCount: Int,
        startOffsetMm: BigDecimal,
        endOffsetMm: BigDecimal,
        maximumSpacingMm: BigDecimal? = null
    ): LayoutOutcome {
        validateWallAndOffsets(wallLengthMm, startOffsetMm, endOffsetMm)?.let { return LayoutOutcome.Invalid(it) }
        if (studCount < MIN_STUD_POSITIONS) return LayoutOutcome.Invalid(LayoutError.STUD_COUNT_TOO_LOW)
        if (studCount > MAX_STUD_POSITIONS) return LayoutOutcome.Invalid(LayoutError.STUD_COUNT_TOO_HIGH)
        if (maximumSpacingMm != null && maximumSpacingMm.signum() <= 0) {
            return LayoutOutcome.Invalid(LayoutError.SPACING_INVALID)
        }

        val usable = calculateUsableLength(wallLengthMm, startOffsetMm, endOffsetMm)
        return LayoutOutcome.Success(
            buildResult(wallLengthMm, startOffsetMm, endOffsetMm, usable, studCount - 1, maximumSpacingMm)
        )
    }

    fun calculateUsableLength(
        wallLengthMm: BigDecimal,
        startOffsetMm: BigDecimal,
        endOffsetMm: BigDecimal
    ): BigDecimal = wallLengthMm.subtract(startOffsetMm).subtract(endOffsetMm)

    /** Number of equal spaces needed so that no space exceeds [maximumSpacingMm]. */
    fun calculateIntervals(usableLengthMm: BigDecimal, maximumSpacingMm: BigDecimal): Int {
        require(usableLengthMm.signum() > 0) { "Usable length must be positive" }
        require(maximumSpacingMm.signum() > 0) { "Maximum spacing must be positive" }
        return usableLengthMm.divide(maximumSpacingMm, 0, RoundingMode.CEILING).toInt()
    }

    fun calculateActualSpacing(usableLengthMm: BigDecimal, intervals: Int): BigDecimal {
        require(intervals > 0) { "Intervals must be positive" }
        return usableLengthMm.divide(BigDecimal(intervals), CALC_CONTEXT)
    }

    /**
     * Each position is computed as start + usable × i ÷ intervals rather than by
     * repeated addition, so the final stud lands exactly on wallLength − endOffset.
     */
    fun calculateStudPositions(
        startOffsetMm: BigDecimal,
        usableLengthMm: BigDecimal,
        intervals: Int
    ): List<StudPosition> {
        require(intervals > 0) { "Intervals must be positive" }
        val divisor = BigDecimal(intervals)
        return (0..intervals).map { i ->
            val along = when (i) {
                0 -> BigDecimal.ZERO
                intervals -> usableLengthMm
                else -> usableLengthMm.multiply(BigDecimal(i)).divide(divisor, CALC_CONTEXT)
            }
            StudPosition(index = i + 1, positionMm = startOffsetMm.add(along))
        }
    }

    private fun validateWallAndOffsets(
        wallLengthMm: BigDecimal,
        startOffsetMm: BigDecimal,
        endOffsetMm: BigDecimal
    ): LayoutError? = when {
        wallLengthMm.signum() <= 0 -> LayoutError.WALL_LENGTH_INVALID
        startOffsetMm.signum() < 0 || endOffsetMm.signum() < 0 -> LayoutError.OFFSET_NEGATIVE
        startOffsetMm.signum() == 0 && endOffsetMm.signum() == 0 -> null
        startOffsetMm.add(endOffsetMm) >= wallLengthMm -> LayoutError.OFFSETS_TOO_LARGE
        calculateUsableLength(wallLengthMm, startOffsetMm, endOffsetMm).signum() <= 0 ->
            LayoutError.USABLE_LENGTH_INVALID
        else -> null
    }

    private fun buildResult(
        wallLengthMm: BigDecimal,
        startOffsetMm: BigDecimal,
        endOffsetMm: BigDecimal,
        usable: BigDecimal,
        intervals: Int,
        maximumSpacingMm: BigDecimal?
    ) = StudSpacingResult(
        wallLengthMm = wallLengthMm,
        startOffsetMm = startOffsetMm,
        endOffsetMm = endOffsetMm,
        usableLengthMm = usable,
        intervals = intervals,
        studCount = intervals + 1,
        actualSpacingMm = calculateActualSpacing(usable, intervals),
        maximumSpacingMm = maximumSpacingMm,
        positions = calculateStudPositions(startOffsetMm, usable, intervals)
    )
}
