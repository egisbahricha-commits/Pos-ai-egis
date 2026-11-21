package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.LicenseDoc
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val licenses by viewModel.allLicenses.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var newOwnerName by remember { mutableStateOf("") }
    var newOwnerEmail by remember { mutableStateOf("") }
    var generatedKeyResult by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (isSuperAdmin) {
            viewModel.loadSuperAdminLicenses()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Admin Pusat (Super Admin)")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSuperAdminLicenses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
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
                    contentColor = Color.White
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
                        "Menu rahasia ini khusus untuk Admin Pusat (egisbahricha@gmail.com). Silakan masuk dengan akun Super Admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Daftar Lisensi Toko Pelanggan (${licenses.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (licenses.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada lisensi terdaftar di cloud.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(licenses) { license ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Store, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = license.storeName.ifBlank { "Toko Pelanggan" },
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                            Badge(containerColor = if (license.status == "ACTIVE") Color(0xFF16A34A) else Color.Red) {
                                                Text(license.status, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Pemilik: ${license.ownerName} (${license.ownerEmail})", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Kunci Lisensi: ${license.licenseKey}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = AccentBlue))
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (license.deviceId.isNotBlank()) "Terikat Device ID: ${license.deviceId}" else "Belum terikat perangkat (Device Kosong)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (license.deviceId.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD97706)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (license.deviceId.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.resetCustomerDeviceBinding(license.licenseKey) { success, msg ->
                                                            snackbarMessage = msg
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reset Device Binding")
                                                }
                                            }
                                        }
                                    }
                                }
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
                title = { Text("Buat Lisensi Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (generatedKeyResult != null) {
                            Text("Lisensi berhasil dibuat!", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = generatedKeyResult ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kunci Lisensi") }
                            )
                            Text("Berikan kunci ini kepada pelanggan untuk aktivasi di aplikasi mereka.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            OutlinedTextField(
                                value = newStoreName,
                                onValueChange = { newStoreName = it },
                                label = { Text("Nama Toko Pelanggan") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOwnerName,
                                onValueChange = { newOwnerName = it },
                                label = { Text("Nama Pemilik") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOwnerEmail,
                                onValueChange = { newOwnerEmail = it },
                                label = { Text("Email Pemilik") },
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
                        }) {
                            Text("Tutup")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (newStoreName.isNotBlank() && newOwnerEmail.isNotBlank()) {
                                    viewModel.generateNewLicense(newStoreName, newOwnerName, newOwnerEmail) { key ->
                                        generatedKeyResult = key
                                    }
                                }
                            }
                        ) {
                            Text("Generate")
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
