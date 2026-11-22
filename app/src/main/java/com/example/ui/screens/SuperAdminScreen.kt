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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.LicenseDoc
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val licenses by viewModel.allLicenses.collectAsStateWithLifecycle()
    val superAdminError by viewModel.superAdminError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSuperAdminLoading.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("ALL") } // ALL, DEMO, FULL, LOCKED
    var showCreateDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var newOwnerName by remember { mutableStateOf("") }
    var newOwnerEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newPlanType by remember { mutableStateOf("FULL") }
    var generatedKeyResult by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

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
                    IconButton(onClick = { viewModel.loadSuperAdminLicenses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang Firestore")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (isSuperAdmin) {
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
                        "Menu rahasia ini khusus untuk Admin Pusat ($SUPER_ADMIN_EMAIL) melalui login Google Firebase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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
                                Text("Memuat data pelanggan dari Firestore...", fontSize = 13.sp, color = Slate600)
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
                                    text = if (licenses.isEmpty()) "Belum ada pelanggan terdaftar di Firestore /licenses."
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
                                isFullActive && license.expiresAtMillis > 0L ->
                                    "Aktif s/d ${dateFormat.format(Date(license.expiresAtMillis))}"
                                isFullActive -> "Lisensi Penuh Permanen"
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
                                        if (!isFullActive || isSuspended) {
                                            Button(
                                                onClick = {
                                                    viewModel.superAdminActivateFull(license.licenseKey) { _, msg ->
                                                        snackbarMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Aktifkan FULL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
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
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
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
                            Text("Lisensi berhasil dibuat di Firestore!", color = Emerald700, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = generatedKeyResult ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kunci Lisensi") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Kirim kunci ini kepada pelanggan untuk aktivasi pada menu Pengaturan KASLITE.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            DesignSegmentedTabs(
                                options = listOf("FULL" to "Lisensi FULL", "DEMO" to "Demo 3 Hari"),
                                selectedKey = newPlanType,
                                onSelect = { newPlanType = it }
                            )
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
                                label = { Text("Email Google Pemilik *") },
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
                                        planType = newPlanType
                                    ) { key ->
                                        if (key != null) {
                                            generatedKeyResult = key
                                        } else {
                                            snackbarMessage = "Gagal membuat lisensi di Firestore."
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
