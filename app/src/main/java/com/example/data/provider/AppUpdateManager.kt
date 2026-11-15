package com.example.data.provider

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val forceUpdate: Boolean,
    val updateNotes: String,
    val updateNotesAr: String = updateNotes,
    val updateNotesEn: String = updateNotes,
    val isUpdateAvailable: Boolean
) {
    fun getLocalizedNotes(isArabic: Boolean): String {
        return if (isArabic) {
            updateNotesAr.ifBlank { updateNotes }
        } else {
            updateNotesEn.ifBlank { updateNotes }
        }
    }
}

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"

    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
    private const val KEY_APK_DOWNLOAD_URL = "apk_download_url"
    private const val KEY_FORCE_UPDATE = "force_update"
    private const val KEY_UPDATE_NOTES = "update_notes"
    private const val KEY_UPDATE_NOTES_AR = "update_notes_ar"
    private const val KEY_UPDATE_NOTES_EN = "update_notes_en"

    private const val STATIC_VERSION_JSON_URL = "https://holyquranapp.web.app/version.json"
    private const val FALLBACK_STATIC_VERSION_JSON_URL = "https://holy-quran-app-live.web.app/version.json"
    private const val DEFAULT_APK_URL = "https://holyquranapp.web.app/HolyQuran_v1.1.0.data"
    private const val DEFAULT_UPDATE_NOTES = "تحديث جديد v1.2.0: تحسينات متقدمة في السرعة والأداء، دعم التحديث المباشر بضغطة واحدة، وتطوير واجهة المصحف الشريف."

    private var pendingDownloadId: Long = -1L
    private var pendingDownloadFileName: String? = null
    private var isReceiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L && downloadId == pendingDownloadId) {
                    promptInstallDownloadedApk(context, downloadId)
                }
            }
        }
    }

    private fun registerDownloadReceiver(context: Context) {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                ContextCompat.registerReceiver(
                    context.applicationContext,
                    downloadReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register download receiver", e)
            }
        }
    }

    private fun promptInstallDownloadedApk(context: Context, downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            var resolvedUri: Uri? = null

            // 1. First priority: Query DownloadManager cursor for exact local file path of this downloadId
            if (downloadManager != null) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    downloadManager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriIndex != -1) {
                                val localUriStr = cursor.getString(uriIndex)
                                if (!localUriStr.isNullOrBlank()) {
                                    val localUri = Uri.parse(localUriStr)
                                    val path = localUri.path
                                    if (path != null) {
                                        val downloadedFile = File(path)
                                        if (downloadedFile.exists() && downloadedFile.length() > 0) {
                                            resolvedUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                downloadedFile
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed resolving downloaded file via query cursor", e)
                }
            }

            // 2. Second priority: Check specific pendingDownloadFileName in Downloads
            if (resolvedUri == null && !pendingDownloadFileName.isNullOrBlank()) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, pendingDownloadFileName!!)
                if (targetFile.exists() && targetFile.length() > 0) {
                    resolvedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )
                }
            }

            // 3. Third priority: downloadManager.getUriForDownloadedFile
            if (resolvedUri == null && downloadManager != null) {
                resolvedUri = downloadManager.getUriForDownloadedFile(downloadId)
            }

                        if (resolvedUri != null) {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    setDataAndType(resolvedUri, "application/vnd.android.package-archive")
                }
                context.startActivity(installIntent)
                Toast.makeText(context, "اكتمل التنزيل! ⚠️ تذكير: يرجى إلغاء تثبيت الإصدار القديم أولاً ثم تثبيت هذا التحديث لتجنب تعارض الحزم 🚀", Toast.LENGTH_LONG).show()
            } else {
                val targetName = pendingDownloadFileName ?: "ملف التحديث"
                Toast.makeText(context, "اكتمل التنزيل! ⚠️ احذف الإصدار القديم وافتح $targetName من التنزيلات 📥", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error prompting APK installation", e)
            Toast.makeText(context, "اكتمل التنزيل! تفقد مجلد التنزيلات (Downloads) لتثبيت ملف التحديث 📥", Toast.LENGTH_LONG).show()
        }
    }

    private val remoteConfig: FirebaseRemoteConfig?
        get() = try {
            FirebaseRemoteConfig.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseRemoteConfig unavailable: ${e.localizedMessage}")
            null
        }

    fun isFirebaseConfigAvailable(): Boolean {
        return remoteConfig != null
    }

    fun fetchStaticVersionJson(onResult: (AppUpdateInfo?) -> Unit) {
        Thread {
            val endpoints = listOf(STATIC_VERSION_JSON_URL, FALLBACK_STATIC_VERSION_JSON_URL)
            var resolvedInfo: AppUpdateInfo? = null

            for (endpoint in endpoints) {
                try {
                    val cacheBusterUrl = if (endpoint.contains("?")) "$endpoint&_t=${System.currentTimeMillis()}" else "$endpoint?_t=${System.currentTimeMillis()}"
                    val url = URL(cacheBusterUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    connection.setRequestProperty("Pragma", "no-cache")
                    connection.useCaches = false

                    if (connection.responseCode == 200) {
                        val jsonText = connection.inputStream.bufferedReader(java.nio.charset.StandardCharsets.UTF_8).use { it.readText() }
                        val json = JSONObject(jsonText)
                        val remoteCode = json.optLong("latest_version_code", 4L)
                        val remoteName = json.optString("latest_version_name", "1.2.0")
                        val downloadUrl = json.optString("apk_download_url", DEFAULT_APK_URL)
                        val isForce = json.optBoolean("force_update", false)
                        val notes = json.optString("update_notes", DEFAULT_UPDATE_NOTES)
                        val notesAr = json.optString("update_notes_ar", "").ifBlank { notes }
                        val notesEn = json.optString("update_notes_en", "").ifBlank { notes }

                        val currentCode = BuildConfig.VERSION_CODE.toLong()
                        val isAvailable = remoteCode > currentCode

                        resolvedInfo = AppUpdateInfo(
                            latestVersionCode = remoteCode,
                            latestVersionName = remoteName,
                            apkDownloadUrl = downloadUrl,
                            forceUpdate = isForce,
                            updateNotes = notes,
                            updateNotesAr = notesAr,
                            updateNotesEn = notesEn,
                            isUpdateAvailable = isAvailable
                        )
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch version from $endpoint: ${e.message}")
                }
            }

            Handler(Looper.getMainLooper()).post { onResult(resolvedInfo) }
        }.start()
    }

    fun checkForUpdate(
        currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
        onResult: (AppUpdateInfo?) -> Unit
    ) {
        var hasResponded = false
        fun respondOnce(info: AppUpdateInfo?) {
            if (!hasResponded) {
                hasResponded = true
                onResult(info)
            }
        }

        // 1. Fetch static version.json from Firebase Hosting first (Fastest and most reliable)
        fetchStaticVersionJson { staticInfo ->
            if (staticInfo != null) {
                respondOnce(staticInfo)
                return@fetchStaticVersionJson
            }

            // 2. If static JSON fails, try Firebase Remote Config as fallback
            val config = remoteConfig
            if (config == null) {
                respondOnce(null)
                return@fetchStaticVersionJson
            }

            try {
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
                config.setConfigSettingsAsync(configSettings)

                config.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        val remoteCode = config.getLong(KEY_LATEST_VERSION_CODE).let { if (it == 0L) config.getLong("latestVersionCode") else it }
                        val remoteName = config.getString(KEY_LATEST_VERSION_NAME).ifBlank { config.getString("latestVersionName") }
                        val downloadUrl = config.getString(KEY_APK_DOWNLOAD_URL).ifBlank { config.getString("apkDownloadUrl") }
                        val isForce = config.getBoolean(KEY_FORCE_UPDATE) || config.getBoolean("forceUpdate")
                        val notes = config.getString(KEY_UPDATE_NOTES).ifBlank { config.getString("updateNotes") }
                        val notesAr = config.getString(KEY_UPDATE_NOTES_AR).ifBlank { notes }
                        val notesEn = config.getString(KEY_UPDATE_NOTES_EN).ifBlank { notes }

                        val isAvailable = remoteCode > currentVersionCode
                        val info = AppUpdateInfo(
                            latestVersionCode = remoteCode,
                            latestVersionName = remoteName,
                            apkDownloadUrl = downloadUrl.ifBlank { DEFAULT_APK_URL },
                            forceUpdate = isForce,
                            updateNotes = notes.ifBlank { DEFAULT_UPDATE_NOTES },
                            updateNotesAr = notesAr.ifBlank { DEFAULT_UPDATE_NOTES },
                            updateNotesEn = notesEn.ifBlank { DEFAULT_UPDATE_NOTES },
                            isUpdateAvailable = isAvailable
                        )
                        respondOnce(info)
                    }
                    .addOnFailureListener {
                        respondOnce(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Firebase Remote Config", e)
                respondOnce(null)
            }
        }
    }

    fun startApkDownload(context: Context, downloadUrl: String, targetVersionName: String = "") {
        val url = downloadUrl.ifBlank { DEFAULT_APK_URL }
        registerDownloadReceiver(context)

        // Derive version-specific filename from downloadUrl or targetVersionName
        val resolvedFileName = try {
            val lastSegment = Uri.parse(url).lastPathSegment
            if (!lastSegment.isNullOrBlank() && (lastSegment.endsWith(".apk", ignoreCase = true) || lastSegment.endsWith(".data", ignoreCase = true))) {
                lastSegment.substringBeforeLast(".") + ".apk"
            } else if (targetVersionName.isNotBlank()) {
                "HolyQuran_v${targetVersionName.trimStart('v')}.apk"
            } else {
                "HolyQuran_Update_${System.currentTimeMillis()}.apk"
            }
        } catch (e: Exception) {
            if (targetVersionName.isNotBlank()) "HolyQuran_v${targetVersionName.trimStart('v')}.apk" else "HolyQuran_Update.apk"
        }

        pendingDownloadFileName = resolvedFileName

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager != null) {
                // Remove stale previous download file if exists
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val oldApk = File(downloadsDir, resolvedFileName)
                    if (oldApk.exists()) {
                        oldApk.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete old update APK file: ${e.message}")
                }

                val uri = Uri.parse(url)
                val request = DownloadManager.Request(uri).apply {
                    setTitle("تحديث تطبيق القرآن الكريم 🚀")
                    setDescription("جاري تنزيل أحدث إصدار ($resolvedFileName)...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resolvedFileName)
                    setMimeType("application/vnd.android.package-archive")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                pendingDownloadId = downloadManager.enqueue(request)
                incrementDownloadCount()
                Toast.makeText(context, "بدأ تنزيل التحديث في الخلفية 📥 تفقد شريط الإشعارات", Toast.LENGTH_LONG).show()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager enqueue failed", e)
        }

        // Web Fallback if DownloadManager fails
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://holyquranapp.web.app/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "جاري فتح صفحة تنزيل التحديث المباشر 📥", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start browser download intent", e)
            Toast.makeText(context, "تعذر فتح رابط التنزيل المباشر", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        Thread {
            try {
                val url = java.net.URL("https://firestore.googleapis.com/v1/projects/holy-quran-app-live/databases/(default)/documents:commit?key=AIzaSyAkUKpq947M8UmFULf4qH6UUwBhH0BiDS0")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.doOutput = true

                val payload = """
                    {
                      "writes": [
                        {
                          "transform": {
                            "document": "projects/holy-quran-app-live/databases/(default)/documents/stats/downloads",
                            "fieldTransforms": [
                              {
                                "fieldPath": "count",
                                "increment": { "integerValue": "1" }
                              },
                              {
                                "fieldPath": "lastUpdated",
                                "setToServerValue": "REQUEST_TIME"
                              }
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }
                connection.responseCode
            } catch (e: Exception) {
                Log.w(TAG, "Failed to increment download counter", e)
            }
        }.start()
    }

    fun showUpdateAvailableNotification(context: Context, updateInfo: AppUpdateInfo, isArabic: Boolean = true) {
        try {
            com.example.notification.NotificationHelper.createNotificationChannels(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val builder = com.example.notification.NotificationHelper.buildAppUpdateNotification(
                context = context,
                versionName = updateInfo.latestVersionName,
                versionCode = updateInfo.latestVersionCode,
                apkDownloadUrl = updateInfo.apkDownloadUrl,
                isArabic = isArabic
            )
            nm.notify(9701, builder.build())
            Log.d(TAG, "Posted red app update notification for v${updateInfo.latestVersionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting update notification", e)
        }
    }

}