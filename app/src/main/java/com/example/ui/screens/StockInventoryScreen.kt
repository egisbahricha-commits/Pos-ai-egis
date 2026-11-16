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

    val totalAssetValue = products.sumOf { it.stock * it.purchasePrice }

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
                        "Total nilai persediaan (modal)",
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
                                selectedProduct = products.firstOrNull()
                                showQuickStockInDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ModuleProduk),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stok Masuk", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedProduct = products.firstOrNull()
                                showAdjustDialog = true
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
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
            "STOK" -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        val isDigital = product.isUnlimitedStock || product.hasNoStock || product.productType == "DIGITAL"
                        val isMinus = !isDigital && product.stock < 0
                        val isOut = !isDigital && product.stock == 0.0
                        val isLow = !isDigital && product.stock > 0.0 && product.stock <= product.minStock

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
                                        "Modal Rp ${formatRupiah(product.purchasePrice)} • Min ${product.minStock.toInt()} ${product.baseUnit}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                    )
                                    Text(
                                        "Nilai aset: Rp ${formatRupiah(product.stock * product.purchasePrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = AccentBlue)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    DesignStatusBadge(
                                        text = when {
                                            isDigital -> "Digital"
                                            isMinus -> "Stok minus ${product.stock.toInt()}"
                                            isOut -> "Habis"
                                            else -> "${if (product.allowDecimalQty) product.stock.toString() else product.stock.toInt().toString()} ${product.baseUnit}"
                                        },
                                        type = when {
                                            isDigital -> BadgeType.INFO
                                            isMinus || isOut -> BadgeType.DANGER
                                            isLow -> BadgeType.WARNING
                                            else -> BadgeType.SUCCESS
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Opname",
                                        style = MaterialTheme.typography.labelMedium.copy(color = AccentBlue, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.clickable {
                                            selectedProduct = product
                                            showAdjustDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "BATCH" -> {
                val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                val monthSoonStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 30L * 86400_000L)) }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Button(
                            onClick = {
                                selectedProduct = products.firstOrNull { !it.hasNoStock && !it.isUnlimitedStock } ?: products.firstOrNull()
                                showAddBatchDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                            "Jumlah: ${batch.qty.toInt()} unit",
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
                                        IconButton(onClick = { viewModel.deleteProductBatch(batch) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Batch", tint = Rose500, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                if (movements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.weight(1f)
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
                                            "${if (isPositive) "+" else ""}${m.quantityChange.toInt()} ${m.unit}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) Emerald700 else Rose500
                                            )
                                        )
                                        Text("Sisa ${m.currentStock.toInt()}", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Batch Dialog
    if (showAddBatchDialog) {
        var batchCode by remember { mutableStateOf("B-${System.currentTimeMillis() % 10000}") }
        var expiryStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 60L * 86400_000L))) }
        var qtyText by remember { mutableStateOf("10") }

        Dialog(onDismissRequest = { showAddBatchDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tambah Batch Kedaluwarsa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pilih Produk", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products.filter { !it.hasNoStock }.take(8)) { p ->
                            DesignFilterChip(
                                label = p.name.take(18),
                                selected = selectedProduct?.id == p.id,
                                onClick = { selectedProduct = p }
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
                        onValueChange = { expiryStr = it },
                        placeholder = "2027-06-30",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Jumlah Stok Batch",
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddBatchDialog = false }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val p = selectedProduct
                                val q = qtyText.toDoubleOrNull() ?: 0.0
                                if (p != null && expiryStr.isNotBlank()) {
                                    viewModel.saveProductBatch(
                                        ProductBatchEntity(
                                            productId = p.id,
                                            batchCode = batchCode.trim(),
                                            expiryDateStr = expiryStr.trim(),
                                            qty = q
                                        )
                                    )
                                    showAddBatchDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simpan Batch", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Quick Stock In Dialog
    if (showQuickStockInDialog) {
        var qtyText by remember { mutableStateOf("10") }
        var unitText by remember { mutableStateOf(selectedProduct?.baseUnit ?: "pcs") }
        var reason by remember { mutableStateOf("Stok Masuk Cepat") }

        Dialog(onDismissRequest = { showQuickStockInDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tambah Stok Masuk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pilih Produk", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products.take(6)) { p ->
                            DesignFilterChip(
                                label = p.name.take(18),
                                selected = selectedProduct?.id == p.id,
                                onClick = {
                                    selectedProduct = p
                                    unitText = p.baseUnit
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    DesignLabeledTextField(
                        label = "Jumlah Masuk ($unitText) *",
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Keterangan",
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showQuickStockInDialog = false }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val p = selectedProduct
                                val q = qtyText.toDoubleOrNull() ?: 0.0
                                if (p != null && q > 0) {
                                    viewModel.quickStockIn(p.id, q, unitText, reason)
                                    showQuickStockInDialog = false
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

    // Stock Opname / Adjustment Dialog
    if (showAdjustDialog) {
        var exactStockText by remember { mutableStateOf("${selectedProduct?.stock?.toInt() ?: 0}") }
        var reason by remember { mutableStateOf("Stok Opname Rutin") }

        Dialog(onDismissRequest = { showAdjustDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Penyesuaian Stok (Opname)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Text(selectedProduct?.name ?: "", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))

                    Spacer(modifier = Modifier.height(12.dp))
                    DesignLabeledTextField(
                        label = "Stok Fisik Sebenarnya (${selectedProduct?.baseUnit ?: "pcs"}) *",
                        value = exactStockText,
                        onValueChange = { exactStockText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Alasan Penyesuaian",
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAdjustDialog = false }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val p = selectedProduct
                                val newS = exactStockText.toDoubleOrNull()
                                if (p != null && newS != null) {
                                    viewModel.adjustStock(p.id, newS, reason)
                                    showAdjustDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Terapkan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
