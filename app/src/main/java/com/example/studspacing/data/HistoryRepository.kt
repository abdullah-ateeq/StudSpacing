package com.example.studspacing.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    private val historyKey = stringPreferencesKey("history")

    /** Newest first. */
    val history: Flow<List<HistoryEntry>> = dataStore.data.map { HistoryCodec.decode(it[historyKey]) }

    suspend fun add(entry: HistoryEntry) = modify { (listOf(entry) + it).take(MAX_RECORDS) }

    suspend fun delete(id: Long) = modify { list -> list.filterNot { it.id == id } }

    suspend fun clear() {
        dataStore.edit { it.remove(historyKey) }
    }

    private suspend fun modify(transform: (List<HistoryEntry>) -> List<HistoryEntry>) {
        dataStore.edit { prefs ->
            prefs[historyKey] = HistoryCodec.encode(transform(HistoryCodec.decode(prefs[historyKey])))
        }
    }

    companion object {
        const val MAX_RECORDS = 50
    }
}
