package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.BugReportDoc
import com.example.firebase.DeveloperBankConfig
import com.example.firebase.DonationPackage
import com.example.firebase.DonationRequestDoc
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

internal fun launchPrefilledLicenseEmail(
    context: Context,
    recipientEmail: String,
    ownerName: String,
    storeName: String,
    licenseKey: String,
    packageTitle: String,
    amountRupiah: Long,
    expiresAtMillis: Long,
    supportEmail: String = SUPER_ADMIN_EMAIL
): Boolean {
    val cleanTo = recipientEmail.trim()
    if (cleanTo.isBlank()) return false
    val subject = KasLiteSupportEmailHelper.buildLicenseEmailSubject(storeName, licenseKey)
    val body = KasLiteSupportEmailHelper.buildLicenseEmailBody(
        ownerName = ownerName,
        storeName = storeName,
        recipientEmail = cleanTo,
        licenseKey = licenseKey,
        packageTitle = packageTitle,
        amountRupiah = amountRupiah,
        expiresAtMillis = expiresAtMillis,
        supportEmail = supportEmail
    )
    val mailtoUri = Uri.parse(
        "mailto:${Uri.encode(cleanTo)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, mailtoUri))
        true
    }.getOrDefault(false)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminDonationsTab(
    donations: List<DonationRequestDoc>,
    supportEmail: String,
    viewModel: PosViewModel,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var filterStatus by remember { mutableStateOf("ALL") }
    var rejectingDonation by remember { mutableStateOf<DonationRequestDoc?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var busyId by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    val pendingCount = donations.count { it.status.equals("PENDING", ignoreCase = true) }
    val paidCount = donations.count { it.status.equals("PAID", ignoreCase = true) }
    val rejectedCount = donations.count { it.status.equals("REJECTED", ignoreCase = true) }

    val filtered = remember(donations, filterStatus) {
        when (filterStatus) {
            "PENDING" -> donations.filter { it.status.equals("PENDING", ignoreCase = true) }
            "PAID" -> donations.filter { it.status.equals("PAID", ignoreCase = true) }
            "REJECTED" -> donations.filter { it.status.equals("REJECTED", ignoreCase = true) }
            else -> donations
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTabMetricChip("Total", "${donations.size}", AccentBlue, Modifier.weight(1f))
                AdminTabMetricChip("Pending", "$pendingCount", Amber500, Modifier.weight(1f))
                AdminTabMetricChip("Dibayar", "$paidCount", Emerald700, Modifier.weight(1f))
                AdminTabMetricChip("Ditolak", "$rejectedCount", Rose500, Modifier.weight(1f))
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DesignFilterChip("Semua (${donations.size})", filterStatus == "ALL", onClick = { filterStatus = "ALL" }) }
                item { DesignFilterChip("Pending ($pendingCount)", filterStatus == "PENDING", onClick = { filterStatus = "PENDING" }) }
                item { DesignFilterChip("Sudah Dibayar ($paidCount)", filterStatus == "PAID", onClick = { filterStatus = "PAID" }) }
                item { DesignFilterChip("Ditolak ($rejectedCount)", filterStatus == "REJECTED", onClick = { filterStatus = "REJECTED" }) }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada konfirmasi donasi masuk pada filter ini.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate500),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id.ifBlank { "${it.userEmail}_${it.createdAtMillis}" } }) { donation ->
                val isPending = donation.status.equals("PENDING", ignoreCase = true)
                val isPaid = donation.status.equals("PAID", ignoreCase = true)
                val (badgeLabel, badgeType) = when {
                    isPaid -> "SUDAH DIBAYAR" to BadgeType.SUCCESS
                    donation.status.equals("REJECTED", ignoreCase = true) -> "DITOLAK" to BadgeType.DANGER
                    else -> "MENUNGGU CEK MUTASI" to BadgeType.WARNING
                }
                val pkg = DonationPackage.fromId(donation.packageId)
                val effectiveExpiry = if (donation.durationDays > 0 && donation.approvedAtMillis > 0L) {
                    donation.approvedAtMillis + donation.durationDays * 86_400_000L
                } else 0L

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = donation.storeName.ifBlank { donation.ownerName.ifBlank { "Donatur KASLITE" } },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    text = "${donation.packageTitle.ifBlank { pkg.title }} • ${KasLiteSupportEmailHelper.formatIdr(donation.amountRupiah.takeIf { it > 0L } ?: pkg.amountRupiah)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                )
                            }
                            DesignStatusBadge(text = badgeLabel, type = badgeType)
                        }

                        Text("Email Penerima Lisensi: ${donation.userEmail}", style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontWeight = FontWeight.SemiBold))
                        Text("Pengirim Transfer: ${donation.senderBankName} a.n. ${donation.senderAccountName}", style = MaterialTheme.typography.bodySmall.copy(color = Slate700))
                        if (donation.transferNote.isNotBlank()) {
                            Text("Catatan Transfer: ${donation.transferNote}", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                        }
                        if (donation.createdAtMillis > 0L) {
                            Text("Masuk: ${dateFormat.format(Date(donation.createdAtMillis))}", style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                        }
                        if (donation.licenseKey.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald50,
                                border = BorderStroke(1.dp, Emerald200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Kunci Lisensi Diterbitkan:", fontSize = 11.sp, color = Emerald800)
                                        Text(donation.licenseKey, fontWeight = FontWeight.ExtraBold, color = Emerald700, fontSize = 14.sp)
                                    }
                                    if (donation.emailSentAtMillis > 0L) {
                                        DesignStatusBadge(text = "Email Terkirim", type = BadgeType.SUCCESS)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isPending || (isPaid && donation.licenseKey.isBlank())) {
                                Button(
                                    onClick = {
                                        busyId = donation.id
                                        viewModel.superAdminApproveDonationAndIssueLicense(donation) { ok, msg, outcome ->
                                            busyId = null
                                            onFeedback(msg)
                                            if (ok && outcome != null && !outcome.emailDispatchedAutomatically) {
                                                val opened = launchPrefilledLicenseEmail(
                                                    context = context,
                                                    recipientEmail = donation.userEmail,
                                                    ownerName = donation.ownerName,
                                                    storeName = donation.storeName,
                                                    licenseKey = outcome.license.licenseKey,
                                                    packageTitle = donation.packageTitle.ifBlank { pkg.title },
                                                    amountRupiah = donation.amountRupiah.takeIf { it > 0L } ?: pkg.amountRupiah,
                                                    expiresAtMillis = outcome.license.expiresAtMillis,
                                                    supportEmail = supportEmail
                                                )
                                                if (opened) {
                                                    viewModel.superAdminMarkDonationEmailSent(outcome.donation.id)
                                                } else {
                                                    clipboard.setText(AnnotatedString(outcome.license.licenseKey))
                                                    onFeedback("Aplikasi email tidak ditemukan. Kunci ${outcome.license.licenseKey} disalin ke clipboard.")
                                                }
                                            }
                                        }
                                    },
                                    enabled = busyId != donation.id,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("admin_approve_donation_${donation.id}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sudah Dibayar & Kirim Lisensi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (donation.licenseKey.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val opened = launchPrefilledLicenseEmail(
                                            context = context,
                                            recipientEmail = donation.userEmail,
                                            ownerName = donation.ownerName,
                                            storeName = donation.storeName,
                                            licenseKey = donation.licenseKey,
                                            packageTitle = donation.packageTitle.ifBlank { pkg.title },
                                            amountRupiah = donation.amountRupiah.takeIf { it > 0L } ?: pkg.amountRupiah,
                                            expiresAtMillis = effectiveExpiry,
                                            supportEmail = supportEmail
                                        )
                                        if (opened) {
                                            viewModel.superAdminMarkDonationEmailSent(donation.id)
                                            onFeedback("Email siap dikirim ke ${donation.userEmail}.")
                                        } else {
                                            onFeedback("Aplikasi email tidak tersedia.")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kirim ke Email", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(donation.licenseKey))
                                        onFeedback("Kunci ${donation.licenseKey} disalin!")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin Kunci", fontSize = 12.sp)
                                }
                            }

                            if (isPending) {
                                OutlinedButton(
                                    onClick = {
                                        rejectingDonation = donation
                                        rejectReason = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tolak", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    rejectingDonation?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectingDonation = null },
            title = { Text("Tolak Konfirmasi Donasi?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Toko: ${target.storeName.ifBlank { target.userEmail }} (${KasLiteSupportEmailHelper.formatIdr(target.amountRupiah)})")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Alasan / Catatan untuk Pelanggan") },
                        placeholder = { Text("Mis. Mutasi belum masuk, mohon kirim ulang nominal tepat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.superAdminRejectDonation(target.id, rejectReason) { _, msg ->
                            onFeedback(msg)
                        }
                        rejectingDonation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White)
                ) {
                    Text("Tandai Ditolak")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingDonation = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminBugReportsTab(
    reports: List<BugReportDoc>,
    viewModel: PosViewModel,
    onFeedback: (String) -> Unit
) {
    var filterStatus by remember { mutableStateOf("ALL") }
    var editingReport by remember { mutableStateOf<BugReportDoc?>(null) }
    var draftStatus by remember { mutableStateOf("DIPROSES") }
    var draftNotes by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    val newCount = reports.count { it.status.equals("BARU", ignoreCase = true) }
    val inProgressCount = reports.count { it.status.equals("DIPROSES", ignoreCase = true) }
    val resolvedCount = reports.count { it.status.equals("SELESAI", ignoreCase = true) }

    val filtered = remember(reports, filterStatus) {
        if (filterStatus == "ALL") reports
        else reports.filter { it.status.equals(filterStatus, ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTabMetricChip("Total", "${reports.size}", AccentBlue, Modifier.weight(1f))
                AdminTabMetricChip("Baru", "$newCount", Amber500, Modifier.weight(1f))
                AdminTabMetricChip("Proses", "$inProgressCount", Sapphire800, Modifier.weight(1f))
                AdminTabMetricChip("Selesai", "$resolvedCount", Emerald700, Modifier.weight(1f))
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DesignFilterChip("Semua (${reports.size})", filterStatus == "ALL", onClick = { filterStatus = "ALL" }) }
                item { DesignFilterChip("Baru ($newCount)", filterStatus == "BARU", onClick = { filterStatus = "BARU" }) }
                item { DesignFilterChip("Diproses ($inProgressCount)", filterStatus == "DIPROSES", onClick = { filterStatus = "DIPROSES" }) }
                item { DesignFilterChip("Selesai ($resolvedCount)", filterStatus == "SELESAI", onClick = { filterStatus = "SELESAI" }) }
                item { DesignFilterChip("Ditolak", filterStatus == "DITOLAK", onClick = { filterStatus = "DITOLAK" }) }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada laporan bug pada filter ini.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate500),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id.ifBlank { "${it.title}_${it.createdAtMillis}" } }) { bug ->
                val (badgeLabel, badgeType) = when (bug.status.uppercase()) {
                    "SELESAI" -> "SELESAI" to BadgeType.SUCCESS
                    "DIPROSES" -> "DIPROSES" to BadgeType.INFO
                    "DITOLAK" -> "DITOLAK" to BadgeType.DANGER
                    else -> "BARU" to BadgeType.WARNING
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = Rose500, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${bug.category} • ${bug.severity}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                )
                            }
                            DesignStatusBadge(text = badgeLabel, type = badgeType)
                        }
                        Text(
                            text = bug.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            text = bug.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                        )
                        HorizontalDivider(color = BorderDefault)
                        Text(
                            text = "Pelapor: ${bug.reporterName.ifBlank { "-" }} (${bug.userEmail.ifBlank { "-" }}) • Toko: ${bug.storeName.ifBlank { "-" }}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                        )
                        Text(
                            text = "Perangkat: ${bug.deviceModel} • ${bug.androidVersion} • App v${bug.appVersion}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                        )
                        if (bug.createdAtMillis > 0L) {
                            Text(
                                text = "Diterima: ${dateFormat.format(Date(bug.createdAtMillis))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                            )
                        }
                        if (bug.adminNotes.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Sapphire50,
                                border = BorderStroke(1.dp, Sapphire200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Catatan Admin: ${bug.adminNotes}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Sapphire800),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                editingReport = bug
                                draftStatus = bug.status.ifBlank { "DIPROSES" }
                                draftNotes = bug.adminNotes
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Tanggapi & Ubah Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    editingReport?.let { bug ->
        AlertDialog(
            onDismissRequest = { editingReport = null },
            title = { Text("Tindak Lanjut Laporan Bug", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(bug.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BARU", "DIPROSES", "SELESAI", "DITOLAK").forEach { st ->
                            DesignFilterChip(
                                label = st,
                                selected = draftStatus == st,
                                onClick = { draftStatus = st }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draftNotes,
                        onValueChange = { draftNotes = it },
                        label = { Text("Catatan Perbaikan / Balasan untuk Pelanggan") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.superAdminUpdateBugReportStatus(bug.id, draftStatus, draftNotes) { _, msg ->
                            onFeedback(msg)
                        }
                        editingReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White)
                ) {
                    Text("Simpan Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReport = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun SuperAdminPaymentConfigTab(
    currentConfig: DeveloperBankConfig,
    viewModel: PosViewModel,
    onFeedback: (String) -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var bankName by remember(currentConfig) { mutableStateOf(currentConfig.bankName) }
    var accountNumber by remember(currentConfig) { mutableStateOf(currentConfig.accountNumber) }
    var accountHolder by remember(currentConfig) { mutableStateOf(currentConfig.accountHolder) }
    var whatsappNumber by remember(currentConfig) { mutableStateOf(currentConfig.whatsappNumber) }
    var supportEmail by remember(currentConfig) { mutableStateOf(currentConfig.supportEmail.ifBlank { SUPER_ADMIN_EMAIL }) }
    var notes by remember(currentConfig) { mutableStateOf(currentConfig.notes) }
    var showEdgeGuideDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rekening Bank Donasi Pengembang",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    Text(
                        text = "Rekening ini tampil otomatis pada halaman Donasi ke Pengembang di semua aplikasi pelanggan (paket Rp 10.000/bln, Rp 50.000/thn, dan Rp 250.000 selamanya).",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Nama Bank / E-Wallet *") },
                        placeholder = { Text("Contoh: BCA / BRI / Mandiri / SeaBank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Nomor Rekening *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = { Text("Atas Nama Pemilik Rekening *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("No. WhatsApp Konfirmasi (opsional)") },
                        placeholder = { Text("Contoh: 0812xxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it.trim() },
                        label = { Text("Email Pengembang / Pengirim Lisensi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Transfer untuk Pelanggan") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveSuperAdminBankConfig(
                                currentConfig.copy(
                                    bankName = bankName,
                                    accountNumber = accountNumber,
                                    accountHolder = accountHolder,
                                    whatsappNumber = whatsappNumber,
                                    supportEmail = supportEmail,
                                    notes = notes
                                )
                            ) { _, msg ->
                                onFeedback(msg)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_save_bank_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Pengaturan Rekening", fontWeight = FontWeight.Bold)
                    }
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pengiriman Kunci Lisensi via Email",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    Text(
                        text = "• Saat tombol 'Sudah Dibayar & Kirim Lisensi' ditekan, sistem membuat kunci lisensi FULL baru di Supabase.\n• Jika Supabase Edge Function 'send-license-email' belum di-deploy, aplikasi otomatis membuka draft Email siap kirim ke alamat email pelanggan dalam 1 ketukan.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600, lineHeight = 18.sp)
                    )
                    OutlinedButton(
                        onClick = { showEdgeGuideDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat Snippet Auto-Email Supabase (Opsional)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showEdgeGuideDialog) {
        val snippet = remember { KasLiteSupportEmailHelper.buildResendEdgeFunctionSnippet() }
        AlertDialog(
            onDismissRequest = { showEdgeGuideDialog = false },
            title = { Text("Opsional: Edge Function send-license-email", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deploy kode Deno berikut sebagai Edge Function bernama 'send-license-email' di Supabase jika ingin pengiriman otomatis tanpa membuka aplikasi Gmail:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = snippet,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(snippet))
                        onFeedback("Snippet Edge Function disalin!")
                        showEdgeGuideDialog = false
                    }
                ) {
                    Text("Salin Kode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEdgeGuideDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
internal fun AdminTabMetricChip(label: String, count: String, accent: Color, modifier: Modifier = Modifier) {
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
