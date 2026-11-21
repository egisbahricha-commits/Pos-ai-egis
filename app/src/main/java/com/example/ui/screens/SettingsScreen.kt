package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.KasGoImportOptions
import com.example.data.backup.KasGoInspectResult
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.CashierDeviceEntity
import com.example.data.entity.PrinterEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.entity.UserAccountEntity
import com.example.firebase.SyncMode
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.PinPadDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SettingsScreen(viewModel: PosViewModel) {
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val menuPermissions by viewModel.menuPermissions.collectAsStateWithLifecycle()
    val printers by viewModel.printers.collectAsStateWithLifecycle()
    val cashierDevices by viewModel.cashierDevices.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val kasGoImportState by viewModel.kasGoImportState.collectAsStateWithLifecycle()

    val kasGoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectKasGoBackupUri(uri)
        }
    }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showStoreProfileDialog by remember { mutableStateOf(false) }
    var showPrintersDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showBackupInfoDialog by remember { mutableStateOf(false) }

    val isOwner = activeRole.equals("Owner", ignoreCase = true) || activeRole.equals("Admin", ignoreCase = true)
    val activeUserId by viewModel.activeUserId.collectAsStateWithLifecycle()
    val primaryPrinter = printers.firstOrNull()
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

        // Section: Firebase Cloud & Multi-Perangkat
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Firebase Cloud & Login Multi-Perangkat",
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
                Column {
                    SettingsMenuRow(
                        icon = Icons.Default.CloudDone,
                        title = if (authUiState.isCloudSignedIn) authUiState.userEmail else "Belum Login Cloud (Mode Offline)",
                        subtitle = if (authUiState.isCloudSignedIn)
                            "Perangkat: ${authUiState.deviceName} • Ketuk untuk pantau arus kas Firestore"
                        else
                            "Masuk dengan Google (Firebase) untuk sync & pantau dari perangkat lain",
                        badgeText = if (authUiState.isCloudSignedIn) "Terhubung" else "Login",
                        badgeType = if (authUiState.isCloudSignedIn) BadgeType.SUCCESS else BadgeType.WARNING,
                        onClick = { viewModel.navigateTo(Screen.CLOUD_MONITOR) }
                    )
                    if (authUiState.isCloudSignedIn) {
                        HorizontalDivider(color = BorderDefault)
                        SettingsMenuRow(
                            icon = Icons.Default.Sync,
                            title = "Sinkronkan Data Sekarang",
                            subtitle = authUiState.lastSyncMessage.ifBlank { "Kirim & tarik saldo kas, mutasi, dan penjualan" },
                            onClick = { viewModel.syncNowWithFirebase(SyncMode.TWO_WAY) }
                        )
                    }
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = if (authUiState.isCloudSignedIn) "Keluar / Ganti Akun Google" else "Buka Halaman Login Firebase",
                        subtitle = "Kembali ke halaman login untuk masuk akun Google atau ganti kasir",
                        onClick = { viewModel.logoutToLoginScreen() }
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
                        title = storeProfile?.name?.ifBlank { "Toko Saya" } ?: "Toko Saya",
                        subtitle = "Tipe: ${storeProfile?.businessType ?: "keduanya"} • PPN ${String.format("%.1f", ppnPercent)}%",
                        badgeText = "Edit Profil",
                        badgeType = BadgeType.INFO,
                        onClick = { showStoreProfileDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.Print,
                        title = "Printer struk & stiker (${printers.size})",
                        subtitle = if (primaryPrinter != null) "${primaryPrinter.name} (${primaryPrinter.widthMm}mm, ${primaryPrinter.language.uppercase()})" else "Tambah printer Bluetooth",
                        onClick = { showPrintersDialog = true }
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
                        icon = Icons.Default.Backup,
                        title = "Cadangan data SQLite",
                        subtitle = "Database lokal aktif • Ketuk untuk info & impor .kasgo",
                        badgeText = "Aktif",
                        badgeType = BadgeType.SUCCESS,
                        onClick = { showBackupInfoDialog = true }
                    )
                    HorizontalDivider(color = BorderDefault)
                    SettingsMenuRow(
                        icon = Icons.Default.UploadFile,
                        title = if (kasGoImportState.isInspecting) "Membaca file .kasgo..." else "Import Backup KasGo (.kasgo)",
                        subtitle = kasGoImportState.lastImportSummary?.let { s ->
                            "Terakhir impor: ${s.productsImported} produk, ${s.salesImported} transaksi, ${s.photosCopied} foto"
                        } ?: "Pulihkan produk, stok, pelanggan, foto & transaksi dari file .kasgo",
                        badgeText = if (kasGoImportState.isInspecting) "Proses..." else "Pilih File",
                        badgeType = BadgeType.INFO,
                        onClick = {
                            if (!kasGoImportState.isInspecting && !kasGoImportState.isImporting) {
                                kasGoPickerLauncher.launch(arrayOf("*/*"))
                            }
                        }
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
                        text = storeProfile?.name?.ifBlank { "Sistem Kasir POS" } ?: "Sistem Kasir POS",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = "Mode Produksi • Penyimpanan Lokal SQLite & Firebase Opsional",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                    if (isOwner) {
                        Spacer(modifier = Modifier.height(12.dp))
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
        var newPrinterName by remember { mutableStateOf("") }
        var btAddr by remember { mutableStateOf("") }
        var printerType by remember { mutableStateOf("struk") }
        var widthMm by remember { mutableStateOf(58) }
        var autoCut by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showPrintersDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Printer Bluetooth", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) }
                    printers.forEach { p ->
                        item {
                            Surface(shape = RoundedCornerShape(10.dp), color = BgCanvas, border = BorderStroke(1.dp, BorderDefault), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${p.bluetoothAddress.ifBlank { "Belum diatur" }} • ${p.type} • ${p.widthMm}mm • ${p.language}", fontSize = 11.sp, color = Slate600)
                                    }
                                    IconButton(onClick = { viewModel.deletePrinter(p) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Rose500)
                                    }
                                }
                            }
                        }
                    }
                    item { HorizontalDivider(color = BorderDefault) }
                    item { DesignLabeledTextField("Nama Printer Baru", newPrinterName, { newPrinterName = it }) }
                    item { DesignLabeledTextField("Alamat Bluetooth / MAC", btAddr, { btAddr = it }) }
                    item {
                        DesignSegmentedTabs(
                            options = listOf("struk" to "Struk (ESC/POS)", "stiker" to "Stiker (TSPL)"),
                            selectedKey = printerType,
                            onSelect = { printerType = it }
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(40, 58, 80).forEach { w ->
                                DesignFilterChip(label = "${w}mm", selected = widthMm == w, onClick = { widthMm = w })
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Auto Cut Kertas", fontSize = 13.sp, color = Slate800)
                            Switch(checked = autoCut, onCheckedChange = { autoCut = it }, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showPrintersDialog = false }) { Text("Tutup") }
                            Button(
                                onClick = {
                                    if (newPrinterName.isNotBlank()) {
                                        viewModel.savePrinter(
                                            PrinterEntity(
                                                name = newPrinterName.trim(),
                                                bluetoothAddress = btAddr.trim(),
                                                type = printerType,
                                                language = if (printerType == "stiker") "tspl" else "escpos",
                                                widthMm = widthMm,
                                                autoCut = autoCut
                                            )
                                        )
                                        newPrinterName = ""
                                        btAddr = ""
                                    }
                                },
                                enabled = newPrinterName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Printer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
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

    if (showBackupInfoDialog) {
        Dialog(onDismissRequest = { showBackupInfoDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Penyimpanan SQLite & Backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        "Seluruh data produk, stok, pelanggan, transaksi, dan multi-kas tersimpan lokal di SQLite perangkat (gerai_una_database) serta dapat disinkronkan ke Firebase Cloud.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showBackupInfoDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tutup", color = Slate700)
                        }
                        Button(
                            onClick = {
                                showBackupInfoDialog = false
                                if (!kasGoImportState.isInspecting && !kasGoImportState.isImporting) {
                                    kasGoPickerLauncher.launch(arrayOf("*/*"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import .kasgo", fontWeight = FontWeight.Bold)
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

    if (kasGoImportState.isInspecting) {
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
                            "Menganalisis Backup .kasgo",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            "Membaca database.db, prefs, & folder photos...",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }
    }

    kasGoImportState.inspectResult?.let { inspect ->
        KasGoImportPreviewDialog(
            inspect = inspect,
            isImporting = kasGoImportState.isImporting,
            errorMessage = kasGoImportState.errorMessage,
            onConfirm = { options -> viewModel.confirmKasGoImport(options) },
            onDismiss = { if (!kasGoImportState.isImporting) viewModel.dismissKasGoImportPreview() }
        )
    }

    if (kasGoImportState.inspectResult == null && kasGoImportState.errorMessage != null) {
        Dialog(onDismissRequest = { viewModel.dismissKasGoImportPreview() }) {
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
                        kasGoImportState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                    )
                    Button(
                        onClick = { viewModel.dismissKasGoImportPreview() },
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
private fun KasGoImportPreviewDialog(
    inspect: KasGoInspectResult,
    isImporting: Boolean,
    errorMessage: String?,
    onConfirm: (KasGoImportOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(false) }
    var importStoreProfile by remember { mutableStateOf(true) }
    var showTables by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = AccentBlue)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pratinjau Backup KasGo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            inspect.fileName,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                    }
                }

                if (inspect.storePreview.storeName.isNotBlank() || inspect.manifestSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sapphire50,
                        border = BorderStroke(1.dp, Sapphire200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (inspect.storePreview.storeName.isNotBlank()) {
                                Text(
                                    "Toko: ${inspect.storePreview.storeName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                                )
                            }
                            if (inspect.manifestSummary.isNotBlank()) {
                                Text(
                                    inspect.manifestSummary,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KasGoCountPill("Produk", "${inspect.productCount}", Modifier.weight(1f))
                    KasGoCountPill("Transaksi", "${inspect.saleCount}", Modifier.weight(1f))
                    KasGoCountPill("Pelanggan", "${inspect.customerCount}", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KasGoCountPill("Akun Kas", "${inspect.cashAccountCount}", Modifier.weight(1f))
                    KasGoCountPill("Pengeluaran", "${inspect.expenseCount}", Modifier.weight(1f))
                    KasGoCountPill("Foto", "${inspect.photoCount}", Modifier.weight(1f))
                }

                TextButton(
                    onClick = { showTables = !showTables },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (showTables) "Sembunyikan daftar tabel (${inspect.discoveredTables.size})" else "Lihat semua tabel di database.db (${inspect.discoveredTables.size})",
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold)
                    )
                }

                if (showTables) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            inspect.discoveredTables.forEach { tbl ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${tbl.tableName} (${tbl.mappedRole})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate800, fontSize = 11.sp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${tbl.rowCount} baris",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp).tabular()
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDefault)

                Text(
                    "Metode Pengisian Data",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                )

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
                            Text("Gabungkan (Merge) • Disarankan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            Text("Tambah data baru dari .kasgo tanpa menghapus data yang sudah ada", fontSize = 10.sp, color = Slate600)
                        }
                    }
                }

                Surface(
                    onClick = { replaceExisting = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (replaceExisting) Rose100.copy(alpha = 0.55f) else Slate50,
                    border = BorderStroke(1.dp, if (replaceExisting) Rose500 else BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                        Column {
                            Text("Ganti Data Lama (Replace)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Rose500)
                            Text("Hapus data bisnis saat ini lalu isi penuh dari backup .kasgo", fontSize = 10.sp, color = Slate600)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { importStoreProfile = !importStoreProfile },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = importStoreProfile, onCheckedChange = { importStoreProfile = it })
                    Text(
                        "Terapkan profil toko dari kasgo_settings.json",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
                    )
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
                        enabled = !isImporting,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Slate700)
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                KasGoImportOptions(
                                    replaceExisting = replaceExisting,
                                    importStoreProfile = importStoreProfile
                                )
                            )
                        },
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kasgo_confirm_import_btn")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mengimpor...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text("Mulai Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KasGoCountPill(label: String, value: String, modifier: Modifier = Modifier) {
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
