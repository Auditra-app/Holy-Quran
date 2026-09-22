package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.R
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val splashStartTime = remember { System.currentTimeMillis() }
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val neonRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neonRotation"
    )

    val neonPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neonPulseAlpha"
    )

    val goldMetallicBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFDD0),
                Color(0xFFFFE885),
                Color(0xFFFFD700),
                Color(0xFFDAA520),
                Color(0xFFB8860B)
            )
        )
    }

    val holyGlowColors = remember {
        listOf(
            Color(0xFFFFDF7A), // Sacred Light Gold
            Color(0xFF80E8D4), // Soft Celestial Emerald
            Color(0xFFFFF5C0), // Pure Light
            Color(0xFF52D6B6), // Soft Green
            Color(0xFFFFDF7A)  // Sacred Light Gold
        )
    }

    LaunchedEffect(Unit) {
        Log.d("SplashScreen", " SplashScreen: Composition mounted. Beginning transition animations...")
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 800)
                )
            }
        }
        Log.d("SplashScreen", " SplashScreen: Animations completed in ${System.currentTimeMillis() - splashStartTime}ms. Waiting for display timeout...")
        delay(1800)
        Log.d("SplashScreen", " SplashScreen: Display timeout reached. Triggering onSplashFinished callback...")
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041912),
                        Color(0xFF0A2E24),
                        Color(0xFF041912)
                    )
                )
            )
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background geometric radial glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseGlow)
                .alpha(0.25f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            IslamicGold,
                            Color(0x22DAA520),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp)
        ) {
            // App Logo Emblem with Smooth Moving Neon Circle Effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Outer Soft Neon Glow Aura
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseGlow)
                        .alpha(0.25f * neonPulseAlpha)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF80E8D4),
                                Color(0xFFFFDF7A),
                                Color.Transparent
                            )
                        )
                    )
                }

                // Smooth Rotating Outer Sacred Ring
                Canvas(
                    modifier = Modifier
                        .size(145.dp)
                        .rotate(neonRotation)
                        .alpha(neonPulseAlpha)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(holyGlowColors),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Counter-Rotating Inner Accent Ring
                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .rotate(-neonRotation * 0.7f)
                        .alpha(neonPulseAlpha * 0.7f)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFF5C0),
                                Color(0xFF80E8D4),
                                Color(0xFFFFDF7A),
                                Color(0xFF80E8D4),
                                Color(0xFFFFF5C0)
                            )
                        ),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Central App Launcher Logo
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF06291E),
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold),
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Holy Quran Logo",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Holy Quran" Subtitle directly under the App Logo (small, thin, artistic lettering)
            Text(
                text = "H O L Y   Q U R A N",
                fontFamily = com.example.ui.theme.TajawalFontFamily,
                fontSize = 12.sp,
                style = TextStyle(
                    brush = goldMetallicBrush,
                    shadow = Shadow(
                        color = Color(0x40000000),
                        offset = Offset(0.5f, 0.5f),
                        blurRadius = 2f
                    )
                ),
                fontWeight = FontWeight.Light,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, IslamicGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "صدقة جارية • نسألكم الدعاء لي ولوالدي وأهل بيتي",
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Progress Indicator & Loading Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                color = IslamicGold,
                trackColor = Color(0x33FFFFFF),
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "جاري تحضير المصحف الشريف...",
                fontFamily = TajawalFontFamily,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun QuranOrnateCalligraphyBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Left Emerald Geometric Lattice Ornament
        EmeraldLatticeOrnament(isFlip = false)

        Spacer(modifier = Modifier.width(12.dp))

        // Center Stacked 3D Golden Calligraphy
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Shadow / Extrusion layer for 3D metallic depth effect
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(x = 2.dp, y = 2.dp)
            ) {
                Text(
                    text = "الْقُرْآنُ",
                    fontFamily = TajawalFontFamily,
                    fontSize = 46.sp,
                    color = Color(0xFF5C4308),
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "الْكَرِيمُ",
                    fontFamily = TajawalFontFamily,
                    fontSize = 46.sp,
                    color = Color(0xFF5C4308),
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Primary Metallic Gold Calligraphy Layer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الْقُرْآنُ",
                    fontFamily = TajawalFontFamily,
                    fontSize = 46.sp,
                    color = IslamicGold,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "الْكَرِيمُ",
                    fontFamily = TajawalFontFamily,
                    fontSize = 46.sp,
                    color = IslamicGold,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Emerald Geometric Lattice Ornament
        EmeraldLatticeOrnament(isFlip = true)
    }
}

@Composable
fun EmeraldLatticeOrnament(isFlip: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(54.dp, 80.dp)
    ) {
        // Gold Geometric Lattice Star Frame
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF134234),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold.copy(alpha = 0.8f)),
            modifier = Modifier.size(46.dp, 72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Inner Decorative Ring
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, IslamicGold.copy(alpha = 0.4f), CircleShape)
                )

                // Emerald Gemstone Center
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00FF87),
                                    Color(0xFF00A86B),
                                    Color(0xFF05472A)
                                )
                            )
                        )
                        .border(1.dp, IslamicGold, CircleShape)
                )
            }
        }
    }
}
