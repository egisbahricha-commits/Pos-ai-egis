package com.example.data.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StockMovementEntity
import com.example.data.pricing.ProfitLossCalculator
import com.example.data.pricing.ProfitLossPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExcelProductRowDraft(
    val rowNumber: Int,
    val name: String,
    val barcode: String,
    val category: String,
    val baseUnit: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stock: Double,
    val minStock: Double,
    val productType: String,
    val brand: String
)

data class ExcelProductInspectResult(
    val fileName: String,
    val fileFormat: String, // "XLSX" or "CSV"
    val totalRawRows: Int,
    val rows: List<ExcelProductRowDraft>,
    val skippedRows: List<String>,
    val detectedColumns: List<String>
)

data class ExcelProductImportSummary(
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val newCategoriesCount: Int,
    val message: String
)

object ExcelProductReportManager {

    private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
    private val fileTimestampFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    // =========================================================================
    // 1. PRODUCT IMPORT (.xlsx / .csv)
    // =========================================================================

    suspend fun inspectProductSpreadsheet(
        context: Context,
        uri: Uri
    ): ExcelProductInspectResult = withContext(Dispatchers.IO) {
        val fileName = resolveDisplayName(context, uri)
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Gagal membaca file yang dipilih.")
        if (rawBytes.isEmpty()) {
            throw IllegalStateException("File kosong.")
        }

        val isZipXlsx = rawBytes.size >= 4 &&
            rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()

        val tableRows: List<List<String>>
        val formatLabel: String
        if (isZipXlsx || fileName.endsWith(".xlsx", ignoreCase = true)) {
            formatLabel = "XLSX"
            tableRows = parseFirstXlsxSheet(ByteArrayInputStream(rawBytes))
        } else if (fileName.endsWith(".xls", ignoreCase = true)) {
            throw IllegalStateException(
                "Format .xls biner lama belum didukung langsung. Simpan ulang file sebagai .xlsx (Excel Modern) atau .csv."
            )
        } else {
            formatLabel = "CSV"
            tableRows = parseCsvRows(String(rawBytes, StandardCharsets.UTF_8))
        }

        parseProductTableRows(
            fileName = fileName,
            formatLabel = formatLabel,
            tableRows = tableRows
        )
    }

    internal fun parseProductTableRows(
        fileName: String,
        formatLabel: String,
        tableRows: List<List<String>>
    ): ExcelProductInspectResult {
        val nonEmptyRows = tableRows.filter { row -> row.any { it.trim().isNotEmpty() } }
        if (nonEmptyRows.isEmpty()) {
            throw IllegalStateException("File tidak berisi baris data.")
        }

        val headerRowIndex = nonEmptyRows.indexOfFirst { row ->
            row.any { cell ->
                val norm = normalizeHeader(cell)
                norm.contains("nama") || norm.contains("name") || norm.contains("produk") ||
                    norm.contains("barcode") || norm.contains("harga") || norm.contains("jual")
            }
        }.let { if (it >= 0) it else 0 }

        val headers = nonEmptyRows[headerRowIndex].map { it.trim() }
        val colMap = mapProductColumns(headers)
        if (colMap.nameIdx == -1 && colMap.barcodeIdx == -1) {
            throw IllegalStateException(
                "Kolom 'Nama Produk' tidak ditemukan. Gunakan template Excel (.xlsx) atau pastikan baris header mencantumkan kolom Nama/Produk."
            )
        }

        val drafts = mutableListOf<ExcelProductRowDraft>()
        val skipped = mutableListOf<String>()

        for (i in (headerRowIndex + 1) until nonEmptyRows.size) {
            val row = nonEmptyRows[i]
            val excelRowNum = i + 1
            val rawName = cellAt(row, colMap.nameIdx).trim()
            val rawBarcode = cellAt(row, colMap.barcodeIdx).trim()
            if (rawName.isBlank()) {
                if (rawBarcode.isNotBlank()) {
                    skipped.add("Baris $excelRowNum: nama produk kosong (barcode $rawBarcode)")
                }
                continue
            }

            val category = cellAt(row, colMap.categoryIdx).trim().ifBlank { "Umum" }
            val unit = cellAt(row, colMap.unitIdx).trim().ifBlank { "pcs" }
            val purchasePrice = parseFlexibleNumber(cellAt(row, colMap.purchaseIdx)).coerceAtLeast(0.0)
            val sellingPrice = parseFlexibleNumber(cellAt(row, colMap.sellingIdx)).coerceAtLeast(0.0)
            val stock = parseFlexibleNumber(cellAt(row, colMap.stockIdx)).coerceAtLeast(0.0)
            val minStock = cellAt(row, colMap.minStockIdx).let {
                if (it.isBlank()) 5.0 else parseFlexibleNumber(it).coerceAtLeast(0.0)
            }
            val rawType = cellAt(row, colMap.typeIdx).trim().uppercase(Locale.US)
            val productType = if (rawType.contains("DIGITAL") || rawType.contains("PPOB") || rawType.contains("JASA")) {
                "DIGITAL"
            } else {
                "FISIK"
            }
            val brand = cellAt(row, colMap.brandIdx).trim()

            drafts.add(
                ExcelProductRowDraft(
                    rowNumber = excelRowNum,
                    name = rawName,
                    barcode = rawBarcode,
                    category = category,
                    baseUnit = unit,
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    minStock = minStock,
                    productType = productType,
                    brand = brand
                )
            )
        }

        return ExcelProductInspectResult(
            fileName = fileName,
            fileFormat = formatLabel,
            totalRawRows = (nonEmptyRows.size - headerRowIndex - 1).coerceAtLeast(0),
            rows = drafts,
            skippedRows = skipped,
            detectedColumns = headers.filter { it.isNotBlank() }
        )
    }

    suspend fun importProductsToRoom(
        db: GeraiUnaDatabase,
        inspect: ExcelProductInspectResult,
        updateExistingByBarcodeOrName: Boolean
    ): ExcelProductImportSummary = withContext(Dispatchers.IO) {
        if (inspect.rows.isEmpty()) {
            return@withContext ExcelProductImportSummary(0, 0, inspect.skippedRows.size, 0, "Tidak ada baris produk valid untuk diimpor.")
        }

        var inserted = 0
        var updated = 0
        var skipped = inspect.skippedRows.size
        var newCategories = 0

        db.withTransaction {
            val productDao = db.productDao()
            val movementDao = db.stockMovementDao()
            val existingCategories = productDao.getAllCategoriesList().toMutableList()
            val existingCategoryNames = existingCategories.map { it.name.trim().lowercase(Locale.US) }.toMutableSet()

            val existingProducts = productDao.getAllProductsList().toMutableList()
            val byBarcode = existingProducts
                .filter { it.barcode.isNotBlank() }
                .associateBy { it.barcode.trim().lowercase(Locale.US) }
                .toMutableMap()
            val byName = existingProducts
                .associateBy { it.name.trim().lowercase(Locale.US) }
                .toMutableMap()

            val now = System.currentTimeMillis()
            for (draft in inspect.rows) {
                val catNorm = draft.category.trim().lowercase(Locale.US)
                if (catNorm.isNotBlank() && existingCategoryNames.add(catNorm)) {
                    productDao.insertCategory(CategoryEntity(name = draft.category.trim(), icon = "store"))
                    newCategories++
                }

                val barcodeKey = draft.barcode.trim().lowercase(Locale.US)
                val nameKey = draft.name.trim().lowercase(Locale.US)
                val matched = (if (barcodeKey.isNotBlank()) byBarcode[barcodeKey] else null) ?: byName[nameKey]

                if (matched != null) {
                    if (updateExistingByBarcodeOrName) {
                        val updatedEntity = matched.copy(
                            barcode = draft.barcode.ifBlank { matched.barcode },
                            name = draft.name,
                            category = draft.category,
                            baseUnit = draft.baseUnit,
                            purchasePrice = draft.purchasePrice,
                            sellingPrice = draft.sellingPrice,
                            stock = draft.stock,
                            minStock = draft.minStock,
                            productType = draft.productType,
                            brand = draft.brand.ifBlank { matched.brand },
                            isActive = true
                        )
                        productDao.updateProduct(updatedEntity)
                        if (kotlin.math.abs(draft.stock - matched.stock) > 0.0001) {
                            val diff = draft.stock - matched.stock
                            movementDao.insertMovement(
                                StockMovementEntity(
                                    productId = matched.id,
                                    productName = updatedEntity.name,
                                    date = now,
                                    movementType = if (diff >= 0) "PENYESUAIAN_MASUK" else "PENYESUAIAN_KELUAR",
                                    quantityChange = diff,
                                    unit = updatedEntity.baseUnit,
                                    previousStock = matched.stock,
                                    currentStock = draft.stock,
                                    referenceNumber = "IMPORT-EXCEL",
                                    notes = "Update dari file ${inspect.fileName}",
                                    loggedBy = "Import Excel"
                                )
                            )
                        }
                        if (barcodeKey.isNotBlank()) byBarcode[barcodeKey] = updatedEntity
                        byName[nameKey] = updatedEntity
                        updated++
                    } else {
                        skipped++
                    }
                } else {
                    val newProduct = ProductEntity(
                        barcode = draft.barcode,
                        name = draft.name,
                        category = draft.category,
                        baseUnit = draft.baseUnit,
                        purchasePrice = draft.purchasePrice,
                        sellingPrice = draft.sellingPrice,
                        stock = draft.stock,
                        minStock = draft.minStock,
                        brand = draft.brand,
                        isActive = true,
                        productType = draft.productType,
                        hasNoStock = draft.productType == "DIGITAL",
                        createdAt = now
                    )
                    val newId = productDao.insertProduct(newProduct)
                    val saved = newProduct.copy(id = newId)
                    if (draft.stock > 0.0 && draft.productType == "FISIK") {
                        movementDao.insertMovement(
                            StockMovementEntity(
                                productId = newId,
                                productName = saved.name,
                                date = now,
                                movementType = "PENYESUAIAN_MASUK",
                                quantityChange = draft.stock,
                                unit = saved.baseUnit,
                                previousStock = 0.0,
                                currentStock = draft.stock,
                                referenceNumber = "IMPORT-EXCEL",
                                notes = "Stok awal import ${inspect.fileName}",
                                loggedBy = "Import Excel"
                            )
                        )
                    }
                    if (barcodeKey.isNotBlank()) byBarcode[barcodeKey] = saved
                    byName[nameKey] = saved
                    inserted++
                }
            }
        }

        ExcelProductImportSummary(
            insertedCount = inserted,
            updatedCount = updated,
            skippedCount = skipped,
            newCategoriesCount = newCategories,
            message = "Import selesai: $inserted produk baru, $updated diperbarui, $skipped dilewati."
        )
    }

    suspend fun exportProductTemplateXlsx(context: Context, targetUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = generateProductTemplateXlsxBytes()
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(bytes)
                } ?: throw IllegalStateException("Tidak dapat membuka lokasi tujuan file.")
                Unit
            }
        }

    fun generateProductTemplateXlsxBytes(): ByteArray {
        val sheet = XlsxSheetSpec(
            name = "Template_Produk",
            rows = listOf(
                listOf(
                    cellStr("Nama Produk", style = 1),
                    cellStr("Barcode / SKU", style = 1),
                    cellStr("Kategori", style = 1),
                    cellStr("Satuan", style = 1),
                    cellStr("Harga Modal", style = 1),
                    cellStr("Harga Jual", style = 1),
                    cellStr("Stok", style = 1),
                    cellStr("Stok Minimal", style = 1),
                    cellStr("Tipe (FISIK/DIGITAL)", style = 1),
                    cellStr("Merek", style = 1)
                ),
                listOf(
                    cellStr("Beras Ramos 5kg"),
                    cellStr("8991002100011"),
                    cellStr("Sembako & Kebutuhan Pokok"),
                    cellStr("sak"),
                    cellNum(64000.0),
                    cellNum(72000.0),
                    cellNum(25.0),
                    cellNum(5.0),
                    cellStr("FISIK"),
                    cellStr("Cap Bunga")
                ),
                listOf(
                    cellStr("Kopi Susu Kaleng 240ml"),
                    cellStr("8992761135012"),
                    cellStr("Makanan & Minuman"),
                    cellStr("kaleng"),
                    cellNum(4500.0),
                    cellNum(6500.0),
                    cellNum(48.0),
                    cellNum(10.0),
                    cellStr("FISIK"),
                    cellStr("KopiKu")
                )
            )
        )
        return buildXlsxWorkbook(listOf(sheet))
    }

    // =========================================================================
    // 2. REPORT EXPORT (.xlsx)
    // =========================================================================

    suspend fun generateReportsXlsxBytes(
        db: GeraiUnaDatabase,
        storeName: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val sales = db.saleDao().getAllSales().first()
        val saleItems = db.saleDao().getAllSaleItems().first()
        val products = db.productDao().getAllProducts().first()
        val expenses = db.financeDao().getAllExpenses().first()
        val incomes = db.financeDao().getAllIncomes().first()
        val customers = db.contactDao().getAllCustomers().first()
        val suppliers = db.contactDao().getAllSuppliers().first()

        val saleById = sales.associateBy { it.id }

        // Sheet 1: Ringkasan Keuangan (Harian, Mingguan, Bulanan, Tahunan)
        val summaryRows = mutableListOf<List<XlsxCell>>()
        summaryRows.add(listOf(cellStr("LAPORAN KEUANGAN KASLITE - ${storeName.ifBlank { "Toko" }}", style = 1)))
        summaryRows.add(listOf(cellStr("Diekspor pada: ${dateTimeFormatter.format(Date())}")))
        summaryRows.add(emptyList())
        summaryRows.add(
            listOf(
                cellStr("Periode", style = 1),
                cellStr("Jumlah Transaksi", style = 1),
                cellStr("Penjualan Bersih", style = 1),
                cellStr("HPP / Modal", style = 1),
                cellStr("Laba Kotor", style = 1),
                cellStr("Pemasukan Jasa", style = 1),
                cellStr("Biaya Operasional", style = 1),
                cellStr("Laba Bersih", style = 1)
            )
        )
        ProfitLossPeriod.entries.forEach { period ->
            val pl = ProfitLossCalculator.calculate(
                period = period,
                offset = 0,
                sales = sales,
                saleItems = saleItems,
                expenses = expenses,
                incomes = incomes,
                products = products
            )
            summaryRows.add(
                listOf(
                    cellStr("${period.label} (${pl.window.title})"),
                    cellNum(pl.transactionCount.toDouble()),
                    cellNum(pl.netSales),
                    cellNum(pl.cogs),
                    cellNum(pl.grossProfit),
                    cellNum(pl.otherIncomes + pl.agentCommissionIncome),
                    cellNum(pl.operatingExpenses),
                    cellNum(pl.netProfit)
                )
            )
        }

        // Sheet 2: Daftar Transaksi Penjualan
        val salesRows = mutableListOf<List<XlsxCell>>()
        salesRows.add(
            listOf(
                cellStr("No Faktur", style = 1),
                cellStr("Tanggal", style = 1),
                cellStr("Pelanggan", style = 1),
                cellStr("Kasir", style = 1),
                cellStr("Metode Bayar", style = 1),
                cellStr("Subtotal", style = 1),
                cellStr("Diskon", style = 1),
                cellStr("Pajak", style = 1),
                cellStr("Admin Toko", style = 1),
                cellStr("Total Akhir", style = 1),
                cellStr("Dibayar", style = 1),
                cellStr("Status", style = 1)
            )
        )
        sales.forEach { s ->
            salesRows.add(
                listOf(
                    cellStr(s.invoiceNumber),
                    cellStr(dateTimeFormatter.format(Date(s.date))),
                    cellStr(s.customerName),
                    cellStr(s.cashierName),
                    cellStr(s.paymentMethod),
                    cellNum(s.subtotal),
                    cellNum(s.discount + s.promoDiscount),
                    cellNum(s.tax),
                    cellNum(s.storeAdminFee),
                    cellNum(s.finalAmount),
                    cellNum(s.amountPaid),
                    cellStr(s.status)
                )
            )
        }

        // Sheet 3: Detail Item Terjual
        val itemRows = mutableListOf<List<XlsxCell>>()
        itemRows.add(
            listOf(
                cellStr("No Faktur", style = 1),
                cellStr("Tanggal", style = 1),
                cellStr("Nama Produk", style = 1),
                cellStr("Varian", style = 1),
                cellStr("Qty", style = 1),
                cellStr("Satuan", style = 1),
                cellStr("Harga Satuan", style = 1),
                cellStr("Modal Snapshot", style = 1),
                cellStr("Subtotal", style = 1)
            )
        )
        saleItems.forEach { item ->
            val parent = saleById[item.saleId]
            itemRows.add(
                listOf(
                    cellStr(parent?.invoiceNumber ?: "#${item.saleId}"),
                    cellStr(parent?.let { dateTimeFormatter.format(Date(it.date)) } ?: ""),
                    cellStr(item.productName),
                    cellStr(item.variantName),
                    cellNum(item.quantity),
                    cellStr(item.unit),
                    cellNum(item.unitPrice),
                    cellNum(item.purchasePriceSnapshot),
                    cellNum(item.subtotal)
                )
            )
        }

        // Sheet 4: Stok Produk
        val stockRows = mutableListOf<List<XlsxCell>>()
        stockRows.add(
            listOf(
                cellStr("Nama Produk", style = 1),
                cellStr("Barcode", style = 1),
                cellStr("Kategori", style = 1),
                cellStr("Satuan", style = 1),
                cellStr("Harga Modal", style = 1),
                cellStr("Harga Jual", style = 1),
                cellStr("Stok Saat Ini", style = 1),
                cellStr("Nilai Aset Modal", style = 1),
                cellStr("Tipe", style = 1),
                cellStr("Status", style = 1)
            )
        )
        products.forEach { p ->
            stockRows.add(
                listOf(
                    cellStr(p.name),
                    cellStr(p.barcode),
                    cellStr(p.category),
                    cellStr(p.baseUnit),
                    cellNum(p.purchasePrice),
                    cellNum(p.sellingPrice),
                    cellNum(p.stock),
                    cellNum((p.stock * p.purchasePrice).coerceAtLeast(0.0)),
                    cellStr(p.productType),
                    cellStr(if (p.isActive) "Aktif" else "Nonaktif")
                )
            )
        }

        // Sheet 5: Pengeluaran & Pemasukan
        val cashflowRows = mutableListOf<List<XlsxCell>>()
        cashflowRows.add(
            listOf(
                cellStr("Jenis", style = 1),
                cellStr("Tanggal", style = 1),
                cellStr("Kategori / Sumber", style = 1),
                cellStr("Nominal", style = 1),
                cellStr("Akun / Metode", style = 1),
                cellStr("Catatan", style = 1),
                cellStr("Dicatat Oleh", style = 1)
            )
        )
        expenses.forEach { e ->
            cashflowRows.add(
                listOf(
                    cellStr("PENGELUARAN"),
                    cellStr(dateTimeFormatter.format(Date(e.date))),
                    cellStr(e.category),
                    cellNum(e.amount),
                    cellStr(e.paymentSource),
                    cellStr(e.notes),
                    cellStr(e.recordedBy)
                )
            )
        }
        incomes.forEach { inc ->
            cashflowRows.add(
                listOf(
                    cellStr("PEMASUKAN_JASA"),
                    cellStr(dateTimeFormatter.format(Date(inc.date))),
                    cellStr(inc.source),
                    cellNum(inc.amount),
                    cellStr("-"),
                    cellStr(inc.notes),
                    cellStr(inc.recordedBy)
                )
            )
        }

        // Sheet 6: Utang & Piutang
        val debtRows = mutableListOf<List<XlsxCell>>()
        debtRows.add(
            listOf(
                cellStr("Tipe", style = 1),
                cellStr("Nama Pihak", style = 1),
                cellStr("No HP", style = 1),
                cellStr("Saldo Utang / Piutang", style = 1),
                cellStr("Catatan", style = 1)
            )
        )
        customers.filter { it.totalReceivable > 0.0 }.forEach { c ->
            debtRows.add(
                listOf(
                    cellStr("PIUTANG PELANGGAN"),
                    cellStr(c.name),
                    cellStr(c.phone),
                    cellNum(c.totalReceivable),
                    cellStr(c.notes)
                )
            )
        }
        suppliers.filter { it.totalDebt > 0.0 }.forEach { sup ->
            debtRows.add(
                listOf(
                    cellStr("UTANG PEMASOK"),
                    cellStr(sup.name),
                    cellStr(sup.phone),
                    cellNum(sup.totalDebt),
                    cellStr(sup.notes)
                )
            )
        }

        buildXlsxWorkbook(
            listOf(
                XlsxSheetSpec("Ringkasan_Keuangan", summaryRows),
                XlsxSheetSpec("Penjualan", salesRows),
                XlsxSheetSpec("Detail_Item_Terjual", itemRows),
                XlsxSheetSpec("Stok_Produk", stockRows),
                XlsxSheetSpec("Pengeluaran_Pemasukan", cashflowRows),
                XlsxSheetSpec("Utang_Piutang", debtRows)
            )
        )
    }

    suspend fun exportReportsXlsxToUri(
        context: Context,
        db: GeraiUnaDatabase,
        storeName: String,
        targetUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = generateReportsXlsxBytes(db, storeName)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(bytes)
            } ?: throw IllegalStateException("Tidak dapat menulis file laporan ke lokasi pilihan.")
            Unit
        }
    }

    suspend fun createShareableReportXlsxUri(
        context: Context,
        db: GeraiUnaDatabase,
        storeName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = generateReportsXlsxBytes(db, storeName)
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeStore = storeName.ifBlank { "Toko" }
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(24)
            val stamp = fileTimestampFormatter.format(Date())
            val outFile = File(exportsDir, "Laporan_KASLITE_${safeStore}_$stamp.xlsx")
            outFile.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }
    }

    // =========================================================================
    // 3. PURE-KOTLIN XLSX READER & CSV PARSER
    // =========================================================================

    internal fun parseCsvRows(text: String): List<List<String>> {
        val clean = text.removePrefix("\uFEFF")
        val firstLine = clean.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < clean.length && clean[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentRow.add(cell.toString())
                    cell.clear()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < clean.length && clean[i + 1] == '\n') i++
                    currentRow.add(cell.toString())
                    cell.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    internal fun parseFirstXlsxSheet(input: InputStream): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val normalizedName = entry.name.removePrefix("/").lowercase(Locale.US)
                    if (normalizedName == "xl/sharedstrings.xml" ||
                        (normalizedName.startsWith("xl/worksheets/sheet") && normalizedName.endsWith(".xml"))
                    ) {
                        entries[normalizedName] = zis.readBytes()
                    }
                }
                zis.closeEntry()
            }
        }

        val sharedStrings = entries["xl/sharedstrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries
                .filter { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }
                .minByOrNull { it.key }
                ?.value
            ?: throw IllegalStateException("File Excel (.xlsx) tidak memiliki worksheet yang valid.")

        return parseWorksheetXml(sheetBytes, sharedStrings)
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inSi = false
        var inText = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = localTag(parser.name)
                    if (tag == "si") {
                        inSi = true
                        sb.clear()
                    } else if (inSi && tag == "t") {
                        inText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inText) sb.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    val tag = localTag(parser.name)
                    if (tag == "t") {
                        inText = false
                    } else if (tag == "si") {
                        list.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var currentRow = mutableListOf<String>()
        var currentColIdx = -1
        var cellType = ""
        var inV = false
        var inT = false
        var textBuf = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (localTag(parser.name)) {
                        "row" -> {
                            currentRow = mutableListOf()
                            currentColIdx = -1
                        }
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            val colFromRef = columnIndexFromRef(ref)
                            currentColIdx = if (colFromRef >= 0) colFromRef else currentColIdx + 1
                            while (currentRow.size <= currentColIdx) currentRow.add("")
                            textBuf.clear()
                        }
                        "v" -> {
                            inV = true
                            textBuf.clear()
                        }
                        "t" -> {
                            inT = true
                            textBuf.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inT) textBuf.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    when (localTag(parser.name)) {
                        "v" -> {
                            inV = false
                            val rawVal = textBuf.toString()
                            val resolved = if (cellType == "s") {
                                val idx = rawVal.trim().toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                            } else {
                                rawVal
                            }
                            if (currentColIdx >= 0 && currentColIdx < currentRow.size) {
                                currentRow[currentColIdx] = resolved
                            }
                        }
                        "t" -> {
                            inT = false
                            if (cellType == "inlineStr" && currentColIdx >= 0 && currentColIdx < currentRow.size) {
                                currentRow[currentColIdx] = textBuf.toString()
                            }
                        }
                        "row" -> {
                            rows.add(currentRow)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun localTag(rawName: String?): String {
        val name = rawName ?: return ""
        val colon = name.indexOf(':')
        return if (colon >= 0) name.substring(colon + 1) else name
    }

    private fun columnIndexFromRef(cellRef: String): Int {
        if (cellRef.isBlank()) return -1
        var col = 0
        var hasLetters = false
        for (ch in cellRef) {
            if (ch in 'A'..'Z' || ch in 'a'..'z') {
                hasLetters = true
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return if (hasLetters) col - 1 else -1
    }

    private data class ProductColMap(
        val nameIdx: Int = -1,
        val barcodeIdx: Int = -1,
        val categoryIdx: Int = -1,
        val unitIdx: Int = -1,
        val purchaseIdx: Int = -1,
        val sellingIdx: Int = -1,
        val stockIdx: Int = -1,
        val minStockIdx: Int = -1,
        val typeIdx: Int = -1,
        val brandIdx: Int = -1
    )

    private fun mapProductColumns(headers: List<String>): ProductColMap {
        var nameIdx = -1
        var barcodeIdx = -1
        var categoryIdx = -1
        var unitIdx = -1
        var purchaseIdx = -1
        var sellingIdx = -1
        var stockIdx = -1
        var minStockIdx = -1
        var typeIdx = -1
        var brandIdx = -1

        headers.forEachIndexed { idx, h ->
            val norm = normalizeHeader(h)
            when {
                nameIdx == -1 && (norm.contains("nama") || norm == "produk" || norm.contains("item") || norm == "name") ->
                    nameIdx = idx
                barcodeIdx == -1 && (norm.contains("barcode") || norm.contains("sku") || norm == "kode") ->
                    barcodeIdx = idx
                categoryIdx == -1 && (norm.contains("kategori") || norm.contains("category")) ->
                    categoryIdx = idx
                unitIdx == -1 && (norm.contains("satuan") || norm == "unit" || norm == "uom") ->
                    unitIdx = idx
                purchaseIdx == -1 && (norm.contains("modal") || norm.contains("hpp") || norm.contains("beli") || norm.contains("purchase") || norm.contains("cost")) ->
                    purchaseIdx = idx
                sellingIdx == -1 && (norm.contains("jual") || norm.contains("selling") || norm == "harga" || norm == "price") ->
                    sellingIdx = idx
                minStockIdx == -1 && ((norm.contains("stok") || norm.contains("stock")) && norm.contains("min")) ->
                    minStockIdx = idx
                stockIdx == -1 && (norm == "stok" || norm == "stock" || norm == "qty" || norm.contains("stok saat") || norm.contains("jumlah stok")) ->
                    stockIdx = idx
                typeIdx == -1 && (norm.contains("tipe") || norm.contains("type")) ->
                    typeIdx = idx
                brandIdx == -1 && (norm.contains("merek") || norm.contains("merk") || norm.contains("brand")) ->
                    brandIdx = idx
            }
        }
        return ProductColMap(
            nameIdx = nameIdx,
            barcodeIdx = barcodeIdx,
            categoryIdx = categoryIdx,
            unitIdx = unitIdx,
            purchaseIdx = purchaseIdx,
            sellingIdx = sellingIdx,
            stockIdx = stockIdx,
            minStockIdx = minStockIdx,
            typeIdx = typeIdx,
            brandIdx = brandIdx
        )
    }

    private fun normalizeHeader(raw: String): String =
        raw.trim().lowercase(Locale.US).replace('_', ' ').replace(Regex("\\s+"), " ")

    private fun cellAt(row: List<String>, index: Int): String =
        if (index in row.indices) row[index] else ""

    internal fun parseFlexibleNumber(raw: String): Double {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return 0.0
        val cleaned = trimmed
            .replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace(" ", "")
        cleaned.toDoubleOrNull()?.let { return it }

        // Handle Indonesian formatted thousands e.g. "12.500" or "1.250.000,50"
        val dotCount = cleaned.count { it == '.' }
        val commaCount = cleaned.count { it == ',' }
        val normalized = when {
            commaCount == 1 && dotCount >= 1 -> cleaned.replace(".", "").replace(",", ".")
            commaCount == 1 && dotCount == 0 -> {
                val after = cleaned.substringAfter(',')
                if (after.length == 3 && cleaned.substringBefore(',').length <= 3) cleaned.replace(",", "")
                else cleaned.replace(',', '.')
            }
            dotCount > 1 -> cleaned.replace(".", "")
            dotCount == 1 -> {
                val after = cleaned.substringAfter('.')
                if (after.length == 3 && cleaned.substringBefore('.').all { it.isDigit() || it == '-' }) {
                    // Could be 15000 written as 15.000 or 1.500 -> prefer standard double if decimal, wait: Excel XML raw numbers have dot decimals
                    cleaned
                } else cleaned
            }
            else -> cleaned
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    // =========================================================================
    // 4. PURE-KOTLIN OOXML (.xlsx) WRITER
    // =========================================================================

    private sealed class XlsxCell(val style: Int) {
        class Str(val value: String, style: Int = 0) : XlsxCell(style)
        class Num(val value: Double, style: Int = 2) : XlsxCell(style)
    }

    private fun cellStr(value: String, style: Int = 0): XlsxCell = XlsxCell.Str(value, style)
    private fun cellNum(value: Double, style: Int = 2): XlsxCell = XlsxCell.Num(value, style)

    private data class XlsxSheetSpec(
        val name: String,
        val rows: List<List<XlsxCell>>
    )

    private fun buildXlsxWorkbook(sheets: List<XlsxSheetSpec>): ByteArray {
        val safeSheets = sheets.mapIndexed { idx, s ->
            val sanitizedName = s.name.replace(Regex("[\\\\/*?:\\[\\]]"), "_").take(31).ifBlank { "Sheet${idx + 1}" }
            s.copy(name = sanitizedName)
        }

        val sharedStrings = mutableListOf<String>()
        val sharedStringIndex = mutableMapOf<String, Int>()
        fun internString(text: String): Int {
            return sharedStringIndex.getOrPut(text) {
                sharedStrings.add(text)
                sharedStrings.lastIndex
            }
        }

        safeSheets.forEach { sheet ->
            sheet.rows.forEach { row ->
                row.forEach { cell ->
                    if (cell is XlsxCell.Str) internString(cell.value)
                }
            }
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeZipString(zos, "[Content_Types].xml", buildContentTypesXml(safeSheets.size))
            writeZipString(zos, "_rels/.rels", ROOT_RELS_XML)
            writeZipString(zos, "xl/workbook.xml", buildWorkbookXml(safeSheets))
            writeZipString(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(safeSheets.size))
            writeZipString(zos, "xl/styles.xml", STYLES_XML)
            writeZipString(zos, "xl/sharedStrings.xml", buildSharedStringsXml(sharedStrings))
            safeSheets.forEachIndexed { idx, sheet ->
                writeZipString(
                    zos,
                    "xl/worksheets/sheet${idx + 1}.xml",
                    buildSheetXml(sheet.rows, sharedStringIndex)
                )
            }
        }
        return baos.toByteArray()
    }

    private fun writeZipString(zos: ZipOutputStream, entryName: String, content: String) {
        zos.putNextEntry(ZipEntry(entryName))
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""</Types>""")
    }

    private fun buildWorkbookXml(sheets: List<XlsxSheetSpec>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheets>""")
        sheets.forEachIndexed { idx, s ->
            append("""<sheet name="${escapeXml(s.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
        }
        append("""</sheets></workbook>""")
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""<Relationship Id="rId${sheetCount + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>""")
        append("""</Relationships>""")
    }

    private fun buildSharedStringsXml(strings: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
        strings.forEach { s ->
            append("""<si><t xml:space="preserve">${escapeXml(s)}</t></si>""")
        }
        append("""</sst>""")
    }

    private fun buildSheetXml(rows: List<List<XlsxCell>>, sharedStringIndex: Map<String, Int>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            append("""<row r="$rowNum">""")
            row.forEachIndexed { cIdx, cell ->
                val ref = "${colLetters(cIdx)}$rowNum"
                when (cell) {
                    is XlsxCell.Str -> {
                        val sIdx = sharedStringIndex[cell.value] ?: 0
                        append("""<c r="$ref" t="s" s="${cell.style}"><v>$sIdx</v></c>""")
                    }
                    is XlsxCell.Num -> {
                        val cleanNum = if (cell.value.isFinite()) {
                            if (cell.value % 1.0 == 0.0 && kotlin.math.abs(cell.value) < 9e15) {
                                cell.value.toLong().toString()
                            } else {
                                cell.value.toString()
                            }
                        } else "0"
                        append("""<c r="$ref" s="${cell.style}"><v>$cleanNum</v></c>""")
                    }
                }
            }
            append("""</row>""")
        }
        append("""</sheetData></worksheet>""")
    }

    private fun colLetters(colIndex: Int): String {
        var n = colIndex + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A' + rem))
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(s: String): String = buildString(s.length) {
        s.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') append(ch)
            }
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx) ?: "produk.xlsx"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "produk.xlsx"
    }

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    // Style 0 = normal text, Style 1 = bold header fill, Style 2 = standard number format
    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>"""
}
