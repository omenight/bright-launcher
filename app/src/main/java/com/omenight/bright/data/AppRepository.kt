package com.omenight.bright.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Artwork for an app card, a 16:9 TV banner when the app ships one, its icon otherwise. */
sealed interface AppArt {
    data class Banner(val image: ImageBitmap) : AppArt
    data class Icon(val image: ImageBitmap) : AppArt
}

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val artCache = LruCache<String, AppArt>(64)

    /** Emits the launchable apps now and again on every install, update or removal. */
    fun apps(): Flow<List<LaunchableApp>> = callbackFlow {
        trySend(Unit)
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
            override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(::changed)

            override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(::changed)

            private fun changed(packageName: String) {
                artCache.remove(packageName)
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        .conflate()
        .map { loadApps() }
        .flowOn(Dispatchers.IO)

    suspend fun art(app: LaunchableApp): AppArt = withContext(Dispatchers.IO) {
        artCache.get(app.packageName) ?: loadArt(app).also { artCache.put(app.packageName, it) }
    }

    fun launch(app: LaunchableApp) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: packageManager.getLaunchIntentForPackage(app.packageName)
            ?: Intent(Intent.ACTION_MAIN).setComponent(app.component)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun loadApps(): List<LaunchableApp> {
        val tvActivities = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val tvPackages = tvActivities.mapTo(HashSet()) { it.activityInfo.packageName }
        // TV entry points come first so distinctBy keeps them over phone ones
        return (tvActivities + query(Intent.CATEGORY_LAUNCHER))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val activity = info.activityInfo
                LaunchableApp(
                    packageName = activity.packageName,
                    label = info.loadLabel(packageManager).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                    isTvApp = activity.packageName in tvPackages,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun loadArt(app: LaunchableApp): AppArt {
        val activity = packageManager.getActivityInfo(app.component, 0)
        val banner = activity.loadBanner(packageManager)
            ?: activity.applicationInfo.loadBanner(packageManager)
        return if (banner != null) {
            AppArt.Banner(banner.toBitmap(BANNER_WIDTH, BANNER_HEIGHT).asImageBitmap())
        } else {
            AppArt.Icon(activity.loadIcon(packageManager).toBitmap(ICON_SIZE, ICON_SIZE).asImageBitmap())
        }
    }

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }

    private companion object {
        const val BANNER_WIDTH = 320
        const val BANNER_HEIGHT = 180
        const val ICON_SIZE = 144
    }
}
