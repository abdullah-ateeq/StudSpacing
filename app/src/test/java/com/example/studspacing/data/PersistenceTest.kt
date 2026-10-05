package com.example.studspacing.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.Orientation
import com.example.studspacing.domain.StudSpacingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal

class PersistenceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    private fun dataStore(name: String, file: File = File(tmp.root, "$name.preferences_pb")): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    private fun entry(id: Long, mode: StudSpacingMode = StudSpacingMode.MAX_SPACING) = HistoryEntry(
        id = id,
        timestampMillis = 1_700_000_000_000 + id,
        mode = mode,
        wallLength = "12",
        wallUnit = MeasurementUnit.FOOT,
        maximumSpacing = "16",
        spacingUnit = MeasurementUnit.INCH,
        studCount = "10",
        startOffset = "0",
        endOffset = "1.5",
        offsetUnit = MeasurementUnit.INCH,
        equalOffsets = true,
        actualSpacingMm = BigDecimal("406.4"),
        positionsMm = listOf(BigDecimal("0"), BigDecimal("406.4"), BigDecimal("812.8"))
    )

    // 25
    @Test
    fun historyCodec_roundTrips() {
        val entries = listOf(entry(1), entry(2, StudSpacingMode.STUD_COUNT).copy(maximumSpacing = "", positionsMm = emptyList()))
        val decoded = HistoryCodec.decode(HistoryCodec.encode(entries))
        assertEquals(entries, decoded)
    }

    @Test
    fun historyCodec_skipsMalformedRecords() {
        val text = HistoryCodec.encode(listOf(entry(1))) + "\ngarbage|record\n" + "1|2|NOT_A_MODE|1|FOOT|1|INCH|1|0|0|INCH|false|1|1"
        assertEquals(listOf(entry(1)), HistoryCodec.decode(text))
        assertTrue(HistoryCodec.decode("unknown-version\nabc").isEmpty())
        assertTrue(HistoryCodec.decode(null).isEmpty())
    }

    @Test
    fun historyRepository_keepsNewest50() = runBlocking {
        val repo = HistoryRepository(dataStore("history"))
        for (i in 1L..51L) repo.add(entry(i))
        val saved = repo.history.first()
        assertEquals(HistoryRepository.MAX_RECORDS, saved.size)
        assertEquals(51L, saved.first().id)
        assertEquals(2L, saved.last().id) // record 1 (the oldest) was dropped

        repo.delete(51L)
        assertEquals(50L, repo.history.first().first().id)
        repo.clear()
        assertTrue(repo.history.first().isEmpty())
    }

    // 26
    @Test
    fun settings_persistAcrossInstances() = runBlocking {
        val file = File(tmp.root, "settings.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }))
        assertEquals(AppSettings(), repo.settings.first())

        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = true,
            defaultMode = StudSpacingMode.STUD_COUNT,
            defaultWallUnit = MeasurementUnit.METER,
            defaultSpacingUnit = MeasurementUnit.CENTIMETER,
            defaultWallLength = "3.6",
            defaultMaximumSpacing = "40",
            defaultStartOffset = "2",
            defaultEndOffset = "3",
            orientation = Orientation.VERTICAL,
            decimalPlaces = 3,
            showVisualization = false,
            showFormula = false,
            showPositionList = false,
            hapticFeedback = false
        )
        repo.update { changed }
        assertEquals(changed, repo.settings.first())

        // A fresh DataStore on the same file reads the same values.
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = SettingsRepository(PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file }))
            assertEquals(changed, reopened.settings.first())
            reopened.reset()
            assertEquals(AppSettings(), reopened.settings.first())
        } finally {
            reopenedScope.cancel()
        }
    }
}
