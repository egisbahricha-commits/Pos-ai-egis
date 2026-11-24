package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.KasLiteBackupInspectResult
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.CashierDeviceEntity
import com.example.data.entity.PrinterEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.entity.UserAccountEntity
import com.example.firebase.LicenseAccessStatus
import com.example.hardware.HardwareDeviceDiscovery
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.PinPadDialog
import com.example.ui.components.PrinterManagerDialog
import com.example.ui.components.ScannerSettingsDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import com.example.ui.viewmodel.Screen
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val menuPermissions by viewModel.menuPermissions.collectAsStateWithLifecycle()
    val printers by viewModel.printers.collectAsStateWithLifecycle()
    val activePrinter by viewModel.activePrinter.collectAsStateWithLifecycle()
    val scannerSettings by viewModel.scannerSettings.collectAsStateWithLifecycle()
    val cashierDevices by viewModel.cashierDevices.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val kasLiteBackupState by viewModel.kasLiteBackupState.collectAsStateWithLifecycle()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportKasLiteBackup(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectKasLiteBackupUri(uri)
        }
    }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showStoreProfileDialog by remember { mutableStateOf(false) }
    var showPrintersDialog by remember { mutableStateOf(false) }
    var showScannerSettingsDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showLicenseKeyDialog by remember { mutableStateOf(false) }
    var showRefreshAuthDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<PosViewModel.AppUpdateInfo?>(null) }

    val hardwareSummary = remember(showPrintersDialog, showScannerSettingsDialog) {
        HardwareDeviceDiscovery.inspectHardware(context)
    }
    val isOwner = activeRole.equals("Owner", ignoreCase = true) || activeRole.equals("Admin", ignoreCase = true)
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val activeUserId by viewModel.activeUserId.collectAsStateWithLifecycle()
    val primaryPrinter = activePrinter
    val ppnPercent = (storeProfile?.ppnBp ?: 0) / 100.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Tampilan & Mode Gelap
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Tampilan & Tema Aplikasi",
                    style = MaterialTheme.typography.labelMedium.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Mode Warna Layar",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        "Pilih mode terang untuk kondisi toko cerah atau mode gelap agar nyaman di mata",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                    )
                    DesignSegmentedTabs(
                        options = listOf(
                            "SYSTEM" to "Sistem",
                            "LIGHT" to "Terang",
                            "DARK" to "Gelap"
                        ),
                        selectedKey = themeMode,
                        onSelect = { viewModel.setThemeMode(it) },
                        modifier = Modifier.testTag("settings_theme_selector")
                    )
                }
            }
        }

        // Section: Lisensi & Masa Aktif KASLITE
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Lisensi & Masa Aktif KASLITE",
                    style = MaterialTheme.typography.labelMedium.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        item {
            val daysLeft = ((licenseUiState.remainingMillis.coerceAtLeast(0L) + 86_399_999L) / 86_400_000L)
            val (badgeLabel, badgeType) = when (licenseUiState.accessStatus) {
                LicenseAccessStatus.FULL_ACTIVE -> "FULL AKTIF" to BadgeType.SUCCESS
                LicenseAccessStatus.DEMO_ACTIVE -> "DEMO ($daysLeft hr)" to BadgeType.INFO
                LicenseAccessStatus.DEMO_EXPIRED -> "DEMO HABIS" to BadgeType.DANGER
                LicenseAccessStatus.SUSPENDED -> "SUSPEND" to BadgeType.DANGER
                LicenseAccessStatus.DEVICE_MISMATCH -> "BEDA HP" to BadgeType.DANGER
                LicenseAccessStatus.UNREGISTERED -> "BELUM AKTIF" to BadgeType.WARNING
            }
            val registeredEmail = licenseUiState.ownerEmail.ifBlank { authUiState.lastEmail }.ifBlank { "-" }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuRow(
                        icon = Icons.Default.VerifiedUser,
                        title = when (licenseUiState.accessStatus) {
                            LicenseAccessStatus.FULL_ACTIVE -> "Lisensi Penuh Aktif"
                            LicenseAccessStatus.DEMO_ACTIVE -> "Mode Demo 3 Hari (Sisa $daysLeft hari)"
                            LicenseAccessStatus.DEMO_EXPIRED -> "Masa Demo Berakhir (Hanya Produk)"
                            LicenseAccessStatus.SUSPENDED -> "Lisensi Dinonaktifkan"
                            LicenseAccessStatus.DEVICE_MISMATCH -> "Terikat Perangkat Lain"
                            LicenseAccessStatus.UNREGISTERED -> "Status Lokal"
                        },
                        subtitle = "Email: $registeredEmail • Paket: ${licenseUiState.planType.ifBlank { "DEMO" }}\nAktivasi / bantuan hubungi: $SUPER_ADMIN_EMAIL",
                        badgeText = badgeLabel,
                        badgeType = badgeType,
                        onClick = { showLicenseKeyDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Refresh,
                        title = if (licenseUiState.isRefreshing) "Memeriksa ke server..." else "Cek Status Lisensi Online",
                        subtitle = "Segarkan status Demo/Full dari pusat lisensi Supabase KASLITE",
                        badgeText = if (licenseUiState.isRefreshing) "Cek..." else "Online",
                        badgeType = BadgeType.INFO,
                        onClick = {
                            if (viewModel.hasActiveCustomerSupabaseSession()) {
                                viewModel.refreshCustomerLicenseOnline(
                                    onRequireAuth = { showRefreshAuthDialog = true }
                                )
                            } else {
                                showRefreshAuthDialog = true
                            }
                        }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Key,
                        title = "Aktivasi Kunci Lisensi",
                        subtitle = "Masukkan kode lisensi penuh yang diberikan oleh Admin Pusat",
                        badgeText = "Masukkan Kode",
                        badgeType = BadgeType.NORMAL,
                        onClick = { showLicenseKeyDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Favorite,
                        title = "Donasi ke Pengembang",
                        subtitle = "Dukung pengembangan KASLITE (Rp 10rb/bln, Rp 50rb/thn, Rp 250rb Selamanya)",
                        badgeText = "Donasi",
                        badgeType = BadgeType.SUCCESS,
                        onClick = { viewModel.navigateTo(Screen.DONASI_PENGEMBANG) }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Keluar / Ganti Pengguna Login",
                        subtitle = "Kembali ke halaman login Username & PIN lokal KASLITE",
                        onClick = { viewModel.logoutToLoginScreen() }
                    )
                    if (isSuperAdmin) {
                        HorizontalDivider(color = BorderDefault)
                        SettingsMenuRow(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "Dashboard Admin Pusat (Super Admin)",
                            subtitle = "Kelola lisensi pelanggan, demo, & reset device binding",
                            badgeText = "ADMIN",
                            badgeType = BadgeType.WARNING,
                            onClick = { viewModel.navigateTo(Screen.SUPER_ADMIN) }
                        )
                    }
                }
            }
        }

        // Section: Backup & Restore Lengkap (.kaslite)
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Backup & Restore Data Lengkap (.kaslite)",
                    style = MaterialTheme.typography.labelMedium.copy(color = Emerald700, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuRow(
                        icon = Icons.Default.Download,
                        title = "Export Backup Lengkap (.kaslite)",
                        subtitle = "Simpan seluruh database lokal (produk, transaksi, stok, kas, pelanggan, user, foto) ke file .kaslite",
                        badgeText = if (kasLiteBackupState.isWorking) "Proses..." else "Export",
                        badgeType = BadgeType.SUCCESS,
                        onClick = {
                            if (!kasLiteBackupState.isWorking && !kasLiteBackupState.isRestoring) {
                                exportBackupLauncher.launch("Backup_KASLITE.kaslite")
                            }
                        }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Share,
                        title = "Bagikan Backup (.kaslite)",
                        subtitle = "Kirim arsip backup lengkap ke WhatsApp, Drive, atau Telegram agar data aman",
                        onClick = {
                            if (!kasLiteBackupState.isWorking && !kasLiteBackupState.isRestoring) {
                                viewModel.shareKasLiteBackup { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Backup Lengkap KASLITE (.kaslite)")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Backup KASLITE"))
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.UploadFile,
                        title = if (kasLiteBackupState.isInspecting) "Membaca file .kaslite..." else "Import / Restore Backup (.kaslite)",
                        subtitle = kasLiteBackupState.lastRestoreSummary?.let { s ->
                            "Terakhir dipulihkan: ${s.restoredRecords} rekaman data & ${s.restoredPhotos} foto"
                        } ?: "Pulihkan seluruh data toko dan foto produk dari arsip .kaslite",
                        badgeText = if (kasLiteBackupState.isInspecting) "Proses..." else "Restore",
                        badgeType = BadgeType.INFO,
                        onClick = {
                            if (!kasLiteBackupState.isInspecting && !kasLiteBackupState.isRestoring) {
                                restoreBackupLauncher.launch(arrayOf("*/*"))
                            }
                        }
                    )
                }
            }
        }

        // Section: Toko & Perangkat
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.Store, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Toko & Perangkat",
                    style = MaterialTheme.typography.labelMedium.copy(color = Slate500, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuRow(
                        icon = Icons.Default.Store,
                        title = storeProfile?.name?.ifBlank { "KASLITE" } ?: "KASLITE",
                        subtitle = "Tipe: ${storeProfile?.businessType ?: "keduanya"} • PPN ${String.format("%.1f", ppnPercent)}%",
                        badgeText = "Edit Profil",
                        badgeType = BadgeType.INFO,
                        onClick = { showStoreProfileDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Print,
                        title = "Printer struk & stiker (${printers.size})",
                        subtitle = if (primaryPrinter != null) {
                            "Utama: ${primaryPrinter.name} (${primaryPrinter.widthMm}mm, ${primaryPrinter.language.uppercase()})"
                        } else if (hardwareSummary.printers.isNotEmpty()) {
                            "${hardwareSummary.printers.size} printer terdeteksi di perangkat — ketuk untuk pilih"
                        } else {
                            "Deteksi otomatis printer Bluetooth / USB OTG"
                        },
                        badgeText = if (primaryPrinter != null) "Aktif" else "Atur",
                        badgeType = if (primaryPrinter != null) BadgeType.SUCCESS else BadgeType.INFO,
                        onClick = { showPrintersDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.QrCodeScanner,
                        title = "Pengaturan Scan Barcode",
                        subtitle = buildString {
                            val modeLabel = when (scannerSettings.preferredMode) {
                                "KAMERA" -> "Kamera"
                                "HARDWARE" -> "Alat USB/BT"
                                else -> "Kamera + Alat"
                            }
                            append("Mode: $modeLabel")
                            if (scannerSettings.selectedScannerName.isNotBlank()) {
                                append(" • ${scannerSettings.selectedScannerName}")
                            } else if (hardwareSummary.scanners.isNotEmpty()) {
                                append(" • ${hardwareSummary.scanners.size} alat terdeteksi")
                            }
                        },
                        badgeText = if (hardwareSummary.scanners.any { it.isConnectedNow }) "Siap" else "Atur",
                        badgeType = if (hardwareSummary.scanners.any { it.isConnectedNow }) BadgeType.SUCCESS else BadgeType.INFO,
                        onClick = { showScannerSettingsDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Devices,
                        title = "Perangkat kasir (${cashierDevices.size})",
                        subtitle = cashierDevices.firstOrNull()?.let { "${it.name} (${it.role})" } ?: "Kelola terminal utama / kasir",
                        onClick = { showDevicesDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Lock,
                        title = "Verifikasi PIN Kasir",
                        subtitle = "Uji PIN 6-digit dengan hash SHA-256",
                        onClick = { showPinDialog = true }
                    )
                }
            }
        }

        // Section: Pengguna & Izin Menu
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Pengguna & Izin Menu ($activeUserName)",
                        style = MaterialTheme.typography.labelMedium.copy(color = Slate500, fontWeight = FontWeight.SemiBold)
                    )
                }
                if (isOwner) {
                    TextButton(
                        onClick = {
                            userToEdit = null
                            showAddUserDialog = true
                        },
                        modifier = Modifier.testTag("settings_add_user_button")
                    ) {
                        Text("+ Tambah User", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    DesignStatusBadge(text = "Khusus Admin Utama", type = BadgeType.WARNING)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (userAccounts.isEmpty()) {
                        Text(
                            text = "Belum ada pengguna lokal terdaftar.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    userAccounts.forEachIndexed { index, user ->
                        val isSelected = activeUserId == user.id || activeUserName.equals(user.name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) AccentBlue else Slate900
                                    )
                                )
                                Text(
                                    text = "@${user.username} • ${user.role.uppercase()}" +
                                        if (user.isActive) "" else " • NONAKTIF",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (user.isActive) Slate500 else Rose500,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isSelected) {
                                    DesignStatusBadge(text = "Login Aktif", type = BadgeType.INFO)
                                } else if (!user.isActive) {
                                    DesignStatusBadge(text = "Nonaktif", type = BadgeType.DANGER)
                                }
                                if (isOwner) {
                                    IconButton(
                                        onClick = {
                                            userToEdit = user
                                            showAddUserDialog = true
                                        },
                                        modifier = Modifier.testTag("settings_edit_user_${user.username}")
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = "Edit Akun & Izin Menu", tint = Slate600, modifier = Modifier.size(18.dp))
                                    }
                                    if (!isSelected) {
                                        IconButton(
                                            onClick = { viewModel.deleteUserAccount(user) },
                                            modifier = Modifier.testTag("settings_delete_user_${user.username}")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Akun", tint = Rose500, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                        if (index < userAccounts.lastIndex) {
                            HorizontalDivider(color = BorderDefault)
                        }
                    }
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Ganti Akun / Keluar Sesi",
                        subtitle = "Kembali ke layar login untuk masuk menggunakan akun lain (wajib PIN)",
                        onClick = { viewModel.logoutToLoginScreen() }
                    )
                }
            }
        }

        // Section: Reset Database & Info
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = (storeProfile?.name?.ifBlank { "KASLITE POS" } ?: "KASLITE POS") + " (Versi ${com.example.BuildConfig.VERSION_NAME})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = "Mode Produksi • Sinkronisasi Cloud Supabase & Update Otomatis GitHub",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.checkForGitHubAppUpdate { hasUpdate, msg, info ->
                                if (hasUpdate && info != null) {
                                    updateDialogInfo = info
                                    showUpdateDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_check_update_btn")
                    ) {
                        val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memeriksa Pembaruan...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Periksa Pembaruan Aplikasi (GitHub)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isOwner) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_clean_reset_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Semua Data Toko (Mulai Bersih)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Bantuan & Laporan Bug
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Bantuan & Laporan Bug",
                    style = MaterialTheme.typography.labelMedium.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsMenuRow(
                    icon = Icons.Default.BugReport,
                    title = "Laporan Bug Aplikasi",
                    subtitle = "Laporkan kendala, error printer/scanner, atau bug agar dapat diperbaiki pengembang",
                    badgeText = "Laporkan",
                    badgeType = BadgeType.INFO,
                    onClick = { viewModel.navigateTo(Screen.LAPOR_BUG) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showPinDialog) {
        PinPadDialog(
            title = "Verifikasi PIN Keamanan",
            onVerifyPin = { viewModel.verifyPin(it) },
            onSuccess = { showPinDialog = false },
            onDismiss = { showPinDialog = false }
        )
    }

    if (showResetPinDialog) {
        PinPadDialog(
            title = "Masukkan PIN Admin Utama untuk Reset",
            onVerifyPin = { viewModel.verifyPin(it) },
            onSuccess = {
                showResetPinDialog = false
                viewModel.resetDatabaseToClean()
            },
            onDismiss = { showResetPinDialog = false }
        )
    }

    if (showStoreProfileDialog) {
        val current = storeProfile ?: StoreProfileEntity()
        var name by remember(current) { mutableStateOf(current.name) }
        var address by remember(current) { mutableStateOf(current.address) }
        var phone by remember(current) { mutableStateOf(current.phone) }
        var businessType by remember(current) { mutableStateOf(current.businessType) }
        var ppnPercentText by remember(current) { mutableStateOf((current.ppnBp / 100.0).toString()) }
        var footer by remember(current) { mutableStateOf(current.receiptFooter) }

        Dialog(onDismissRequest = { showStoreProfileDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Profil Toko & PPN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) }
                    item { DesignLabeledTextField("Nama Toko", name, { name = it }) }
                    item { DesignLabeledTextField("Alamat", address, { address = it }) }
                    item { DesignLabeledTextField("No. Telepon", phone, { phone = it }) }
                    item {
                        Text("Tipe Bisnis", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate700))
                        Spacer(modifier = Modifier.height(4.dp))
                        DesignSegmentedTabs(
                            options = listOf("retail" to "Retail", "grosir" to "Grosir", "keduanya" to "Keduanya"),
                            selectedKey = businessType,
                            onSelect = { businessType = it }
                        )
                    }
                    item {
                        DesignLabeledTextField(
                            label = "PPN (%) — mis. 11 atau 0",
                            value = ppnPercentText,
                            onValueChange = { raw ->
                                ppnPercentText = raw.filter { ch -> ch.isDigit() || ch == '.' }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    item { DesignLabeledTextField("Footer Struk", footer, { footer = it }) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showStoreProfileDialog = false }) { Text("Batal", color = Slate600) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val pct = ppnPercentText.toDoubleOrNull() ?: 0.0
                                    val bp = (pct * 100).toInt().coerceIn(0, 10000)
                                    viewModel.saveStoreProfile(
                                        current.copy(
                                            name = name.ifBlank { "Toko Saya" },
                                            address = address,
                                            phone = phone,
                                            businessType = businessType,
                                            ppnBp = bp,
                                            receiptFooter = footer,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                    showStoreProfileDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Simpan", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        val editing = userToEdit
        val allMenuKeys = listOf("POS_KASIR", "SCAN_BARCODE", "RIWAYAT_TRANSAKSI", "PRODUK", "MANAJEMEN_PRODUK", "PEMBELIAN", "STOK_INVENTARIS", "MULTI_KAS", "TARIF_ADMIN", "KEUANGAN", "PENGELUARAN", "UTANG_PIUTANG", "LAPORAN", "AI_CHAT", "WHATSAPP", "KONTAK")
        var name by remember(editing?.id) { mutableStateOf(editing?.name ?: "") }
        var username by remember(editing?.id) { mutableStateOf(editing?.username ?: "") }
        var role by remember(editing?.id) { mutableStateOf(editing?.role ?: "kasir") }
        var isUserActive by remember(editing?.id) { mutableStateOf(editing?.isActive ?: true) }
        var pinText by remember(editing?.id) { mutableStateOf("") }
        var formError by remember(editing?.id) { mutableStateOf<String?>(null) }
        val permMap = remember(editing?.id, menuPermissions) {
            mutableStateMapOf<String, Boolean>().apply {
                allMenuKeys.forEach { key ->
                    val existing = editing?.let { u -> menuPermissions.find { it.userId == u.id && it.menuKey == key }?.allowed }
                    put(key, existing ?: (role == "owner" || key in listOf("POS_KASIR", "SCAN_BARCODE", "RIWAYAT_TRANSAKSI", "PRODUK", "MANAJEMEN_PRODUK", "STOK_INVENTARIS", "TARIF_ADMIN", "KONTAK")))
                }
            }
        }

        Dialog(onDismissRequest = { showAddUserDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            if (editing == null) "Tambah Pengguna & Izin Menu" else "Edit ${editing.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    item { DesignLabeledTextField("Nama Lengkap", name, { name = it; formError = null }) }
                    item {
                        DesignLabeledTextField(
                            "Username (tanpa spasi, min. 3 karakter)",
                            username,
                            { username = it.filter { ch -> !ch.isWhitespace() }; formError = null }
                        )
                    }
                    item {
                        DesignSegmentedTabs(
                            options = listOf("owner" to "Owner", "kasir" to "Kasir"),
                            selectedKey = role,
                            onSelect = { role = it; formError = null }
                        )
                    }
                    item {
                        DesignLabeledTextField(
                            label = if (editing == null) "PIN Login (Wajib 6 digit angka)" else "PIN Baru (opsional, 6 digit jika ingin ganti)",
                            value = pinText,
                            onValueChange = { pinText = it.filter { ch -> ch.isDigit() }.take(6); formError = null },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status Akun Aktif", fontSize = 13.sp, color = Slate800, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isUserActive,
                                onCheckedChange = { isUserActive = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
                            )
                        }
                    }
                    if (role != "owner") {
                        item {
                            Text("Izin Akses Menu", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800))
                        }
                        allMenuKeys.forEach { key ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { permMap[key] = !(permMap[key] ?: false) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key.replace("_", " "), fontSize = 12.sp, color = Slate800)
                                    Checkbox(checked = permMap[key] ?: false, onCheckedChange = { permMap[key] = it })
                                }
                            }
                        }
                    }
                    if (formError != null) {
                        item {
                            Text(
                                text = formError!!,
                                color = Rose500,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddUserDialog = false }) { Text("Batal", color = Slate600) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cleanName = name.trim()
                                    val cleanUser = username.trim().lowercase()
                                    val pinValid = if (editing == null) {
                                        pinText.length == 6
                                    } else {
                                        pinText.isEmpty() || pinText.length == 6
                                    }
                                    val duplicateUser = userAccounts.any {
                                        it.id != editing?.id && it.username.trim().equals(cleanUser, ignoreCase = true)
                                    }
                                    when {
                                        cleanName.isEmpty() -> formError = "Nama lengkap wajib diisi"
                                        cleanUser.length < 3 -> formError = "Username minimal 3 karakter tanpa spasi"
                                        duplicateUser -> formError = "Username \"$cleanUser\" sudah digunakan"
                                        !pinValid -> formError = "PIN harus tepat 6 digit angka"
                                        else -> {
                                            val hash = if (pinText.length == 6) {
                                                GeraiUnaDatabase.hashPin(pinText)
                                            } else {
                                                editing!!.pinHash
                                            }
                                            val finalUser = (editing ?: UserAccountEntity(
                                                name = cleanName,
                                                username = cleanUser,
                                                role = role,
                                                pinHash = hash,
                                                isActive = isUserActive
                                            )).copy(
                                                name = cleanName,
                                                username = cleanUser,
                                                role = role,
                                                pinHash = hash,
                                                isActive = isUserActive,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            viewModel.saveUserAccount(finalUser, permMap.toMap())
                                            showAddUserDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("settings_save_user_button")
                            ) { Text("Simpan", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showPrintersDialog) {
        PrinterManagerDialog(
            printers = printers,
            activePrinterId = primaryPrinter?.id ?: 0L,
            onSelectPrimaryPrinter = { printerId -> viewModel.selectPreferredPrinter(printerId) },
            onSavePrinter = { printer, setAsPrimary -> viewModel.savePrinter(printer, setAsPrimary) },
            onDeletePrinter = { printer -> viewModel.deletePrinter(printer) },
            onTestPrint = { printer, callback -> viewModel.printTestReceipt(context, printer, callback) },
            onDismiss = { showPrintersDialog = false }
        )
    }

    if (showScannerSettingsDialog) {
        ScannerSettingsDialog(
            currentSettings = scannerSettings,
            onSaveSettings = { updated -> viewModel.updateScannerSettings(updated) },
            onDismiss = { showScannerSettingsDialog = false }
        )
    }

    if (showDevicesDialog) {
        var devName by remember { mutableStateOf("") }
        var devRole by remember { mutableStateOf("kasir") }
        Dialog(onDismissRequest = { showDevicesDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Perangkat Kasir", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) }
                    cashierDevices.forEach { d ->
                        item {
                            Surface(shape = RoundedCornerShape(10.dp), color = BgCanvas, border = BorderStroke(1.dp, BorderDefault), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(d.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Peran: ${d.role.uppercase()}", fontSize = 11.sp, color = Slate600)
                                    }
                                    IconButton(onClick = { viewModel.deleteCashierDevice(d) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Rose500)
                                    }
                                }
                            }
                        }
                    }
                    item { HorizontalDivider(color = BorderDefault) }
                    item { DesignLabeledTextField("Nama Perangkat", devName, { devName = it }) }
                    item {
                        DesignSegmentedTabs(
                            options = listOf("utama" to "Utama", "kasir" to "Kasir"),
                            selectedKey = devRole,
                            onSelect = { devRole = it }
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showDevicesDialog = false }) { Text("Tutup") }
                            Button(
                                onClick = {
                                    if (devName.isNotBlank()) {
                                        viewModel.saveCashierDevice(CashierDeviceEntity(name = devName.trim(), role = devRole))
                                        devName = ""
                                    }
                                },
                                enabled = devName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Tambah Perangkat", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showRefreshAuthDialog) {
        var authEmail by remember { mutableStateOf(licenseUiState.ownerEmail.ifBlank { authUiState.lastEmail }) }
        var authPassword by remember { mutableStateOf("") }
        var showPass by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }
        var localInfo by remember { mutableStateOf<String?>(null) }
        var isBusy by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isBusy) showRefreshAuthDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cek Status Lisensi Supabase", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text(
                        "Masukkan Email & Password akun KASLITE Anda untuk memperbarui status lisensi dari Supabase.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    OutlinedTextField(
                        value = authEmail,
                        onValueChange = { authEmail = it.trim(); localError = null },
                        label = { Text("Email Terdaftar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = authPassword,
                        onValueChange = { authPassword = it; localError = null },
                        label = { Text("Password Akun") },
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPass) "Sembunyikan password" else "Lihat password"
                                )
                            }
                        },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            enabled = !isBusy,
                            onClick = {
                                isBusy = true
                                localError = null
                                viewModel.sendSupabasePasswordReset(authEmail) { ok, msg ->
                                    isBusy = false
                                    if (ok) localInfo = msg else localError = msg
                                }
                            }
                        ) { Text("Lupa Password?") }
                    }
                    if (!localInfo.isNullOrBlank()) {
                        Text(localInfo ?: "", color = AccentBlue, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!localError.isNullOrBlank()) {
                        Text(localError ?: "", color = Rose500, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showRefreshAuthDialog = false },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Batal", color = Slate700) }
                        Button(
                            onClick = {
                                if (authEmail.isBlank() || authPassword.isBlank()) {
                                    localError = "Email & password wajib diisi."
                                    return@Button
                                }
                                isBusy = true
                                localError = null
                                viewModel.refreshCustomerLicenseOnline(authEmail, authPassword) { ok, msg ->
                                    isBusy = false
                                    if (ok) showRefreshAuthDialog = false else localError = msg
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Cek Lisensi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLicenseKeyDialog) {
        var keyInput by remember { mutableStateOf("") }
        val hasSession = viewModel.hasActiveCustomerSupabaseSession()
        var authEmail by remember { mutableStateOf(licenseUiState.ownerEmail.ifBlank { authUiState.lastEmail }) }
        var authPassword by remember { mutableStateOf("") }
        var showPass by remember { mutableStateOf(false) }
        var localDialogError by remember { mutableStateOf<String?>(null) }
        var isSubmittingKey by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isSubmittingKey) showLicenseKeyDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aktivasi Kunci Lisensi KASLITE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text(
                        "Masukkan kunci lisensi dari Admin Pusat ($SUPER_ADMIN_EMAIL) untuk membuka seluruh menu secara penuh.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it; localDialogError = null },
                        label = { Text("Kunci Lisensi") },
                        placeholder = { Text("Contoh: KASLITE-XXXXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!hasSession) {
                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it.trim(); localDialogError = null },
                            label = { Text("Email Akun KASLITE") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { authPassword = it; localDialogError = null },
                            label = { Text("Password Akun") },
                            trailingIcon = {
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(
                                        if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPass) "Sembunyikan password" else "Lihat password"
                                    )
                                }
                            },
                            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!localDialogError.isNullOrBlank()) {
                        Text(localDialogError ?: "", color = Rose500, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showLicenseKeyDialog = false },
                            enabled = !isSubmittingKey,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", color = Slate700)
                        }
                        Button(
                            onClick = {
                                if (keyInput.isBlank()) {
                                    localDialogError = "Kunci lisensi tidak boleh kosong."
                                    return@Button
                                }
                                if (!hasSession && (authEmail.isBlank() || authPassword.isBlank())) {
                                    localDialogError = "Masukkan Email & Password akun KASLITE Anda terlebih dahulu."
                                    return@Button
                                }
                                isSubmittingKey = true
                                viewModel.activateCustomerLicenseKey(
                                    licenseKey = keyInput,
                                    email = if (hasSession) null else authEmail,
                                    password = if (hasSession) null else authPassword
                                ) { ok, msg ->
                                    isSubmittingKey = false
                                    if (ok) showLicenseKeyDialog = false else localDialogError = msg
                                }
                            },
                            enabled = !isSubmittingKey,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSubmittingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Aktifkan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Reset Bersih Database?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Seluruh produk, transaksi, kontak, kas, dan akun pengguna akan dihapus untuk memulai toko bersih dari awal.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = false },
                            border = BorderStroke(1.dp, BorderInput),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", color = Slate700)
                        }
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                showResetPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lanjut Verifikasi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (kasLiteBackupState.isInspecting) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = AccentBlue)
                    Column {
                        Text(
                            "Menganalisis Backup .kaslite",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            "Membaca manifest, seluruh data tabel, & foto...",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }
    }

    kasLiteBackupState.inspectResult?.let { inspect ->
        KasLiteRestorePreviewDialog(
            inspect = inspect,
            isRestoring = kasLiteBackupState.isRestoring,
            errorMessage = kasLiteBackupState.errorMessage,
            onConfirm = { replaceExisting -> viewModel.confirmKasLiteRestore(replaceExisting) },
            onDismiss = { if (!kasLiteBackupState.isRestoring) viewModel.dismissKasLiteRestorePreview() }
        )
    }

    if (showUpdateDialog && updateDialogInfo != null) {
        val info = updateDialogInfo!!
        Dialog(onDismissRequest = { showUpdateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentBlueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = AccentBlue)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Pembaruan KASLITE Tersedia!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            Text(
                                "Versi Terbaru: ${info.latestVersionName} (Saat ini: ${com.example.BuildConfig.VERSION_NAME})",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sapphire50,
                        border = BorderStroke(1.dp, Sapphire200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Catatan Rilis Pembaruan:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                            )
                            Text(
                                info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 12.sp)
                            )
                        }
                    }

                    Text(
                        "Pembaruan ini akan mendownload file APK dari GitHub dan langsung menginstalnya tanpa menghapus data kasir Anda.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showUpdateDialog = false },
                            border = BorderStroke(1.dp, BorderInput),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nanti Saja", color = Slate700)
                        }
                        Button(
                            onClick = {
                                showUpdateDialog = false
                                viewModel.downloadAndInstallUpdate(context, info)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kaslite_download_install_btn")
                        ) {
                            Text("Download & Install", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (kasLiteBackupState.inspectResult == null && kasLiteBackupState.errorMessage != null) {
        Dialog(onDismissRequest = { viewModel.dismissKasLiteRestorePreview() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "File Backup Tidak Valid",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Rose500)
                    )
                    Text(
                        kasLiteBackupState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                    )
                    Button(
                        onClick = { viewModel.dismissKasLiteRestorePreview() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KasLiteRestorePreviewDialog(
    inspect: KasLiteBackupInspectResult,
    isRestoring: Boolean,
    errorMessage: String?,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(true) }
    val dateLabel = remember(inspect.createdAtMillis) {
        if (inspect.createdAtMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(inspect.createdAtMillis))
        } else "-"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = AccentBlue)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pratinjau Backup KASLITE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            inspect.fileName,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Sapphire50,
                    border = BorderStroke(1.dp, Sapphire200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Toko: ${inspect.storeName.ifBlank { "KASLITE" }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                        )
                        Text(
                            "Dibuat: $dateLabel • Versi ${inspect.appVersion} • ${inspect.totalRecords} total rekaman",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KasLiteCountPill("Produk", "${inspect.productCount}", Modifier.weight(1f))
                    KasLiteCountPill("Penjualan", "${inspect.saleCount}", Modifier.weight(1f))
                    KasLiteCountPill("Pembelian", "${inspect.purchaseCount}", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KasLiteCountPill("Pelanggan", "${inspect.customerCount}", Modifier.weight(1f))
                    KasLiteCountPill("Akun Kas", "${inspect.cashAccountCount}", Modifier.weight(1f))
                    KasLiteCountPill("User/Foto", "${inspect.userAccountCount}/${inspect.photoCount}", Modifier.weight(1f))
                }

                HorizontalDivider(color = BorderDefault)

                Text(
                    "Metode Pemulihan Data",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                )

                Surface(
                    onClick = { replaceExisting = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (replaceExisting) AccentBlueSoft.copy(alpha = 0.45f) else Slate50,
                    border = BorderStroke(1.dp, if (replaceExisting) AccentBlue else BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                        Column {
                            Text("Pulihkan Penuh (Timpa Data Saat Ini)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            Text("Kembalikan 100% data persis seperti arsip backup beserta seluruh foto & profil toko", fontSize = 10.sp, color = Slate600)
                        }
                    }
                }

                Surface(
                    onClick = { replaceExisting = false },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!replaceExisting) AccentBlueSoft.copy(alpha = 0.45f) else Slate50,
                    border = BorderStroke(1.dp, if (!replaceExisting) AccentBlue else BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !replaceExisting, onClick = { replaceExisting = false })
                        Column {
                            Text("Gabung ke Data Saat Ini (Merge)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            Text("Tambahkan data dari backup tanpa menghapus data yang sedang aktif di HP", fontSize = 10.sp, color = Slate600)
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(color = Rose500, fontWeight = FontWeight.SemiBold)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isRestoring,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Slate700)
                    }
                    Button(
                        onClick = { onConfirm(replaceExisting) },
                        enabled = !isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kaslite_confirm_restore_btn")
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Memulihkan...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text("Mulai Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KasLiteCountPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate50,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Slate500)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular()
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeType: BadgeType = BadgeType.NORMAL,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Slate700, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp)
            )
        }
        if (badgeText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            DesignStatusBadge(text = badgeText, type = badgeType)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Slate400, modifier = Modifier.size(13.dp))
    }
}
