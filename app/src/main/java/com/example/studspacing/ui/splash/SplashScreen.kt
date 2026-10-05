package com.example.studspacing.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.studspacing.ui.theme.OverlineStyle

private const val SPLASH_DURATION_MS = 1300

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_DURATION_MS, easing = LinearEasing))
        onFinished()
    }
    val p = progress.value
    val titleAlpha = phase(p, 0.7f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WallMark(progress = p)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(titleAlpha)
        ) {
            Text(
                "STUDSPACING",
                style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = OverlineStyle.letterSpacing * 2),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Plan evenly spaced stud positions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Maps overall [progress] to 0..1 within the window [start, end]. */
private fun phase(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Brand mark: a wall plate draws in, studs drop into place, slide out to even
 * spacing, then a dimension line appears. At progress = 1 it is a static logo.
 */
@Composable
fun WallMark(progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(
        modifier
            .width(220.dp)
            .height(120.dp)
            .clearAndSetSemantics { }
    ) {
        val wall = phase(progress, 0f, 0.25f)
        val studs = phase(progress, 0.2f, 0.45f)
        val distribute = phase(progress, 0.4f, 0.7f)
        val dimension = phase(progress, 0.65f, 0.85f)

        val left = 12.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = 80.dp.toPx()
        val plate = 3.dp.toPx()

        // Plates draw in from the left.
        val wallEnd = left + (right - left) * wall
        drawLine(colors.onBackground, Offset(left, top), Offset(wallEnd, top), plate, StrokeCap.Square)
        drawLine(colors.onBackground, Offset(left, bottom), Offset(wallEnd, bottom), plate, StrokeCap.Square)

        // Five studs start clustered near the left and spread to even spacing.
        val count = 5
        val eased = 1f - (1f - distribute) * (1f - distribute)
        for (i in 0 until count) {
            val even = left + (right - left) * i / (count - 1)
            val clustered = left + 10.dp.toPx() * i
            val x = clustered + (even - clustered) * eased
            val stagger = phase(studs, i * 0.12f, i * 0.12f + 0.5f)
            if (stagger > 0f) {
                drawLine(
                    colors.primary,
                    Offset(x, top),
                    Offset(x, top + (bottom - top) * stagger),
                    2.5.dp.toPx()
                )
            }
        }

        // Dimension line with slash ticks.
        if (dimension > 0f) {
            val y = bottom + 20.dp.toPx()
            val color = colors.outline.copy(alpha = dimension)
            val t = 4.dp.toPx()
            drawLine(color, Offset(left, y), Offset(right, y), 1.dp.toPx())
            listOf(left, right).forEach { x ->
                drawLine(color, Offset(x - t, y + t), Offset(x + t, y - t), 1.5.dp.toPx())
                drawLine(color, Offset(x, y - 8.dp.toPx()), Offset(x, y + 8.dp.toPx()), 1.dp.toPx())
            }
        }
    }
}
