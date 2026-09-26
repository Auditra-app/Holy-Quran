package com.example.data.provider

import android.util.Log
import com.example.domain.model.GroupKhatma
import com.example.domain.model.GroupKhatmaJuz
import com.example.domain.model.GroupKhatmaJuzStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FirebaseKhatmaRepository {

    private const val TAG = "FirebaseKhatmaRepo"
    private const val COLLECTION_KHATMAS = "khatmas"
    private const val PROJECT_ID = "holy-quran-app-live"
    private const val API_KEY = "AIzaSyAkUKpq947M8UmFULf4qH6UUwBhH0BiDS0"
    private const val REST_BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_KHATMAS"

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore instance unavailable: ${e.localizedMessage}")
            null
        }

    fun isFirebaseAvailable(): Boolean {
        return firestore != null
    }

    /**
     * Listens to real-time live updates for a specific Khatma ID from Cloud Firestore.
     * Supports both Firestore SDK snapshot listeners and automatic periodic REST polling fallback.
     */
    fun observeKhatma(khatmaId: String): Flow<GroupKhatma?> = callbackFlow {
        val cleanId = khatmaId.trim()
        if (cleanId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val db = firestore
        val listenerRegistration = if (db != null) {
            try {
                val docRef = db.collection(COLLECTION_KHATMAS).document(cleanId)
                docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in Khatma $cleanId snapshot: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data != null) {
                            val khatma = parseFirestoreData(cleanId, data)
                            trySend(khatma)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to attach snapshot listener for $cleanId: ${e.message}")
                null
            }
        } else null

        // Background continuous polling every 4 seconds to guarantee real-time sync across all devices
        val pollJob = CoroutineScope(Dispatchers.IO).launch {
            // Initial direct fetch
            try {
                val initial = fetchKhatmaOnce(cleanId)
                if (initial != null) {
                    trySend(initial)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Initial fetch failed for $cleanId in observer", e)
            }

            // Continuous polling every 4 seconds while active to catch any updates made by other users immediately
            while (isActive) {
                delay(4000L)
                try {
                    val polled = fetchKhatmaOnceRest(cleanId) ?: fetchKhatmaOnce(cleanId)
                    if (polled != null) {
                        trySend(polled)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Periodic sync poll failed for $cleanId: ${e.message}")
                }
            }
        }

        awaitClose {
            listenerRegistration?.remove()
            pollJob.cancel()
        }
    }

    /**
     * Fetches a GroupKhatma document once from Cloud Firestore.
     * Uses Firestore SDK first with strict timeout, with automatic REST API fallback.
     */
    suspend fun fetchKhatmaOnce(khatmaId: String): GroupKhatma? = withContext(Dispatchers.IO) {
        val cleanId = khatmaId.trim()
        if (cleanId.isBlank()) return@withContext null

        // 1. Try Firestore SDK first with timeout
        val db = firestore
        if (db != null) {
            try {
                val snapshot = kotlinx.coroutines.withTimeout(3500L) {
                    val docRef = db.collection(COLLECTION_KHATMAS).document(cleanId)
                    docRef.get().await()
                }
                if (snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        Log.d(TAG, "Successfully fetched Khatma $cleanId via Firestore SDK")
                        return@withContext parseFirestoreData(cleanId, data)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore SDK fetch failed/timed out for $cleanId: ${e.message}. Trying REST fallback...")
            }
        }

        // 2. Resilient REST API fallback
        return@withContext fetchKhatmaOnceRest(cleanId)
    }

    /**
     * Publishes or updates an entire GroupKhatma in Cloud Firestore.
     * Uses Firestore SDK first with timeout, with automatic REST API fallback and async mirror.
     */
    suspend fun publishKhatma(khatma: GroupKhatma): Boolean = withContext(Dispatchers.IO) {
        val cleanId = khatma.id.trim()
        if (cleanId.isBlank()) return@withContext false

        // 1. Try Firestore SDK first with strict timeout
        val db = firestore
        var sdkSuccess = false
        if (db != null) {
            try {
                kotlinx.coroutines.withTimeout(3500L) {
                    val docRef = db.collection(COLLECTION_KHATMAS).document(cleanId)
                    val mapData = khatmaToFirestoreMap(khatma)
                    docRef.set(mapData, SetOptions.merge()).await()
                    sdkSuccess = true
                    Log.d(TAG, "Successfully published Khatma $cleanId via Firestore SDK")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore SDK publish failed/timed out for $cleanId: ${e.message}. Using REST...")
            }
        }

        // 2. Resilient REST API fallback & guaranteed global sync
        val restSuccess = if (!sdkSuccess) {
            publishKhatmaRest(khatma)
        } else {
            // Asynchronously ensure REST is synced in background without blocking
            CoroutineScope(Dispatchers.IO).launch {
                try { publishKhatmaRest(khatma) } catch (e: Throwable) { Log.w(TAG, "Async REST mirror error", e) }
            }
            true
        }

        return@withContext sdkSuccess || restSuccess
    }

    /**
     * Real-time update of a single Juz status in Cloud Firestore.
     */
    suspend fun updateJuzStatusCloud(
        khatmaId: String,
        juzNumber: Int,
        userName: String,
        newStatus: GroupKhatmaJuzStatus
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanId = khatmaId.trim()
        if (cleanId.isBlank()) return@withContext false

        val currentKhatma = fetchKhatmaOnce(cleanId)
        val khatmaToUpdate = if (currentKhatma != null) {
            currentKhatma
        } else {
            val defaultJuzList = (1..30).map { GroupKhatmaJuz(juzNumber = it) }
            GroupKhatma(id = cleanId, title = "ختمة قرآنية جماعية", creatorName = userName.ifBlank { "مشارك" }, juzList = defaultJuzList)
        }

        val updatedJuzList = khatmaToUpdate.juzList.map { juz ->
            if (juz.juzNumber == juzNumber) {
                juz.copy(
                    status = newStatus,
                    assignedUserName = if (newStatus == GroupKhatmaJuzStatus.UNASSIGNED) "" else userName.trim(),
                    completedTimestamp = if (newStatus == GroupKhatmaJuzStatus.COMPLETED) System.currentTimeMillis() else juz.completedTimestamp
                )
            } else {
                juz
            }
        }

        val updatedKhatma = khatmaToUpdate.copy(juzList = updatedJuzList)
        return@withContext publishKhatma(updatedKhatma)
    }

    // ==========================================
    // REST API Implementation for Cloud Firestore
    // ==========================================

    private fun fetchKhatmaOnceRest(cleanId: String): GroupKhatma? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$REST_BASE_URL/$cleanId?key=$API_KEY")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val parsed = parseRestJson(cleanId, json)
                Log.d(TAG, "Successfully fetched Khatma $cleanId via Firestore REST API")
                parsed
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "Khatma $cleanId not found in Cloud Firestore (HTTP 404)")
                null
            } else {
                Log.w(TAG, "Firestore REST fetch failed for $cleanId: HTTP $responseCode")
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during Firestore REST fetch for $cleanId: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun publishKhatmaRest(khatma: GroupKhatma): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val cleanId = khatma.id.trim()
            val url = URL("$REST_BASE_URL/$cleanId?key=$API_KEY")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val restJson = khatmaToRestJson(khatma)
            connection.outputStream.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(restJson.toString())
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            val isSuccess = responseCode in 200..299
            if (isSuccess) {
                Log.d(TAG, "Successfully published Khatma $cleanId via Firestore REST API (HTTP $responseCode)")
            } else {
                val errorStream = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText)
                Log.w(TAG, "Firestore REST publish failed for $cleanId: HTTP $responseCode - $errorStream")
            }
            isSuccess
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during Firestore REST publish for ${khatma.id}: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun khatmaToRestJson(khatma: GroupKhatma): JSONObject {
        val fields = JSONObject()
        fields.put("id", JSONObject().put("stringValue", khatma.id))
        fields.put("title", JSONObject().put("stringValue", khatma.title))
        fields.put("creatorName", JSONObject().put("stringValue", khatma.creatorName))
        fields.put("createdAt", JSONObject().put("integerValue", khatma.createdAt.toString()))
        fields.put("targetDeadlineTimestamp", JSONObject().put("integerValue", khatma.targetDeadlineTimestamp.toString()))
        fields.put("lastUpdated", JSONObject().put("integerValue", System.currentTimeMillis().toString()))

        val juzValues = JSONArray()
        khatma.juzList.forEach { j ->
            val jMap = JSONObject()
            jMap.put("num", JSONObject().put("integerValue", j.juzNumber.toString()))
            jMap.put("st", JSONObject().put("stringValue", j.status.name))
            jMap.put("usr", JSONObject().put("stringValue", j.assignedUserName))
            jMap.put("ts", JSONObject().put("integerValue", j.completedTimestamp.toString()))

            val valObj = JSONObject()
            valObj.put("mapValue", JSONObject().put("fields", jMap))
            juzValues.put(valObj)
        }

        fields.put("juzList", JSONObject().put("arrayValue", JSONObject().put("values", juzValues)))

        val root = JSONObject()
        root.put("fields", fields)
        return root
    }

    private fun parseRestJson(cleanId: String, json: JSONObject): GroupKhatma? {
        val fields = json.optJSONObject("fields") ?: return null

        val title = fields.optJSONObject("title")?.optString("stringValue") ?: "ختمة قرآنية جماعية"
        val creatorName = fields.optJSONObject("creatorName")?.optString("stringValue") ?: ""
        
        val createdAt = fields.optJSONObject("createdAt")?.let {
            it.optLong("integerValue", it.optString("integerValue", "0").toLongOrNull() ?: 0L)
        } ?: System.currentTimeMillis()

        val targetDeadlineTimestamp = fields.optJSONObject("targetDeadlineTimestamp")?.let {
            it.optLong("integerValue", it.optString("integerValue", "0").toLongOrNull() ?: 0L)
        } ?: 0L

        val juzList = mutableListOf<GroupKhatmaJuz>()
        val arrayValues = fields.optJSONObject("juzList")?.optJSONObject("arrayValue")?.optJSONArray("values")
        if (arrayValues != null) {
            for (i in 0 until arrayValues.length()) {
                val item = arrayValues.optJSONObject(i)?.optJSONObject("mapValue")?.optJSONObject("fields")
                if (item != null) {
                    val num = item.optJSONObject("num")?.let {
                        it.optInt("integerValue", it.optString("integerValue", "${i + 1}").toIntOrNull() ?: (i + 1))
                    } ?: (i + 1)

                    val stStr = item.optJSONObject("st")?.optString("stringValue") ?: GroupKhatmaJuzStatus.UNASSIGNED.name
                    val status = try {
                        GroupKhatmaJuzStatus.valueOf(stStr)
                    } catch (e: Exception) {
                        GroupKhatmaJuzStatus.UNASSIGNED
                    }

                    val usr = item.optJSONObject("usr")?.optString("stringValue") ?: ""
                    val ts = item.optJSONObject("ts")?.let {
                        it.optLong("integerValue", it.optString("integerValue", "0").toLongOrNull() ?: 0L)
                    } ?: 0L

                    juzList.add(GroupKhatmaJuz(juzNumber = num, status = status, assignedUserName = usr, completedTimestamp = ts))
                }
            }
        }

        if (juzList.isEmpty()) {
            for (n in 1..30) {
                juzList.add(GroupKhatmaJuz(juzNumber = n))
            }
        }

        return GroupKhatma(
            id = cleanId,
            title = title,
            creatorName = creatorName,
            createdAt = createdAt,
            targetDeadlineTimestamp = targetDeadlineTimestamp,
            juzList = juzList
        )
    }

    private fun khatmaToFirestoreMap(khatma: GroupKhatma): Map<String, Any> {
        val juzListMap = khatma.juzList.map { j ->
            mapOf(
                "num" to j.juzNumber,
                "st" to j.status.name,
                "usr" to j.assignedUserName,
                "ts" to j.completedTimestamp
            )
        }
        return mapOf(
            "id" to khatma.id,
            "title" to khatma.title,
            "creatorName" to khatma.creatorName,
            "createdAt" to khatma.createdAt,
            "targetDeadlineTimestamp" to khatma.targetDeadlineTimestamp,
            "juzList" to juzListMap,
            "lastUpdated" to System.currentTimeMillis()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFirestoreData(id: String, data: Map<String, Any>): GroupKhatma {
        val title = (data["title"] as? String) ?: "ختمة قرآنية جماعية"
        val creatorName = (data["creatorName"] as? String) ?: ""
        val createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
        val targetDeadlineTimestamp = (data["targetDeadlineTimestamp"] as? Long) ?: 0L

        val juzList = mutableListOf<GroupKhatmaJuz>()
        val rawJuzList = data["juzList"] as? List<Map<String, Any>>
        if (rawJuzList != null) {
            for (jMap in rawJuzList) {
                val num = (jMap["num"] as? Long)?.toInt() ?: (jMap["num"] as? Int) ?: 1
                val stStr = (jMap["st"] as? String) ?: GroupKhatmaJuzStatus.UNASSIGNED.name
                val status = try { GroupKhatmaJuzStatus.valueOf(stStr) } catch (e: Exception) { GroupKhatmaJuzStatus.UNASSIGNED }
                val usr = (jMap["usr"] as? String) ?: ""
                val ts = (jMap["ts"] as? Long) ?: 0L
                juzList.add(GroupKhatmaJuz(juzNumber = num, status = status, assignedUserName = usr, completedTimestamp = ts))
            }
        }

        if (juzList.isEmpty()) {
            for (n in 1..30) {
                juzList.add(GroupKhatmaJuz(juzNumber = n))
            }
        }

        return GroupKhatma(
            id = id,
            title = title,
            creatorName = creatorName,
            createdAt = createdAt,
            targetDeadlineTimestamp = targetDeadlineTimestamp,
            juzList = juzList
        )
    }
}
