package com.omenight.bright.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omenight.bright.ui.wallpaper.WallpaperPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val context: Context) {
    private val wallpaperKey = stringPreferencesKey("wallpaper")
    private val welcomedKey = booleanPreferencesKey("welcomed")
    private val systemWallpaperKey = stringPreferencesKey("system_wallpaper")
    private val updateCheckedAtKey = longPreferencesKey("update_checked_at")

    val wallpaper: Flow<WallpaperPreset> = context.brightPreferences.data.map { prefs ->
        WallpaperPreset.entries.firstOrNull { it.name == prefs[wallpaperKey] } ?: WallpaperPreset.Dawn
    }

    /** Whether the first-run guide has been dismissed. */
    val welcomed: Flow<Boolean> = context.brightPreferences.data.map { it[welcomedKey] ?: false }

    suspend fun setWelcomed() {
        context.brightPreferences.edit { it[welcomedKey] = true }
    }

    /** The preset last copied into the system wallpaper, so it is written only when it changes. */
    val systemWallpaper: Flow<String?> = context.brightPreferences.data.map { it[systemWallpaperKey] }

    suspend fun setSystemWallpaper(preset: WallpaperPreset) {
        context.brightPreferences.edit { it[systemWallpaperKey] = preset.name }
    }

    suspend fun setWallpaper(preset: WallpaperPreset) {
        context.brightPreferences.edit { it[wallpaperKey] = preset.name }
    }

    /** When the last update check reached GitHub, in epoch millis. */
    val updateCheckedAt: Flow<Long> = context.brightPreferences.data.map { it[updateCheckedAtKey] ?: 0L }

    suspend fun setUpdateCheckedAt(millis: Long) {
        context.brightPreferences.edit { it[updateCheckedAtKey] = millis }
    }
}
