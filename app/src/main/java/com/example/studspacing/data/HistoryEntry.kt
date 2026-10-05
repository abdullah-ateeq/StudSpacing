package com.example.studspacing.data

import com.example.studspacing.domain.MeasurementUnit
import com.example.studspacing.domain.StudSpacingMode
import java.math.BigDecimal

/**
 * A saved calculation. Input values are stored as the text the user entered so a
 * restore puts the calculator back exactly as it was.
 */
data class HistoryEntry(
    val id: Long,
    val timestampMillis: Long,
    val mode: StudSpacingMode,
    val wallLength: String,
    val wallUnit: MeasurementUnit,
    val maximumSpacing: String,
    val spacingUnit: MeasurementUnit,
    val studCount: String,
    val startOffset: String,
    val endOffset: String,
    val offsetUnit: MeasurementUnit,
    val equalOffsets: Boolean,
    val actualSpacingMm: BigDecimal,
    val positionsMm: List<BigDecimal>
) {
    val resultStudCount: Int get() = positionsMm.size
}

/**
 * Compact text codec for storing history in a single Preferences string.
 * All fields are numbers or enum names, so a fixed field order with simple
 * separators is unambiguous. Malformed records are skipped on decode.
 */
object HistoryCodec {
    private const val VERSION = "v1"
    private const val RECORD_SEPARATOR = "\n"
    private const val FIELD_SEPARATOR = "|"
    private const val LIST_SEPARATOR = ","
    private const val FIELD_COUNT = 14

    fun encode(entries: List<HistoryEntry>): String =
        entries.joinToString(RECORD_SEPARATOR, prefix = VERSION + RECORD_SEPARATOR) { e ->
            listOf(
                e.id.toString(),
                e.timestampMillis.toString(),
                e.mode.name,
                e.wallLength,
                e.wallUnit.name,
                e.maximumSpacing,
                e.spacingUnit.name,
                e.studCount,
                e.startOffset,
                e.endOffset,
                e.offsetUnit.name,
                e.equalOffsets.toString(),
                e.actualSpacingMm.toPlainString(),
                e.positionsMm.joinToString(LIST_SEPARATOR) { it.toPlainString() }
            ).joinToString(FIELD_SEPARATOR) { sanitize(it) }
        }

    fun decode(text: String?): List<HistoryEntry> {
        if (text.isNullOrBlank()) return emptyList()
        val lines = text.split(RECORD_SEPARATOR)
        if (lines.firstOrNull() != VERSION) return emptyList()
        return lines.drop(1).mapNotNull(::decodeRecord)
    }

    private fun decodeRecord(line: String): HistoryEntry? {
        val f = line.split(FIELD_SEPARATOR)
        if (f.size != FIELD_COUNT) return null
        return runCatching {
            HistoryEntry(
                id = f[0].toLong(),
                timestampMillis = f[1].toLong(),
                mode = StudSpacingMode.valueOf(f[2]),
                wallLength = f[3],
                wallUnit = MeasurementUnit.valueOf(f[4]),
                maximumSpacing = f[5],
                spacingUnit = MeasurementUnit.valueOf(f[6]),
                studCount = f[7],
                startOffset = f[8],
                endOffset = f[9],
                offsetUnit = MeasurementUnit.valueOf(f[10]),
                equalOffsets = f[11].toBooleanStrict(),
                actualSpacingMm = BigDecimal(f[12]),
                positionsMm = if (f[13].isEmpty()) emptyList() else f[13].split(LIST_SEPARATOR).map(::BigDecimal)
            )
        }.getOrNull()
    }

    /** Input text is restricted to digits and '.', but strip separators defensively. */
    private fun sanitize(value: String): String =
        value.replace(FIELD_SEPARATOR, "").replace(RECORD_SEPARATOR, "")
}
