package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.PrinterEntity
import com.example.hardware.DetectedPrinterDevice
import com.example.hardware.DetectedScannerDevice
import com.example.hardware.HardwareDeviceDiscovery
import com.example.hardware.HardwareDiscoverySnapshot
import com.example.hardware.ScannerFeedbackPlayer
import com.example.hardware.ScannerSettings
import com.example.hardware.ThermalPrinterEngine
import com.example.ui.theme.*

@Composable
fun ScannerSettingsDialog(
    currentSettings: ScannerSettings,
    onSaveSettings: (ScannerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(currentSettings) { mutableStateOf(currentSettings) }
    var snapshot by remember { mutableStateOf(HardwareDeviceDiscovery.inspectHardware(context)) }
    var testCodeInput by remember { mutableStateOf("") }
    var lastTestResult by remember { mutableStateOf<String?>(null) }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
    }

    LaunchedEffect(Unit) {
        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .testTag("scanner_settings_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Pengaturan Scan Barcode",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    "Deteksi alat scanner USB/Bluetooth & perilaku kamera",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }
                }

                // Connected Scanners Section
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgCanvas,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Scanner Terhubung (${snapshot.scanners.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { HardwareDeviceDiscovery.openBluetoothSettings(context) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bluetooth HP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                                    }
                                    IconButton(
                                        onClick = { snapshot = HardwareDeviceDiscovery.inspectHardware(context) },
                                        modifier = Modifier.size(34.dp).testTag("refresh_scanners_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Deteksi Ulang", tint = AccentBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (!snapshot.hasBluetoothPermission) {
                                Button(
                                    onClick = {
                                        val perms = HardwareDeviceDiscovery.requiredBluetoothPermissions()
                                        if (perms.isNotEmpty()) btPermLauncher.launch(perms)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Izinkan Deteksi Bluetooth", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (snapshot.scanners.isEmpty()) {
                                Text(
                                    "Belum ada alat scanner eksternal terdeteksi. Hubungkan scanner USB OTG atau pair Bluetooth HID/SPP di Pengaturan Bluetooth HP lalu tekan Segarkan.",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            } else {
                                snapshot.scanners.forEach { sc ->
                                    val isSelected = draft.selectedScannerId == sc.id
                                    DetectedScannerRow(
                                        scanner = sc,
                                        isSelected = isSelected,
                                        onSelect = {
                                            if (sc.usbDeviceName != null && !sc.hasUsbPermission) {
                                                HardwareDeviceDiscovery.requestUsbPermission(context, sc.usbDeviceName) { granted ->
                                                    snapshot = HardwareDeviceDiscovery.inspectHardware(context)
                                                    if (granted) {
                                                        draft = draft.copy(
                                                            selectedScannerId = sc.id,
                                                            selectedScannerName = sc.name
                                                        )
                                                    }
                                                }
                                            } else {
                                                draft = if (isSelected) {
                                                    draft.copy(selectedScannerId = "", selectedScannerName = "")
                                                } else {
                                                    draft.copy(selectedScannerId = sc.id, selectedScannerName = sc.name)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            if (draft.selectedScannerName.isNotBlank()) {
                                Text(
                                    "Dipilih: ${draft.selectedScannerName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }

                // Live Test Input Box
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Sapphire50,
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tes Tembak Barcode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                            OutlinedTextField(
                                value = testCodeInput,
                                onValueChange = { raw ->
                                    if (raw.contains('\n') || raw.contains('\r')) {
                                        val code = raw.replace("\r", "").replace("\n", "").trim()
                                        if (code.isNotEmpty()) {
                                            ScannerFeedbackPlayer.playSuccess(context, draft.beepOnScan, draft.vibrateOnScan)
                                            lastTestResult = "Terbaca: $code (${code.length} karakter)"
                                            testCodeInput = ""
                                        }
                                    } else {
                                        testCodeInput = raw
                                    }
                                },
                                placeholder = { Text("Arahkan kursor ke sini lalu tembak scanner...", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val code = testCodeInput.trim()
                                        if (code.isNotEmpty()) {
                                            ScannerFeedbackPlayer.playSuccess(context, draft.beepOnScan, draft.vibrateOnScan)
                                            lastTestResult = "Terbaca: $code (${code.length} karakter)"
                                            testCodeInput = ""
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceWhite,
                                    unfocusedContainerColor = SurfaceWhite,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderInput,
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onPreviewKeyEvent { ev ->
                                        if (ev.type == KeyEventType.KeyUp &&
                                            (ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                                        ) {
                                            val code = testCodeInput.trim()
                                            if (code.isNotEmpty()) {
                                                ScannerFeedbackPlayer.playSuccess(context, draft.beepOnScan, draft.vibrateOnScan)
                                                lastTestResult = "Terbaca: $code (${code.length} karakter)"
                                                testCodeInput = ""
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                            if (lastTestResult != null) {
                                Text(
                                    lastTestResult!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }

                // Mode & Behavior Settings
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode Default Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        DesignSegmentedTabs(
                            options = listOf(
                                "AUTO" to "Kamera + Alat",
                                "KAMERA" to "Kamera",
                                "HARDWARE" to "Alat Scanner"
                            ),
                            selectedKey = draft.preferredMode,
                            onSelect = { draft = draft.copy(preferredMode = it) }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ScannerToggleRow(
                            title = "Auto-Tangkap Scanner USB/Bluetooth",
                            subtitle = "Langsung tangkap tembakan scanner di halaman Kasir & Scan",
                            checked = draft.autoCaptureHardwareInput,
                            onCheckedChange = { draft = draft.copy(autoCaptureHardwareInput = it) }
                        )
                        HorizontalDivider(color = BorderDefault)
                        ScannerToggleRow(
                            title = "Scan Berkelanjutan (Otomatis)",
                            subtitle = "Kamera tetap aktif untuk scan banyak barang berurutan",
                            checked = draft.continuousMode,
                            onCheckedChange = { draft = draft.copy(continuousMode = it) }
                        )
                        HorizontalDivider(color = BorderDefault)
                        ScannerToggleRow(
                            title = "Bunyi Beep Saat Barcode Terbaca",
                            subtitle = "Mainkan suara beep POS tiap kali kode ditemukan",
                            checked = draft.beepOnScan,
                            onCheckedChange = { draft = draft.copy(beepOnScan = it) }
                        )
                        HorizontalDivider(color = BorderDefault)
                        ScannerToggleRow(
                            title = "Getar (Haptic) Saat Terbaca",
                            subtitle = "Berikan getaran singkat konfirmasi scan berhasil",
                            checked = draft.vibrateOnScan,
                            onCheckedChange = { draft = draft.copy(vibrateOnScan = it) }
                        )
                        HorizontalDivider(color = BorderDefault)
                        ScannerToggleRow(
                            title = "Nyalakan Lampu Kilat (Flash) Default",
                            subtitle = "Aktifkan senter kamera otomatis di tempat minim cahaya",
                            checked = draft.defaultTorchOn,
                            onCheckedChange = { draft = draft.copy(defaultTorchOn = it) }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Jeda Anti-Scan Ganda Kamera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(600L to "Cepat (0.6d)", 1200L to "Normal (1.2d)", 2200L to "Lambat (2.2d)").forEach { (ms, label) ->
                                DesignFilterChip(
                                    label = label,
                                    selected = draft.scanCooldownMs == ms,
                                    onClick = { draft = draft.copy(scanCooldownMs = ms) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Slate600)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveSettings(draft)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_scanner_settings_button")
                        ) {
                            Text("Simpan Pengaturan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedScannerRow(
    scanner: DetectedScannerDevice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Sapphire50 else SurfaceWhite,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = when {
                isSelected -> AccentBlue
                scanner.isConnectedNow -> Emerald500.copy(alpha = 0.5f)
                else -> BorderDefault
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (scanner.transport.contains("USB", true)) Icons.Default.Usb else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isSelected) AccentBlue else if (scanner.isConnectedNow) Emerald700 else Slate500,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    scanner.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${scanner.transport} • ${scanner.detail}",
                    fontSize = 10.sp,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DesignStatusBadge(
                text = when {
                    isSelected -> "Dipilih"
                    !scanner.hasUsbPermission -> "Beri Izin"
                    scanner.isConnectedNow -> "Terhubung"
                    else -> "Terpasang"
                },
                type = when {
                    isSelected -> BadgeType.INFO
                    !scanner.hasUsbPermission -> BadgeType.WARNING
                    scanner.isConnectedNow -> BadgeType.SUCCESS
                    else -> BadgeType.NORMAL
                }
            )
        }
    }
}

@Composable
private fun ScannerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
            Text(subtitle, fontSize = 11.sp, color = Slate500)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
        )
    }
}

@Composable
fun PrinterManagerDialog(
    printers: List<PrinterEntity>,
    activePrinterId: Long,
    onSelectPrimaryPrinter: (Long) -> Unit,
    onSavePrinter: (PrinterEntity, Boolean) -> Unit,
    onDeletePrinter: (PrinterEntity) -> Unit,
    onTestPrint: (PrinterEntity, (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(HardwareDeviceDiscovery.inspectHardware(context)) }
    var nearbyDevices by remember { mutableStateOf<List<DetectedPrinterDevice>>(emptyList()) }
    var isScanningNearby by remember { mutableStateOf(false) }
    var stopDiscoveryAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var newPrinterName by remember { mutableStateOf("") }
    var btAddr by remember { mutableStateOf("") }
    var printerType by remember { mutableStateOf("struk") }
    var widthMm by remember { mutableIntStateOf(58) }
    var autoCut by remember { mutableStateOf(false) }
    var openDrawer by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var isTestingPrint by remember { mutableStateOf(false) }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
    }

    LaunchedEffect(Unit) {
        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            stopDiscoveryAction?.invoke()
            stopDiscoveryAction = null
        }
    }

    val combinedDetectedPrinters = remember(snapshot.printers, nearbyDevices) {
        val byAddress = LinkedHashMap<String, DetectedPrinterDevice>()
        snapshot.printers.forEach { byAddress[it.address] = it }
        nearbyDevices.forEach { dev ->
            if (!byAddress.containsKey(dev.address)) {
                byAddress[dev.address] = dev
            }
        }
        byAddress.values.toList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 690.dp)
                .testTag("printer_manager_dialog")
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Printer Bluetooth & USB",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    "Deteksi otomatis printer thermal 58mm/80mm & USB OTG",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }
                }

                if (statusNotice != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (statusIsError) Rose100 else Emerald100,
                            border = BorderStroke(1.dp, if (statusIsError) Rose500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusNotice!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (statusIsError) Rose500 else Emerald800,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Saved Printers List
                if (printers.isNotEmpty()) {
                    item {
                        Text(
                            "Printer Tersimpan (${printers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                    printers.forEach { p ->
                        val isPrimary = p.id == activePrinterId || (activePrinterId <= 0L && printers.firstOrNull()?.id == p.id)
                        item {
                            Surface(
                                onClick = { onSelectPrimaryPrinter(p.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPrimary) Sapphire50 else BgCanvas,
                                border = BorderStroke(if (isPrimary) 1.5.dp else 1.dp, if (isPrimary) AccentBlue else BorderDefault),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                                if (isPrimary) {
                                                    DesignStatusBadge(text = "Utama", type = BadgeType.INFO)
                                                }
                                            }
                                            Text(
                                                "${if (ThermalPrinterEngine.isUsbAddress(p.bluetoothAddress)) "USB OTG" else p.bluetoothAddress.ifBlank { "Cetak Sistem" }} • ${p.widthMm}mm • ${p.language.uppercase()}",
                                                fontSize = 11.sp,
                                                color = Slate600,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onDeletePrinter(p) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Rose500, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!isPrimary) {
                                            OutlinedButton(
                                                onClick = { onSelectPrimaryPrinter(p.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentBlue)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Jadikan Utama", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                if (ThermalPrinterEngine.isUsbAddress(p.bluetoothAddress)) {
                                                    val usbName = p.bluetoothAddress.split(":", limit = 4).getOrNull(3)
                                                    if (!usbName.isNullOrBlank()) {
                                                        HardwareDeviceDiscovery.requestUsbPermission(context, usbName) { granted ->
                                                            if (granted) {
                                                                isTestingPrint = true
                                                                statusNotice = "Mengirim tes cetak ke ${p.name}..."
                                                                statusIsError = false
                                                                onTestPrint(p) { ok, msg ->
                                                                    isTestingPrint = false
                                                                    statusIsError = !ok
                                                                    statusNotice = msg
                                                                }
                                                            } else {
                                                                statusIsError = true
                                                                statusNotice = "Izin USB printer ditolak"
                                                            }
                                                        }
                                                        return@Button
                                                    }
                                                }
                                                isTestingPrint = true
                                                statusNotice = "Mengirim tes cetak ke ${p.name}..."
                                                statusIsError = false
                                                onTestPrint(p) { ok, msg ->
                                                    isTestingPrint = false
                                                    statusIsError = !ok
                                                    statusNotice = msg
                                                }
                                            },
                                            enabled = !isTestingPrint,
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f).testTag("test_print_button_${p.id}")
                                        ) {
                                            if (isTestingPrint) {
                                                CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tes Cetak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto-Detected Printers Section
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgCanvas,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Printer Terdeteksi di Perangkat (${combinedDetectedPrinters.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
                                    },
                                    modifier = Modifier.size(32.dp).testTag("refresh_printers_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Segarkan Printer", tint = AccentBlue, modifier = Modifier.size(18.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!snapshot.hasBluetoothPermission) {
                                    Button(
                                        onClick = {
                                            val perms = HardwareDeviceDiscovery.requiredBluetoothPermissions()
                                            if (perms.isNotEmpty()) btPermLauncher.launch(perms)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).testTag("grant_bt_printer_perm_button")
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Izinkan Bluetooth", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            if (isScanningNearby) {
                                                stopDiscoveryAction?.invoke()
                                                stopDiscoveryAction = null
                                                isScanningNearby = false
                                            } else {
                                                nearbyDevices = emptyList()
                                                isScanningNearby = true
                                                stopDiscoveryAction = HardwareDeviceDiscovery.startBluetoothDiscovery(
                                                    context = context,
                                                    onDeviceFound = { found ->
                                                        if (nearbyDevices.none { it.address == found.address }) {
                                                            nearbyDevices = nearbyDevices + found
                                                        }
                                                    },
                                                    onFinished = {
                                                        isScanningNearby = false
                                                        snapshot = HardwareDeviceDiscovery.inspectHardware(context)
                                                    }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).testTag("scan_nearby_bt_printers_button")
                                    ) {
                                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isScanningNearby) "Hentikan Scan..." else "Scan Bluetooth",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlue
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { HardwareDeviceDiscovery.openBluetoothSettings(context) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate700)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bluetooth HP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                }
                            }

                            if (!snapshot.isBluetoothEnabled && snapshot.hasBluetoothPermission) {
                                Text(
                                    "Bluetooth HP sedang nonaktif. Nyalakan Bluetooth di HP agar printer Bluetooth terbaca.",
                                    fontSize = 11.sp,
                                    color = Amber500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (combinedDetectedPrinters.isEmpty()) {
                                Text(
                                    "Belum ada printer Bluetooth/USB terdeteksi. Pastikan printer menyala & sudah dipair di Bluetooth HP atau terhubung kabel USB OTG, lalu tekan Segarkan.",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            } else {
                                combinedDetectedPrinters.forEach { dev ->
                                    val alreadyChosen = btAddr.equals(dev.address, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            if (dev.usbDeviceName != null && !dev.hasUsbPermission) {
                                                HardwareDeviceDiscovery.requestUsbPermission(context, dev.usbDeviceName) { granted ->
                                                    snapshot = HardwareDeviceDiscovery.inspectHardware(context)
                                                    if (granted) {
                                                        newPrinterName = dev.name
                                                        btAddr = dev.address
                                                        statusIsError = false
                                                        statusNotice = "Printer USB ${dev.name} dipilih. Klik Simpan Printer di bawah."
                                                    } else {
                                                        statusIsError = true
                                                        statusNotice = "Izin USB printer ditolak oleh sistem."
                                                    }
                                                }
                                            } else {
                                                newPrinterName = dev.name
                                                btAddr = dev.address
                                                statusIsError = false
                                                statusNotice = "Printer \"${dev.name}\" dipilih. Atur ukuran kertas lalu klik Simpan Printer."
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (alreadyChosen) Sapphire50 else SurfaceWhite,
                                        border = BorderStroke(
                                            if (alreadyChosen) 1.5.dp else 1.dp,
                                            if (alreadyChosen) AccentBlue else if (dev.isConnectedNow) Emerald500.copy(alpha = 0.5f) else BorderDefault
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (dev.transport.contains("USB", true)) Icons.Default.Usb else Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (alreadyChosen) AccentBlue else if (dev.isConnectedNow) Emerald700 else Slate500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(dev.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    "${dev.transport} • ${if (dev.address.startsWith("USB:")) "USB OTG" else dev.address}" +
                                                        if (dev.isLikelyPrinter) " • Printer" else "",
                                                    fontSize = 10.sp,
                                                    color = Slate600,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            DesignStatusBadge(
                                                text = when {
                                                    alreadyChosen -> "Dipilih"
                                                    !dev.hasUsbPermission -> "Izin USB"
                                                    dev.isConnectedNow -> "Terhubung"
                                                    dev.isPaired -> "Terpasang"
                                                    else -> "Ditemukan"
                                                },
                                                type = when {
                                                    alreadyChosen -> BadgeType.INFO
                                                    !dev.hasUsbPermission -> BadgeType.WARNING
                                                    dev.isConnectedNow -> BadgeType.SUCCESS
                                                    else -> BadgeType.NORMAL
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = BorderDefault) }

                // Add / Configure Selected Printer Form
                item {
                    Text(
                        "Tambah / Simpan Printer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }
                item { DesignLabeledTextField("Nama Printer", newPrinterName, { newPrinterName = it }, placeholder = "Contoh: Iware 58mm Kasir") }
                item { DesignLabeledTextField("Alamat Bluetooth (MAC) / USB", btAddr, { btAddr = it }, placeholder = "Pilih dari daftar di atas atau ketik MAC") }
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Auto Cut Kertas", fontSize = 13.sp, color = Slate800)
                            Switch(checked = autoCut, onCheckedChange = { autoCut = it }, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Laci Kasir (Cash Drawer)", fontSize = 13.sp, color = Slate800)
                            Switch(checked = openDrawer, onCheckedChange = { openDrawer = it }, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Tutup", color = Slate600) }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newPrinterName.isNotBlank()) {
                                    onSavePrinter(
                                        PrinterEntity(
                                            name = newPrinterName.trim(),
                                            bluetoothAddress = btAddr.trim(),
                                            type = printerType,
                                            language = if (printerType == "stiker") "tspl" else "escpos",
                                            widthMm = widthMm,
                                            autoCut = autoCut,
                                            openDrawer = openDrawer
                                        ),
                                        true
                                    )
                                    statusIsError = false
                                    statusNotice = "Printer \"${newPrinterName.trim()}\" disimpan & dijadikan utama."
                                    newPrinterName = ""
                                    btAddr = ""
                                }
                            },
                            enabled = newPrinterName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_printer_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan Printer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
