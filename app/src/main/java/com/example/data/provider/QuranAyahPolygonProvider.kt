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

    fun getCachedPagePolygons(pageNumber: Int): PagePolygonData? = pagePolygonCache[pageNumber]

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

                val jsonArray = JSONArray(sb.toString())
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
}
