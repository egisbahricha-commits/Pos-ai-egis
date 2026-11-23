package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.CartItem
import com.example.hardware.BluetoothSppScannerClient
import com.example.hardware.DetectedScannerDevice
import com.example.hardware.HardwareDeviceDiscovery
import com.example.hardware.ScannerFeedbackPlayer
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.ScannerSettingsDialog
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarcodeLookupOutcome
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.ceil

private sealed class ScanBannerState {
    data class Matched(
        val code: String,
        val productName: String,
        val unitPrice: Double,
        val productId: Long,
        val unitName: String,
        val variantId: Long?,
        val timestampMs: Long
    ) : ScanBannerState()

    data class Unrecognized(
        val code: String,
        val timestampMs: Long
    ) : ScanBannerState()

    data class Status(
        val message: String,
        val isError: Boolean = false
    ) : ScanBannerState()
}

private data class DetectedBoxUi(
    val normalizedRect: RectF,
    val label: String
)

@Composable
fun BarcodeScanCartScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val adminTiers by viewModel.adminTiers.collectAsStateWithLifecycle()
    val cartDiscount by viewModel.cartDiscount.collectAsStateWithLifecycle()
    val cartTaxRate by viewModel.cartTaxRate.collectAsStateWithLifecycle()
    val scannerSettings by viewModel.scannerSettings.collectAsStateWithLifecycle()

    val productImageMap = remember(products) { products.associate { it.id to it.imageUri } }
    val decimalProductIds = remember(products) { products.filter { it.allowDecimalQty }.map { it.id }.toSet() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var continuousMode by remember(scannerSettings.continuousMode) { mutableStateOf(scannerSettings.continuousMode) }
    var isPaused by remember(scannerSettings.preferredMode) {
        mutableStateOf(scannerSettings.preferredMode == "HARDWARE")
    }
    var torchEnabled by remember(scannerSettings.defaultTorchOn) { mutableStateOf(scannerSettings.defaultTorchOn) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }
    var isManualInputFocused by remember { mutableStateOf(false) }
    var scanBanner by remember { mutableStateOf<ScanBannerState?>(null) }
    var detectedBoxes by remember { mutableStateOf<List<DetectedBoxUi>>(emptyList()) }

    var showScannerSettingsDialog by remember { mutableStateOf(false) }
    var detectedScanners by remember { mutableStateOf(HardwareDeviceDiscovery.detectConnectedScanners(context)) }
    val hardwareKeyBuffer = remember { StringBuilder() }
    var lastHardwareKeyAtMs by remember { mutableLongStateOf(0L) }

    var pendingSelection by remember { mutableStateOf<BarcodeLookupOutcome.NeedsSelection?>(null) }
    var quickRegisterBarcode by remember { mutableStateOf<String?>(null) }
    var flexiblePriceInput by remember { mutableStateOf("") }

    var lastScannedCode by remember { mutableStateOf("") }
    var lastScanElapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showScannerSettingsDialog) {
        if (!showScannerSettingsDialog) {
            detectedScanners = HardwareDeviceDiscovery.detectConnectedScanners(context)
        }
    }

    BackHandler {
        viewModel.navigateTo(Screen.POS_KASIR)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    fun processScannedCode(rawCode: String, fromCameraFrame: Boolean, source: String = if (fromCameraFrame) "CAMERA" else "MANUAL") {
        val cleaned = if (scannerSettings.stripWhitespace) rawCode.trim() else rawCode
        if (cleaned.isEmpty()) return

        if (fromCameraFrame) {
            val now = SystemClock.elapsedRealtime()
            val minDelay = if (cleaned.equals(lastScannedCode, ignoreCase = true)) scannerSettings.scanCooldownMs else 350L
            if (now - lastScanElapsedMs < minDelay) return
            lastScannedCode = cleaned
            lastScanElapsedMs = now
        }

        when (val outcome = viewModel.scanBarcodeAndAddToCart(cleaned, source = source)) {
            is BarcodeLookupOutcome.AddedVariant -> {
                if (scannerSettings.vibrateOnScan) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ScannerFeedbackPlayer.playSuccess(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                scanBanner = ScanBannerState.Matched(
                    code = cleaned,
                    productName = "${outcome.product.name} (${outcome.variant.name})",
                    unitPrice = outcome.variant.sellingPrice,
                    productId = outcome.product.id,
                    unitName = outcome.product.baseUnit,
                    variantId = outcome.variant.id,
                    timestampMs = System.currentTimeMillis()
                )
                if (!continuousMode && fromCameraFrame) isPaused = true
            }
            is BarcodeLookupOutcome.AddedProduct -> {
                if (scannerSettings.vibrateOnScan) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ScannerFeedbackPlayer.playSuccess(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                scanBanner = ScanBannerState.Matched(
                    code = cleaned,
                    productName = outcome.product.name,
                    unitPrice = outcome.product.sellingPrice,
                    productId = outcome.product.id,
                    unitName = outcome.product.baseUnit,
                    variantId = null,
                    timestampMs = System.currentTimeMillis()
                )
                if (!continuousMode && fromCameraFrame) isPaused = true
            }
            is BarcodeLookupOutcome.NeedsSelection -> {
                if (scannerSettings.vibrateOnScan) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ScannerFeedbackPlayer.playSuccess(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                pendingSelection = outcome
                flexiblePriceInput = if (outcome.product.sellingPrice > 0) {
                    outcome.product.sellingPrice.toLong().toString()
                } else ""
                if (fromCameraFrame) isPaused = true
            }
            is BarcodeLookupOutcome.NotFound -> {
                ScannerFeedbackPlayer.playWarning(context, scannerSettings.beepOnScan, scannerSettings.vibrateOnScan)
                scanBanner = ScanBannerState.Unrecognized(
                    code = cleaned,
                    timestampMs = System.currentTimeMillis()
                )
                if (!continuousMode && fromCameraFrame) isPaused = true
            }
        }
    }

    // Listen to Bluetooth SPP scanner if explicitly selected in settings
    DisposableEffect(scannerSettings.selectedScannerId) {
        val selectedId = scannerSettings.selectedScannerId
        if (selectedId.startsWith("BT:")) {
            val mac = selectedId.removePrefix("BT:")
            val client = BluetoothSppScannerClient()
            client.startListening(
                context = context,
                macAddress = mac,
                scope = coroutineScope,
                onBarcodeRead = { code ->
                    processScannedCode(code, fromCameraFrame = false, source = "BLUETOOTH_SPP")
                },
                onStatusChanged = { _, _ -> }
            )
            onDispose { client.stop() }
        } else {
            onDispose { }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanBanner = ScanBannerState.Status("Memindai barcode dari gambar galeri...")
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = createMlKitBarcodeScanner()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val detected = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (!detected.isNullOrBlank()) {
                            processScannedCode(detected, fromCameraFrame = false, source = "PHOTO")
                        } else {
                            scanBanner = ScanBannerState.Status(
                                "Barcode tidak terbaca pada foto. Pastikan gambar jelas.",
                                isError = true
                            )
                        }
                    }
                    .addOnFailureListener {
                        scanBanner = ScanBannerState.Status("Gagal memproses gambar.", isError = true)
                    }
                    .addOnCompleteListener {
                        scanner.close()
                    }
            } catch (_: Exception) {
                scanBanner = ScanBannerState.Status("Foto tidak dapat diakses.", isError = true)
            }
        }
    }

    val cartSubtotal = remember(cart) { cart.sumOf { it.unitPrice * it.quantity } }
    val cartAdminTotal = remember(cart) { cart.sumOf { it.storeAdminFee } }
    val cartTax = remember(cartSubtotal, cartDiscount, cartTaxRate) {
        ((cartSubtotal - cartDiscount) * cartTaxRate).coerceAtLeast(0.0)
    }
    val cartFinalTotal = remember(cartSubtotal, cartAdminTotal, cartDiscount, cartTax) {
        (cartSubtotal + cartAdminTotal - cartDiscount + cartTax).coerceAtLeast(0.0)
    }
    val cartTotalQty = remember(cart) {
        cart.sumOf { ceil(it.quantity).toInt().coerceAtLeast(1) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("barcode_scan_cart_screen")
            .onPreviewKeyEvent { event ->
                val activeHardwareCapture = scannerSettings.autoCaptureHardwareInput &&
                    !isManualInputFocused &&
                    pendingSelection == null &&
                    quickRegisterBarcode == null &&
                    !showScannerSettingsDialog
                if (!activeHardwareCapture) return@onPreviewKeyEvent false

                val native = event.nativeKeyEvent
                val dev = native.device
                val isExternalHardware = dev != null && !dev.isVirtual && native.deviceId > 0
                if (!isExternalHardware) return@onPreviewKeyEvent false

                if (event.type == KeyEventType.KeyDown) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastHardwareKeyAtMs > 1200L) {
                        hardwareKeyBuffer.setLength(0)
                    }
                    lastHardwareKeyAtMs = now
                    val keyCode = native.keyCode
                    if (keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                        keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                        keyCode == android.view.KeyEvent.KEYCODE_TAB
                    ) {
                        val code = hardwareKeyBuffer.toString().trim()
                        hardwareKeyBuffer.setLength(0)
                        if (code.isNotEmpty()) {
                            processScannedCode(code, fromCameraFrame = false, source = "HID_SCANNER")
                        }
                        true
                    } else {
                        val ch = native.unicodeChar
                        if (ch >= 32) {
                            hardwareKeyBuffer.append(ch.toChar())
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        val isTablet = maxWidth >= 720.dp
        val isScannerSuspended = isPaused || pendingSelection != null || quickRegisterBarcode != null || showScannerSettingsDialog

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                CameraScannerPane(
                    hasCameraPermission = hasCameraPermission,
                    isPaused = isScannerSuspended,
                    torchEnabled = torchEnabled,
                    useFrontCamera = useFrontCamera,
                    continuousMode = continuousMode,
                    detectedBoxes = detectedBoxes,
                    scanBanner = scanBanner,
                    onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onTogglePause = { isPaused = !isPaused },
                    onToggleTorch = { torchEnabled = !torchEnabled },
                    onFlipCamera = {
                        useFrontCamera = !useFrontCamera
                        if (useFrontCamera) torchEnabled = false
                    },
                    onToggleContinuous = { continuousMode = !continuousMode },
                    onOpenSettings = { showScannerSettingsDialog = true },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onBackToCashier = { viewModel.navigateTo(Screen.POS_KASIR) },
                    onDetectedBoxesChanged = { detectedBoxes = it },
                    onBarcodeScanned = { code -> processScannedCode(code, fromCameraFrame = true) },
                    onQuickRegisterCode = { code -> quickRegisterBarcode = code },
                    onUndoLastMatched = { matched ->
                        val existing = cart.find {
                            it.productId == matched.productId &&
                                it.unitName.equals(matched.unitName, ignoreCase = true) &&
                                it.variantId == matched.variantId
                        }
                        if (existing != null) {
                            viewModel.updateCartItemQuantity(
                                productId = existing.productId,
                                unitName = existing.unitName,
                                newQty = existing.quantity - 1.0,
                                variantId = existing.variantId,
                                bundleId = existing.bundleId
                            )
                        }
                        scanBanner = null
                    },
                    onDismissBanner = { scanBanner = null },
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(12.dp)
                )

                ScanCartDockPanel(
                    cart = cart,
                    productImages = productImageMap,
                    decimalProductIds = decimalProductIds,
                    detectedScanners = detectedScanners,
                    selectedScannerName = scannerSettings.selectedScannerName,
                    onOpenScannerSettings = { showScannerSettingsDialog = true },
                    onManualFocusChanged = { isManualInputFocused = it },
                    manualBarcode = manualBarcode,
                    onManualBarcodeChange = { manualBarcode = it },
                    onManualSubmit = { code ->
                        processScannedCode(code, fromCameraFrame = false)
                        manualBarcode = ""
                    },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUpdateQty = { item, newQty ->
                        viewModel.updateCartItemQuantity(item.productId, item.unitName, newQty, item.variantId, item.bundleId)
                    },
                    onRemoveItem = { item ->
                        viewModel.removeFromCart(item.productId, item.unitName, item.variantId, item.bundleId)
                    },
                    onClearCart = { viewModel.clearCart() },
                    onManageProducts = { viewModel.navigateTo(Screen.MANAJEMEN_PRODUK) },
                    onGoToCheckout = { viewModel.navigateTo(Screen.POS_KASIR) },
                    cartTotalQty = cartTotalQty,
                    cartFinalTotal = cartFinalTotal,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CameraScannerPane(
                    hasCameraPermission = hasCameraPermission,
                    isPaused = isScannerSuspended,
                    torchEnabled = torchEnabled,
                    useFrontCamera = useFrontCamera,
                    continuousMode = continuousMode,
                    detectedBoxes = detectedBoxes,
                    scanBanner = scanBanner,
                    onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onTogglePause = { isPaused = !isPaused },
                    onToggleTorch = { torchEnabled = !torchEnabled },
                    onFlipCamera = {
                        useFrontCamera = !useFrontCamera
                        if (useFrontCamera) torchEnabled = false
                    },
                    onToggleContinuous = { continuousMode = !continuousMode },
                    onOpenSettings = { showScannerSettingsDialog = true },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onBackToCashier = { viewModel.navigateTo(Screen.POS_KASIR) },
                    onDetectedBoxesChanged = { detectedBoxes = it },
                    onBarcodeScanned = { code -> processScannedCode(code, fromCameraFrame = true) },
                    onQuickRegisterCode = { code -> quickRegisterBarcode = code },
                    onUndoLastMatched = { matched ->
                        val existing = cart.find {
                            it.productId == matched.productId &&
                                it.unitName.equals(matched.unitName, ignoreCase = true) &&
                                it.variantId == matched.variantId
                        }
                        if (existing != null) {
                            viewModel.updateCartItemQuantity(
                                productId = existing.productId,
                                unitName = existing.unitName,
                                newQty = existing.quantity - 1.0,
                                variantId = existing.variantId,
                                bundleId = existing.bundleId
                            )
                        }
                        scanBanner = null
                    },
                    onDismissBanner = { scanBanner = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.46f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                ScanCartDockPanel(
                    cart = cart,
                    productImages = productImageMap,
                    decimalProductIds = decimalProductIds,
                    detectedScanners = detectedScanners,
                    selectedScannerName = scannerSettings.selectedScannerName,
                    onOpenScannerSettings = { showScannerSettingsDialog = true },
                    onManualFocusChanged = { isManualInputFocused = it },
                    manualBarcode = manualBarcode,
                    onManualBarcodeChange = { manualBarcode = it },
                    onManualSubmit = { code ->
                        processScannedCode(code, fromCameraFrame = false)
                        manualBarcode = ""
                    },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUpdateQty = { item, newQty ->
                        viewModel.updateCartItemQuantity(item.productId, item.unitName, newQty, item.variantId, item.bundleId)
                    },
                    onRemoveItem = { item ->
                        viewModel.removeFromCart(item.productId, item.unitName, item.variantId, item.bundleId)
                    },
                    onClearCart = { viewModel.clearCart() },
                    onManageProducts = { viewModel.navigateTo(Screen.MANAJEMEN_PRODUK) },
                    onGoToCheckout = { viewModel.navigateTo(Screen.POS_KASIR) },
                    cartTotalQty = cartTotalQty,
                    cartFinalTotal = cartFinalTotal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.54f)
                )
            }
        }
    }

    if (showScannerSettingsDialog) {
        ScannerSettingsDialog(
            currentSettings = scannerSettings,
            onSaveSettings = { updated -> viewModel.updateScannerSettings(updated) },
            onDismiss = { showScannerSettingsDialog = false }
        )
    }

    pendingSelection?.let { selection ->
        ScannedProductOptionDialog(
            selection = selection,
            flexiblePriceInput = flexiblePriceInput,
            onFlexiblePriceChange = { flexiblePriceInput = it },
            onSelectVariant = { variant ->
                viewModel.addToCart(selection.product, variant = variant)
                scanBanner = ScanBannerState.Matched(
                    code = selection.product.barcode,
                    productName = "${selection.product.name} (${variant.name})",
                    unitPrice = variant.sellingPrice,
                    productId = selection.product.id,
                    unitName = selection.product.baseUnit,
                    variantId = variant.id,
                    timestampMs = System.currentTimeMillis()
                )
                pendingSelection = null
                if (continuousMode) isPaused = false
            },
            onSelectBaseUnit = {
                val customPrice = if (selection.product.isFlexiblePrice) {
                    flexiblePriceInput.toDoubleOrNull() ?: selection.product.sellingPrice
                } else null
                val tierSvc = selection.product.adminTierGroupId?.let { gid ->
                    adminTiers.find { it.service.id == gid }
                }
                val calc = TierPricingResolver.calculate(tierSvc, customPrice ?: selection.product.sellingPrice)
                viewModel.addToCart(
                    product = selection.product,
                    customPrice = customPrice,
                    storeAdminFee = calc.storeAdminFee,
                    providerAdminFee = calc.providerAdminFee,
                    agentCommission = calc.agentCommission
                )
                scanBanner = ScanBannerState.Matched(
                    code = selection.product.barcode,
                    productName = selection.product.name,
                    unitPrice = customPrice ?: selection.product.sellingPrice,
                    productId = selection.product.id,
                    unitName = selection.product.baseUnit,
                    variantId = null,
                    timestampMs = System.currentTimeMillis()
                )
                pendingSelection = null
                if (continuousMode) isPaused = false
            },
            onSelectConversion = { conv ->
                val resolvedPrice = conv.customSellingPrice ?: (selection.product.sellingPrice * conv.multiplier)
                viewModel.addToCart(
                    product = selection.product,
                    unitName = conv.unitName,
                    multiplier = conv.multiplier,
                    price = resolvedPrice
                )
                scanBanner = ScanBannerState.Matched(
                    code = selection.product.barcode,
                    productName = "${selection.product.name} (${conv.unitName})",
                    unitPrice = resolvedPrice,
                    productId = selection.product.id,
                    unitName = conv.unitName,
                    variantId = null,
                    timestampMs = System.currentTimeMillis()
                )
                pendingSelection = null
                if (continuousMode) isPaused = false
            },
            onDismiss = {
                pendingSelection = null
                if (continuousMode) isPaused = false
            }
        )
    }

    quickRegisterBarcode?.let { barcode ->
        QuickRegisterScannedProductDialog(
            barcode = barcode,
            onDismiss = {
                quickRegisterBarcode = null
                if (continuousMode) isPaused = false
            },
            onSaveAndAdd = { name, sellingPrice, costPrice, stock ->
                viewModel.quickCreateAndAddScannedProduct(
                    name = name,
                    barcode = barcode,
                    sellingPrice = sellingPrice,
                    costPrice = costPrice,
                    stock = stock
                )
                scanBanner = ScanBannerState.Matched(
                    code = barcode,
                    productName = name,
                    unitPrice = sellingPrice,
                    productId = -1L,
                    unitName = "pcs",
                    variantId = null,
                    timestampMs = System.currentTimeMillis()
                )
                quickRegisterBarcode = null
                if (continuousMode) isPaused = false
            }
        )
    }
}

private fun createMlKitBarcodeScanner() = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_PDF417
        )
        .build()
)

@Composable
private fun CameraScannerPane(
    hasCameraPermission: Boolean,
    isPaused: Boolean,
    torchEnabled: Boolean,
    useFrontCamera: Boolean,
    continuousMode: Boolean,
    detectedBoxes: List<DetectedBoxUi>,
    scanBanner: ScanBannerState?,
    onRequestPermission: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleContinuous: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickPhoto: () -> Unit,
    onBackToCashier: () -> Unit,
    onDetectedBoxesChanged: (List<DetectedBoxUi>) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onQuickRegisterCode: (String) -> Unit,
    onUndoLastMatched: (ScanBannerState.Matched) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF090D16),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 4.dp,
        modifier = modifier.clip(RoundedCornerShape(18.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasCameraPermission) {
                CameraPermissionPlaceholder(
                    onRequestPermission = onRequestPermission,
                    onPickPhoto = onPickPhoto,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CameraXBarcodeLivePreview(
                    isPaused = isPaused,
                    torchEnabled = torchEnabled,
                    useFrontCamera = useFrontCamera,
                    onDetectedBoxesChanged = onDetectedBoxesChanged,
                    onBarcodeDetected = onBarcodeScanned,
                    modifier = Modifier.fillMaxSize()
                )

                ScannerReticleAndBoxesOverlay(
                    isPaused = isPaused,
                    detectedBoxes = detectedBoxes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top translucent control bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBackToCashier,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scanner_back_cashier_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Kasir",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleContinuous,
                        shape = RoundedCornerShape(20.dp),
                        color = if (continuousMode) ModuleKasir.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("scanner_mode_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (continuousMode) R.string.scanner_mode_continuous else R.string.scanner_mode_single
                                ),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCameraPermission) {
                        Surface(
                            onClick = onTogglePause,
                            shape = CircleShape,
                            color = if (isPaused) Amber500 else Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = stringResource(
                                        if (isPaused) R.string.scanner_resume_label else R.string.scanner_pause_label
                                    ),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (!useFrontCamera) {
                            Surface(
                                onClick = onToggleTorch,
                                shape = CircleShape,
                                color = if (torchEnabled) Amber400 else Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("scanner_torch_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = if (torchEnabled) "Matikan Flash" else "Nyalakan Flash",
                                        tint = if (torchEnabled) Slate950 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onFlipCamera,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_flip_camera_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Ganti Kamera",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scanner_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan Scan Barcode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Paused Center Pill
            if (hasCameraPermission && isPaused) {
                Surface(
                    onClick = onTogglePause,
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Amber400),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Amber400)
                        Text(
                            "${stringResource(R.string.scanner_paused_label)} • Ketuk Lanjut",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom scan result feedback overlay
            AnimatedVisibility(
                visible = scanBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                scanBanner?.let { banner ->
                    ScanResultFeedbackBanner(
                        banner = banner,
                        onUndoLastMatched = onUndoLastMatched,
                        onQuickRegisterCode = onQuickRegisterCode,
                        onDismiss = onDismissBanner
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPlaceholder(
    onRequestPermission: () -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.scanner_permission_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.scanner_permission_desc),
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.78f)),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("scanner_permission_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.scanner_grant_permission), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPickPhoto,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("scanner_photo_fallback_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.scanner_choose_photo), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraXBarcodeLivePreview(
    isPaused: Boolean,
    torchEnabled: Boolean,
    useFrontCamera: Boolean,
    onDetectedBoxesChanged: (List<DetectedBoxUi>) -> Unit,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { createMlKitBarcodeScanner() }

    val latestPaused by rememberUpdatedState(isPaused)
    val latestOnBoxes by rememberUpdatedState(onDetectedBoxesChanged)
    val latestOnBarcode by rememberUpdatedState(onBarcodeDetected)

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(boundCamera, torchEnabled, useFrontCamera) {
        runCatching {
            boundCamera?.cameraControl?.enableTorch(torchEnabled && !useFrontCamera)
        }
    }

    LaunchedEffect(previewViewRef, useFrontCamera, isPaused, lifecycleOwner) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProviderRef = provider
                provider.unbindAll()

                if (isPaused) {
                    boundCamera = null
                    latestOnBoxes(emptyList())
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    if (latestPaused) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val rotatedWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                        val rotatedHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val boxes = barcodes.mapNotNull { barcode ->
                                    val box = barcode.boundingBox ?: return@mapNotNull null
                                    if (rotatedWidth <= 0 || rotatedHeight <= 0) return@mapNotNull null
                                    val left = (box.left.toFloat() / rotatedWidth).coerceIn(0f, 1f)
                                    val right = (box.right.toFloat() / rotatedWidth).coerceIn(0f, 1f)
                                    val top = (box.top.toFloat() / rotatedHeight).coerceIn(0f, 1f)
                                    val bottom = (box.bottom.toFloat() / rotatedHeight).coerceIn(0f, 1f)
                                    val norm = if (useFrontCamera) {
                                        RectF(1f - right, top, 1f - left, bottom)
                                    } else {
                                        RectF(left, top, right, bottom)
                                    }
                                    DetectedBoxUi(norm, barcode.rawValue.orEmpty())
                                }
                                latestOnBoxes(boxes)
                                val firstValue = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                                if (!firstValue.isNullOrEmpty()) {
                                    latestOnBarcode(firstValue)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val selector = if (useFrontCamera && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraError = null
                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                cameraError = "Kamera tidak tersedia: ${e.localizedMessage ?: "Periksa izin atau perangkat"}"
            }
        }, mainExecutor)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderRef?.unbindAll() }
            runCatching { analyzerExecutor.shutdown() }
            runCatching { barcodeScanner.close() }
        }
    }

    Box(modifier = modifier) {
        if (cameraError != null) {
            Text(
                text = cameraError!!,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ScannerReticleAndBoxesOverlay(
    isPaused: Boolean,
    detectedBoxes: List<DetectedBoxUi>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Canvas(modifier = modifier) {
        val frameWidth = (size.width * 0.68f).coerceIn(180.dp.toPx(), 320.dp.toPx())
        val frameHeight = (size.height * 0.48f).coerceIn(110.dp.toPx(), 190.dp.toPx())
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val right = left + frameWidth
        val bottom = top + frameHeight

        // Dim outer area with cutout path
        val overlayPath = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
            )
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        drawPath(path = overlayPath, color = Color.Black.copy(alpha = 0.42f))

        // Corner brackets
        val bracketColor = if (detectedBoxes.isNotEmpty()) Emerald500 else Color.White.copy(alpha = 0.92f)
        val bracketLen = 22.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

        // Top-left
        drawPath(
            Path().apply {
                moveTo(left, top + bracketLen)
                lineTo(left, top)
                lineTo(left + bracketLen, top)
            },
            color = bracketColor,
            style = stroke
        )
        // Top-right
        drawPath(
            Path().apply {
                moveTo(right - bracketLen, top)
                lineTo(right, top)
                lineTo(right, top + bracketLen)
            },
            color = bracketColor,
            style = stroke
        )
        // Bottom-left
        drawPath(
            Path().apply {
                moveTo(left, bottom - bracketLen)
                lineTo(left, bottom)
                lineTo(left + bracketLen, bottom)
            },
            color = bracketColor,
            style = stroke
        )
        // Bottom-right
        drawPath(
            Path().apply {
                moveTo(right - bracketLen, bottom)
                lineTo(right, bottom)
                lineTo(right, bottom - bracketLen)
            },
            color = bracketColor,
            style = stroke
        )

        // Animated laser line while actively scanning
        if (!isPaused) {
            val lineY = top + frameHeight * laserProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Emerald500, Color(0xFF6EE7B7), Emerald500, Color.Transparent),
                    startX = left + 10.dp.toPx(),
                    endX = right - 10.dp.toPx()
                ),
                start = Offset(left + 10.dp.toPx(), lineY),
                end = Offset(right - 10.dp.toPx(), lineY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Detected ML Kit bounding boxes
        detectedBoxes.forEach { boxUi ->
            val r = boxUi.normalizedRect
            val bx = r.left * size.width
            val by = r.top * size.height
            val bw = ((r.right - r.left) * size.width).coerceAtLeast(18f)
            val bh = ((r.bottom - r.top) * size.height).coerceAtLeast(18f)
            drawRoundRect(
                color = Emerald500.copy(alpha = 0.18f),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            drawRoundRect(
                color = Emerald500,
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun ScanResultFeedbackBanner(
    banner: ScanBannerState,
    onUndoLastMatched: (ScanBannerState.Matched) -> Unit,
    onQuickRegisterCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    when (banner) {
        is ScanBannerState.Matched -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Emerald800.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Emerald200.copy(alpha = 0.6f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_matched_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "+1 ${banner.productName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Rp ${formatRupiah(banner.unitPrice)} • Kode: ${banner.code}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        onClick = { onUndoLastMatched(banner) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            "Batal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        is ScanBannerState.Unrecognized -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF78350F).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Amber400),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_unknown_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber400, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.scanner_unknown_barcode),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            banner.code,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        onClick = { onQuickRegisterCode(banner.code) },
                        shape = RoundedCornerShape(8.dp),
                        color = Amber400,
                        modifier = Modifier.testTag("scanner_quick_add_button")
                    ) {
                        Text(
                            "Tambah",
                            color = Slate950,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        is ScanBannerState.Status -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (banner.isError) Rose500.copy(alpha = 0.94f) else Slate950.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        banner.message,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanCartDockPanel(
    cart: List<CartItem>,
    productImages: Map<Long, String>,
    decimalProductIds: Set<Long>,
    detectedScanners: List<DetectedScannerDevice>,
    selectedScannerName: String,
    onOpenScannerSettings: () -> Unit,
    onManualFocusChanged: (Boolean) -> Unit,
    manualBarcode: String,
    onManualBarcodeChange: (String) -> Unit,
    onManualSubmit: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onUpdateQty: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onManageProducts: () -> Unit,
    onGoToCheckout: () -> Unit,
    cartTotalQty: Int,
    cartFinalTotal: Double,
    modifier: Modifier = Modifier
) {
    val activeHardware = detectedScanners.firstOrNull { it.isConnectedNow } ?: detectedScanners.firstOrNull()
    val bannerTitle = when {
        selectedScannerName.isNotBlank() -> "Alat Aktif: $selectedScannerName"
        activeHardware != null -> "${activeHardware.transport}: ${activeHardware.name}"
        else -> "Pengaturan & Deteksi Scanner USB / Bluetooth"
    }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = SurfaceWhite,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Connected Hardware Scanner Status Bar + Settings shortcut
            Surface(
                onClick = onOpenScannerSettings,
                color = if (activeHardware?.isConnectedNow == true || selectedScannerName.isNotBlank()) Emerald100 else Sapphire50,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_hardware_status_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (activeHardware?.transport?.contains("USB", true) == true) Icons.Default.Usb else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (activeHardware?.isConnectedNow == true || selectedScannerName.isNotBlank()) Emerald700 else AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bannerTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeHardware?.isConnectedNow == true || selectedScannerName.isNotBlank()) Emerald800 else AccentBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atur", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                }
            }

            // Manual barcode & gallery photo bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { raw ->
                        if (raw.contains('\n') || raw.contains('\r')) {
                            val cleaned = raw.replace("\r", "").replace("\n", "").trim()
                            if (cleaned.isNotEmpty()) onManualSubmit(cleaned) else onManualBarcodeChange("")
                        } else {
                            onManualBarcodeChange(raw)
                        }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.scanner_manual_placeholder),
                            fontSize = 12.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (manualBarcode.isNotBlank()) onManualSubmit(manualBarcode)
                        }
                    ),
                    trailingIcon = {
                        if (manualBarcode.isNotBlank()) {
                            IconButton(
                                onClick = { onManualSubmit(manualBarcode) },
                                modifier = Modifier.testTag("scanner_manual_submit_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah Barcode", tint = AccentBlue)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgCanvas,
                        unfocusedContainerColor = BgCanvas,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderInput,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onManualFocusChanged(it.isFocused) }
                        .testTag("scanner_manual_input")
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp &&
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) &&
                                manualBarcode.isNotBlank()
                            ) {
                                onManualSubmit(manualBarcode)
                                true
                            } else false
                        }
                )

                Surface(
                    onClick = onPickPhoto,
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceStrong,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("scanner_pick_photo_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.scanner_choose_photo),
                            tint = AccentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderDefault)

            // Cart header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ModuleKasir, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.scanner_cart_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    if (cartTotalQty > 0) {
                        DesignStatusBadge(text = "$cartTotalQty pcs", type = BadgeType.SUCCESS)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onManageProducts, modifier = Modifier.testTag("scanner_manage_products_button")) {
                        Text(
                            "Kelola Produk",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (cart.isNotEmpty()) {
                        TextButton(onClick = onClearCart, modifier = Modifier.testTag("scanner_clear_cart_button")) {
                            Text(
                                stringResource(R.string.scanner_clear_cart),
                                color = Rose500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Cart items list
            Box(modifier = Modifier.weight(1f)) {
                if (cart.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.scanner_cart_empty_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate700),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.scanner_cart_empty_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("scanner_cart_list")
                    ) {
                        items(
                            items = cart,
                            key = { "${it.productId}_${it.unitName}_${it.variantId ?: 0}_${it.bundleId ?: 0}" }
                        ) { item ->
                            ScannedCartItemCard(
                                item = item,
                                imageUri = productImages[item.productId].orEmpty(),
                                allowDecimal = decimalProductIds.contains(item.productId),
                                onDecrement = {
                                    val step = if (decimalProductIds.contains(item.productId) && item.quantity <= 1.0) 0.5 else 1.0
                                    onUpdateQty(item, item.quantity - step)
                                },
                                onIncrement = { onUpdateQty(item, item.quantity + 1.0) },
                                onRemove = { onRemoveItem(item) }
                            )
                        }
                    }
                }
            }

            // Sticky checkout summary footer
            Surface(
                color = if (cart.isNotEmpty()) Sapphire900 else SurfaceStrong,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgedBox(
                                badge = {
                                    if (cartTotalQty > 0) {
                                        Badge(containerColor = ModuleKasir, contentColor = Color.White) {
                                            Text("$cartTotalQty", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = if (cart.isNotEmpty()) Color.White else Slate500
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Rp ${formatRupiah(cartFinalTotal)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (cart.isNotEmpty()) Color.White else Slate700
                                ).tabular()
                            )
                        }
                        Text(
                            if (cart.isEmpty()) "0 item dipilih" else "${cart.size} jenis item siap dibayar",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (cart.isNotEmpty()) Color.White.copy(alpha = 0.78f) else Slate500
                            )
                        )
                    }

                    Button(
                        onClick = onGoToCheckout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cart.isNotEmpty()) ModuleKasir else AccentBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("scanner_checkout_button")
                    ) {
                        Text(stringResource(R.string.scanner_checkout_cta), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedCartItemCard(
    item: CartItem,
    imageUri: String,
    allowDecimal: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit
) {
    val lineTotal = (item.unitPrice * item.quantity) + item.storeAdminFee
    val qtyText = if (item.quantity % 1.0 == 0.0) {
        item.quantity.toInt().toString()
    } else {
        String.format(java.util.Locale("id", "ID"), "%.2f", item.quantity)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCanvas),
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProductThumbnail(
                imageUri = imageUri,
                contentDescription = item.productName,
                isDigital = item.serviceNominal != null,
                size = 42.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.variantName.isNotBlank()) "${item.productName} (${item.variantName})" else item.productName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Rp ${formatRupiah(item.unitPrice)} / ${item.unitName}${if (allowDecimal) " • timbang" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
                Text(
                    text = "Subtotal: Rp ${formatRupiah(lineTotal)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ModuleKasir).tabular()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onDecrement,
                    shape = CircleShape,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi", tint = Slate700, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900).tabular(),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    onClick = onIncrement,
                    shape = CircleShape,
                    color = AccentBlue,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Item", tint = Rose500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ScannedProductOptionDialog(
    selection: BarcodeLookupOutcome.NeedsSelection,
    flexiblePriceInput: String,
    onFlexiblePriceChange: (String) -> Unit,
    onSelectVariant: (ProductVariantEntity) -> Unit,
    onSelectBaseUnit: () -> Unit,
    onSelectConversion: (UnitConversionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pilih Opsi: ${selection.product.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )

                if (selection.variants.isNotEmpty()) {
                    Text("Pilih Varian Produk", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Bold))
                    selection.variants.forEach { variant ->
                        Surface(
                            onClick = { onSelectVariant(variant) },
                            shape = RoundedCornerShape(10.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(variant.name, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text("Stok: ${variant.stock.toInt()}", fontSize = 11.sp, color = Slate500)
                                }
                                Text("Rp ${formatRupiah(variant.sellingPrice)}", fontWeight = FontWeight.Bold, color = AccentBlue)
                            }
                        }
                    }
                } else if (selection.product.isFlexiblePrice) {
                    DesignLabeledTextField(
                        label = "Nominal / Harga Jual (Rp)",
                        value = flexiblePriceInput,
                        onValueChange = { onFlexiblePriceChange(it.filter(Char::isDigit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = onSelectBaseUnit,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambahkan ke Keranjang", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Pilih Satuan Jual", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Bold))
                    Surface(
                        onClick = onSelectBaseUnit,
                        shape = RoundedCornerShape(10.dp),
                        color = BgCanvas,
                        border = BorderStroke(1.dp, AccentBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 ${selection.product.baseUnit} (Dasar)", fontWeight = FontWeight.Bold, color = Slate900)
                            Text("Rp ${formatRupiah(selection.product.sellingPrice)}", fontWeight = FontWeight.Bold, color = AccentBlue)
                        }
                    }
                    selection.conversions.forEach { conv ->
                        val convPrice = conv.customSellingPrice ?: (selection.product.sellingPrice * conv.multiplier)
                        Surface(
                            onClick = { onSelectConversion(conv) },
                            shape = RoundedCornerShape(10.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1 ${conv.unitName} (${conv.multiplier.toInt()} ${selection.product.baseUnit})", fontWeight = FontWeight.SemiBold, color = Slate900)
                                Text("Rp ${formatRupiah(convPrice)}", fontWeight = FontWeight.Bold, color = AccentBlue)
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Tutup", color = Slate500)
                }
            }
        }
    }
}

@Composable
private fun QuickRegisterScannedProductDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onSaveAndAdd: (name: String, sellingPrice: Double, costPrice: Double, stock: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.scanner_quick_add_product),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BgCanvas,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Barcode: $barcode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                DesignLabeledTextField(
                    label = "Nama Produk",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Contoh: Teh Pucuk 350 ml"
                )
                DesignLabeledTextField(
                    label = "Harga Jual (Rp)",
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it.filter(Char::isDigit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = "4000"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DesignLabeledTextField(
                            label = "Harga Modal (Rp)",
                            value = costPrice,
                            onValueChange = { costPrice = it.filter(Char::isDigit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "3000"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DesignLabeledTextField(
                            label = "Stok Awal",
                            value = stock,
                            onValueChange = { stock = it.filter(Char::isDigit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "10"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Slate600)
                    }
                    val parsedSell = sellingPrice.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            onSaveAndAdd(
                                name.trim(),
                                parsedSell,
                                costPrice.toDoubleOrNull() ?: 0.0,
                                stock.toDoubleOrNull() ?: 10.0
                            )
                        },
                        enabled = name.isNotBlank() && parsedSell > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = ModuleKasir, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Simpan & Masuk Keranjang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
