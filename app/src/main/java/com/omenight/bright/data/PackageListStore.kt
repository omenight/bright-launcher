package com.omenight.bright.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** An ordered list of package names under one preferences key, used for favorites and hidden apps. */
class PackageListStore(private val context: Context, name: String) {
    private val key = stringPreferencesKey(name)

    val packages: Flow<List<String>> = context.brightPreferences.data.map { prefs -> decode(prefs[key]) }

    suspend fun toggle(packageName: String) = update { if (packageName in it) it - packageName else it + packageName }

    suspend fun add(packageName: String) = update { if (packageName in it) it else it + packageName }

    suspend fun remove(packageName: String) = update { it - packageName }

    private suspend fun update(change: (List<String>) -> List<String>) {
        context.brightPreferences.edit { prefs -> prefs[key] = change(decode(prefs[key])).joinToString(SEPARATOR) }
    }

    // Package names never contain a comma, so a joined string keeps the order a string set would lose
    private fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    private companion object {
        const val SEPARATOR = ","
    }
}
