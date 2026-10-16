package com.omenight.bright

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omenight.bright.data.AppArt
import com.omenight.bright.data.AppRepository
import com.omenight.bright.data.FavoritesStore
import com.omenight.bright.data.HomeRole
import com.omenight.bright.data.LaunchableApp
import com.omenight.bright.data.NetworkMonitor
import com.omenight.bright.data.NetworkStatus
import com.omenight.bright.data.SettingsStore
import com.omenight.bright.data.SystemWallpaper
import com.omenight.bright.ui.glass.WallpaperPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeState(
    val favorites: List<LaunchableApp> = emptyList(),
    val apps: List<LaunchableApp> = emptyList(),
    val favoritePackages: Set<String> = emptySet(),
    val loaded: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

    private val settingsStore = SettingsStore(application)
    private val homeRole = HomeRole(application)
    private val systemWallpaper = SystemWallpaper(application)
    private val systemWallpaperLock = Mutex()

    val wallpaper: StateFlow<WallpaperPreset> = settingsStore.wallpaper
        .stateIn(viewModelScope, SharingStarted.Eagerly, WallpaperPreset.Dawn)

    /** Null until the stored flag is read, so the guide never flashes for returning users. */
    val welcomed: StateFlow<Boolean?> = settingsStore.welcomed
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val network: StateFlow<NetworkStatus> = NetworkMonitor(application).status()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkStatus.Offline)

    val state: StateFlow<HomeState> = combine(repository.apps(), favoritesStore.favorites) { apps, favorites ->
        val byPackage = apps.associateBy { it.packageName }
        HomeState(
            // Uninstalled favorites drop out here but stay stored, so a reinstall brings them back
            favorites = favorites.mapNotNull(byPackage::get),
            apps = apps,
            favoritePackages = favorites.toSet(),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    fun launch(app: LaunchableApp) = repository.launch(app)

    fun openSystemSettings() = repository.openSystemSettings()

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch { favoritesStore.toggle(app.packageName) }
    }

    fun finishWelcome() {
        viewModelScope.launch { settingsStore.setWelcomed() }
    }

    fun setWallpaper(preset: WallpaperPreset) {
        viewModelScope.launch {
            settingsStore.setWallpaper(preset)
            syncSystemWallpaper()
        }
    }

    /** Called on every resume, so it also catches Bright becoming the default home. */
    fun onResume() {
        viewModelScope.launch { syncSystemWallpaper() }
    }

    // Only as the default home, another launcher may rely on the system wallpaper for its own look
    private suspend fun syncSystemWallpaper() = systemWallpaperLock.withLock {
        val preset = settingsStore.wallpaper.first()
        if (!homeRole.isDefault() || settingsStore.systemWallpaper.first() == preset.name) return@withLock
        if (systemWallpaper.set(preset)) settingsStore.setSystemWallpaper(preset)
    }

    suspend fun art(app: LaunchableApp): AppArt = repository.art(app)
}
