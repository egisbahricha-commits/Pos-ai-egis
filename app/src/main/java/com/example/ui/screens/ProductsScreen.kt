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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AdminTierRangeEntity
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.AdminTierWithRanges
import com.example.data.entity.BundleItemEntity
import com.example.data.entity.BundlePackageEntity
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductTierPriceEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.PromoEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.entity.WholesaleTierEntity
import com.example.data.pricing.TierPricingResolver
import com.example.hardware.BarcodeInputSanitizer
import com.example.hardware.ScannerSettings
import com.example.ui.components.BadgeType
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BarcodeSearchBar
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.ScannerSettingsDialog
import com.example.ui.components.copyUriToInternalProductImage
import com.example.ui.components.formatRupiah
import com.example.ui.components.hardwareBarcodeCaptureModifier
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExcelImportUiState
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.matchesProductSearchQuery

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
    val adminTiers by viewModel.adminTiers.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val excelImportState by viewModel.excelImportState.collectAsStateWithLifecycle()
    val scannerSettings by viewModel.scannerSettings.collectAsStateWithLifecycle()

    val excelImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectExcelProductsUri(uri)
        }
    }
    val excelTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            viewModel.exportExcelProductTemplate(uri)
        }
    }

    var activeModuleTab by remember { mutableStateOf("PRODUK") } // PRODUK, PAKET, PROMO
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedTypeTab by remember { mutableStateOf("SEMUA") } // SEMUA, FISIK, DIGITAL
    var selectedStatusChip by remember { mutableStateOf("SEMUA") } // SEMUA, MENIPIS, HABIS, MINUS

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showTierManagerDialog by remember { mutableStateOf(false) }
    var showSearchScanner by remember { mutableStateOf(false) }
    var showScannerSettingsDialog by remember { mutableStateOf(false) }

    var showAddBundleDialog by remember { mutableStateOf(false) }
    var showAddPromoDialog by remember { mutableStateOf(false) }

    val filtered = remember(products, productVariants, searchQuery, selectedTypeTab, selectedStatusChip) {
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
            val matchesQuery = matchesProductSearchQuery(p, searchQuery, productVariants)
            matchesTab && matchesStatus && matchesQuery
        }
    }

    val anyDialogOpen = showAddEditDialog ||
        productToDelete != null ||
        showTierManagerDialog ||
        showSearchScanner ||
        showScannerSettingsDialog ||
        showAddBundleDialog ||
        showAddPromoDialog

    val hardwareModifier = hardwareBarcodeCaptureModifier(
        enabled = activeModuleTab == "PRODUK" &&
            scannerSettings.autoCaptureHardwareInput &&
            !isSearchFocused &&
            !anyDialogOpen,
        scannerSettings = scannerSettings,
        onBarcodeScanned = { code, _ ->
            searchQuery = BarcodeInputSanitizer.cleanBarcodeValue(code, scannerSettings.stripWhitespace)
        }
    )

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
                .then(hardwareModifier)
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
                        onBarcodeScanClick = { showSearchScanner = true },
                        onBarcodeSubmit = {
                            searchQuery = BarcodeInputSanitizer.cleanBarcodeValue(it, scannerSettings.stripWhitespace)
                        },
                        onFocusChanged = { isSearchFocused = it }
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
                        item {
                            DesignFilterChip(
                                label = "Kelola Manual",
                                selected = false,
                                onClick = { viewModel.navigateTo(com.example.ui.viewmodel.Screen.MANAJEMEN_PRODUK) },
                                modifier = Modifier.testTag("products_open_manual_mgr_chip")
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Import Excel",
                                selected = false,
                                onClick = {
                                    excelImportLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("products_import_excel_chip")
                            )
                        }
                        item {
                            DesignFilterChip(
                                label = "Template Excel",
                                selected = false,
                                onClick = {
                                    excelTemplateLauncher.launch("Template_Produk_KASLITE.xlsx")
                                },
                                modifier = Modifier.testTag("products_template_excel_chip")
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
                    if (selectedTypeTab == "DIGITAL") {
                        val totalRanges = adminTiers.sumOf { it.ranges.size }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Sapphire50,
                            border = BorderStroke(1.dp, Sapphire200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(AccentBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Tarif Berjenjang Otomatis",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                                        )
                                        Text(
                                            "${adminTiers.size} layanan • $totalRanges aturan rentang nominal",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showTierManagerDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AccentBlue),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("open_tier_manager_btn")
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kelola Tarif", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

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
                                val linkedTier = if (isDigital && product.adminTierGroupId != null) {
                                    adminTiers.find { it.service.id == product.adminTierGroupId }
                                } else null
                                val fixedTierCalc = if (linkedTier != null && !product.isFlexiblePrice && product.sellingPrice > 0.0) {
                                    TierPricingResolver.calculate(linkedTier, product.sellingPrice)
                                } else null

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
                                            ProductThumbnail(
                                                imageUri = product.imageUri,
                                                contentDescription = product.name,
                                                isDigital = isDigital,
                                                size = 46.dp,
                                                shape = RoundedCornerShape(10.dp),
                                                iconSize = 22.dp
                                            )

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
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular()
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

                                        if (linkedTier != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Sapphire50,
                                                border = BorderStroke(1.dp, Sapphire200),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                    Text(
                                                        "Tarif Berjenjang: ${linkedTier.service.serviceName} (${linkedTier.ranges.size} rentang)",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = Sapphire800, fontWeight = FontWeight.Bold)
                                                    )
                                                    if (fixedTierCalc != null && fixedTierCalc.matchedRange != null) {
                                                        Text(
                                                            "Admin Otomatis: Rp ${formatRupiah(fixedTierCalc.storeAdminFee)} • Tagihan: Rp ${formatRupiah(fixedTierCalc.totalCustomerPayable)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                        )
                                                    } else if (product.isFlexiblePrice) {
                                                        Text(
                                                            "Admin terisi otomatis saat nominal diinput kasir",
                                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 11.sp)
                                                        )
                                                    }
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
                                                "Satuan lain: " + conversions.joinToString {
                                                    val m = if (it.multiplier % 1.0 == 0.0) it.multiplier.toLong().toString() else it.multiplier.toString()
                                                    "1 ${it.unitName} = $m ${product.baseUnit}"
                                                },
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
        val existingConversions = remember(productToEdit, unitConversions) {
            productToEdit?.let { p -> unitConversions.filter { it.productId == p.id } } ?: emptyList()
        }
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
            existingConversions = existingConversions,
            existingTiers = existingTiers,
            existingVariants = existingVariants,
            existingWholesale = existingWholesale,
            tierServices = tierServices,
            adminTiers = adminTiers,
            cashAccounts = cashAccounts,
            defaultDigital = productToEdit == null && selectedTypeTab == "DIGITAL",
            scannerSettings = scannerSettings,
            onOpenTierManager = { showTierManagerDialog = true },
            onGenerateBarcode = { viewModel.generateRandomBarcode() },
            onDismiss = { showAddEditDialog = false },
            onSave = { product, conversions, tiers, variants, wholesales, digitalTierRanges ->
                viewModel.saveProductWithDetails(product, conversions, tiers, variants, wholesales, digitalTierRanges)
                showAddEditDialog = false
            }
        )
    }

    if (showTierManagerDialog) {
        ManageTierPricingDialog(
            adminTiers = adminTiers,
            onCreateService = { name, code -> viewModel.createAdminTierService(name, code) },
            onDeleteService = { viewModel.deleteAdminTierService(it) },
            onCreateRange = { svcId, minA, maxA, storeF, provF, agentF ->
                viewModel.createAdminTierRange(svcId, minA, maxA, storeF, provF, agentF)
            },
            onUpdateRange = { viewModel.updateAdminTierRange(it) },
            onDeleteRange = { viewModel.deleteAdminTierRange(it) },
            onDismiss = { showTierManagerDialog = false }
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

    if (showSearchScanner) {
        BarcodeScannerDialog(
            title = "Cari Produk via Barcode",
            scannerSettings = scannerSettings,
            onOpenScannerSettings = {
                showSearchScanner = false
                showScannerSettingsDialog = true
            },
            onBarcodeDetected = { code ->
                showSearchScanner = false
                searchQuery = BarcodeInputSanitizer.cleanBarcodeValue(code, scannerSettings.stripWhitespace)
            },
            onDismiss = { showSearchScanner = false }
        )
    }

    if (showScannerSettingsDialog) {
        ScannerSettingsDialog(
            currentSettings = scannerSettings,
            onSaveSettings = { updated -> viewModel.updateScannerSettings(updated) },
            onDismiss = { showScannerSettingsDialog = false }
        )
    }

    ExcelProductImportHost(
        excelImportState = excelImportState,
        onConfirm = { updateExisting -> viewModel.confirmExcelProductImport(updateExisting) },
        onDismiss = { viewModel.dismissExcelImportPreview() }
    )
}

@Composable
internal fun ExcelProductImportHost(
    excelImportState: ExcelImportUiState,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (excelImportState.isInspecting) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), color = AccentBlue)
                    Column {
                        Text("Membaca File Excel/CSV...", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        Text("Mendeteksi kolom produk & harga", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                    }
                }
            }
        }
    }

    excelImportState.inspectResult?.let { inspect ->
        var updateExisting by remember(inspect.fileName) { mutableStateOf(true) }
        Dialog(onDismissRequest = { if (!excelImportState.isImporting) onDismiss() }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(26.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pratinjau Import Produk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                Text("${inspect.fileName} • Format ${inspect.fileFormat}", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp))
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Sapphire50,
                            border = BorderStroke(1.dp, Sapphire200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Siap diimpor: ${inspect.rows.size} produk (dari ${inspect.totalRawRows} baris)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                                )
                                if (inspect.detectedColumns.isNotEmpty()) {
                                    Text(
                                        "Kolom terdeteksi: ${inspect.detectedColumns.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate700, fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }

                    if (inspect.skippedRows.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmberWarningBg,
                                border = BorderStroke(1.dp, AmberWarningBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "${inspect.skippedRows.size} baris dilewati:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    inspect.skippedRows.take(5).forEach { warn ->
                                        Text("• $warn", style = MaterialTheme.typography.labelSmall.copy(color = Slate700, fontSize = 11.sp))
                                    }
                                    if (inspect.skippedRows.size > 5) {
                                        Text("...dan ${inspect.skippedRows.size - 5} lainnya", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }

                    if (inspect.rows.isNotEmpty()) {
                        item {
                            Text("Contoh Produk (${minOf(5, inspect.rows.size)} pertama):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800))
                        }
                        items(inspect.rows.take(5)) { row ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BgCanvas,
                                border = BorderStroke(1.dp, BorderDefault),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                        Text("${row.category} • Stok ${row.stock} ${row.baseUnit}", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
                                    }
                                    Text("Rp ${formatRupiah(row.sellingPrice)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Emerald700))
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { updateExisting = !updateExisting },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = updateExisting, onCheckedChange = { updateExisting = it })
                            Text(
                                "Perbarui produk yang sudah ada (cocokkan barcode/nama)",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate800)
                            )
                        }
                    }

                    if (!excelImportState.errorMessage.isNullOrBlank()) {
                        item {
                            Text(
                                text = excelImportState.errorMessage,
                                color = Rose500,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !excelImportState.isImporting,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal")
                            }
                            Button(
                                onClick = { onConfirm(updateExisting) },
                                enabled = !excelImportState.isImporting && inspect.rows.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (excelImportState.isImporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mengimpor...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Import Sekarang", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (excelImportState.inspectResult == null && !excelImportState.errorMessage.isNullOrBlank()) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gagal Membaca Excel/CSV", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Rose500))
                    Text(excelImportState.errorMessage, style = MaterialTheme.typography.bodySmall.copy(color = Slate700))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class TierRowDraft(val qtyText: String, val totalPriceText: String)
private data class VariantRowDraft(val name: String, val sellingPriceText: String, val stockText: String)
private data class WholesaleRowDraft(val minQtyText: String, val pricePerUnitText: String)
private data class DigitalTierRangeDraft(
    val minText: String,
    val maxText: String,
    val storeFeeText: String,
    val providerFeeText: String = "0",
    val agentFeeText: String = "0"
)

@Composable
fun AddEditProductDialog(
    product: ProductEntity?,
    existingConversions: List<UnitConversionEntity> = emptyList(),
    existingTiers: List<ProductTierPriceEntity> = emptyList(),
    existingVariants: List<ProductVariantEntity> = emptyList(),
    existingWholesale: List<WholesaleTierEntity> = emptyList(),
    tierServices: List<AdminTierServiceEntity> = emptyList(),
    adminTiers: List<AdminTierWithRanges> = emptyList(),
    cashAccounts: List<CashAccountEntity> = emptyList(),
    defaultDigital: Boolean = false,
    scannerSettings: ScannerSettings = ScannerSettings(),
    onOpenTierManager: (() -> Unit)? = null,
    onGenerateBarcode: () -> String,
    onDismiss: () -> Unit,
    onSave: (
        ProductEntity,
        List<UnitConversionEntity>,
        List<ProductTierPriceEntity>,
        List<ProductVariantEntity>,
        List<WholesaleTierEntity>,
        List<AdminTierRangeEntity>?
    ) -> Unit
) {
    val context = LocalContext.current
    val productKey = product?.id
    val initialProduct = product
    var name by remember(productKey) { mutableStateOf(initialProduct?.name ?: "") }
    var nameError by remember(productKey) { mutableStateOf(false) }
    var barcode by remember(productKey) { mutableStateOf(initialProduct?.barcode ?: "") }
    var autoBarcode by remember(productKey) { mutableStateOf(initialProduct?.autoBarcode ?: false) }
    var imageUri by remember(productKey) { mutableStateOf(initialProduct?.imageUri ?: "") }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            copyUriToInternalProductImage(context, pickedUri)?.let { savedPath ->
                imageUri = savedPath
            }
        }
    }

    var category by remember(productKey, defaultDigital) {
        mutableStateOf(initialProduct?.category ?: if (defaultDigital) "Layanan Digital" else "Makanan & Minuman")
    }
    var baseUnit by remember(productKey, defaultDigital) {
        mutableStateOf(initialProduct?.baseUnit ?: if (defaultDigital) "transaksi" else "pcs")
    }
    var purchasePriceText by remember(productKey) {
        mutableStateOf(initialProduct?.purchasePrice?.let { "${it.toLong()}" } ?: "")
    }
    var sellingPriceText by remember(productKey) {
        mutableStateOf(initialProduct?.sellingPrice?.let { "${it.toLong()}" } ?: "")
    }
    var specialPriceText by remember(productKey) {
        mutableStateOf(initialProduct?.specialPrice?.let { "${it.toLong()}" } ?: "")
    }
    var strikePriceText by remember(productKey) {
        mutableStateOf(initialProduct?.strikePrice?.let { "${it.toLong()}" } ?: "")
    }
    var stockText by remember(productKey) {
        mutableStateOf(
            initialProduct?.stock?.let {
                if (initialProduct.allowDecimalQty || it % 1.0 != 0.0) it.toString() else "${it.toInt()}"
            } ?: "0"
        )
    }
    var minStockText by remember(productKey) {
        mutableStateOf(initialProduct?.minStock?.let { "${it.toInt()}" } ?: "5")
    }
    var isFlexiblePrice by remember(productKey) { mutableStateOf(initialProduct?.isFlexiblePrice ?: false) }
    var allowDecimalQty by remember(productKey) { mutableStateOf(initialProduct?.allowDecimalQty ?: false) }
    var hasNoStock by remember(productKey) { mutableStateOf(initialProduct?.hasNoStock ?: false) }
    var isDigitalProduct by remember(productKey, defaultDigital) {
        mutableStateOf(defaultDigital || (initialProduct?.productType == "DIGITAL") || (initialProduct?.isUnlimitedStock == true))
    }

    var selectedTierGroupId by remember(productKey) { mutableStateOf(initialProduct?.adminTierGroupId) }
    var selectedServiceCashId by remember(productKey) { mutableStateOf(initialProduct?.serviceCashAccountId) }
    var serviceDirection by remember(productKey) { mutableStateOf(initialProduct?.serviceDirection ?: "SETOR") }
    var previewNominalText by remember(productKey) {
        val initSell = initialProduct?.sellingPrice ?: 0.0
        mutableStateOf(if (initSell > 0.0) initSell.toLong().toString() else "345000")
    }

    val digitalTierRows = remember(productKey) { mutableStateListOf<DigitalTierRangeDraft>() }
    var digitalTierModified by remember(productKey) { mutableStateOf(false) }

    LaunchedEffect(selectedTierGroupId, adminTiers) {
        if (!digitalTierModified) {
            digitalTierRows.clear()
            val matchedRanges = selectedTierGroupId
                ?.let { gid -> adminTiers.find { it.service.id == gid }?.ranges }
                ?.sortedBy { it.minAmount }
                .orEmpty()
            matchedRanges.forEach { r ->
                digitalTierRows.add(
                    DigitalTierRangeDraft(
                        minText = r.minAmount.toLong().toString(),
                        maxText = if (r.maxAmount > 0.0) r.maxAmount.toLong().toString() else "",
                        storeFeeText = r.storeAdminFee.toLong().toString(),
                        providerFeeText = r.providerAdminFee.toLong().toString(),
                        agentFeeText = r.agentCommission.toLong().toString()
                    )
                )
            }
        }
    }

    val digitalTierValidationError = remember(digitalTierRows.toList(), isDigitalProduct, selectedTierGroupId) {
        validateDigitalTierDrafts(digitalTierRows, isDigitalProduct, selectedTierGroupId)
    }

    val conversionsList = remember(productKey, existingConversions) {
        mutableStateListOf<UnitConversionEntity>().apply { addAll(existingConversions) }
    }
    val tierRows = remember(productKey, existingTiers) {
        mutableStateListOf<TierRowDraft>().apply {
            existingTiers.sortedBy { it.quantity }.forEach {
                add(TierRowDraft(it.quantity.toInt().toString(), it.totalPrice.toLong().toString()))
            }
        }
    }
    val variantRows = remember(productKey, existingVariants) {
        mutableStateListOf<VariantRowDraft>().apply {
            existingVariants.forEach {
                add(VariantRowDraft(it.name, it.sellingPrice.toLong().toString(), it.stock.toInt().toString()))
            }
        }
    }
    val wholesaleRows = remember(productKey, existingWholesale) {
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
                    ProductDialogHeader(
                        isEdit = product != null,
                        isDigitalProduct = isDigitalProduct,
                        onSelectDigital = { isDigitalProduct = it },
                        onDismiss = onDismiss
                    )
                }

                item {
                    ProductPhotoPickerCard(
                        imageUri = imageUri,
                        productName = name,
                        isDigitalProduct = isDigitalProduct,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearPhoto = { imageUri = "" }
                    )
                }

                item {
                    ProductBasicFieldsSection(
                        name = name,
                        nameError = nameError,
                        category = category,
                        baseUnit = baseUnit,
                        purchasePriceText = purchasePriceText,
                        sellingPriceText = sellingPriceText,
                        strikePriceText = strikePriceText,
                        specialPriceText = specialPriceText,
                        isFlexiblePrice = isFlexiblePrice,
                        onNameChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        onCategoryChange = { category = it },
                        onBaseUnitChange = { baseUnit = it },
                        onPurchasePriceChange = { purchasePriceText = it.filter { ch -> ch.isDigit() } },
                        onSellingPriceChange = { sellingPriceText = it.filter { ch -> ch.isDigit() } },
                        onStrikePriceChange = { strikePriceText = it.filter { ch -> ch.isDigit() } },
                        onSpecialPriceChange = { specialPriceText = it.filter { ch -> ch.isDigit() } },
                        onFlexibleChange = { isFlexiblePrice = it }
                    )
                }

                if (!isDigitalProduct) {
                    item {
                        ProductPhysicalStockSection(
                            allowDecimalQty = allowDecimalQty,
                            hasNoStock = hasNoStock,
                            stockText = stockText,
                            minStockText = minStockText,
                            onDecimalChange = { allowDecimalQty = it },
                            onNoStockChange = { hasNoStock = it },
                            onStockChange = { stockText = it },
                            onMinStockChange = { minStockText = it.filter { ch -> ch.isDigit() } }
                        )
                    }
                } else {
                    item {
                        ProductDigitalServiceSection(
                            productName = name,
                            sellingPriceText = sellingPriceText,
                            isFlexiblePrice = isFlexiblePrice,
                            previewNominalText = previewNominalText,
                            serviceDirection = serviceDirection,
                            selectedTierGroupId = selectedTierGroupId,
                            selectedServiceCashId = selectedServiceCashId,
                            tierServices = tierServices,
                            adminTiers = adminTiers,
                            cashAccounts = cashAccounts,
                            digitalTierRows = digitalTierRows,
                            digitalTierValidationError = digitalTierValidationError,
                            onOpenTierManager = onOpenTierManager,
                            onServiceDirectionChange = { serviceDirection = it },
                            onSelectTierGroup = { gid, modified ->
                                selectedTierGroupId = gid
                                digitalTierModified = modified
                            },
                            onMarkTierModified = { digitalTierModified = true },
                            onPreviewNominalChange = { previewNominalText = it.filter { ch -> ch.isDigit() } },
                            onSelectServiceCash = { selectedServiceCashId = it }
                        )
                    }
                }

                item {
                    ProductBarcodeSection(
                        barcode = barcode,
                        onBarcodeChange = { barcode = it },
                        onScanClick = { showBarcodeScanner = true },
                        onAutoGenerateClick = {
                            barcode = onGenerateBarcode()
                            autoBarcode = true
                        }
                    )
                }

                item {
                    ProductVariantsSection(
                        variantRows = variantRows,
                        defaultPriceText = sellingPriceText
                    )
                }

                item {
                    ProductWholesaleSection(wholesaleRows = wholesaleRows)
                }

                item {
                    ProductTierQtySection(
                        tierRows = tierRows,
                        duplicateQtyKeys = duplicateQtyKeys
                    )
                }

                if (!isDigitalProduct) {
                    item {
                        ProductUnitConversionsSection(
                            productId = product?.id ?: 0L,
                            baseUnit = baseUnit,
                            conversionsList = conversionsList
                        )
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
                                } else if (duplicateQtyKeys.isEmpty() && digitalTierValidationError == null) {
                                    saveProductDialogForm(
                                        existingProductId = product?.id ?: 0L,
                                        barcode = barcode,
                                        name = name,
                                        category = category,
                                        baseUnit = baseUnit,
                                        purchasePriceText = purchasePriceText,
                                        sellingPriceText = sellingPriceText,
                                        stockText = stockText,
                                        minStockText = minStockText,
                                        isDigitalProduct = isDigitalProduct,
                                        isFlexiblePrice = isFlexiblePrice,
                                        hasNoStock = hasNoStock,
                                        allowDecimalQty = allowDecimalQty,
                                        autoBarcode = autoBarcode,
                                        specialPriceText = specialPriceText,
                                        strikePriceText = strikePriceText,
                                        imageUri = imageUri,
                                        selectedTierGroupId = selectedTierGroupId,
                                        selectedServiceCashId = selectedServiceCashId,
                                        serviceDirection = serviceDirection,
                                        digitalTierModified = digitalTierModified,
                                        conversionsList = conversionsList,
                                        tierRows = tierRows,
                                        variantRows = variantRows,
                                        wholesaleRows = wholesaleRows,
                                        digitalTierRows = digitalTierRows,
                                        onSave = onSave
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("product_save_button")
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Barcode Produk",
            scannerSettings = scannerSettings,
            onBarcodeDetected = { code ->
                barcode = BarcodeInputSanitizer.cleanBarcodeValue(code, scannerSettings.stripWhitespace)
                autoBarcode = false
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

private fun validateDigitalTierDrafts(
    digitalTierRows: List<DigitalTierRangeDraft>,
    isDigitalProduct: Boolean,
    selectedTierGroupId: Long?
): String? {
    if (!isDigitalProduct || digitalTierRows.isEmpty()) return null
    val parsed = ArrayList<AdminTierRangeEntity>(digitalTierRows.size)
    for (idx in digitalTierRows.indices) {
        val row = digitalTierRows[idx]
        val minV = row.minText.toDoubleOrNull()
        val maxV = row.maxText.toDoubleOrNull() ?: 0.0
        val feeV = row.storeFeeText.toDoubleOrNull()
        if (minV == null || feeV == null) {
            return "Tingkat #${idx + 1}: isi nominal 'Dari' dan 'Biaya Admin'"
        }
        if (maxV > 0.0 && minV > maxV) {
            return "Tingkat #${idx + 1}: nominal 'Dari' melebihi 'Sampai'"
        }
        parsed.add(
            AdminTierRangeEntity(
                id = (idx + 1).toLong(),
                serviceId = selectedTierGroupId ?: 0L,
                minAmount = minV,
                maxAmount = maxV,
                storeAdminFee = feeV,
                providerAdminFee = row.providerFeeText.toDoubleOrNull() ?: 0.0,
                agentCommission = row.agentFeeText.toDoubleOrNull() ?: 0.0
            )
        )
    }
    val hasConflict = parsed.indices.any { i ->
        TierPricingResolver.hasOverlap(
            existingRanges = parsed,
            minAmount = parsed[i].minAmount,
            maxAmount = parsed[i].maxAmount,
            excludeRangeId = parsed[i].id
        )
    }
    return if (hasConflict) "Ada rentang biaya admin yang bertumpukan" else null
}

private fun saveProductDialogForm(
    existingProductId: Long,
    barcode: String,
    name: String,
    category: String,
    baseUnit: String,
    purchasePriceText: String,
    sellingPriceText: String,
    stockText: String,
    minStockText: String,
    isDigitalProduct: Boolean,
    isFlexiblePrice: Boolean,
    hasNoStock: Boolean,
    allowDecimalQty: Boolean,
    autoBarcode: Boolean,
    specialPriceText: String,
    strikePriceText: String,
    imageUri: String,
    selectedTierGroupId: Long?,
    selectedServiceCashId: Long?,
    serviceDirection: String,
    digitalTierModified: Boolean,
    conversionsList: List<UnitConversionEntity>,
    tierRows: List<TierRowDraft>,
    variantRows: List<VariantRowDraft>,
    wholesaleRows: List<WholesaleRowDraft>,
    digitalTierRows: List<DigitalTierRangeDraft>,
    onSave: (
        ProductEntity,
        List<UnitConversionEntity>,
        List<ProductTierPriceEntity>,
        List<ProductVariantEntity>,
        List<WholesaleTierEntity>,
        List<AdminTierRangeEntity>?
    ) -> Unit
) {
    val newProduct = ProductEntity(
        id = existingProductId,
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
        imageUri = imageUri,
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
    val parsedDigitalTiers = if (
        isDigitalProduct && (digitalTierModified || (selectedTierGroupId == null && digitalTierRows.isNotEmpty()))
    ) {
        digitalTierRows.mapNotNull { r ->
            val minV = r.minText.toDoubleOrNull() ?: return@mapNotNull null
            val maxV = r.maxText.toDoubleOrNull() ?: 0.0
            val storeV = r.storeFeeText.toDoubleOrNull() ?: return@mapNotNull null
            AdminTierRangeEntity(
                serviceId = selectedTierGroupId ?: 0L,
                minAmount = minV,
                maxAmount = maxV,
                storeAdminFee = storeV,
                providerAdminFee = r.providerFeeText.toDoubleOrNull() ?: 0.0,
                agentCommission = r.agentFeeText.toDoubleOrNull() ?: 0.0
            )
        }
    } else null
    onSave(newProduct, conversionsList.toList(), parsedTiers, parsedVariants, parsedWholesales, parsedDigitalTiers)
}

@Composable
private fun ProductDialogHeader(
    isEdit: Boolean,
    isDigitalProduct: Boolean,
    onSelectDigital: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isEdit) "Edit produk" else "Tambah produk",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
        }
    }

    DesignSegmentedTabs(
        options = listOf("FISIK" to "Fisik", "DIGITAL" to "Digital"),
        selectedKey = if (isDigitalProduct) "DIGITAL" else "FISIK",
        onSelect = { onSelectDigital(it == "DIGITAL") }
    )
}

@Composable
private fun ProductPhotoPickerCard(
    imageUri: String,
    productName: String,
    isDigitalProduct: Boolean,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BgCanvas,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProductThumbnail(
                imageUri = imageUri,
                contentDescription = productName.ifBlank { "Foto Produk" },
                isDigital = isDigitalProduct,
                size = 56.dp,
                shape = RoundedCornerShape(10.dp),
                iconSize = 26.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Foto Produk (${if (isDigitalProduct) "Digital" else "Fisik"})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Text(
                    text = if (imageUri.isBlank()) "Opsional • Tampil di kasir & daftar produk" else "Foto siap ditampilkan",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPickPhoto,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("product_pick_photo_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (imageUri.isBlank()) "Pilih Foto" else "Ganti Foto",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (imageUri.isNotBlank()) {
                        TextButton(
                            onClick = onClearPhoto,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Hapus", color = Rose500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductBasicFieldsSection(
    name: String,
    nameError: Boolean,
    category: String,
    baseUnit: String,
    purchasePriceText: String,
    sellingPriceText: String,
    strikePriceText: String,
    specialPriceText: String,
    isFlexiblePrice: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onBaseUnitChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onSellingPriceChange: (String) -> Unit,
    onStrikePriceChange: (String) -> Unit,
    onSpecialPriceChange: (String) -> Unit,
    onFlexibleChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DesignLabeledTextField(
            label = "Nama produk *",
            value = name,
            onValueChange = onNameChange,
            placeholder = "mis. Buku tulis",
            isError = nameError,
            errorMessage = "Nama wajib diisi"
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignLabeledTextField(label = "Kategori", value = category, onValueChange = onCategoryChange, modifier = Modifier.weight(1.4f))
            DesignLabeledTextField(label = "Satuan", value = baseUnit, onValueChange = onBaseUnitChange, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignLabeledTextField(
                label = "Harga modal",
                value = purchasePriceText,
                onValueChange = onPurchasePriceChange,
                prefixText = "Rp ",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            DesignLabeledTextField(
                label = "Harga jual",
                value = sellingPriceText,
                onValueChange = onSellingPriceChange,
                prefixText = "Rp ",
                enabled = !isFlexiblePrice,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignLabeledTextField(
                label = "Harga coret (opsional)",
                value = strikePriceText,
                onValueChange = onStrikePriceChange,
                prefixText = "Rp ",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            DesignLabeledTextField(
                label = "Harga khusus (opsional)",
                value = specialPriceText,
                onValueChange = onSpecialPriceChange,
                prefixText = "Rp ",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
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
                onCheckedChange = onFlexibleChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
            )
        }
    }
}

@Composable
private fun ProductPhysicalStockSection(
    allowDecimalQty: Boolean,
    hasNoStock: Boolean,
    stockText: String,
    minStockText: String,
    onDecimalChange: (Boolean) -> Unit,
    onNoStockChange: (Boolean) -> Unit,
    onStockChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Qty Desimal (kg, liter, meter)", fontSize = 13.sp, color = Slate800)
            Switch(checked = allowDecimalQty, onCheckedChange = onDecimalChange, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Produk Tanpa Stok (Jasa/Unlimited)", fontSize = 13.sp, color = Slate800)
            Switch(checked = hasNoStock, onCheckedChange = onNoStockChange, colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
        }
        if (!hasNoStock) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignLabeledTextField(
                    label = "Stok awal (boleh minus)",
                    value = stockText,
                    onValueChange = onStockChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                DesignLabeledTextField(
                    label = "Stok minimum",
                    value = minStockText,
                    onValueChange = onMinStockChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProductDigitalServiceSection(
    productName: String,
    sellingPriceText: String,
    isFlexiblePrice: Boolean,
    previewNominalText: String,
    serviceDirection: String,
    selectedTierGroupId: Long?,
    selectedServiceCashId: Long?,
    tierServices: List<AdminTierServiceEntity>,
    adminTiers: List<AdminTierWithRanges>,
    cashAccounts: List<CashAccountEntity>,
    digitalTierRows: SnapshotStateList<DigitalTierRangeDraft>,
    digitalTierValidationError: String?,
    onOpenTierManager: (() -> Unit)?,
    onServiceDirectionChange: (String) -> Unit,
    onSelectTierGroup: (Long?, Boolean) -> Unit,
    onMarkTierModified: () -> Unit,
    onPreviewNominalChange: (String) -> Unit,
    onSelectServiceCash: (Long?) -> Unit
) {
    var expandedTierIdx by remember { mutableStateOf<Int?>(0) }

    Column {
        HorizontalDivider(color = BorderDefault)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Koneksi Layanan Digital & Tarif Berjenjang", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
            if (onOpenTierManager != null) {
                TextButton(onClick = onOpenTierManager, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Kelola Tarif", style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        DesignSegmentedTabs(
            options = listOf("SETOR" to "Setor / Transfer", "TARIK" to "Tarik Tunai"),
            selectedKey = serviceDirection,
            onSelect = onServiceDirectionChange
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("Grup Tarif Berjenjang Otomatis:", fontSize = 11.sp, color = Slate600)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                DesignFilterChip(
                    label = "Khusus Layanan Ini",
                    selected = selectedTierGroupId == null,
                    onClick = { onSelectTierGroup(null, true) }
                )
            }
            items(tierServices) { ts ->
                DesignFilterChip(
                    label = ts.serviceName,
                    selected = selectedTierGroupId == ts.id,
                    onClick = {
                        val nextId = if (selectedTierGroupId == ts.id) null else ts.id
                        onSelectTierGroup(nextId, false)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daftar Biaya Admin Berjenjang (${digitalTierRows.size} rentang)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            Text("0/kosong = seterusnya", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
        }
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            digitalTierRows.forEachIndexed { idx, row ->
                DigitalTierDraftRowCard(
                    idx = idx,
                    row = row,
                    isExpanded = expandedTierIdx == idx,
                    onToggleExpand = { expandedTierIdx = if (expandedTierIdx == idx) null else idx },
                    onRemove = {
                        onMarkTierModified()
                        digitalTierRows.removeAt(idx)
                        if (expandedTierIdx == idx) expandedTierIdx = null
                    },
                    onUpdateRow = { updated ->
                        onMarkTierModified()
                        digitalTierRows[idx] = updated
                    }
                )
            }

            if (digitalTierValidationError != null) {
                DesignStatusBadge(text = digitalTierValidationError, type = BadgeType.DANGER)
            }

            OutlinedButton(
                onClick = {
                    onMarkTierModified()
                    if (digitalTierRows.isEmpty()) {
                        digitalTierRows.add(DigitalTierRangeDraft(minText = "1", maxText = "1000000", storeFeeText = "5000"))
                    } else {
                        val lastIdx = digitalTierRows.lastIndex
                        val lastRow = digitalTierRows[lastIdx]
                        val lastMin = lastRow.minText.toLongOrNull() ?: 1L
                        var lastMax = lastRow.maxText.toLongOrNull() ?: 0L
                        if (lastMax <= 0L) {
                            lastMax = if (lastMin <= 1L) 1_000_000L else lastMin + 1_999_999L
                            digitalTierRows[lastIdx] = lastRow.copy(maxText = lastMax.toString())
                        }
                        val lastAdmin = lastRow.storeFeeText.toLongOrNull() ?: 5_000L
                        val nextMin = lastMax + 1L
                        val nextMax = if (nextMin == 1_000_001L) 3_000_000L else nextMin + 1_999_999L
                        digitalTierRows.add(
                            DigitalTierRangeDraft(
                                minText = nextMin.toString(),
                                maxText = nextMax.toString(),
                                storeFeeText = (lastAdmin + 5_000L).toString()
                            )
                        )
                    }
                    expandedTierIdx = digitalTierRows.lastIndex
                },
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Sapphire50, contentColor = AccentBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("product_digital_add_tier_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Biaya Admin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        DigitalTierLivePreviewCard(
            productName = productName,
            sellingPriceText = sellingPriceText,
            isFlexiblePrice = isFlexiblePrice,
            previewNominalText = previewNominalText,
            selectedTierGroupId = selectedTierGroupId,
            adminTiers = adminTiers,
            digitalTierRows = digitalTierRows,
            onPreviewNominalChange = onPreviewNominalChange
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("Akun Kas Layanan Terkait:", fontSize = 11.sp, color = Slate600)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cashAccounts) { ca ->
                DesignFilterChip(
                    label = ca.accountName,
                    selected = selectedServiceCashId == ca.id,
                    onClick = { onSelectServiceCash(if (selectedServiceCashId == ca.id) null else ca.id) }
                )
            }
        }
    }
}

@Composable
private fun DigitalTierDraftRowCard(
    idx: Int,
    row: DigitalTierRangeDraft,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRemove: () -> Unit,
    onUpdateRow: (DigitalTierRangeDraft) -> Unit
) {
    val minLabel = row.minText.toDoubleOrNull()?.let { "Rp ${formatRupiah(it)}" } ?: "Rp 0"
    val maxLabel = row.maxText.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { "Rp ${formatRupiah(it)}" } ?: "∞"
    val feeLabel = row.storeFeeText.toDoubleOrNull()?.let { "Rp ${formatRupiah(it)}" } ?: "Rp 0"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BgCanvas,
        border = BorderStroke(1.dp, if (isExpanded) AccentBlue.copy(alpha = 0.6f) else BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tingkat #${idx + 1}: $minLabel – $maxLabel",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Admin Toko $feeLabel",
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp).tabular()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp).testTag("product_digital_remove_tier_$idx")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Kurangi / Hapus Biaya Admin", tint = Rose500, modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup rincian" else "Edit rincian",
                        tint = Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesignLabeledTextField(
                        label = "Dari",
                        value = row.minText,
                        onValueChange = { v -> onUpdateRow(row.copy(minText = v.filter { it.isDigit() })) },
                        prefixText = "Rp ",
                        placeholder = "1",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    DesignLabeledTextField(
                        label = "Sampai",
                        value = row.maxText,
                        onValueChange = { v -> onUpdateRow(row.copy(maxText = v.filter { it.isDigit() })) },
                        prefixText = "Rp ",
                        placeholder = "Seterusnya",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesignLabeledTextField(
                        label = "Biaya Admin",
                        value = row.storeFeeText,
                        onValueChange = { v -> onUpdateRow(row.copy(storeFeeText = v.filter { it.isDigit() })) },
                        prefixText = "Rp ",
                        placeholder = "5000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f)
                    )
                    DesignLabeledTextField(
                        label = "Penyedia",
                        value = row.providerFeeText,
                        onValueChange = { v -> onUpdateRow(row.copy(providerFeeText = v.filter { it.isDigit() })) },
                        prefixText = "Rp ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    DesignLabeledTextField(
                        label = "Fee Agen",
                        value = row.agentFeeText,
                        onValueChange = { v -> onUpdateRow(row.copy(agentFeeText = v.filter { it.isDigit() })) },
                        prefixText = "Rp ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalTierLivePreviewCard(
    productName: String,
    sellingPriceText: String,
    isFlexiblePrice: Boolean,
    previewNominalText: String,
    selectedTierGroupId: Long?,
    adminTiers: List<AdminTierWithRanges>,
    digitalTierRows: List<DigitalTierRangeDraft>,
    onPreviewNominalChange: (String) -> Unit
) {
    val draftRangesForPreview = remember(digitalTierRows.toList(), selectedTierGroupId) {
        digitalTierRows.mapIndexedNotNull { idx, row ->
            val minV = row.minText.toDoubleOrNull() ?: return@mapIndexedNotNull null
            val maxV = row.maxText.toDoubleOrNull() ?: 0.0
            val storeV = row.storeFeeText.toDoubleOrNull() ?: return@mapIndexedNotNull null
            AdminTierRangeEntity(
                id = (idx + 1).toLong(),
                serviceId = selectedTierGroupId ?: 0L,
                minAmount = minV,
                maxAmount = maxV,
                storeAdminFee = storeV,
                providerAdminFee = row.providerFeeText.toDoubleOrNull() ?: 0.0,
                agentCommission = row.agentFeeText.toDoubleOrNull() ?: 0.0
            )
        }
    }
    val activeTierWithRanges = remember(adminTiers, selectedTierGroupId, productName, draftRangesForPreview) {
        if (draftRangesForPreview.isNotEmpty()) {
            val baseService = selectedTierGroupId?.let { id -> adminTiers.find { it.service.id == id }?.service }
                ?: AdminTierServiceEntity(id = 0L, serviceName = productName.ifBlank { "Layanan Ini" }, serviceCode = "CUSTOM")
            AdminTierWithRanges(service = baseService, ranges = draftRangesForPreview)
        } else {
            selectedTierGroupId?.let { id -> adminTiers.find { it.service.id == id } }
                ?: TierPricingResolver.resolveServiceGroup(adminTiers, null, productName)
        }
    }
    val activeNominal = if (!isFlexiblePrice) (sellingPriceText.toDoubleOrNull() ?: 0.0) else (previewNominalText.toDoubleOrNull() ?: 0.0)
    val autoTierResult = remember(activeTierWithRanges, activeNominal) {
        TierPricingResolver.calculate(activeTierWithRanges, activeNominal)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Sapphire50,
        border = BorderStroke(1.dp, Sapphire200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Otomatisasi Tarif Berjenjang", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800))
                activeTierWithRanges?.let { resolved ->
                    Text(resolved.service.serviceName, style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold))
                }
            }

            if (isFlexiblePrice) {
                DesignLabeledTextField(
                    label = "Nominal Transaksi / Uji Deteksi (Rp)",
                    value = previewNominalText,
                    onValueChange = onPreviewNominalChange,
                    prefixText = "Rp ",
                    placeholder = "mis. 345000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("product_dialog_preview_nominal")
                )
            } else {
                Text(
                    "Nominal dideteksi real-time dari kolom Harga Jual: Rp ${formatRupiah(activeNominal)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
                )
            }

            val matched = autoTierResult.matchedRange
            if (matched != null) {
                val maxLabel = if (matched.maxAmount > 0.0) "Rp ${formatRupiah(matched.maxAmount)}" else "Seterusnya"
                DesignStatusBadge(text = "Cocok rentang: Rp ${formatRupiah(matched.minAmount)} – $maxLabel", type = BadgeType.SUCCESS)
            } else if (activeNominal > 0.0) {
                DesignStatusBadge(text = "Nominal di luar tabel rentang tarif", type = BadgeType.WARNING)
            }

            DesignLabeledTextField(
                label = "Tarif Admin / Biaya Layanan (Terisi Otomatis)",
                value = autoTierResult.storeAdminFee.toLong().toString(),
                onValueChange = {},
                prefixText = "Rp ",
                enabled = false,
                modifier = Modifier.testTag("product_dialog_auto_admin_fee")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Laba Bersih: Rp ${formatRupiah(autoTierResult.netProfit)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.Bold)
                )
                Text(
                    "Total Tagihan: Rp ${formatRupiah(autoTierResult.totalCustomerPayable)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate900, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ProductBarcodeSection(
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onAutoGenerateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        DesignLabeledTextField(
            label = "Barcode / SKU",
            value = barcode,
            onValueChange = onBarcodeChange,
            placeholder = "Scan atau ketik kode",
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onScanClick,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, AccentBlue),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.height(50.dp).testTag("product_dialog_scan_barcode_btn")
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Scan", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onAutoGenerateClick,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderInput),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Auto", fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProductVariantsSection(
    variantRows: SnapshotStateList<VariantRowDraft>,
    defaultPriceText: String
) {
    Column {
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
                onClick = { variantRows.add(VariantRowDraft("Varian ${variantRows.size + 1}", defaultPriceText.ifBlank { "10000" }, "10")) },
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
}

@Composable
private fun ProductWholesaleSection(
    wholesaleRows: SnapshotStateList<WholesaleRowDraft>
) {
    Column {
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
}

@Composable
private fun ProductTierQtySection(
    tierRows: SnapshotStateList<TierRowDraft>,
    duplicateQtyKeys: Set<Int>
) {
    Column {
        HorizontalDivider(color = BorderDefault)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Harga per jumlah beli", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tierRows.forEachIndexed { idx, row ->
                val qtyInt = row.qtyText.toIntOrNull()
                val isDup = qtyInt != null && duplicateQtyKeys.contains(qtyInt)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
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
                    IconButton(onClick = { tierRows.removeAt(idx) }, modifier = Modifier.padding(top = 24.dp)) {
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
}

@Composable
private fun ProductUnitConversionsSection(
    productId: Long,
    baseUnit: String,
    conversionsList: SnapshotStateList<UnitConversionEntity>
) {
    var conversionUnit by remember(productId) { mutableStateOf("") }
    var conversionMultiplier by remember(productId) { mutableStateOf("") }

    Column {
        HorizontalDivider(color = BorderDefault)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Satuan grosir (opsional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            DesignLabeledTextField(label = "Satuan (dus)", value = conversionUnit, onValueChange = { conversionUnit = it }, modifier = Modifier.weight(1f))
            DesignLabeledTextField(
                label = "Isi ($baseUnit)",
                value = conversionMultiplier,
                onValueChange = { conversionMultiplier = it.filter { ch -> ch.isDigit() || ch == '.' } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val mult = conversionMultiplier.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
                    if (conversionUnit.isNotBlank()) {
                        conversionsList.add(UnitConversionEntity(productId = productId, unitName = conversionUnit.trim(), multiplier = mult))
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
        conversionsList.forEachIndexed { idx, conv ->
            val multStr = if (conv.multiplier % 1.0 == 0.0) conv.multiplier.toLong().toString() else conv.multiplier.toString()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("• 1 ${conv.unitName} = $multStr $baseUnit", style = MaterialTheme.typography.bodySmall, color = Slate600)
                IconButton(onClick = { conversionsList.removeAt(idx) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus satuan", tint = Rose500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ManageTierPricingDialog(
    adminTiers: List<AdminTierWithRanges>,
    onCreateService: (name: String, code: String) -> Unit,
    onDeleteService: (AdminTierServiceEntity) -> Unit,
    onCreateRange: (serviceId: Long, minAmount: Double, maxAmount: Double, storeFee: Double, providerFee: Double, agentFee: Double) -> Unit,
    onUpdateRange: (AdminTierRangeEntity) -> Unit,
    onDeleteRange: (AdminTierRangeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedServiceId by remember(adminTiers) {
        mutableStateOf(adminTiers.firstOrNull()?.service?.id ?: 0L)
    }
    var newServiceName by remember { mutableStateOf("") }
    var showAddServiceRow by remember { mutableStateOf(false) }

    var minText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }
    var storeFeeText by remember { mutableStateOf("") }
    var providerFeeText by remember { mutableStateOf("0") }
    var agentFeeText by remember { mutableStateOf("0") }
    var rangeFormError by remember { mutableStateOf<String?>(null) }

    val selectedTier = adminTiers.find { it.service.id == selectedServiceId } ?: adminTiers.firstOrNull()
    val sortedRanges = remember(selectedTier) {
        selectedTier?.ranges?.sortedBy { it.minAmount } ?: emptyList()
    }

    LaunchedEffect(selectedTier?.service?.id, sortedRanges.size) {
        if (minText.isBlank() && storeFeeText.isBlank()) {
            val last = sortedRanges.lastOrNull()
            if (last == null) {
                minText = "1"
                maxText = "1000000"
                storeFeeText = "5000"
            } else if (last.maxAmount > 0.0) {
                val nextMin = last.maxAmount.toLong() + 1L
                minText = nextMin.toString()
                maxText = if (nextMin == 1_000_001L) "3000000" else (nextMin + 1_999_999L).toString()
                storeFeeText = (last.storeAdminFee.toLong() + 5_000L).toString()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Kelola Tarif Berjenjang",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                            Text(
                                "Atur banyak rentang nominal dan biaya admin otomatis",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }
                    HorizontalDivider(color = BorderDefault)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grup Layanan Digital", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700))
                        Text(
                            if (showAddServiceRow) "Batal" else "+ Grup Baru",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { showAddServiceRow = !showAddServiceRow }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (showAddServiceRow) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            DesignLabeledTextField(
                                label = "Nama Layanan Baru",
                                value = newServiceName,
                                onValueChange = { newServiceName = it },
                                placeholder = "mis. Top Up E-Wallet",
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newServiceName.isNotBlank()) {
                                        val clean = newServiceName.trim()
                                        onCreateService(clean, clean.uppercase().replace(" ", "_"))
                                        newServiceName = ""
                                        showAddServiceRow = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Buat", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(adminTiers, key = { it.service.id }) { tier ->
                            DesignFilterChip(
                                label = tier.service.serviceName,
                                selected = selectedTier?.service?.id == tier.service.id,
                                onClick = {
                                    selectedServiceId = tier.service.id
                                    minText = ""
                                    maxText = ""
                                    storeFeeText = ""
                                    rangeFormError = null
                                }
                            )
                        }
                    }
                }

                if (selectedTier != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tambah Biaya Admin (${selectedTier.service.serviceName})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    IconButton(
                                        onClick = { onDeleteService(selectedTier.service) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Grup", tint = Rose500, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesignLabeledTextField(
                                        label = "Nominal Dari *",
                                        value = minText,
                                        onValueChange = {
                                            minText = it.filter { ch -> ch.isDigit() }
                                            rangeFormError = null
                                        },
                                        prefixText = "Rp ",
                                        placeholder = "1",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    DesignLabeledTextField(
                                        label = "Sampai (0 = seterusnya)",
                                        value = maxText,
                                        onValueChange = {
                                            maxText = it.filter { ch -> ch.isDigit() }
                                            rangeFormError = null
                                        },
                                        prefixText = "Rp ",
                                        placeholder = "1000000",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesignLabeledTextField(
                                        label = "Tarif Admin Toko *",
                                        value = storeFeeText,
                                        onValueChange = {
                                            storeFeeText = it.filter { ch -> ch.isDigit() }
                                            rangeFormError = null
                                        },
                                        prefixText = "Rp ",
                                        placeholder = "5000",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    DesignLabeledTextField(
                                        label = "Biaya Penyedia",
                                        value = providerFeeText,
                                        onValueChange = { providerFeeText = it.filter { ch -> ch.isDigit() } },
                                        prefixText = "Rp ",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    DesignLabeledTextField(
                                        label = "Fee Agen",
                                        value = agentFeeText,
                                        onValueChange = { agentFeeText = it.filter { ch -> ch.isDigit() } },
                                        prefixText = "Rp ",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }

                                if (rangeFormError != null) {
                                    DesignStatusBadge(text = rangeFormError!!, type = BadgeType.DANGER)
                                }

                                Button(
                                    onClick = {
                                        val minV = minText.toDoubleOrNull()
                                        val maxV = maxText.toDoubleOrNull() ?: 0.0
                                        val storeV = storeFeeText.toDoubleOrNull()
                                        val provV = providerFeeText.toDoubleOrNull() ?: 0.0
                                        val agentV = agentFeeText.toDoubleOrNull() ?: 0.0

                                        when {
                                            minV == null || storeV == null -> {
                                                rangeFormError = "Isi Nominal Dari dan Tarif Admin Toko"
                                            }
                                            maxV > 0.0 && minV > maxV -> {
                                                rangeFormError = "Nominal 'Dari' tidak boleh lebih besar dari 'Sampai'"
                                            }
                                            TierPricingResolver.hasOverlap(sortedRanges, minV, maxV) -> {
                                                rangeFormError = "Rentang bertumpukan — periksa batas 'Sampai' rentang sebelumnya"
                                            }
                                            else -> {
                                                onCreateRange(selectedTier.service.id, minV, maxV, storeV, provV, agentV)
                                                if (maxV > 0.0) {
                                                    val nextMin = maxV.toLong() + 1L
                                                    minText = nextMin.toString()
                                                    maxText = if (nextMin == 1_000_001L) "3000000" else (nextMin + 1_999_999L).toString()
                                                    storeFeeText = (storeV.toLong() + 5_000L).toString()
                                                } else {
                                                    minText = ""
                                                    maxText = ""
                                                    storeFeeText = ""
                                                }
                                                rangeFormError = null
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah Biaya Admin", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Daftar Rentang Aktif (${sortedRanges.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                        )
                    }

                    if (sortedRanges.isEmpty()) {
                        item {
                            Text(
                                "Belum ada aturan rentang pada layanan ini.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                    } else {
                        itemsIndexed(sortedRanges, key = { _, r -> r.id }) { idx, range ->
                            var editMin by remember(range.id, range.minAmount) { mutableStateOf(range.minAmount.toLong().toString()) }
                            var editMax by remember(range.id, range.maxAmount) {
                                mutableStateOf(if (range.maxAmount > 0.0) range.maxAmount.toLong().toString() else "")
                            }
                            var editStoreFee by remember(range.id, range.storeAdminFee) { mutableStateOf(range.storeAdminFee.toLong().toString()) }
                            var editProviderFee by remember(range.id, range.providerAdminFee) { mutableStateOf(range.providerAdminFee.toLong().toString()) }
                            var editAgentFee by remember(range.id, range.agentCommission) { mutableStateOf(range.agentCommission.toLong().toString()) }

                            val parsedMin = editMin.toDoubleOrNull() ?: range.minAmount
                            val parsedMax = editMax.toDoubleOrNull() ?: 0.0
                            val parsedStore = editStoreFee.toDoubleOrNull() ?: range.storeAdminFee
                            val parsedProv = editProviderFee.toDoubleOrNull() ?: range.providerAdminFee
                            val parsedAgent = editAgentFee.toDoubleOrNull() ?: range.agentCommission
                            val hasEdits = parsedMin != range.minAmount ||
                                parsedMax != range.maxAmount ||
                                parsedStore != range.storeAdminFee ||
                                parsedProv != range.providerAdminFee ||
                                parsedAgent != range.agentCommission
                            val hasInvalidBounds = parsedMax > 0.0 && parsedMin > parsedMax

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceWhite,
                                border = BorderStroke(1.dp, if (hasEdits) AccentBlue else BorderDefault),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Tingkat #${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (hasEdits && !hasInvalidBounds) {
                                                TextButton(
                                                    onClick = {
                                                        onUpdateRange(
                                                            range.copy(
                                                                minAmount = parsedMin,
                                                                maxAmount = parsedMax,
                                                                storeAdminFee = parsedStore,
                                                                providerAdminFee = parsedProv,
                                                                agentCommission = parsedAgent
                                                            )
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Simpan", fontWeight = FontWeight.Bold, color = AccentBlue, fontSize = 11.sp)
                                                }
                                            }
                                            TextButton(
                                                onClick = { onDeleteRange(range) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Rentang", tint = Rose500, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Hapus", color = Rose500, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DesignLabeledTextField(
                                            label = "Dari",
                                            value = editMin,
                                            onValueChange = { editMin = it.filter { ch -> ch.isDigit() } },
                                            prefixText = "Rp ",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        DesignLabeledTextField(
                                            label = "Sampai",
                                            value = editMax,
                                            onValueChange = { editMax = it.filter { ch -> ch.isDigit() } },
                                            prefixText = "Rp ",
                                            placeholder = "Seterusnya",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DesignLabeledTextField(
                                            label = "Tarif Admin",
                                            value = editStoreFee,
                                            onValueChange = { editStoreFee = it.filter { ch -> ch.isDigit() } },
                                            prefixText = "Rp ",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        DesignLabeledTextField(
                                            label = "Penyedia",
                                            value = editProviderFee,
                                            onValueChange = { editProviderFee = it.filter { ch -> ch.isDigit() } },
                                            prefixText = "Rp ",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        DesignLabeledTextField(
                                            label = "Fee Agen",
                                            value = editAgentFee,
                                            onValueChange = { editAgentFee = it.filter { ch -> ch.isDigit() } },
                                            prefixText = "Rp ",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(0.9f)
                                        )
                                    }
                                    if (hasInvalidBounds) {
                                        Text(
                                            "Nominal 'Dari' tidak boleh lebih besar dari 'Sampai'",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Rose500, fontSize = 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesai", fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }
            }
        }
    }
}

