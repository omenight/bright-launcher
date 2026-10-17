package com.omenight.bright.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from GitHub releases. Checks every few hours and downloads a newer APK in the background,
 * but installs only when the user asks, since installing restarts the home screen.
 */
class Updater private constructor(private val context: Context) {

    sealed interface State {
        /** Debug builds carry another app id and key, so they never update themselves. */
        data object Disabled : State
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Downloading(val version: String) : State
        data class Ready(val version: String) : State
        data class Installing(val version: String) : State
        data object Failed : State
    }

    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    private val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val _state = MutableStateFlow(if (debuggable) State.Disabled else State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val settings = SettingsStore(context)
    private val dir = File(context.cacheDir, "update")
    // Set while the user is away granting the install permission, so coming back continues the install
    private var installAfterGrant = false

    /** Called on every resume, checks only when the last check is old enough. */
    fun onResume() {
        if (_state.value == State.Disabled) return
        if (installAfterGrant) {
            installAfterGrant = false
            if (canInstall()) install()
            return
        }
        scope.launch { check(manual = false) }
    }

    fun checkNow() {
        if (_state.value == State.Disabled) return
        scope.launch { check(manual = true) }
    }

    // Background checks fail silently and keep the previous state, a manual one shows the failure
    private suspend fun check(manual: Boolean) {
        if (!lock.tryLock()) return
        try {
            if (_state.value == State.Idle) cached()?.let { _state.value = State.Ready(it) }
            val before = _state.value
            if (before !in listOf(State.Idle, State.UpToDate, State.Failed) && !(manual && before is State.Ready)) return
            if (!manual && System.currentTimeMillis() - settings.updateCheckedAt.first() < CheckInterval) return

            if (manual) _state.value = State.Checking
            val release = runCatching { latestRelease() }.getOrNull()
            val result = when {
                release == null -> null
                !isNewer(release.version, currentVersion) -> State.UpToDate
                apkFile(release.version).exists() -> State.Ready(release.version)
                else -> {
                    _state.value = State.Downloading(release.version)
                    if (runCatching { download(release) }.getOrDefault(false)) State.Ready(release.version) else null
                }
            }
            if (result == State.UpToDate) clean(keep = null)
            if (result != null) settings.setUpdateCheckedAt(System.currentTimeMillis())
            _state.value = result ?: if (manual) State.Failed else before
        } finally {
            lock.unlock()
        }
    }

    /** Installs the downloaded update, first sending the user to grant the install permission if needed. */
    fun install() {
        val version = (_state.value as? State.Ready)?.version ?: return
        if (!canInstall()) {
            installAfterGrant = openInstallPermission()
            if (!installAfterGrant) _state.value = State.Failed
            return
        }
        _state.value = State.Installing(version)
        scope.launch {
            runCatching { commit(apkFile(version)) }.onFailure { _state.value = State.Failed }
        }
    }

    internal fun onInstallStatus(status: Int, confirm: Intent?) {
        val version = (_state.value as? State.Installing)?.version
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val launched = confirm != null && runCatching {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
                if (!launched) _state.value = State.Failed
            }
            // Success replaces this process, nothing is left to do
            PackageInstaller.STATUS_SUCCESS -> Unit
            // Cancelled on the confirm screen, the download stays ready
            PackageInstaller.STATUS_FAILURE_ABORTED -> if (version != null) _state.value = State.Ready(version)
            else -> _state.value = State.Failed
        }
    }

    private fun canInstall() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    private fun openInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val forBright = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
        // Some TV settings apps only have the list of all apps, not the page for one
        return listOf(forBright, Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)).any { intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    private fun commit(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            // Android 12+ lets an app update itself without the confirm screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        try {
            installer.openSession(id).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                // Mutable because the installer adds the status extras to it
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val callback = PendingIntent.getBroadcast(context, id, Intent(context, UpdateReceiver::class.java), flags)
                session.commit(callback.intentSender)
            }
        } catch (e: Exception) {
            installer.abandonSession(id)
            throw e
        }
    }

    private class Release(val version: String, val url: String, val size: Long)

    private fun latestRelease(): Release? {
        val json = JSONObject(get(LatestReleaseUrl) { it.reader().readText() })
        val asset = json.getJSONArray("assets").let { assets ->
            (0 until assets.length()).map(assets::getJSONObject).firstOrNull { it.getString("name") == ApkAsset }
        } ?: return null
        return Release(
            version = json.getString("tag_name").removePrefix("v"),
            url = asset.getString("browser_download_url"),
            size = asset.getLong("size"),
        )
    }

    private fun download(release: Release): Boolean {
        dir.mkdirs()
        val target = apkFile(release.version)
        val part = File(dir, "${target.name}.part")
        get(release.url) { input -> part.outputStream().use { input.copyTo(it) } }
        if (part.length() != release.size || !part.renameTo(target)) return false.also { part.delete() }
        if (!isOurApk(target, release.version)) return false.also { target.delete() }
        clean(keep = target)
        return true
    }

    private fun <T> get(url: String, read: (java.io.InputStream) -> T): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Bright/$currentVersion")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode} for $url" }
            return connection.inputStream.use(read)
        } finally {
            connection.disconnect()
        }
    }

    // A download left by an earlier run that is still newer than what is installed
    private fun cached(): String? {
        val version = dir.listFiles().orEmpty()
            .mapNotNull { ApkName.matchEntire(it.name)?.groupValues?.get(1) }
            .filter { isNewer(it, currentVersion) }
            .maxWithOrNull { a, b -> if (isNewer(a, b)) 1 else if (isNewer(b, a)) -1 else 0 }
        val apk = version?.let(::apkFile)
        clean(keep = apk?.takeIf { isOurApk(it, version) })
        return version?.takeIf { apk?.exists() == true }
    }

    private fun clean(keep: File?) {
        dir.listFiles().orEmpty().filter { it != keep }.forEach { it.delete() }
    }

    private fun isOurApk(apk: File, version: String): Boolean =
        context.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?.let { it.packageName == context.packageName && it.versionName == version } == true

    private fun apkFile(version: String) = File(dir, "bright-$version.apk")

    companion object {
        private const val LatestReleaseUrl = "https://api.github.com/repos/omenight/bright-launcher/releases/latest"
        // The stable asset name the release workflow publishes next to the versioned one
        private const val ApkAsset = "bright.apk"
        private val ApkName = Regex("""bright-(.+)\.apk""")
        private const val CheckInterval = 6 * 60 * 60 * 1000L

        // Holds only the application context, so the singleton outlives no activity
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Updater? = null

        fun get(context: Context): Updater = instance ?: synchronized(this) {
            instance ?: Updater(context.applicationContext).also { instance = it }
        }

        /** Compares dotted versions numerically, ignoring any `-suffix`. */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff > 0
            }
            return false
        }
    }
}

/** Receives the result of an update install session. Not exported, only Bright's own PendingIntent reaches it. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Updater.get(context).onInstallStatus(
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java),
        )
    }
}
