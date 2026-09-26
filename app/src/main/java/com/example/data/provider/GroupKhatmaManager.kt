package com.example.data.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.domain.model.GroupKhatma
import com.example.domain.model.GroupKhatmaJuz
import com.example.domain.model.GroupKhatmaJuzStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object GroupKhatmaManager {
    private const val PREFS_NAME = "group_khatma_prefs"
    private const val KEY_KHATMAS = "khatmas_list_json"
    private const val KEY_USER_NAME = "saved_user_name"
    private const val KEY_LAST_ACTIVE_KHATMA_ID = "last_active_khatma_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastActiveKhatmaId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_ACTIVE_KHATMA_ID, null)
    }

    fun saveLastActiveKhatmaId(context: Context, khatmaId: String) {
        if (khatmaId.isNotBlank()) {
            getPrefs(context).edit().putString(KEY_LAST_ACTIVE_KHATMA_ID, khatmaId).apply()
        }
    }

    fun getSavedUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun getAllKhatmas(context: Context): List<GroupKhatma> {
        val jsonStr = getPrefs(context).getString(KEY_KHATMAS, null) ?: return emptyList()
        return parseKhatmasListJson(jsonStr)
    }

    fun generateShortKhatmaId(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun saveKhatmaLocalOnly(context: Context, khatma: GroupKhatma) {
        val currentList = getAllKhatmas(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == khatma.id }
        if (index >= 0) {
            currentList[index] = khatma
        } else {
            currentList.add(0, khatma)
        }
        val jsonStr = khatmasListToJson(currentList)
        getPrefs(context).edit().putString(KEY_KHATMAS, jsonStr).apply()
    }

    fun saveKhatma(context: Context, khatma: GroupKhatma) {
        saveKhatmaLocalOnly(context, khatma)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseKhatmaRepository.publishKhatma(khatma)
            } catch (e: Exception) {
                android.util.Log.w("GroupKhatmaManager", "Background cloud publish error on saveKhatma", e)
            }
        }
    }

    fun deleteKhatma(context: Context, khatmaId: String) {
        val currentList = getAllKhatmas(context).filter { it.id != khatmaId }
        val jsonStr = khatmasListToJson(currentList)
        getPrefs(context).edit().putString(KEY_KHATMAS, jsonStr).apply()
    }

    fun createNewKhatma(
        context: Context,
        title: String,
        creatorName: String,
        targetDeadlineTimestamp: Long = 0L
    ): GroupKhatma {
        val newKhatma = GroupKhatma(
            id = generateShortKhatmaId(),
            title = title.ifBlank { "ختمة قرآنية جماعية" },
            creatorName = creatorName.ifBlank { "مشارك" },
            createdAt = System.currentTimeMillis(),
            targetDeadlineTimestamp = targetDeadlineTimestamp
        )
        saveKhatma(context, newKhatma)
        if (creatorName.isNotBlank()) {
            saveUserName(context, creatorName)
        }
        return newKhatma
    }

    fun updateJuzReservation(
        context: Context,
        khatmaId: String,
        juzNumber: Int,
        userName: String,
        newStatus: GroupKhatmaJuzStatus
    ): GroupKhatma? {
        val all = getAllKhatmas(context)
        val target = all.firstOrNull { it.id == khatmaId } ?: return null

        val updatedJuzList = target.juzList.map { juz ->
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

        val updatedKhatma = target.copy(juzList = updatedJuzList)
        saveKhatma(context, updatedKhatma)
        if (userName.isNotBlank()) {
            saveUserName(context, userName)
        }
        return updatedKhatma
    }

    fun encodeKhatmaToCode(khatma: GroupKhatma): String {
        val json = khatmaToJsonObject(khatma).toString()
        val base64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        return "KHATMA#$base64"
    }

    fun extractKhatmaCode(input: String): String {
        var raw = input.trim()
        if (raw.isBlank()) return ""

        // 1. If it contains a URL with code= or data=
        if (raw.contains("code=")) {
            val extracted = raw.substringAfter("code=").substringBefore("&").substringBefore(" ").substringBefore("\n").substringBefore("#Intent").trim()
            val cleaned = extracted.removeSurrounding("`").removeSurrounding("\"").removeSurrounding("'").trim()
            if (cleaned.isNotBlank()) return cleaned
        }
        if (raw.contains("data=")) {
            val extracted = raw.substringAfter("data=").substringBefore("&").substringBefore(" ").substringBefore("\n").substringBefore("#Intent").trim()
            val cleaned = extracted.removeSurrounding("`").removeSurrounding("\"").removeSurrounding("'").trim()
            if (cleaned.isNotBlank()) return cleaned
        }

        // 2. If it contains a full KHATMA# base64 string
        if (raw.contains("KHATMA#")) {
            val part = raw.substringAfter("KHATMA#").substringBefore(" ").substringBefore("\n").trim()
            return "KHATMA#$part"
        }

        // 3. Search for 6-character alphanumeric code pattern (e.g. 6ZZBSL, NJ3GB9, RL56PV)
        val shortCodeMatch = Regex("\\b([2-9A-HJ-NP-Z]{6})\\b").find(raw)
        if (shortCodeMatch != null) {
            return shortCodeMatch.groupValues[1]
        }

        // 4. Backtick match `XYZ123`
        val backtickMatch = Regex("`([A-Za-z0-9_-]{4,20})`").find(raw)
        if (backtickMatch != null) {
            return backtickMatch.groupValues[1]
        }

        // 5. Line scan: find first line that looks like a clean code (no spaces, 4-16 chars)
        val stripped = raw.removeSurrounding("`").removeSurrounding("\"").removeSurrounding("'").trim()
        for (line in stripped.lines()) {
            val trimmedLine = line.trim().removeSurrounding("`").removeSurrounding("\"").removeSurrounding("'").trim()
            if (trimmedLine.length in 4..16 && !trimmedLine.contains(" ") && !trimmedLine.contains(":") && !trimmedLine.contains("•") && !trimmedLine.contains("http")) {
                return trimmedLine
            }
        }

        return stripped.lines().firstOrNull { it.isNotBlank() }?.trim()?.removeSurrounding("`")?.trim() ?: ""
    }

    suspend fun importKhatma(codeOrLink: String): GroupKhatma? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val cleanCode = extractKhatmaCode(codeOrLink)
        if (cleanCode.isBlank()) return@withContext null

        // Check if it's base64 encoded KHATMA#...
        val base64Part = if (cleanCode.startsWith("KHATMA#")) cleanCode.removePrefix("KHATMA#") else cleanCode
        try {
            val jsonBytes = Base64.decode(base64Part, Base64.NO_WRAP or Base64.URL_SAFE)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                val jsonObj = JSONObject(jsonStr)
                val parsed = parseKhatmaJsonObject(jsonObj)
                FirebaseKhatmaRepository.publishKhatma(parsed)
                return@withContext parsed
            }
        } catch (e: Throwable) {
            // Not a base64 JSON payload, proceed to cloud lookup
        }

        // Lookup in Cloud Firestore (SDK + REST)
        val candidateId = cleanCode.removePrefix("KHATMA#").trim()
        if (candidateId.isNotBlank()) {
            try {
                val cloudKhatma = FirebaseKhatmaRepository.fetchKhatmaOnce(candidateId)
                if (cloudKhatma != null) {
                    return@withContext cloudKhatma
                }
            } catch (e: Throwable) {
                android.util.Log.w("GroupKhatmaManager", "Error fetching Khatma $candidateId from cloud: ${e.message}")
            }
        }

        // Return null if not found in Cloud - never create a fake empty Khatma!
        return@withContext null
    }

    fun decodeKhatmaFromCode(codeOrLink: String): GroupKhatma? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            importKhatma(codeOrLink)
        }
    }

    fun generateShareableMessage(khatma: GroupKhatma, isArabic: Boolean, context: Context? = null): String {
        val cleanId = if (khatma.id.isNotBlank() && !khatma.id.startsWith("KHATMA#") && khatma.id.length <= 16) {
            khatma.id
        } else {
            generateShortKhatmaId()
        }

        if (khatma.id != cleanId) {
            val updated = khatma.copy(id = cleanId)
            val ctx = context ?: com.example.HolyQuranApplication.instance
            if (ctx != null) {
                saveKhatma(ctx, updated)
            }
        }

        // Ensure cloud is immediately synced with the latest data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseKhatmaRepository.publishKhatma(khatma)
            } catch (e: Exception) {
                android.util.Log.w("GroupKhatmaManager", "Background cloud publish error on share", e)
            }
        }

        val completedCount = khatma.juzList.count { it.status == GroupKhatmaJuzStatus.COMPLETED }
        val bookedCount = khatma.juzList.count { it.status == GroupKhatmaJuzStatus.BOOKED }
        val remainingCount = 30 - completedCount - bookedCount

        val deadlineStr = if (khatma.targetDeadlineTimestamp > 0L) {
            val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale("ar")).format(java.util.Date(khatma.targetDeadlineTimestamp))
            if (isArabic) "⏰ موعد إغلاق الختمة المستهدف: $dateStr\n" else "⏰ Target Closing Time: $dateStr\n"
        } else ""

        return if (isArabic) {
            "📖 *${khatma.title}* 📖\n" +
            "دعوة للمشاركة في الختمة القرآنية الجماعية ✨\n" +
            deadlineStr +
            "• الأجزاء المكتملة: $completedCount من 30\n" +
            "• الأجزاء المحجوزة: $bookedCount\n" +
            "• الأجزاء المتبقية: $remainingCount\n\n" +
            "نسأل الله القبول والشفاعة. لحجز جزء وتسجيل إنجازك، افتح تطبيق القرآن الكريم واستورد كود الختمة التالي:\n\n" +
            "$cleanId\n\n" +
            "رابط مباشر للفتح في التطبيق:\n" +
            "https://holy-quran-app-live.web.app/?code=$cleanId"
        } else {
            "📖 *${khatma.title}* 📖\n" +
            "Join our Group Quran Khatma! ✨\n" +
            deadlineStr +
            "• Completed: $completedCount / 30\n" +
            "• Reserved: $bookedCount\n" +
            "• Remaining: $remainingCount\n\n" +
            "Open Holy Quran App and import this Khatma code to reserve your Juz:\n\n" +
            "$cleanId\n\n" +
            "Direct link:\n" +
            "https://holy-quran-app-live.web.app/?code=$cleanId"
        }
    }

    private fun khatmaToJsonObject(khatma: GroupKhatma): JSONObject {
        val obj = JSONObject()
        obj.put("id", khatma.id)
        obj.put("title", khatma.title)
        obj.put("creator", khatma.creatorName)
        obj.put("createdAt", khatma.createdAt)
        obj.put("deadline", khatma.targetDeadlineTimestamp)

        val juzArr = JSONArray()
        khatma.juzList.forEach { j ->
            val jObj = JSONObject()
            jObj.put("num", j.juzNumber)
            jObj.put("st", j.status.name)
            jObj.put("usr", j.assignedUserName)
            jObj.put("ts", j.completedTimestamp)
            juzArr.put(jObj)
        }
        obj.put("juz", juzArr)
        return obj
    }

    private fun parseKhatmaJsonObject(obj: JSONObject): GroupKhatma {
        val id = obj.optString("id", java.util.UUID.randomUUID().toString().take(8))
        val title = obj.optString("title", "ختمة قرآنية جماعية")
        val creator = obj.optString("creator", "")
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        val deadline = obj.optLong("deadline", 0L)

        val juzList = mutableListOf<GroupKhatmaJuz>()
        val juzArr = obj.optJSONArray("juz")
        if (juzArr != null) {
            for (i in 0 until juzArr.length()) {
                val jObj = juzArr.getJSONObject(i)
                val num = jObj.optInt("num", i + 1)
                val stStr = jObj.optString("st", GroupKhatmaJuzStatus.UNASSIGNED.name)
                val status = try { GroupKhatmaJuzStatus.valueOf(stStr) } catch (e: Exception) { GroupKhatmaJuzStatus.UNASSIGNED }
                val usr = jObj.optString("usr", "")
                val ts = jObj.optLong("ts", 0L)
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
            creatorName = creator,
            createdAt = createdAt,
            targetDeadlineTimestamp = deadline,
            juzList = juzList
        )
    }

    private fun khatmasListToJson(list: List<GroupKhatma>): String {
        val arr = JSONArray()
        list.forEach { arr.put(khatmaToJsonObject(it)) }
        return arr.toString()
    }

    private fun parseKhatmasListJson(jsonStr: String): List<GroupKhatma> {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<GroupKhatma>()
            for (i in 0 until arr.length()) {
                list.add(parseKhatmaJsonObject(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
