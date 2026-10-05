package com.example.studspacing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studspacing.data.ThemeMode
import com.example.studspacing.ui.StudSpacingApp
import com.example.studspacing.ui.StudSpacingViewModel
import com.example.studspacing.ui.theme.StudSpacingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudSpacingViewModel by viewModels { StudSpacingViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings
            val dark = when (current?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            StudSpacingTheme(darkTheme = dark, dynamicColor = current?.dynamicColor == true) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Wait for stored settings so the theme and defaults never flash.
                    if (current != null) StudSpacingApp(viewModel, current)
                }
            }
        }
    }
}
