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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.PurchaseItemEntity
import com.example.data.entity.SupplierEntity
import com.example.data.repository.PurchaseItemDraft
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseScreen(viewModel: PosViewModel) {
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    var selectedPurchaseDetail by remember { mutableStateOf<PurchaseEntity?>(null) }
    var detailItems by remember { mutableStateOf<List<PurchaseItemEntity>>(emptyList()) }

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
                            onClick = {
                                selectedPurchaseDetail = purchase
                                coroutineScope.launch {
                                    detailItems = viewModel.getItemsForPurchase(purchase.id)
                                }
                            },
                            onConfirmReceipt = { viewModel.confirmGoodsReceipt(purchase.id) },
                            onCancel = { viewModel.cancelPurchase(purchase.id) }
                        )
                    }
                }
            }
        }
    }

    if (selectedPurchaseDetail != null) {
        val p = selectedPurchaseDetail!!
        Dialog(onDismissRequest = { selectedPurchaseDetail = null }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detail Pembelian", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        IconButton(onClick = { selectedPurchaseDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }
                    HorizontalDivider(color = BorderDefault)
                    Text("${p.invoiceNumber} • ${p.supplierName}", fontWeight = FontWeight.Bold, color = AccentBlue)
                    Text("Metode: ${p.paymentMethod} (${p.paymentStatus}) • Status: ${p.status}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    Surface(
                        color = BgCanvas,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (detailItems.isEmpty()) {
                                Text("Tidak ada detail item.", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            } else {
                                detailItems.forEach { itm ->
                                    val qtyTxt = if (itm.quantity % 1.0 == 0.0) itm.quantity.toLong().toString() else itm.quantity.toString()
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${itm.productName} ($qtyTxt ${itm.unit})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text("Rp ${formatRupiah(itm.subtotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            if (p.discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diskon", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                                    Text("-Rp ${formatRupiah(p.discount)}", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                                }
                            }
                            if (p.additionalCost > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Biaya Tambahan", style = MaterialTheme.typography.bodySmall, color = Slate600)
                                    Text("+Rp ${formatRupiah(p.additionalCost)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = BorderDefault)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Akhir", fontWeight = FontWeight.Bold)
                                Text("Rp ${formatRupiah(p.finalAmount)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (p.notes.isNotBlank()) {
                        Text("Catatan: ${p.notes}", style = MaterialTheme.typography.bodySmall, color = Slate600)
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
    onClick: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
    val physicalProducts = remember(products) {
        products.filter { it.productType != "DIGITAL" && !it.hasNoStock && !it.isUnlimitedStock }.ifEmpty { products }
    }
    var invoiceNumber by remember { mutableStateOf("") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var paymentMethod by remember { mutableStateOf("TUNAI") } // TUNAI, KREDIT
    var initialStatus by remember { mutableStateOf("DRAFT") } // DRAFT, DITERIMA
    var discountText by remember { mutableStateOf("") }
    var additionalCostText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }

    // Item addition row
    var selectedProduct by remember { mutableStateOf(physicalProducts.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var unitText by remember { mutableStateOf(physicalProducts.firstOrNull()?.baseUnit ?: "pcs") }
    var priceText by remember { mutableStateOf(physicalProducts.firstOrNull()?.purchasePrice?.let { "${it.toLong()}" } ?: "") }

    val itemsList = remember { mutableStateListOf<PurchaseItemDraft>() }
    val matchingProducts = remember(physicalProducts, productSearch) {
        if (productSearch.isBlank()) physicalProducts
        else physicalProducts.filter { it.name.contains(productSearch, ignoreCase = true) || it.barcode.contains(productSearch, ignoreCase = true) }
    }
    val itemsSubtotal = itemsList.sumOf { it.quantity * it.purchasePrice }
    val discVal = discountText.toDoubleOrNull() ?: 0.0
    val addVal = additionalCostText.toDoubleOrNull() ?: 0.0
    val grandTotal = (itemsSubtotal - discVal + addVal).coerceAtLeast(0.0)

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
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suppliers, key = { it.id }) { s ->
                            DesignFilterChip(
                                label = s.name,
                                selected = selectedSupplier?.id == s.id,
                                onClick = { selectedSupplier = s }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Metode Pembayaran:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TUNAI" to "Tunai (Potong Kas)", "KREDIT" to "Kredit / Tempo (Utang)").forEach { (method, label) ->
                            val isSel = paymentMethod == method
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Sapphire800 else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { paymentMethod = method }
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tambah Barang Belanja:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = productSearch,
                        onValueChange = { productSearch = it },
                        label = { Text("Cari nama barang / barcode...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingProducts, key = { it.id }) { p ->
                            DesignFilterChip(
                                label = p.name.take(20),
                                selected = selectedProduct?.id == p.id,
                                onClick = {
                                    selectedProduct = p
                                    unitText = p.baseUnit
                                    priceText = "${p.purchasePrice.toLong()}"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { raw -> qtyText = raw.filter { it.isDigit() || it == '.' } },
                            label = { Text("Jumlah") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            onValueChange = { raw -> priceText = raw.filter { it.isDigit() } },
                            label = { Text("Harga Beli") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f)
                        )
                        Button(
                            onClick = {
                                val p = selectedProduct
                                val q = qtyText.toDoubleOrNull() ?: 0.0
                                val pr = priceText.toDoubleOrNull() ?: 0.0
                                if (p != null && q > 0 && unitText.isNotBlank()) {
                                    itemsList.add(
                                        PurchaseItemDraft(
                                            productId = p.id,
                                            productName = p.name,
                                            quantity = q,
                                            unit = unitText.trim(),
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
                        val qtyTxt = if (itm.quantity % 1.0 == 0.0) itm.quantity.toLong().toString() else itm.quantity.toString()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${idx + 1}. ${itm.productName} ($qtyTxt ${itm.unit})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Rp ${formatRupiah(itm.quantity * itm.purchasePrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { itemsList.removeAt(idx) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus item", tint = Rose500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { raw -> discountText = raw.filter { it.isDigit() } },
                            label = { Text("Diskon (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = additionalCostText,
                            onValueChange = { raw -> additionalCostText = raw.filter { it.isDigit() } },
                            label = { Text("Ongkir / Biaya") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Pembelian") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Belanja:", fontWeight = FontWeight.SemiBold)
                        Text("Rp ${formatRupiah(grandTotal)}", fontWeight = FontWeight.Bold, color = Sapphire800)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Status Pembelian Awal:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
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
                                    onCreate(
                                        invoiceNumber.trim(),
                                        selectedSupplier,
                                        itemsList.toList(),
                                        paymentMethod,
                                        discVal,
                                        addVal,
                                        notes.trim(),
                                        initialStatus
                                    )
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
