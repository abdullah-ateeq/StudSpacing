package com.example.studspacing.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.parseDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val WALL_UNIT = stringPreferencesKey("default_wall_unit")
        val SPACING_UNIT = stringPreferencesKey("default_spacing_unit")
        val WALL_LENGTH = stringPreferencesKey("default_wall_length")
        val MAX_SPACING = stringPreferencesKey("default_max_spacing")
        val START_OFFSET = stringPreferencesKey("default_start_offset")
        val END_OFFSET = stringPreferencesKey("default_end_offset")
        val ORIENTATION = stringPreferencesKey("orientation")
        val DECIMALS = intPreferencesKey("decimal_places")
        val SHOW_VISUALIZATION = booleanPreferencesKey("show_visualization")
        val SHOW_FORMULA = booleanPreferencesKey("show_formula")
        val SHOW_POSITIONS = booleanPreferencesKey("show_positions")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
    }

    val settings: Flow<AppSettings> = dataStore.data.map(::fromPreferences)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(fromPreferences(prefs))
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = updated.dynamicColor
            prefs[Keys.DEFAULT_MODE] = updated.defaultMode.name
            prefs[Keys.WALL_UNIT] = updated.defaultWallUnit.name
            prefs[Keys.SPACING_UNIT] = updated.defaultSpacingUnit.name
            prefs[Keys.WALL_LENGTH] = updated.defaultWallLength
            prefs[Keys.MAX_SPACING] = updated.defaultMaximumSpacing
            prefs[Keys.START_OFFSET] = updated.defaultStartOffset
            prefs[Keys.END_OFFSET] = updated.defaultEndOffset
            prefs[Keys.ORIENTATION] = updated.orientation.name
            prefs[Keys.DECIMALS] = updated.decimalPlaces
            prefs[Keys.SHOW_VISUALIZATION] = updated.showVisualization
            prefs[Keys.SHOW_FORMULA] = updated.showFormula
            prefs[Keys.SHOW_POSITIONS] = updated.showPositionList
            prefs[Keys.HAPTICS] = updated.hapticFeedback
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            listOf(
                Keys.THEME, Keys.DYNAMIC_COLOR, Keys.DEFAULT_MODE, Keys.WALL_UNIT, Keys.SPACING_UNIT,
                Keys.WALL_LENGTH, Keys.MAX_SPACING, Keys.START_OFFSET, Keys.END_OFFSET, Keys.ORIENTATION,
                Keys.DECIMALS, Keys.SHOW_VISUALIZATION, Keys.SHOW_FORMULA, Keys.SHOW_POSITIONS, Keys.HAPTICS
            ).forEach { prefs.remove(it) }
        }
    }

    private fun fromPreferences(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(prefs[Keys.THEME], defaults.themeMode),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultMode = enumOrDefault(prefs[Keys.DEFAULT_MODE], defaults.defaultMode),
            defaultWallUnit = enumOrDefault(prefs[Keys.WALL_UNIT], defaults.defaultWallUnit),
            defaultSpacingUnit = enumOrDefault(prefs[Keys.SPACING_UNIT], defaults.defaultSpacingUnit)
                .takeIf { it in MeasurementUnit.spacingUnits } ?: defaults.defaultSpacingUnit,
            defaultWallLength = validNumber(prefs[Keys.WALL_LENGTH], defaults.defaultWallLength),
            defaultMaximumSpacing = validNumber(prefs[Keys.MAX_SPACING], defaults.defaultMaximumSpacing),
            defaultStartOffset = validNumber(prefs[Keys.START_OFFSET], defaults.defaultStartOffset),
            defaultEndOffset = validNumber(prefs[Keys.END_OFFSET], defaults.defaultEndOffset),
            orientation = enumOrDefault(prefs[Keys.ORIENTATION], defaults.orientation),
            decimalPlaces = (prefs[Keys.DECIMALS] ?: defaults.decimalPlaces)
                .takeIf { it in AppSettings.DECIMAL_OPTIONS } ?: defaults.decimalPlaces,
            showVisualization = prefs[Keys.SHOW_VISUALIZATION] ?: defaults.showVisualization,
            showFormula = prefs[Keys.SHOW_FORMULA] ?: defaults.showFormula,
            showPositionList = prefs[Keys.SHOW_POSITIONS] ?: defaults.showPositionList,
            hapticFeedback = prefs[Keys.HAPTICS] ?: defaults.hapticFeedback
        )
    }

    private fun validNumber(stored: String?, fallback: String): String =
        stored?.takeIf { parseDecimal(it)?.let { value -> value.signum() >= 0 } == true } ?: fallback

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback
}

