package com.example.studspacing.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rounds a millimeter value for display in [unit].
 *
 * @param trimZeros when true "16.00" becomes "16" and "15.50" becomes "15.5".
 */
fun formatNumber(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int, trimZeros: Boolean = false): String =
    formatPlain(fromMillimeters(valueMm, unit), decimals, trimZeros)

fun formatMeasurement(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int, trimZeros: Boolean = false): String =
    "${formatNumber(valueMm, unit, decimals, trimZeros)} ${unit.symbol}"

/** Spoken form for accessibility, e.g. "16 inches". */
fun spokenMeasurement(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
    "${formatNumber(valueMm, unit, decimals, trimZeros = true)} ${unit.spokenName}"

/**
 * Formats the actual spacing. When a maximum is given the displayed value is never
 * rounded up past that maximum: 15.996 with a 15.995 maximum shows 15.99, not 16.00.
 */
fun formatSpacing(
    actualMm: BigDecimal,
    maximumMm: BigDecimal?,
    unit: MeasurementUnit,
    decimals: Int,
    trimZeros: Boolean = false
): String {
    val value = fromMillimeters(actualMm, unit)
    val roundsAboveMaximum = maximumMm != null && actualMm <= maximumMm &&
        value.setScale(decimals, RoundingMode.HALF_UP) > fromMillimeters(maximumMm, unit)
    val mode = if (roundsAboveMaximum) RoundingMode.DOWN else RoundingMode.HALF_UP
    return "${formatPlain(value, decimals, trimZeros, mode)} ${unit.symbol}"
}

fun formatPlain(
    value: BigDecimal,
    decimals: Int,
    trimZeros: Boolean = false,
    roundingMode: RoundingMode = RoundingMode.HALF_UP
): String {
    var rounded = value.setScale(decimals, roundingMode)
    if (rounded.signum() == 0) rounded = BigDecimal.ZERO.setScale(decimals)
    if (trimZeros) rounded = rounded.stripTrailingZeros()
    if (rounded.scale() < 0) rounded = rounded.setScale(0)
    return rounded.toPlainString()
}

/** True when [valueMm] shown in [unit] needs no rounding at [decimals] places. */
fun isExactAt(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int): Boolean =
    fromMillimeters(valueMm, unit).stripTrailingZeros().scale() <= decimals

/**
 * Safe user-input parsing. Accepts "12", "12.5", ".5" and a comma decimal
 * separator. Returns null for anything malformed, never throws.
 */
fun parseDecimal(text: String): BigDecimal? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty() || cleaned == ".") return null
    if (!cleaned.matches(Regex("""^-?\d*\.?\d*$"""))) return null
    return cleaned.toBigDecimalOrNull()
}

/** Plain text for an input field after a unit conversion, e.g. 144 or 40.64. */
fun toInputText(value: BigDecimal): String = formatPlain(value, 6, trimZeros = true)
