package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.provider.FirebaseKhatmaRepository
import com.example.data.provider.GroupKhatmaManager
import com.example.domain.model.GroupKhatma
import com.example.domain.model.GroupKhatmaJuz
import com.example.domain.model.GroupKhatmaJuzStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupKhatmaIntegrationTest {

    @Test
    fun testExtractKhatmaCode_allFormats() {
        val testCode = "7M3K9P"

        // 1. Raw code
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("7M3K9P"))
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("  7M3K9P  "))

        // 2. Backtick wrapped
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("`7M3K9P`"))
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("```7M3K9P```"))

        // 3. URLs
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("https://holy-quran-app-live.web.app/?code=7M3K9P"))
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("https://holy-quran-app-live.web.app/?code=7M3K9P&utm_source=wa"))
        assertEquals(testCode, GroupKhatmaManager.extractKhatmaCode("https://holy-quran-app-live.web.app/?data=7M3K9P"))

        // 4. Full WhatsApp share message
        val fullMessage = """
            📖 *ختمة الرحمة والمغفرة* 📖
            دعوة للمشاركة في الختمة القرآنية الجماعية ✨
            • الأجزاء المكتملة: 5 من 30
            • الأجزاء المحجوزة: 3
            • الأجزاء المتبقية: 22

            نسأل الله القبول والشفاعة. لحجز جزء وتسجيل إنجازك، افتح تطبيق القرآن الكريم واستورد كود الختمة التالي:

            7M3K9P

            رابط مباشر للفتح في التطبيق:
            https://holy-quran-app-live.web.app/?code=7M3K9P
        """.trimIndent()

        val extracted = GroupKhatmaManager.extractKhatmaCode(fullMessage)
        assertEquals(testCode, extracted)
    }

    @Test
    fun testKhatmaManager_localSaveAndLoad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialCount = GroupKhatmaManager.getAllKhatmas(context).size

        val khatma = GroupKhatmaManager.createNewKhatma(
            context = context,
            title = "ختمة العائلة الكريمة",
            creatorName = "أبو محمد",
            targetDeadlineTimestamp = 1750000000000L
        )

        assertNotNull(khatma)
        assertEquals("ختمة العائلة الكريمة", khatma.title)
        assertEquals("أبو محمد", khatma.creatorName)
        assertEquals(30, khatma.juzList.size)

        val all = GroupKhatmaManager.getAllKhatmas(context)
        assertEquals(initialCount + 1, all.size)
        assertTrue(all.any { it.id == khatma.id })

        // Clean up
        GroupKhatmaManager.deleteKhatma(context, khatma.id)
    }

    @Test
    fun testCloudFirestore_liveRoundtripAndReservation() = runBlocking {
        val testId = "TEST_" + System.currentTimeMillis().toString().takeLast(6)
        val testTitle = "ختمة الاختبار المباشر"
        val testCreator = "مهندس الاختبار"

        val initialJuzList = (1..30).map {
            GroupKhatmaJuz(
                juzNumber = it,
                status = if (it == 1) GroupKhatmaJuzStatus.BOOKED else GroupKhatmaJuzStatus.UNASSIGNED,
                assignedUserName = if (it == 1) "المستخدم الأول" else ""
            )
        }

        val testKhatma = GroupKhatma(
            id = testId,
            title = testTitle,
            creatorName = testCreator,
            createdAt = System.currentTimeMillis(),
            targetDeadlineTimestamp = System.currentTimeMillis() + 86400000L,
            juzList = initialJuzList
        )

        // 1. Publish to cloud
        val published = FirebaseKhatmaRepository.publishKhatma(testKhatma)
        assertTrue("Cloud publish should succeed", published)

        // 2. Fetch from cloud
        val fetched = FirebaseKhatmaRepository.fetchKhatmaOnce(testId)
        assertNotNull("Fetched khatma must not be null", fetched)
        assertEquals(testId, fetched?.id)
        assertEquals(testTitle, fetched?.title)
        assertEquals(testCreator, fetched?.creatorName)
        assertEquals(GroupKhatmaJuzStatus.BOOKED, fetched?.juzList?.firstOrNull { it.juzNumber == 1 }?.status)
        assertEquals("المستخدم الأول", fetched?.juzList?.firstOrNull { it.juzNumber == 1 }?.assignedUserName)

        // 3. Update reservation (User Two books Juz 2)
        val updated = FirebaseKhatmaRepository.updateJuzStatusCloud(
            khatmaId = testId,
            juzNumber = 2,
            userName = "المستخدم الثاني",
            newStatus = GroupKhatmaJuzStatus.BOOKED
        )
        assertTrue("Juz reservation update should succeed", updated)

        // 4. Verify updated state in cloud
        val fetchedUpdated = FirebaseKhatmaRepository.fetchKhatmaOnce(testId)
        assertNotNull(fetchedUpdated)
        assertEquals(GroupKhatmaJuzStatus.BOOKED, fetchedUpdated?.juzList?.firstOrNull { it.juzNumber == 2 }?.status)
        assertEquals("المستخدم الثاني", fetchedUpdated?.juzList?.firstOrNull { it.juzNumber == 2 }?.assignedUserName)

        // 5. Test importKhatma returns the real khatma and NOT dummy data
        val imported = GroupKhatmaManager.importKhatma(testId)
        assertNotNull("importKhatma must succeed for valid cloud ID", imported)
        assertEquals(testTitle, imported?.title)
        assertEquals(testCreator, imported?.creatorName)

        // 6. Test importKhatma with a nonexistent ID returns null (NOT fake dummy)
        val nonExistent = GroupKhatmaManager.importKhatma("NONEXISTENT_9999")
        assertNull("importKhatma must return null for nonexistent ID", nonExistent)
    }
}
