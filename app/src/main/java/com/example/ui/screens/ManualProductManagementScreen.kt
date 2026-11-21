package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.ProductEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BarcodeSearchBar
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.validateManualProductInput
import kotlin.math.roundToInt

@Composable
fun ManualProductManagementScreen(viewModel: PosViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT, BARCODE
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var showSearchScanner by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    // Keep selectedProduct synced with live Room updates (or clear if deleted)
    LaunchedEffect(products) {
        selectedProduct?.let { current ->
            selectedProduct = products.find { it.id == current.id }
        }
    }

    BackHandler {
        if (selectedProduct != null) {
            selectedProduct = null
        } else {
            viewModel.navigateTo(Screen.PRODUK)
        }
    }

    val physicalProducts = remember(products) {
        products.filter { it.productType != "DIGITAL" && !it.isUnlimitedStock && !it.hasNoStock }
    }
    val lowOrOutCount = remember(physicalProducts) {
        physicalProducts.count { it.stock <= it.minStock }
    }
    val totalInventoryValue = remember(physicalProducts) {
        physicalProducts.sumOf { (it.sellingPrice * it.stock.coerceAtLeast(0.0)) }
    }

    val filteredProducts = remember(products, searchQuery, activeFilter) {
        products.filter { product ->
            val isDigital = product.productType == "DIGITAL" || product.isUnlimitedStock || product.hasNoStock
            val matchesFilter = when (activeFilter) {
                "LOW" -> !isDigital && product.stock > 0.0 && product.stock <= product.minStock
                "OUT" -> !isDigital && product.stock <= 0.0
                "BARCODE" -> product.barcode.isNotBlank()
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                product.name.contains(q, ignoreCase = true) ||
                product.barcode.contains(q, ignoreCase = true) ||
                product.category.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("manual_product_management_screen")
    ) {
        val isTablet = maxWidth >= 720.dp

        Scaffold(
            containerColor = BgCanvas,
            floatingActionButton = {
                if (!isTablet) {
                    FloatingActionButton(
                        onClick = {
                            selectedProduct = null
                            showFormDialog = true
                        },
                        containerColor = ModuleProduk,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("manual_add_product_fab")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.manual_products_add_cta)
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ManualCatalogPane(
                        totalProducts = products.size,
                        lowOrOutCount = lowOrOutCount,
                        totalInventoryValue = totalInventoryValue,
                        filteredProducts = filteredProducts,
                        selectedProductId = selectedProduct?.id,
                        searchQuery = searchQuery,
                        activeFilter = activeFilter,
                        onSearchQueryChange = { searchQuery = it },
                        onOpenSearchScanner = { showSearchScanner = true },
                        onSelectFilter = { activeFilter = it },
                        onOpenBarcodeCart = { viewModel.navigateTo(Screen.SCAN_BARCODE) },
                        onNewProductClick = { selectedProduct = null },
                        onSelectProductForEdit = { selectedProduct = it },
                        onStepStock = { product, delta -> viewModel.stepProductStock(product, delta) },
                        onRequestDelete = { productToDelete = it },
                        isTablet = true,
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    )

                    VerticalDividerLine()

                    Surface(
                        color = SurfaceWhite,
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    ) {
                        ManualProductFormContent(
                            editingProduct = selectedProduct,
                            existingProducts = products,
                            onGenerateBarcode = { viewModel.generateRandomBarcode() },
                            onSave = { name, sell, buy, stock, minStock, barcode, category, unit, allowDecimal ->
                                viewModel.saveManualProduct(
                                    existingProduct = selectedProduct,
                                    name = name,
                                    sellingPrice = sell,
                                    purchasePrice = buy,
                                    stock = stock,
                                    minStock = minStock,
                                    barcode = barcode,
                                    category = category,
                                    baseUnit = unit,
                                    allowDecimalQty = allowDecimal
                                )
                                selectedProduct = null
                            },
                            onCancelEdit = { selectedProduct = null },
                            showCloseButton = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        )
                    }
                }
            } else {
                ManualCatalogPane(
                    totalProducts = products.size,
                    lowOrOutCount = lowOrOutCount,
                    totalInventoryValue = totalInventoryValue,
                    filteredProducts = filteredProducts,
                    selectedProductId = selectedProduct?.id,
                    searchQuery = searchQuery,
                    activeFilter = activeFilter,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenSearchScanner = { showSearchScanner = true },
                    onSelectFilter = { activeFilter = it },
                    onOpenBarcodeCart = { viewModel.navigateTo(Screen.SCAN_BARCODE) },
                    onNewProductClick = {
                        selectedProduct = null
                        showFormDialog = true
                    },
                    onSelectProductForEdit = {
                        selectedProduct = it
                        showFormDialog = true
                    },
                    onStepStock = { product, delta -> viewModel.stepProductStock(product, delta) },
                    onRequestDelete = { productToDelete = it },
                    isTablet = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showFormDialog) {
        Dialog(onDismissRequest = { showFormDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                ManualProductFormContent(
                    editingProduct = selectedProduct,
                    existingProducts = products,
                    onGenerateBarcode = { viewModel.generateRandomBarcode() },
                    onSave = { name, sell, buy, stock, minStock, barcode, category, unit, allowDecimal ->
                        viewModel.saveManualProduct(
                            existingProduct = selectedProduct,
                            name = name,
                            sellingPrice = sell,
                            purchasePrice = buy,
                            stock = stock,
                            minStock = minStock,
                            barcode = barcode,
                            category = category,
                            baseUnit = unit,
                            allowDecimalQty = allowDecimal
                        )
                        showFormDialog = false
                        selectedProduct = null
                    },
                    onCancelEdit = {
                        showFormDialog = false
                        selectedProduct = null
                    },
                    showCloseButton = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                )
            }
        }
    }

    productToDelete?.let { item ->
        ManualProductDeleteDialog(
            product = item,
            onConfirm = {
                viewModel.deleteProduct(item)
                if (selectedProduct?.id == item.id) selectedProduct = null
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    if (showSearchScanner) {
        BarcodeScannerDialog(
            title = "Cari Produk via Barcode",
            onBarcodeDetected = { code ->
                showSearchScanner = false
                searchQuery = code
            },
            onDismiss = { showSearchScanner = false }
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(BorderDefault)
    )
}

@Composable
private fun ManualCatalogPane(
    totalProducts: Int,
    lowOrOutCount: Int,
    totalInventoryValue: Double,
    filteredProducts: List<ProductEntity>,
    selectedProductId: Long?,
    searchQuery: String,
    activeFilter: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearchScanner: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onOpenBarcodeCart: () -> Unit,
    onNewProductClick: () -> Unit,
    onSelectProductForEdit: (ProductEntity) -> Unit,
    onStepStock: (ProductEntity, Double) -> Unit,
    onRequestDelete: (ProductEntity) -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Summary Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4C1D95), ModuleProduk)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.manual_products_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalProducts item • Nilai stok Rp ${formatRupiah(totalInventoryValue)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            ).tabular()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onOpenBarcodeCart,
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("manual_products_open_scanner_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Buka Scanner Barcode",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Scan",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isTablet) {
                            Surface(
                                onClick = onNewProductClick,
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("manual_add_product_header_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = ModuleProduk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Baru",
                                        color = ModuleProduk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BarcodeSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Cari nama item, kategori, atau barcode...",
                onBarcodeScanClick = onOpenSearchScanner,
                onBarcodeSubmit = onSearchQueryChange
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    DesignFilterChip(
                        label = "Semua ($totalProducts)",
                        selected = activeFilter == "ALL",
                        onClick = { onSelectFilter("ALL") }
                    )
                }
                item {
                    DesignFilterChip(
                        label = "Stok Menipis ($lowOrOutCount)",
                        selected = activeFilter == "LOW",
                        onClick = { onSelectFilter(if (activeFilter == "LOW") "ALL" else "LOW") }
                    )
                }
                item {
                    DesignFilterChip(
                        label = "Stok Habis",
                        selected = activeFilter == "OUT",
                        onClick = { onSelectFilter(if (activeFilter == "OUT") "ALL" else "OUT") }
                    )
                }
                item {
                    DesignFilterChip(
                        label = "Ada Barcode",
                        selected = activeFilter == "BARCODE",
                        onClick = { onSelectFilter(if (activeFilter == "BARCODE") "ALL" else "BARCODE") }
                    )
                }
            }
        }

        HorizontalDivider(color = BorderDefault)

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DesignEmptyState(
                    icon = Icons.Default.Inventory2,
                    title = stringResource(R.string.manual_products_empty_title),
                    subtitle = stringResource(R.string.manual_products_empty_desc),
                    ctaText = stringResource(R.string.manual_products_add_cta),
                    onCtaClick = onNewProductClick
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("manual_product_list")
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ManualProductItemCard(
                        product = product,
                        isSelected = isTablet && selectedProductId == product.id,
                        onEdit = { onSelectProductForEdit(product) },
                        onDelete = { onRequestDelete(product) },
                        onDecrementStock = { onStepStock(product, -1.0) },
                        onIncrementStock = { onStepStock(product, 1.0) }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun ManualProductItemCard(
    product: ProductEntity,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDecrementStock: () -> Unit,
    onIncrementStock: () -> Unit
) {
    val isDigital = product.productType == "DIGITAL" || product.isUnlimitedStock || product.hasNoStock
    val stockBadgeType = when {
        isDigital -> BadgeType.INFO
        product.stock <= 0.0 -> BadgeType.DANGER
        product.stock <= product.minStock -> BadgeType.WARNING
        else -> BadgeType.SUCCESS
    }
    val stockDisplay = when {
        isDigital -> "Unlimited"
        product.allowDecimalQty || product.stock % 1.0 != 0.0 ->
            "Stok: ${String.format(java.util.Locale.US, "%.2f", product.stock).trimEnd('0').trimEnd('.')} ${product.baseUnit}"
        else -> "Stok: ${product.stock.toInt()} ${product.baseUnit}"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Sapphire50 else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentBlue else BorderDefault
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("manual_product_card_${product.id}")
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
                    size = 44.dp,
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.category} • ${product.barcode.ifBlank { "Tanpa barcode" }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manual_edit_btn_${product.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Ubah ${product.name}",
                        tint = AccentBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manual_delete_btn_${product.id}")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Hapus ${product.name}",
                        tint = Rose500,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rp ${formatRupiah(product.sellingPrice)} / ${product.baseUnit}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        ).tabular()
                    )
                    if (product.purchasePrice > 0.0) {
                        Text(
                            text = "Modal Rp ${formatRupiah(product.purchasePrice)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontSize = 11.sp
                            ).tabular()
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DesignStatusBadge(text = stockDisplay, type = stockBadgeType)

                    if (!isDigital) {
                        Surface(
                            onClick = onDecrementStock,
                            enabled = product.stock > 0.0,
                            shape = CircleShape,
                            color = SurfaceStrong,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("manual_stock_dec_${product.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Kurangi stok ${product.name}",
                                    tint = if (product.stock > 0.0) Slate800 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onIncrementStock,
                            shape = CircleShape,
                            color = ModuleProduk,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("manual_stock_inc_${product.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah stok ${product.name}",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualProductFormContent(
    editingProduct: ProductEntity?,
    existingProducts: List<ProductEntity>,
    onGenerateBarcode: () -> String,
    onSave: (
        name: String,
        sellingPrice: Double,
        purchasePrice: Double,
        stock: Double,
        minStock: Double,
        barcode: String,
        category: String,
        baseUnit: String,
        allowDecimalQty: Boolean
    ) -> Unit,
    onCancelEdit: () -> Unit,
    showCloseButton: Boolean,
    modifier: Modifier = Modifier
) {
    val key = editingProduct?.id ?: -1L
    var name by remember(key) { mutableStateOf(editingProduct?.name ?: "") }
    var sellingPriceText by remember(key) {
        mutableStateOf(editingProduct?.sellingPrice?.let { "${it.toLong()}" } ?: "")
    }
    var purchasePriceText by remember(key) {
        mutableStateOf(
            editingProduct?.purchasePrice?.takeIf { it > 0.0 }?.let { "${it.toLong()}" } ?: ""
        )
    }
    var stockText by remember(key) {
        mutableStateOf(
            editingProduct?.stock?.let {
                if (editingProduct.allowDecimalQty || it % 1.0 != 0.0) it.toString() else "${it.toInt()}"
            } ?: "0"
        )
    }
    var minStockText by remember(key) {
        mutableStateOf(editingProduct?.minStock?.let { "${it.toInt()}" } ?: "5")
    }
    var barcode by remember(key) { mutableStateOf(editingProduct?.barcode ?: "") }
    var category by remember(key) { mutableStateOf(editingProduct?.category ?: "Umum") }
    var baseUnit by remember(key) { mutableStateOf(editingProduct?.baseUnit ?: "pcs") }
    var allowDecimalQty by remember(key) { mutableStateOf(editingProduct?.allowDecimalQty ?: false) }

    var showValidationErrors by remember(key) { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val validation = remember(name, sellingPriceText, stockText, barcode, editingProduct?.id, existingProducts) {
        validateManualProductInput(
            name = name,
            sellingPriceText = sellingPriceText,
            stockText = stockText,
            barcode = barcode,
            editingProductId = editingProduct?.id,
            existingProducts = existingProducts
        )
    }

    val parsedSell = sellingPriceText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedBuy = purchasePriceText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("manual_product_form"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (editingProduct == null) R.string.manual_products_new_title
                        else R.string.manual_products_edit_title
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
                Text(
                    text = stringResource(R.string.manual_products_subtitle),
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
            }
            if (showCloseButton) {
                IconButton(onClick = onCancelEdit) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                }
            } else if (editingProduct != null) {
                TextButton(onClick = onCancelEdit) {
                    Text("Reset Baru", color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = BorderDefault)

        DesignLabeledTextField(
            label = stringResource(R.string.manual_products_field_name),
            value = name,
            onValueChange = { name = it },
            placeholder = "mis. Indomie Goreng 85g",
            isError = showValidationErrors && validation.nameError != null,
            errorMessage = validation.nameError,
            modifier = Modifier.testTag("manual_field_name")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_price),
                value = sellingPriceText,
                onValueChange = { sellingPriceText = it.filter(Char::isDigit) },
                prefixText = "Rp ",
                placeholder = "3500",
                isError = showValidationErrors && validation.priceError != null,
                errorMessage = validation.priceError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_field_price")
            )
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_cost_price),
                value = purchasePriceText,
                onValueChange = { purchasePriceText = it.filter(Char::isDigit) },
                prefixText = "Rp ",
                placeholder = "2800",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_field_cost_price")
            )
        }

        if (parsedSell > 0.0 && parsedBuy > 0.0) {
            val profit = parsedSell - parsedBuy
            val marginPct = ((profit / parsedSell) * 100.0).roundToInt()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (profit >= 0) Emerald50 else Rose100,
                border = BorderStroke(1.dp, if (profit >= 0) Emerald200 else Rose500.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Estimasi Laba: Rp ${formatRupiah(profit)} ($marginPct%)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (profit >= 0) Emerald700 else Rose500
                    ).tabular(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Current Stock Level with +/- Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_stock),
                value = stockText,
                onValueChange = { raw ->
                    stockText = raw.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }
                },
                placeholder = "0",
                isError = showValidationErrors && validation.stockError != null,
                errorMessage = validation.stockError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (allowDecimalQty) KeyboardType.Decimal else KeyboardType.Number
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_field_stock")
            )

            OutlinedButton(
                onClick = {
                    val cur = stockText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                    val next = (cur - 1.0).coerceAtLeast(0.0)
                    stockText = if (allowDecimalQty || next % 1.0 != 0.0) next.toString() else "${next.toInt()}"
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderInput),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("manual_form_stock_minus")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi stok", tint = Slate700)
            }

            Button(
                onClick = {
                    val cur = stockText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                    val next = cur + 1.0
                    stockText = if (allowDecimalQty || next % 1.0 != 0.0) next.toString() else "${next.toInt()}"
                },
                colors = ButtonDefaults.buttonColors(containerColor = ModuleProduk, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("manual_form_stock_plus")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah stok")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_category),
                value = category,
                onValueChange = { category = it },
                placeholder = "Umum",
                modifier = Modifier.weight(1.2f)
            )
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_unit),
                value = baseUnit,
                onValueChange = { baseUnit = it },
                placeholder = "pcs",
                modifier = Modifier.weight(0.9f)
            )
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_min_stock),
                value = minStockText,
                onValueChange = { minStockText = it.filter(Char::isDigit) },
                placeholder = "5",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.9f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            DesignLabeledTextField(
                label = stringResource(R.string.manual_products_field_barcode),
                value = barcode,
                onValueChange = { barcode = it },
                placeholder = "Scan / isi manual",
                isError = showValidationErrors && validation.barcodeError != null,
                errorMessage = validation.barcodeError,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_field_barcode")
            )
            OutlinedButton(
                onClick = { showBarcodeScanner = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AccentBlue),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("manual_scan_barcode_btn")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { barcode = onGenerateBarcode() },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderInput),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("manual_auto_barcode_btn")
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = "Buat Barcode Otomatis", tint = Slate700, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto", fontSize = 12.sp, color = Slate700)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stok Desimal (kg, liter, meter)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                )
                Text(
                    text = "Aktifkan untuk barang timbang atau pecahan",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
            }
            Switch(
                checked = allowDecimalQty,
                onCheckedChange = { allowDecimalQty = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ModuleProduk)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showCloseButton) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderInput),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Batal", color = Slate700, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = {
                    if (validation.isValid) {
                        val finalSell = sellingPriceText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        val finalBuy = purchasePriceText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        val finalStock = stockText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        val finalMin = minStockText.trim().toDoubleOrNull() ?: 5.0
                        onSave(
                            name.trim(),
                            finalSell,
                            finalBuy,
                            finalStock,
                            finalMin,
                            barcode.trim(),
                            category.trim().ifBlank { "Umum" },
                            baseUnit.trim().ifBlank { "pcs" },
                            allowDecimalQty
                        )
                        if (!showCloseButton && editingProduct == null) {
                            name = ""
                            sellingPriceText = ""
                            purchasePriceText = ""
                            stockText = "0"
                            barcode = ""
                            showValidationErrors = false
                        }
                    } else {
                        showValidationErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ModuleProduk, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .heightIn(min = 48.dp)
                    .testTag("manual_save_product_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (editingProduct == null) R.string.manual_products_save_cta
                        else R.string.manual_products_update_cta
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Barcode Produk",
            onBarcodeDetected = { code ->
                barcode = code
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
private fun ManualProductDeleteDialog(
    product: ProductEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.manual_products_delete_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Produk \"${product.name}\" (Rp ${formatRupiah(product.sellingPrice)} • Stok ${product.stock.toInt()} ${product.baseUnit}) akan dihapus dari database Room.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Batal", color = Slate700)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("manual_confirm_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.manual_products_delete_cta), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
