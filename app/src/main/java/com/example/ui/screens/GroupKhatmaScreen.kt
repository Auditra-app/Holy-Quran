package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.GroupKhatmaManager
import com.example.domain.model.AppLanguage
import com.example.domain.model.GroupKhatma
import com.example.domain.model.GroupKhatmaJuz
import com.example.domain.model.GroupKhatmaJuzStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val juzArabicNames = listOf(
    "الجزء الأول", "الجزء الثاني", "الجزء الثالث", "الجزء الرابع", "الجزء الخامس",
    "الجزء السادس", "الجزء السابع", "الجزء الثامن", "الجزء التاسع", "الجزء العاشر",
    "الجزء الحادي عشر", "الجزء الثاني عشر", "الجزء الثالث عشر", "الجزء الرابع عشر", "الجزء الخامس عشر",
    "الجزء السادس عشر", "الجزء السابع عشر", "الجزء الثامن عشر", "الجزء التاسع عشر", "الجزء العشرون",
    "الجزء الحادي والعشرون", "الجزء الثاني والعشرون", "الجزء الثالث والعشرون", "الجزء الرابع والعشرون", "الجزء الخامس والعشرون",
    "الجزء السادس والعشرون", "الجزء السابع والعشرون", "الجزء الثامن والعشرون", "الجزء التاسع والعشرون", "الجزء الثلاثون"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupKhatmaScreen(
    currentLanguage: AppLanguage,
    onNavigateBack: () -> Unit,
    onReadJuzClick: (Int) -> Unit = {},
    initialKhatmaCode: String? = null
) {
    val context = LocalContext.current
    val isArabic = currentLanguage == AppLanguage.ARABIC

    var allKhatmas by remember { mutableStateOf(GroupKhatmaManager.getAllKhatmas(context)) }
    var activeKhatma by remember {
        mutableStateOf(
            allKhatmas.firstOrNull() ?: GroupKhatmaManager.createNewKhatma(
                context,
                title = if (isArabic) "ختمة المحبة والتآخي" else "Shared Group Khatma",
                creatorName = if (isArabic) "مشارك" else "Participant"
            )
        )
    }

    LaunchedEffect(Unit) {
        com.example.util.AnalyticsHelper.logScreenView("GroupKhatmaScreen")
        com.example.util.AnalyticsHelper.logKhatmaEvent("opened", activeKhatma.id)
    }

    LaunchedEffect(initialKhatmaCode) {
        if (!initialKhatmaCode.isNullOrBlank()) {
            try {
                val imported = GroupKhatmaManager.decodeKhatmaFromCode(initialKhatmaCode)
                if (imported != null) {
                    GroupKhatmaManager.saveKhatma(context, imported)
                    allKhatmas = GroupKhatmaManager.getAllKhatmas(context)
                    activeKhatma = imported
                    com.example.util.AnalyticsHelper.logKhatmaEvent("joined", imported.id)
                }
            } catch (e: Throwable) {
                android.util.Log.e("GroupKhatmaScreen", "Error handling initial khatma code", e)
            }
        }
    }

    // View mode: True = 30-Juz Compact Visual Grid (Single Glance), False = Detailed List View
    var isGridView by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSelectKhatmaDialog by remember { mutableStateOf(false) }
    var showDeadlineDialog by remember { mutableStateOf(false) }
    
    // Juz selected for details / reservation / reading
    var selectedJuzDetails by remember { mutableStateOf<GroupKhatmaJuz?>(null) }
    var savedUserName by remember { mutableStateOf(GroupKhatmaManager.getSavedUserName(context)) }

    // Live ticker for active countdowns
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Real-time Cloud Sync Observer
    LaunchedEffect(activeKhatma.id) {
        try {
            com.example.data.provider.FirebaseKhatmaRepository.observeKhatma(activeKhatma.id).collect { liveKhatma ->
                if (liveKhatma != null) {
                    activeKhatma = liveKhatma
                    GroupKhatmaManager.saveKhatma(context, liveKhatma)
                    allKhatmas = GroupKhatmaManager.getAllKhatmas(context)
                    selectedJuzDetails?.let { current ->
                        selectedJuzDetails = liveKhatma.juzList.firstOrNull { it.juzNumber == current.juzNumber }
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("GroupKhatmaScreen", "Error in observeKhatma flow", e)
        }
    }

    fun refreshData() {
        allKhatmas = GroupKhatmaManager.getAllKhatmas(context)
        activeKhatma = allKhatmas.firstOrNull { it.id == activeKhatma.id }
            ?: allKhatmas.firstOrNull()
            ?: GroupKhatmaManager.createNewKhatma(context, "ختمة جماعية", "مشارك")
        
        selectedJuzDetails?.let { current ->
            selectedJuzDetails = activeKhatma.juzList.firstOrNull { it.juzNumber == current.juzNumber }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "الختمة الجماعية (مشاركة الأجر)" else "Group Khatma (Shared)",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = activeKhatma.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = IslamicGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("group_khatma_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSelectKhatmaDialog = true },
                        modifier = Modifier.testTag("select_khatma_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "My Khatmas", tint = EmeraldPrimary)
                    }
                    IconButton(
                        onClick = {
                            val msg = GroupKhatmaManager.generateShareableMessage(activeKhatma, isArabic)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, if (isArabic) "مشاركة الختمة" else "Share Khatma"))
                        },
                        modifier = Modifier.testTag("share_khatma_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = IslamicGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWide = this.maxWidth > 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Summary Header Card & Re-allocated non-wrapping action buttons
                KhatmaSummaryHeaderCard(
                    khatma = activeKhatma,
                    isArabic = isArabic,
                    currentTimeMillis = currentTimeMillis,
                    onSetDeadlineClick = { showDeadlineDialog = true },
                    onCreateClick = { showCreateDialog = true },
                    onImportClick = { showImportDialog = true },
                    onSelectKhatmaClick = { showSelectKhatmaDialog = true },
                    onShareClick = {
                        val msg = GroupKhatmaManager.generateShareableMessage(activeKhatma, isArabic)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Khatma Code", msg))
                        Toast.makeText(context, if (isArabic) "تم نسخ كود ورابط الختمة بنجاح!" else "Khatma code & link copied!", Toast.LENGTH_SHORT).show()
                    }
                )

                // View Toggle & Legend Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "أجزاء القرآن الـ 30:" else "30 Quran Ajzaa:",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    // Compact / List View Switcher (Icons Only)
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = isGridView,
                            onClick = { isGridView = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                activeContentColor = EmeraldPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = if (isArabic) "عرض الشبكة" else "Grid View",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        SegmentedButton(
                            selected = !isGridView,
                            onClick = { isGridView = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                activeContentColor = EmeraldPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = if (isArabic) "عرض القائمة" else "List View",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Visual Status Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF2E7D32), label = if (isArabic) "مكتمل 🟢" else "Done")
                    LegendItem(color = IslamicGold, label = if (isArabic) "محجوز 🟡" else "Booked")
                    LegendItem(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), label = if (isArabic) "متاح ⚪" else "Free")
                }

                if (isGridView) {
                    // Single-Glance Compact Grid View of all 30 Ajzaa
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isWide) 6 else 5),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        gridItems(activeKhatma.juzList, key = { it.juzNumber }) { juz ->
                            CompactJuzGridTile(
                                juz = juz,
                                isArabic = isArabic,
                                onClick = { selectedJuzDetails = juz }
                            )
                        }
                    }
                } else {
                    // Detailed Scrollable List View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isWide) 2 else 1),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        gridItems(activeKhatma.juzList, key = { it.juzNumber }) { juz ->
                            JuzCardItem(
                                juz = juz,
                                isArabic = isArabic,
                                onBookClick = { selectedJuzDetails = juz },
                                onReadClick = { onReadJuzClick(juz.juzNumber) },
                                onStatusChange = { newStatus ->
                                    GroupKhatmaManager.updateJuzReservation(
                                        context = context,
                                        khatmaId = activeKhatma.id,
                                        juzNumber = juz.juzNumber,
                                        userName = juz.assignedUserName.ifBlank { savedUserName },
                                        newStatus = newStatus
                                    )
                                    refreshData()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Juz Action & Details Modal Sheet / Dialog
    if (selectedJuzDetails != null) {
        val juz = selectedJuzDetails!!
        val juzTitle = juzArabicNames.getOrElse(juz.juzNumber - 1) { "الجزء ${juz.juzNumber}" }
        var inputName by remember { mutableStateOf(juz.assignedUserName.ifBlank { savedUserName }) }

        AlertDialog(
            onDismissRequest = { selectedJuzDetails = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = when (juz.status) {
                        GroupKhatmaJuzStatus.COMPLETED -> Color(0xFF2E7D32)
                        GroupKhatmaJuzStatus.BOOKED -> IslamicGold
                        GroupKhatmaJuzStatus.UNASSIGNED -> EmeraldPrimary
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${juz.juzNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            },
            title = {
                Text(
                    text = juzTitle,
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Status Badge
                    val statusText = when (juz.status) {
                        GroupKhatmaJuzStatus.COMPLETED -> if (isArabic) "🟢 تمت القراءة (مكتمل)" else "🟢 Completed"
                        GroupKhatmaJuzStatus.BOOKED -> if (isArabic) "🟡 محجوز للقراءة" else "🟡 Reserved"
                        GroupKhatmaJuzStatus.UNASSIGNED -> if (isArabic) "⚪ متاح للحجز" else "⚪ Available"
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Participant Name Field
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text(if (isArabic) "اسم القارئ / المشارك" else "Participant Name") },
                        placeholder = { Text(if (isArabic) "مثال: عبد الله" else "e.g. Abdullah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Quick Actions inside Modal
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Read in Quran
                        OutlinedButton(
                            onClick = {
                                selectedJuzDetails = null
                                onReadJuzClick(juz.juzNumber)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "قراءة الجزء في المصحف" else "Read Juz in Quran")
                        }

                        // Mark Booked Button
                        Button(
                            onClick = {
                                val nameToSave = inputName.ifBlank { if (isArabic) "مشارك" else "Participant" }
                                GroupKhatmaManager.updateJuzReservation(
                                    context = context,
                                    khatmaId = activeKhatma.id,
                                    juzNumber = juz.juzNumber,
                                    userName = nameToSave,
                                    newStatus = GroupKhatmaJuzStatus.BOOKED
                                )
                                savedUserName = nameToSave
                                refreshData()
                                Toast.makeText(context, if (isArabic) "تم حجز $juzTitle بنجاح!" else "Juz booked successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "حجز الجزء باسمي / القارئ" else "Book / Reserve Juz")
                        }

                        // Mark Complete Button
                        Button(
                            onClick = {
                                val nameToSave = inputName.ifBlank { if (isArabic) "مشارك" else "Participant" }
                                GroupKhatmaManager.updateJuzReservation(
                                    context = context,
                                    khatmaId = activeKhatma.id,
                                    juzNumber = juz.juzNumber,
                                    userName = nameToSave,
                                    newStatus = GroupKhatmaJuzStatus.COMPLETED
                                )
                                savedUserName = nameToSave
                                refreshData()
                                Toast.makeText(context, if (isArabic) "بارك الله فيك، تم تسجيل إتمام $juzTitle!" else "Marked as completed!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "تأكيد إتمام القراءة (مكتمل)" else "Mark as Completed")
                        }

                        if (juz.status != GroupKhatmaJuzStatus.UNASSIGNED) {
                            TextButton(
                                onClick = {
                                    GroupKhatmaManager.updateJuzReservation(
                                        context = context,
                                        khatmaId = activeKhatma.id,
                                        juzNumber = juz.juzNumber,
                                        userName = "",
                                        newStatus = GroupKhatmaJuzStatus.UNASSIGNED
                                    )
                                    refreshData()
                                    Toast.makeText(context, if (isArabic) "تم إلغاء حجز الجزء" else "Reservation reset", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "إلغاء الحجز وتركه متاحاً" else "Reset Reservation", color = Color.Red)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedJuzDetails = null }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }

    // Create New Khatma Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newCreatorName by remember { mutableStateOf(savedUserName) }
        var selectedDeadlineOption by remember { mutableStateOf(0L) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            icon = { Icon(Icons.Default.AddCircle, contentDescription = null, tint = IslamicGold) },
            title = {
                Text(
                    text = if (isArabic) "إنشاء ختمة جماعية جديدة" else "Create New Group Khatma",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(if (isArabic) "عنوان الختمة" else "Khatma Title") },
                        placeholder = { Text(if (isArabic) "مثال: ختمة المرحوم... / ختمة شهر رمضان" else "e.g., Family Khatma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCreatorName,
                        onValueChange = { newCreatorName = it },
                        label = { Text(if (isArabic) "اسم منشئ الختمة" else "Creator Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isArabic) "موعد إغلاق الختمة (مؤقت مباشر):" else "Closing Deadline Timer:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    val now = System.currentTimeMillis()
                    val deadlineOptions = listOf(
                        Pair(if (isArabic) "بدون مؤقت" else "None", 0L),
                        Pair(if (isArabic) "6 ساعات" else "6 Hours", now + 6 * 3600 * 1000L),
                        Pair(if (isArabic) "24 ساعة" else "24 Hours", now + 24 * 3600 * 1000L),
                        Pair(if (isArabic) "05:00 مساءً" else "05:00 PM", getTodayAtHourMillis(17)),
                        Pair(if (isArabic) "3 أيام" else "3 Days", now + 3 * 24 * 3600 * 1000L)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        deadlineOptions.forEach { (label, ts) ->
                            FilterChip(
                                selected = selectedDeadlineOption == ts,
                                onClick = { selectedDeadlineOption = ts },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val created = GroupKhatmaManager.createNewKhatma(
                            context,
                            title = newTitle.ifBlank { "ختمة قرآنية مباركة" },
                            creatorName = newCreatorName.ifBlank { "مشارك" },
                            targetDeadlineTimestamp = selectedDeadlineOption
                        )
                        activeKhatma = created
                        refreshData()
                        com.example.util.AnalyticsHelper.logKhatmaEvent("created", created.id)
                        showCreateDialog = false
                        Toast.makeText(context, if (isArabic) "تم إنشاء الختمة بنجاح!" else "Khatma created successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isArabic) "إنشاء الختمة" else "Create Khatma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Set / Edit Deadline Timer Dialog
    if (showDeadlineDialog) {
        AlertDialog(
            onDismissRequest = { showDeadlineDialog = false },
            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = IslamicGold) },
            title = {
                Text(
                    text = if (isArabic) "تحديد موعد إغلاق الختمة (المؤقت المباشر)" else "Set Khatma Closing Timer",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "اختر الوقت المتبقي أو الموعد المحدد لإغلاق هذه الختمة ليظهر العداد المباشر لجميع المشاركين:"
                        else "Select target closing deadline for all participants:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val now = System.currentTimeMillis()
                    val options = listOf(
                        Pair(if (isArabic) "⏱️ خلال 3 ساعات" else "3 Hours", now + 3 * 3600 * 1000L),
                        Pair(if (isArabic) "⏱️ خلال 6 ساعات" else "6 Hours", now + 6 * 3600 * 1000L),
                        Pair(if (isArabic) "⏱️ خلال 12 ساعة" else "12 Hours", now + 12 * 3600 * 1000L),
                        Pair(if (isArabic) "🌅 اليوم/غداً الساعة 05:00 مساءً" else "05:00 PM", getTodayAtHourMillis(17)),
                        Pair(if (isArabic) "📅 خلال 24 ساعة (يوم كامل)" else "24 Hours (1 Day)", now + 24 * 3600 * 1000L),
                        Pair(if (isArabic) "📅 خلال 3 أيام" else "3 Days", now + 3 * 24 * 3600 * 1000L),
                        Pair(if (isArabic) "📅 خلال 7 أيام (أسبوع)" else "7 Days (1 Week)", now + 7 * 24 * 3600 * 1000L),
                        Pair(if (isArabic) "🚫 بدون موعد محدد (إلغاء المؤقت)" else "No Deadline", 0L)
                    )

                    options.forEach { (label, timestamp) ->
                        OutlinedButton(
                            onClick = {
                                val updated = activeKhatma.copy(targetDeadlineTimestamp = timestamp)
                                GroupKhatmaManager.saveKhatma(context, updated)
                                refreshData()
                                showDeadlineDialog = false
                                Toast.makeText(context, if (isArabic) "تم تحديث موعد إغلاق الختمة!" else "Deadline updated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (timestamp > 0L && Math.abs(timestamp - activeKhatma.targetDeadlineTimestamp) < 60000L) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeadlineDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Import Khatma Code Dialog
    if (showImportDialog) {
        var inputCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary) },
            title = {
                Text(
                    text = if (isArabic) "استيراد ختمة من كود / رابط" else "Import Khatma from Link / Code",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) "ألصق كود الختمة المشترك (الذي يبدأ بـ KHATMA#) أو الرابط للانضمام إليه:" else "Paste the shared Khatma code or link to join:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text(if (isArabic) "كود أو رابط الختمة" else "Khatma Code / Link") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = clipboard.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                                    if (pastedText.isNotBlank()) {
                                        inputCode = pastedText
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "لصق من الحافظة" else "Paste Clipboard")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = GroupKhatmaManager.decodeKhatmaFromCode(inputCode)
                        if (imported != null) {
                            GroupKhatmaManager.saveKhatma(context, imported)
                            activeKhatma = imported
                            refreshData()
                            com.example.util.AnalyticsHelper.logKhatmaEvent("joined", imported.id)
                            showImportDialog = false
                            Toast.makeText(context, if (isArabic) "تم استيراد الختمة '${imported.title}' بنجاح!" else "Khatma imported successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, if (isArabic) "كود الختمة غير صحيح أو غير مكتمل" else "Invalid Khatma code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isArabic) "استيراد وانضمام" else "Import & Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Select Khatma Dialog
    if (showSelectKhatmaDialog) {
        AlertDialog(
            onDismissRequest = { showSelectKhatmaDialog = false },
            icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = EmeraldPrimary) },
            title = {
                Text(
                    text = if (isArabic) "قائمة الختمات الجماعية المحفوظة" else "Saved Group Khatmas",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(allKhatmas) { k ->
                        val isCurrent = k.id == activeKhatma.id
                        val completedCount = k.juzList.count { it.status == GroupKhatmaJuzStatus.COMPLETED }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isCurrent) EmeraldPrimary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeKhatma = k
                                    showSelectKhatmaDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(k.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isArabic) "مكتمل: $completedCount من 30 جزء" else "Completed: $completedCount / 30",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (allKhatmas.size > 1) {
                                    IconButton(
                                        onClick = {
                                            GroupKhatmaManager.deleteKhatma(context, k.id)
                                            refreshData()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectKhatmaDialog = false
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إنشاء ختمة جديدة" else "New Khatma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectKhatmaDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }
}

fun getTodayAtHourMillis(hour24: Int): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    if (cal.timeInMillis <= System.currentTimeMillis()) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

@Composable
fun KhatmaSummaryHeaderCard(
    khatma: GroupKhatma,
    isArabic: Boolean,
    currentTimeMillis: Long = System.currentTimeMillis(),
    onSetDeadlineClick: () -> Unit = {},
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    onSelectKhatmaClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val completedCount = khatma.juzList.count { it.status == GroupKhatmaJuzStatus.COMPLETED }
    val bookedCount = khatma.juzList.count { it.status == GroupKhatmaJuzStatus.BOOKED }
    val unassignedCount = 30 - completedCount - bookedCount
    val progressPercent = (completedCount / 30f)
    val remainingMillis = khatma.targetDeadlineTimestamp - currentTimeMillis

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.08f),
                            IslamicGold.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Title and Progress Circle Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = khatma.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (khatma.creatorName.isNotBlank()) {
                                Text(
                                    text = if (isArabic) "بواسطة: ${khatma.creatorName}" else "By: ${khatma.creatorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IslamicGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, IslamicGold)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Live Closing Timer Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (remainingMillis <= 0L && khatma.targetDeadlineTimestamp > 0L) Color(0xFFFEF2F2) else EmeraldPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (remainingMillis <= 0L && khatma.targetDeadlineTimestamp > 0L) Color(0xFFEF4444) else IslamicGold.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetDeadlineClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (remainingMillis <= 0L && khatma.targetDeadlineTimestamp > 0L) Color(0xFFDC2626) else IslamicGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                if (khatma.targetDeadlineTimestamp > 0L) {
                                    if (remainingMillis > 0L) {
                                        val hours = remainingMillis / (1000 * 60 * 60)
                                        val minutes = (remainingMillis / (1000 * 60)) % 60
                                        val seconds = (remainingMillis / 1000) % 60
                                        val formattedTime = if (hours >= 24) {
                                            val days = hours / 24
                                            val remHours = hours % 24
                                            if (isArabic) "$days يوم و $remHours ساعة" else "$days days & $remHours hrs"
                                        } else {
                                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                                        }

                                        Text(
                                            text = if (isArabic) "متبقي على إغلاق الختمة:" else "Closing timer:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "⏱️ $formattedTime",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    } else {
                                        Text(
                                            text = if (isArabic) "⌛ انتهت مهلة الختمة المحددة" else "⌛ Deadline elapsed",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isArabic) "تحديد وقت لإغلاق الختمة (مؤقت مباشر)" else "Set Khatma Closing Timer",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onSetDeadlineClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Set Timer",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Stats Summary Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(
                        label = if (isArabic) "مكتمل" else "Done",
                        count = "$completedCount",
                        color = Color(0xFF2E7D32),
                        icon = Icons.Default.CheckCircle
                    )
                    StatPill(
                        label = if (isArabic) "محجوز" else "Booked",
                        count = "$bookedCount",
                        color = Color(0xFFD97706),
                        icon = Icons.Default.Bookmark
                    )
                    StatPill(
                        label = if (isArabic) "متاح" else "Free",
                        count = "$unassignedCount",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.RadioButtonUnchecked
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Structured Action Buttons Grid (2x2 with clear labels)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShareClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "مشاركة الختمة" else "Share Khatma",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onImportClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "الانضمام لكود" else "Join by Code",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onSelectKhatmaClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, IslamicGold),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "ختماتي" else "My Khatmas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    count: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(text = "($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp)
        ) {}
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Premium Ornamented 30-Juz Tile for Visual Grid View
@Composable
fun CompactJuzGridTile(
    juz: GroupKhatmaJuz,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val containerBg = when (juz.status) {
        GroupKhatmaJuzStatus.COMPLETED -> Color(0xFF1B5E20)
        GroupKhatmaJuzStatus.BOOKED -> Color(0xFFD97706)
        GroupKhatmaJuzStatus.UNASSIGNED -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (juz.status) {
        GroupKhatmaJuzStatus.COMPLETED -> Color.White
        GroupKhatmaJuzStatus.BOOKED -> Color.White
        GroupKhatmaJuzStatus.UNASSIGNED -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = when (juz.status) {
        GroupKhatmaJuzStatus.COMPLETED -> BorderStroke(1.dp, IslamicGold)
        GroupKhatmaJuzStatus.BOOKED -> BorderStroke(1.dp, Color(0xFFB45309))
        GroupKhatmaJuzStatus.UNASSIGNED -> BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (juz.status != GroupKhatmaJuzStatus.UNASSIGNED) 2.dp else 0.dp),
        border = borderStroke,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                // Juz Number inside Rub-El-Hizb / Octagonal Badge styling
                Text(
                    text = "${juz.juzNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = TajawalFontFamily,
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Status Indicator / Participant Name
                when (juz.status) {
                    GroupKhatmaJuzStatus.COMPLETED -> {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold,
                            modifier = Modifier.size(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color.Black,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    GroupKhatmaJuzStatus.BOOKED -> {
                        if (juz.assignedUserName.isNotBlank()) {
                            Text(
                                text = juz.assignedUserName.take(6),
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Booked",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    GroupKhatmaJuzStatus.UNASSIGNED -> {
                        Text(
                            text = if (isArabic) "متاح" else "Free",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun JuzCardItem(
    juz: GroupKhatmaJuz,
    isArabic: Boolean,
    onBookClick: () -> Unit,
    onReadClick: () -> Unit,
    onStatusChange: (GroupKhatmaJuzStatus) -> Unit
) {
    val juzTitle = juzArabicNames.getOrElse(juz.juzNumber - 1) { "الجزء ${juz.juzNumber}" }

    val containerBg = when (juz.status) {
        GroupKhatmaJuzStatus.COMPLETED -> Color(0xFFE8F5E9)
        GroupKhatmaJuzStatus.BOOKED -> Color(0xFFFFFDE7)
        GroupKhatmaJuzStatus.UNASSIGNED -> MaterialTheme.colorScheme.surface
    }

    val borderStrokeColor = when (juz.status) {
        GroupKhatmaJuzStatus.COMPLETED -> Color(0xFF4CAF50)
        GroupKhatmaJuzStatus.BOOKED -> IslamicGold
        GroupKhatmaJuzStatus.UNASSIGNED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, borderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Juz Number Icon & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when (juz.status) {
                        GroupKhatmaJuzStatus.COMPLETED -> Color(0xFF2E7D32)
                        GroupKhatmaJuzStatus.BOOKED -> IslamicGold
                        GroupKhatmaJuzStatus.UNASSIGNED -> EmeraldPrimary
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${juz.juzNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = juzTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily)
                    )

                    when (juz.status) {
                        GroupKhatmaJuzStatus.COMPLETED -> {
                            Text(
                                text = if (isArabic) "تمت القراءة: ${juz.assignedUserName}" else "Completed: ${juz.assignedUserName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GroupKhatmaJuzStatus.BOOKED -> {
                            Text(
                                text = if (isArabic) "محجوز لـ: ${juz.assignedUserName}" else "Reserved for: ${juz.assignedUserName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GroupKhatmaJuzStatus.UNASSIGNED -> {
                            Text(
                                text = if (isArabic) "متاح للحجز 📖" else "Available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (juz.status) {
                    GroupKhatmaJuzStatus.UNASSIGNED -> {
                        Button(
                            onClick = onBookClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "احجز" else "Book", fontSize = 12.sp)
                        }
                    }
                    GroupKhatmaJuzStatus.BOOKED -> {
                        Button(
                            onClick = { onStatusChange(GroupKhatmaJuzStatus.COMPLETED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "تمت" else "Done", fontSize = 11.sp)
                        }
                        IconButton(onClick = { onStatusChange(GroupKhatmaJuzStatus.UNASSIGNED) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    GroupKhatmaJuzStatus.COMPLETED -> {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC8E6C9),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Done", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = { onStatusChange(GroupKhatmaJuzStatus.BOOKED) }) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

