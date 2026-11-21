package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CashAccountEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.KasGoSectionHeader
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun MultiKasScreen(viewModel: PosViewModel) {
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val cashTransfers by viewModel.cashTransfers.collectAsStateWithLifecycle()
    val cashMutations by viewModel.cashMutations.collectAsStateWithLifecycle()

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showInactiveAccounts by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<CashAccountEntity?>(null) }
    var activeLedgerTab by remember { mutableStateOf("MUTASI") } // MUTASI, TRANSFER

    val filteredAccounts = remember(cashAccounts, showInactiveAccounts) {
        if (showInactiveAccounts) cashAccounts else cashAccounts.filter { it.isActive }
    }

    val totalBalance = remember(cashAccounts) {
        cashAccounts.filter { it.isActive }.sumOf { it.balance }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("multi_kas_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MULTI KAS HERO CARD (Ocean Teal #0E7490, matches Design System Photo)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ModuleKas),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total semua kas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${if (totalBalance < 0) "-" else ""}Rp ${formatRupiah(abs(totalBalance))}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 32.sp
                        )
                    )

                    if (filteredAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            filteredAccounts.take(3).forEach { acc ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.accountName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.78f),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (acc.balance < 0) "-" else ""}Rp ${formatRupiah(abs(acc.balance))}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Bar (Transfer Kas + Tambah Akun)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showTransferDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transfer Kas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showCreateAccountDialog = true },
                    border = BorderStroke(1.dp, BorderInput),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = Slate800),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Akun", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Account List Header + Chip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar kas (${filteredAccounts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                DesignFilterChip(
                    label = "Nonaktif",
                    selected = showInactiveAccounts,
                    onClick = { showInactiveAccounts = !showInactiveAccounts }
                )
            }
        }

        // Account List Container (Clean Card Matching Design System Photo)
        if (filteredAccounts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DesignEmptyState(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Belum ada akun kas",
                        subtitle = "Tambahkan Kas Toko, Saldo BRILink, atau Deposit Pulsa.",
                        ctaText = "Tambah Akun",
                        onCtaClick = { showCreateAccountDialog = true }
                    )
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        filteredAccounts.forEachIndexed { index, account ->
                            val isNegative = account.balance < 0
                            val icon = when (account.accountType) {
                                "CASH" -> Icons.Default.Store
                                "PULSA_DEPOSIT" -> Icons.Default.PhoneAndroid
                                "BANK_AGENT" -> Icons.Default.AccountBalance
                                else -> Icons.Default.AccountBalanceWallet
                            }
                            val typeLabel = when (account.accountType) {
                                "CASH" -> "Tunai"
                                "PULSA_DEPOSIT" -> "Layanan"
                                "BANK_AGENT" -> "Layanan"
                                else -> "Bank"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = account.accountName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                            )
                                            if (account.isDefault) {
                                                DesignStatusBadge(text = "Utama", type = BadgeType.INFO)
                                            }
                                            if (!account.isActive) {
                                                DesignStatusBadge(text = "Nonaktif", type = BadgeType.NORMAL)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isNegative) "-" else ""}Rp ${formatRupiah(abs(account.balance))}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isNegative) Rose500 else Slate900,
                                                fontSize = 15.sp
                                            )
                                        )
                                        if (isNegative) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            DesignStatusBadge(text = "Saldo minus", type = BadgeType.DANGER)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!account.isDefault) {
                                        OutlinedButton(
                                            onClick = { viewModel.setDefaultCashAccount(account.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, BorderDefault),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Amber500, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Jadikan Utama", fontSize = 11.sp, color = Slate700)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { accountToAdjust = account },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderDefault),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Koreksi Saldo", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (index < filteredAccounts.lastIndex) {
                                HorizontalDivider(color = BorderDefault)
                            }
                        }
                    }
                }
            }
        }

        // Ledger Tabs: Mutasi Kas vs Riwayat Transfer
        item {
            Spacer(modifier = Modifier.height(6.dp))
            KasGoSectionHeader(
                title = "Buku Mutasi & Transfer Kas",
                subtitle = "Catatan append-only setiap arus dana masuk/keluar"
            )
            Spacer(modifier = Modifier.height(8.dp))
            DesignSegmentedTabs(
                options = listOf(
                    "MUTASI" to "Mutasi Kas (${cashMutations.size})",
                    "TRANSFER" to "Transfer (${cashTransfers.size})"
                ),
                selectedKey = activeLedgerTab,
                onSelect = { activeLedgerTab = it }
            )
        }

        if (activeLedgerTab == "MUTASI") {
            if (cashMutations.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada mutasi kas tercatat.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(cashMutations.take(30), key = { it.id }) { mut ->
                    val isIn = mut.amount >= 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(mut.accountName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                    DesignStatusBadge(
                                        text = mut.mutationType.uppercase(),
                                        type = if (isIn) BadgeType.SUCCESS else BadgeType.WARNING
                                    )
                                }
                                Text(
                                    mut.notes.ifBlank { mut.refType },
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp)
                                )
                                Text(
                                    SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(mut.timestamp)),
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                                )
                            }
                            Text(
                                text = "${if (isIn) "+" else "-"}Rp ${formatRupiah(abs(mut.amount))}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIn) Emerald700 else Rose500
                                )
                            )
                        }
                    }
                }
            }
        } else {
            if (cashTransfers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada riwayat transfer kas.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(cashTransfers.take(15)) { transfer ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(SurfaceStrong),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = ModuleKas,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${transfer.fromAccountName} -> ${transfer.toAccountName}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(transfer.date)) +
                                            if (transfer.notes.isNotBlank()) " • ${transfer.notes}" else "",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                )
                            }
                            Text(
                                text = "Rp ${formatRupiah(transfer.amount)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ModuleKas
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (accountToAdjust != null) {
        val acc = accountToAdjust!!
        var balanceText by remember(acc.id) { mutableStateOf("${acc.balance.toLong()}") }
        var reason by remember(acc.id) { mutableStateOf("Koreksi Saldo Manual") }

        Dialog(onDismissRequest = { accountToAdjust = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Koreksi Saldo: ${acc.accountName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Saldo saat ini: Rp ${formatRupiah(acc.balance)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                    Spacer(modifier = Modifier.height(12.dp))
                    DesignLabeledTextField(
                        label = "Saldo Sebenarnya",
                        value = balanceText,
                        onValueChange = { raw ->
                            balanceText = raw.filterIndexed { idx, ch -> ch.isDigit() || (ch == '-' && idx == 0) }
                        },
                        prefixText = "Rp ",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Alasan Koreksi",
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { accountToAdjust = null }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsed = balanceText.toDoubleOrNull()
                                if (parsed != null) {
                                    viewModel.adjustCashAccountBalance(acc.id, parsed, reason.ifBlank { "Koreksi saldo" })
                                    accountToAdjust = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simpan Koreksi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        CreateCashAccountDialog(
            onDismiss = { showCreateAccountDialog = false },
            onCreate = { name, type, balance, notes ->
                viewModel.createCashAccount(name, type, balance, notes = notes)
                showCreateAccountDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferCashDialog(
            accounts = cashAccounts.filter { it.isActive },
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amount, notes ->
                viewModel.transferCash(fromId, toId, amount, notes)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun CreateCashAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, balance: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("CASH") }
    var initialBalanceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showNameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tambah Akun Kas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DesignLabeledTextField(
                    label = "Nama akun *",
                    value = name,
                    onValueChange = {
                        name = it
                        showNameError = false
                    },
                    placeholder = "mis. Kas Toko, Saldo BRILink",
                    isError = showNameError,
                    errorMessage = "Nama wajib diisi"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Tipe akun", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate700))
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH" to "Tunai", "PULSA_DEPOSIT" to "Pulsa", "BANK_AGENT" to "Agen Bank").forEach { (type, label) ->
                        DesignFilterChip(
                            label = label,
                            selected = accountType == type,
                            onClick = { accountType = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DesignLabeledTextField(
                    label = "Saldo awal",
                    value = initialBalanceText,
                    onValueChange = { raw ->
                        initialBalanceText = raw.filterIndexed { idx, ch -> ch.isDigit() || (ch == '-' && idx == 0) }
                    },
                    prefixText = "Rp ",
                    placeholder = "0 (boleh minus jika defisit)"
                )

                Spacer(modifier = Modifier.height(12.dp))

                DesignLabeledTextField(
                    label = "Catatan (opsional)",
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Keterangan rekening"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Slate700)
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                showNameError = true
                            } else {
                                onCreate(name.trim(), accountType, initialBalanceText.toDoubleOrNull() ?: 0.0, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TransferCashDialog(
    accounts: List<CashAccountEntity>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: Long, toId: Long, amount: Double, notes: String) -> Unit
) {
    var fromAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember(accounts) { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transfer Antar Kas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Dari akun", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate700))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accounts.forEach { acc ->
                        Surface(
                            onClick = {
                                fromAccountId = acc.id
                                if (toAccountId == acc.id) {
                                    toAccountId = accounts.firstOrNull { it.id != acc.id }?.id ?: 0L
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (fromAccountId == acc.id) AccentBlueSoft else Slate50,
                            border = BorderStroke(1.dp, if (fromAccountId == acc.id) AccentBlue else BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acc.accountName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                Text("Rp ${formatRupiah(acc.balance)}", fontSize = 12.sp, color = Slate600)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Ke akun", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate700))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accounts.filter { it.id != fromAccountId }.forEach { acc ->
                        Surface(
                            onClick = { toAccountId = acc.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (toAccountId == acc.id) AccentBlueSoft else Slate50,
                            border = BorderStroke(1.dp, if (toAccountId == acc.id) AccentBlue else BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acc.accountName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                Text("Rp ${formatRupiah(acc.balance)}", fontSize = 12.sp, color = Slate600)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DesignLabeledTextField(
                    label = "Nominal transfer *",
                    value = amountText,
                    onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
                    prefixText = "Rp ",
                    placeholder = "0"
                )

                Spacer(modifier = Modifier.height(10.dp))

                DesignLabeledTextField(
                    label = "Catatan (opsional)",
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "mis. Setor kas ke BRILink"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Slate700)
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccountId != toAccountId) {
                                onTransfer(fromAccountId, toAccountId, amt, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
