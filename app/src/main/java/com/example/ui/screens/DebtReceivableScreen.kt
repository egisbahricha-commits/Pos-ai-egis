package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SupplierEntity
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun DebtReceivableScreen(viewModel: PosViewModel) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("UTANG") } // UTANG, PIUTANG
    var selectedSupplierForPayment by remember { mutableStateOf<SupplierEntity?>(null) }
    var selectedCustomerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }

    val totalSupplierDebt = suppliers.sumOf { it.totalDebt }
    val totalCustomerReceivable = customers.sumOf { it.totalReceivable }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("debt_receivable_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Rose500),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Utang Grosir", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Rp ${formatRupiah(totalSupplierDebt)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text("${suppliers.count { it.totalDebt > 0 }} pemasok", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f)))
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ModuleLaporan),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Piutang Pelanggan", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Rp ${formatRupiah(totalCustomerReceivable)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text("${customers.count { it.totalReceivable > 0 }} pelanggan", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f)))
                    }
                }
            }

            DesignSegmentedTabs(
                options = listOf(
                    "UTANG" to "Utang Grosir",
                    "PIUTANG" to "Piutang Pelanggan"
                ),
                selectedKey = activeTab,
                onSelect = { activeTab = it }
            )
        }
        HorizontalDivider(color = BorderDefault)

        if (activeTab == "UTANG") {
            val suppliersWithDebt = suppliers.filter { it.totalDebt > 0 }
            if (suppliersWithDebt.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    DesignEmptyState(
                        icon = Icons.Default.CreditCard,
                        title = "Tidak ada utang ke pemasok",
                        subtitle = "Semua tagihan belanja ke pemasok sudah lunas."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(suppliersWithDebt, key = { it.id }) { s ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, BorderDefault),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                    Text(s.phone.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Rp ${formatRupiah(s.totalDebt)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose500))
                                }

                                Button(
                                    onClick = { selectedSupplierForPayment = s },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Bayar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val customersWithReceivable = customers.filter { it.totalReceivable > 0 }
            if (customersWithReceivable.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    DesignEmptyState(
                        icon = Icons.Default.CreditCard,
                        title = "Tidak ada piutang pelanggan",
                        subtitle = "Semua transaksi pelanggan sudah lunas."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(customersWithReceivable, key = { it.id }) { c ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, BorderDefault),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                    Text(c.phone.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Rp ${formatRupiah(c.totalReceivable)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose500))
                                }

                                Button(
                                    onClick = { selectedCustomerForPayment = c },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Terima Bayar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pay Supplier Debt Dialog
    if (selectedSupplierForPayment != null) {
        val s = selectedSupplierForPayment!!
        val defaultAcc = remember(cashAccounts) { cashAccounts.find { it.isDefault } ?: cashAccounts.firstOrNull() }
        var payAmountText by remember(s.id) { mutableStateOf("${s.totalDebt.toLong()}") }
        var selectedMethod by remember(s.id) { mutableStateOf("TUNAI") }
        var selectedAccount by remember(s.id, cashAccounts) { mutableStateOf(defaultAcc) }
        var notes by remember(s.id) { mutableStateOf("Pelunasan utang nota") }
        var showError by remember(s.id) { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedSupplierForPayment = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Bayar Utang ke Pemasok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text("${s.name} • Sisa Rp ${formatRupiah(s.totalDebt)}", style = MaterialTheme.typography.bodySmall.copy(color = Rose500, fontWeight = FontWeight.SemiBold))

                    Spacer(modifier = Modifier.height(14.dp))
                    DesignLabeledTextField(
                        label = "Jumlah Pembayaran *",
                        value = payAmountText,
                        onValueChange = { raw ->
                            payAmountText = raw.filter { it.isDigit() }
                            showError = false
                        },
                        prefixText = "Rp ",
                        isError = showError,
                        errorMessage = if (showError) "Nominal harus > Rp 0" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Metode Pembayaran", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("TUNAI", "TRANSFER", "QRIS")) { method ->
                            DesignFilterChip(
                                label = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                        }
                    }

                    if (cashAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sumber Akun Kas", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(cashAccounts, key = { it.id }) { acc ->
                                DesignFilterChip(
                                    label = acc.accountName,
                                    selected = selectedAccount?.id == acc.id,
                                    onClick = { selectedAccount = acc }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Catatan",
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedSupplierForPayment = null }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = payAmountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.paySupplierDebt(s.id, amt, "", selectedMethod, notes.trim(), selectedAccount?.id)
                                    selectedSupplierForPayment = null
                                } else {
                                    showError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Bayar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Receive Customer Receivable Dialog
    if (selectedCustomerForPayment != null) {
        val c = selectedCustomerForPayment!!
        val defaultAcc = remember(cashAccounts) { cashAccounts.find { it.isDefault } ?: cashAccounts.firstOrNull() }
        var payAmountText by remember(c.id) { mutableStateOf("${c.totalReceivable.toLong()}") }
        var selectedMethod by remember(c.id) { mutableStateOf("TUNAI") }
        var selectedAccount by remember(c.id, cashAccounts) { mutableStateOf(defaultAcc) }
        var notes by remember(c.id) { mutableStateOf("Pelunasan piutang") }
        var showError by remember(c.id) { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedCustomerForPayment = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Terima Pembayaran Piutang", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text("${c.name} • Total Rp ${formatRupiah(c.totalReceivable)}", style = MaterialTheme.typography.bodySmall.copy(color = Rose500, fontWeight = FontWeight.SemiBold))

                    Spacer(modifier = Modifier.height(14.dp))
                    DesignLabeledTextField(
                        label = "Jumlah Diterima *",
                        value = payAmountText,
                        onValueChange = { raw ->
                            payAmountText = raw.filter { it.isDigit() }
                            showError = false
                        },
                        prefixText = "Rp ",
                        isError = showError,
                        errorMessage = if (showError) "Nominal harus > Rp 0" else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Metode Pembayaran", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("TUNAI", "TRANSFER", "QRIS")) { method ->
                            DesignFilterChip(
                                label = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                        }
                    }

                    if (cashAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Masuk ke Akun Kas", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(cashAccounts, key = { it.id }) { acc ->
                                DesignFilterChip(
                                    label = acc.accountName,
                                    selected = selectedAccount?.id == acc.id,
                                    onClick = { selectedAccount = acc }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Catatan",
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedCustomerForPayment = null }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = payAmountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.payCustomerReceivable(c.id, amt, "", selectedMethod, notes.trim(), selectedAccount?.id)
                                    selectedCustomerForPayment = null
                                } else {
                                    showError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
