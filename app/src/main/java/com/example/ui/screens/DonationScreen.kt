package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    val developerName = "علي إبراهيم الشيخ / Ali Ibrahim Elsheikh"
    val feedbackEmail = "aelsheikh1987@gmail.com"
    val whatsappNumber = "+201092892892"
    val whatsappClean = "201092892892"
    val appVersion = "v1.0.0 (Build 1)"

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, if (isArabic) "تم نسخ $label بنجاح!" else "Copied $label to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun openWhatsApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$whatsappClean")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            copyToClipboard("رقم الواتساب", whatsappNumber)
        }
    }

    fun sendFeedbackEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$feedbackEmail")
                putExtra(Intent.EXTRA_SUBJECT, "ملاحظات وتواصل - تطبيق القرآن الكريم")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            copyToClipboard("البريد الإلكتروني", feedbackEmail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "عن التطبيق والمطور" else "About & Developer Info",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("donation_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            item {
                Surface(
                    shape = CircleShape,
                    color = IslamicGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, IslamicGold),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = IslamicGold,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // App Title
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isArabic) "تطبيق القرآن الكريم" else "Holy Quran App",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "تطبيق مجاني 100% بدون أي إعلانات" else "100% Free App With Zero Ads",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Prominent Sadaqah Jariyah Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = IslamicGold.copy(alpha = 0.12f),
                    border = BorderStroke(2.dp, IslamicGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "صدقة جارية • إهداء وتضرع" else "Sadaqah Jariyah",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = if (isArabic) "الدعاء لي ولوالدي وأهل بيتي" else "Dua for Me, My Parents & My Family",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = TajawalFontFamily, fontSize = 22.sp),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isArabic)
                                "نسأل الله العلي القدير أن يجعل هذا العمل خالصاً لوجهه الكريم، وصدقة جارية عني (علي إبراهيم الشيخ) وعن والديّ وأهل بيتي، ولكل من ساهم في إكمال هذا التطبيق ومن ساهم في نشره وقراءته والعمل به.\n\nفضلاً لا تنسونا من صالح دعائكم بالرحمة والمغفرة والقبول."
                            else
                                "May Almighty Allah accept this noble application as an ongoing charity (Sadaqah Jariyah) for me (Ali Ibrahim Elsheikh), my parents, my household, and for everyone who contributed to completing this application and spreading it.\n\nPlease keep us in your sincere prayers.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, lineHeight = 24.sp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Developer Details & Contact
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(
                                    text = if (isArabic) "المطور والمنفذ:" else "Developer:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = developerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Feature Suggestion / Contact Title
                        Text(
                            text = if (isArabic) "💡 للتواصل واقتراح الميزات مع المطور:" else "💡 Contact & Feature Suggestions:",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )

                        // WhatsApp Contact Option (Single clean row card)
                        Card(
                            onClick = { openWhatsApp() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF25D366).copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().testTag("whatsapp_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isArabic) "واتساب المطور" else "WhatsApp Contact",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = "\u202A$whatsappNumber\u202C",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = { openWhatsApp() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "مراسلة" else "Chat",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Email Contact Option (Single clean row card)
                        Card(
                            onClick = { sendFeedbackEmail() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().testTag("contact_developer_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isArabic) "البريد الإلكتروني" else "Email Feedback",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = feedbackEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = { sendFeedbackEmail() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "إرسال" else "Send",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App Version Footer
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = if (isArabic) "إصدار التطبيق: $appVersion" else "App Version: $appVersion",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Holy Quran Mobile Application",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
