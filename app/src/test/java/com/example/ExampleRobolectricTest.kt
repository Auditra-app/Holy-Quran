package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import com.example.data.provider.QuranDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Holy Quran", appName)
  }

  @Test
  fun `launch MainActivity scenario`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertNotNull(activity)
      }
    }
  }

  @Test
  fun `verify azkar widget data provider and counting logic`() {
    val morningList = com.example.data.provider.AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId == "morning" }
    assertTrue(morningList.isNotEmpty())
    val firstZikr = morningList.first()
    assertTrue(firstZikr.text.isNotEmpty())
    assertTrue(firstZikr.count >= 1)
  }

  @Test
  fun `verify azkar widget time slot automatic category resolution`() {
    fun createCal(hour: Int, minute: Int): Calendar {
      return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    }

    // Dawn (4:00 AM) to 11:00 AM -> Morning Azkar
    assertEquals("morning", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(4, 0)))
    assertEquals("morning", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(7, 30)))
    assertEquals("morning", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(10, 59)))
    assertEquals("morning", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(11, 0)))

    // 11:01 AM to 15:59 (3:59 PM) -> Duas & Tasbih
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(11, 1)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(11, 5)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(14, 0)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(15, 59)))

    // 16:00 (4:00 PM) to 20:00 (8:00 PM) -> Evening Azkar
    assertEquals("evening", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(16, 0)))
    assertEquals("evening", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(18, 30)))
    assertEquals("evening", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(20, 0)))

    // 20:01 (8:01 PM) till Dawn (< 4:00 AM) -> Duas & Tasbih
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(20, 1)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(23, 30)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(1, 0)))
    assertEquals("daily", com.example.widget.AzkarWidgetProvider.getDefaultCategory(createCal(3, 59)))
  }

  @Test
  fun `verify azkar widget next transition calendar calculation`() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 8)
      set(Calendar.MINUTE, 0)
    }
    val next = com.example.widget.AzkarWidgetProvider.getNextTransitionCalendar(cal)
    assertEquals(11, next.get(Calendar.HOUR_OF_DAY))
    assertEquals(1, next.get(Calendar.MINUTE))
  }
}


