package com.example.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.example.data.entity.PrinterEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.repository.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalPrinterEngine {
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    private val SIZE_TALL = byteArrayOf(0x1D, 0x21, 0x01)
    private val AUTO_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
    private val OPEN_DRAWER = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

    fun columnsForWidthMm(widthMm: Int): Int {
        return when {
            widthMm >= 76 -> 48
            widthMm <= 44 -> 24
            else -> 32
        }
    }

    fun isUsbAddress(address: String): Boolean = address.startsWith("USB:", ignoreCase = true)

    fun isSystemPrintAddress(address: String): Boolean =
        address.equals("SYSTEM_PRINT", ignoreCase = true) || address.isBlank()

    fun buildEscPosReceiptBytes(
        printer: PrinterEntity,
        sale: SaleEntity,
        items: List<CartItem>,
        storeProfile: StoreProfileEntity?
    ): ByteArray {
        val cols = columnsForWidthMm(printer.widthMm)
        val out = ByteArrayOutputStream()
        fun writeBytes(bytes: ByteArray) = out.write(bytes)
        fun writeLine(text: String = "") {
            val ascii = sanitizeEscPosText(text)
            out.write((ascii + "\n").toByteArray(Charsets.ISO_8859_1))
        }

        val storeName = storeProfile?.name?.ifBlank { "KASLITE POS" } ?: "KASLITE POS"
        val storeAddress = storeProfile?.address.orEmpty().trim()
        val storePhone = storeProfile?.phone.orEmpty().trim()
        val footer = storeProfile?.receiptFooter?.ifBlank { "Terima kasih telah berbelanja!" }
            ?: "Terima kasih telah berbelanja!"
        val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale("id", "ID")).format(Date(sale.date))
        val divider = "-".repeat(cols)

        writeBytes(ESC_INIT)
        writeBytes(ALIGN_CENTER)
        writeBytes(BOLD_ON)
        writeBytes(SIZE_TALL)
        writeLine(storeName.take(cols))
        writeBytes(SIZE_NORMAL)
        writeBytes(BOLD_OFF)
        if (storeAddress.isNotBlank()) {
            wrapText(storeAddress, cols).forEach { writeLine(it) }
        }
        if (storePhone.isNotBlank()) {
            writeLine("Telp: $storePhone")
        }

        writeBytes(ALIGN_LEFT)
        writeLine(divider)
        writeLine(formatKeyValue("Faktur", sale.invoiceNumber, cols))
        writeLine(formatKeyValue("Waktu", dateStr, cols))
        writeLine(formatKeyValue("Kasir", sale.cashierName, cols))
        writeLine(formatKeyValue("Pelanggan", sale.customerName, cols))
        writeLine(divider)

        items.forEach { item ->
            val lineTotal = item.unitPrice * item.quantity + item.storeAdminFee
            val qtyStr = if (item.quantity % 1.0 == 0.0) {
                item.quantity.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", item.quantity).trimEnd('0').trimEnd('.')
            }
            val title = if (item.variantName.isNotBlank()) {
                "${item.productName} (${item.variantName})"
            } else {
                item.productName
            }
            wrapText(title, cols).forEach { writeLine(it) }
            val left = "$qtyStr ${item.unitName} x ${formatRp(item.unitPrice)}"
            val right = formatRp(lineTotal)
            writeLine(formatKeyValue(left, right, cols))
            if (item.storeAdminFee > 0.0) {
                writeLine(formatKeyValue("  + Admin", formatRp(item.storeAdminFee), cols))
            }
        }

        writeLine(divider)
        writeLine(formatKeyValue("Subtotal", "Rp ${formatRp(sale.subtotal)}", cols))
        if (sale.storeAdminFee > 0) writeLine(formatKeyValue("Admin Jasa", "+Rp ${formatRp(sale.storeAdminFee)}", cols))
        if (sale.shippingCost > 0) writeLine(formatKeyValue("Ongkir", "+Rp ${formatRp(sale.shippingCost)}", cols))
        if (sale.additionalFee > 0) writeLine(formatKeyValue("Biaya Tamb.", "+Rp ${formatRp(sale.additionalFee)}", cols))
        if (sale.discount + sale.promoDiscount > 0) {
            writeLine(formatKeyValue("Diskon", "-Rp ${formatRp(sale.discount + sale.promoDiscount)}", cols))
        }
        if (sale.tax > 0) writeLine(formatKeyValue("Pajak", "+Rp ${formatRp(sale.tax)}", cols))
        if (sale.depositUsed > 0) writeLine(formatKeyValue("Deposit", "-Rp ${formatRp(sale.depositUsed)}", cols))

        writeBytes(BOLD_ON)
        writeLine(formatKeyValue("TOTAL", "Rp ${formatRp(sale.finalAmount)}", cols))
        writeBytes(BOLD_OFF)
        writeLine(formatKeyValue("Metode", sale.paymentMethod, cols))
        if (sale.paymentMethod == "TUNAI" || sale.paymentMethod == "SPLIT") {
            writeLine(formatKeyValue("Bayar", "Rp ${formatRp(sale.amountPaid)}", cols))
            writeLine(formatKeyValue("Kembali", "Rp ${formatRp(sale.changeAmount)}", cols))
        }

        writeLine(divider)
        writeBytes(ALIGN_CENTER)
        wrapText(footer, cols).forEach { writeLine(it) }
        writeLine("")
        writeLine("")
        writeLine("")
        if (printer.openDrawer) {
            writeBytes(OPEN_DRAWER)
        }
        if (printer.autoCut) {
            writeBytes(AUTO_CUT)
        }
        return out.toByteArray()
    }

    fun buildTsplReceiptBytes(
        printer: PrinterEntity,
        sale: SaleEntity,
        items: List<CartItem>,
        storeProfile: StoreProfileEntity?
    ): ByteArray {
        val widthMm = printer.widthMm.coerceIn(30, 100)
        val heightMm = (35 + items.size * 6).coerceIn(40, 140)
        val storeName = sanitizeEscPosText(storeProfile?.name?.ifBlank { "KASLITE POS" } ?: "KASLITE POS")
        val sb = StringBuilder()
        sb.append("SIZE $widthMm mm, $heightMm mm\r\n")
        sb.append("GAP 2 mm, 0 mm\r\n")
        sb.append("DIRECTION 1\r\n")
        sb.append("CLS\r\n")
        var y = 16
        sb.append("TEXT 12,$y,\"2\",0,1,1,\"$storeName\"\r\n")
        y += 28
        sb.append("TEXT 12,$y,\"1\",0,1,1,\"${sanitizeEscPosText(sale.invoiceNumber)}\"\r\n")
        y += 22
        items.take(8).forEach { item ->
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity.toString()
            val line = "${qty}x ${sanitizeEscPosText(item.productName).take(18)} ${formatRp(item.unitPrice * item.quantity)}"
            sb.append("TEXT 12,$y,\"1\",0,1,1,\"$line\"\r\n")
            y += 20
        }
        sb.append("TEXT 12,$y,\"2\",0,1,1,\"TOTAL Rp ${formatRp(sale.finalAmount)}\"\r\n")
        sb.append("PRINT 1,1\r\n")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }

    fun buildTestPageBytes(
        printer: PrinterEntity,
        storeProfile: StoreProfileEntity?
    ): ByteArray {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
        val storeName = storeProfile?.name?.ifBlank { "KASLITE POS" } ?: "KASLITE POS"
        if (printer.language.equals("tspl", ignoreCase = true)) {
            val sb = StringBuilder()
            sb.append("SIZE ${printer.widthMm.coerceIn(30, 100)} mm, 30 mm\r\n")
            sb.append("GAP 2 mm, 0 mm\r\n")
            sb.append("CLS\r\n")
            sb.append("TEXT 16,20,\"2\",0,1,1,\"${sanitizeEscPosText(storeName)}\"\r\n")
            sb.append("TEXT 16,54,\"1\",0,1,1,\"TES CETAK STIKER OK\"\r\n")
            sb.append("TEXT 16,78,\"1\",0,1,1,\"$nowStr\"\r\n")
            sb.append("BARCODE 16,105,\"128\",45,1,0,2,2,\"KASLITE001\"\r\n")
            sb.append("PRINT 1,1\r\n")
            return sb.toString().toByteArray(Charsets.ISO_8859_1)
        }

        val cols = columnsForWidthMm(printer.widthMm)
        val divider = "-".repeat(cols)
        val out = ByteArrayOutputStream()
        fun writeLine(text: String = "") {
            out.write((sanitizeEscPosText(text) + "\n").toByteArray(Charsets.ISO_8859_1))
        }
        out.write(ESC_INIT)
        out.write(ALIGN_CENTER)
        out.write(BOLD_ON)
        out.write(SIZE_TALL)
        writeLine(storeName.take(cols))
        out.write(SIZE_NORMAL)
        out.write(BOLD_OFF)
        writeLine("TES PRINTER BERHASIL")
        out.write(ALIGN_LEFT)
        writeLine(divider)
        writeLine(formatKeyValue("Printer", printer.name, cols))
        writeLine(formatKeyValue("Koneksi", if (isUsbAddress(printer.bluetoothAddress)) "USB OTG" else "Bluetooth", cols))
        writeLine(formatKeyValue("Lebar", "${printer.widthMm}mm ($cols kol)", cols))
        writeLine(formatKeyValue("Bahasa", printer.language.uppercase(), cols))
        writeLine(formatKeyValue("Waktu", nowStr, cols))
        writeLine(divider)
        out.write(ALIGN_CENTER)
        writeLine("Printer siap digunakan KASLITE POS")
        writeLine("")
        writeLine("")
        writeLine("")
        if (printer.openDrawer) out.write(OPEN_DRAWER)
        if (printer.autoCut) out.write(AUTO_CUT)
        return out.toByteArray()
    }

    suspend fun sendToPrinter(
        context: Context,
        printer: PrinterEntity,
        payload: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        val address = printer.bluetoothAddress.trim()
        when {
            isUsbAddress(address) -> printViaUsb(context, address, payload)
            address.isNotBlank() && !isSystemPrintAddress(address) -> printViaBluetoothSpp(context, address, payload)
            else -> Result.failure(IllegalStateException("Alamat printer belum diatur"))
        }
    }

    @SuppressLint("MissingPermission")
    fun printViaBluetoothSpp(
        context: Context,
        macAddress: String,
        payload: ByteArray
    ): Result<String> {
        val adapter = HardwareDeviceDiscovery.getBluetoothAdapter(context)
            ?: return Result.failure(IllegalStateException("Perangkat ini tidak mendukung Bluetooth"))
        if (!HardwareDeviceDiscovery.hasBluetoothConnectPermission(context)) {
            return Result.failure(SecurityException("Izin Bluetooth belum diberikan. Buka Pengaturan Printer untuk memberi izin."))
        }
        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth HP sedang mati. Aktifkan Bluetooth terlebih dahulu."))
        }

        val cleanMac = macAddress.trim().uppercase()
        val device: BluetoothDevice = runCatching {
            adapter.getRemoteDevice(cleanMac)
        }.getOrElse {
            return Result.failure(IllegalArgumentException("Alamat MAC printer tidak valid ($cleanMac)"))
        }

        if (HardwareDeviceDiscovery.hasBluetoothScanPermission(context)) {
            runCatching {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
            }
        }

        var socket: BluetoothSocket? = null
        return try {
            socket = try {
                val standard = device.createRfcommSocketToServiceRecord(HardwareDeviceDiscovery.SPP_UUID)
                standard.connect()
                standard
            } catch (_: Exception) {
                @Suppress("UNCHECKED_CAST")
                val fallbackMethod = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                val fallback = fallbackMethod.invoke(device, 1) as BluetoothSocket
                fallback.connect()
                fallback
            }

            val output = socket.outputStream
            val chunkSize = 512
            var offset = 0
            while (offset < payload.size) {
                val count = minOf(chunkSize, payload.size - offset)
                output.write(payload, offset, count)
                output.flush()
                offset += count
                if (offset < payload.size) {
                    Thread.sleep(20)
                }
            }
            Thread.sleep(150)
            Result.success("Struk terkirim ke printer Bluetooth ${device.name ?: cleanMac}")
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Gagal terhubung ke printer ${device.name ?: cleanMac}. Pastikan printer menyala & sudah ter-pair (${e.localizedMessage ?: "koneksi gagal"})."
                )
            )
        } finally {
            runCatching { socket?.close() }
        }
    }

    fun printViaUsb(
        context: Context,
        usbAddress: String,
        payload: ByteArray
    ): Result<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return Result.failure(IllegalStateException("USB Host tidak didukung perangkat ini"))

        val targetDevice = resolveUsbDevice(usbManager, usbAddress)
            ?: return Result.failure(IllegalStateException("Printer USB tidak terdeteksi. Pastikan kabel OTG & printer menyala."))

        if (!usbManager.hasPermission(targetDevice)) {
            return Result.failure(SecurityException("Izin USB belum diberikan untuk printer ini. Ketuk Tes / Pilih Printer USB di Pengaturan."))
        }

        var selectedInterface: UsbInterface? = null
        var outEndpoint: UsbEndpoint? = null
        for (i in 0 until targetDevice.interfaceCount) {
            val iface = targetDevice.getInterface(i)
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT &&
                    ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                ) {
                    if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER || selectedInterface == null) {
                        selectedInterface = iface
                        outEndpoint = ep
                        if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) break
                    }
                }
            }
        }

        val iface = selectedInterface
            ?: return Result.failure(IllegalStateException("Endpoint cetak USB tidak ditemukan pada perangkat"))
        val endpoint = outEndpoint
            ?: return Result.failure(IllegalStateException("Saluran output USB tidak ditemukan"))

        val connection = usbManager.openDevice(targetDevice)
            ?: return Result.failure(IllegalStateException("Tidak dapat membuka koneksi USB printer"))

        return try {
            if (!connection.claimInterface(iface, true)) {
                return Result.failure(IllegalStateException("Gagal mengklaim antarmuka printer USB"))
            }
            val chunkSize = 4096
            var offset = 0
            while (offset < payload.size) {
                val len = minOf(chunkSize, payload.size - offset)
                val slice = payload.copyOfRange(offset, offset + len)
                val sent = connection.bulkTransfer(endpoint, slice, len, 5000)
                if (sent < 0) {
                    return Result.failure(IllegalStateException("Pengiriman data ke printer USB gagal"))
                }
                offset += len
            }
            Result.success("Struk terkirim ke printer USB")
        } finally {
            runCatching { connection.releaseInterface(iface) }
            runCatching { connection.close() }
        }
    }

    private fun resolveUsbDevice(usbManager: UsbManager, usbAddress: String): UsbDevice? {
        val parts = usbAddress.split(":", limit = 4)
        val targetVid = parts.getOrNull(1)?.toIntOrNull(16)
        val targetPid = parts.getOrNull(2)?.toIntOrNull(16)
        val targetName = parts.getOrNull(3)
        if (!targetName.isNullOrBlank()) {
            usbManager.deviceList[targetName]?.let { return it }
        }
        return usbManager.deviceList.values.firstOrNull { dev ->
            (targetVid == null || dev.vendorId == targetVid) &&
                (targetPid == null || dev.productId == targetPid)
        }
    }

    private fun formatKeyValue(left: String, right: String, cols: Int): String {
        val cleanLeft = sanitizeEscPosText(left)
        val cleanRight = sanitizeEscPosText(right)
        if (cleanLeft.length + cleanRight.length + 1 <= cols) {
            val spaces = (cols - cleanLeft.length - cleanRight.length).coerceAtLeast(1)
            return cleanLeft + " ".repeat(spaces) + cleanRight
        }
        val maxLeft = (cols - cleanRight.length - 1).coerceAtLeast(4)
        val trimmedLeft = cleanLeft.take(maxLeft)
        val spaces = (cols - trimmedLeft.length - cleanRight.length).coerceAtLeast(1)
        return trimmedLeft + " ".repeat(spaces) + cleanRight
    }

    private fun wrapText(text: String, cols: Int): List<String> {
        val clean = sanitizeEscPosText(text).trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= cols) return listOf(clean)
        val lines = mutableListOf<String>()
        var remaining = clean
        while (remaining.length > cols) {
            val breakIdx = remaining.lastIndexOf(' ', cols).let { if (it > 0) it else cols }
            lines += remaining.substring(0, breakIdx).trim()
            remaining = remaining.substring(breakIdx).trim()
        }
        if (remaining.isNotEmpty()) lines += remaining
        return lines
    }

    private fun sanitizeEscPosText(input: String): String {
        return input
            .replace("\u2022", "*")
            .replace("•", "*")
            .replace("—", "-")
            .replace("–", "-")
            .map { ch -> if (ch.code in 32..126 || ch == '\n') ch else ' ' }
            .joinToString("")
    }

    private fun formatRp(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())
    }
}
