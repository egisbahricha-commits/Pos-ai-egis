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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.components.KasGoSearchBar
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun ContactsScreen(viewModel: PosViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, SUPPLIER
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var supplierToDelete by remember { mutableStateOf<SupplierEntity?>(null) }
    var topUpCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    editingSupplier = null
                    showAddDialog = true
                },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KasGoSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari nama atau no. HP kontak...",
                    onBarcodeScanClick = null
                )

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
                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.Person,
                            title = if (searchQuery.isBlank()) "Belum ada pelanggan" else "Pelanggan tidak ditemukan",
                            subtitle = "Tambahkan kontak pelanggan untuk memantau transaksi dan piutang.",
                            ctaText = "Tambah pelanggan",
                            onCtaClick = {
                                editingCustomer = null
                                editingSupplier = null
                                showAddDialog = true
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCustomers, key = { it.id }) { c ->
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = { topUpCustomer = c },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, BorderDefault),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Top Up", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                            }
                                            IconButton(onClick = {
                                                editingCustomer = c
                                                editingSupplier = null
                                                showAddDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Ubah pelanggan", tint = Slate600, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { customerToDelete = c }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus pelanggan", tint = Rose500, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredSuppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.Business,
                            title = if (searchQuery.isBlank()) "Belum ada pemasok" else "Pemasok tidak ditemukan",
                            subtitle = "Tambahkan pemasok atau grosir untuk memudahkan kulakan stok.",
                            ctaText = "Tambah pemasok",
                            onCtaClick = {
                                editingCustomer = null
                                editingSupplier = null
                                showAddDialog = true
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredSuppliers, key = { it.id }) { s ->
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
                                    IconButton(onClick = {
                                        editingSupplier = s
                                        editingCustomer = null
                                        showAddDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah pemasok", tint = Slate600, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { supplierToDelete = s }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus pemasok", tint = Rose500, modifier = Modifier.size(18.dp))
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

    if (customerToDelete != null) {
        val cust = customerToDelete!!
        val hasBalance = cust.totalReceivable > 0 || cust.depositBalance > 0
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Hapus Pelanggan?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (hasBalance) "${cust.name} masih memiliki piutang Rp ${formatRupiah(cust.totalReceivable)} atau deposit Rp ${formatRupiah(cust.depositBalance)}. Lunasi/kosongkan terlebih dahulu sebelum menghapus."
                    else "Yakin ingin menghapus data pelanggan \"${cust.name}\"?"
                )
            },
            confirmButton = {
                if (!hasBalance) {
                    Button(
                        onClick = {
                            viewModel.deleteCustomer(cust)
                            customerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White)
                    ) {
                        Text("Hapus", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) { Text(if (hasBalance) "Mengerti" else "Batal") }
            }
        )
    }

    if (supplierToDelete != null) {
        val supp = supplierToDelete!!
        val hasDebt = supp.totalDebt > 0
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("Hapus Pemasok?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (hasDebt) "${supp.name} masih memiliki sisa utang Rp ${formatRupiah(supp.totalDebt)}. Lunasi utang terlebih dahulu sebelum menghapus."
                    else "Yakin ingin menghapus data pemasok \"${supp.name}\"?"
                )
            },
            confirmButton = {
                if (!hasDebt) {
                    Button(
                        onClick = {
                            viewModel.deleteSupplier(supp)
                            supplierToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White)
                    ) {
                        Text("Hapus", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) { Text(if (hasDebt) "Mengerti" else "Batal") }
            }
        )
    }

    if (showAddDialog) {
        val isCustMode = editingCustomer != null || (editingSupplier == null && activeTab == "CUSTOMER")
        AddContactDialog(
            isCustomer = isCustMode,
            existingCustomer = editingCustomer,
            existingSupplier = editingSupplier,
            onDismiss = {
                showAddDialog = false
                editingCustomer = null
                editingSupplier = null
            },
            onSaveCustomer = { name, phone, address, notes ->
                val base = editingCustomer ?: CustomerEntity(name = name)
                viewModel.saveCustomer(base.copy(name = name, phone = phone, address = address, notes = notes))
                showAddDialog = false
                editingCustomer = null
            },
            onSaveSupplier = { name, phone, address, notes ->
                val base = editingSupplier ?: SupplierEntity(name = name)
                viewModel.saveSupplier(base.copy(name = name, phone = phone, address = address, notes = notes))
                showAddDialog = false
                editingSupplier = null
            }
        )
    }
}

@Composable
fun AddContactDialog(
    isCustomer: Boolean,
    existingCustomer: CustomerEntity? = null,
    existingSupplier: SupplierEntity? = null,
    onDismiss: () -> Unit,
    onSaveCustomer: (name: String, phone: String, addr: String, notes: String) -> Unit,
    onSaveSupplier: (name: String, phone: String, addr: String, notes: String) -> Unit
) {
    val key = existingCustomer?.id ?: existingSupplier?.id ?: if (isCustomer) -1L else -2L
    var name by remember(key) { mutableStateOf(existingCustomer?.name ?: existingSupplier?.name ?: "") }
    var phone by remember(key) { mutableStateOf(existingCustomer?.phone ?: existingSupplier?.phone ?: "") }
    var address by remember(key) { mutableStateOf(existingCustomer?.address ?: existingSupplier?.address ?: "") }
    var notes by remember(key) { mutableStateOf(existingCustomer?.notes ?: existingSupplier?.notes ?: "") }
    var showError by remember(key) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    when {
                        existingCustomer != null -> "Ubah Data Pelanggan"
                        existingSupplier != null -> "Ubah Data Pemasok"
                        isCustomer -> "Tambah Pelanggan Baru"
                        else -> "Tambah Pemasok Baru"
                    },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
