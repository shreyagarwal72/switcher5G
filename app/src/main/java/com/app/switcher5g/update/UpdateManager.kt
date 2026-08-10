package com.app.switcher5g.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.app.switcher5g.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val apkUrl: String,
    val isAvailable: Boolean = false,
)

/**
 * Handles checking for updates from GitHub Releases, downloading update APKs with
 * byte-range resumption & retry mechanism, and prompting the Android package installer.
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
            conn.setRequestProperty("Accept-Encoding", "identity")
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
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val apkFile = File(cacheDir, "switcher5g-update.apk")

        // Try HTTP connection with byte-range resumption first
        val success = downloadWithResumption(apkUrl, apkFile, onProgress)
        if (success && apkFile.exists() && apkFile.length() > 0) {
            AppLogger.i("UpdateManager", "HTTP download succeeded (${apkFile.length()} bytes). Prompting installer...")
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            return@withContext true
        }

        // Fallback to System DownloadManager if HTTP stream drops repeatedly
        AppLogger.w("UpdateManager", "Direct stream download incomplete. Launching System DownloadManager fallback...")
        downloadWithDownloadManager(context, apkUrl)
    }

    private suspend fun downloadWithResumption(
        apkUrl: String,
        apkFile: File,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        if (apkFile.exists()) apkFile.delete()

        var attempts = 0
        val maxAttempts = 6
        var totalBytesDownloaded = 0L
        var expectedTotalSize = -1L

        while (attempts < maxAttempts) {
            attempts++
            try {
                var currentUrl = apkUrl
                var conn: HttpURLConnection
                var redirectCount = 0
                val maxRedirects = 5

                // Resolve redirects manually
                while (true) {
                    val url = URL(currentUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.setRequestProperty("Accept-Encoding", "identity")
                    conn.setRequestProperty("Connection", "close")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.instanceFollowRedirects = false

                    if (totalBytesDownloaded > 0) {
                        conn.setRequestProperty("Range", "bytes=$totalBytesDownloaded-")
                        AppLogger.i("UpdateManager", "Resuming download attempt $attempts at byte $totalBytesDownloaded")
                    }

                    conn.connect()
                    val code = conn.responseCode

                    if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                        val loc = conn.getHeaderField("Location")
                        if (!loc.isNullOrBlank() && redirectCount < maxRedirects) {
                            currentUrl = loc
                            redirectCount++
                            continue
                        }
                    }
                    break
                }

                val responseCode = conn.responseCode
                AppLogger.i("UpdateManager", "Download response code: $responseCode (Attempt $attempts)")

                if (responseCode != 200 && responseCode != 206) {
                    AppLogger.w("UpdateManager", "Unexpected HTTP status $responseCode on attempt $attempts")
                    delay(1000)
                    continue
                }

                val contentLength = conn.contentLengthLong
                if (contentLength > 0 && expectedTotalSize <= 0) {
                    expectedTotalSize = if (responseCode == 206) contentLength + totalBytesDownloaded else contentLength
                }

                val append = responseCode == 206 && totalBytesDownloaded > 0
                val outputStream = FileOutputStream(apkFile, append)

                conn.inputStream.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesDownloaded += bytesRead
                            if (expectedTotalSize > 0) {
                                val progress = (totalBytesDownloaded.toFloat() / expectedTotalSize.toFloat()).coerceIn(0f, 1f)
                                onProgress(progress)
                            }
                        }
                    }
                }

                if (expectedTotalSize > 0 && totalBytesDownloaded >= expectedTotalSize) {
                    AppLogger.i("UpdateManager", "Download fully completed ($totalBytesDownloaded / $expectedTotalSize bytes)")
                    return@withContext true
                } else if (expectedTotalSize <= 0 && totalBytesDownloaded > 1000000) {
                    AppLogger.i("UpdateManager", "Stream closed naturally after $totalBytesDownloaded bytes")
                    return@withContext true
                }
            } catch (e: Exception) {
                AppLogger.w("UpdateManager", "Download attempt $attempts interrupted (${e.javaClass.simpleName}: ${e.message}). Bytes saved: $totalBytesDownloaded")
                totalBytesDownloaded = if (apkFile.exists()) apkFile.length() else 0L
                delay(1200)
            }
        }

        return@withContext apkFile.exists() && apkFile.length() > 5000000
    }

    private fun downloadWithDownloadManager(context: Context, apkUrl: String): Boolean {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) return false

            val uri = Uri.parse(apkUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Switcher 5G Update")
                setDescription("Downloading latest release APK…")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "switcher5g-update.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            dm.enqueue(request)
            AppLogger.i("UpdateManager", "Enqueued System DownloadManager request for $apkUrl")
            true
        } catch (t: Throwable) {
            AppLogger.e("UpdateManager", "DownloadManager fallback failed", t)
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
