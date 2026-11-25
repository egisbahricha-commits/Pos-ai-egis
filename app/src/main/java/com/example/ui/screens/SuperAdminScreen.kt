package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.AdminLicensePlanOption
import com.example.firebase.LicenseDoc
import com.example.supabase.SupabaseLicenseRepository
import com.example.ui.components.DesignFilterChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val licenses by viewModel.allLicenses.collectAsStateWithLifecycle()
    val allDonations by viewModel.allDonationRequests.collectAsStateWithLifecycle()
    val allBugReports by viewModel.allBugReports.collectAsStateWithLifecycle()
    val bankConfig by viewModel.developerBankConfig.collectAsStateWithLifecycle()
    val promoStatus by viewModel.freeLifetimePromoStatus.collectAsStateWithLifecycle()
    val superAdminError by viewModel.superAdminError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSuperAdminLoading.collectAsStateWithLifecycle()

    var activeSectionTab by remember { mutableStateOf("LICENSES") } // LICENSES, DONATIONS, BUGS, BANK
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("ALL") } // ALL, DEMO, FULL, LOCKED
    var showCreateDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var newOwnerName by remember { mutableStateOf("") }
    var newOwnerEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newPlanOption by remember { mutableStateOf(AdminLicensePlanOption.MONTHLY_30) }
    var managingLicense by remember { mutableStateOf<LicenseDoc?>(null) }
    var selectedManagePlan by remember { mutableStateOf(AdminLicensePlanOption.MONTHLY_30) }
    var generatedKeyResult by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val pendingDonationsCount = allDonations.count { it.status.equals("PENDING", ignoreCase = true) }
    val newBugCount = allBugReports.count { it.status.equals("BARU", ignoreCase = true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val nowMillis = System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    LaunchedEffect(isSuperAdmin) {
        if (isSuperAdmin) {
            viewModel.loadSuperAdminLicenses()
            viewModel.loadSuperAdminDonationsAndBugs()
        }
    }

    val isLicenseExpiredOrSuspended: (LicenseDoc) -> Boolean = { doc ->
        doc.status.equals("SUSPENDED", ignoreCase = true) ||
            doc.status.equals("EXPIRED", ignoreCase = true) ||
            (doc.planType.equals("DEMO", ignoreCase = true) &&
                !doc.status.equals("ACTIVE", ignoreCase = true) &&
                doc.demoExpiresAtMillis > 0L &&
                nowMillis >= doc.demoExpiresAtMillis) ||
            (doc.expiresAtMillis > 0L && nowMillis >= doc.expiresAtMillis)
    }

    val filteredLicenses = remember(licenses, searchQuery, filterTab) {
        licenses.filter { doc ->
            val locked = isLicenseExpiredOrSuspended(doc)
            val isFull = (doc.planType.equals("FULL", ignoreCase = true) || doc.status.equals("ACTIVE", ignoreCase = true)) && !locked
            val isDemo = doc.planType.equals("DEMO", ignoreCase = true) && !locked && !doc.status.equals("ACTIVE", ignoreCase = true)

            val matchesFilter = when (filterTab) {
                "DEMO" -> isDemo
                "FULL" -> isFull
                "LOCKED" -> locked
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                doc.storeName.contains(q, ignoreCase = true) ||
                doc.ownerName.contains(q, ignoreCase = true) ||
                doc.ownerEmail.contains(q, ignoreCase = true) ||
                doc.licenseKey.contains(q, ignoreCase = true) ||
                doc.phone.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    val totalCount = licenses.size
    val demoActiveCount = licenses.count {
        it.planType.equals("DEMO", ignoreCase = true) &&
            !it.status.equals("ACTIVE", ignoreCase = true) &&
            !isLicenseExpiredOrSuspended(it)
    }
    val fullActiveCount = licenses.count {
        (it.planType.equals("FULL", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true)) &&
            !isLicenseExpiredOrSuspended(it)
    }
    val lockedCount = licenses.count { isLicenseExpiredOrSuspended(it) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Admin Pusat KASLITE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Database Lisensi Pelanggan Live", fontSize = 11.sp, color = Slate500)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadSuperAdminLicenses()
                        viewModel.loadSuperAdminDonationsAndBugs()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang Supabase")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (isSuperAdmin && activeSectionTab == "LICENSES") {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("super_admin_add_license_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Lisensi")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgCanvas)
                .testTag("super_admin_screen")
        ) {
            if (!isSuperAdmin) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Akses Ditolak",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Menu rahasia ini khusus untuk Admin Pusat ($SUPER_ADMIN_EMAIL) melalui login Email Supabase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            DesignFilterChip(
                                label = "Lisensi (${licenses.size})",
                                selected = activeSectionTab == "LICENSES",
                                onClick = { activeSectionTab = "LICENSES" }
                            )
                        }
                        item {
                            val donLabel = if (pendingDonationsCount > 0) "Donasi ($pendingDonationsCount Baru)" else "Donasi (${allDonations.size})"
                            DesignFilterChip(
                                label = donLabel,
                                selected = activeSectionTab == "DONATIONS",
                                onClick = { activeSectionTab = "DONATIONS" }
                            )
                        }
                        item {
                            val bugLabel = if (newBugCount > 0) "Laporan Bug ($newBugCount Baru)" else "Laporan Bug (${allBugReports.size})"
                            DesignFilterChip(
                                label = bugLabel,
                                selected = activeSectionTab == "BUGS",
                                onClick = { activeSectionTab = "BUGS" }
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Rekening & Email",
                                selected = activeSectionTab == "BANK",
                                onClick = { activeSectionTab = "BANK" }
                            )
                        }
                    }

                    when (activeSectionTab) {
                        "DONATIONS" -> {
                            SuperAdminDonationsTab(
                                donations = allDonations,
                                supportEmail = bankConfig.supportEmail.ifBlank { SUPER_ADMIN_EMAIL },
                                viewModel = viewModel,
                                onFeedback = { snackbarMessage = it }
                            )
                        }
                        "BUGS" -> {
                            SuperAdminBugReportsTab(
                                reports = allBugReports,
                                viewModel = viewModel,
                                onFeedback = { snackbarMessage = it }
                            )
                        }
                        "BANK" -> {
                            SuperAdminPaymentConfigTab(
                                currentConfig = bankConfig,
                                promoStatus = promoStatus,
                                registeredCustomerCount = licenses.count {
                                    !it.ownerEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                                },
                                viewModel = viewModel,
                                onFeedback = { snackbarMessage = it }
                            )
                        }
                        else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!superAdminError.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Rose100,
                                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = superAdminError ?: "",
                                    color = Rose500,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Summary Metrics Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricChip("Total", "$totalCount", AccentBlue, Modifier.weight(1f))
                            SummaryMetricChip("Demo", "$demoActiveCount", Sapphire800, Modifier.weight(1f))
                            SummaryMetricChip("Full", "$fullActiveCount", Emerald700, Modifier.weight(1f))
                            SummaryMetricChip("Expired", "$lockedCount", Rose500, Modifier.weight(1f))
                        }
                    }

                    if (promoStatus.quota > 0 || promoStatus.yearlyQuota > 0) {
                        item {
                            val hasOpenSlots = promoStatus.isActiveWithSlots
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (hasOpenSlots) Emerald50 else AmberWarningBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (hasOpenSlots) Emerald200 else AmberWarningBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Promo Pendaftar Pertama (Terdaftar: ${promoStatus.used} akun)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasOpenSlots) Emerald800 else Slate900
                                            )
                                        )
                                        if (promoStatus.quota > 0) {
                                            Text(
                                                text = "• Gratis Selamanya: Sisa ${promoStatus.remaining} dari ${promoStatus.quota} slot",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 11.sp)
                                            )
                                        }
                                        if (promoStatus.yearlyQuota > 0) {
                                            Text(
                                                text = "• Gratis 1 Tahun: Sisa ${promoStatus.yearlyRemaining} dari ${promoStatus.yearlyQuota} slot",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 11.sp)
                                            )
                                        }
                                    }
                                    TextButton(onClick = { activeSectionTab = "BANK" }) {
                                        Text("Atur Kuota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Search & Filter Controls
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari toko, pemilik, email, No HP, atau key...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                DesignFilterChip("Semua ($totalCount)", filterTab == "ALL", onClick = { filterTab = "ALL" })
                            }
                            item {
                                DesignFilterChip("Demo ($demoActiveCount)", filterTab == "DEMO", onClick = { filterTab = "DEMO" })
                            }
                            item {
                                DesignFilterChip("Full Aktif ($fullActiveCount)", filterTab == "FULL", onClick = { filterTab = "FULL" })
                            }
                            item {
                                DesignFilterChip("Expired/Suspend ($lockedCount)", filterTab == "LOCKED", onClick = { filterTab = "LOCKED" })
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = AccentBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Memuat data pelanggan dari Supabase...", fontSize = 13.sp, color = Slate600)
                            }
                        }
                    } else if (filteredLicenses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (licenses.isEmpty()) "Belum ada pelanggan terdaftar di Supabase (tabel licenses)."
                                    else "Tidak ada lisensi yang cocok dengan pencarian.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500
                                )
                            }
                        }
                    } else {
                        items(filteredLicenses, key = { it.licenseKey.ifBlank { it.id } }) { license ->
                            val isSuspended = license.status.equals("SUSPENDED", ignoreCase = true)
                            val isFullActive = (license.planType.equals("FULL", ignoreCase = true) ||
                                license.status.equals("ACTIVE", ignoreCase = true)) && !isSuspended
                            val isDemoExpired = !isFullActive && !isSuspended &&
                                license.demoExpiresAtMillis > 0L && nowMillis >= license.demoExpiresAtMillis

                            val statusColor = when {
                                isSuspended -> Rose500
                                isFullActive -> Emerald700
                                isDemoExpired -> Amber500
                                else -> AccentBlue
                            }
                            val statusText = when {
                                isSuspended -> "SUSPENDED"
                                isFullActive -> "FULL AKTIF"
                                isDemoExpired -> "DEMO HABIS"
                                else -> "DEMO 3 HARI"
                            }

                            val expirySummary = when {
                                isSuspended -> "Dinonaktifkan oleh Admin Pusat"
                                isFullActive && license.expiresAtMillis > 0L -> {
                                    val diff = license.expiresAtMillis - nowMillis
                                    val daysLeft = ((diff.coerceAtLeast(0L) + 86_399_999L) / 86_400_000L)
                                    "Aktif s/d ${dateFormat.format(Date(license.expiresAtMillis))} (Sisa $daysLeft hari)"
                                }
                                isFullActive -> "Lisensi Penuh Permanen (Selamanya)"
                                license.demoExpiresAtMillis > 0L -> {
                                    val diff = license.demoExpiresAtMillis - nowMillis
                                    if (diff > 0L) {
                                        val daysLeft = (diff + 86_399_999L) / 86_400_000L
                                        "Demo s/d ${dateFormat.format(Date(license.demoExpiresAtMillis))} (Sisa $daysLeft hari)"
                                    } else {
                                        "Demo berakhir ${dateFormat.format(Date(license.demoExpiresAtMillis))}"
                                    }
                                }
                                else -> "Terdaftar Mode Demo"
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = BorderStroke(1.dp, BorderDefault),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Store, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = license.storeName.ifBlank { "Toko Pelanggan" },
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Slate100
                                            ) {
                                                Text(
                                                    text = license.planType.ifBlank { "DEMO" }.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate700,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusColor
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Pemilik: ${license.ownerName.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate800, fontWeight = FontWeight.Medium)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = license.ownerEmail.ifBlank { "-" },
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                    }
                                    if (license.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Slate500)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = license.phone,
                                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Kunci Lisensi: ${license.licenseKey}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(15.dp), tint = statusColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = expirySummary,
                                            style = MaterialTheme.typography.bodySmall.copy(color = statusColor, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (license.deviceId.isNotBlank()) "Device ID: ${license.deviceId}" else "Belum terikat perangkat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (license.deviceId.isNotBlank()) Slate600 else Amber500
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                managingLicense = license
                                                selectedManagePlan = when {
                                                    !isFullActive -> AdminLicensePlanOption.MONTHLY_30
                                                    license.expiresAtMillis > 0L -> AdminLicensePlanOption.MONTHLY_30
                                                    else -> AdminLicensePlanOption.LIFETIME
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFullActive && !isSuspended) AccentBlue else Emerald700,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("manage_license_btn_${license.licenseKey}")
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isFullActive && !isSuspended) "Perpanjang / Paket" else "Aktifkan / Pilih Paket",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.superAdminExtendDemo(license.licenseKey, days = 3) { _, msg ->
                                                    snackbarMessage = msg
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+3 Hari Demo", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.superAdminSetSuspended(license.licenseKey, suspended = !isSuspended) { _, msg ->
                                                    snackbarMessage = msg
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isSuspended) Emerald700 else Rose500
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                if (isSuspended) Icons.Default.CheckCircle else Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isSuspended) "Aktifkan" else "Suspend", fontSize = 12.sp)
                                        }

                                        if (license.deviceId.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.resetCustomerDeviceBinding(license.licenseKey) { _, msg ->
                                                        snackbarMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reset Device", fontSize = 12.sp)
                                            }
                                        }

                                        if (license.ownerEmail.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    val opened = launchPrefilledLicenseEmail(
                                                        context = context,
                                                        recipientEmail = license.ownerEmail,
                                                        ownerName = license.ownerName,
                                                        storeName = license.storeName,
                                                        licenseKey = license.licenseKey,
                                                        packageTitle = if (license.planType.equals("FULL", true)) "Lisensi Penuh KASLITE" else "Mode Demo",
                                                        amountRupiah = 0L,
                                                        expiresAtMillis = license.expiresAtMillis,
                                                        supportEmail = bankConfig.supportEmail.ifBlank { SUPER_ADMIN_EMAIL }
                                                    )
                                                    snackbarMessage = if (opened) {
                                                        "Membuka email untuk kirim kunci ${license.licenseKey} ke ${license.ownerEmail}"
                                                    } else {
                                                        "Aplikasi email tidak tersedia di perangkat ini."
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kirim Kunci Email", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.superAdminSendResetPassword(license.ownerEmail) { _, msg ->
                                                        snackbarMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reset Password", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    generatedKeyResult = null
                },
                title = { Text("Buat Lisensi Pelanggan Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (generatedKeyResult != null) {
                            Text("Lisensi berhasil dibuat di Supabase!", color = Emerald700, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = generatedKeyResult ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kunci Lisensi (${newPlanOption.shortLabel})") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Kirim kunci ini kepada pelanggan untuk aktivasi pada menu Pengaturan KASLITE.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                text = "Pilih Paket Masa Aktif:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AdminLicensePlanOption.entries.forEach { option ->
                                    DesignFilterChip(
                                        label = option.shortLabel,
                                        selected = newPlanOption == option,
                                        onClick = { newPlanOption = option }
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Sapphire50,
                                border = BorderStroke(1.dp, Sapphire200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Paket dipilih: ${newPlanOption.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Sapphire800, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            OutlinedTextField(
                                value = newStoreName,
                                onValueChange = { newStoreName = it },
                                label = { Text("Nama Toko Pelanggan *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOwnerName,
                                onValueChange = { newOwnerName = it },
                                label = { Text("Nama Pemilik *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOwnerEmail,
                                onValueChange = { newOwnerEmail = it },
                                label = { Text("Email Pemilik *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newPhone,
                                onValueChange = { newPhone = it },
                                label = { Text("No. HP / WhatsApp") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    if (generatedKeyResult != null) {
                        Button(onClick = {
                            showCreateDialog = false
                            generatedKeyResult = null
                            newStoreName = ""
                            newOwnerName = ""
                            newOwnerEmail = ""
                            newPhone = ""
                        }) {
                            Text("Tutup")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (newStoreName.isNotBlank() && newOwnerEmail.isNotBlank()) {
                                    viewModel.generateNewLicense(
                                        storeName = newStoreName,
                                        ownerName = newOwnerName,
                                        ownerEmail = newOwnerEmail,
                                        phone = newPhone,
                                        planOption = newPlanOption
                                    ) { key ->
                                        if (key != null) {
                                            generatedKeyResult = key
                                        } else {
                                            snackbarMessage = "Gagal membuat lisensi di Supabase."
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Buat Lisensi")
                        }
                    }
                },
                dismissButton = {
                    if (generatedKeyResult == null) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Batal")
                        }
                    }
                }
            )
        }

        managingLicense?.let { target ->
            val previewExpiryMillis = if (selectedManagePlan == AdminLicensePlanOption.DEMO_3) {
                maxOf(nowMillis, target.demoExpiresAtMillis) + 3L * 86_400_000L
            } else {
                SupabaseLicenseRepository.computeExtendedLicenseExpiryMillis(
                    nowMillis = nowMillis,
                    currentPlanType = target.planType,
                    currentStatus = target.status,
                    currentExpiresAtMillis = target.expiresAtMillis,
                    durationDays = selectedManagePlan.durationDays
                )
            }
            val previewExpiryText = when {
                selectedManagePlan == AdminLicensePlanOption.LIFETIME -> "PERMANEN (Selamanya)"
                else -> dateFormat.format(Date(previewExpiryMillis))
            }
            val isExtendingCurrent = selectedManagePlan != AdminLicensePlanOption.DEMO_3 &&
                selectedManagePlan.durationDays > 0 &&
                (target.planType.equals("FULL", true) || target.status.equals("ACTIVE", true)) &&
                !target.status.equals("SUSPENDED", true) &&
                target.expiresAtMillis > nowMillis

            AlertDialog(
                onDismissRequest = { managingLicense = null },
                title = {
                    Text(
                        text = "Kelola & Perpanjang Lisensi",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = target.storeName.ifBlank { "Toko Pelanggan" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            Text(
                                text = "${target.licenseKey} • ${target.ownerEmail}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                            )
                        }

                        Text(
                            text = "Pilih Paket Aktivasi / Perpanjangan:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminLicensePlanOption.entries.forEach { option ->
                                DesignFilterChip(
                                    label = option.shortLabel,
                                    selected = selectedManagePlan == option,
                                    onClick = { selectedManagePlan = option }
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald50,
                            border = BorderStroke(1.dp, Emerald200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = selectedManagePlan.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Emerald800)
                                )
                                Text(
                                    text = "Berlaku s/d: $previewExpiryText",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontWeight = FontWeight.SemiBold)
                                )
                                if (isExtendingCurrent) {
                                    Text(
                                        text = "Durasi ditambahkan ke sisa masa aktif saat ini agar tidak hangus.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Emerald700)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.superAdminApplyLicensePackage(
                                licenseKey = target.licenseKey,
                                option = selectedManagePlan,
                                extendExisting = true
                            ) { _, msg ->
                                snackbarMessage = msg
                            }
                            managingLicense = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        modifier = Modifier.testTag("confirm_manage_license_btn")
                    ) {
                        Text("Terapkan Paket", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { managingLicense = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun SummaryMetricChip(label: String, count: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Slate500)
            Text(
                count,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = accent)
            )
        }
    }
}
