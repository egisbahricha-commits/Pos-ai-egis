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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CustomerEntity
import com.example.data.entity.SupplierEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun ContactsScreen(viewModel: PosViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, SUPPLIER
    var showAddDialog by remember { mutableStateOf(false) }
    var topUpCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    Scaffold(
        containerColor = BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kontak")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCanvas)
                .padding(innerPadding)
                .testTag("contacts_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(16.dp)
            ) {
                DesignSegmentedTabs(
                    options = listOf(
                        "CUSTOMER" to "Pelanggan (${customers.size})",
                        "SUPPLIER" to "Pemasok (${suppliers.size})"
                    ),
                    selectedKey = activeTab,
                    onSelect = { activeTab = it }
                )
            }
            HorizontalDivider(color = BorderDefault)

            if (activeTab == "CUSTOMER") {
                if (customers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.Person,
                            title = "Belum ada pelanggan",
                            subtitle = "Tambahkan kontak pelanggan untuk memantau transaksi dan piutang.",
                            ctaText = "Tambah pelanggan",
                            onCtaClick = { showAddDialog = true }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(customers, key = { it.id }) { c ->
                            val loyaltyTier = when {
                                c.loyaltyPoints >= 100 -> "VIP"
                                c.loyaltyPoints >= 50 -> "Gold"
                                c.loyaltyPoints >= 20 -> "Silver"
                                else -> "Bronze"
                            }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = BorderStroke(1.dp, BorderDefault),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceStrong),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(c.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    DesignStatusBadge(text = "$loyaltyTier (${c.loyaltyPoints} pt)", type = BadgeType.INFO)
                                                    if (c.totalReceivable > 0) {
                                                        DesignStatusBadge(
                                                            text = "Piutang Rp ${formatRupiah(c.totalReceivable)}",
                                                            type = BadgeType.DANGER
                                                        )
                                                    }
                                                }
                                            }
                                            Text(c.phone.ifBlank { "Tanpa nomor HP" }, style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                                            if (c.address.isNotBlank()) Text(c.address, style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Deposit: Rp ${formatRupiah(c.depositBalance)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                                        )
                                        OutlinedButton(
                                            onClick = { topUpCustomer = c },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, BorderDefault),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Top Up Deposit", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (suppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.Business,
                            title = "Belum ada pemasok",
                            subtitle = "Tambahkan pemasok atau grosir untuk memudahkan kulakan stok.",
                            ctaText = "Tambah pemasok",
                            onCtaClick = { showAddDialog = true }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(suppliers, key = { it.id }) { s ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = BorderStroke(1.dp, BorderDefault),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceStrong),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = ModuleProduk, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(s.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                            if (s.totalDebt > 0) {
                                                DesignStatusBadge(
                                                    text = "Utang Rp ${formatRupiah(s.totalDebt)}",
                                                    type = BadgeType.DANGER
                                                )
                                            }
                                        }
                                        Text(s.phone.ifBlank { "Tanpa kontak" }, style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                                        if (s.address.isNotBlank()) Text(s.address, style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (topUpCustomer != null) {
        val cust = topUpCustomer!!
        var amountText by remember(cust.id) { mutableStateOf("50000") }
        Dialog(onDismissRequest = { topUpCustomer = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Top Up Deposit: ${cust.name}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text("Saldo deposit saat ini: Rp ${formatRupiah(cust.depositBalance)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                    Spacer(modifier = Modifier.height(12.dp))
                    DesignLabeledTextField(
                        label = "Nominal Deposit Masuk",
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                        prefixText = "Rp ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { topUpCustomer = null }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.topUpCustomerDeposit(cust, amt)
                                    topUpCustomer = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simpan Deposit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            isCustomer = activeTab == "CUSTOMER",
            onDismiss = { showAddDialog = false },
            onSaveCustomer = { name, phone, address, notes ->
                viewModel.saveCustomer(CustomerEntity(name = name, phone = phone, address = address, notes = notes))
                showAddDialog = false
            },
            onSaveSupplier = { name, phone, address, notes ->
                viewModel.saveSupplier(SupplierEntity(name = name, phone = phone, address = address, notes = notes))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddContactDialog(
    isCustomer: Boolean,
    onDismiss: () -> Unit,
    onSaveCustomer: (name: String, phone: String, addr: String, notes: String) -> Unit,
    onSaveSupplier: (name: String, phone: String, addr: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (isCustomer) "Tambah Pelanggan Baru" else "Tambah Pemasok Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Spacer(modifier = Modifier.height(14.dp))

                DesignLabeledTextField(
                    label = "Nama Lengkap *",
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    isError = showError && name.isBlank(),
                    errorMessage = if (showError && name.isBlank()) "Nama wajib diisi" else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "No. HP / WhatsApp",
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Alamat",
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Catatan",
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                showError = true
                            } else {
                                if (isCustomer) onSaveCustomer(name.trim(), phone.trim(), address.trim(), notes.trim())
                                else onSaveSupplier(name.trim(), phone.trim(), address.trim(), notes.trim())
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
