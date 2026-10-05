package com.example.studspacing.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UnitAndFormatTest {

    private fun assertMm(expected: String, value: String, unit: MeasurementUnit) {
        assertEquals(0, BigDecimal(expected).compareTo(toMillimeters(BigDecimal(value), unit)))
        assertEquals(0, BigDecimal(value).compareTo(fromMillimeters(BigDecimal(expected), unit)))
    }

    // 15-20
    @Test fun millimeters() = assertMm("250", "250", MeasurementUnit.MILLIMETER)
    @Test fun centimeters() = assertMm("405", "40.5", MeasurementUnit.CENTIMETER)
    @Test fun meters() = assertMm("1250", "1.25", MeasurementUnit.METER)
    @Test fun inches() = assertMm("406.4", "16", MeasurementUnit.INCH)
    @Test fun feet() = assertMm("3657.6", "12", MeasurementUnit.FOOT)
    @Test fun yards() = assertMm("914.4", "1", MeasurementUnit.YARD)

    @Test
    fun convertPreservesPhysicalLength() {
        assertEquals(0, BigDecimal("144").compareTo(convert(BigDecimal("12"), MeasurementUnit.FOOT, MeasurementUnit.INCH)))
        assertEquals(0, BigDecimal("40.64").compareTo(convert(BigDecimal("16"), MeasurementUnit.INCH, MeasurementUnit.CENTIMETER)))
        assertEquals(0, BigDecimal("4").compareTo(convert(BigDecimal("12"), MeasurementUnit.FOOT, MeasurementUnit.YARD)))
    }

    @Test
    fun formatMeasurement_fixedAndTrimmed() {
        val sixteen = toMillimeters(BigDecimal("16"), MeasurementUnit.INCH)
        assertEquals("16.00 in", formatMeasurement(sixteen, MeasurementUnit.INCH, 2))
        assertEquals("16 in", formatMeasurement(sixteen, MeasurementUnit.INCH, 2, trimZeros = true))
        assertEquals("40.64 cm", formatMeasurement(sixteen, MeasurementUnit.CENTIMETER, 2))
        assertEquals("15.5 in", formatMeasurement(BigDecimal("393.7"), MeasurementUnit.INCH, 2, trimZeros = true))
        assertEquals("1.25 m", formatMeasurement(BigDecimal("1250"), MeasurementUnit.METER, 2))
        assertEquals("0.0 in", formatMeasurement(BigDecimal.ZERO, MeasurementUnit.INCH, 1))
        assertEquals("17.1429 in", formatMeasurement(BigDecimal("240").multiply(BigDecimal("25.4")).divide(BigDecimal(14), CALC_CONTEXT), MeasurementUnit.INCH, 4))
    }

    @Test
    fun formatSpacing_neverRoundsAboveMaximum() {
        val max = toMillimeters(BigDecimal("15.995"), MeasurementUnit.INCH)
        val actual = toMillimeters(BigDecimal("15.9949"), MeasurementUnit.INCH)
        assertEquals("15.99 in", formatSpacing(actual, max, MeasurementUnit.INCH, 2))
        // Without a maximum normal rounding applies.
        assertEquals("15.99 in", formatSpacing(actual, null, MeasurementUnit.INCH, 2))
        val actual2 = toMillimeters(BigDecimal("15.9951"), MeasurementUnit.INCH)
        val max2 = toMillimeters(BigDecimal("15.9952"), MeasurementUnit.INCH)
        assertEquals("15.99 in", formatSpacing(actual2, max2, MeasurementUnit.INCH, 2))
        assertEquals("16.00 in", formatSpacing(actual2, null, MeasurementUnit.INCH, 2))
    }

    @Test
    fun isExactAt_detectsRounding() {
        val r = (StudSpacingCalculator.calculateStudCountLayout(BigDecimal("3048"), 6, BigDecimal.ZERO, BigDecimal.ZERO)
            as LayoutOutcome.Success).result
        assertTrue(isExactAt(r.actualSpacingMm, MeasurementUnit.INCH, 2))
        val r2 = (StudSpacingCalculator.calculateStudCountLayout(BigDecimal("3048"), 8, BigDecimal.ZERO, BigDecimal.ZERO)
            as LayoutOutcome.Success).result
        assertFalse(isExactAt(r2.actualSpacingMm, MeasurementUnit.INCH, 2)) // 120 in / 7
    }

    @Test
    fun parseDecimal_isSafe() {
        assertEquals(0, BigDecimal("12.5").compareTo(parseDecimal("12.5")))
        assertEquals(0, BigDecimal("12.5").compareTo(parseDecimal("12,5")))
        assertEquals(0, BigDecimal("0.5").compareTo(parseDecimal(".5")))
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("."))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("1.2.3"))
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("Infinity"))
        assertNull(parseDecimal("1e5"))
    }
}
