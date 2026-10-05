package com.example.studspacing.data

import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.Orientation
import com.example.studspacing.domain.StudSpacingMode

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultMode: StudSpacingMode = StudSpacingMode.MAX_SPACING,
    val defaultWallUnit: MeasurementUnit = MeasurementUnit.FOOT,
    val defaultSpacingUnit: MeasurementUnit = MeasurementUnit.INCH,
    val defaultWallLength: String = "12",
    val defaultMaximumSpacing: String = "16",
    val defaultStartOffset: String = "0",
    val defaultEndOffset: String = "0",
    val orientation: Orientation = Orientation.HORIZONTAL,
    val decimalPlaces: Int = 2,
    val showVisualization: Boolean = true,
    val showFormula: Boolean = true,
    val showPositionList: Boolean = true,
    val hapticFeedback: Boolean = true
) {
    companion object {
        val DECIMAL_OPTIONS = listOf(1, 2, 3, 4)
    }
}
