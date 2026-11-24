package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.firebase.BugReportDoc
import com.example.firebase.KasLiteSupportEmailHelper
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BugReportScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val bankConfig by viewModel.developerBankConfig.collectAsStateWithLifecycle()
    val myBugReports by viewModel.myBugReports.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSupportSyncLoading.collectAsStateWithLifecycle()

    var category by remember { mutableStateOf(KasLiteSupportEmailHelper.BUG_CATEGORIES.first()) }
    var severity by remember { mutableStateOf("SEDANG") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactEmail by remember(licenseUiState.ownerEmail, authUiState.lastEmail) {
        mutableStateOf(
            licenseUiState.ownerEmail.ifBlank { authUiState.userEmail }.ifBlank { authUiState.lastEmail }
        )
    }
    var formStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showSupabaseAuthDialog by remember { mutableStateOf(false) }

    val diagnosticSummary = remember { viewModel.buildCurrentDeviceBugDiagnosticSummary() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LaunchedEffect(Unit) {
        viewModel.loadCustomerSupportData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("bug_report_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ModulePengaturan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Amber400, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Laporan Bug & Kendala",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Laporkan error, crash, atau kendala fitur agar bisa kami perbaiki di pembaruan berikutnya.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                        )
                    }
                    IconButton(onClick = { viewModel.loadCustomerSupportData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan", tint = Color.White)
                    }
                }
            }
        }

        // Bug Form Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Kategori Masalah",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Slate800)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KasLiteSupportEmailHelper.BUG_CATEGORIES.forEach { cat ->
                            DesignFilterChip(
                                label = cat,
                                selected = category == cat,
                                onClick = { category = cat }
                            )
                        }
                    }

                    Text(
                        text = "Tingkat Dampak",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Slate800)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KasLiteSupportEmailHelper.BUG_SEVERITIES.forEach { (key, label) ->
                            DesignFilterChip(
                                label = label,
                                selected = severity == key,
                                onClick = { severity = key }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Masalah *") },
                        placeholder = { Text("Contoh: Scanner Bluetooth tidak terdeteksi saat kasir") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bug_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detail & Langkah Kejadian Bug *") },
                        placeholder = { Text("Jelaskan apa yang Anda tekan, hasil yang muncul, dan kondisi perangkat...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bug_description_input")
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it.trim() },
                        label = { Text("Email Balasan (opsional)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Transparent diagnostics preview
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate50,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "Info Diagnostik Terlampir Otomatis",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800)
                                )
                                Text(
                                    text = diagnosticSummary,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = formStatus != null) {
                        formStatus?.let { (isOk, msg) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOk) Emerald50 else Rose100,
                                border = BorderStroke(1.dp, if (isOk) Emerald200 else Rose500.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isOk) Emerald800 else Rose500,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            formStatus = null
                            viewModel.submitCustomerBugReport(
                                category = category,
                                severity = severity,
                                title = title,
                                description = description,
                                contactEmail = contactEmail,
                                onRequireAuth = { showSupabaseAuthDialog = true },
                                onResult = { ok, msg, _ ->
                                    formStatus = ok to msg
                                    if (ok) {
                                        title = ""
                                        description = ""
                                    }
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bug_report_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Kirim Laporan Bug", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (title.isBlank() || description.isBlank()) {
                                formStatus = false to "Isi judul dan detail masalah terlebih dahulu."
                                return@OutlinedButton
                            }
                            val targetEmail = bankConfig.supportEmail.ifBlank { SUPER_ADMIN_EMAIL }
                            val draft = BugReportDoc(
                                userEmail = contactEmail,
                                reporterName = activeUserName,
                                storeName = storeProfile?.name.orEmpty(),
                                deviceId = authUiState.deviceId,
                                category = category,
                                severity = severity,
                                title = title,
                                description = description,
                                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                            )
                            val mailtoUri = Uri.parse(
                                "mailto:${Uri.encode(targetEmail)}?subject=${
                                    Uri.encode(KasLiteSupportEmailHelper.buildBugReportEmailSubject(category, title))
                                }&body=${Uri.encode(KasLiteSupportEmailHelper.buildBugReportEmailBody(draft))}"
                            )
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, mailtoUri))
                            }.onFailure {
                                formStatus = false to "Aplikasi email tidak tersedia di perangkat ini."
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim via Aplikasi Email", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section: Riwayat Laporan Saya
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Riwayat Laporan Bug Saya (${myBugReports.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
            }
        }

        if (myBugReports.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada laporan bug yang dikirim dari akun ini.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(myBugReports, key = { it.id.ifBlank { "${it.title}_${it.createdAtMillis}" } }) { bug ->
                val (statusLabel, badgeType) = when (bug.status.uppercase()) {
                    "SELESAI" -> "Selesai Diperbaiki" to BadgeType.SUCCESS
                    "DIPROSES" -> "Sedang Ditangani" to BadgeType.INFO
                    "DITOLAK" -> "Ditutup" to BadgeType.DANGER
                    else -> "Laporan Masuk" to BadgeType.WARNING
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${bug.category} • ${bug.severity}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                            )
                            DesignStatusBadge(text = statusLabel, type = badgeType)
                        }
                        Text(
                            text = bug.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            text = bug.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                        )
                        if (bug.createdAtMillis > 0L) {
                            Text(
                                text = "Dilaporkan: ${dateFormat.format(Date(bug.createdAtMillis))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                            )
                        }
                        if (bug.adminNotes.isNotBlank()) {
                            HorizontalDivider(color = BorderDefault)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Sapphire50,
                                border = BorderStroke(1.dp, Sapphire200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Tanggapan Pengembang:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                                    )
                                    Text(
                                        text = bug.adminNotes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showSupabaseAuthDialog) {
        SupportSupabaseAuthDialog(
            initialEmail = contactEmail.ifBlank { authUiState.lastEmail },
            viewModel = viewModel,
            onDismiss = { showSupabaseAuthDialog = false },
            onAuthenticated = {
                showSupabaseAuthDialog = false
                viewModel.loadCustomerSupportData()
                formStatus = true to "Akun terverifikasi! Tekan Kirim Laporan Bug untuk menyimpan ke server."
            }
        )
    }
}
