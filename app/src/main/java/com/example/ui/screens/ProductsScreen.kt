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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.BundleItemEntity
import com.example.data.entity.BundlePackageEntity
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductTierPriceEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.PromoEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.entity.WholesaleTierEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.BarcodeSearchBar
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun ProductsScreen(viewModel: PosViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val unitConversions by viewModel.unitConversions.collectAsStateWithLifecycle()
    val allTierPrices by viewModel.allProductTierPrices.collectAsStateWithLifecycle()
    val productVariants by viewModel.productVariants.collectAsStateWithLifecycle()
    val wholesaleTiers by viewModel.wholesaleTiers.collectAsStateWithLifecycle()
    val bundles by viewModel.bundles.collectAsStateWithLifecycle()
    val bundleItems by viewModel.bundleItems.collectAsStateWithLifecycle()
    val promos by viewModel.promos.collectAsStateWithLifecycle()
    val tierServices by viewModel.tierServices.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()

    var activeModuleTab by remember { mutableStateOf("PRODUK") } // PRODUK, PAKET, PROMO
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeTab by remember { mutableStateOf("SEMUA") } // SEMUA, FISIK, DIGITAL
    var selectedStatusChip by remember { mutableStateOf("SEMUA") } // SEMUA, MENIPIS, HABIS, MINUS

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    var showAddBundleDialog by remember { mutableStateOf(false) }
    var showAddPromoDialog by remember { mutableStateOf(false) }

    val filtered = remember(products, searchQuery, selectedTypeTab, selectedStatusChip) {
        products.filter { p ->
            val isDigital = p.productType == "DIGITAL" || p.isUnlimitedStock || p.hasNoStock
            val matchesTab = when (selectedTypeTab) {
                "FISIK" -> !isDigital
                "DIGITAL" -> isDigital
                else -> true
            }
            val matchesStatus = when (selectedStatusChip) {
                "MENIPIS" -> !isDigital && p.stock > 0.0 && p.stock <= p.minStock
                "HABIS" -> !isDigital && p.stock == 0.0
                "MINUS" -> !isDigital && p.stock < 0.0
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesStatus && matchesQuery
        }
    }

    Scaffold(
        containerColor = BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeModuleTab) {
                        "PRODUK" -> {
                            productToEdit = null
                            showAddEditDialog = true
                        }
                        "PAKET" -> showAddBundleDialog = true
                        "PROMO" -> showAddPromoDialog = true
                    }
                },
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("products_screen")
        ) {
            // Top Module Tabs + Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DesignSegmentedTabs(
                    options = listOf(
                        "PRODUK" to "Produk (${products.size})",
                        "PAKET" to "Paket (${bundles.size})",
                        "PROMO" to "Promo (${promos.size})"
                    ),
                    selectedKey = activeModuleTab,
                    onSelect = { activeModuleTab = it }
                )

                if (activeModuleTab == "PRODUK") {
                    BarcodeSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Cari nama produk atau barcode...",
                        onBarcodeScanClick = {
                            val p = products.firstOrNull()
                            if (p != null) searchQuery = p.barcode
                        }
                    )

                    DesignSegmentedTabs(
                        options = listOf("SEMUA" to "Semua", "FISIK" to "Fisik", "DIGITAL" to "Digital"),
                        selectedKey = selectedTypeTab,
                        onSelect = { selectedTypeTab = it }
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            DesignFilterChip(
                                label = "Semua (${products.size})",
                                selected = selectedStatusChip == "SEMUA",
                                onClick = { selectedStatusChip = "SEMUA" }
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Stok menipis",
                                selected = selectedStatusChip == "MENIPIS",
                                onClick = { selectedStatusChip = if (selectedStatusChip == "MENIPIS") "SEMUA" else "MENIPIS" }
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Habis",
                                selected = selectedStatusChip == "HABIS",
                                onClick = { selectedStatusChip = if (selectedStatusChip == "HABIS") "SEMUA" else "HABIS" }
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Stok minus",
                                selected = selectedStatusChip == "MINUS",
                                onClick = { selectedStatusChip = if (selectedStatusChip == "MINUS") "SEMUA" else "MINUS" }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDefault)

            when (activeModuleTab) {
                "PAKET" -> {
                    if (bundles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            DesignEmptyState(
                                icon = Icons.Default.ShoppingBag,
                                title = "Belum ada paket bundling",
                                subtitle = "Gabungkan beberapa barang dalam 1 harga paket hemat.",
                                ctaText = "Buat Paket",
                                onCtaClick = { showAddBundleDialog = true }
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(bundles, key = { it.id }) { bundle ->
                                val itemsInBundle = bundleItems.filter { it.bundleId == bundle.id }
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    border = BorderStroke(1.dp, BorderDefault),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bundle.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                            Text(
                                                "Harga Paket: Rp ${formatRupiah(bundle.sellingPrice)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                                            )
                                            val desc = itemsInBundle.joinToString(", ") { bi ->
                                                val pn = products.find { it.id == bi.productId }?.name ?: "Produk"
                                                "${bi.qty.toInt()}x $pn"
                                            }
                                            if (desc.isNotBlank()) {
                                                Text(desc, style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteBundle(bundle) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Paket", tint = Rose500)
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }

                "PROMO" -> {
                    if (promos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            DesignEmptyState(
                                icon = Icons.Default.LocalOffer,
                                title = "Belum ada promo",
                                subtitle = "Buat promo voucher, tebus murah, atau beli X gratis Y.",
                                ctaText = "Tambah Promo",
                                onCtaClick = { showAddPromoDialog = true }
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(promos, key = { it.id }) { promo ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
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
                                                Text(promo.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                                DesignStatusBadge(text = promo.promoType.replace("_", " ").uppercase(), type = BadgeType.INFO)
                                            }
                                            Text(
                                                "Kode: ${promo.code.ifBlank { "-" }} • Min Belanja Rp ${formatRupiah(promo.minPurchase)} • Potongan Rp ${formatRupiah(promo.discountAmount)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = promo.isActive,
                                                onCheckedChange = { viewModel.togglePromoActive(promo) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                                            )
                                            IconButton(onClick = { viewModel.deletePromo(promo) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Rose500)
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }

                else -> {
                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            DesignEmptyState(
                                icon = Icons.Default.Inventory2,
                                title = "Belum ada produk",
                                subtitle = "Tambahkan produk pertama atau ubah filter pencarian.",
                                ctaText = "Tambah produk",
                                onCtaClick = {
                                    productToEdit = null
                                    showAddEditDialog = true
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filtered, key = { it.id }) { product ->
                                val conversions = unitConversions.filter { it.productId == product.id }
                                val tiers = allTierPrices.filter { it.productId == product.id }.sortedBy { it.quantity }
                                val variants = productVariants.filter { it.productId == product.id }
                                val wholesales = wholesaleTiers.filter { it.productId == product.id }.sortedBy { it.minQty }
                                val isDigital = product.productType == "DIGITAL" || product.isUnlimitedStock || product.hasNoStock
                                val isMinus = !isDigital && product.stock < 0.0
                                val isOut = !isDigital && product.stock == 0.0
                                val isLow = !isDigital && product.stock > 0.0 && product.stock <= product.minStock

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    border = BorderStroke(1.dp, BorderDefault),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isDigital) AccentBlueSoft else SurfaceStrong),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (isDigital) Icons.Default.PhoneAndroid else Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = if (isDigital) AccentBlue else Slate600,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    product.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                                )
                                                Text(
                                                    "${product.category} • ${product.barcode.ifBlank { "Tanpa barcode" }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                                )
                                            }

                                            IconButton(onClick = {
                                                productToEdit = product
                                                showAddEditDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentBlue, modifier = Modifier.size(19.dp))
                                            }
                                            IconButton(onClick = { productToDelete = product }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Rose500, modifier = Modifier.size(19.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (product.strikePrice != null && product.strikePrice > 0) {
                                                        Text(
                                                            "Rp ${formatRupiah(product.strikePrice)}",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = Slate400,
                                                                textDecoration = TextDecoration.LineThrough
                                                            )
                                                        )
                                                    }
                                                    Text(
                                                        text = if (product.isFlexiblePrice) "Harga fleksibel" else "Rp ${formatRupiah(product.sellingPrice)} / ${product.baseUnit}",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                                    )
                                                }
                                                if (product.specialPrice != null && product.specialPrice > 0) {
                                                    Text(
                                                        "Harga khusus: Rp ${formatRupiah(product.specialPrice)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.SemiBold)
                                                    )
                                                }
                                                if (!isDigital) {
                                                    Text(
                                                        "Modal Rp ${formatRupiah(product.purchasePrice)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (product.allowDecimalQty) {
                                                    DesignStatusBadge(text = "Desimal", type = BadgeType.NORMAL)
                                                }
                                                when {
                                                    isDigital -> DesignStatusBadge(text = if (product.serviceDirection == "TARIK") "Digital (Tarik)" else "Digital", type = BadgeType.INFO)
                                                    isMinus -> DesignStatusBadge(text = "Stok minus ${product.stock.toInt()}", type = BadgeType.DANGER)
                                                    isOut -> DesignStatusBadge(text = "Habis", type = BadgeType.DANGER)
                                                    isLow -> DesignStatusBadge(text = "Menipis ${product.stock.toInt()}", type = BadgeType.WARNING)
                                                    else -> DesignStatusBadge(
                                                        text = "Stok ${if (product.allowDecimalQty) product.stock.toString() else product.stock.toInt().toString()} ${product.baseUnit}",
                                                        type = BadgeType.NORMAL
                                                    )
                                                }
                                            }
                                        }

                                        if (variants.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Varian: " + variants.joinToString(" • ") { "${it.name} (Rp ${formatRupiah(it.sellingPrice)})" },
                                                style = MaterialTheme.typography.labelSmall.copy(color = ModuleProduk, fontWeight = FontWeight.SemiBold)
                                            )
                                        }

                                        if (wholesales.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Harga grosir: " + wholesales.joinToString(" • ") { "≥${it.minQty.toInt()} = Rp ${formatRupiah(it.pricePerUnit)}/unit" },
                                                style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.SemiBold)
                                            )
                                        }

                                        if (tiers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Harga per jumlah: " + tiers.joinToString(" • ") { "${it.quantity.toInt()}x = Rp ${formatRupiah(it.totalPrice)}" },
                                                style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                                            )
                                        }

                                        if (conversions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Satuan lain: " + conversions.joinToString { "1 ${it.unitName} = ${it.multiplier.toInt()} ${product.baseUnit}" },
                                                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        val existingTiers = remember(productToEdit, allTierPrices) {
            productToEdit?.let { p -> allTierPrices.filter { it.productId == p.id } } ?: emptyList()
        }
        val existingVariants = remember(productToEdit, productVariants) {
            productToEdit?.let { p -> productVariants.filter { it.productId == p.id } } ?: emptyList()
        }
        val existingWholesale = remember(productToEdit, wholesaleTiers) {
            productToEdit?.let { p -> wholesaleTiers.filter { it.productId == p.id } } ?: emptyList()
        }
        AddEditProductDialog(
            product = productToEdit,
            existingTiers = existingTiers,
            existingVariants = existingVariants,
            existingWholesale = existingWholesale,
            tierServices = tierServices,
            cashAccounts = cashAccounts,
            onGenerateBarcode = { viewModel.generateRandomBarcode() },
            onDismiss = { showAddEditDialog = false },
            onSave = { product, conversions, tiers, variants, wholesales ->
                viewModel.saveProductWithDetails(product, conversions, tiers, variants, wholesales)
                showAddEditDialog = false
            }
        )
    }

    if (showAddBundleDialog) {
        var bName by remember { mutableStateOf("") }
        var bPriceText by remember { mutableStateOf("") }
        val chosenItems = remember { mutableStateListOf<BundleItemEntity>() }

        Dialog(onDismissRequest = { showAddBundleDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Buat Paket Bundling", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) }
                    item { DesignLabeledTextField("Nama Paket *", bName, { bName = it }, placeholder = "mis. Paket Hemat Sarapan") }
                    item { DesignLabeledTextField("Harga Jual Paket *", bPriceText, { bPriceText = it.filter { c -> c.isDigit() } }, prefixText = "Rp ") }
                    item {
                        Text("Pilih Produk Isi Paket:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate700))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(products.filter { !it.hasNoStock && !it.isUnlimitedStock }.take(10)) { p ->
                                val already = chosenItems.any { it.productId == p.id }
                                DesignFilterChip(
                                    label = p.name.take(16),
                                    selected = already,
                                    onClick = {
                                        if (already) chosenItems.removeAll { it.productId == p.id }
                                        else chosenItems.add(BundleItemEntity(bundleId = 0L, productId = p.id, qty = 1.0))
                                    }
                                )
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddBundleDialog = false }) { Text("Batal") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val price = bPriceText.toDoubleOrNull() ?: 0.0
                                    if (bName.isNotBlank() && price > 0 && chosenItems.isNotEmpty()) {
                                        viewModel.saveBundle(
                                            BundlePackageEntity(name = bName.trim(), sellingPrice = price),
                                            chosenItems.toList()
                                        )
                                        showAddBundleDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Simpan Paket", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showAddPromoDialog) {
        var promoName by remember { mutableStateOf("") }
        var promoCode by remember { mutableStateOf("") }
        var promoType by remember { mutableStateOf("voucher") }
        var minPurchaseText by remember { mutableStateOf("50000") }
        var discountText by remember { mutableStateOf("5000") }

        Dialog(onDismissRequest = { showAddPromoDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Tambah Promo / Voucher", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("voucher", "tebus_murah", "beli_x_gratis_y", "poin", "stempel")) { t ->
                                DesignFilterChip(
                                    label = t.replace("_", " "),
                                    selected = promoType == t,
                                    onClick = { promoType = t }
                                )
                            }
                        }
                    }
                    item { DesignLabeledTextField("Nama Promo *", promoName, { promoName = it }, placeholder = "mis. Voucher Jumat Berkah") }
                    item { DesignLabeledTextField("Kode Promo / Voucher", promoCode, { promoCode = it }, placeholder = "JUMAT5K") }
                    item { DesignLabeledTextField("Min. Belanja", minPurchaseText, { minPurchaseText = it.filter { c -> c.isDigit() } }, prefixText = "Rp ") }
                    item { DesignLabeledTextField("Nominal Diskon / Nilai", discountText, { discountText = it.filter { c -> c.isDigit() } }, prefixText = "Rp ") }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddPromoDialog = false }) { Text("Batal") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (promoName.isNotBlank()) {
                                        viewModel.savePromo(
                                            PromoEntity(
                                                promoType = promoType,
                                                name = promoName.trim(),
                                                code = promoCode.trim().uppercase(),
                                                minPurchase = minPurchaseText.toDoubleOrNull() ?: 0.0,
                                                discountAmount = discountText.toDoubleOrNull() ?: 0.0
                                            )
                                        )
                                        showAddPromoDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Simpan Promo", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (productToDelete != null) {
        Dialog(onDismissRequest = { productToDelete = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hapus Produk?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Apakah Anda yakin ingin menghapus \"${productToDelete!!.name}\"?",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { productToDelete = null },
                            border = BorderStroke(1.dp, BorderInput),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", color = Slate700)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteProduct(productToDelete!!)
                                productToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private data class TierRowDraft(val qtyText: String, val totalPriceText: String)
private data class VariantRowDraft(val name: String, val sellingPriceText: String, val stockText: String)
private data class WholesaleRowDraft(val minQtyText: String, val pricePerUnitText: String)

@Composable
fun AddEditProductDialog(
    product: ProductEntity?,
    existingTiers: List<ProductTierPriceEntity> = emptyList(),
    existingVariants: List<ProductVariantEntity> = emptyList(),
    existingWholesale: List<WholesaleTierEntity> = emptyList(),
    tierServices: List<AdminTierServiceEntity> = emptyList(),
    cashAccounts: List<CashAccountEntity> = emptyList(),
    onGenerateBarcode: () -> String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, List<UnitConversionEntity>, List<ProductTierPriceEntity>, List<ProductVariantEntity>, List<WholesaleTierEntity>) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var autoBarcode by remember { mutableStateOf(product?.autoBarcode ?: false) }
    var category by remember { mutableStateOf(product?.category ?: "Makanan & Minuman") }
    var baseUnit by remember { mutableStateOf(product?.baseUnit ?: "pcs") }
    var purchasePriceText by remember { mutableStateOf(product?.purchasePrice?.let { "${it.toLong()}" } ?: "") }
    var sellingPriceText by remember { mutableStateOf(product?.sellingPrice?.let { "${it.toLong()}" } ?: "") }
    var specialPriceText by remember { mutableStateOf(product?.specialPrice?.let { "${it.toLong()}" } ?: "") }
    var strikePriceText by remember { mutableStateOf(product?.strikePrice?.let { "${it.toLong()}" } ?: "") }
    var stockText by remember { mutableStateOf(product?.stock?.let { if (product.allowDecimalQty) it.toString() else "${it.toInt()}" } ?: "0") }
    var minStockText by remember { mutableStateOf(product?.minStock?.let { "${it.toInt()}" } ?: "5") }
    var isFlexiblePrice by remember { mutableStateOf(product?.isFlexiblePrice ?: false) }
    var allowDecimalQty by remember { mutableStateOf(product?.allowDecimalQty ?: false) }
    var hasNoStock by remember { mutableStateOf(product?.hasNoStock ?: false) }
    var isDigitalProduct by remember { mutableStateOf((product?.productType == "DIGITAL") || (product?.isUnlimitedStock == true)) }

    var selectedTierGroupId by remember { mutableStateOf(product?.adminTierGroupId) }
    var selectedServiceCashId by remember { mutableStateOf(product?.serviceCashAccountId) }
    var serviceDirection by remember { mutableStateOf(product?.serviceDirection ?: "SETOR") }

    var conversionUnit by remember { mutableStateOf("") }
    var conversionMultiplier by remember { mutableStateOf("") }
    val conversionsList = remember { mutableStateListOf<UnitConversionEntity>() }

    val tierRows = remember {
        mutableStateListOf<TierRowDraft>().apply {
            existingTiers.sortedBy { it.quantity }.forEach {
                add(TierRowDraft(it.quantity.toInt().toString(), it.totalPrice.toLong().toString()))
            }
        }
    }

    val variantRows = remember {
        mutableStateListOf<VariantRowDraft>().apply {
            existingVariants.forEach {
                add(VariantRowDraft(it.name, it.sellingPrice.toLong().toString(), it.stock.toInt().toString()))
            }
        }
    }

    val wholesaleRows = remember {
        mutableStateListOf<WholesaleRowDraft>().apply {
            existingWholesale.sortedBy { it.minQty }.forEach {
                add(WholesaleRowDraft(it.minQty.toInt().toString(), it.pricePerUnit.toLong().toString()))
            }
        }
    }

    val duplicateQtyKeys = remember(tierRows.toList()) {
        val counts = tierRows.mapNotNull { it.qtyText.toIntOrNull() }.groupingBy { it }.eachCount()
        counts.filter { it.value > 1 }.keys
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (product == null) "Tambah produk" else "Edit produk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }

                    DesignSegmentedTabs(
                        options = listOf("FISIK" to "Fisik", "DIGITAL" to "Digital"),
                        selectedKey = if (isDigitalProduct) "DIGITAL" else "FISIK",
                        onSelect = { isDigitalProduct = (it == "DIGITAL") }
                    )
                }

                item {
                    DesignLabeledTextField(
                        label = "Nama produk *",
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        placeholder = "mis. Buku tulis",
                        isError = nameError,
                        errorMessage = "Nama wajib diisi"
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesignLabeledTextField(
                            label = "Kategori",
                            value = category,
                            onValueChange = { category = it },
                            modifier = Modifier.weight(1.4f)
                        )
                        DesignLabeledTextField(
                            label = "Satuan",
                            value = baseUnit,
                            onValueChange = { baseUnit = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesignLabeledTextField(
                            label = "Harga modal",
                            value = purchasePriceText,
                            onValueChange = { purchasePriceText = it.filter { ch -> ch.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        DesignLabeledTextField(
                            label = "Harga jual",
                            value = sellingPriceText,
                            onValueChange = { sellingPriceText = it.filter { ch -> ch.isDigit() } },
                            prefixText = "Rp ",
                            enabled = !isFlexiblePrice,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesignLabeledTextField(
                            label = "Harga coret (opsional)",
                            value = strikePriceText,
                            onValueChange = { strikePriceText = it.filter { ch -> ch.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        DesignLabeledTextField(
                            label = "Harga khusus (opsional)",
                            value = specialPriceText,
                            onValueChange = { specialPriceText = it.filter { ch -> ch.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Harga fleksibel", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate900))
                            Text("Kasir isi nominal sendiri saat transaksi", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp))
                        }
                        Switch(
                            checked = isFlexiblePrice,
                            onCheckedChange = { isFlexiblePrice = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                        )
                    }
                }

                if (!isDigitalProduct) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Qty Desimal (kg, liter, meter)", fontSize = 13.sp, color = Slate800)
                            Switch(checked = allowDecimalQty, onCheckedChange = { allowDecimalQty = it }, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Produk Tanpa Stok (Jasa/Unlimited)", fontSize = 13.sp, color = Slate800)
                            Switch(checked = hasNoStock, onCheckedChange = { hasNoStock = it }, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }
                    }

                    if (!hasNoStock) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DesignLabeledTextField(
                                    label = "Stok awal (boleh minus)",
                                    value = stockText,
                                    onValueChange = { stockText = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                DesignLabeledTextField(
                                    label = "Stok minimum",
                                    value = minStockText,
                                    onValueChange = { minStockText = it.filter { ch -> ch.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // Digital service linking (kelompok_tarif, akun_kas_layanan, arah_layanan)
                    item {
                        HorizontalDivider(color = BorderDefault)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Koneksi Layanan Digital / Agen", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Spacer(modifier = Modifier.height(6.dp))
                        DesignSegmentedTabs(
                            options = listOf("SETOR" to "Setor / Transfer", "TARIK" to "Tarik Tunai"),
                            selectedKey = serviceDirection,
                            onSelect = { serviceDirection = it }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Grup Tarif Admin:", fontSize = 11.sp, color = Slate600)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tierServices) { ts ->
                                DesignFilterChip(
                                    label = ts.serviceName,
                                    selected = selectedTierGroupId == ts.id,
                                    onClick = { selectedTierGroupId = if (selectedTierGroupId == ts.id) null else ts.id }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Akun Kas Layanan Terkait:", fontSize = 11.sp, color = Slate600)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(cashAccounts) { ca ->
                                DesignFilterChip(
                                    label = ca.accountName,
                                    selected = selectedServiceCashId == ca.id,
                                    onClick = { selectedServiceCashId = if (selectedServiceCashId == ca.id) null else ca.id }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        DesignLabeledTextField(
                            label = "Barcode / SKU",
                            value = barcode,
                            onValueChange = { barcode = it },
                            placeholder = "Opsional",
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                barcode = onGenerateBarcode()
                                autoBarcode = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderInput),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto", fontSize = 12.sp)
                        }
                    }
                }

                // VARIAN PRODUK
                item {
                    HorizontalDivider(color = BorderDefault)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Varian Produk (Ukuran / Warna)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        variantRows.forEachIndexed { idx, row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                DesignLabeledTextField("Nama", row.name, { variantRows[idx] = row.copy(name = it) }, modifier = Modifier.weight(1.2f))
                                DesignLabeledTextField("Harga", row.sellingPriceText, { v -> variantRows[idx] = row.copy(sellingPriceText = v.filter { it.isDigit() }) }, prefixText = "Rp ", modifier = Modifier.weight(1.2f))
                                DesignLabeledTextField("Stok", row.stockText, { v -> variantRows[idx] = row.copy(stockText = v.filter { it.isDigit() }) }, modifier = Modifier.weight(0.7f))
                                IconButton(onClick = { variantRows.removeAt(idx) }, modifier = Modifier.padding(top = 24.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus varian", tint = Rose500)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { variantRows.add(VariantRowDraft("Varian ${variantRows.size + 1}", sellingPriceText.ifBlank { "10000" }, "10")) },
                            border = BorderStroke(1.dp, BorderDefault),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceStrong, contentColor = Slate800),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah varian", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // HARGA BERTINGKAT / GROSIR (harga_bertingkat)
                item {
                    HorizontalDivider(color = BorderDefault)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Harga grosir bertingkat (harga per unit >= min beli)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        wholesaleRows.forEachIndexed { idx, row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                DesignLabeledTextField("Min qty (>=", row.minQtyText, { v -> wholesaleRows[idx] = row.copy(minQtyText = v.filter { it.isDigit() }) }, modifier = Modifier.weight(0.8f))
                                DesignLabeledTextField("Harga / unit", row.pricePerUnitText, { v -> wholesaleRows[idx] = row.copy(pricePerUnitText = v.filter { it.isDigit() }) }, prefixText = "Rp ", modifier = Modifier.weight(1.3f))
                                IconButton(onClick = { wholesaleRows.removeAt(idx) }, modifier = Modifier.padding(top = 24.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus grosir", tint = Rose500)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val nextMin = (wholesaleRows.mapNotNull { it.minQtyText.toIntOrNull() }.maxOrNull() ?: 0) + 5
                                wholesaleRows.add(WholesaleRowDraft(nextMin.toString(), ""))
                            },
                            border = BorderStroke(1.dp, BorderDefault),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceStrong, contentColor = Slate800),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah tingkat grosir", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // HARGA PER JUMLAH BELI
                item {
                    HorizontalDivider(color = BorderDefault)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Harga per jumlah beli",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tierRows.forEachIndexed { idx, row ->
                            val qtyInt = row.qtyText.toIntOrNull()
                            val isDup = qtyInt != null && duplicateQtyKeys.contains(qtyInt)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                DesignLabeledTextField(
                                    label = "Jumlah beli",
                                    value = row.qtyText,
                                    onValueChange = { valStr -> tierRows[idx] = row.copy(qtyText = valStr.filter { it.isDigit() }) },
                                    isError = isDup,
                                    errorMessage = "Jumlah kembar",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.75f)
                                )
                                DesignLabeledTextField(
                                    label = "Total harga",
                                    value = row.totalPriceText,
                                    onValueChange = { valStr -> tierRows[idx] = row.copy(totalPriceText = valStr.filter { it.isDigit() }) },
                                    prefixText = "Rp ",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.35f)
                                )
                                IconButton(
                                    onClick = { tierRows.removeAt(idx) },
                                    modifier = Modifier.padding(top = 24.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus baris", tint = Rose500)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val nextQty = (tierRows.mapNotNull { it.qtyText.toIntOrNull() }.maxOrNull() ?: 0) + 1
                                tierRows.add(TierRowDraft(nextQty.toString(), ""))
                            },
                            border = BorderStroke(1.dp, BorderDefault),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceStrong, contentColor = Slate800),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah baris", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // Multi-satuan
                if (!isDigitalProduct) {
                    item {
                        HorizontalDivider(color = BorderDefault)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Satuan grosir (opsional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                            DesignLabeledTextField(
                                label = "Satuan (dus)",
                                value = conversionUnit,
                                onValueChange = { conversionUnit = it },
                                modifier = Modifier.weight(1f)
                            )
                            DesignLabeledTextField(
                                label = "Isi ($baseUnit)",
                                value = conversionMultiplier,
                                onValueChange = { conversionMultiplier = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val mult = conversionMultiplier.toDoubleOrNull() ?: 1.0
                                    if (conversionUnit.isNotBlank()) {
                                        conversionsList.add(
                                            UnitConversionEntity(
                                                productId = product?.id ?: 0L,
                                                unitName = conversionUnit.trim(),
                                                multiplier = mult
                                            )
                                        )
                                        conversionUnit = ""
                                        conversionMultiplier = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceStrong, contentColor = Slate900),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                        conversionsList.forEach { conv ->
                            Text("• 1 ${conv.unitName} = ${conv.multiplier.toInt()} $baseUnit", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    nameError = true
                                } else if (duplicateQtyKeys.isEmpty()) {
                                    val newProduct = ProductEntity(
                                        id = product?.id ?: 0L,
                                        barcode = barcode,
                                        name = name.trim(),
                                        category = category.trim().ifBlank { "Umum" },
                                        baseUnit = baseUnit.trim().ifBlank { "pcs" },
                                        purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0,
                                        sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0,
                                        stock = if (isDigitalProduct || hasNoStock) 999.0 else (stockText.toDoubleOrNull() ?: 0.0),
                                        minStock = minStockText.toDoubleOrNull() ?: 5.0,
                                        productType = if (isDigitalProduct) "DIGITAL" else "FISIK",
                                        isUnlimitedStock = isDigitalProduct || hasNoStock,
                                        isFlexiblePrice = isFlexiblePrice,
                                        hasNoStock = isDigitalProduct || hasNoStock,
                                        allowDecimalQty = allowDecimalQty,
                                        autoBarcode = autoBarcode,
                                        specialPrice = specialPriceText.toDoubleOrNull(),
                                        strikePrice = strikePriceText.toDoubleOrNull(),
                                        adminTierGroupId = if (isDigitalProduct) selectedTierGroupId else null,
                                        serviceCashAccountId = if (isDigitalProduct) selectedServiceCashId else null,
                                        serviceDirection = if (isDigitalProduct) serviceDirection else "SETOR"
                                    )
                                    val parsedTiers = tierRows.mapNotNull { r ->
                                        val q = r.qtyText.toDoubleOrNull()
                                        val p = r.totalPriceText.toDoubleOrNull()
                                        if (q != null && q > 0 && p != null && p >= 0) {
                                            ProductTierPriceEntity(productId = newProduct.id, quantity = q, totalPrice = p)
                                        } else null
                                    }
                                    val parsedVariants = variantRows.mapNotNull { r ->
                                        val sp = r.sellingPriceText.toDoubleOrNull() ?: 0.0
                                        val st = r.stockText.toDoubleOrNull() ?: 0.0
                                        if (r.name.isNotBlank()) {
                                            ProductVariantEntity(productId = newProduct.id, name = r.name.trim(), sellingPrice = sp, stock = st)
                                        } else null
                                    }
                                    val parsedWholesales = wholesaleRows.mapNotNull { r ->
                                        val mq = r.minQtyText.toDoubleOrNull()
                                        val ppu = r.pricePerUnitText.toDoubleOrNull()
                                        if (mq != null && mq > 0 && ppu != null && ppu >= 0) {
                                            WholesaleTierEntity(productId = newProduct.id, minQty = mq, pricePerUnit = ppu)
                                        } else null
                                    }
                                    onSave(newProduct, conversionsList.toList(), parsedTiers, parsedVariants, parsedWholesales)
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
}
