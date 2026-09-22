package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.QuranDataProvider
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.LocationHelper
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CityLocation(
    val nameAr: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double
)

val CITIES = listOf(
    CityLocation("القاهرة (مصر)", "Cairo (Egypt)", 30.0444, 31.2357),
    CityLocation("مكة المكرمة", "Makkah Al-Mukarramah", 21.4225, 39.8262),
    CityLocation("الرياض (السعودية)", "Riyadh (KSA)", 24.7136, 46.6753),
    CityLocation("دبي (الإمارات)", "Dubai (UAE)", 25.2048, 55.2708),
    CityLocation("إسطنبول (تركيا)", "Istanbul (Turkey)", 41.0082, 28.9784),
    CityLocation("لندن (المملكة المتحدة)", "London (UK)", 51.5074, -0.1278),
    CityLocation("نيويورك (أمريكا)", "New York (USA)", 40.7128, -74.0060),
    CityLocation("جاكرتا (إندونيسيا)", "Jakarta (Indonesia)", -6.2088, 106.8456),
    CityLocation("كوالالمبور (ماليزيا)", "Kuala Lumpur (Malaysia)", 3.1390, 101.6869),
    CityLocation("باريس (فرنسا)", "Paris (France)", 48.8566, 2.3522)
)

/**
 * Shortest path angle unwrapping to prevent 360-degree spin glitches when passing North.
 */
private fun unwrapAngle(currentUnwrapped: Float, targetDegrees: Float): Float {
    var delta = (targetDegrees - currentUnwrapped) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return currentUnwrapped + delta
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var selectedCity by remember { mutableStateOf(CITIES[0]) } // Default Cairo
    var showCityDialog by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    val userLocation by viewModel.userLocation.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                      (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            isDetectingLocation = true
            LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                isDetectingLocation = false
                viewModel.setUserGPSLocation(lat, lng, cityName)
                selectedCity = CityLocation("موقعي الحالي ($cityName)", "My GPS Location ($cityName)", lat, lng)
                Toast.makeText(context, if (isArabic) "تم تحديد موقعك: $cityName" else "Qibla location set: $cityName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            val (lat, lng, cityName) = userLocation!!
            selectedCity = CityLocation("موقعي الحالي ($cityName)", "My GPS Location ($cityName)", lat, lng)
        } else if (LocationHelper.hasLocationPermission(context)) {
            isDetectingLocation = true
            LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                isDetectingLocation = false
                viewModel.setUserGPSLocation(lat, lng, cityName)
                selectedCity = CityLocation("موقعي الحالي ($cityName)", "My GPS Location ($cityName)", lat, lng)
            }
        }
    }

    // Unwrapped continuous orientation angle (Degrees from North)
    var unwrappedAzimuth by remember { mutableFloatStateOf(0f) }
    var hasHardwareSensors by remember { mutableStateOf(true) }
    var manualMode by remember { mutableStateOf(false) }
    var manualHeadingDegrees by remember { mutableFloatStateOf(0f) }

    // Calculate exact Qibla bearing & distance for selected location
    val (qiblaBearing, distanceKm) = remember(selectedCity) {
        QuranDataProvider.calculateQibla(selectedCity.lat, selectedCity.lng)
    }

    // Sensor listener setup
    DisposableEffect(manualMode) {
        if (manualMode) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            hasHardwareSensors = false
            return@DisposableEffect onDispose {}
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor == null && (accelSensor == null || magnetSensor == null)) {
            hasHardwareSensors = false
            return@DisposableEffect onDispose {}
        }

        hasHardwareSensors = true

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        val gravityValues = FloatArray(3)
        val geomagneticValues = FloatArray(3)

        var hasGravity = false
        var hasGeomagnetic = false

        val listener = object : SensorEventListener {
            private var smoothedAzimuth = 0f
            private var isInitialized = false

            override fun onSensorChanged(event: SensorEvent?) {
                try {
                    if (event == null || event.values == null) return

                    var rawAzimuthDeg = Float.NaN

                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        if (event.values.size >= 3) {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientationValues)
                            rawAzimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        }
                    } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravityValues[0] = gravityValues[0] * 0.8f + event.values[0] * 0.2f
                        gravityValues[1] = gravityValues[1] * 0.8f + event.values[1] * 0.2f
                        gravityValues[2] = gravityValues[2] * 0.8f + event.values[2] * 0.2f
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagneticValues[0] = geomagneticValues[0] * 0.8f + event.values[0] * 0.2f
                        geomagneticValues[1] = geomagneticValues[1] * 0.8f + event.values[1] * 0.2f
                        geomagneticValues[2] = geomagneticValues[2] * 0.8f + event.values[2] * 0.2f
                        hasGeomagnetic = true
                    }

                    if (rawAzimuthDeg.isNaN() && hasGravity && hasGeomagnetic) {
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                            SensorManager.getOrientation(rotationMatrix, orientationValues)
                            rawAzimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        }
                    }

                    if (rawAzimuthDeg.isNaN()) return

                    if (rawAzimuthDeg < 0) rawAzimuthDeg += 360f

                    // Continuous angle unwrapping and low-pass filtering
                    if (!isInitialized) {
                        smoothedAzimuth = rawAzimuthDeg
                        isInitialized = true
                    } else {
                        val nextUnwrapped = unwrapAngle(smoothedAzimuth, rawAzimuthDeg)
                        smoothedAzimuth += (nextUnwrapped - smoothedAzimuth) * 0.20f
                    }

                    unwrappedAzimuth = smoothedAzimuth
                } catch (e: Throwable) {
                    android.util.Log.e("QiblaScreen", "Error in onSensorChanged", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        try {
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (accelSensor != null && magnetSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Throwable) {
            android.util.Log.e("QiblaScreen", "Error registering sensor listener", e)
        }

        onDispose {
            try {
                sensorManager.unregisterListener(listener)
            } catch (e: Throwable) {
                android.util.Log.e("QiblaScreen", "Error unregistering sensor listener", e)
            }
        }
    }

    val effectiveAzimuth = if (manualMode) manualHeadingDegrees else unwrappedAzimuth

    // Qibla direction relative to top of device = (qiblaBearing - effectiveAzimuth)
    val relativeQiblaAngle = unwrapAngle(0f, (qiblaBearing.toFloat() - effectiveAzimuth))
    val currentDeviation = abs((relativeQiblaAngle % 360f + 360f) % 360f.let { if (it > 180f) 360f - it else it })
    val isAligned = currentDeviation < 4.5f

    // Haptic feedback trigger on alignment
    var lastVibratedState by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !lastVibratedState) {
            lastVibratedState = true
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(120)
                    }
                }
            } catch (_: Exception) {}
        } else if (!isAligned) {
            lastVibratedState = false
        }
    }

    // Smooth animations for compass rotation
    val animatedDialRotation by animateFloatAsState(
        targetValue = -effectiveAzimuth,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val animatedNeedleRotation by animateFloatAsState(
        targetValue = relativeQiblaAngle,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isAligned) 1f else 0.2f,
        animationSpec = tween(500)
    )

    val ringBorderColor by animateColorAsState(
        targetValue = if (isAligned) EmeraldPrimary else IslamicGold.copy(alpha = 0.6f),
        animationSpec = tween(300)
    )

    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "بوصلة اتجاه القبلة" else "Qibla Compass",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qibla_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCityDialog = true }) {
                        Icon(imageVector = Icons.Default.LocationCity, contentDescription = "Change City", tint = EmeraldPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isArabic) "الموقع الحالي المعتمد" else "Current Location",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) selectedCity.nameAr else selectedCity.nameEn,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic)
                                "درجة القبلة: ${qiblaBearing.toInt()}° | المسافة: ${distanceKm.toInt()} كم"
                            else
                                "Qibla Angle: ${qiblaBearing.toInt()}° | Distance: ${distanceKm.toInt()} km",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                            color = IslamicGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    isDetectingLocation = true
                                    LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                                        isDetectingLocation = false
                                        viewModel.setUserGPSLocation(lat, lng, cityName)
                                        selectedCity = CityLocation("موقعي الحالي ($cityName)", "My GPS Location ($cityName)", lat, lng)
                                        Toast.makeText(context, if (isArabic) "تم تحديد موقعك: $cityName" else "Location updated: $cityName", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = EmeraldPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS", tint = EmeraldPrimary)
                            }
                        }

                        IconButton(
                            onClick = { showCityDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(IslamicGold.copy(alpha = 0.15f))
                        ) {
                            Icon(imageVector = Icons.Default.EditLocation, contentDescription = "Select City", tint = IslamicGold)
                        }
                    }
                }
            }

            // Alignment Guidance Status Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAligned) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, if (isAligned) EmeraldPrimary else GoldAccent.copy(alpha = 0.4f)),
                shadowElevation = if (isAligned) 6.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isAligned) EmeraldPrimary else IslamicGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (isAligned) Color.White else IslamicGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAligned)
                                (if (isArabic) "🎯 تم تحديد اتجاه الكعبة المشرفة بنجاح!" else "🎯 Perfectly Aligned with the Holy Kaaba!")
                            else {
                                val directionHint = if (relativeQiblaAngle > 0) {
                                    if (isArabic) "أدر الهاتف لليمين" else "Turn right"
                                } else {
                                    if (isArabic) "أدر الهاتف لليار" else "Turn left"
                                }
                                if (isArabic) "وجه أعلى الهاتف نحو السهم الأخضر ($directionHint)" else "Point phone top to green arrow ($directionHint)"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (isAligned) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic)
                                "الانحراف الحالي: ${currentDeviation.toInt()}° | أفق الهاتف: مستوٍ"
                            else
                                "Current Deviation: ${currentDeviation.toInt()}° | Position: Flat",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // MAIN INTERACTIVE QIBLA COMPASS DIAL
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .pointerInput(manualMode) {
                        if (manualMode) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val currentPos = change.position
                                val prevPos = currentPos - dragAmount
                                val angleCurrent = Math.toDegrees(atan2((currentPos.y - center.y).toDouble(), (currentPos.x - center.x).toDouble())).toFloat()
                                val anglePrev = Math.toDegrees(atan2((prevPos.y - center.y).toDouble(), (prevPos.x - center.x).toDouble())).toFloat()
                                val delta = angleCurrent - anglePrev
                                manualHeadingDegrees += delta
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Glowing Aura when aligned
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EmeraldPrimary.copy(alpha = 0.35f * glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Phone Top Reference Pointer (Fixed 12 o'clock marker)
                Canvas(
                    modifier = Modifier
                        .size(310.dp)
                        .align(Alignment.Center)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val outerRadius = size.width / 2

                    // Top phone direction marker triangle
                    val pointerPath = Path().apply {
                        moveTo(center.x, center.y - outerRadius + 2.dp.toPx())
                        lineTo(center.x - 10.dp.toPx(), center.y - outerRadius + 18.dp.toPx())
                        lineTo(center.x + 10.dp.toPx(), center.y - outerRadius + 18.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = if (isAligned) EmeraldPrimary else Color.Red
                    )
                }

                // Main Compass Circular Body
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(4.dp, ringBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Canvas Dial (Rotates with -Azimuth)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 14.dp.toPx()

                        withTransform({ rotate(animatedDialRotation, center) }) {
                            // 1. Draw Rub el Hizb (8-Pointed Islamic Star) subtle background
                            val starPath = Path()
                            val starRadius = radius * 0.42f
                            for (i in 0 until 8) {
                                val angle = Math.toRadians((i * 45).toDouble())
                                val x = center.x + starRadius * cos(angle).toFloat()
                                val y = center.y + starRadius * sin(angle).toFloat()
                                if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                            }
                            starPath.close()
                            drawPath(starPath, color = IslamicGold.copy(alpha = 0.12f), style = Stroke(width = 2.dp.toPx()))

                            val starPath2 = Path()
                            for (i in 0 until 8) {
                                val angle = Math.toRadians((i * 45 + 22.5).toDouble())
                                val x = center.x + starRadius * cos(angle).toFloat()
                                val y = center.y + starRadius * sin(angle).toFloat()
                                if (i == 0) starPath2.moveTo(x, y) else starPath2.lineTo(x, y)
                            }
                            starPath2.close()
                            drawPath(starPath2, color = IslamicGold.copy(alpha = 0.12f), style = Stroke(width = 2.dp.toPx()))

                            // 2. Degree Tick Marks (360 degrees)
                            for (deg in 0 until 360 step 5) {
                                val angleRad = Math.toRadians(deg.toDouble() - 90)
                                val isMajor = deg % 30 == 0
                                val isCardinal = deg % 90 == 0

                                val tickLength = if (isCardinal) 16.dp.toPx() else if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                                val strokeW = if (isCardinal) 3.5.dp.toPx() else if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                                val startX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
                                val startY = center.y + (radius - tickLength) * sin(angleRad).toFloat()
                                val endX = center.x + radius * cos(angleRad).toFloat()
                                val endY = center.y + radius * sin(angleRad).toFloat()

                                val tickColor = when {
                                    deg == 0 -> Color.Red
                                    isCardinal -> IslamicGold
                                    isMajor -> Color.Gray.copy(alpha = 0.7f)
                                    else -> Color.Gray.copy(alpha = 0.35f)
                                }

                                drawLine(
                                    color = tickColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = strokeW
                                )
                            }
                        }
                    }

                    // Rotating Cardinal Letters Overlay (North N / ش, East E / ق, South S / ج, West W / غ)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                            .rotate(animatedDialRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        // North
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Text("N", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                            Text(if (isArabic) "شمال" else "North", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Red, fontFamily = TajawalFontFamily)
                        }

                        // South
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Text(if (isArabic) "جنوب" else "South", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = TajawalFontFamily)
                            Text("S", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // East
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(if (isArabic) "شرق" else "East", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = IslamicGold, fontFamily = TajawalFontFamily)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("E", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IslamicGold)
                        }

                        // West
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text("W", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isArabic) "غرب" else "West", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = TajawalFontFamily)
                        }
                    }

                    // Qibla Needle & Kaaba Symbol (Rotates to relative Qibla Angle)
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .rotate(animatedNeedleRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val needleLength = size.width / 2 - 20.dp.toPx()

                            // Drawing 3D Dual-Tone Qibla Needle Path (Pointing UP / 0 deg in local box)
                            val leftNeedlePath = Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x - 7.dp.toPx(), center.y)
                                lineTo(center.x, center.y)
                                close()
                            }

                            val rightNeedlePath = Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x + 7.dp.toPx(), center.y)
                                lineTo(center.x, center.y)
                                close()
                            }

                            // Needle Colors
                            val mainNeedleColor = if (isAligned) EmeraldPrimary else IslamicGold
                            val shadowNeedleColor = if (isAligned) EmeraldPrimary.copy(alpha = 0.7f) else GoldAccent

                            drawPath(leftNeedlePath, color = mainNeedleColor)
                            drawPath(rightNeedlePath, color = shadowNeedleColor)

                            // Tail counterweight
                            val tailPath = Path().apply {
                                moveTo(center.x, center.y + 24.dp.toPx())
                                lineTo(center.x - 5.dp.toPx(), center.y)
                                lineTo(center.x + 5.dp.toPx(), center.y)
                                close()
                            }
                            drawPath(tailPath, color = Color.Gray.copy(alpha = 0.5f))

                            // Miniature Kaaba Icon at the Tip of the Needle
                            val kaabaSize = 22.dp.toPx()
                            val kaabaTop = center.y - needleLength - kaabaSize / 2

                            // Kaaba Body (Black Cube)
                            drawRoundRect(
                                color = Color(0xFF1A1A1A),
                                topLeft = Offset(center.x - kaabaSize / 2, kaabaTop),
                                size = Size(kaabaSize, kaabaSize),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            // Kaaba Golden Kiswah Belt Line
                            drawRect(
                                color = IslamicGold,
                                topLeft = Offset(center.x - kaabaSize / 2, kaabaTop + kaabaSize * 0.25f),
                                size = Size(kaabaSize, kaabaSize * 0.18f)
                            )
                        }
                    }

                    // Center Pivot Hub
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isAligned) EmeraldPrimary else IslamicGold)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            // Sensor Controls & Calibration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (hasHardwareSensors) Icons.Default.Sensors else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasHardwareSensors) EmeraldPrimary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (hasHardwareSensors)
                                    (if (isArabic) "حساس البوصلة: نشط (${((effectiveAzimuth % 360f + 360f) % 360f).toInt()}°)" else "Compass Sensor: Active (${((effectiveAzimuth % 360f + 360f) % 360f).toInt()}°)")
                                else
                                    (if (isArabic) "البوصلة المغناطيسية غير متوفرة" else "Hardware Compass Sensor Unavailable"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Toggle manual vs automatic mode
                        FilterChip(
                            selected = manualMode,
                            onClick = { manualMode = !manualMode },
                            label = {
                                Text(
                                    text = if (manualMode) (if (isArabic) "وضع اللمس 👆" else "Touch Mode 👆") else (if (isArabic) "تلقائي 📱" else "Sensors 📱"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (manualMode) {
                        Text(
                            text = if (isArabic) "اسحب بأصبعك دائرِياً على البوصلة فوق لتتدرب على توجيه القبلة." else "Drag your finger in a circle around the compass dial to adjust orientation.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                            color = EmeraldPrimary
                        )
                    } else {
                        Text(
                            text = if (isArabic) "لضمان دقة القبلة، حرك الهاتف في الهواء على شكل رقم (8) للـ معايرة، وضعه على سطح مستوٍ بعيداً عن المعادن." else "For maximum accuracy, move phone in a figure-8 pattern to calibrate sensors and lay on a flat surface.",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // City Selection Dialog
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = {
                Text(
                    text = if (isArabic) "اختر مدينتك لتحديد القبلة" else "Select City for Qibla",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CITIES.forEach { city ->
                        val isSelected = city.nameAr == selectedCity.nameAr || city.nameEn == selectedCity.nameEn
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) city.nameAr else city.nameEn,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    val (cityQibla, cityDist) = QuranDataProvider.calculateQibla(city.lat, city.lng)
                                    Text(
                                        text = "${cityQibla.toInt()}° | ${cityDist.toInt()} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        selectedCity = city
                                        showCityDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) (if (isArabic) "مُحدد" else "Selected") else (if (isArabic) "اختيار" else "Select"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }
}
