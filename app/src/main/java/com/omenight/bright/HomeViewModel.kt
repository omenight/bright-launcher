package com.omenight.bright

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omenight.bright.data.AppArt
import com.omenight.bright.data.AppRepository
import com.omenight.bright.data.FavoritesStore
import com.omenight.bright.data.LaunchableApp
import com.omenight.bright.data.NetworkMonitor
import com.omenight.bright.data.NetworkStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val favorites: List<LaunchableApp> = emptyList(),
    val apps: List<LaunchableApp> = emptyList(),
    val favoritePackages: Set<String> = emptySet(),
    val loaded: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

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

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch { favoritesStore.toggle(app.packageName) }
    }

    suspend fun art(app: LaunchableApp): AppArt = repository.art(app)
}
