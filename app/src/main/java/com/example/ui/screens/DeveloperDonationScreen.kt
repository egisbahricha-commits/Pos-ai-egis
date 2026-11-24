package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.DonationPackage
import com.example.firebase.KasLiteSupportEmailHelper
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperDonationScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val bankConfig by viewModel.developerBankConfig.collectAsStateWithLifecycle()
    val myDonations by viewModel.myDonationRequests.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSupportSyncLoading.collectAsStateWithLifecycle()
    val supportError by viewModel.supportError.collectAsStateWithLifecycle()

    var selectedPackage by remember { mutableStateOf(DonationPackage.YEARLY) }
    var recipientEmail by remember(licenseUiState.ownerEmail, authUiState.lastEmail) {
        mutableStateOf(
            licenseUiState.ownerEmail.ifBlank { authUiState.userEmail }.ifBlank { authUiState.lastEmail }
        )
    }
    var ownerName by remember(activeUserName) { mutableStateOf(activeUserName) }
    var storeName by remember(storeProfile?.name) { mutableStateOf(storeProfile?.name.orEmpty()) }
    var phone by remember(storeProfile?.phone) { mutableStateOf(storeProfile?.phone.orEmpty()) }
    var senderBankName by remember { mutableStateOf("") }
    var senderAccountName by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var formMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showSupabaseVerifyDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LaunchedEffect(Unit) {
        viewModel.loadCustomerSupportData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("developer_donation_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Sapphire700, ModuleKas)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Donasi ke Pengembang KASLITE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Dukungan Anda menjaga aplikasi terus berkembang",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                        IconButton(onClick = { viewModel.loadCustomerSupportData() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat ulang info donasi",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Sebagai ucapan terima kasih atas donasi dukungan ke pengembang, admin pusat akan mengirimkan kunci lisensi penuh KASLITE langsung ke email Anda.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.92f),
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        if (!supportError.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarningBg,
                    border = BorderStroke(1.dp, AmberWarningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = supportError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate900),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Section: 3 Pilihan Paket Donasi
        item {
            Text(
                text = "1. Pilih Paket Donasi ke Pengembang",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )
        }

        items(DonationPackage.entries.toList(), key = { it.id }) { pkg ->
            val isSelected = selectedPackage == pkg
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Sapphire50 else SurfaceWhite
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AccentBlue else BorderDefault
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPackage = pkg }
                    .testTag("donation_package_${pkg.id.lowercase()}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = pkg.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pkg == DonationPackage.LIFETIME) Emerald700 else AccentBlueSoft
                                ) {
                                    Text(
                                        text = pkg.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pkg == DonationPackage.LIFETIME) Color.White else AccentBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pkg.priceLabel,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) AccentBlue else Emerald700
                                ).tabular()
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Dipilih",
                                tint = AccentBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Text(
                        text = pkg.summary,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700, lineHeight = 18.sp)
                    )

                    HorizontalDivider(color = BorderDefault)

                    Text(
                        text = "Fitur Unggulan KASLITE yang Didapat:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pkg.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate700,
                                        lineHeight = 17.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Rekening Tujuan Donasi Manual
        item {
            Text(
                text = "2. Transfer Donasi Manual ke Rekening Pengembang",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                            text = "Tujuan Transfer (${selectedPackage.priceLabel})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                    }

                    if (bankConfig.isConfigured) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald50,
                            border = BorderStroke(1.dp, Emerald200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = bankConfig.bankName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald800
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bankConfig.accountNumber,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate900
                                            ).tabular()
                                        )
                                        Text(
                                            text = "a.n. ${bankConfig.accountHolder}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Slate700,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(bankConfig.accountNumber))
                                            formMessage = true to "Nomor rekening ${bankConfig.accountNumber} disalin!"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Salin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = Emerald200)
                                Text(
                                    text = "Nominal Donasi: ${KasLiteSupportEmailHelper.formatIdr(selectedPackage.amountRupiah)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald700
                                    )
                                )
                                if (bankConfig.notes.isNotBlank()) {
                                    Text(
                                        text = bankConfig.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberWarningBg,
                            border = BorderStroke(1.dp, AmberWarningBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Rekening Pengembang Belum Ditampilkan",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Text(
                                    text = "Admin pengembang (${bankConfig.supportEmail.ifBlank { SUPER_ADMIN_EMAIL }}) dapat mengisi nomor rekening tujuan melalui menu Super Admin -> Rekening & Email, atau Anda dapat mengirim konfirmasi email langsung ke admin.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Form Konfirmasi Transfer Donasi
        item {
            Text(
                text = "3. Konfirmasi Transfer & Email Penerima Kunci",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Setelah transfer, isi form konfirmasi berikut. Admin akan memverifikasi dan menekan tombol Sudah Dibayar agar kunci lisensi dikirim ke email Anda.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )

                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it.trim() },
                        label = { Text("Email Penerima Kunci Lisensi *") },
                        placeholder = { Text("emailanda@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Nama Pemilik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = senderBankName,
                            onValueChange = { senderBankName = it },
                            label = { Text("Bank Pengirim *") },
                            placeholder = { Text("BRI / BCA / DANA") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = senderAccountName,
                            onValueChange = { senderAccountName = it },
                            label = { Text("Nama Pengirim *") },
                            placeholder = { Text("Sesuai mutasi") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("Catatan / No. Referensi / Jam Transfer") },
                        placeholder = { Text("Contoh: Transfer Rp ${selectedPackage.amountRupiah / 1000}rb jam 14:20") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = formMessage != null) {
                        formMessage?.let { (isOk, msg) ->
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
                            formMessage = null
                            viewModel.submitCustomerDonationRequest(
                                pkg = selectedPackage,
                                recipientEmail = recipientEmail,
                                ownerName = ownerName,
                                storeName = storeName,
                                phone = phone,
                                senderBankName = senderBankName,
                                senderAccountName = senderAccountName,
                                transferNote = transferNote,
                                onRequireAuth = { showSupabaseVerifyDialog = true },
                                onResult = { ok, msg, _ ->
                                    formMessage = ok to msg
                                    if (ok) {
                                        transferNote = ""
                                    }
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Kirim Konfirmasi Donasi (${selectedPackage.priceLabel})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (bankConfig.whatsappNumber.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val waDigits = bankConfig.whatsappNumber.filter { it.isDigit() }.let { raw ->
                                    if (raw.startsWith("0")) "62" + raw.drop(1) else raw
                                }
                                val waMsg = buildString {
                                    appendLine("Halo Pengembang KASLITE, saya baru mengirim konfirmasi donasi:")
                                    appendLine("• Paket: ${selectedPackage.title} (${selectedPackage.priceLabel})")
                                    appendLine("• Email Lisensi: $recipientEmail")
                                    appendLine("• Toko: ${storeName.ifBlank { "-" }}")
                                    appendLine("• Pengirim: $senderBankName - $senderAccountName")
                                    if (transferNote.isNotBlank()) appendLine("• Catatan: $transferNote")
                                }
                                val uri = Uri.parse("https://wa.me/$waDigits?text=${Uri.encode(waMsg)}")
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.onFailure {
                                    formMessage = false to "Tidak dapat membuka WhatsApp di perangkat ini."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Konfirmasi Tambahan via WhatsApp Admin", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section 4: Riwayat Donasi Saya
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Riwayat Konfirmasi Donasi Saya (${myDonations.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                }
            }
        }

        if (myDonations.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada riwayat konfirmasi donasi pada akun ini.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(myDonations, key = { it.id.ifBlank { "${it.packageId}_${it.createdAtMillis}" } }) { item ->
                val (badgeLabel, badgeType) = when (item.status.uppercase()) {
                    "PAID" -> "Sudah Dibayar • Cek Email" to BadgeType.SUCCESS
                    "REJECTED" -> "Ditolak" to BadgeType.DANGER
                    else -> "Menunggu Verifikasi" to BadgeType.WARNING
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
                                text = item.packageTitle.ifBlank { DonationPackage.fromId(item.packageId).title },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            DesignStatusBadge(text = badgeLabel, type = badgeType)
                        }
                        Text(
                            text = "${KasLiteSupportEmailHelper.formatIdr(item.amountRupiah)} • ke email ${item.userEmail}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Pengirim: ${item.senderBankName} (${item.senderAccountName})",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                        if (item.createdAtMillis > 0L) {
                            Text(
                                text = "Dikirim: ${dateFormat.format(Date(item.createdAtMillis))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                            )
                        }
                        if (item.status.equals("PAID", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald50,
                                border = BorderStroke(1.dp, Emerald200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Donasi terverifikasi! Kunci lisensi penuh telah dikirim ke email ${item.userEmail}. Cek Kotak Masuk / Spam lalu masukkan kuncinya di menu Pengaturan -> Aktivasi Kunci Lisensi.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Emerald800)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(Screen.PENGATURAN) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buka Pengaturan Aktivasi Kunci", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (item.adminNotes.isNotBlank()) {
                            Text(
                                text = "Catatan Admin: ${item.adminNotes}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Rose500)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showSupabaseVerifyDialog) {
        SupportSupabaseAuthDialog(
            initialEmail = recipientEmail.ifBlank { authUiState.lastEmail },
            viewModel = viewModel,
            onDismiss = { showSupabaseVerifyDialog = false },
            onAuthenticated = {
                showSupabaseVerifyDialog = false
                viewModel.loadCustomerSupportData()
                formMessage = true to "Sesi akun terverifikasi! Silakan tekan Kirim kembali."
            }
        )
    }
}

@Composable
internal fun SupportSupabaseAuthDialog(
    initialEmail: String,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Verifikasi Akun Supabase",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Text(
                    text = "Masukkan Email & Password akun KASLITE Anda untuk mengirim data secara aman ke server pengembang.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim(); localError = null },
                    label = { Text("Email Terdaftar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text("Password Akun") },
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPass) "Sembunyikan" else "Lihat"
                            )
                        }
                    },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!localError.isNullOrBlank()) {
                    Text(localError ?: "", color = Rose500, style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                localError = "Email & password wajib diisi."
                                return@Button
                            }
                            isBusy = true
                            localError = null
                            viewModel.refreshCustomerLicenseOnline(email, password) { ok, msg ->
                                isBusy = false
                                if (ok) onAuthenticated() else localError = msg
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
                            Text("Verifikasi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
