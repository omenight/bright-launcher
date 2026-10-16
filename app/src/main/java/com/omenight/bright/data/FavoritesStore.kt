package com.omenight.bright.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ordered list of favorite package names. */
class FavoritesStore(private val context: Context) {
    private val key = stringPreferencesKey("favorites")

    val favorites: Flow<List<String>> = context.brightPreferences.data.map { prefs -> decode(prefs[key]) }

    suspend fun toggle(packageName: String) {
        context.brightPreferences.edit { prefs ->
            val current = decode(prefs[key])
            val updated = if (packageName in current) current - packageName else current + packageName
            prefs[key] = updated.joinToString(SEPARATOR)
        }
    }

    // Package names never contain a comma, so a joined string keeps the order a string set would lose
    private fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    private companion object {
        const val SEPARATOR = ","
    }
}
