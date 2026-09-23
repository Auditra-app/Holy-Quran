package com.example.data.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

data class AppUpdateInfo(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val forceUpdate: Boolean,
    val updateNotes: String,
    val isUpdateAvailable: Boolean
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"

    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
    private const val KEY_APK_DOWNLOAD_URL = "apk_download_url"
    private const val KEY_FORCE_UPDATE = "force_update"
    private const val KEY_UPDATE_NOTES = "update_notes"

    private const val DEFAULT_APK_URL = "https://holy-quran-app-live.web.app/app-v1.0.0.bin"

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

    fun checkForUpdate(
        currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
        onResult: (AppUpdateInfo?) -> Unit
    ) {
        val config = remoteConfig
        if (config == null) {
            onResult(null)
            return
        }

        try {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour fetch interval
                .build()
            config.setConfigSettingsAsync(configSettings)

            val defaults = mapOf<String, Any>(
                KEY_LATEST_VERSION_CODE to currentVersionCode,
                KEY_LATEST_VERSION_NAME to BuildConfig.VERSION_NAME,
                KEY_APK_DOWNLOAD_URL to DEFAULT_APK_URL,
                KEY_FORCE_UPDATE to false,
                KEY_UPDATE_NOTES to "يتوفر إصدار جديد من تطبيق القرآن الكريم يتضمن تحسينات جديدة وإصلاحات للأداء."
            )
            config.setDefaultsAsync(defaults)

            config.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "RemoteConfig fetchAndActivate succeeded")
                    } else {
                        Log.w(TAG, "RemoteConfig fetch failed, fallback to defaults/cached values")
                    }

                    val latestCode = config.getLong(KEY_LATEST_VERSION_CODE)
                    val latestName = config.getString(KEY_LATEST_VERSION_NAME).ifBlank { BuildConfig.VERSION_NAME }
                    val downloadUrl = config.getString(KEY_APK_DOWNLOAD_URL).ifBlank { DEFAULT_APK_URL }
                    val isForce = config.getBoolean(KEY_FORCE_UPDATE)
                    val notes = config.getString(KEY_UPDATE_NOTES).ifBlank { "يتوفر إصدار جديد من تطبيق القرآن الكريم." }

                    val isAvailable = latestCode > currentVersionCode
                    val info = AppUpdateInfo(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        apkDownloadUrl = downloadUrl,
                        forceUpdate = isForce,
                        updateNotes = notes,
                        isUpdateAvailable = isAvailable
                    )
                    onResult(info)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for app updates", e)
            onResult(null)
        }
    }

    fun startApkDownload(context: Context, downloadUrl: String) {
        try {
            val url = downloadUrl.ifBlank { DEFAULT_APK_URL }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "جاري فتح رابط تنزيل التحديث المباشر 📥", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download intent", e)
            Toast.makeText(context, "تعذر فتح رابط التنزيل المباشر", Toast.LENGTH_SHORT).show()
        }
    }
}
