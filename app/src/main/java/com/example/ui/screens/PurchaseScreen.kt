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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SupplierEntity
import com.example.data.repository.PurchaseItemDraft
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseScreen(viewModel: PosViewModel) {
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("SEMUA") }

    val filteredPurchases = remember(purchases, selectedFilter) {
        if (selectedFilter == "SEMUA") purchases
        else purchases.filter { it.status == selectedFilter }
    }

    Scaffold(
        containerColor = BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_purchase_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pembelian")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCanvas)
                .padding(innerPadding)
                .testTag("purchase_screen")
        ) {
            // Header Info Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceStrong),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = ModuleProduk, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Pembelian & Belanja Toko",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            Text(
                                "Stok bertambah saat penerimaan barang dikonfirmasi fisik.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                    }
                }

                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("SEMUA", "DRAFT", "DIPESAN", "DITERIMA", "DIBATALKAN")) { filter ->
                        DesignFilterChip(
                            label = filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderDefault)

            // Purchase List
            if (filteredPurchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    DesignEmptyState(
                        icon = Icons.Default.ShoppingBag,
                        title = "Belum ada data pembelian",
                        subtitle = "Catat kulakan atau belanja stok dari pemasok.",
                        ctaText = "Tambah pembelian",
                        onCtaClick = { showCreateDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPurchases, key = { it.id }) { purchase ->
                        PurchaseItemCard(
                            purchase = purchase,
                            onConfirmReceipt = { viewModel.confirmGoodsReceipt(purchase.id) },
                            onCancel = { viewModel.cancelPurchase(purchase.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePurchaseDialog(
            suppliers = suppliers,
            products = products,
            onDismiss = { showCreateDialog = false },
            onCreate = { inv, supp, items, method, disc, addCost, notes, initialStatus ->
                viewModel.createPurchaseOrder(inv, supp, items, method, disc, addCost, notes, initialStatus)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun PurchaseItemCard(
    purchase: PurchaseEntity,
    onConfirmReceipt: () -> Unit,
    onCancel: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(purchase.date))

    val badgeType = when (purchase.status) {
        "DITERIMA" -> BadgeType.SUCCESS
        "DIPESAN" -> BadgeType.INFO
        "DRAFT" -> BadgeType.WARNING
        "DIBATALKAN" -> BadgeType.DANGER
        else -> BadgeType.NORMAL
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(purchase.invoiceNumber, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text(
                        "${purchase.supplierName} • $dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                }

                DesignStatusBadge(text = purchase.status, type = badgeType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Belanja", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                Text(
                    "Rp ${formatRupiah(purchase.finalAmount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${purchase.paymentMethod} (${purchase.paymentStatus})", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                if (purchase.notes.isNotBlank()) {
                    Text(purchase.notes, style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                }
            }

            // Action row for Goods Receipt
            if (purchase.status != "DITERIMA" && purchase.status != "DIBATALKAN") {
                HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batalkan", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirmReceipt,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terima Barang (+Stok)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePurchaseDialog(
    suppliers: List<SupplierEntity>,
    products: List<com.example.data.entity.ProductEntity>,
    onDismiss: () -> Unit,
    onCreate: (inv: String, supp: SupplierEntity?, items: List<PurchaseItemDraft>, method: String, disc: Double, addCost: Double, notes: String, initialStatus: String) -> Unit
) {
    var invoiceNumber by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var paymentMethod by remember { mutableStateOf("TUNAI") } // TUNAI, KREDIT
    var initialStatus by remember { mutableStateOf("DRAFT") } // DRAFT, DITERIMA
    var notes by remember { mutableStateOf("") }

    // Item addition row
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var unitText by remember { mutableStateOf(products.firstOrNull()?.baseUnit ?: "pcs") }
    var priceText by remember { mutableStateOf(products.firstOrNull()?.purchasePrice?.let { "${it.toLong()}" } ?: "") }

    val itemsList = remember { mutableStateListOf<PurchaseItemDraft>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text("Catat Pembelian Belanja", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("No. Faktur / Nota (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Pemasok:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suppliers.forEach { s ->
                            val isSelected = selectedSupplier?.id == s.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Sapphire800 else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedSupplier = s }
                            ) {
                                Text(
                                    s.name,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tambah Barang Belanja:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Product quick select chips
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        products.take(4).forEach { p ->
                            val isSelected = selectedProduct?.id == p.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Sapphire600 else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedProduct = p
                                    unitText = p.baseUnit
                                    priceText = "${p.purchasePrice.toLong()}"
                                }
                            ) {
                                Text(
                                    p.name.take(15),
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it },
                            label = { Text("Jumlah") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitText,
                            onValueChange = { unitText = it },
                            label = { Text("Satuan") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Harga Beli") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                        Button(
                            onClick = {
                                val p = selectedProduct
                                val q = qtyText.toDoubleOrNull() ?: 1.0
                                val pr = priceText.toDoubleOrNull() ?: 0.0
                                if (p != null) {
                                    itemsList.add(
                                        PurchaseItemDraft(
                                            productId = p.id,
                                            productName = p.name,
                                            quantity = q,
                                            unit = unitText,
                                            purchasePrice = pr
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("+")
                        }
                    }

                    // Display added items
                    Spacer(modifier = Modifier.height(10.dp))
                    itemsList.forEachIndexed { idx, itm ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${idx + 1}. ${itm.productName} (${itm.quantity.toInt()} ${itm.unit})", style = MaterialTheme.typography.bodySmall)
                            Text("Rp ${formatRupiah(itm.quantity * itm.purchasePrice)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Status Pembelian Awal:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DRAFT" to "Draft (Stok Belum Masuk)", "DITERIMA" to "Langsung Terima (+Stok)").forEach { (status, label) ->
                            val isSel = initialStatus == status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Sapphire800 else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { initialStatus = status }
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (itemsList.isNotEmpty()) {
                                    onCreate(invoiceNumber, selectedSupplier, itemsList, paymentMethod, 0.0, 0.0, notes, initialStatus)
                                }
                            },
                            enabled = itemsList.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Sapphire800)
                        ) {
                            Text("Simpan Pembelian")
                        }
                    }
                }
            }
        }
    }
}
