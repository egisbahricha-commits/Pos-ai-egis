package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.entity.SaleEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.repository.CartItem
import com.example.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

fun copyUriToInternalProductImage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "product_images").apply {
            if (!exists()) mkdirs()
        }
        val destFile = File(dir, "prod_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ProductThumbnail(
    imageUri: String,
    contentDescription: String,
    isDigital: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    iconSize: Dp = 22.dp
) {
    val hasValidPhoto = imageUri.isNotBlank() && (
        !imageUri.startsWith("/") || File(imageUri).exists()
    )
    val modelData: Any? = remember(imageUri) {
        when {
            imageUri.isBlank() -> null
            imageUri.startsWith("/") -> File(imageUri)
            else -> Uri.parse(imageUri)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (isDigital) Sapphire50 else SurfaceStrong),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidPhoto && modelData != null) {
            AsyncImage(
                model = modelData,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (isDigital) Icons.Default.PhoneAndroid else Icons.Default.Inventory2,
                contentDescription = contentDescription,
                tint = if (isDigital) AccentBlue else Slate500,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun BarcodeScannerDialog(
    title: String = "Scan Barcode Produk",
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("KAMERA") } // KAMERA, FOTO, SCANNER
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoStatusMessage by remember { mutableStateOf<String?>(null) }
    var candidateBarcodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualOrHidCode by remember { mutableStateOf("") }
    var alreadyEmitted by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingPhoto = true
            photoStatusMessage = "Memindai barcode dari foto..."
            candidateBarcodes = emptyList()
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        isProcessingPhoto = false
                        val values = barcodes.mapNotNull { it.rawValue?.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                        when {
                            values.isEmpty() -> {
                                photoStatusMessage = "Barcode tidak ditemukan pada foto. Pastikan foto tajam dan tidak blur."
                            }
                            values.size == 1 -> {
                                if (!alreadyEmitted) {
                                    alreadyEmitted = true
                                    onBarcodeDetected(values.first())
                                }
                            }
                            else -> {
                                candidateBarcodes = values
                                photoStatusMessage = "Ditemukan ${values.size} barcode. Pilih salah satu:"
                            }
                        }
                    }
                    .addOnFailureListener {
                        isProcessingPhoto = false
                        photoStatusMessage = "Gagal membaca gambar. Coba pilih foto lain."
                    }
            } catch (_: Exception) {
                isProcessingPhoto = false
                photoStatusMessage = "Foto tidak dapat diakses."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                DesignSegmentedTabs(
                    options = listOf(
                        "KAMERA" to "Kamera",
                        "FOTO" to "Dari Foto",
                        "SCANNER" to "Alat Scanner"
                    ),
                    selectedKey = activeTab,
                    onSelect = { activeTab = it }
                )

                when (activeTab) {
                    "KAMERA" -> {
                        if (!hasCameraPermission) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BgCanvas,
                                border = BorderStroke(1.dp, BorderDefault),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(36.dp))
                                    Text(
                                        "Izin Kamera Diperlukan",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Text(
                                        "Aktifkan kamera untuk scan barcode langsung, atau gunakan tab Dari Foto / Alat Scanner.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Izinkan Kamera", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            CameraBarcodePreviewBox(
                                torchEnabled = torchEnabled,
                                onToggleTorch = { torchEnabled = !torchEnabled },
                                onBarcodeDetected = { code ->
                                    if (!alreadyEmitted) {
                                        alreadyEmitted = true
                                        onBarcodeDetected(code)
                                    }
                                }
                            )
                            Text(
                                "Arahkan kamera ke barcode atau QR produk",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    "FOTO" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(36.dp))
                                Text(
                                    "Deteksi Barcode dari Foto",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    "Pilih foto kemasan barang, label harga, atau struk untuk membaca kode barcode secara otomatis.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                    textAlign = TextAlign.Center
                                )
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(26.dp), color = AccentBlue)
                                } else {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("scan_from_photo_pick_btn")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pilih Foto Barcode", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (photoStatusMessage != null) {
                                    Text(
                                        text = photoStatusMessage!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (candidateBarcodes.isNotEmpty()) Emerald700 else Amber500,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                candidateBarcodes.forEach { code ->
                                    Surface(
                                        onClick = {
                                            if (!alreadyEmitted) {
                                                alreadyEmitted = true
                                                onBarcodeDetected(code)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = SurfaceWhite,
                                        border = BorderStroke(1.dp, AccentBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(code, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text("Gunakan", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "SCANNER" -> {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(activeTab) {
                            if (activeTab == "SCANNER") {
                                focusRequester.requestFocus()
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mode Alat Scanner USB / Bluetooth",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                }
                                Text(
                                    "Tembak barcode menggunakan alat scanner fisik USB/Bluetooth (HID), atau ketik kode lalu tekan Enter.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                )
                                OutlinedTextField(
                                    value = manualOrHidCode,
                                    onValueChange = { raw ->
                                        if (raw.contains('\n') || raw.contains('\r')) {
                                            val cleaned = raw.replace("\r", "").replace("\n", "").trim()
                                            if (cleaned.isNotEmpty() && !alreadyEmitted) {
                                                alreadyEmitted = true
                                                onBarcodeDetected(cleaned)
                                            }
                                        } else {
                                            manualOrHidCode = raw
                                        }
                                    },
                                    placeholder = { Text("Menunggu input alat scanner / ketik SKU...") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val cleaned = manualOrHidCode.trim()
                                            if (cleaned.isNotEmpty() && !alreadyEmitted) {
                                                alreadyEmitted = true
                                                onBarcodeDetected(cleaned)
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = BorderInput,
                                        focusedTextColor = Slate900,
                                        unfocusedTextColor = Slate900
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter) {
                                                val cleaned = manualOrHidCode.trim()
                                                if (cleaned.isNotEmpty() && !alreadyEmitted) {
                                                    alreadyEmitted = true
                                                    onBarcodeDetected(cleaned)
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                )
                                Button(
                                    onClick = {
                                        val cleaned = manualOrHidCode.trim()
                                        if (cleaned.isNotEmpty() && !alreadyEmitted) {
                                            alreadyEmitted = true
                                            onBarcodeDetected(cleaned)
                                        }
                                    },
                                    enabled = manualOrHidCode.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Gunakan Kode Barcode", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodePreviewBox(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(boundCamera, torchEnabled) {
        boundCamera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
    ) {
        if (cameraError != null) {
            Text(
                text = cameraError!!,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            val detected = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                                            if (!detected.isNullOrEmpty()) {
                                                onBarcodeDetected(detected)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            cameraError = "Kamera tidak tersedia: ${e.localizedMessage ?: "Periksa perangkat"}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Viewfinder frame overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 210.dp, height = 110.dp)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
        )

        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Lampu Kilat",
                tint = Color.White
            )
        }
    }
}

object ReceiptPrintHelper {
    fun printReceipt(
        context: Context,
        sale: SaleEntity,
        items: List<CartItem>,
        storeProfile: StoreProfileEntity?
    ): Boolean {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
            val storeName = storeProfile?.name?.ifBlank { "GERAI UNA SMART POS" } ?: "GERAI UNA SMART POS"
            val storeAddress = storeProfile?.address.orEmpty()
            val storePhone = storeProfile?.phone.orEmpty()
            val footer = storeProfile?.receiptFooter?.ifBlank { "Terima kasih telah berbelanja!" }
                ?: "Terima kasih telah berbelanja!"
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(sale.date))

            val itemsHtml = items.joinToString("") { item ->
                val lineTotal = item.unitPrice * item.quantity + item.storeAdminFee
                val adminLine = if (item.storeAdminFee > 0.0) {
                    "<div class='sub'>+ Admin Layanan: Rp ${formatRupiah(item.storeAdminFee)}</div>"
                } else ""
                """
                <div class='item-row'>
                  <div><b>${escapeHtml(item.productName)}</b></div>
                  <div class='flex'>
                    <span>${item.quantity.toInt()} ${escapeHtml(item.unitName)} x Rp ${formatRupiah(item.unitPrice)}</span>
                    <span>Rp ${formatRupiah(lineTotal)}</span>
                  </div>
                  $adminLine
                </div>
                """.trimIndent()
            }

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <style>
                    body { font-family: monospace; font-size: 12px; width: 280px; margin: 0 auto; color: #111; }
                    h2 { text-align: center; margin: 4px 0; font-size: 15px; }
                    .center { text-align: center; font-size: 11px; color: #444; }
                    .hr { border-top: 1px dashed #666; margin: 8px 0; }
                    .flex { display: flex; justify-content: space-between; margin: 2px 0; }
                    .item-row { margin-bottom: 6px; }
                    .sub { font-size: 10px; color: #444; }
                    .total { font-size: 14px; font-weight: bold; }
                  </style>
                </head>
                <body>
                  <h2>${escapeHtml(storeName)}</h2>
                  ${if (storeAddress.isNotBlank()) "<div class='center'>${escapeHtml(storeAddress)}</div>" else ""}
                  ${if (storePhone.isNotBlank()) "<div class='center'>Telp: ${escapeHtml(storePhone)}</div>" else ""}
                  <div class='hr'></div>
                  <div class='flex'><span>Faktur</span><span>${escapeHtml(sale.invoiceNumber)}</span></div>
                  <div class='flex'><span>Waktu</span><span>$dateStr</span></div>
                  <div class='flex'><span>Kasir</span><span>${escapeHtml(sale.cashierName)}</span></div>
                  <div class='flex'><span>Pelanggan</span><span>${escapeHtml(sale.customerName)}</span></div>
                  <div class='hr'></div>
                  $itemsHtml
                  <div class='hr'></div>
                  <div class='flex'><span>Subtotal</span><span>Rp ${formatRupiah(sale.subtotal)}</span></div>
                  ${if (sale.storeAdminFee > 0) "<div class='flex'><span>Admin Layanan</span><span>+Rp ${formatRupiah(sale.storeAdminFee)}</span></div>" else ""}
                  ${if (sale.discount + sale.promoDiscount > 0) "<div class='flex'><span>Diskon</span><span>-Rp ${formatRupiah(sale.discount + sale.promoDiscount)}</span></div>" else ""}
                  ${if (sale.tax > 0) "<div class='flex'><span>Pajak</span><span>+Rp ${formatRupiah(sale.tax)}</span></div>" else ""}
                  <div class='flex total'><span>TOTAL</span><span>Rp ${formatRupiah(sale.finalAmount)}</span></div>
                  <div class='flex'><span>Metode</span><span>${escapeHtml(sale.paymentMethod)}</span></div>
                  ${if (sale.paymentMethod == "TUNAI" || sale.paymentMethod == "SPLIT") """
                    <div class='flex'><span>Bayar</span><span>Rp ${formatRupiah(sale.amountPaid)}</span></div>
                    <div class='flex'><span>Kembali</span><span>Rp ${formatRupiah(sale.changeAmount)}</span></div>
                  """.trimIndent() else ""}
                  <div class='hr'></div>
                  <div class='center'>${escapeHtml(footer)}</div>
                </body>
                </html>
            """.trimIndent()

            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter("Struk-${sale.invoiceNumber}")
                    printManager.print(
                        "Struk ${sale.invoiceNumber}",
                        printAdapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun escapeHtml(raw: String): String {
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
