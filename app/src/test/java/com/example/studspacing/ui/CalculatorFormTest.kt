package com.example.studspacing.ui

import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.StudSpacingMode
import com.example.studspacing.ui.calculator.buildPositionsText
import com.example.studspacing.ui.calculator.buildResultText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the form → result pipeline the ViewModel exposes to the UI. */
class CalculatorFormTest {

    private val evaluate = StudSpacingViewModel.Companion::evaluate

    @Test
    fun defaultForm_givesTenStuds() {
        val state = evaluate(CalculatorForm())
        assertNotNull(state.result)
        val r = state.result!!
        assertEquals(10, r.studCount)
        assertEquals(9, r.intervals)
    }

    @Test
    fun studCountOfOne_showsMessage() {
        val state = evaluate(CalculatorForm(mode = StudSpacingMode.STUD_COUNT, studCount = "1"))
        assertNull(state.result)
        assertEquals("Enter at least 2 stud positions.", state.fieldErrors[InputField.STUD_COUNT])
    }

    @Test
    fun offsetsLargerThanWall_showsMessage() {
        val state = evaluate(CalculatorForm(wallLength = "10", wallUnit = MeasurementUnit.INCH, startOffset = "6", endOffset = "4"))
        assertNull(state.result)
        assertEquals("Offsets must be smaller than the wall length.", state.fieldErrors[InputField.END_OFFSET])
    }

    @Test
    fun zeroSpacing_showsMessage() {
        val state = evaluate(CalculatorForm(maximumSpacing = "0"))
        assertEquals("Maximum spacing must be greater than zero.", state.fieldErrors[InputField.SPACING])
    }

    @Test
    fun equalOffsets_useEndOffsetForStart() {
        val state = evaluate(
            CalculatorForm(wallLength = "120", wallUnit = MeasurementUnit.INCH, startOffset = "0", endOffset = "4", equalOffsets = true)
        )
        assertEquals(0, java.math.BigDecimal("101.6").compareTo(state.result!!.startOffsetMm))
    }

    @Test
    fun unitChange_preservesPhysicalValue() {
        assertEquals("144", StudSpacingViewModel.convertText("12", MeasurementUnit.FOOT, MeasurementUnit.INCH))
        assertEquals("40.64", StudSpacingViewModel.convertText("16", MeasurementUnit.INCH, MeasurementUnit.CENTIMETER))
        assertEquals("", StudSpacingViewModel.convertText("", MeasurementUnit.INCH, MeasurementUnit.CENTIMETER))
    }

    @Test
    fun sanitizeDecimal_rejectsSignsAndExtraSeparators() {
        assertEquals("12.5", StudSpacingViewModel.sanitizeDecimal("-12.5"))
        assertEquals("1.23", StudSpacingViewModel.sanitizeDecimal("1.2.3"))
        assertEquals("3.5", StudSpacingViewModel.sanitizeDecimal("3,5"))
    }

    @Test
    fun copyText_matchesFormat() {
        val state = evaluate(CalculatorForm())
        val text = buildResultText(state.form, state.result!!, 2)
        assertEquals(
            listOf(
                "StudSpacing",
                "Mode: Max spacing",
                "Wall: 12 ft",
                "Start offset: 0 in",
                "End offset: 0 in",
                "Maximum spacing: 16 in",
                "Stud positions: 10",
                "Intervals: 9",
                "Actual spacing: 16 in"
            ),
            text.lines()
        )
        val positions = buildPositionsText(state.form, state.result!!, 2).lines()
        assertEquals("Stud 1: 0 in", positions.first())
        assertEquals("Stud 2: 16 in", positions[1])
        assertEquals("Stud 10: 144 in", positions.last())
    }
}
