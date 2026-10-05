package com.example.studspacing.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class StudSpacingCalculatorTest {

    private fun inch(v: String) = toMillimeters(BigDecimal(v), MeasurementUnit.INCH)
    private fun feet(v: String) = toMillimeters(BigDecimal(v), MeasurementUnit.FOOT)
    private fun inInches(mm: BigDecimal) = fromMillimeters(mm, MeasurementUnit.INCH)
    private fun assertNumber(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual.stripTrailingZeros()))

    private fun success(outcome: LayoutOutcome): StudSpacingResult {
        assertTrue("Expected success but was $outcome", outcome is LayoutOutcome.Success)
        return (outcome as LayoutOutcome.Success).result
    }

    private fun error(outcome: LayoutOutcome): LayoutError {
        assertTrue("Expected error but was $outcome", outcome is LayoutOutcome.Invalid)
        return (outcome as LayoutOutcome.Invalid).error
    }

    private fun maxLayout(wall: BigDecimal, max: BigDecimal, start: BigDecimal = BigDecimal.ZERO, end: BigDecimal = BigDecimal.ZERO) =
        StudSpacingCalculator.calculateMaxSpacingLayout(wall, max, start, end)

    private fun countLayout(wall: BigDecimal, count: Int, start: BigDecimal = BigDecimal.ZERO, end: BigDecimal = BigDecimal.ZERO) =
        StudSpacingCalculator.calculateStudCountLayout(wall, count, start, end)

    // 1, 3: exact maximum spacing — 12 ft at 16 in max.
    @Test
    fun maxSpacing_exactDivision_12ftAt16in() {
        val r = success(maxLayout(feet("12"), inch("16")))
        assertEquals(9, r.intervals)
        assertEquals(10, r.studCount)
        assertNumber("16", inInches(r.actualSpacingMm))
    }

    // 2: non-exact — 10 ft at 16 in max gives 8 intervals of 15 in.
    @Test
    fun maxSpacing_nonExactDivision_10ftAt16in() {
        val r = success(maxLayout(feet("10"), inch("16")))
        assertEquals(8, r.intervals)
        assertEquals(9, r.studCount)
        assertNumber("15", inInches(r.actualSpacingMm))
        assertTrue(r.actualSpacingMm < inch("16"))
    }

    @Test
    fun maxSpacing_actualNeverExceedsMaximum() {
        val walls = listOf("1", "7.3", "99.99", "144.01", "1000", "0.4", "12345.678")
        val maxima = listOf("0.7", "1", "16", "15.999", "24", "3.3333")
        for (w in walls) for (m in maxima) {
            val outcome = maxLayout(inch(w), inch(m))
            if (outcome is LayoutOutcome.Success) {
                assertTrue("$w / $m", outcome.result.actualSpacingMm <= inch(m))
                assertFalse(outcome.result.exceedsMaximum)
            }
        }
    }

    // 4, 6: stud count mode — 12 ft with 10 studs.
    @Test
    fun studCount_12ftWith10Studs() {
        val r = success(countLayout(feet("12"), 10))
        assertEquals(9, r.intervals)
        assertEquals(10, r.studCount)
        assertNumber("16", inInches(r.actualSpacingMm))
    }

    // 5: N studs create N − 1 intervals; never wall ÷ N.
    @Test
    fun studCount_intervalsAreCountMinusOne() {
        for (n in listOf(2, 3, 10, 57, 1000)) {
            assertEquals(n - 1, success(countLayout(inch("120"), n)).intervals)
        }
        val r = success(countLayout(inch("120"), 6))
        assertNumber("24", inInches(r.actualSpacingMm))
    }

    @Test
    fun calculateIntervals_roundsUp() {
        assertEquals(9, StudSpacingCalculator.calculateIntervals(BigDecimal("144"), BigDecimal("16")))
        assertEquals(8, StudSpacingCalculator.calculateIntervals(BigDecimal("120"), BigDecimal("16")))
        assertEquals(1, StudSpacingCalculator.calculateIntervals(BigDecimal("5"), BigDecimal("16")))
    }

    @Test
    fun calculateActualSpacing_dividesUsableLength() {
        assertNumber("15", StudSpacingCalculator.calculateActualSpacing(BigDecimal("120"), 8))
        assertNumber("14", StudSpacingCalculator.calculateActualSpacing(BigDecimal("112"), 8))
    }

    // 7, 8: offsets — 120 in wall with 4 in each end and 9 studs.
    @Test
    fun offsets_startAndEnd() {
        val r = success(countLayout(inch("120"), 9, inch("4"), inch("4")))
        assertNumber("112", inInches(r.usableLengthMm))
        assertEquals(8, r.intervals)
        assertNumber("14", inInches(r.actualSpacingMm))
        val expected = listOf("4", "18", "32", "46", "60", "74", "88", "102", "116")
        assertEquals(expected.size, r.positions.size)
        expected.zip(r.positions).forEach { (e, p) -> assertNumber(e, inInches(p.positionMm)) }
    }

    @Test
    fun offsets_onlyStart() {
        val r = success(maxLayout(inch("100"), inch("16"), start = inch("4")))
        assertNumber("96", inInches(r.usableLengthMm))
        assertEquals(6, r.intervals)
        assertNumber("4", inInches(r.positions.first().positionMm))
        assertNumber("100", inInches(r.positions.last().positionMm))
    }

    @Test
    fun offsets_onlyEnd() {
        val r = success(maxLayout(inch("100"), inch("16"), end = inch("4")))
        assertNumber("0", inInches(r.positions.first().positionMm))
        assertNumber("96", inInches(r.positions.last().positionMm))
    }

    // 9: equal offsets apply the end offset to both ends.
    @Test
    fun equalOffsets_applyEndOffsetToBothEnds() {
        val input = StudSpacingInput(
            mode = StudSpacingMode.MAX_SPACING,
            wallLength = BigDecimal("120"),
            wallUnit = MeasurementUnit.INCH,
            maximumSpacing = BigDecimal("16"),
            spacingUnit = MeasurementUnit.INCH,
            studCount = 0,
            startOffset = BigDecimal("99"), // ignored when equal offsets are on
            endOffset = BigDecimal("4"),
            offsetUnit = MeasurementUnit.INCH,
            equalOffsets = true
        )
        val r = success(StudSpacingCalculator.calculate(input))
        assertNumber("4", inInches(r.startOffsetMm))
        assertNumber("4", inInches(r.endOffsetMm))
        assertNumber("112", inInches(r.usableLengthMm))
        assertEquals(7, r.intervals)
        assertNumber("16", inInches(r.actualSpacingMm))
    }

    // 10: invalid offsets.
    @Test
    fun offsets_tooLarge() {
        assertEquals(LayoutError.OFFSETS_TOO_LARGE, error(maxLayout(inch("120"), inch("16"), inch("60"), inch("60"))))
        assertEquals(LayoutError.OFFSETS_TOO_LARGE, error(maxLayout(inch("120"), inch("16"), inch("100"), inch("30"))))
        assertEquals(LayoutError.OFFSETS_TOO_LARGE, error(countLayout(inch("120"), 5, inch("120"), BigDecimal.ZERO)))
    }

    @Test
    fun offsets_negative() {
        assertEquals(LayoutError.OFFSET_NEGATIVE, error(maxLayout(inch("120"), inch("16"), inch("-1"), BigDecimal.ZERO)))
    }

    // 11
    @Test
    fun zeroWallLength() {
        assertEquals(LayoutError.WALL_LENGTH_INVALID, error(maxLayout(BigDecimal.ZERO, inch("16"))))
        assertEquals(LayoutError.WALL_LENGTH_INVALID, error(countLayout(BigDecimal.ZERO, 5)))
        assertEquals(LayoutError.WALL_LENGTH_INVALID, error(maxLayout(inch("-12"), inch("16"))))
    }

    // 12
    @Test
    fun zeroSpacing() {
        assertEquals(LayoutError.SPACING_INVALID, error(maxLayout(inch("120"), BigDecimal.ZERO)))
    }

    // 13
    @Test
    fun negativeSpacing() {
        assertEquals(LayoutError.SPACING_INVALID, error(maxLayout(inch("120"), inch("-16"))))
    }

    // 14
    @Test
    fun invalidStudCount() {
        assertEquals(LayoutError.STUD_COUNT_TOO_LOW, error(countLayout(inch("120"), 1)))
        assertEquals(LayoutError.STUD_COUNT_TOO_LOW, error(countLayout(inch("120"), 0)))
        assertEquals(LayoutError.STUD_COUNT_TOO_LOW, error(countLayout(inch("120"), -3)))
        assertEquals(LayoutError.STUD_COUNT_TOO_HIGH, error(countLayout(inch("120"), 1001)))
        assertEquals("Enter at least 2 stud positions.", LayoutError.STUD_COUNT_TOO_LOW.message)
    }

    @Test
    fun tooManyStudsForMaxSpacing() {
        assertEquals(LayoutError.TOO_MANY_STUDS, error(maxLayout(feet("1000"), inch("0.01"))))
        // 999 intervals = 1000 studs is still allowed.
        assertEquals(1000, success(maxLayout(inch("999"), inch("1"))).studCount)
    }

    // 21
    @Test
    fun decimalMeasurements() {
        val r = success(maxLayout(feet("12.5"), inch("16")))
        // 150 in / 16 = 9.375 -> 10 intervals of 15 in
        assertEquals(10, r.intervals)
        assertNumber("15", inInches(r.actualSpacingMm))

        val r2 = success(maxLayout(feet("25.75"), inch("16")))
        // 309 in / 16 = 19.3125 -> 20 intervals of 15.45 in
        assertEquals(20, r2.intervals)
        assertNumber("15.45", inInches(r2.actualSpacingMm))
    }

    // 22, 23, 24
    @Test
    fun positionGeneration_firstAndLast() {
        val r = success(countLayout(inch("120"), 7))
        val expected = listOf("0", "20", "40", "60", "80", "100", "120")
        expected.zip(r.positions).forEach { (e, p) -> assertNumber(e, inInches(p.positionMm)) }
        assertEquals(1, r.positions.first().index)
        assertEquals(7, r.positions.last().index)
        assertEquals(0, r.positions.first().positionMm.signum())
        assertEquals(0, r.positions.last().positionMm.compareTo(r.wallLengthMm))
    }

    @Test
    fun lastPositionIsExactForRepeatingSpacing() {
        // 100 / 3 never terminates; the last stud must still land exactly on wall − end offset.
        val r = success(countLayout(BigDecimal("100"), 4, BigDecimal("1"), BigDecimal("2")))
        assertEquals(0, r.positions.last().positionMm.compareTo(BigDecimal("98")))
        assertEquals(0, r.positions.first().positionMm.compareTo(BigDecimal("1")))
    }

    @Test
    fun extremeValues_doNotCrash() {
        success(maxLayout(BigDecimal("0.001"), BigDecimal("100000")))
        success(maxLayout(BigDecimal("1E+12"), BigDecimal("1E+10")))
        success(countLayout(BigDecimal("0.001"), 1000))
        error(maxLayout(BigDecimal("1E+12"), BigDecimal("0.0001")))
    }

    @Test
    fun studCount_comparesAgainstOptionalMaximum() {
        val over = success(StudSpacingCalculator.calculateStudCountLayout(inch("120"), 5, BigDecimal.ZERO, BigDecimal.ZERO, inch("16")))
        assertTrue(over.exceedsMaximum) // 30 in > 16 in, stud count is not silently adjusted
        assertEquals(5, over.studCount)
        val within = success(StudSpacingCalculator.calculateStudCountLayout(inch("120"), 9, BigDecimal.ZERO, BigDecimal.ZERO, inch("16")))
        assertFalse(within.exceedsMaximum)
    }
}
