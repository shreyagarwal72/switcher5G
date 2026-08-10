package com.app.switcher5g.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val apkUrl: String,
    val isAvailable: Boolean = false,
)

/**
 * Handles checking for updates from GitHub Releases, downloading update APKs,
 * and prompting the Android package installer.
 */
object UpdateManager {

    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/shreyagarwal72/switcher5G/releases/latest"
    private const val USER_AGENT = "Switcher5G-Android-App"

    suspend fun checkForUpdates(currentVersion: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("UpdateManager", "Checking for updates via $GITHUB_RELEASES_API (currentAppVersion=$currentVersion)")
            val url = URL(GITHUB_RELEASES_API)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            AppLogger.i("UpdateManager", "GitHub API HTTP response code: $responseCode")

            if (responseCode != 200) {
                val errText = conn.errorStream?.bufferedReader()?.readText() ?: ""
                AppLogger.w("UpdateManager", "GitHub API non-200 response ($responseCode): $errText")
                return@withContext UpdateInfo(false, currentVersion, "GitHub API HTTP $responseCode: $errText", "", false)
            }

            val jsonStr = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(jsonStr)

            val rawTag = json.optString("tag_name", "")
            val latestVersion = rawTag.removePrefix("v").trim()
            val body = json.optString("body", "Latest release from GitHub Actions CI/CD.")
            val assets = json.optJSONArray("assets")

            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val isNewer = isVersionNewer(latestVersion, currentVersion)
            AppLogger.i("UpdateManager", "Check success: latestVersion=$latestVersion, isNewer=$isNewer, apkUrl=$apkUrl")

            UpdateInfo(
                hasUpdate = isNewer && apkUrl.isNotBlank(),
                latestVersion = if (latestVersion.isBlank()) currentVersion else latestVersion,
                releaseNotes = body.ifBlank { "Latest release from GitHub." },
                apkUrl = apkUrl,
                isAvailable = apkUrl.isNotBlank(),
            )
        } catch (t: Throwable) {
            AppLogger.e("UpdateManager", "Failed to check for updates", t)
            UpdateInfo(false, currentVersion, "Error checking updates: ${t.message}", "", false)
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("UpdateManager", "Starting download from $apkUrl")

            var currentUrl = apkUrl
            var conn: HttpURLConnection
            var redirectCount = 0
            val maxRedirects = 5

            // Follow HTTP 301/302 redirects manually for GitHub asset CDN links
            while (true) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = false
                conn.connect()

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308
                ) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank() && redirectCount < maxRedirects) {
                        currentUrl = loc
                        redirectCount++
                        AppLogger.i("UpdateManager", "Following download redirect ($redirectCount) -> $currentUrl")
                        continue
                    }
                }
                break
            }

            val fileLength = conn.contentLengthLong
            AppLogger.i("UpdateManager", "Downloading APK size: $fileLength bytes")

            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val apkFile = File(cacheDir, "switcher5g-update.apk")
            if (apkFile.exists()) apkFile.delete()

            conn.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val data = ByteArray(8192)
                    var total = 0L
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val progress = total.toFloat() / fileLength.toFloat()
                            onProgress(progress)
                        }
                    }
                }
            }

            AppLogger.i("UpdateManager", "APK download complete. Saved file size: ${apkFile.length()} bytes")
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            true
        } catch (t: Throwable) {
            AppLogger.e("UpdateManager", "Failed to download update APK", t)
            false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            AppLogger.e("UpdateManager", "Cannot install: APK file does not exist")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                AppLogger.w("UpdateManager", "Permission REQUEST_INSTALL_PACKAGES not granted. Opening settings...")
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            AppLogger.i("UpdateManager", "Launching Package Installer with FileProvider URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            AppLogger.e("UpdateManager", "Failed to trigger package installer", t)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
