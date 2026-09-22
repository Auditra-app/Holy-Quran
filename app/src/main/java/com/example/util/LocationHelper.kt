package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

object LocationHelper {

    private const val TAG = "LocationHelper"

    private const val PREFS_NAME = "quran_location_prefs"
    private const val KEY_LAT = "saved_lat"
    private const val KEY_LNG = "saved_lng"
    private const val KEY_CITY = "saved_city_name"
    private const val KEY_CALC_METHOD = "saved_calc_method"
    private const val KEY_SUMMER_TIME = "saved_summer_time"
    private const val KEY_FAJR_OFFSET = "saved_fajr_offset"
    private const val KEY_DHUHR_OFFSET = "saved_dhuhr_offset"
    private const val KEY_ASR_OFFSET = "saved_asr_offset"
    private const val KEY_MAGHRIB_OFFSET = "saved_maghrib_offset"
    private const val KEY_ISHA_OFFSET = "saved_isha_offset"

    fun saveLocationAndSettings(
        context: Context,
        lat: Double,
        lng: Double,
        cityName: String,
        calcMethod: String? = null,
        isSummerTime: Boolean? = null,
        fajrOffset: Int? = null,
        dhuhrOffset: Int? = null,
        asrOffset: Int? = null,
        maghribOffset: Int? = null,
        ishaOffset: Int? = null
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
                .putFloat(KEY_LAT, lat.toFloat())
                .putFloat(KEY_LNG, lng.toFloat())
                .putString(KEY_CITY, cityName)
            if (calcMethod != null) editor.putString(KEY_CALC_METHOD, calcMethod)
            if (isSummerTime != null) editor.putBoolean(KEY_SUMMER_TIME, isSummerTime)
            if (fajrOffset != null) editor.putInt(KEY_FAJR_OFFSET, fajrOffset)
            if (dhuhrOffset != null) editor.putInt(KEY_DHUHR_OFFSET, dhuhrOffset)
            if (asrOffset != null) editor.putInt(KEY_ASR_OFFSET, asrOffset)
            if (maghribOffset != null) editor.putInt(KEY_MAGHRIB_OFFSET, maghribOffset)
            if (ishaOffset != null) editor.putInt(KEY_ISHA_OFFSET, ishaOffset)
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location preferences", e)
        }
    }

    fun saveCalculationSettings(
        context: Context,
        calcMethod: String? = null,
        isSummerTime: Boolean? = null,
        fajrOffset: Int? = null,
        dhuhrOffset: Int? = null,
        asrOffset: Int? = null,
        maghribOffset: Int? = null,
        ishaOffset: Int? = null
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (calcMethod != null) editor.putString(KEY_CALC_METHOD, calcMethod)
            if (isSummerTime != null) editor.putBoolean(KEY_SUMMER_TIME, isSummerTime)
            if (fajrOffset != null) editor.putInt(KEY_FAJR_OFFSET, fajrOffset)
            if (dhuhrOffset != null) editor.putInt(KEY_DHUHR_OFFSET, dhuhrOffset)
            if (asrOffset != null) editor.putInt(KEY_ASR_OFFSET, asrOffset)
            if (maghribOffset != null) editor.putInt(KEY_MAGHRIB_OFFSET, maghribOffset)
            if (ishaOffset != null) editor.putInt(KEY_ISHA_OFFSET, ishaOffset)
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calculation settings", e)
        }
    }

    fun getSavedLocationAndSettings(context: Context): LocationSettingsData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAT, 30.0444f).toDouble()
        val lng = prefs.getFloat(KEY_LNG, 31.2357f).toDouble()
        val cityName = prefs.getString(KEY_CITY, "القاهرة • Cairo") ?: "القاهرة • Cairo"
        val method = prefs.getString(KEY_CALC_METHOD, "EGYPT") ?: "EGYPT"
        val isSummerTime = prefs.getBoolean(KEY_SUMMER_TIME, false)
        val fajrOffset = prefs.getInt(KEY_FAJR_OFFSET, 0)
        val dhuhrOffset = prefs.getInt(KEY_DHUHR_OFFSET, 0)
        val asrOffset = prefs.getInt(KEY_ASR_OFFSET, 0)
        val maghribOffset = prefs.getInt(KEY_MAGHRIB_OFFSET, 0)
        val ishaOffset = prefs.getInt(KEY_ISHA_OFFSET, 0)
        return LocationSettingsData(
            latitude = lat,
            longitude = lng,
            cityName = cityName,
            calculationMethod = method,
            isSummerTime = isSummerTime,
            fajrOffset = fajrOffset,
            dhuhrOffset = dhuhrOffset,
            asrOffset = asrOffset,
            maghribOffset = maghribOffset,
            ishaOffset = ishaOffset
        )
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onLocationRetrieved: (lat: Double, lng: Double, cityName: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission missing, returning saved location")
            val saved = getSavedLocationAndSettings(context)
            onLocationRetrieved(saved.latitude, saved.longitude, saved.cityName)
            return
        }

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = try { lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true } catch (e: Exception) { false }
            val isNetworkEnabled = try { lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true } catch (e: Exception) { false }

            if (!isGpsEnabled && !isNetworkEnabled) {
                Log.w(TAG, "All location providers disabled, using saved location")
                val saved = getSavedLocationAndSettings(context)
                onLocationRetrieved(saved.latitude, saved.longitude, saved.cityName)
                return
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val priority = if (isGpsEnabled) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val cityName = getCityNameFromCoordinates(context, location.latitude, location.longitude)
                    saveLocationAndSettings(context, location.latitude, location.longitude, cityName)
                    onLocationRetrieved(location.latitude, location.longitude, cityName)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            val cityName = getCityNameFromCoordinates(context, lastLoc.latitude, lastLoc.longitude)
                            saveLocationAndSettings(context, lastLoc.latitude, lastLoc.longitude, cityName)
                            onLocationRetrieved(lastLoc.latitude, lastLoc.longitude, cityName)
                        } else {
                            fetchFromLocationManager(context, onLocationRetrieved)
                        }
                    }.addOnFailureListener {
                        fetchFromLocationManager(context, onLocationRetrieved)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "FusedLocationProvider error, falling back", e)
                fetchFromLocationManager(context, onLocationRetrieved)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching location", e)
            fetchFromLocationManager(context, onLocationRetrieved)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFromLocationManager(
        context: Context,
        onLocationRetrieved: (lat: Double, lng: Double, cityName: String) -> Unit
    ) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm == null) {
                val saved = getSavedLocationAndSettings(context)
                onLocationRetrieved(saved.latitude, saved.longitude, saved.cityName)
                return
            }

            val providers = try { lm.getProviders(true) } catch (e: Throwable) { emptyList() }
            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    val l = lm.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not fetch last known location for provider: $provider", e)
                }
            }
            if (bestLocation != null) {
                val cityName = getCityNameFromCoordinates(context, bestLocation.latitude, bestLocation.longitude)
                saveLocationAndSettings(context, bestLocation.latitude, bestLocation.longitude, cityName)
                onLocationRetrieved(bestLocation.latitude, bestLocation.longitude, cityName)
            } else {
                val saved = getSavedLocationAndSettings(context)
                onLocationRetrieved(saved.latitude, saved.longitude, saved.cityName)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "LocationManager fallback failed", e)
            val saved = getSavedLocationAndSettings(context)
            onLocationRetrieved(saved.latitude, saved.longitude, saved.cityName)
        }
    }

    private fun getCityNameFromCoordinates(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("ar"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locality = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                locality ?: "موقعي الحالي"
            } else {
                "موقعي الحالي"
            }
        } catch (e: Throwable) {
            "موقعي الحالي"
        }
    }
}

data class LocationSettingsData(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val calculationMethod: String,
    val isSummerTime: Boolean,
    val fajrOffset: Int = 0,
    val dhuhrOffset: Int = 0,
    val asrOffset: Int = 0,
    val maghribOffset: Int = 0,
    val ishaOffset: Int = 0
)
