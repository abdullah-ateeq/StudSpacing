package com.example.studspacing.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studspacing.data.AppSettings
import com.example.studspacing.ui.about.AboutScreen
import com.example.studspacing.ui.calculator.CalculatorScreen
import com.example.studspacing.ui.components.StudsIcon
import com.example.studspacing.ui.history.HistoryScreen
import com.example.studspacing.ui.settings.SettingsScreen
import com.example.studspacing.ui.splash.SplashScreen

object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    TopLevelDestination(Routes.CALCULATOR, "Calculator", StudsIcon),
    TopLevelDestination(Routes.HISTORY, "History", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info)
)

@Composable
fun StudSpacingApp(viewModel: StudSpacingViewModel, settings: AppSettings) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    Crossfade(targetState = showSplash, label = "splash") { splash ->
        if (splash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            MainScaffold(viewModel, settings)
        }
    }
}

@Composable
private fun MainScaffold(viewModel: StudSpacingViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val haptic: (HapticFeedbackType) -> Unit = { type ->
        if (settings.hapticFeedback) hapticFeedback.performHapticFeedback(type)
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigateTopLevel(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CALCULATOR,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(viewModel, settings, snackbarHostState, haptic, padding)
            }
            composable(Routes.HISTORY) {
                val history by viewModel.history.collectAsStateWithLifecycle()
                HistoryScreen(
                    history = history,
                    decimals = settings.decimalPlaces,
                    contentPadding = padding,
                    onOpen = { entry ->
                        viewModel.restore(entry)
                        navController.navigateTopLevel(Routes.CALCULATOR)
                    },
                    onDelete = viewModel::deleteHistory,
                    onClearAll = viewModel::clearHistory
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    contentPadding = padding,
                    onUpdate = viewModel::updateSettings,
                    onResetSettings = viewModel::resetSettings,
                    onClearHistory = viewModel::clearHistory
                )
            }
            composable(Routes.ABOUT) { AboutScreen(padding) }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
