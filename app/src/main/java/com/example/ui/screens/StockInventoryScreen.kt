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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ProductBatchEntity
import com.example.data.entity.ProductEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatStockNumber(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        qty.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }
}

private fun filterDecimalInput(input: String): String {
    var dotSeen = false
    val sb = StringBuilder()
    for (ch in input) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !dotSeen) {
            dotSeen = true
            sb.append(ch)
        }
    }
    return sb.toString()
}

@Composable
fun StockInventoryScreen(viewModel: PosViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val movements by viewModel.stockMovements.collectAsStateWithLifecycle()
    val productBatches by viewModel.productBatches.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("STOK") } // STOK, RIWAYAT, BATCH
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showQuickStockInDialog by remember { mutableStateOf(false) }
    var showAddBatchDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val physicalProducts = remember(products) {
        products.filter { !it.isUnlimitedStock && !it.hasNoStock && it.productType != "DIGITAL" }
    }
    val totalAssetValue = remember(physicalProducts) {
        physicalProducts.sumOf { it.stock.coerceAtLeast(0.0) * it.purchasePrice }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("stock_inventory_screen")
    ) {
        // Header Strip: Asset Summary + Segmented Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ModuleProduk),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Total nilai persediaan fisik (modal)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rp ${formatRupiah(totalAssetValue)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                selectedProduct = physicalProducts.firstOrNull() ?: products.firstOrNull()
                                showQuickStockInDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ModuleProduk),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stok Masuk", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedProduct = physicalProducts.firstOrNull() ?: products.firstOrNull()
                                showAdjustDialog = true
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Opname", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            DesignSegmentedTabs(
                options = listOf(
                    "STOK" to "Status Stok",
                    "BATCH" to "Batch Expired",
                    "RIWAYAT" to "Kartu Stok"
                ),
                selectedKey = activeTab,
                onSelect = { activeTab = it }
            )
        }
        HorizontalDivider(color = BorderDefault)

        when (activeTab) {
            "STOK" -> StockStatusTabContent(
                products = products,
                onOpnameClick = { product ->
                    selectedProduct = product
                    showAdjustDialog = true
                },
                modifier = Modifier.weight(1f)
            )
            "BATCH" -> BatchExpiredTabContent(
                products = products,
                productBatches = productBatches,
                onAddBatchClick = {
                    selectedProduct = physicalProducts.firstOrNull() ?: products.firstOrNull()
                    showAddBatchDialog = true
                },
                onDeleteBatch = { batch -> viewModel.deleteProductBatch(batch) },
                modifier = Modifier.weight(1f)
            )
            else -> StockCardTabContent(
                movements = movements,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showAddBatchDialog) {
        AddBatchDialog(
            physicalProducts = physicalProducts,
            allProducts = products,
            initialProduct = selectedProduct,
            onDismiss = { showAddBatchDialog = false },
            onSave = { batch ->
                viewModel.saveProductBatch(batch)
                showAddBatchDialog = false
            }
        )
    }

    if (showQuickStockInDialog) {
        QuickStockInDialog(
            physicalProducts = physicalProducts,
            allProducts = products,
            initialProduct = selectedProduct,
            onDismiss = { showQuickStockInDialog = false },
            onSave = { productId, qty, unit, reason ->
                viewModel.quickStockIn(productId, qty, unit, reason)
                showQuickStockInDialog = false
            }
        )
    }

    if (showAdjustDialog) {
        StockOpnameDialog(
            physicalProducts = physicalProducts,
            allProducts = products,
            initialProduct = selectedProduct,
            onDismiss = { showAdjustDialog = false },
            onApply = { productId, newStock, reason ->
                viewModel.adjustStock(productId, newStock, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
private fun StockStatusTabContent(
    products: List<ProductEntity>,
    onOpnameClick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        items(products, key = { it.id }) { product ->
            val isDigital = product.isUnlimitedStock || product.hasNoStock || product.productType == "DIGITAL"
            val isMinus = !isDigital && product.stock < 0
            val isOut = !isDigital && product.stock == 0.0
            val isLow = !isDigital && product.stock > 0.0 && product.stock <= product.minStock
            val assetValue = if (isDigital) 0.0 else product.stock.coerceAtLeast(0.0) * product.purchasePrice

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
                        Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isDigital) "Layanan Digital / Stok Tak Terbatas"
                            else "Modal Rp ${formatRupiah(product.purchasePrice)} • Min ${formatStockNumber(product.minStock)} ${product.baseUnit}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                        Text(
                            if (isDigital) "Non-stok fisik"
                            else "Nilai aset: Rp ${formatRupiah(assetValue)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = AccentBlue)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        DesignStatusBadge(
                            text = when {
                                isDigital -> "Digital"
                                isMinus -> "Stok minus ${formatStockNumber(product.stock)}"
                                isOut -> "Habis"
                                else -> "${formatStockNumber(product.stock)} ${product.baseUnit}"
                            },
                            type = when {
                                isDigital -> BadgeType.INFO
                                isMinus || isOut -> BadgeType.DANGER
                                isLow -> BadgeType.WARNING
                                else -> BadgeType.SUCCESS
                            }
                        )
                        if (!isDigital) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { onOpnameClick(product) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Opname", color = AccentBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchExpiredTabContent(
    products: List<ProductEntity>,
    productBatches: List<ProductBatchEntity>,
    onAddBatchClick: () -> Unit,
    onDeleteBatch: (ProductBatchEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val monthSoonStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 30L * 86400_000L)) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        item {
            Button(
                onClick = onAddBatchClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Batch & Tanggal Kedaluwarsa", fontWeight = FontWeight.Bold)
            }
        }

        if (productBatches.isEmpty()) {
            item {
                DesignEmptyState(
                    icon = Icons.Default.EventBusy,
                    title = "Belum ada batch kedaluwarsa",
                    subtitle = "Catat batch barang berexpired agar tidak lolos terjual."
                )
            }
        } else {
            items(productBatches, key = { it.id }) { batch ->
                val prodName = products.find { it.id == batch.productId }?.name ?: "Produk #${batch.productId}"
                val isExpired = batch.expiryDateStr <= todayStr
                val isSoon = !isExpired && batch.expiryDateStr <= monthSoonStr

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, if (isExpired) Rose500.copy(alpha = 0.45f) else BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prodName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                            Text(
                                "Batch: ${batch.batchCode.ifBlank { "-" }} • Exp: ${batch.expiryDateStr}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                            )
                            Text(
                                "Jumlah: ${formatStockNumber(batch.qty)} unit",
                                style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DesignStatusBadge(
                                text = when {
                                    isExpired -> "Kedaluwarsa"
                                    isSoon -> "Segera Expired"
                                    else -> "Aman"
                                },
                                type = when {
                                    isExpired -> BadgeType.DANGER
                                    isSoon -> BadgeType.WARNING
                                    else -> BadgeType.SUCCESS
                                }
                            )
                            IconButton(onClick = { onDeleteBatch(batch) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Batch", tint = Rose500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockCardTabContent(
    movements: List<com.example.data.entity.StockMovementEntity>,
    modifier: Modifier = Modifier
) {
    if (movements.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DesignEmptyState(
                icon = Icons.Default.History,
                title = "Belum ada riwayat kartu stok",
                subtitle = "Mutasi stok masuk dan keluar akan tercatat otomatis di sini."
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
        ) {
            items(movements, key = { it.id }) { m ->
                val isPositive = m.quantityChange > 0
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) Emerald700 else Rose500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.productName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                            Text(
                                "${m.movementType} • ${SimpleDateFormat("dd/MM HH.mm", Locale.getDefault()).format(Date(m.date))}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                            if (m.notes.isNotBlank()) {
                                Text(m.notes, style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${if (isPositive) "+" else ""}${formatStockNumber(m.quantityChange)} ${m.unit}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) Emerald700 else Rose500
                                )
                            )
                            Text(
                                "Sisa ${formatStockNumber(m.currentStock)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBatchDialog(
    physicalProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>,
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductBatchEntity) -> Unit
) {
    var selectedProduct by remember(initialProduct?.id) { mutableStateOf(initialProduct) }
    var searchProduct by remember { mutableStateOf("") }
    var batchCode by remember { mutableStateOf("B-${System.currentTimeMillis() % 10000}") }
    var expiryStr by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 60L * 86400_000L)))
    }
    var qtyText by remember { mutableStateOf("10") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val candidateProducts = remember(physicalProducts, allProducts, searchProduct) {
        val base = physicalProducts.ifEmpty { allProducts }
        if (searchProduct.isBlank()) base else base.filter { it.name.contains(searchProduct, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Tambah Batch Kedaluwarsa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                Spacer(modifier = Modifier.height(10.dp))

                DesignLabeledTextField(
                    label = "Cari Produk",
                    value = searchProduct,
                    onValueChange = { searchProduct = it },
                    placeholder = "Ketik nama produk...",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidateProducts, key = { it.id }) { p ->
                        DesignFilterChip(
                            label = p.name.take(20),
                            selected = selectedProduct?.id == p.id,
                            onClick = { selectedProduct = p; errorMsg = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Kode Batch / Lot",
                    value = batchCode,
                    onValueChange = { batchCode = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Tanggal Expired (YYYY-MM-DD) *",
                    value = expiryStr,
                    onValueChange = { expiryStr = it; errorMsg = null },
                    placeholder = "2027-06-30",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Jumlah Stok Batch *",
                    value = qtyText,
                    onValueChange = { qtyText = filterDecimalInput(it); errorMsg = null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Batal", color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = selectedProduct
                            val q = qtyText.toDoubleOrNull() ?: 0.0
                            val validDate = Regex("\\d{4}-\\d{2}-\\d{2}").matches(expiryStr.trim())
                            when {
                                p == null -> errorMsg = "Pilih produk terlebih dahulu."
                                !validDate -> errorMsg = "Format tanggal harus YYYY-MM-DD (contoh: 2027-06-30)."
                                q <= 0.0 -> errorMsg = "Jumlah stok batch harus lebih dari 0."
                                else -> onSave(
                                    ProductBatchEntity(
                                        productId = p.id,
                                        batchCode = batchCode.trim(),
                                        expiryDateStr = expiryStr.trim(),
                                        qty = q
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Simpan Batch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStockInDialog(
    physicalProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>,
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, Double, String, String) -> Unit
) {
    var selectedProduct by remember(initialProduct?.id) { mutableStateOf(initialProduct) }
    var searchProduct by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("10") }
    var unitText by remember(selectedProduct?.id) { mutableStateOf(selectedProduct?.baseUnit ?: "pcs") }
    var reason by remember { mutableStateOf("Stok Masuk Cepat") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val candidateProducts = remember(physicalProducts, allProducts, searchProduct) {
        val base = physicalProducts.ifEmpty { allProducts }
        if (searchProduct.isBlank()) base else base.filter { it.name.contains(searchProduct, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Tambah Stok Masuk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                Spacer(modifier = Modifier.height(10.dp))

                DesignLabeledTextField(
                    label = "Cari Produk",
                    value = searchProduct,
                    onValueChange = { searchProduct = it },
                    placeholder = "Ketik nama produk...",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidateProducts, key = { it.id }) { p ->
                        DesignFilterChip(
                            label = p.name.take(20),
                            selected = selectedProduct?.id == p.id,
                            onClick = {
                                selectedProduct = p
                                unitText = p.baseUnit
                                errorMsg = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DesignLabeledTextField(
                    label = "Jumlah Masuk ($unitText) *",
                    value = qtyText,
                    onValueChange = { qtyText = filterDecimalInput(it); errorMsg = null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Keterangan",
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Batal", color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = selectedProduct
                            val q = qtyText.toDoubleOrNull() ?: 0.0
                            when {
                                p == null -> errorMsg = "Pilih produk terlebih dahulu."
                                q <= 0.0 -> errorMsg = "Jumlah stok masuk harus lebih dari 0."
                                else -> onSave(p.id, q, unitText, reason)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockOpnameDialog(
    physicalProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>,
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onApply: (Long, Double, String) -> Unit
) {
    var selectedProduct by remember(initialProduct?.id) { mutableStateOf(initialProduct) }
    var searchProduct by remember { mutableStateOf("") }
    var exactStockText by remember(selectedProduct?.id) {
        mutableStateOf(selectedProduct?.let { formatStockNumber(it.stock) } ?: "0")
    }
    var reason by remember(selectedProduct?.id) { mutableStateOf("Stok Opname Rutin") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val candidateProducts = remember(physicalProducts, allProducts, searchProduct) {
        val base = physicalProducts.ifEmpty { allProducts }
        if (searchProduct.isBlank()) base else base.filter { it.name.contains(searchProduct, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Penyesuaian Stok (Opname)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                Text(selectedProduct?.name ?: "Pilih produk", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))

                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Ganti Produk",
                    value = searchProduct,
                    onValueChange = { searchProduct = it },
                    placeholder = "Cari produk opname...",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidateProducts, key = { it.id }) { p ->
                        DesignFilterChip(
                            label = p.name.take(20),
                            selected = selectedProduct?.id == p.id,
                            onClick = {
                                selectedProduct = p
                                errorMsg = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DesignLabeledTextField(
                    label = "Stok Fisik Sebenarnya (${selectedProduct?.baseUnit ?: "pcs"}) *",
                    value = exactStockText,
                    onValueChange = { exactStockText = filterDecimalInput(it); errorMsg = null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Alasan Penyesuaian",
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Batal", color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = selectedProduct
                            val newS = exactStockText.toDoubleOrNull()
                            when {
                                p == null -> errorMsg = "Pilih produk terlebih dahulu."
                                newS == null || newS < 0.0 -> errorMsg = "Masukkan angka stok fisik yang valid (>= 0)."
                                else -> onApply(p.id, newS, reason)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Terapkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

