package com.example.studspacing.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.Orientation
import com.example.studspacing.domain.StudSpacingResult
import com.example.studspacing.domain.formatMeasurement
import com.example.studspacing.domain.formatSpacing
import com.example.studspacing.domain.spokenMeasurement

/** Maximum studs drawn individually; larger layouts switch to a schematic view. */
private const val MAX_DRAWN_STUDS = 60

private enum class LabelSide { BEFORE, AFTER }

/**
 * Minimal wall elevation: top and bottom plates, one line per stud, and dimension
 * lines for wall length, actual spacing and end offsets. Purely a visual aid; the
 * numbers come from [StudSpacingResult].
 */
@Composable
fun WallDiagram(
    result: StudSpacingResult,
    wallLengthText: String,
    wallUnit: MeasurementUnit,
    spacingUnit: MeasurementUnit,
    decimals: Int,
    orientation: Orientation,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val hasOffsets = result.startOffsetMm.signum() > 0 || result.endOffsetMm.signum() > 0
    val vertical = orientation == Orientation.VERTICAL
    val height: Dp = when {
        vertical -> 400.dp
        hasOffsets -> 196.dp
        else -> 164.dp
    }

    val spacingLabel = formatSpacing(result.actualSpacingMm, result.maximumSpacingMm, spacingUnit, decimals)
    val wallLabel = "$wallLengthText ${wallUnit.symbol}"
    val startLabel = formatMeasurement(result.startOffsetMm, spacingUnit, decimals, trimZeros = true)
    val endLabel = formatMeasurement(result.endOffsetMm, spacingUnit, decimals, trimZeros = true)
    val description = remember(result, wallUnit, spacingUnit, decimals) {
        "Wall diagram. ${result.studCount} stud positions spaced " +
            "${spokenMeasurement(result.actualSpacingMm, spacingUnit, decimals)} apart along " +
            spokenMeasurement(result.wallLengthMm, wallUnit, decimals) + "."
    }

    val labelStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = colors.onSurfaceVariant
    )
    val accentLabelStyle = labelStyle.copy(color = colors.primary, fontWeight = FontWeight.SemiBold)
    val captionStyle = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold,
        color = colors.onSurfaceVariant)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        val wallMm = result.wallLengthMm.toFloat()
        val fractions = result.positions.map { (it.positionMm.toFloat() / wallMm).coerceIn(0f, 1f) }
        val studCount = fractions.size

        // Geometry in the "along" (wall length) and "across" (wall height) axes.
        val alongStart: Float
        val alongEnd: Float
        val acrossSpacingDim: Float
        val acrossPlateA: Float
        val acrossPlateB: Float
        val acrossOffsetDim: Float
        val acrossWallDim: Float
        if (vertical) {
            alongStart = 30.dp.toPx()
            alongEnd = size.height - 30.dp.toPx()
            val contentWidth = 270.dp.toPx()
            val left = ((size.width - contentWidth) / 2f).coerceAtLeast(0f)
            acrossSpacingDim = left + 82.dp.toPx()
            acrossPlateA = left + 100.dp.toPx()
            acrossPlateB = left + 156.dp.toPx()
            acrossOffsetDim = left + 176.dp.toPx()
            acrossWallDim = left + if (hasOffsets) 200.dp.toPx() else 176.dp.toPx()
        } else {
            alongStart = 16.dp.toPx()
            alongEnd = size.width - 16.dp.toPx()
            acrossSpacingDim = 40.dp.toPx()
            acrossPlateA = 54.dp.toPx()
            acrossPlateB = 112.dp.toPx()
            acrossOffsetDim = 130.dp.toPx()
            acrossWallDim = if (hasOffsets) 166.dp.toPx() else 132.dp.toPx()
        }
        val alongLength = alongEnd - alongStart

        fun p(along: Float, across: Float) = if (vertical) Offset(across, along) else Offset(along, across)
        fun at(fraction: Float) = alongStart + fraction * alongLength

        // Decide between a to-scale drawing and a schematic for dense layouts.
        val minGap = 4.dp.toPx()
        val toScale = studCount <= MAX_DRAWN_STUDS && alongLength * (fractions[1] - fractions[0]) >= minGap
        val drawn: List<Float>
        var gapAlong: Float? = null
        if (toScale) {
            drawn = fractions.map(::at)
        } else {
            val first = at(fractions.first())
            val last = at(fractions.last())
            val span = last - first
            val step = span / 12f
            drawn = (0..6).map { first + it * step } + (9..12).map { first + it * step }
            gapAlong = first + 7.5f * step
        }

        val plateColor = colors.onSurface
        val studColor = colors.primary
        val dimColor = colors.outline
        val faint = colors.outlineVariant
        val plateStroke = 3.dp.toPx()
        val studStroke = 2.dp.toPx()
        val dimStroke = 1.dp.toPx()

        // Wall end extension lines.
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        listOf(alongStart, alongEnd).forEach { a ->
            drawLine(faint, p(a, acrossPlateA), p(a, acrossWallDim + 6.dp.toPx()), dimStroke, pathEffect = dash)
        }

        // Plates.
        drawLine(plateColor, p(alongStart, acrossPlateA), p(alongEnd, acrossPlateA), plateStroke, StrokeCap.Square)
        drawLine(plateColor, p(alongStart, acrossPlateB), p(alongEnd, acrossPlateB), plateStroke, StrokeCap.Square)

        // Studs.
        drawn.forEach { a ->
            drawLine(studColor, p(a, acrossPlateA), p(a, acrossPlateB), studStroke)
        }
        gapAlong?.let { g ->
            val ellipsis = if (vertical) "⋮" else "…"
            drawLabel(textMeasurer, ellipsis, accentLabelStyle, g, (acrossPlateA + acrossPlateB) / 2f, vertical, null)
        }

        // Actual spacing dimension over the first interval.
        if (drawn.size >= 2) {
            val a = drawn[0]
            val b = drawn[1]
            dimension(a, b, acrossSpacingDim, studColor, dimStroke, ::p)
            drawLabel(textMeasurer, spacingLabel, accentLabelStyle, (a + b) / 2f, acrossSpacingDim, vertical,
                LabelSide.BEFORE, clampMin = 0f, clampMax = if (vertical) size.height else size.width)
        }

        // Offsets.
        if (hasOffsets) {
            if (result.startOffsetMm.signum() > 0) {
                val s = at(fractions.first())
                dimension(alongStart, s, acrossOffsetDim, dimColor, dimStroke, ::p)
                drawLabel(textMeasurer, "start $startLabel", labelStyle, alongStart, acrossOffsetDim, vertical,
                    LabelSide.AFTER, anchorStart = true)
            }
            if (result.endOffsetMm.signum() > 0) {
                val e = at(fractions.last())
                dimension(e, alongEnd, acrossOffsetDim, dimColor, dimStroke, ::p)
                drawLabel(textMeasurer, "end $endLabel", labelStyle, alongEnd, acrossOffsetDim, vertical,
                    LabelSide.AFTER, anchorEnd = true)
            }
        }

        // Overall wall length.
        dimension(alongStart, alongEnd, acrossWallDim, dimColor, dimStroke, ::p)
        drawLabel(textMeasurer, wallLabel, labelStyle.copy(color = colors.onSurface), (alongStart + alongEnd) / 2f,
            acrossWallDim, vertical, LabelSide.AFTER)

        // START / END markers.
        val markerAcross = (acrossPlateA + acrossPlateB) / 2f
        if (vertical) {
            drawLabel(textMeasurer, "START", captionStyle, alongStart - 16.dp.toPx(), markerAcross, true, null)
            drawLabel(textMeasurer, "END", captionStyle, alongEnd + 16.dp.toPx(), markerAcross, true, null)
        } else {
            drawLabel(textMeasurer, "START", captionStyle, alongStart, 8.dp.toPx(), false, null, anchorStart = true)
            drawLabel(textMeasurer, "END", captionStyle, alongEnd, 8.dp.toPx(), false, null, anchorEnd = true)
        }
    }
    Text(
        text = if (result.studCount > MAX_DRAWN_STUDS) {
            "${result.studCount} stud positions · simplified view"
        } else {
            "${result.studCount} stud positions"
        },
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    }
}

/** A dimension line with architectural slash ticks at both ends. */
private fun DrawScope.dimension(
    from: Float,
    to: Float,
    across: Float,
    color: Color,
    stroke: Float,
    p: (Float, Float) -> Offset
) {
    drawLine(color, p(from, across), p(to, across), stroke)
    val t = 4.dp.toPx()
    listOf(from, to).forEach { a ->
        drawLine(color, p(a - t, across + t), p(a + t, across - t), stroke * 1.4f)
    }
}

/**
 * Draws [text] centred on a point. [side] moves it before (above / left) or after
 * (below / right) the dimension line, depending on orientation.
 */
private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    along: Float,
    across: Float,
    vertical: Boolean,
    side: LabelSide?,
    anchorStart: Boolean = false,
    anchorEnd: Boolean = false,
    clampMin: Float = 0f,
    clampMax: Float = Float.MAX_VALUE
) {
    val layout = measurer.measure(text, style)
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val gap = 4.dp.toPx()
    val topLeft = if (!vertical) {
        val x = when {
            anchorStart -> along
            anchorEnd -> along - w
            else -> (along - w / 2f).coerceIn(clampMin, (minOf(clampMax, size.width) - w).coerceAtLeast(clampMin))
        }
        val y = when (side) {
            LabelSide.BEFORE -> across - h - gap
            LabelSide.AFTER -> across + gap
            null -> across - h / 2f
        }
        Offset(x, y)
    } else {
        val y = when {
            anchorStart -> along + gap
            anchorEnd -> along - h - gap
            else -> along - h / 2f
        }
        val x = when (side) {
            LabelSide.BEFORE -> across - w - gap * 2
            LabelSide.AFTER -> across + gap * 2
            null -> across - w / 2f
        }
        Offset(x.coerceAtLeast(0f), y)
    }
    drawText(layout, topLeft = topLeft)
}
