package com.example.data.provider

import android.util.Log
import com.example.domain.model.GroupKhatma
import com.example.domain.model.GroupKhatmaJuz
import com.example.domain.model.GroupKhatmaJuzStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseKhatmaRepository {

    private const val TAG = "FirebaseKhatmaRepo"
    private const val COLLECTION_KHATMAS = "khatmas"

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
     */
    fun observeKhatma(khatmaId: String): Flow<GroupKhatma?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = db.collection(COLLECTION_KHATMAS).document(khatmaId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error listening to Khatma $khatmaId snapshot: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                if (data != null) {
                    val khatma = parseFirestoreData(khatmaId, data)
                    trySend(khatma)
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Fetches a GroupKhatma document once from Cloud Firestore without starting a persistent listener.
     */
    suspend fun fetchKhatmaOnce(khatmaId: String): GroupKhatma? {
        val db = firestore ?: return null
        return try {
            val docRef = db.collection(COLLECTION_KHATMAS).document(khatmaId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val data = snapshot.data
                if (data != null) {
                    parseFirestoreData(khatmaId, data)
                } else null
            } else null
        } catch (e: Throwable) {
            Log.w(TAG, "Error fetching Khatma $khatmaId once from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Publishes or updates an entire GroupKhatma in Cloud Firestore.
     */
    suspend fun publishKhatma(khatma: GroupKhatma): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection(COLLECTION_KHATMAS).document(khatma.id)
            val mapData = khatmaToFirestoreMap(khatma)
            docRef.set(mapData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully published Khatma ${khatma.id} to Cloud Firestore")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish Khatma ${khatma.id} to Cloud Firestore", e)
            false
        }
    }

    /**
     * Real-time update of a single Juz status in Cloud Firestore.
     */
    suspend fun updateJuzStatusCloud(
        khatmaId: String,
        juzNumber: Int,
        userName: String,
        newStatus: GroupKhatmaJuzStatus
    ): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection(COLLECTION_KHATMAS).document(khatmaId)
            val snapshot = docRef.get().await()
            val currentData = if (snapshot.exists()) snapshot.data else null
            val khatma = if (currentData != null) {
                parseFirestoreData(khatmaId, currentData)
            } else {
                val defaultJuzList = (1..30).map { GroupKhatmaJuz(juzNumber = it) }
                GroupKhatma(id = khatmaId, title = "ختمة قرآنية جماعية", creatorName = userName.ifBlank { "مشارك" }, juzList = defaultJuzList)
            }

            val updatedJuzList = khatma.juzList.map { juz ->
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

            val updatedKhatma = khatma.copy(juzList = updatedJuzList)
            val mapData = khatmaToFirestoreMap(updatedKhatma)
            docRef.set(mapData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully updated Juz $juzNumber for Khatma $khatmaId in Firestore")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update Juz $juzNumber for Khatma $khatmaId in Firestore", e)
            false
        }
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
