package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AdminTierWithRanges
import com.example.data.entity.BundlePackageEntity
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CashierShiftEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.PendingSaleEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductTierPriceEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.PromoEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.CartItem
import com.example.hardware.BarcodeInputSanitizer
import com.example.hardware.ScannerFeedbackPlayer
import com.example.ui.components.BadgeType
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.KasLiteSearchBar
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.ScannerSettingsDialog
import com.example.ui.components.formatRupiah
import com.example.ui.components.hardwareBarcodeCaptureModifier
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.matchesProductSearchQuery
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCashierScreen(viewModel: PosViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val unitConversions by viewModel.unitConversions.collectAsStateWithLifecycle()
    val allProductTierPrices by viewModel.allProductTierPrices.collectAsStateWithLifecycle()
    val cartDiscount by viewModel.cartDiscount.collectAsStateWithLifecycle()
    val cartTaxRate by viewModel.cartTaxRate.collectAsStateWithLifecycle()

    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val pendingSales by viewModel.pendingSales.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val adminTiers by viewModel.adminTiers.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val productVariants by viewModel.productVariants.collectAsStateWithLifecycle()
    val bundles by viewModel.bundles.collectAsStateWithLifecycle()
    val promos by viewModel.promos.collectAsStateWithLifecycle()
    val scannerSettings by viewModel.scannerSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val decimalProductIds = remember(products) {
        products.filter { it.allowDecimalQty }.map { it.id }.toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedProductType by remember { mutableStateOf("SEMUA") } // SEMUA, FISIK, DIGITAL, PAKET
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showScannerSettingsDialog by remember { mutableStateOf(false) }

    var isCartSheetOpen by remember { mutableStateOf(false) }
    var isPaymentModalOpen by remember { mutableStateOf(false) }
    var isCustomerPickerOpen by remember { mutableStateOf(false) }
    var selectedProductForUnit by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductForVariant by remember { mutableStateOf<ProductEntity?>(null) }

    var isHoldSaleDialogOpen by remember { mutableStateOf(false) }
    var isPendingSalesDialogOpen by remember { mutableStateOf(false) }
    var isStartShiftDialogOpen by remember { mutableStateOf(false) }
    var isCloseShiftDialogOpen by remember { mutableStateOf(false) }
    var isShiftCashDialogOpen by remember { mutableStateOf(false) }
    var flexiblePriceProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var editingDigitalCartItem by remember { mutableStateOf<CartItem?>(null) }

    val productImageMap = remember(products) { products.associate { it.id to it.imageUri } }
    val cashAccountNameMap = remember(cashAccounts) { cashAccounts.associate { it.id to it.accountName } }

    fun handleProductClick(
        prod: ProductEntity,
        conversions: List<UnitConversionEntity>,
        variants: List<ProductVariantEntity>
    ) {
        when {
            prod.hasVariants && variants.isNotEmpty() -> selectedProductForVariant = prod
            prod.productType == "DIGITAL" || prod.isFlexiblePrice -> {
                editingDigitalCartItem = null
                flexiblePriceProduct = prod
            }
            conversions.isNotEmpty() -> selectedProductForUnit = prod
            else -> viewModel.addToCart(prod)
        }
    }

    fun handleBarcodeScanned(rawCode: String) {
        val code = BarcodeInputSanitizer.cleanBarcodeValue(rawCode, scannerSettings.stripWhitespace)
        if (code.isEmpty()) return
        val matchedVariant = productVariants.firstOrNull {
            it.isActive && it.barcode.isNotBlank() && BarcodeInputSanitizer.matchesBarcodeCode(it.barcode, code)
        }
        if (matchedVariant != null) {
            val parentProd = products.firstOrNull { it.id == matchedVariant.productId && it.isActive }
            if (parentProd != null) {
                ScannerFeedbackPlayer.playSuccess(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                viewModel.addToCart(parentProd, variant = matchedVariant)
                searchQuery = ""
                return
            }
        }
        val matchedProd = products.firstOrNull {
            it.isActive && it.barcode.isNotBlank() && BarcodeInputSanitizer.matchesBarcodeCode(it.barcode, code)
        }
        if (matchedProd != null) {
            ScannerFeedbackPlayer.playSuccess(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
            val convs = unitConversions.filter { it.productId == matchedProd.id }
            val vars = productVariants.filter { it.productId == matchedProd.id && it.isActive }
            handleProductClick(matchedProd, convs, vars)
            searchQuery = ""
        } else {
            ScannerFeedbackPlayer.playWarning(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
            searchQuery = code
        }
    }

    val filteredProducts = remember(products, productVariants, searchQuery, selectedCategory, selectedProductType) {
        if (selectedProductType == "PAKET") emptyList()
        else products.filter { p ->
            val matchQuery = matchesProductSearchQuery(p, searchQuery, productVariants)
            val matchCat = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
            val isDigital = p.productType == "DIGITAL"
            val matchType = when (selectedProductType) {
                "FISIK" -> !isDigital
                "DIGITAL" -> isDigital
                else -> true
            }
            p.isActive && matchQuery && matchCat && matchType
        }
    }

    val filteredBundles = remember(bundles, searchQuery, selectedCategory, selectedProductType) {
        if ((selectedProductType == "SEMUA" || selectedProductType == "PAKET") && selectedCategory == null) {
            bundles.filter { b ->
                b.isActive && (searchQuery.isBlank() || b.name.contains(searchQuery, ignoreCase = true))
            }
        } else emptyList()
    }

    val cartSubtotal = remember(cart) { cart.sumOf { it.unitPrice * it.quantity } }
    val cartItemStoreAdmin = remember(cart) { cart.sumOf { it.storeAdminFee } }
    val cartTax = remember(cartSubtotal, cartDiscount, cartTaxRate) {
        ((cartSubtotal - cartDiscount) * cartTaxRate).coerceAtLeast(0.0)
    }
    val cartFinalTotal = remember(cartSubtotal, cartItemStoreAdmin, cartDiscount, cartTax) {
        (cartSubtotal + cartItemStoreAdmin - cartDiscount + cartTax).coerceAtLeast(0.0)
    }

    val anyDialogOpen = showBarcodeScanner ||
        showScannerSettingsDialog ||
        isCartSheetOpen ||
        isPaymentModalOpen ||
        isCustomerPickerOpen ||
        selectedProductForUnit != null ||
        selectedProductForVariant != null ||
        isHoldSaleDialogOpen ||
        isPendingSalesDialogOpen ||
        isStartShiftDialogOpen ||
        isCloseShiftDialogOpen ||
        isShiftCashDialogOpen ||
        flexiblePriceProduct != null ||
        editingDigitalCartItem != null

    val hardwareModifier = hardwareBarcodeCaptureModifier(
        enabled = scannerSettings.autoCaptureHardwareInput && !isSearchFocused && !anyDialogOpen,
        scannerSettings = scannerSettings,
        onBarcodeScanned = { code, _ -> handleBarcodeScanned(code) }
    )

    // Adaptive Layout: Dual-column on Tablet (maxWidth >= 720dp) vs Single column on Phone
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("pos_cashier_screen")
            .then(hardwareModifier)
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // TABLET: DUAL COLUMN (Left: Catalog Grid, Right: Cart Panel)
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CashierShiftBanner(
                            activeShift = activeShift,
                            onStartShift = { isStartShiftDialogOpen = true },
                            onCloseShift = { isCloseShiftDialogOpen = true },
                            onShiftCashMovement = { isShiftCashDialogOpen = true }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KasLiteSearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = "Cari produk atau scan barcode...",
                                onBarcodeScanClick = { showBarcodeScanner = true },
                                onBarcodeSubmit = { handleBarcodeScanned(it) },
                                onFocusChanged = { isSearchFocused = it },
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = { viewModel.navigateTo(Screen.SCAN_BARCODE) },
                                shape = RoundedCornerShape(10.dp),
                                color = ModuleKasir,
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("pos_open_scan_cart_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Keranjang", tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mode Scan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        ProductTypePillsRow(
                            selectedType = selectedProductType,
                            onSelectType = { selectedProductType = it }
                        )

                        CategoryPillsRow(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { selectedCategory = it }
                        )
                    }
                    HorizontalDivider(color = BorderDefault)

                    ProductCatalogGrid(
                        products = filteredProducts,
                        bundles = filteredBundles,
                        productVariants = productVariants,
                        cart = cart,
                        unitConversions = unitConversions,
                        adminTiers = adminTiers,
                        cashAccountNameMap = cashAccountNameMap,
                        onBundleClick = { viewModel.addBundleToCart(it) },
                        onProductClick = { prod, conversions, variants ->
                            handleProductClick(prod, conversions, variants)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(BorderDefault)
                )

                // Right Cart Pane
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .background(SurfaceWhite)
                        .padding(16.dp)
                ) {
                    KasLiteOrderCartPanel(
                        cart = cart,
                        productImages = productImageMap,
                        decimalProductIds = decimalProductIds,
                        allTierPrices = allProductTierPrices,
                        selectedCustomer = selectedCustomer,
                        cartSubtotal = cartSubtotal,
                        cartDiscount = cartDiscount,
                        cartTax = cartTax,
                        cartFinalTotal = cartFinalTotal,
                        pendingCount = pendingSales.size,
                        onOpenCustomerPicker = { isCustomerPickerOpen = true },
                        onClearCart = { viewModel.clearCart() },
                        onUpdateQty = { item, qty ->
                            viewModel.updateCartItemQuantity(
                                productId = item.productId,
                                unitName = item.unitName,
                                newQty = qty,
                                variantId = item.variantId,
                                bundleId = item.bundleId,
                                lineId = item.lineId
                            )
                        },
                        onUpdateManualTotal = { item, total ->
                            viewModel.updateCartItemManualTotal(
                                productId = item.productId,
                                unitName = item.unitName,
                                totalAmount = total,
                                lineId = item.lineId
                            )
                        },
                        onRemoveItem = { item ->
                            viewModel.removeFromCart(
                                productId = item.productId,
                                unitName = item.unitName,
                                variantId = item.variantId,
                                bundleId = item.bundleId,
                                lineId = item.lineId
                            )
                        },
                        onEditDigitalItem = { item ->
                            val prod = products.find { it.id == item.productId }
                            if (prod != null) {
                                editingDigitalCartItem = item
                                flexiblePriceProduct = prod
                            }
                        },
                        onHoldCart = { isHoldSaleDialogOpen = true },
                        onOpenPendingSales = { isPendingSalesDialogOpen = true },
                        onPayClick = { isPaymentModalOpen = true }
                    )
                }
            }
        } else {
            // PHONE: SINGLE COLUMN with Sticky Bottom Bar & Bottom Sheet
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashierShiftBanner(
                        activeShift = activeShift,
                        onStartShift = { isStartShiftDialogOpen = true },
                        onCloseShift = { isCloseShiftDialogOpen = true },
                        onShiftCashMovement = { isShiftCashDialogOpen = true }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KasLiteSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Cari produk atau scan barcode...",
                            onBarcodeScanClick = { showBarcodeScanner = true },
                            onBarcodeSubmit = { handleBarcodeScanned(it) },
                            onFocusChanged = { isSearchFocused = it },
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { viewModel.navigateTo(Screen.SCAN_BARCODE) },
                            shape = RoundedCornerShape(10.dp),
                            color = ModuleKasir,
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("pos_open_scan_cart_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Keranjang", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ProductTypePillsRow(
                        selectedType = selectedProductType,
                        onSelectType = { selectedProductType = it }
                    )

                    CategoryPillsRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                }
                HorizontalDivider(color = BorderDefault)

                Box(modifier = Modifier.weight(1f)) {
                    ProductCatalogGrid(
                        products = filteredProducts,
                        bundles = filteredBundles,
                        productVariants = productVariants,
                        cart = cart,
                        unitConversions = unitConversions,
                        adminTiers = adminTiers,
                        cashAccountNameMap = cashAccountNameMap,
                        onBundleClick = { viewModel.addBundleToCart(it) },
                        onProductClick = { prod, conversions, variants ->
                            handleProductClick(prod, conversions, variants)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Phone Elevated Sticky Floating Cart Bar
                Surface(
                    color = if (cart.isNotEmpty()) Sapphire900 else SurfaceWhite,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, if (cart.isNotEmpty()) ModuleKasir.copy(alpha = 0.45f) else BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCartSheetOpen = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        if (cart.isNotEmpty()) {
                                            Badge(containerColor = ModuleKasir, contentColor = Color.White) {
                                                Text("${cart.sumOf { ceil(it.quantity).toInt().coerceAtLeast(1) }}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = "Lihat Keranjang",
                                        tint = if (cart.isNotEmpty()) Color.White else AccentBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Rp ${formatRupiah(cartFinalTotal)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (cart.isNotEmpty()) Color.White else Slate900
                                    ).tabular()
                                )
                            }
                            Text(
                                text = if (cart.isEmpty()) {
                                    if (pendingSales.isNotEmpty()) "${pendingSales.size} transaksi tertahan" else "Keranjang kosong • Ketuk lihat"
                                } else {
                                    "${cart.size} item • Ketuk rincian keranjang"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (cart.isNotEmpty()) Color.White.copy(alpha = 0.78f) else Slate500
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (pendingSales.isNotEmpty() && cart.isEmpty()) {
                                OutlinedButton(
                                    onClick = { isPendingSalesDialogOpen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AmberWarningBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text("Tertahan (${pendingSales.size})", color = Amber500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    if (cart.isNotEmpty()) isPaymentModalOpen = true
                                    else isCartSheetOpen = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cart.isNotEmpty()) ModuleKasir else AccentBlue,
                                    contentColor = Color.White,
                                    disabledContainerColor = DisabledBg,
                                    disabledContentColor = DisabledText
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = cart.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("pos_pay_button")
                            ) {
                                Text("Bayar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Barcode Kasir",
            scannerSettings = scannerSettings,
            onOpenScannerSettings = {
                showBarcodeScanner = false
                showScannerSettingsDialog = true
            },
            onBarcodeDetected = { code ->
                showBarcodeScanner = false
                handleBarcodeScanned(code)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showScannerSettingsDialog) {
        ScannerSettingsDialog(
            currentSettings = scannerSettings,
            onSaveSettings = { updated -> viewModel.updateScannerSettings(updated) },
            onDismiss = { showScannerSettingsDialog = false }
        )
    }

    // Modal Bottom Sheet: Full Cart Details (on Phone)
    if (isCartSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isCartSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                KasLiteOrderCartPanel(
                    cart = cart,
                    productImages = productImageMap,
                    decimalProductIds = decimalProductIds,
                    allTierPrices = allProductTierPrices,
                    selectedCustomer = selectedCustomer,
                    cartSubtotal = cartSubtotal,
                    cartDiscount = cartDiscount,
                    cartTax = cartTax,
                    cartFinalTotal = cartFinalTotal,
                    pendingCount = pendingSales.size,
                    onOpenCustomerPicker = { isCustomerPickerOpen = true },
                    onClearCart = { viewModel.clearCart() },
                    onUpdateQty = { item, qty ->
                        viewModel.updateCartItemQuantity(
                            productId = item.productId,
                            unitName = item.unitName,
                            newQty = qty,
                            variantId = item.variantId,
                            bundleId = item.bundleId,
                            lineId = item.lineId
                        )
                    },
                    onUpdateManualTotal = { item, total ->
                        viewModel.updateCartItemManualTotal(
                            productId = item.productId,
                            unitName = item.unitName,
                            totalAmount = total,
                            lineId = item.lineId
                        )
                    },
                    onRemoveItem = { item ->
                        viewModel.removeFromCart(
                            productId = item.productId,
                            unitName = item.unitName,
                            variantId = item.variantId,
                            bundleId = item.bundleId,
                            lineId = item.lineId
                        )
                    },
                    onEditDigitalItem = { item ->
                        val prod = products.find { it.id == item.productId }
                        if (prod != null) {
                            isCartSheetOpen = false
                            editingDigitalCartItem = item
                            flexiblePriceProduct = prod
                        }
                    },
                    onHoldCart = {
                        isCartSheetOpen = false
                        isHoldSaleDialogOpen = true
                    },
                    onOpenPendingSales = {
                        isCartSheetOpen = false
                        isPendingSalesDialogOpen = true
                    },
                    onPayClick = {
                        isCartSheetOpen = false
                        isPaymentModalOpen = true
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Payment Dialog / Modal
    if (isPaymentModalOpen) {
        KasLitePaymentDialog(
            totalAmount = cartFinalTotal,
            selectedCustomer = selectedCustomer,
            cashAccounts = cashAccounts,
            promos = promos,
            onDismiss = { isPaymentModalOpen = false },
            onConfirmPayment = { method, cashPaid, notes, accountId, splitCash, splitNonCash, splitMethod, storeAdmin, providerAdmin, agentFee, shippingCost, additionalFee, promoDiscount, appliedPromoName, depositUsed, isPreOrder ->
                isPaymentModalOpen = false
                viewModel.checkoutSale(
                    paymentMethod = method,
                    amountPaid = cashPaid,
                    notes = notes,
                    storeAdminFee = storeAdmin,
                    providerAdminFee = providerAdmin,
                    agentCommission = agentFee,
                    cashAccountId = accountId,
                    splitCashAmount = splitCash,
                    splitNonCashAmount = splitNonCash,
                    splitPaymentMethod = splitMethod,
                    shippingCost = shippingCost,
                    additionalFee = additionalFee,
                    promoDiscount = promoDiscount,
                    appliedPromoName = appliedPromoName,
                    depositUsed = depositUsed,
                    isPreOrder = isPreOrder
                )
            }
        )
    }

    // Customer Picker Dialog
    if (isCustomerPickerOpen) {
        CustomerPickerDialog(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onSelect = {
                viewModel.selectCustomerForSale(it)
                isCustomerPickerOpen = false
            },
            onDismiss = { isCustomerPickerOpen = false }
        )
    }

    // Variant picker dialog
    if (selectedProductForVariant != null) {
        val prod = selectedProductForVariant!!
        val variants = productVariants.filter { it.productId == prod.id && it.isActive }
        Dialog(onDismissRequest = { selectedProductForVariant = null }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih Varian: ${prod.name}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800))
                    variants.forEach { v ->
                        Surface(
                            onClick = {
                                viewModel.addToCart(prod, variant = v)
                                selectedProductForVariant = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Sapphire50,
                            border = BorderStroke(1.dp, Sapphire200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(v.name, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text("Stok: ${v.stock.toInt()} ${prod.baseUnit}", fontSize = 11.sp, color = Slate600)
                                }
                                Text("Rp ${formatRupiah(v.sellingPrice)}", fontWeight = FontWeight.Bold, color = Emerald700)
                            }
                        }
                    }
                    OutlinedButton(onClick = { selectedProductForVariant = null }, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
                }
            }
        }
    }

    // Multi-unit dialog for selected product
    if (selectedProductForUnit != null) {
        val prod = selectedProductForUnit!!
        val conversions = unitConversions.filter { it.productId == prod.id }
        MultiUnitPickerDialog(
            product = prod,
            conversions = conversions,
            onSelectUnit = { unitName, multiplier, price ->
                viewModel.addToCart(prod, unitName, multiplier, price)
                selectedProductForUnit = null
            },
            onDismiss = { selectedProductForUnit = null }
        )
    }

    // Flexible Price & Tier Dialog for Digital / Transfer items
    if (flexiblePriceProduct != null) {
        val prod = flexiblePriceProduct!!
        val editingItem = editingDigitalCartItem
        FlexiblePriceAndTierDialog(
            product = prod,
            adminTiers = adminTiers,
            cashAccounts = cashAccounts,
            initialCartItem = editingItem,
            onConfirm = { price, storeAdmin, providerAdmin, agentFee, cashAccountId, direction ->
                if (editingItem != null) {
                    viewModel.updateDigitalCartItem(
                        lineId = editingItem.lineId,
                        nominal = price,
                        storeAdminFee = storeAdmin,
                        providerAdminFee = providerAdmin,
                        agentCommission = agentFee,
                        serviceCashAccountId = cashAccountId,
                        serviceDirection = direction
                    )
                } else {
                    viewModel.addToCart(
                        product = prod,
                        customPrice = price,
                        storeAdminFee = storeAdmin,
                        providerAdminFee = providerAdmin,
                        agentCommission = agentFee,
                        serviceCashAccountId = cashAccountId,
                        serviceDirection = direction
                    )
                }
                editingDigitalCartItem = null
                flexiblePriceProduct = null
            },
            onDismiss = {
                editingDigitalCartItem = null
                flexiblePriceProduct = null
            }
        )
    }

    // Shift Kas Masuk / Keluar Dialog
    if (isShiftCashDialogOpen && activeShift != null) {
        var isCashIn by remember { mutableStateOf(true) }
        var amtText by remember { mutableStateOf("50000") }
        var reasonText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { isShiftCashDialogOpen = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kas Laci Masuk / Keluar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    DesignSegmentedTabs(
                        options = listOf("IN" to "Kas Masuk (+)", "OUT" to "Kas Keluar (-)"),
                        selectedKey = if (isCashIn) "IN" else "OUT",
                        onSelect = { isCashIn = (it == "IN") }
                    )
                    DesignLabeledTextField("Nominal", amtText, { amtText = it.filter { c -> c.isDigit() } }, prefixText = "Rp ")
                    DesignLabeledTextField("Catatan", reasonText, { reasonText = it }, placeholder = "mis. Tambah uang kembalian / beli galon")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isShiftCashDialogOpen = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amtText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.recordShiftCashMovement(amt, isCashIn, reasonText.ifBlank { if (isCashIn) "Kas masuk laci" else "Kas keluar laci" })
                                    isShiftCashDialogOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
                        ) { Text("Catat", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // Hold Sale Dialog
    if (isHoldSaleDialogOpen) {
        HoldSaleDialog(
            defaultName = selectedCustomer?.name ?: "Pesanan #${System.currentTimeMillis() % 10000}",
            onConfirm = { holdName, notes ->
                viewModel.holdCurrentCart(holdName, notes)
                isHoldSaleDialogOpen = false
            },
            onDismiss = { isHoldSaleDialogOpen = false }
        )
    }

    // Pending Sales List Dialog
    if (isPendingSalesDialogOpen) {
        PendingSalesListDialog(
            pendingSales = pendingSales,
            onResumeSale = { pending ->
                viewModel.resumeHoldSale(pending)
                isPendingSalesDialogOpen = false
            },
            onDeleteSale = { id ->
                viewModel.deletePendingSale(id)
            },
            onDismiss = { isPendingSalesDialogOpen = false }
        )
    }

    // Start Shift Dialog
    if (isStartShiftDialogOpen) {
        StartShiftDialog(
            defaultCashierName = activeUserName,
            onConfirm = { name, startCash, notes ->
                viewModel.startShift(name, startCash, notes)
                isStartShiftDialogOpen = false
            },
            onDismiss = { isStartShiftDialogOpen = false }
        )
    }

    // Close Shift Dialog
    if (isCloseShiftDialogOpen && activeShift != null) {
        CloseShiftDialog(
            shift = activeShift!!,
            onConfirm = { actualCash, notes ->
                viewModel.closeShift(activeShift!!.id, actualCash, notes)
                isCloseShiftDialogOpen = false
            },
            onDismiss = { isCloseShiftDialogOpen = false }
        )
    }
}

@Composable
fun CashierShiftBanner(
    activeShift: CashierShiftEntity?,
    onStartShift: () -> Unit,
    onCloseShift: () -> Unit,
    onShiftCashMovement: () -> Unit = {}
) {
    if (activeShift != null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Emerald50,
            border = BorderStroke(1.dp, Emerald200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald700),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Shift: ${activeShift.cashierName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            DesignStatusBadge(text = "Aktif", type = BadgeType.SUCCESS)
                        }
                        Text(
                            "Kas laci Rp ${formatRupiah(activeShift.startingCash + activeShift.totalCashSales + activeShift.cashIn - activeShift.cashOut)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate600)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onShiftCashMovement,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Kas Laci", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCloseShift,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500)
                    ) {
                        Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AmberWarningBg.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, AmberWarningBorder.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Amber400),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = Slate900, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Shift belum dibuka",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            "Catat modal awal kas laci",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate600)
                        )
                    }
                }

                Button(
                    onClick = onStartShift,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Buka Shift", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductTypePillsRow(
    selectedType: String,
    onSelectType: (String) -> Unit
) {
    DesignSegmentedTabs(
        options = listOf(
            "SEMUA" to "Semua",
            "FISIK" to "Fisik",
            "DIGITAL" to "Digital",
            "PAKET" to "Paket"
        ),
        selectedKey = selectedType,
        onSelect = onSelectType
    )
}

@Composable
fun CategoryPillsRow(
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            DesignFilterChip(
                label = "Semua",
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) }
            )
        }
        items(categories) { cat ->
            val isSelected = selectedCategory == cat.name
            DesignFilterChip(
                label = cat.name,
                selected = isSelected,
                onClick = { onSelectCategory(if (isSelected) null else cat.name) }
            )
        }
    }
}

@Composable
fun ProductCatalogGrid(
    products: List<ProductEntity>,
    bundles: List<BundlePackageEntity> = emptyList(),
    productVariants: List<ProductVariantEntity> = emptyList(),
    cart: List<CartItem>,
    unitConversions: List<UnitConversionEntity>,
    adminTiers: List<AdminTierWithRanges> = emptyList(),
    cashAccountNameMap: Map<Long, String> = emptyMap(),
    onBundleClick: (BundlePackageEntity) -> Unit = {},
    onProductClick: (ProductEntity, List<UnitConversionEntity>, List<ProductVariantEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty() && bundles.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            DesignEmptyState(
                icon = Icons.Default.Inventory2,
                title = "Produk tidak ditemukan",
                subtitle = "Coba kata kunci atau kategori lain"
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
        ) {
            items(bundles, key = { "bundle_${it.id}" }) { bundle ->
                val bCount = cart.filter { it.bundleId == bundle.id }.sumOf { it.quantity.toInt() }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(if (bCount > 0) 1.5.dp else 1.dp, if (bCount > 0) AccentBlue else BorderDefault),
                    modifier = Modifier.fillMaxWidth().clickable { onBundleClick(bundle) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(10.dp)).background(Emerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Emerald700, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(bundle.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Rp ${formatRupiah(bundle.sellingPrice)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900).tabular()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DesignStatusBadge(text = if (bCount > 0) "$bCount di keranjang" else "Paket Bundling", type = BadgeType.SUCCESS)
                    }
                }
            }

            items(products, key = { it.id }) { product ->
                val conversions = unitConversions.filter { it.productId == product.id }
                val variants = productVariants.filter { it.productId == product.id && it.isActive }
                val cartCount = cart.filter { it.productId == product.id }.sumOf { ceil(it.quantity).toInt().coerceAtLeast(1) }
                val autoAdminFee = if (product.productType == "DIGITAL" && !product.isFlexiblePrice && product.sellingPrice > 0.0) {
                    val tierSvc = TierPricingResolver.resolveServiceGroup(adminTiers, product.adminTierGroupId, product.name)
                    TierPricingResolver.calculate(tierSvc, product.sellingPrice).storeAdminFee
                } else 0.0
                val linkedAccountName = product.serviceCashAccountId?.let { cashAccountNameMap[it] }

                KasLiteProductCard(
                    product = product,
                    cartCount = cartCount,
                    hasConversions = conversions.isNotEmpty(),
                    hasVariants = variants.isNotEmpty(),
                    autoAdminFee = autoAdminFee,
                    linkedAccountName = linkedAccountName,
                    onClick = { onProductClick(product, conversions, variants) }
                )
            }
        }
    }
}

@Composable
fun KasLiteProductCard(
    product: ProductEntity,
    cartCount: Int,
    hasConversions: Boolean,
    hasVariants: Boolean = false,
    autoAdminFee: Double = 0.0,
    linkedAccountName: String? = null,
    onClick: () -> Unit
) {
    val isDigitalService = product.productType == "DIGITAL"
    val noStock = isDigitalService || product.isUnlimitedStock || product.hasNoStock
    val isMinusStock = !noStock && product.stock < 0
    val isOutOfStock = !noStock && product.stock == 0.0
    val isLowStock = !noStock && !isOutOfStock && !isMinusStock && product.stock <= product.minStock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) SurfaceStrong.copy(alpha = 0.7f) else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (cartCount > 0) 1.5.dp else 1.dp,
            color = if (cartCount > 0) AccentBlue else BorderDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isOutOfStock) 0.78f else 1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ProductThumbnail(
                    imageUri = product.imageUri,
                    contentDescription = product.name,
                    isDigital = isDigitalService,
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 28.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Slate900,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isDigitalService && product.strikePrice != null && product.strikePrice > 0) {
                    Text(
                        text = "Rp ${formatRupiah(product.strikePrice)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate400,
                            textDecoration = TextDecoration.LineThrough,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when {
                        isDigitalService && (product.isFlexiblePrice || product.sellingPrice <= 0.0) -> "Input Nominal Transfer"
                        product.isFlexiblePrice -> "Harga fleksibel"
                        else -> "Rp ${formatRupiah(product.sellingPrice)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isDigitalService && (product.isFlexiblePrice || product.sellingPrice <= 0.0)) 13.sp else 15.sp,
                        color = if (isDigitalService && (product.isFlexiblePrice || product.sellingPrice <= 0.0)) HeaderTeal else Slate900
                    ).tabular()
                )
                if (autoAdminFee > 0) {
                    Text(
                        text = "+Admin otomatis Rp ${formatRupiah(autoAdminFee)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HeaderTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ).tabular()
                    )
                } else if (isDigitalService) {
                    Text(
                        text = if (product.adminTierGroupId != null) "Admin Berjenjang Otomatis" else "Admin Otomatis / Manual",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HeaderTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!linkedAccountName.isNullOrBlank()) {
                    Text(
                        text = "Kas: $linkedAccountName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        isMinusStock -> DesignStatusBadge(text = "Stok minus ${product.stock.toInt()}", type = BadgeType.DANGER)
                        isOutOfStock -> DesignStatusBadge(text = "Habis", type = BadgeType.DANGER)
                        isLowStock -> DesignStatusBadge(text = "Menipis ${product.stock.toInt()}", type = BadgeType.WARNING)
                        isDigitalService -> Text(
                            text = if (product.serviceDirection == "TARIK") "Digital • Tarik" else "Digital • Transfer",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                        )
                        noStock -> Text(
                            text = "Tanpa stok",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Normal)
                        )
                        else -> Text(
                            text = "Stok ${if (product.allowDecimalQty) product.stock.toString() else product.stock.toInt().toString()}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Normal)
                        )
                    }

                    if (hasVariants) {
                        Text("Varian", style = MaterialTheme.typography.labelSmall.copy(color = ModuleProduk, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    } else if (hasConversions) {
                        Text("Multi", style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }

            if (cartCount > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentBlue,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = "$cartCount di keranjang",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KasLiteOrderCartPanel(
    cart: List<CartItem>,
    productImages: Map<Long, String> = emptyMap(),
    decimalProductIds: Set<Long> = emptySet(),
    allTierPrices: List<ProductTierPriceEntity> = emptyList(),
    selectedCustomer: CustomerEntity?,
    cartSubtotal: Double,
    cartDiscount: Double,
    cartTax: Double,
    cartFinalTotal: Double,
    pendingCount: Int = 0,
    onOpenCustomerPicker: () -> Unit,
    onClearCart: () -> Unit,
    onUpdateQty: (item: CartItem, qty: Double) -> Unit,
    onUpdateManualTotal: (item: CartItem, totalAmount: Double) -> Unit = { _, _ -> },
    onRemoveItem: (item: CartItem) -> Unit,
    onEditDigitalItem: (item: CartItem) -> Unit = {},
    onHoldCart: () -> Unit = {},
    onOpenPendingSales: () -> Unit = {},
    onPayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Keranjang",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Slate900, fontSize = 18.sp)
            )
            Text(
                text = "Kosongkan",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (cart.isNotEmpty()) AccentBlue else Slate400,
                    fontSize = 13.sp
                ),
                modifier = Modifier.clickable(enabled = cart.isNotEmpty()) { onClearCart() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenCustomerPicker,
                shape = RoundedCornerShape(8.dp),
                color = SurfaceStrong,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCustomer?.name ?: "Pelanggan Umum",
                        style = MaterialTheme.typography.labelMedium.copy(color = Slate800, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Ganti", style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold))
                }
            }

            if (cart.isNotEmpty()) {
                Surface(
                    onClick = onHoldCart,
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderDefault)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PauseCircleOutline, contentDescription = null, tint = Slate600, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tahan", style = MaterialTheme.typography.labelSmall.copy(color = Slate700, fontWeight = FontWeight.Bold))
                    }
                }
            }

            if (pendingCount > 0) {
                Surface(
                    onClick = onOpenPendingSales,
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarningBg,
                    border = BorderStroke(1.dp, AmberWarningBorder)
                ) {
                    Text(
                        text = "Tertahan ($pendingCount)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate900, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderDefault)

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Slate400, modifier = Modifier.size(38.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Belum ada barang. Ketuk produk di kiri untuk menambah.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(cart, key = { it.lineId }) { item ->
                    val isDigitalLine = item.serviceNominal != null || item.priceSource == "fleksibel" || item.storeAdminFee > 0.0
                    val isDecimal = !isDigitalLine && decimalProductIds.contains(item.productId)
                    val step = if (isDecimal) 0.5 else 1.0
                    val productTiers = remember(allTierPrices, item.productId) {
                        allTierPrices.filter { it.productId == item.productId }
                    }
                    val hasTierPrices = !isDigitalLine && productTiers.isNotEmpty() && item.multiplier == 1.0 && item.variantId == null
                    val matchedTier = remember(productTiers, item.quantity, hasTierPrices) {
                        if (hasTierPrices) productTiers.find { abs(it.quantity - item.quantity) < 0.01 } else null
                    }
                    val missingTierPrice = hasTierPrices && matchedTier == null && item.priceSource != "manual"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDigitalLine) Modifier.clickable { onEditDigitalItem(item) }
                                else Modifier
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductThumbnail(
                                imageUri = productImages[item.productId] ?: "",
                                contentDescription = item.productName,
                                isDigital = isDigitalLine,
                                size = 36.dp,
                                shape = RoundedCornerShape(8.dp),
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.variantName.isNotBlank()) "${item.productName} [${item.variantName}]" else item.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isDigitalLine) {
                                    val dirLabel = if (item.serviceDirection == "TARIK") "Tarik Tunai" else "Transfer / Setor"
                                    Text(
                                        text = "$dirLabel: Rp ${formatRupiah(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 11.sp).tabular()
                                    )
                                    Text(
                                        text = if (item.storeAdminFee > 0) {
                                            "+ Admin Otomatis Rp ${formatRupiah(item.storeAdminFee)}"
                                        } else {
                                            "Admin Rp 0 • Ketuk untuk ubah"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (item.storeAdminFee > 0) HeaderTeal else Slate500,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ).tabular()
                                    )
                                } else {
                                    val sourceLabel = when (item.priceSource) {
                                        "per_jumlah" -> " • harga per jumlah"
                                        "grosir" -> " • harga grosir"
                                        "manual" -> " • manual"
                                        else -> ""
                                    }
                                    Text(
                                        text = "${formatRupiah(item.unitPrice)} per ${item.unitName}$sourceLabel",
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (item.priceSource == "grosir" || item.priceSource == "per_jumlah") Emerald700 else Slate500, fontSize = 11.sp).tabular()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (isDigitalLine && item.quantity == 1.0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { onEditDigitalItem(item) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("cart_edit_digital_${item.lineId}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Ubah nominal & admin",
                                            tint = AccentBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveItem(item) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("cart_remove_${item.lineId}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus item digital",
                                            tint = Rose500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = { onUpdateQty(item, item.quantity - step) },
                                        shape = CircleShape,
                                        color = SurfaceStrong,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = Slate700, modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Text(
                                        text = if (isDecimal) "${item.quantity}" else "${item.quantity.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular(),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )

                                    Surface(
                                        onClick = { onUpdateQty(item, item.quantity + step) },
                                        shape = CircleShape,
                                        color = SurfaceStrong,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Slate700, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Rp ${formatRupiah(item.unitPrice * item.quantity + item.storeAdminFee)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 13.sp
                                ).tabular(),
                                textAlign = TextAlign.End
                            )
                        }

                        if (missingTierPrice) {
                            Spacer(modifier = Modifier.height(8.dp))
                            var manualTotalInput by remember(item.lineId, item.quantity) {
                                mutableStateOf("${(item.unitPrice * item.quantity).toLong()}")
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmberWarningBg,
                                border = BorderStroke(1.dp, AmberWarningBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Slate900,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Harga untuk jumlah ini belum diatur",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceWhite,
                                        border = BorderStroke(1.dp, AmberWarningBorder.copy(alpha = 0.7f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        DesignLabeledTextField(
                                            label = "Harga total (manual) *",
                                            value = manualTotalInput,
                                            onValueChange = { raw ->
                                                manualTotalInput = raw
                                                val parsed = raw.toDoubleOrNull()
                                                if (parsed != null && parsed >= 0.0) {
                                                    onUpdateManualTotal(item, parsed)
                                                }
                                            },
                                            prefixText = "Rp ",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = SurfaceStrong)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderDefault)
        Spacer(modifier = Modifier.height(10.dp))

        val cartItemAdminTotal = remember(cart) { cart.sumOf { it.storeAdminFee } }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp))
            Text("Rp ${formatRupiah(cartSubtotal)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate800, fontSize = 13.sp))
        }
        if (cartItemAdminTotal > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Biaya Admin Layanan", style = MaterialTheme.typography.bodySmall.copy(color = HeaderTeal, fontSize = 13.sp))
                Text("+Rp ${formatRupiah(cartItemAdminTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = HeaderTeal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))
            }
        }
        if (cartDiscount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diskon", style = MaterialTheme.typography.bodySmall.copy(color = Rose500))
                Text("-Rp ${formatRupiah(cartDiscount)}", style = MaterialTheme.typography.bodySmall.copy(color = Rose500))
            }
        }
        if (cartTax > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pajak (PPN)", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                Text("+Rp ${formatRupiah(cartTax)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate800))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate900))
            Text(
                "Rp ${formatRupiah(cartFinalTotal)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                contentColor = Color.White,
                disabledContainerColor = DisabledBg,
                disabledContentColor = DisabledText
            ),
            enabled = cart.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pos_panel_pay_button")
        ) {
            Text(
                "Bayar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun KasLitePaymentDialog(
    totalAmount: Double,
    selectedCustomer: CustomerEntity?,
    cashAccounts: List<CashAccountEntity> = emptyList(),
    promos: List<PromoEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmPayment: (
        method: String,
        cashPaid: Double,
        notes: String,
        cashAccountId: Long?,
        splitCashAmount: Double,
        splitNonCashAmount: Double,
        splitPaymentMethod: String,
        storeAdminFee: Double,
        providerAdminFee: Double,
        agentCommission: Double,
        shippingCost: Double,
        additionalFee: Double,
        promoDiscount: Double,
        appliedPromoName: String,
        depositUsed: Double,
        isPreOrder: Boolean
    ) -> Unit
) {
    var isManualAdminOverride by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("TUNAI") } // TUNAI, QRIS, TRANSFER, SPLIT, KREDIT
    val activeCashAccounts = remember(cashAccounts) { cashAccounts.filter { it.isActive } }
    val defaultAccount = activeCashAccounts.firstOrNull { it.isDefault } ?: activeCashAccounts.firstOrNull()
    var selectedAccountId by remember(activeCashAccounts) { mutableStateOf<Long?>(defaultAccount?.id) }
    var shippingText by remember { mutableStateOf("0") }
    var additionalFeeText by remember { mutableStateOf("0") }
    var selectedPromo by remember { mutableStateOf<PromoEntity?>(null) }
    var useDeposit by remember { mutableStateOf(false) }
    var isPreOrder by remember { mutableStateOf(false) }

    val shippingCost = shippingText.toDoubleOrNull() ?: 0.0
    val additionalFee = additionalFeeText.toDoubleOrNull() ?: 0.0
    val promoDiscount = if (selectedPromo != null && totalAmount >= selectedPromo!!.minPurchase) selectedPromo!!.discountAmount else 0.0
    val grandTotal = (totalAmount - promoDiscount + shippingCost + additionalFee).coerceAtLeast(0.0)
    val maxDeposit = selectedCustomer?.depositBalance ?: 0.0
    val depositUsed = if (useDeposit && maxDeposit > 0) minOf(maxDeposit, grandTotal) else 0.0
    val payableAmount = (grandTotal - depositUsed).coerceAtLeast(0.0)

    var cashPaidText by remember(payableAmount) { mutableStateOf("${payableAmount.roundToLong()}") }
    val defaultHalfSplit = remember(payableAmount) { (payableAmount / 2.0).roundToLong() }
    var splitCashText by remember(payableAmount) { mutableStateOf("$defaultHalfSplit") }
    var splitNonCashText by remember(payableAmount) { mutableStateOf("${(payableAmount.roundToLong() - defaultHalfSplit).coerceAtLeast(0L)}") }
    var splitNonCashMethod by remember { mutableStateOf("QRIS") }
    var notes by remember { mutableStateOf("") }

    val cashPaid = cashPaidText.toDoubleOrNull() ?: 0.0
    val change = (cashPaid - payableAmount).coerceAtLeast(0.0)

    val splitCash = splitCashText.toDoubleOrNull() ?: 0.0
    val splitNonCash = splitNonCashText.toDoubleOrNull() ?: 0.0
    val splitTotal = splitCash + splitNonCash
    val splitDiff = splitTotal - payableAmount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWhite,
            tonalElevation = 6.dp,
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
                            "Konfirmasi Pembayaran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }
                    HorizontalDivider(color = BorderDefault)
                }

                item {
                    Surface(
                        color = BgCanvas,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (depositUsed > 0) "SISA DIBAYAR (Setelah Deposit Rp ${formatRupiah(depositUsed)})" else "TOTAL PEMBAYARAN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate500)
                            )
                            Text(
                                "Rp ${formatRupiah(payableAmount)}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                            )
                        }
                    }
                }

                // Voucher / Promo & Biaya Tambahan
                if (promos.any { it.isActive }) {
                    item {
                        Text("Promo / Voucher Aktif", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700))
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(promos.filter { it.isActive }) { pr ->
                                val eligible = totalAmount >= pr.minPurchase
                                DesignFilterChip(
                                    label = "${pr.name} (-Rp ${formatRupiah(pr.discountAmount)})",
                                    selected = selectedPromo?.id == pr.id,
                                    onClick = {
                                        if (eligible) {
                                            selectedPromo = if (selectedPromo?.id == pr.id) null else pr
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesignLabeledTextField(
                            label = "Ongkir",
                            value = shippingText,
                            onValueChange = { shippingText = it.filter { c -> c.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        DesignLabeledTextField(
                            label = "Biaya Tambahan",
                            value = additionalFeeText,
                            onValueChange = { additionalFeeText = it.filter { c -> c.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedCustomer != null && selectedCustomer.depositBalance > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gunakan Deposit (${selectedCustomer.name}: Rp ${formatRupiah(selectedCustomer.depositBalance)})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Emerald700),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = useDeposit,
                                onCheckedChange = { useDeposit = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Emerald700)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Simpan sebagai Pre-Order (tanpa potong stok dulu)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Slate800),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isPreOrder,
                            onCheckedChange = { isPreOrder = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber500)
                        )
                    }
                }

                item {
                    Text("Metode Pembayaran", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("TUNAI", "QRIS", "TRANSFER", "SPLIT", "KREDIT").forEach { method ->
                            val isSel = paymentMethod == method
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) ChipActive else SurfaceStrong,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { paymentMethod = method }
                            ) {
                                Text(
                                    text = when (method) {
                                        "KREDIT" -> "Bon"
                                        "SPLIT" -> "Split"
                                        else -> method
                                    },
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.White else Slate700
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (activeCashAccounts.isNotEmpty()) {
                    item {
                        Text("Masuk ke Rekening / Kas", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700))
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeCashAccounts) { acc ->
                                val isAccSel = selectedAccountId == acc.id
                                DesignFilterChip(
                                    label = "${acc.accountName} (${if (acc.balance < 0) "-Rp " else "Rp "}${formatRupiah(abs(acc.balance))})",
                                    selected = isAccSel,
                                    onClick = { selectedAccountId = acc.id }
                                )
                            }
                        }
                    }
                }

                if (paymentMethod == "TUNAI") {
                    item {
                        DesignLabeledTextField(
                            label = "Uang Tunai Diterima",
                            value = cashPaidText,
                            onValueChange = { raw -> cashPaidText = raw.filter { it.isDigit() } },
                            prefixText = "Rp ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { cashPaidText = "${payableAmount.roundToLong()}" },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderInput),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text("Pas", style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold))
                            }
                            listOf(10000L, 20000L, 50000L, 100000L).forEach { denom ->
                                OutlinedButton(
                                    onClick = {
                                        val current = cashPaidText.toDoubleOrNull() ?: payableAmount
                                        cashPaidText = "${(current + denom).toLong()}"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderInput),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                ) {
                                    Text("+${denom / 1000}k", style = MaterialTheme.typography.labelSmall.copy(color = Slate700))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Sapphire50,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Sapphire200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Kembalian", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate900))
                                Text(
                                    "Rp ${formatRupiah(change)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                )
                            }
                        }
                    }
                } else if (paymentMethod == "SPLIT") {
                    item {
                        Text("Pembayaran Sebagian Tunai + Non-Tunai", style = MaterialTheme.typography.labelSmall.copy(color = Slate700))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesignLabeledTextField("Tunai", splitCashText, { raw -> splitCashText = raw.filter { it.isDigit() } }, prefixText = "Rp ", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            DesignLabeledTextField("Non-Tunai", splitNonCashText, { raw -> splitNonCashText = raw.filter { it.isDigit() } }, prefixText = "Rp ", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Metode Non-Tunai:", fontSize = 11.sp, color = Slate600)
                            listOf("QRIS", "TRANSFER").forEach { m ->
                                DesignFilterChip(label = m, selected = splitNonCashMethod == m, onClick = { splitNonCashMethod = m })
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (splitDiff >= 0) Emerald100 else Rose100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (splitDiff >= 0) "Lunas (Kembalian Tunai: Rp ${formatRupiah(splitDiff)})" else "Kurang Rp ${formatRupiah(-splitDiff)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (splitDiff >= 0) Emerald700 else Rose500,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                } else if (paymentMethod == "KREDIT") {
                    item {
                        Surface(
                            color = AmberWarningBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AmberWarningBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedCustomer != null)
                                    "Transaksi dicatat sebagai piutang ke: ${selectedCustomer.name}"
                                else
                                    "Pilih pelanggan terlebih dahulu untuk transaksi piutang / bon!",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate900, fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                item {
                    DesignLabeledTextField("Catatan Struk (Opsional)", notes, { notes = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val finalCashPaid = when (paymentMethod) {
                                "TUNAI" -> cashPaid
                                "SPLIT" -> splitCash
                                "KREDIT" -> 0.0
                                else -> payableAmount
                            }
                            onConfirmPayment(
                                paymentMethod,
                                finalCashPaid,
                                notes,
                                selectedAccountId,
                                if (paymentMethod == "SPLIT") splitCash else 0.0,
                                if (paymentMethod == "SPLIT") splitNonCash else 0.0,
                                if (paymentMethod == "SPLIT") splitNonCashMethod else "",
                                0.0,
                                0.0,
                                0.0,
                                shippingCost,
                                additionalFee,
                                promoDiscount,
                                selectedPromo?.name ?: "",
                                depositUsed,
                                isPreOrder
                            )
                        },
                        enabled = isPreOrder || when (paymentMethod) {
                            "TUNAI" -> cashPaid >= payableAmount
                            "SPLIT" -> splitTotal >= payableAmount
                            "KREDIT" -> selectedCustomer != null
                            else -> true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = DisabledBg,
                            disabledContentColor = DisabledText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_checkout_button")
                    ) {
                        Text(
                            if (isPreOrder) "Simpan Pre-Order" else "Bayar & Cetak Struk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoldSaleDialog(
    defaultName: String,
    onConfirm: (holdName: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var holdName by remember { mutableStateOf(defaultName) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Tahan Transaksi (Hold Sale)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Simpan keranjang saat ini untuk dilanjutkan nanti saat pelanggan kembali.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = holdName,
                    onValueChange = { holdName = it },
                    label = { Text("Nama Referensi / Pelanggan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { if (holdName.isNotBlank()) onConfirm(holdName, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tahan", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingSalesListDialog(
    pendingSales: List<PendingSaleEntity>,
    onResumeSale: (PendingSaleEntity) -> Unit,
    onDeleteSale: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaksi Tertahan (${pendingSales.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Slate400)
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

                if (pendingSales.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada transaksi tertahan", color = Slate500)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pendingSales) { pending ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate50,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pending.holdName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                        )
                                        Text(
                                            "Total: Rp ${formatRupiah(pending.totalAmount)} • ${pending.notes.ifEmpty { "Tanpa catatan" }}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onDeleteSale(pending.id) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus antrian", tint = Rose500, modifier = Modifier.size(20.dp))
                                        }
                                        Button(
                                            onClick = { onResumeSale(pending) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Lanjut", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup", color = Slate700)
                }
            }
        }
    }
}

@Composable
fun StartShiftDialog(
    defaultCashierName: String,
    onConfirm: (cashierName: String, startingCash: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var cashierName by remember { mutableStateOf(defaultCashierName) }
    var startingCashText by remember { mutableStateOf("200000") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Buka Shift Kasir",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Masukkan modal uang tunai awal di laci kasir sebelum melayani transaksi.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("Nama Kasir Bertugas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startingCashText,
                    onValueChange = { raw -> startingCashText = raw.filter { it.isDigit() } },
                    label = { Text("Modal Uang Laci Awal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val cash = startingCashText.toDoubleOrNull() ?: 0.0
                            if (cashierName.isNotBlank()) onConfirm(cashierName, cash, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buka Shift", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CloseShiftDialog(
    shift: CashierShiftEntity,
    onConfirm: (actualCash: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val expectedCash = shift.startingCash + shift.totalCashSales + shift.cashIn - shift.cashOut
    var actualCashText by remember { mutableStateOf("${expectedCash.toLong()}") }
    var notes by remember { mutableStateOf("") }

    val actualCash = actualCashText.toDoubleOrNull() ?: 0.0
    val diff = actualCash - expectedCash

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Tutup Shift Kasir",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Rose500)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Kasir: ${shift.cashierName}", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Modal Awal", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                            Text("Rp ${formatRupiah(shift.startingCash)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Penjualan Tunai", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                            Text("Rp ${formatRupiah(shift.totalCashSales)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Emerald700))
                        }
                        if (shift.cashIn > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kas Masuk Laci", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                Text("+Rp ${formatRupiah(shift.cashIn)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Emerald700))
                            }
                        }
                        if (shift.cashOut > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kas Keluar Laci", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                Text("-Rp ${formatRupiah(shift.cashOut)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Rose500))
                            }
                        }
                        HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Uang Laci Sistem", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Rp ${formatRupiah(expectedCash)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Sapphire800))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = actualCashText,
                    onValueChange = { raw -> actualCashText = raw.filter { it.isDigit() } },
                    label = { Text("Hitungan Fisik Uang Laci (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (diff == 0.0) Emerald100 else if (diff > 0) Sapphire100 else Rose500.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            diff == 0.0 -> "✓ Uang Pas (Sesuai catatan sistem)"
                            diff > 0 -> "Lebih: +Rp ${formatRupiah(diff)}"
                            else -> "Kurang (Selisih): -Rp ${formatRupiah(-diff)}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            diff == 0.0 -> Emerald700
                            diff > 0 -> Sapphire800
                            else -> Rose500
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { onConfirm(actualCash, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup Shift", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FlexiblePriceAndTierDialog(
    product: ProductEntity,
    adminTiers: List<AdminTierWithRanges>,
    cashAccounts: List<CashAccountEntity> = emptyList(),
    initialCartItem: CartItem? = null,
    onConfirm: (
        price: Double,
        storeAdmin: Double,
        providerAdmin: Double,
        agentFee: Double,
        cashAccountId: Long?,
        direction: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val isDigitalService = product.productType == "DIGITAL"
    val activeCashAccounts = remember(cashAccounts) { cashAccounts.filter { it.isActive } }

    val defaultService = remember(adminTiers, product.id) {
        TierPricingResolver.resolveServiceGroup(adminTiers, product.adminTierGroupId, product.name)
    }
    var selectedServiceId by remember(defaultService?.service?.id, initialCartItem?.lineId) {
        mutableStateOf(defaultService?.service?.id)
    }
    val activeServiceWithRanges = remember(adminTiers, selectedServiceId) {
        adminTiers.find { it.service.id == selectedServiceId } ?: defaultService
    }

    var selectedDirection by remember(product.id, initialCartItem?.lineId) {
        mutableStateOf(
            initialCartItem?.serviceDirection?.ifBlank { "SETOR" }
                ?: product.serviceDirection.ifBlank { "SETOR" }
        )
    }
    var selectedCashAccountId by remember(product.id, initialCartItem?.lineId) {
        mutableStateOf(initialCartItem?.serviceCashAccountId ?: product.serviceCashAccountId)
    }

    var nominalText by remember(product.id, initialCartItem?.lineId) {
        mutableStateOf(
            when {
                initialCartItem != null && initialCartItem.unitPrice > 0.0 ->
                    initialCartItem.unitPrice.roundToLong().toString()
                !product.isFlexiblePrice && product.sellingPrice > 0.0 ->
                    product.sellingPrice.roundToLong().toString()
                else -> ""
            }
        )
    }
    val nominal = nominalText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val initialCalc = remember(activeServiceWithRanges, nominal) {
        TierPricingResolver.calculate(activeServiceWithRanges, nominal)
    }

    var isManualAdminOverride by remember(product.id, initialCartItem?.lineId) {
        mutableStateOf(
            if (initialCartItem != null) {
                initialCalc.matchedRange == null ||
                    abs(initialCartItem.storeAdminFee - initialCalc.storeAdminFee) >= 1.0
            } else false
        )
    }
    var manualAdminText by remember(product.id, initialCartItem?.lineId) {
        mutableStateOf(
            when {
                initialCartItem != null -> initialCartItem.storeAdminFee.roundToLong().toString()
                initialCalc.matchedRange != null -> initialCalc.storeAdminFee.roundToLong().toString()
                else -> ""
            }
        )
    }

    val effectiveOverride = if (isManualAdminOverride || initialCalc.matchedRange == null) {
        manualAdminText.filter { it.isDigit() }.toDoubleOrNull()
    } else null

    val result = remember(activeServiceWithRanges, nominal, effectiveOverride) {
        TierPricingResolver.calculate(activeServiceWithRanges, nominal, effectiveOverride)
    }

    val dirLabel = if (selectedDirection == "TARIK") "Tarik Tunai" else "Nominal Transfer"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().padding(6.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HeaderTeal)
                    )
                    Text(
                        if (isDigitalService) {
                            "Layanan Digital • Admin Berjenjang Otomatis"
                        } else {
                            "Harga Jual Fleksibel (Input Nominal di Kasir)"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                }

                // Highlighted Top Total Tagihan Pelanggan Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ModuleKasir,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "TOTAL TAGIHAN PELANGGAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rp ${formatRupiah(result.totalCustomerPayable)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ).tabular()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDigitalService) {
                                    "$dirLabel Rp ${formatRupiah(nominal)} + Admin Rp ${formatRupiah(result.storeAdminFee)}"
                                } else {
                                    "Harga Jual Rp ${formatRupiah(nominal)}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                ).tabular()
                            )
                        }
                    }
                }

                if (isDigitalService) {
                    item {
                        Text(
                            "Jenis Transaksi Digital:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate700)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DesignSegmentedTabs(
                            options = listOf("SETOR" to "Transfer / Setor", "TARIK" to "Tarik Tunai"),
                            selectedKey = selectedDirection,
                            onSelect = { selectedDirection = it }
                        )
                    }

                    if (activeCashAccounts.isNotEmpty()) {
                        item {
                            Text(
                                "Akun Kas / Saldo Sumber:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate700)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    DesignFilterChip(
                                        label = "Tidak Ditautkan",
                                        selected = selectedCashAccountId == null,
                                        onClick = { selectedCashAccountId = null }
                                    )
                                }
                                items(activeCashAccounts, key = { it.id }) { acc ->
                                    DesignFilterChip(
                                        label = "${acc.accountName} (Rp ${formatRupiah(acc.balance)})",
                                        selected = selectedCashAccountId == acc.id,
                                        onClick = { selectedCashAccountId = acc.id }
                                    )
                                }
                            }
                        }
                    }

                    val activeTiers = adminTiers.filter { it.service.isActive }
                    if (activeTiers.isNotEmpty()) {
                        item {
                            Text(
                                "Grup Aturan Tarif Admin:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate700)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(activeTiers, key = { it.service.id }) { tier ->
                                    DesignFilterChip(
                                        label = tier.service.serviceName,
                                        selected = tier.service.id == activeServiceWithRanges?.service?.id,
                                        onClick = {
                                            selectedServiceId = tier.service.id
                                            isManualAdminOverride = false
                                            val autoMatch = TierPricingResolver.findMatchingRange(tier.ranges, nominal)
                                            manualAdminText = autoMatch?.storeAdminFee?.roundToLong()?.toString() ?: ""
                                        }
                                    )
                                }
                            }

                            val activeRanges = activeServiceWithRanges?.ranges?.sortedBy { it.minAmount }.orEmpty()
                            if (activeRanges.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Daftar Tarif Berjenjang (${activeRanges.size} tingkat):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Slate600)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(activeRanges, key = { it.id }) { tierRange ->
                                        val isMatched = result.matchedRange?.id == tierRange.id
                                        val maxLabel = if (tierRange.maxAmount <= 0.0) "∞" else formatRupiah(tierRange.maxAmount)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isMatched) Emerald50 else Slate50,
                                            border = BorderStroke(1.dp, if (isMatched) Emerald500 else BorderDefault)
                                        ) {
                                            Text(
                                                text = "Rp ${formatRupiah(tierRange.minAmount)}–$maxLabel: Rp ${formatRupiah(tierRange.storeAdminFee)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isMatched) Emerald700 else Slate700
                                                ).tabular(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmberWarningBg,
                                border = BorderStroke(1.dp, AmberWarningBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada grup tarif admin berjenjang aktif. Isi admin manual di bawah, atau atur preset tarif di menu Produk.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate900),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    DesignLabeledTextField(
                        label = when {
                            isDigitalService && selectedDirection == "TARIK" -> "Berapa uang yang ditarik tunai? *"
                            isDigitalService -> "Berapa uang yang akan ditransfer? *"
                            else -> "Nominal / Harga Jual *"
                        },
                        value = nominalText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            nominalText = digits
                            val newNominal = digits.toDoubleOrNull() ?: 0.0
                            if (isDigitalService) {
                                val autoMatch = activeServiceWithRanges?.let {
                                    TierPricingResolver.findMatchingRange(it.ranges, newNominal)
                                }
                                if (!isManualAdminOverride) {
                                    manualAdminText = autoMatch?.storeAdminFee?.roundToLong()?.toString() ?: ""
                                }
                            }
                        },
                        prefixText = "Rp ",
                        placeholder = "mis. 500000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("digital_nominal_input")
                    )

                    if (isDigitalService) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // 2-Row 8-Button Quick Nominal Grid
                        val presetRows = listOf(
                            listOf(50000L to "50rb", 100000L to "100rb", 200000L to "200rb", 500000L to "500rb"),
                            listOf(1000000L to "1jt", 2000000L to "2jt", 3000000L to "3jt", 5000000L to "5jt")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            presetRows.forEach { rowPresets ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowPresets.forEach { (amt, label) ->
                                        val isSel = nominal.toLong() == amt
                                        Surface(
                                            onClick = {
                                                nominalText = amt.toString()
                                                isManualAdminOverride = false
                                                val autoMatch = activeServiceWithRanges?.let {
                                                    TierPricingResolver.findMatchingRange(it.ranges, amt.toDouble())
                                                }
                                                manualAdminText = autoMatch?.storeAdminFee?.roundToLong()?.toString() ?: ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) ChipActive else SurfaceStrong,
                                            border = BorderStroke(1.dp, if (isSel) ChipActive else BorderDefault),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSel) Color.White else Slate800
                                                    ).tabular()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isDigitalService) {
                    item {
                        val range = result.matchedRange
                        if (range != null) {
                            val maxLabel = if (range.maxAmount <= 0.0) "Seterusnya" else "Rp ${formatRupiah(range.maxAmount)}"
                            DesignStatusBadge(
                                text = "✓ Cocok rentang: Rp ${formatRupiah(range.minAmount)} – $maxLabel",
                                type = BadgeType.SUCCESS
                            )
                        } else if (nominal > 0.0) {
                            DesignStatusBadge(
                                text = "Nominal di luar tabel tarif — isi tarif admin manual",
                                type = BadgeType.WARNING
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        DesignLabeledTextField(
                            label = if (range != null && !isManualAdminOverride) {
                                "Tarif Admin / Biaya Layanan (Otomatis)"
                            } else {
                                "Tarif Admin / Biaya Layanan (Manual)"
                            },
                            value = if (!isManualAdminOverride && range != null) {
                                range.storeAdminFee.roundToLong().toString()
                            } else {
                                manualAdminText
                            },
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }
                                manualAdminText = digits
                                isManualAdminOverride = true
                            },
                            prefixText = "Rp ",
                            placeholder = "0",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.testTag("digital_admin_fee_input")
                        )

                        if (isManualAdminOverride && range != null) {
                            TextButton(
                                onClick = {
                                    isManualAdminOverride = false
                                    manualAdminText = range.storeAdminFee.roundToLong().toString()
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Kembalikan ke tarif otomatis (Rp ${formatRupiah(range.storeAdminFee)})",
                                    style = MaterialTheme.typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            color = Slate50,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Admin Toko (Diterima)", fontSize = 11.sp, color = Slate600)
                                    Text("Rp ${formatRupiah(result.storeAdminFee)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Biaya Penyedia (Potong Saldo)", fontSize = 11.sp, color = Slate600)
                                    Text("-Rp ${formatRupiah(result.providerAdminFee)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Rose500)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Fee Agen Bank", fontSize = 11.sp, color = Slate600)
                                    Text("+Rp ${formatRupiah(result.agentCommission)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sapphire800)
                                }
                                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Laba Bersih Agen", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text("Rp ${formatRupiah(result.netProfit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Tagihan Pelanggan:", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                        Text("Rp ${formatRupiah(result.totalCustomerPayable)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (nominal > 0.0) {
                                    if (isDigitalService) {
                                        onConfirm(
                                            nominal,
                                            result.storeAdminFee,
                                            result.providerAdminFee,
                                            result.agentCommission,
                                            selectedCashAccountId,
                                            selectedDirection
                                        )
                                    } else {
                                        onConfirm(nominal, 0.0, 0.0, 0.0, null, "SETOR")
                                    }
                                }
                            },
                            enabled = nominal > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = HeaderTeal),
                            modifier = Modifier.weight(1f).testTag("digital_confirm_add_btn")
                        ) {
                            Text(
                                if (initialCartItem != null) "Simpan Perubahan" else "Tambah ke Keranjang",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    onSelect: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilih Pelanggan / Kasbon",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Sapphire800
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama / nomor HP...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sapphire800,
                        unfocusedBorderColor = Slate200
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option: Pelanggan Umum / Non-Member
                    item {
                        Surface(
                            onClick = { onSelect(null) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCustomer == null) Sapphire50 else Slate50,
                            border = BorderStroke(
                                1.dp,
                                if (selectedCustomer == null) Sapphire600 else Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Sapphire100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Sapphire800, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Pelanggan Umum (Tunai)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Text("Tanpa catatan bon / tempo", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                }
                            }
                        }
                    }

                    items(filtered) { customer ->
                        val isSelected = selectedCustomer?.id == customer.id
                        Surface(
                            onClick = { onSelect(customer) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Sapphire50 else SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Sapphire600 else Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (customer.totalReceivable > 0) Rose500.copy(alpha = 0.12f) else Emerald100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (customer.totalReceivable > 0) Rose500 else Emerald700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        customer.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Text(
                                        customer.phone.ifEmpty { "Tanpa nomor HP" },
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                    )
                                }
                                if (customer.totalReceivable > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Bon Aktif",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Rose500, fontSize = 10.sp)
                                        )
                                        Text(
                                            "Rp ${formatRupiah(customer.totalReceivable)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Rose500)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Slate700)
                }
            }
        }
    }
}

@Composable
fun MultiUnitPickerDialog(
    product: ProductEntity,
    conversions: List<UnitConversionEntity>,
    onSelectUnit: (unitName: String, multiplier: Double, price: Double) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilih Satuan Penjualan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Sapphire800
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate900),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Base Unit Option
                    Surface(
                        onClick = {
                            onSelectUnit(product.baseUnit, 1.0, product.sellingPrice)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Sapphire50,
                        border = BorderStroke(1.dp, Sapphire200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    product.baseUnit + " (Satuan Dasar)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Sapphire800)
                                )
                                Text("Pengali: 1x", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                            }
                            Text(
                                "Rp ${formatRupiah(product.sellingPrice)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                            )
                        }
                    }

                    // Multi-units from conversions
                    conversions.forEach { conv ->
                        val effectivePrice = conv.customSellingPrice ?: (product.sellingPrice * conv.multiplier)
                        Surface(
                            onClick = {
                                onSelectUnit(conv.unitName, conv.multiplier, effectivePrice)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        conv.unitName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    val multLabel = if (conv.multiplier % 1.0 == 0.0) conv.multiplier.toLong().toString() else conv.multiplier.toString()
                                    Text(
                                        "Isi: $multLabel ${product.baseUnit}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                    )
                                }
                                Text(
                                    "Rp ${formatRupiah(effectivePrice)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = Slate700)
                }
            }
        }
    }
}

