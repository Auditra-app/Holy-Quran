package com.example.data.provider

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class AyahPolygon(
    val surahNumber: Int,
    val ayahNumber: Int,
    val path: Path,
    val composePath: androidx.compose.ui.graphics.Path,
    val bounds: RectF,
    val region: Region
)

data class PagePolygonData(
    val pageNumber: Int,
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float,
    val ayahs: List<AyahPolygon>
)

object QuranAyahPolygonProvider {
    private val pagePolygonCache = ConcurrentHashMap<Int, PagePolygonData>()

    init {
        try {
            parseAndCacheJson(1, PAGE_1_JSON)
            parseAndCacheJson(2, PAGE_2_JSON)
        } catch (_: Throwable) {}
    }

    fun getCachedPagePolygons(pageNumber: Int): PagePolygonData? = pagePolygonCache[pageNumber]

    fun parseAndCacheJson(pageNumber: Int, jsonStr: String): PagePolygonData? {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val ayahs = mutableListOf<AyahPolygon>()

            // Pages 1 and 2 viewBox is "-53.3109 -198.4777 345 550"
            // Pages 3..604 viewBox is "0 0 345 550"
            val minX = if (pageNumber <= 2) -53.3109f else 0.0f
            val minY = if (pageNumber <= 2) -198.4777f else 0.0f
            val vbWidth = 345.0f
            val vbHeight = 550.0f

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val surah = obj.optInt("surahNumber", obj.optInt("surah", 0))
                val ayah = obj.optInt("ayahNumber", obj.optInt("ayah", 0))
                val polygonStr = obj.optString("polygon", "")
                if (polygonStr.isBlank() || surah == 0 || ayah == 0) continue

                val path = parsePolygonToPath(polygonStr) ?: continue
                val bounds = RectF()
                path.computeBounds(bounds, true)

                val region = Region()
                val clip = Region(
                    (bounds.left - 1).toInt(),
                    (bounds.top - 1).toInt(),
                    (bounds.right + 2).toInt(),
                    (bounds.bottom + 2).toInt()
                )
                region.setPath(path, clip)

                ayahs.add(
                    AyahPolygon(
                        surahNumber = surah,
                        ayahNumber = ayah,
                        path = path,
                        composePath = path.asComposePath(),
                        bounds = bounds,
                        region = region
                    )
                )
            }

            val pageData = PagePolygonData(
                pageNumber = pageNumber,
                minX = minX,
                minY = minY,
                width = vbWidth,
                height = vbHeight,
                ayahs = ayahs
            )
            pagePolygonCache[pageNumber] = pageData
            pageData
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun getPagePolygons(pageNumber: Int): PagePolygonData? = withContext(Dispatchers.IO) {
        pagePolygonCache[pageNumber]?.let { return@withContext it }

        try {
            val pageStr = pageNumber.toString().padStart(3, '0')
            val url = URL("https://raw.githubusercontent.com/quran-ws/quran-svg/main/mushafs/hafs/kfqc/json/$pageStr.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000
            conn.readTimeout = 7000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                parseAndCacheJson(pageNumber, sb.toString())
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parsePolygonToPath(polygon: String): Path? {
        val trimmed = polygon.trim()
        if (trimmed.isEmpty()) return null

        return try {
            if (trimmed.startsWith("M", ignoreCase = true)) {
                PathParser.createPathFromPathData(trimmed)
            } else {
                // Point-list format e.g. "181.08,18.31 57.54,18.31 57.54,48.94 181.08,48.94"
                val regex = Regex("-?\\d+(?:\\.\\d+)?")
                val numbers = regex.findAll(trimmed).map { it.value.toFloat() }.toList()
                if (numbers.size < 4) return null

                val path = Path()
                path.moveTo(numbers[0], numbers[1])
                for (i in 2 until numbers.size step 2) {
                    path.lineTo(numbers[i], numbers[i + 1])
                }
                path.close()
                path
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Hit-test a touch point (tapX, tapY) in Compose coordinates against the page's ayah polygons.
     * Returns matching AyahPolygon if touched, or null if outside any verse.
     */
    fun hitTest(
        pageData: PagePolygonData,
        tapX: Float,
        tapY: Float,
        viewWidth: Float,
        viewHeight: Float,
        isImmersive: Boolean
    ): AyahPolygon? {
        if (viewWidth <= 0f || viewHeight <= 0f || pageData.ayahs.isEmpty()) return null

        val svgX: Float
        val svgY: Float

        if (isImmersive) {
            // ContentScale.FillBounds
            svgX = pageData.minX + (tapX / viewWidth) * pageData.width
            svgY = pageData.minY + (tapY / viewHeight) * pageData.height
        } else {
            // ContentScale.Fit
            val scale = minOf(viewWidth / pageData.width, viewHeight / pageData.height)
            if (scale <= 0f) return null
            val offsetX = (viewWidth - pageData.width * scale) / 2f
            val offsetY = (viewHeight - pageData.height * scale) / 2f

            svgX = pageData.minX + (tapX - offsetX) / scale
            svgY = pageData.minY + (tapY - offsetY) / scale
        }

        val testX = svgX.toInt()
        val testY = svgY.toInt()

        // 1. Strict region contains test
        for (ayah in pageData.ayahs) {
            if (ayah.region.contains(testX, testY)) {
                return ayah
            }
        }

        // 2. Tolerance test (+/- 2 units in SVG coordinates for small touch inaccuracies)
        val tolerance = 2
        for (ayah in pageData.ayahs) {
            if (ayah.bounds.contains(svgX, svgY)) {
                for (dx in -tolerance..tolerance step 2) {
                    for (dy in -tolerance..tolerance step 2) {
                        if (ayah.region.contains(testX + dx, testY + dy)) {
                            return ayah
                        }
                    }
                }
            }
        }

        return null
    }

    private const val PAGE_1_JSON = """[
        {"ayahNumber":1,"surahNumber":1,"polygon":"181.08,18.31 57.54,18.31 57.54,48.94 181.08,48.94"},
        {"ayahNumber":2,"surahNumber":1,"polygon":"203.87,49.07 36.74,49.07 36.74,76.22 203.87,76.22"},
        {"ayahNumber":3,"surahNumber":1,"polygon":"229.77,76.22 128.52,76.22 128.52,103.24 229.77,103.24"},
        {"ayahNumber":4,"surahNumber":1,"polygon":"128.52,76.22 12.95,76.22 12.59,103.24 128.52,103.24"},
        {"ayahNumber":5,"surahNumber":1,"polygon":"229.77,103.24 56.04,103.24 56.04,130.26 229.52,131.26"},
        {"ayahNumber":6,"surahNumber":1,"polygon":"56.04,103.24 8.09,103.24 8.09,129.77 229.52,131.26 229.52,160.65 114.20,160.65 114.20,130.39 55.79,130.02"},
        {"ayahNumber":7,"surahNumber":1,"polygon":"113.70,130.39 13.82,130.39 13.82,162.77 37.11,162.77 37.11,189.05 73.48,189.05 73.48,214.95 171.86,214.95 171.86,191.54 204.99,191.54 204.99,160.65 113.70,160.28 113.70,130.39"}
    ]"""

    private const val PAGE_2_JSON = """[
        {"ayahNumber":1,"surahNumber":2,"polygon":"206.48,46.08 172.36,46.08 172.36,72.23 206.48,72.23"},
        {"ayahNumber":2,"surahNumber":2,"polygon":"172.36,46.08 33.75,46.08 33.75,72.23 225.78,72.36 225.78,102.49 172.36,102.49 172.36,71.98 172.36,72.23"},
        {"ayahNumber":3,"surahNumber":2,"polygon":"172.36,72.23 12.08,72.23 12.08,101.66 113.95,101.66 113.95,133.88 228.16,133.88 228.16,103.66 172.36,103.66 172.36,72.36"},
        {"ayahNumber":4,"surahNumber":2,"polygon":"113.95,101.66 11.46,101.66 11.46,160.15 228.52,160.15 228.52,133.63 113.95,133.63 113.95,101.66"},
        {"ayahNumber":5,"surahNumber":2,"polygon":"200.01,158.53 37.73,158.53 37.73,187.05 69.37,187.05 69.37,216.20 168.87,216.20 168.87,188.17 200.01,188.17 200.01,158.53"}
    ]"""
}

