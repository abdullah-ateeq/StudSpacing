package com.example.studspacing.domain

import java.math.BigDecimal
import java.math.MathContext

/** High precision context used for every division in the app. */
val CALC_CONTEXT: MathContext = MathContext.DECIMAL128

fun toMillimeters(value: BigDecimal, unit: MeasurementUnit): BigDecimal =
    value.multiply(unit.millimeters, CALC_CONTEXT)

fun fromMillimeters(valueMm: BigDecimal, unit: MeasurementUnit): BigDecimal =
    valueMm.divide(unit.millimeters, CALC_CONTEXT)

fun convert(value: BigDecimal, from: MeasurementUnit, to: MeasurementUnit): BigDecimal =
    if (from == to) value else fromMillimeters(toMillimeters(value, from), to)
