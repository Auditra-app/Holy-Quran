package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.provider.AzkarDataProvider
import com.example.domain.model.AzkarItem

class AzkarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AzkarRemoteViewsFactory(applicationContext, intent)
    }
}

class AzkarRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var currentCategory: String = "morning"
    private var azkarList: List<AzkarItem> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val prefs = context.getSharedPreferences(AzkarWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val categoryFromUri = intent.data?.lastPathSegment
            val categoryFromExtra = intent.getStringExtra(AzkarWidgetProvider.EXTRA_CATEGORY)
            currentCategory = prefs.getString("widget_category_$appWidgetId", null)
                ?: categoryFromExtra
                ?: categoryFromUri
                ?: prefs.getString("last_selected_category", null)
                ?: AzkarWidgetProvider.getDefaultCategory()

            azkarList = when (currentCategory) {
                "morning" -> AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId == "morning" }
                "evening" -> AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId == "evening" }
                "daily" -> AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId in listOf("daily", "post_prayer", "istikhara", "wake", "sleep") }
                else -> AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId == currentCategory }
            }

            if (azkarList.isEmpty()) {
                azkarList = AzkarDataProvider.AZKAR_COLLECTION.filter { it.categoryId == "morning" }
            }
            Log.d("AzkarWidgetService", "Loaded ${azkarList.size} items for category '$currentCategory' on widget $appWidgetId")
        } catch (e: Exception) {
            Log.e("AzkarWidgetService", "Error loading data for Azkar widget", e)
            azkarList = AzkarDataProvider.AZKAR_COLLECTION.take(10)
        }
    }

    override fun onDestroy() {
        azkarList = emptyList()
    }

    override fun getCount(): Int = azkarList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= azkarList.size) return null

        val item = azkarList[position]
        val views = RemoteViews(context.packageName, R.layout.item_azkar_widget)

        // Read today's count for this zikr
        val prefs = context.getSharedPreferences(AzkarWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val today = AzkarWidgetProvider.getTodayDateString()
        val currentCount = prefs.getInt("count_${today}_${item.id}", 0)
        val isCompleted = currentCount >= item.count

        // Number badge (e.g. 1, 2, 3...)
        views.setTextViewText(R.id.item_zikr_number, "${position + 1}")

        // Title / Heading
        val title = if (item.reference.isNotBlank()) {
            item.reference.split("-").firstOrNull()?.trim() ?: "ذِكر"
        } else {
            "ذِكر"
        }
        views.setTextViewText(R.id.item_zikr_title, title)

        // Repeat / Interactive Counter Text & Styling
        val counterText = when {
            isCompleted -> "✅ تم (${item.count}/${item.count})"
            currentCount > 0 -> "📿 $currentCount / ${item.count}"
            else -> "📿 عدّ ($currentCount/${item.count})"
        }
        views.setTextViewText(R.id.item_zikr_repeat, counterText)

        if (isCompleted) {
            views.setInt(
                R.id.item_zikr_counter_btn,
                "setBackgroundResource",
                R.drawable.bg_widget_counter_btn_completed
            )
            views.setTextColor(R.id.item_zikr_repeat, android.graphics.Color.parseColor("#FFFFFF"))
        } else if (currentCount > 0) {
            views.setInt(
                R.id.item_zikr_counter_btn,
                "setBackgroundResource",
                R.drawable.bg_widget_time_pill_active
            )
            views.setTextColor(R.id.item_zikr_repeat, android.graphics.Color.parseColor("#0B2A22"))
        } else {
            views.setInt(
                R.id.item_zikr_counter_btn,
                "setBackgroundResource",
                R.drawable.bg_widget_counter_btn
            )
            views.setTextColor(R.id.item_zikr_repeat, android.graphics.Color.parseColor("#FFD700"))
        }

        // Full Arabic Text
        views.setTextViewText(R.id.item_zikr_text, item.text)

        // Virtue / Fadl
        if (item.virtue.isNotBlank()) {
            views.setTextViewText(R.id.item_zikr_virtue, "✨ ${item.virtue}")
            views.setViewVisibility(R.id.item_zikr_virtue, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_zikr_virtue, android.view.View.GONE)
        }

        // Reference / Source
        if (item.reference.isNotBlank()) {
            views.setTextViewText(R.id.item_zikr_reference, "📖 ${item.reference}")
            views.setViewVisibility(R.id.item_zikr_reference, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_zikr_reference, android.view.View.GONE)
        }

        // Fill-in Intent for counting when user clicks the counter button or card
        val fillInIntent = Intent().apply {
            action = AzkarWidgetProvider.ACTION_COUNT_AZKAR_WIDGET
            putExtra(AzkarWidgetProvider.EXTRA_ZIKR_ID, item.id)
            putExtra(AzkarWidgetProvider.EXTRA_ZIKR_TARGET_COUNT, item.count)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setOnClickFillInIntent(R.id.item_zikr_counter_btn, fillInIntent)
        views.setOnClickFillInIntent(R.id.item_azkar_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in azkarList.indices) azkarList[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
