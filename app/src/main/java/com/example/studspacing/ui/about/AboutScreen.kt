package com.example.studspacing.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.studspacing.ui.components.DisclaimerCard
import com.example.studspacing.ui.components.SectionLabel
import com.example.studspacing.ui.splash.WallMark

private val FEATURES = listOf(
    "Stud Count", "Maximum Spacing", "Actual Spacing", "Start/End Offsets", "Stud Position List",
    "Wall Visualization", "Multiple Units", "Calculation History", "Copy Results", "Offline"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(contentPadding: PaddingValues) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
        ) {
            WallMark(progress = 1f, modifier = Modifier.padding(top = 16.dp))
            Column {
                Text("StudSpacing", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text("Version 1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "An offline layout utility for calculating stud positions, stud count, and actual spacing along a wall.",
                style = MaterialTheme.typography.bodyLarge
            )
            SectionLabel("Features")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FEATURES.forEach { FeatureTag(it) }
            }
            SectionLabel("How it works")
            Text(
                "Max Spacing mode divides the usable wall length (wall minus offsets) into the fewest equal " +
                    "intervals that do not exceed your maximum. Stud Count mode divides it into one fewer " +
                    "interval than the number of studs, because both ends hold a stud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DisclaimerCard()
        }
    }
}

@Composable
private fun FeatureTag(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
