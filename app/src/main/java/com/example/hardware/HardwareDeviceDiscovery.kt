package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class DetectedScannerDevice(
    val id: String,
    val name: String,
    val transport: String, // "USB HID", "Bluetooth HID", "USB OTG", "Bluetooth SPP"
    val detail: String,
    val isConnectedNow: Boolean,
    val isLikelyScanner: Boolean,
    val usbDeviceName: String? = null,
    val hasUsbPermission: Boolean = true,
    val bluetoothAddress: String? = null
)

data class DetectedPrinterDevice(
    val id: String,
    val name: String,
    val address: String, // MAC address or "USB:<vid>:<pid>:<usbName>"
    val transport: String, // "Bluetooth", "USB OTG"
    val isConnectedNow: Boolean,
    val isPaired: Boolean,
    val isLikelyPrinter: Boolean,
    val usbDeviceName: String? = null,
    val hasUsbPermission: Boolean = true
)

data class HardwareDiscoverySnapshot(
    val scanners: List<DetectedScannerDevice> = emptyList(),
    val printers: List<DetectedPrinterDevice> = emptyList(),
    val hasBluetoothPermission: Boolean = true,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false
)

object HardwareDeviceDiscovery {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val ACTION_USB_PERMISSION = "com.example.kaslite.USB_PERMISSION"

    private val PRINTER_NAME_KEYWORDS = listOf(
        "print", "pos", "thermal", "rpp", "mpt", "zj", "zjiang", "eppos",
        "iware", "panda", "rongta", "xprinter", "xp-", "blueprint", "bp-",
        "sunmi", "kassen", "vsc", "gprinter", "tspl", "tsc", "58", "80", "pt-"
    )

    private val SCANNER_NAME_KEYWORDS = listOf(
        "scan", "barcode", "qr", "hid", "honeywell", "zebra", "symbol", "urovo",
        "newland", "datalogic", "iware", "eppos", "codesoft", "kassen", "2d", "1d",
        "eyoyo", "netum", "tera", "inateck", "minjcode", "symcode"
    )

    private val IGNORED_INTERNAL_INPUT_KEYWORDS = listOf(
        "gpio", "qpnp", "pwrkey", "pmic", "virtual", "touchscreen", "fingerprint",
        "headset", "uinput", "fpc", "goodix", "synaptics", "atmel", "hall"
    )

    fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            emptyArray()
        }
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasBluetoothScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    fun openBluetoothSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun inspectHardware(context: Context): HardwareDiscoverySnapshot {
        val adapter = getBluetoothAdapter(context)
        val btSupported = adapter != null
        val btPermission = hasBluetoothConnectPermission(context)
        val btEnabled = if (btSupported && btPermission) {
            runCatching { adapter?.isEnabled == true }.getOrDefault(false)
        } else {
            false
        }
        return HardwareDiscoverySnapshot(
            scanners = detectConnectedScanners(context),
            printers = detectConnectedPrinters(context),
            hasBluetoothPermission = btPermission,
            isBluetoothSupported = btSupported,
            isBluetoothEnabled = btEnabled
        )
    }

    @SuppressLint("MissingPermission")
    fun detectConnectedScanners(context: Context): List<DetectedScannerDevice> {
        val results = mutableListOf<DetectedScannerDevice>()
        val seenIds = mutableSetOf<String>()

        // 1. External HID Keyboard / Scanner devices via InputManager
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        if (inputManager != null) {
            runCatching {
                inputManager.inputDeviceIds.forEach { deviceId ->
                    val dev = inputManager.getInputDevice(deviceId) ?: return@forEach
                    if (isExternalKeyboardOrScanner(dev)) {
                        val name = dev.name.orEmpty().ifBlank { "Scanner HID #${dev.id}" }
                        val lower = name.lowercase()
                        val isLikely = SCANNER_NAME_KEYWORDS.any { lower.contains(it) }
                        val isBtHid = lower.contains("bluetooth") || lower.contains("bt") ||
                            (dev.vendorId == 0 && dev.productId == 0)
                        val transport = if (isBtHid) "Bluetooth HID" else "USB HID"
                        val id = "HID:${dev.descriptor}"
                        if (seenIds.add(id)) {
                            results += DetectedScannerDevice(
                                id = id,
                                name = name,
                                transport = transport,
                                detail = "Aktif di Android (${if (dev.vendorId != 0) "VID:${hex4(dev.vendorId)} PID:${hex4(dev.productId)}" else "HID"})",
                                isConnectedNow = true,
                                isLikelyScanner = isLikely
                            )
                        }
                    }
                }
            }
        }

        // 2. USB OTG devices (HID / Serial / Barcode scanners)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager != null) {
            runCatching {
                usbManager.deviceList.values.forEach { usb ->
                    val label = usbDisplayName(usb)
                    val lower = label.lowercase()
                    val isPrinterOnly = hasUsbClass(usb, UsbConstants.USB_CLASS_PRINTER) &&
                        !hasUsbClass(usb, UsbConstants.USB_CLASS_HID)
                    if (!isPrinterOnly) {
                        val hasHidOrSerial = hasUsbClass(usb, UsbConstants.USB_CLASS_HID) ||
                            hasUsbClass(usb, UsbConstants.USB_CLASS_COMM) ||
                            hasUsbClass(usb, UsbConstants.USB_CLASS_CDC_DATA)
                        val isLikely = SCANNER_NAME_KEYWORDS.any { lower.contains(it) } || hasUsbClass(usb, UsbConstants.USB_CLASS_HID)
                        if (hasHidOrSerial || isLikely) {
                            val id = "USB:${hex4(usb.vendorId)}:${hex4(usb.productId)}"
                            if (seenIds.add(id)) {
                                val permitted = runCatching { usbManager.hasPermission(usb) }.getOrDefault(false)
                                results += DetectedScannerDevice(
                                    id = id,
                                    name = label,
                                    transport = "USB OTG",
                                    detail = "VID:${hex4(usb.vendorId)} PID:${hex4(usb.productId)}" +
                                        if (permitted) " • Izin USB OK" else " • Perlu izin USB",
                                    isConnectedNow = true,
                                    isLikelyScanner = isLikely,
                                    usbDeviceName = usb.deviceName,
                                    hasUsbPermission = permitted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Paired Bluetooth devices (Bluetooth HID & SPP scanners)
        val adapter = getBluetoothAdapter(context)
        if (adapter != null && hasBluetoothConnectPermission(context)) {
            runCatching {
                adapter.bondedDevices.orEmpty().forEach { device ->
                    val name = device.name.orEmpty().ifBlank { device.address }
                    val lower = name.lowercase()
                    val majorClass = device.bluetoothClass?.majorDeviceClass
                    val isPeripheral = majorClass == BluetoothClass.Device.Major.PERIPHERAL
                    val isLikely = SCANNER_NAME_KEYWORDS.any { lower.contains(it) } ||
                        (isPeripheral && !PRINTER_NAME_KEYWORDS.any { lower.contains(it) })
                    val isPrinter = PRINTER_NAME_KEYWORDS.any { lower.contains(it) } ||
                        majorClass == BluetoothClass.Device.Major.IMAGING
                    if (isLikely || !isPrinter) {
                        val id = "BT:${device.address}"
                        if (seenIds.add(id)) {
                            val active = isBluetoothDeviceConnected(device)
                            results += DetectedScannerDevice(
                                id = id,
                                name = name,
                                transport = if (isPeripheral) "Bluetooth HID/SPP" else "Bluetooth",
                                detail = "${device.address} • ${if (active) "Terhubung aktif" else "Terpasang (Paired)"}",
                                isConnectedNow = active,
                                isLikelyScanner = isLikely,
                                bluetoothAddress = device.address
                            )
                        }
                    }
                }
            }
        }

        return results.sortedWith(
            compareByDescending<DetectedScannerDevice> { it.isConnectedNow }
                .thenByDescending { it.isLikelyScanner }
                .thenBy { it.name.lowercase() }
        )
    }

    @SuppressLint("MissingPermission")
    fun detectConnectedPrinters(context: Context): List<DetectedPrinterDevice> {
        val results = mutableListOf<DetectedPrinterDevice>()
        val seenIds = mutableSetOf<String>()

        // 1. USB OTG Connected Printers
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager != null) {
            runCatching {
                usbManager.deviceList.values.forEach { usb ->
                    val label = usbDisplayName(usb)
                    val lower = label.lowercase()
                    val isPrinterClass = hasUsbClass(usb, UsbConstants.USB_CLASS_PRINTER)
                    val isLikely = isPrinterClass || PRINTER_NAME_KEYWORDS.any { lower.contains(it) }
                    val isKeyboardOnly = hasUsbClass(usb, UsbConstants.USB_CLASS_HID) && !isLikely
                    if (!isKeyboardOnly) {
                        val address = "USB:${hex4(usb.vendorId)}:${hex4(usb.productId)}:${usb.deviceName}"
                        val id = "USB:${hex4(usb.vendorId)}:${hex4(usb.productId)}"
                        if (seenIds.add(id)) {
                            val permitted = runCatching { usbManager.hasPermission(usb) }.getOrDefault(false)
                            results += DetectedPrinterDevice(
                                id = id,
                                name = label,
                                address = address,
                                transport = "USB OTG",
                                isConnectedNow = true,
                                isPaired = true,
                                isLikelyPrinter = isLikely,
                                usbDeviceName = usb.deviceName,
                                hasUsbPermission = permitted
                            )
                        }
                    }
                }
            }
        }

        // 2. Paired & Connected Bluetooth Devices
        val adapter = getBluetoothAdapter(context)
        if (adapter != null && hasBluetoothConnectPermission(context)) {
            runCatching {
                adapter.bondedDevices.orEmpty().forEach { device ->
                    val name = device.name.orEmpty().ifBlank { device.address }
                    val lower = name.lowercase()
                    val majorClass = device.bluetoothClass?.majorDeviceClass
                    val deviceClass = device.bluetoothClass?.deviceClass ?: 0
                    val isImaging = majorClass == BluetoothClass.Device.Major.IMAGING ||
                        majorClass == BluetoothClass.Device.Major.UNCATEGORIZED ||
                        (deviceClass and 0x0680) == 0x0680
                    val isScanner = SCANNER_NAME_KEYWORDS.any { lower.contains(it) }
                    val isLikely = (!isScanner) && (PRINTER_NAME_KEYWORDS.any { lower.contains(it) } ||
                        majorClass == BluetoothClass.Device.Major.IMAGING)
                    val active = isBluetoothDeviceConnected(device)
                    val id = "BT:${device.address}"
                    if (seenIds.add(id)) {
                        results += DetectedPrinterDevice(
                            id = id,
                            name = name,
                            address = device.address,
                            transport = "Bluetooth",
                            isConnectedNow = active,
                            isPaired = true,
                            isLikelyPrinter = isLikely || isImaging
                        )
                    }
                }
            }
        }

        return results.sortedWith(
            compareByDescending<DetectedPrinterDevice> { it.isConnectedNow }
                .thenByDescending { it.isLikelyPrinter }
                .thenBy { it.name.lowercase() }
        )
    }

    fun requestUsbPermission(
        context: Context,
        usbDeviceName: String,
        onResult: (Boolean) -> Unit
    ) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val device = usbManager?.deviceList?.get(usbDeviceName)
        if (usbManager == null || device == null) {
            onResult(false)
            return
        }
        if (usbManager.hasPermission(device)) {
            onResult(true)
            return
        }
        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_USB_PERMISSION) return
                runCatching { appContext.unregisterReceiver(this) }
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                onResult(granted)
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            appContext,
            usbDeviceName.hashCode(),
            Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
            flags
        )
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    @SuppressLint("MissingPermission")
    fun startBluetoothDiscovery(
        context: Context,
        onDeviceFound: (DetectedPrinterDevice) -> Unit,
        onFinished: () -> Unit
    ): (() -> Unit)? {
        val adapter = getBluetoothAdapter(context)
        if (adapter == null || !hasBluetoothConnectPermission(context) || !hasBluetoothScanPermission(context)) {
            onFinished()
            return null
        }
        if (!adapter.isEnabled) {
            onFinished()
            return null
        }
        val appContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = IntentCompat.getParcelableExtra(
                            intent,
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        ) ?: return
                        val name = runCatching { device.name }.getOrNull().orEmpty().ifBlank { device.address }
                        val lower = name.lowercase()
                        val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
                        val isLikely = PRINTER_NAME_KEYWORDS.any { lower.contains(it) } ||
                            majorClass == BluetoothClass.Device.Major.IMAGING
                        val bonded = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
                        onDeviceFound(
                            DetectedPrinterDevice(
                                id = "BT:${device.address}",
                                name = name,
                                address = device.address,
                                transport = "Bluetooth",
                                isConnectedNow = false,
                                isPaired = bonded,
                                isLikelyPrinter = isLikely
                            )
                        )
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        runCatching { appContext.unregisterReceiver(this) }
                        onFinished()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        runCatching {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        }.onFailure {
            runCatching { appContext.unregisterReceiver(receiver) }
            onFinished()
            return null
        }
        return {
            runCatching {
                if (hasBluetoothScanPermission(appContext)) adapter.cancelDiscovery()
            }
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    private fun isExternalKeyboardOrScanner(dev: InputDevice): Boolean {
        if (dev.isVirtual || dev.id <= 0) return false
        val nameLower = dev.name.orEmpty().lowercase()
        if (IGNORED_INTERNAL_INPUT_KEYWORDS.any { nameLower.contains(it) }) return false
        val hasKeyboardSource = (dev.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
        if (!hasKeyboardSource) return false
        val hasAlphaKeys = dev.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
        val isExternalApi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) dev.isExternal else false
        val hasUsbIds = dev.vendorId != 0 || dev.productId != 0
        val hasKeyword = SCANNER_NAME_KEYWORDS.any { nameLower.contains(it) }
        return hasAlphaKeys && (isExternalApi || hasUsbIds || hasKeyword)
    }

    private fun isBluetoothDeviceConnected(device: BluetoothDevice): Boolean {
        return runCatching {
            val method = device.javaClass.getMethod("isConnected")
            (method.invoke(device) as? Boolean) == true
        }.getOrDefault(false)
    }

    private fun hasUsbClass(device: UsbDevice, targetClass: Int): Boolean {
        if (device.deviceClass == targetClass) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == targetClass) return true
        }
        return false
    }

    private fun usbDisplayName(device: UsbDevice): String {
        val prod = runCatching { device.productName }.getOrNull().orEmpty().trim()
        val mfg = runCatching { device.manufacturerName }.getOrNull().orEmpty().trim()
        return when {
            prod.isNotBlank() && mfg.isNotBlank() && !prod.contains(mfg, ignoreCase = true) -> "$mfg $prod"
            prod.isNotBlank() -> prod
            mfg.isNotBlank() -> "$mfg USB (${hex4(device.vendorId)}:${hex4(device.productId)})"
            else -> "Perangkat USB (${hex4(device.vendorId)}:${hex4(device.productId)})"
        }
    }

    private fun hex4(value: Int): String = value.toString(16).uppercase().padStart(4, '0')
}

/**
 * Utilities for cleaning, matching, and parsing barcode strings from HID keyboard
 * and Bluetooth SPP hardware scanners.
 */
object BarcodeInputSanitizer {
    private val CONTROL_AND_INVISIBLE_REGEX = Regex("[\\u0000-\\u001F\\u007F\\uFEFF\\u200B-\\u200D]")
    private val IGNORED_SYSTEM_KEYCODES = setOf(
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_HEADSETHOOK
    )

    fun cleanBarcodeValue(raw: String, stripWhitespace: Boolean = true): String {
        val noCtrl = raw.replace(CONTROL_AND_INVISIBLE_REGEX, "")
        return if (stripWhitespace) noCtrl.trim() else noCtrl
    }

    fun hasTerminatorChar(raw: String): Boolean =
        raw.any { it == '\n' || it == '\r' || it == '\t' || it == '\u0003' }

    /**
     * Appends [incomingChunk] into [buffer] and extracts any complete barcode payloads
     * terminated by '\r', '\n', '\t', or ETX (0x03). Any remaining unterminated suffix
     * stays in [buffer].
     */
    fun extractTerminatedBarcodes(
        buffer: StringBuilder,
        incomingChunk: String,
        stripWhitespace: Boolean = true
    ): List<String> {
        if (incomingChunk.isEmpty()) return emptyList()
        val emitted = mutableListOf<String>()
        for (ch in incomingChunk) {
            when (ch) {
                '\r', '\n', '\t', '\u0003' -> {
                    val code = cleanBarcodeValue(buffer.toString(), stripWhitespace)
                    buffer.setLength(0)
                    if (code.isNotEmpty()) {
                        emitted.add(code)
                    }
                }
                '\u0002' -> Unit // STX prefix
                else -> buffer.append(ch)
            }
        }
        return emitted
    }

    fun matchesBarcodeCode(storedBarcode: String, scannedCode: String): Boolean {
        val stored = cleanBarcodeValue(storedBarcode, stripWhitespace = true)
        val scanned = cleanBarcodeValue(scannedCode, stripWhitespace = true)
        if (stored.isEmpty() || scanned.isEmpty()) return false
        if (stored.equals(scanned, ignoreCase = true)) return true

        val compactStored = stored.replace(" ", "").replace("-", "")
        val compactScanned = scanned.replace(" ", "").replace("-", "")
        if (compactStored.isNotEmpty() && compactStored.equals(compactScanned, ignoreCase = true)) {
            return true
        }

        // Handle UPC-A (12 digits) vs EAN-13 leading-zero (13 digits starting with '0') equivalence
        val storedAllDigits = compactStored.all { it.isDigit() }
        val scannedAllDigits = compactScanned.all { it.isDigit() }
        if (storedAllDigits && scannedAllDigits) {
            if (compactStored.length == 12 && compactScanned == "0$compactStored") return true
            if (compactScanned.length == 12 && compactStored == "0$compactScanned") return true
        }
        return false
    }

    fun isTerminatorKeyCode(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
            keyCode == KeyEvent.KEYCODE_TAB
    }

    fun isHardwareScannerKeyEvent(native: KeyEvent): Boolean {
        if (native.keyCode in IGNORED_SYSTEM_KEYCODES) return false
        if (native.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD && native.action != KeyEvent.ACTION_MULTIPLE) {
            return false
        }
        val dev = native.device
        if (dev != null && dev.isVirtual) return false
        return true
    }

    fun extractPrintableChar(native: KeyEvent): Char? {
        val codePoint = native.getUnicodeChar(native.metaState)
        if (codePoint == 0 || (codePoint and KeyCharacterMap.COMBINING_ACCENT) != 0) return null
        return if (codePoint >= 32) codePoint.toChar() else null
    }
}

/**
 * Optional RFCOMM SPP reader for Bluetooth barcode scanners running in SPP (Serial Port) mode
 * instead of HID keyboard mode.
 */
class BluetoothSppScannerClient {
    private var job: Job? = null
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun startListening(
        context: Context,
        macAddress: String,
        scope: CoroutineScope,
        onBarcodeRead: (String) -> Unit,
        onStatusChanged: (Boolean, String) -> Unit
    ) {
        stop()
        val adapter = HardwareDeviceDiscovery.getBluetoothAdapter(context)
        if (adapter == null || !HardwareDeviceDiscovery.hasBluetoothConnectPermission(context)) {
            onStatusChanged(false, "Izin Bluetooth belum aktif")
            return
        }
        job = scope.launch(Dispatchers.IO) {
            try {
                val device = adapter.getRemoteDevice(macAddress)
                if (HardwareDeviceDiscovery.hasBluetoothScanPermission(context)) {
                    runCatching { adapter.cancelDiscovery() }
                }
                val sppSocket = runCatching {
                    device.createRfcommSocketToServiceRecord(HardwareDeviceDiscovery.SPP_UUID)
                }.getOrElse {
                    @Suppress("UNCHECKED_CAST")
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, 1) as BluetoothSocket
                }
                socket = sppSocket
                sppSocket.connect()
                withContext(Dispatchers.Main) {
                    onStatusChanged(true, "Terhubung ke scanner ${device.name ?: macAddress}")
                }
                val input = sppSocket.inputStream
                val readBuffer = ByteArray(512)
                val charBuffer = StringBuilder()
                var lastByteAtMs = 0L

                while (isActive) {
                    val available = runCatching { input.available() }.getOrDefault(0)
                    if (available > 0 || charBuffer.isEmpty()) {
                        val toRead = if (available > 0) available.coerceAtMost(readBuffer.size) else 1
                        val count = input.read(readBuffer, 0, toRead)
                        if (count < 0) break
                        if (count > 0) {
                            lastByteAtMs = System.currentTimeMillis()
                            val chunk = String(readBuffer, 0, count, Charsets.UTF_8)
                            val readyCodes = BarcodeInputSanitizer.extractTerminatedBarcodes(charBuffer, chunk)
                            for (code in readyCodes) {
                                withContext(Dispatchers.Main) {
                                    onBarcodeRead(code)
                                }
                            }
                        }
                    } else {
                        delay(45)
                        if (charBuffer.isNotEmpty() && System.currentTimeMillis() - lastByteAtMs >= 130L) {
                            val fallbackCode = BarcodeInputSanitizer.cleanBarcodeValue(charBuffer.toString())
                            charBuffer.setLength(0)
                            if (fallbackCode.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onBarcodeRead(fallbackCode)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusChanged(false, "Scanner Bluetooth dalam mode HID atau tidak merespons SPP (${e.localizedMessage ?: "cek koneksi"})")
                }
            } finally {
                runCatching { socket?.close() }
                socket = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { socket?.close() }
        socket = null
    }
}
