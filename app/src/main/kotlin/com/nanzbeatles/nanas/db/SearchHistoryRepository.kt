/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.db

import com.nanzbeatles.nanas.db.entities.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing recent search queries stored in the Room database ([SearchHistory]).
 */
@Singleton
class SearchHistoryRepository @Inject constructor(
    private val database: MusicDatabase
) {
    /**
     * Observes recent search queries matching the given prefix [queryFilter], ordered from newest to oldest.
     */
    fun getRecentQueries(queryFilter: String = ""): Flow<List<SearchHistory>> =
        database.searchHistory(queryFilter)

    /**
     * Inserts or updates a search query in the Room database.
     */
    suspend fun insertQuery(rawQuery: String) = withContext(Dispatchers.IO) {
        val cleaned = rawQuery.trim()
        if (cleaned.isNotEmpty()) {
            database.query {
                insert(SearchHistory(query = cleaned))
            }
        }
    }

    /**
     * Deletes a specific [SearchHistory] entry from the Room database.
     */
    suspend fun deleteQuery(history: SearchHistory) = withContext(Dispatchers.IO) {
        database.query {
            delete(history)
        }
    }

    /**
     * Clears all stored search history entries from the Room database.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        database.query {
            clearSearchHistory()
        }
    }
}
