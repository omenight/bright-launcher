package com.omenight.bright.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omenight.bright.ui.glass.WallpaperPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val context: Context) {
    private val wallpaperKey = stringPreferencesKey("wallpaper")

    val wallpaper: Flow<WallpaperPreset> = context.brightPreferences.data.map { prefs ->
        WallpaperPreset.entries.firstOrNull { it.name == prefs[wallpaperKey] } ?: WallpaperPreset.Dawn
    }

    suspend fun setWallpaper(preset: WallpaperPreset) {
        context.brightPreferences.edit { it[wallpaperKey] = preset.name }
    }
}
