package com.example.studspacing.ui.calculator

import com.example.studspacing.domain.StudSpacingResult
import com.example.studspacing.domain.formatMeasurement
import com.example.studspacing.domain.formatSpacing
import com.example.studspacing.ui.CalculatorForm

/** Plain-text summary for the clipboard. */
fun buildResultText(form: CalculatorForm, result: StudSpacingResult, decimals: Int): String = buildString {
    val unit = form.spacingUnit
    appendLine("StudSpacing")
    appendLine("Mode: ${form.mode.label}")
    appendLine("Wall: ${form.wallLength} ${form.wallUnit.symbol}")
    appendLine("Start offset: ${formatMeasurement(result.startOffsetMm, unit, decimals, trimZeros = true)}")
    appendLine("End offset: ${formatMeasurement(result.endOffsetMm, unit, decimals, trimZeros = true)}")
    result.maximumSpacingMm?.let {
        appendLine("Maximum spacing: ${formatMeasurement(it, unit, decimals, trimZeros = true)}")
    }
    appendLine("Stud positions: ${result.studCount}")
    appendLine("Intervals: ${result.intervals}")
    append("Actual spacing: ")
    append(formatSpacing(result.actualSpacingMm, result.maximumSpacingMm, unit, decimals, trimZeros = true))
}

/** One line per stud, ready to take to the job site. */
fun buildPositionsText(form: CalculatorForm, result: StudSpacingResult, decimals: Int): String =
    result.positions.joinToString("\n") { position ->
        "Stud ${position.index}: ${formatMeasurement(position.positionMm, form.spacingUnit, decimals, trimZeros = true)}"
    }
