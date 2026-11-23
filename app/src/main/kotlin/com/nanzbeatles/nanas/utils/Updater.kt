/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.BuildConfig
import com.nanzbeatles.nanas.constants.DismissedUpdateVersionCodeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val changelog: String,
) {
    val hasValidDownloadUrl: Boolean
        get() {
            val trimmed = url.trim()
            return trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true)
        }
}

data class UpdateCheckResult(
    val updateInfo: UpdateInfo,
    val hasUpdate: Boolean,
    val shouldShowDialog: Boolean,
)

object Updater {
    private val VERSION_JSON_URL: String
        get() = SecurityGuard.updateJsonUrl
    private const val TIMEOUT_SECONDS = 7L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    var lastCheckTime: Long = -1L
        private set

    @Volatile
    private var cachedUpdateInfo: UpdateInfo? = null

    /**
     * Returns the installed app versionCode using BuildConfig (falling back to PackageInfo if needed).
     */
    fun getInstalledVersionCode(context: Context? = null): Int {
        val buildConfigCode = BuildConfig.VERSION_CODE
        if (buildConfigCode > 0) return buildConfigCode
        if (context != null) {
            return runCatching {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            }.getOrDefault(0)
        }
        return 0
    }

    /**
     * Returns true if the remote versionCode is strictly greater than the installed versionCode.
     */
    fun isUpdateAvailable(
        info: UpdateInfo,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
    ): Boolean = info.versionCode > currentVersionCode

    /**
     * Fetches and parses version.json from raw GitHub on Dispatchers.IO.
     * Fails silently and returns null on any network, HTTP, or JSON parsing error.
     */
    suspend fun fetchLatestUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val urlWithCacheBuster = "$VERSION_JSON_URL?t=${System.currentTimeMillis()}"
            if (!urlWithCacheBuster.startsWith("https://", ignoreCase = true)) {
                return@withContext null
            }

            val request = Request.Builder()
                .url(urlWithCacheBuster)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val info = parseUpdateInfo(body) ?: return@withContext null
                cachedUpdateInfo = info
                lastCheckTime = System.currentTimeMillis()
                info
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Automatic launch check:
     * - Fetches version.json on Dispatchers.IO.
     * - Compares versionCode against installed versionCode.
     * - Suppresses the dialog if the user previously clicked "Nanti" for this versionCode,
     *   while showing it again once a strictly newer versionCode is published.
     * - Silently returns null on any error.
     */
    suspend fun checkForUpdateOnLaunch(context: Context): UpdateCheckResult? =
        withContext(Dispatchers.IO) {
            try {
                val info = fetchLatestUpdate() ?: return@withContext null
                val currentVersionCode = getInstalledVersionCode(context)
                val hasUpdate = isUpdateAvailable(info, currentVersionCode)
                if (!hasUpdate) {
                    return@withContext UpdateCheckResult(
                        updateInfo = info,
                        hasUpdate = false,
                        shouldShowDialog = false,
                    )
                }

                val dismissedVersionCode = getDismissedVersionCode(context)
                val shouldShowDialog = info.versionCode > dismissedVersionCode

                UpdateCheckResult(
                    updateInfo = info,
                    hasUpdate = true,
                    shouldShowDialog = shouldShowDialog,
                )
            } catch (_: Throwable) {
                null
            }
        }

    /**
     * Manual check invoked from the Updater settings screen.
     * Ignores previously dismissed versionCode so the user can always check manually.
     */
    suspend fun checkForUpdateManual(context: Context? = null): Result<Pair<UpdateInfo, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlWithCacheBuster = "$VERSION_JSON_URL?t=${System.currentTimeMillis()}"
                require(urlWithCacheBuster.startsWith("https://", ignoreCase = true)) {
                    "URL pembaruan harus menggunakan HTTPS"
                }

                val request = Request.Builder()
                    .url(urlWithCacheBuster)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()

                val info = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Gagal memeriksa pembaruan (HTTP ${response.code})")
                    }
                    val body = response.body?.string()
                        ?: throw IllegalStateException("Respons server kosong")
                    parseUpdateInfo(body)
                        ?: throw IllegalStateException("Format version.json tidak valid")
                }

                cachedUpdateInfo = info
                lastCheckTime = System.currentTimeMillis()
                val hasUpdate = isUpdateAvailable(info, getInstalledVersionCode(context))
                info to hasUpdate
            }
        }

    /**
     * Reads the versionCode dismissed via the "Nanti" button from DataStore.
     */
    suspend fun getDismissedVersionCode(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            context.applicationContext.dataStore.data.first()[DismissedUpdateVersionCodeKey] ?: 0
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Saves the dismissed versionCode to DataStore so the dialog won't re-appear for the same version.
     */
    suspend fun dismissUpdateVersion(context: Context, versionCode: Int) {
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                context.applicationContext.dataStore.edit { prefs ->
                    prefs[DismissedUpdateVersionCodeKey] = versionCode
                }
            } catch (_: Throwable) {
                // Ignore persistence errors silently
            }
        }
    }

    /**
     * Opens the APK download URL via ACTION_VIEW if it is a valid http/https link.
     */
    fun openUpdateUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return
        }
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun getCachedUpdateInfo(): UpdateInfo? = cachedUpdateInfo

    fun hasNewerCachedUpdate(context: Context? = null): Boolean {
        val cached = cachedUpdateInfo ?: return false
        return isUpdateAvailable(cached, getInstalledVersionCode(context))
    }

    private fun parseUpdateInfo(body: String): UpdateInfo? {
        if (body.isBlank()) return null
        val json = try {
            JSONObject(body)
        } catch (_: Throwable) {
            return null
        }

        if (!json.has("versionCode") || json.isNull("versionCode")) return null
        val versionCode = when (val rawCode = json.opt("versionCode")) {
            is Int -> rawCode
            is Long -> rawCode.toInt()
            is Number -> {
                val asDouble = rawCode.toDouble()
                if (asDouble.isFinite() && asDouble % 1.0 == 0.0) rawCode.toInt() else return null
            }
            else -> return null
        }
        if (versionCode <= 0) return null

        if (!json.has("versionName") || json.isNull("versionName")) return null
        val rawVersionName = json.opt("versionName") as? String ?: return null
        val versionName = rawVersionName.trim()
        if (versionName.isEmpty()) return null

        val url = (json.opt("url") as? String)?.trim().orEmpty()
        val changelog = (json.opt("changelog") as? String)?.trim().orEmpty()

        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            url = url,
            changelog = changelog,
        )
    }
}
