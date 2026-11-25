package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.*
import com.example.firebase.FirebaseSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class KasLiteBackupInspectResult(
    val fileName: String,
    val appVersion: String,
    val createdAtMillis: Long,
    val storeName: String,
    val productCount: Int,
    val saleCount: Int,
    val purchaseCount: Int,
    val customerCount: Int,
    val supplierCount: Int,
    val cashAccountCount: Int,
    val expenseCount: Int,
    val incomeCount: Int,
    val userAccountCount: Int,
    val photoCount: Int,
    val totalRecords: Int,
    val extractedTempDir: File
)

data class KasLiteRestoreSummary(
    val restoredRecords: Int,
    val restoredPhotos: Int,
    val replaceMode: Boolean,
    val message: String
)

object KasLiteBackupManager {

    private const val FORMAT_ID = "KASLITE_BACKUP_V1"
    private val fileStampFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    private val allTableNamesOrderedForDelete = listOf(
        "barcode_scan_logs",
        "cloud_sync_mappings",
        "shift_cash_movements",
        "cash_mutations",
        "promos",
        "bundle_items",
        "bundle_packages",
        "product_batches",
        "wholesale_tiers",
        "product_variants",
        "cashier_devices",
        "printers",
        "menu_permissions",
        "user_accounts",
        "document_counters",
        "store_profiles",
        "pending_sales",
        "cashier_shifts",
        "product_tier_prices",
        "admin_tier_ranges",
        "admin_tier_services",
        "cash_transfers",
        "cash_accounts",
        "app_settings",
        "ai_command_logs",
        "debt_payments",
        "incomes",
        "expenses",
        "stock_movements",
        "purchase_items",
        "purchases",
        "sale_items",
        "sales",
        "suppliers",
        "customers",
        "product_aliases",
        "unit_conversions",
        "categories",
        "products"
    )

    // =========================================================================
    // 1. EXPORT FULL BACKUP (.kaslite ZIP)
    // =========================================================================

    suspend fun exportBackupZipToUri(
        context: Context,
        db: GeraiUnaDatabase,
        sessionStore: FirebaseSessionStore?,
        targetUri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val (zipBytes, totalRecords) = buildBackupZipBytes(context, db, sessionStore)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(zipBytes)
            } ?: throw IllegalStateException("Tidak dapat membuka file tujuan backup.")
            totalRecords
        }
    }

    suspend fun createShareableBackupZip(
        context: Context,
        db: GeraiUnaDatabase,
        sessionStore: FirebaseSessionStore?
    ): Result<Pair<Uri, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val (zipBytes, totalRecords) = buildBackupZipBytes(context, db, sessionStore)
            val backupsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val storeName = db.extendedPosDao().getStoreProfileSync()?.name.orEmpty()
                .ifBlank { "Toko" }
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(24)
            val outFile = File(backupsDir, "Backup_KASLITE_${storeName}_${fileStampFormatter.format(Date())}.kaslite")
            outFile.writeBytes(zipBytes)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            uri to totalRecords
        }
    }

    internal suspend fun buildBackupZipBytes(
        context: Context,
        db: GeraiUnaDatabase,
        sessionStore: FirebaseSessionStore?
    ): Pair<ByteArray, Int> {
        val productDao = db.productDao()
        val saleDao = db.saleDao()
        val purchaseDao = db.purchaseDao()
        val stockDao = db.stockMovementDao()
        val financeDao = db.financeDao()
        val contactDao = db.contactDao()
        val aiDao = db.aiDao()
        val settingsDao = db.appSettingsDao()
        val cashDao = db.cashAccountDao()
        val tierDao = db.adminTierDao()
        val shiftDao = db.shiftDao()
        val extDao = db.extendedPosDao()

        val products = productDao.getAllProducts().first()
        val categories = productDao.getAllCategories().first()
        val conversions = productDao.getAllConversions().first()
        val aliases = productDao.getAllAliases().first()
        val customers = contactDao.getAllCustomers().first()
        val suppliers = contactDao.getAllSuppliers().first()
        val sales = saleDao.getAllSales().first()
        val saleItems = saleDao.getAllSaleItems().first()
        val purchases = purchaseDao.getAllPurchases().first()
        val purchaseItems = purchaseDao.getAllPurchaseItems().first()
        val stockMovements = stockDao.getAllMovements().first()
        val expenses = financeDao.getAllExpenses().first()
        val incomes = financeDao.getAllIncomes().first()
        val debtPayments = financeDao.getAllDebtPayments().first()
        val aiLogs = aiDao.getAllLogs().first()
        val settings = settingsDao.getAllSettings().first()
        val cashAccounts = cashDao.getAllCashAccounts().first()
        val cashTransfers = cashDao.getAllTransfers().first()
        val adminServices = tierDao.getAllServices().first()
        val adminRanges = tierDao.getAllRanges().first()
        val tierPrices = productDao.getAllTierPrices().first()
        val shifts = shiftDao.getAllShifts().first()
        val pendingSales = saleDao.getAllPendingSales().first()
        val storeProfile = extDao.getStoreProfileSync()
        val counters = extDao.getAllCountersList()
        val users = extDao.getAllUsersList()
        val permissions = extDao.getAllMenuPermissions().first()
        val printers = extDao.getAllPrinters().first()
        val cashierDevices = extDao.getAllCashierDevices().first()
        val variants = extDao.getAllVariants().first()
        val wholesaleTiers = extDao.getAllWholesaleTiers().first()
        val batches = extDao.getAllBatches().first()
        val bundles = extDao.getAllBundles().first()
        val bundleItems = extDao.getAllBundleItems().first()
        val promos = extDao.getAllPromos().first()
        val cashMutations = extDao.getAllCashMutations().first()
        val shiftMovements = extDao.getAllShiftCashMovements().first()
        val cloudMappings = extDao.getAllCloudMappingsList()
        val scanLogs = productDao.getRecentBarcodeScans(Int.MAX_VALUE).first()

        // Collect referenced local photo files
        val photosToPack = linkedMapOf<String, File>()
        val productImagesDir = File(context.filesDir, "product_images")
        if (productImagesDir.exists() && productImagesDir.isDirectory) {
            productImagesDir.listFiles()?.forEach { file ->
                if (file.isFile) photosToPack[file.name] = file
            }
        }
        products.forEach { p ->
            resolveReadableLocalFile(p.imageUri)?.let { file ->
                photosToPack.putIfAbsent(file.name, file)
            }
        }
        storeProfile?.logoUri?.let { logo ->
            resolveReadableLocalFile(logo)?.let { file ->
                photosToPack.putIfAbsent("store_logo_${file.name}", file)
            }
        }

        val tablesJson = JSONObject().apply {
            put("products", JSONArray().apply { products.forEach { put(productToJson(it)) } })
            put("categories", JSONArray().apply { categories.forEach { put(categoryToJson(it)) } })
            put("unit_conversions", JSONArray().apply { conversions.forEach { put(unitConversionToJson(it)) } })
            put("product_aliases", JSONArray().apply { aliases.forEach { put(aliasToJson(it)) } })
            put("customers", JSONArray().apply { customers.forEach { put(customerToJson(it)) } })
            put("suppliers", JSONArray().apply { suppliers.forEach { put(supplierToJson(it)) } })
            put("sales", JSONArray().apply { sales.forEach { put(saleToJson(it)) } })
            put("sale_items", JSONArray().apply { saleItems.forEach { put(saleItemToJson(it)) } })
            put("purchases", JSONArray().apply { purchases.forEach { put(purchaseToJson(it)) } })
            put("purchase_items", JSONArray().apply { purchaseItems.forEach { put(purchaseItemToJson(it)) } })
            put("stock_movements", JSONArray().apply { stockMovements.forEach { put(stockMovementToJson(it)) } })
            put("expenses", JSONArray().apply { expenses.forEach { put(expenseToJson(it)) } })
            put("incomes", JSONArray().apply { incomes.forEach { put(incomeToJson(it)) } })
            put("debt_payments", JSONArray().apply { debtPayments.forEach { put(debtPaymentToJson(it)) } })
            put("ai_command_logs", JSONArray().apply { aiLogs.forEach { put(aiLogToJson(it)) } })
            put("app_settings", JSONArray().apply { settings.forEach { put(appSettingToJson(it)) } })
            put("cash_accounts", JSONArray().apply { cashAccounts.forEach { put(cashAccountToJson(it)) } })
            put("cash_transfers", JSONArray().apply { cashTransfers.forEach { put(cashTransferToJson(it)) } })
            put("admin_tier_services", JSONArray().apply { adminServices.forEach { put(adminServiceToJson(it)) } })
            put("admin_tier_ranges", JSONArray().apply { adminRanges.forEach { put(adminRangeToJson(it)) } })
            put("product_tier_prices", JSONArray().apply { tierPrices.forEach { put(tierPriceToJson(it)) } })
            put("cashier_shifts", JSONArray().apply { shifts.forEach { put(shiftToJson(it)) } })
            put("pending_sales", JSONArray().apply { pendingSales.forEach { put(pendingSaleToJson(it)) } })
            put("store_profiles", JSONArray().apply { storeProfile?.let { put(storeProfileToJson(it)) } })
            put("document_counters", JSONArray().apply { counters.forEach { put(counterToJson(it)) } })
            put("user_accounts", JSONArray().apply { users.forEach { put(userToJson(it)) } })
            put("menu_permissions", JSONArray().apply { permissions.forEach { put(permissionToJson(it)) } })
            put("printers", JSONArray().apply { printers.forEach { put(printerToJson(it)) } })
            put("cashier_devices", JSONArray().apply { cashierDevices.forEach { put(cashierDeviceToJson(it)) } })
            put("product_variants", JSONArray().apply { variants.forEach { put(variantToJson(it)) } })
            put("wholesale_tiers", JSONArray().apply { wholesaleTiers.forEach { put(wholesaleTierToJson(it)) } })
            put("product_batches", JSONArray().apply { batches.forEach { put(batchToJson(it)) } })
            put("bundle_packages", JSONArray().apply { bundles.forEach { put(bundleToJson(it)) } })
            put("bundle_items", JSONArray().apply { bundleItems.forEach { put(bundleItemToJson(it)) } })
            put("promos", JSONArray().apply { promos.forEach { put(promoToJson(it)) } })
            put("cash_mutations", JSONArray().apply { cashMutations.forEach { put(cashMutationToJson(it)) } })
            put("shift_cash_movements", JSONArray().apply { shiftMovements.forEach { put(shiftMovementToJson(it)) } })
            put("cloud_sync_mappings", JSONArray().apply { cloudMappings.forEach { put(cloudMappingToJson(it)) } })
            put("barcode_scan_logs", JSONArray().apply { scanLogs.forEach { put(scanLogToJson(it)) } })
        }

        var totalRecords = 0
        val keys = tablesJson.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            totalRecords += tablesJson.getJSONArray(k).length()
        }

        val manifestJson = JSONObject().apply {
            put("format", FORMAT_ID)
            put("appName", "KASLITE")
            put("appVersion", "1.2")
            put("dbVersion", 7)
            put("createdAtMillis", System.currentTimeMillis())
            put("storeName", storeProfile?.name ?: "Toko Saya")
            put("photoCount", photosToPack.size)
            put("totalRecords", totalRecords)
        }

        val prefsJson = JSONObject().apply {
            put("storeNameHint", sessionStore?.getStoreNameHint() ?: (storeProfile?.name ?: ""))
            put("lastEmail", sessionStore?.getLastLoginEmail() ?: "")
            put("deviceName", sessionStore?.getDeviceName() ?: "")
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            writeZipText(zos, "manifest.json", manifestJson.toString(2))
            writeZipText(zos, "database_backup.json", tablesJson.toString())
            writeZipText(zos, "app_preferences.json", prefsJson.toString(2))
            photosToPack.forEach { (archiveName, file) ->
                if (file.exists() && file.canRead()) {
                    zos.putNextEntry(ZipEntry("photos/$archiveName"))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return baos.toByteArray() to totalRecords
    }

    // =========================================================================
    // 2. INSPECT BACKUP (.kaslite ZIP)
    // =========================================================================

    suspend fun inspectBackupZip(
        context: Context,
        uri: Uri
    ): KasLiteBackupInspectResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "kaslite_restore_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val displayName = resolveDisplayName(context, uri)
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Tidak dapat membuka file backup yang dipilih.")

            var photoCount = 0
            input.use { stream ->
                ZipInputStream(stream).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        val safeName = entry.name.replace("\\", "/").removePrefix("/")
                        if (safeName.contains("..") || entry.isDirectory) {
                            zis.closeEntry()
                            continue
                        }
                        val tempRootCanonical = tempDir.canonicalPath.let {
                            if (it.endsWith(File.separator)) it else it + File.separator
                        }
                        val outFile = File(tempDir, safeName)
                        if (!outFile.canonicalPath.startsWith(tempRootCanonical)) {
                            zis.closeEntry()
                            continue
                        }
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out -> zis.copyTo(out) }
                        if (safeName.startsWith("photos/") && outFile.length() > 0L) {
                            photoCount++
                        }
                        zis.closeEntry()
                    }
                }
            }

            val dbFile = File(tempDir, "database_backup.json")
            if (!dbFile.exists()) {
                throw IllegalStateException("File backup bukan format .kaslite yang valid (database_backup.json tidak ditemukan).")
            }

            val manifestFile = File(tempDir, "manifest.json")
            val manifest = if (manifestFile.exists()) JSONObject(manifestFile.readText(StandardCharsets.UTF_8)) else JSONObject()
            val tables = JSONObject(dbFile.readText(StandardCharsets.UTF_8))

            fun arrLen(name: String): Int = tables.optJSONArray(name)?.length() ?: 0
            var total = 0
            val keyIt = tables.keys()
            while (keyIt.hasNext()) {
                total += tables.optJSONArray(keyIt.next())?.length() ?: 0
            }

            val storeProfiles = tables.optJSONArray("store_profiles")
            val inferredStoreName = manifest.optString("storeName").ifBlank {
                if (storeProfiles != null && storeProfiles.length() > 0) {
                    storeProfiles.getJSONObject(0).optString("name", "Toko Saya")
                } else "Toko Saya"
            }

            KasLiteBackupInspectResult(
                fileName = displayName,
                appVersion = manifest.optString("appVersion", "1.2"),
                createdAtMillis = manifest.optLong("createdAtMillis", 0L),
                storeName = inferredStoreName,
                productCount = arrLen("products"),
                saleCount = arrLen("sales"),
                purchaseCount = arrLen("purchases"),
                customerCount = arrLen("customers"),
                supplierCount = arrLen("suppliers"),
                cashAccountCount = arrLen("cash_accounts"),
                expenseCount = arrLen("expenses"),
                incomeCount = arrLen("incomes"),
                userAccountCount = arrLen("user_accounts"),
                photoCount = photoCount,
                totalRecords = total,
                extractedTempDir = tempDir
            )
        } catch (e: Throwable) {
            tempDir.deleteRecursively()
            throw e
        }
    }

    fun cleanupTemp(inspect: KasLiteBackupInspectResult?) {
        inspect?.extractedTempDir?.deleteRecursively()
    }

    // =========================================================================
    // 3. RESTORE BACKUP (.kaslite ZIP) -> REPLACE OR MERGE
    // =========================================================================

    suspend fun restoreBackupZip(
        context: Context,
        db: GeraiUnaDatabase,
        sessionStore: FirebaseSessionStore?,
        inspect: KasLiteBackupInspectResult,
        replaceExisting: Boolean
    ): KasLiteRestoreSummary = withContext(Dispatchers.IO) {
        val tempDir = inspect.extractedTempDir
        val dbFile = File(tempDir, "database_backup.json")
        if (!dbFile.exists()) {
            throw IllegalStateException("Data backup sementara sudah kedaluwarsa. Pilih file .kaslite kembali.")
        }
        val tables = JSONObject(dbFile.readText(StandardCharsets.UTF_8))

        // Step 1: Restore photos to filesDir/product_images and build filename -> new path map
        val targetImagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
        val photoMap = mutableMapOf<String, String>()
        var restoredPhotos = 0
        val photosDir = File(tempDir, "photos")
        if (photosDir.exists() && photosDir.isDirectory) {
            photosDir.listFiles()?.forEach { src ->
                if (src.isFile && src.length() > 0L) {
                    val dst = File(targetImagesDir, src.name)
                    src.copyTo(dst, overwrite = true)
                    photoMap[src.name] = dst.absolutePath
                    restoredPhotos++
                }
            }
        }

        // Step 2: Atomic Room restoration
        var restoredRecords = 0
        db.withTransaction {
            val productDao = db.productDao()
            val saleDao = db.saleDao()
            val purchaseDao = db.purchaseDao()
            val stockDao = db.stockMovementDao()
            val financeDao = db.financeDao()
            val contactDao = db.contactDao()
            val aiDao = db.aiDao()
            val settingsDao = db.appSettingsDao()
            val cashDao = db.cashAccountDao()
            val tierDao = db.adminTierDao()
            val shiftDao = db.shiftDao()
            val extDao = db.extendedPosDao()

            val existingUsersBeforeClear = extDao.getAllUsersList()
            val existingPermsBeforeClear = extDao.getAllMenuPermissions().first()

            if (replaceExisting) {
                val sqliteDb = db.openHelper.writableDatabase
                allTableNamesOrderedForDelete.forEach { table ->
                    sqliteDb.execSQL("DELETE FROM $table")
                }

                // Exact-ID full restoration across all 39 tables
                restoredRecords += restoreJsonArray(tables, "categories") { o ->
                    productDao.insertCategory(jsonToCategory(o))
                }
                restoredRecords += restoreJsonArray(tables, "products") { o ->
                    productDao.insertProduct(jsonToProduct(o, photoMap))
                }
                restoredRecords += restoreJsonArray(tables, "unit_conversions") { o ->
                    productDao.insertConversion(jsonToUnitConversion(o))
                }
                restoredRecords += restoreJsonArray(tables, "product_aliases") { o ->
                    productDao.insertAlias(jsonToAlias(o))
                }
                restoredRecords += restoreJsonArray(tables, "customers") { o ->
                    contactDao.insertCustomer(jsonToCustomer(o))
                }
                restoredRecords += restoreJsonArray(tables, "suppliers") { o ->
                    contactDao.insertSupplier(jsonToSupplier(o))
                }
                restoredRecords += restoreJsonArray(tables, "sales") { o ->
                    saleDao.insertSale(jsonToSale(o))
                }
                val saleItems = parseJsonList(tables, "sale_items") { jsonToSaleItem(it) }
                if (saleItems.isNotEmpty()) {
                    saleDao.insertSaleItems(saleItems)
                    restoredRecords += saleItems.size
                }
                restoredRecords += restoreJsonArray(tables, "purchases") { o ->
                    purchaseDao.insertPurchase(jsonToPurchase(o))
                }
                val purchaseItems = parseJsonList(tables, "purchase_items") { jsonToPurchaseItem(it) }
                if (purchaseItems.isNotEmpty()) {
                    purchaseDao.insertPurchaseItems(purchaseItems)
                    restoredRecords += purchaseItems.size
                }
                restoredRecords += restoreJsonArray(tables, "stock_movements") { o ->
                    stockDao.insertMovement(jsonToStockMovement(o))
                }
                restoredRecords += restoreJsonArray(tables, "expenses") { o ->
                    financeDao.insertExpense(jsonToExpense(o))
                }
                restoredRecords += restoreJsonArray(tables, "incomes") { o ->
                    financeDao.insertIncome(jsonToIncome(o))
                }
                restoredRecords += restoreJsonArray(tables, "debt_payments") { o ->
                    financeDao.insertDebtPayment(jsonToDebtPayment(o))
                }
                restoredRecords += restoreJsonArray(tables, "ai_command_logs") { o ->
                    aiDao.insertLog(jsonToAiLog(o))
                }
                restoredRecords += restoreJsonArray(tables, "app_settings") { o ->
                    settingsDao.setSetting(jsonToAppSetting(o))
                }
                restoredRecords += restoreJsonArray(tables, "cash_accounts") { o ->
                    cashDao.insertCashAccount(jsonToCashAccount(o))
                }
                restoredRecords += restoreJsonArray(tables, "cash_transfers") { o ->
                    cashDao.insertTransfer(jsonToCashTransfer(o))
                }
                restoredRecords += restoreJsonArray(tables, "admin_tier_services") { o ->
                    tierDao.insertService(jsonToAdminService(o))
                }
                restoredRecords += restoreJsonArray(tables, "admin_tier_ranges") { o ->
                    tierDao.insertRange(jsonToAdminRange(o))
                }
                restoredRecords += restoreJsonArray(tables, "product_tier_prices") { o ->
                    productDao.insertTierPrice(jsonToTierPrice(o))
                }
                restoredRecords += restoreJsonArray(tables, "cashier_shifts") { o ->
                    shiftDao.insertShift(jsonToShift(o))
                }
                restoredRecords += restoreJsonArray(tables, "pending_sales") { o ->
                    saleDao.insertPendingSale(jsonToPendingSale(o))
                }
                restoredRecords += restoreJsonArray(tables, "store_profiles") { o ->
                    extDao.upsertStoreProfile(jsonToStoreProfile(o, photoMap))
                }
                restoredRecords += restoreJsonArray(tables, "document_counters") { o ->
                    extDao.upsertCounter(jsonToCounter(o))
                }

                val backupUsersCount = restoreJsonArray(tables, "user_accounts") { o ->
                    extDao.insertUser(jsonToUser(o))
                }
                restoredRecords += backupUsersCount
                val backupPermsCount = restoreJsonArray(tables, "menu_permissions") { o ->
                    extDao.insertMenuPermission(jsonToPermission(o))
                }
                restoredRecords += backupPermsCount

                // Safeguard: never strand current device with 0 users after restore
                if (backupUsersCount == 0 && existingUsersBeforeClear.isNotEmpty()) {
                    existingUsersBeforeClear.forEach { extDao.insertUser(it) }
                    existingPermsBeforeClear.forEach { extDao.insertMenuPermission(it) }
                }

                restoredRecords += restoreJsonArray(tables, "printers") { o ->
                    extDao.insertPrinter(jsonToPrinter(o))
                }
                restoredRecords += restoreJsonArray(tables, "cashier_devices") { o ->
                    extDao.insertCashierDevice(jsonToCashierDevice(o))
                }
                restoredRecords += restoreJsonArray(tables, "product_variants") { o ->
                    extDao.insertVariant(jsonToVariant(o))
                }
                restoredRecords += restoreJsonArray(tables, "wholesale_tiers") { o ->
                    extDao.insertWholesaleTier(jsonToWholesaleTier(o))
                }
                restoredRecords += restoreJsonArray(tables, "product_batches") { o ->
                    extDao.insertBatch(jsonToBatch(o))
                }
                restoredRecords += restoreJsonArray(tables, "bundle_packages") { o ->
                    extDao.insertBundle(jsonToBundle(o))
                }
                restoredRecords += restoreJsonArray(tables, "bundle_items") { o ->
                    extDao.insertBundleItem(jsonToBundleItem(o))
                }
                restoredRecords += restoreJsonArray(tables, "promos") { o ->
                    extDao.insertPromo(jsonToPromo(o))
                }
                restoredRecords += restoreJsonArray(tables, "cash_mutations") { o ->
                    extDao.insertCashMutation(jsonToCashMutation(o))
                }
                restoredRecords += restoreJsonArray(tables, "shift_cash_movements") { o ->
                    extDao.insertShiftCashMovement(jsonToShiftMovement(o))
                }
                restoredRecords += restoreJsonArray(tables, "cloud_sync_mappings") { o ->
                    extDao.upsertCloudMapping(jsonToCloudMapping(o))
                }
                restoredRecords += restoreJsonArray(tables, "barcode_scan_logs") { o ->
                    productDao.recordBarcodeScan(jsonToScanLog(o))
                }
            } else {
                // Safe non-destructive MERGE mode: remap IDs to avoid collisions
                val existingCats = productDao.getAllCategoriesList()
                    .map { it.name.trim().lowercase(Locale.US) }
                    .toMutableSet()
                parseJsonList(tables, "categories") { jsonToCategory(it) }.forEach { cat ->
                    val k = cat.name.trim().lowercase(Locale.US)
                    if (k.isNotBlank() && existingCats.add(k)) {
                        productDao.insertCategory(cat.copy(id = 0))
                        restoredRecords++
                    }
                }

                val existingProds = productDao.getAllProductsList()
                val prodByBarcode = existingProds.filter { it.barcode.isNotBlank() }
                    .associateBy { it.barcode.trim().lowercase(Locale.US) }.toMutableMap()
                val prodByName = existingProds.associateBy { it.name.trim().lowercase(Locale.US) }.toMutableMap()
                val productIdMap = mutableMapOf<Long, Long>()

                parseJsonList(tables, "products") { jsonToProduct(it, photoMap) }.forEach { p ->
                    val bKey = p.barcode.trim().lowercase(Locale.US)
                    val nKey = p.name.trim().lowercase(Locale.US)
                    val match = (if (bKey.isNotBlank()) prodByBarcode[bKey] else null) ?: prodByName[nKey]
                    if (match != null) {
                        productIdMap[p.id] = match.id
                    } else {
                        val newId = productDao.insertProduct(p.copy(id = 0))
                        productIdMap[p.id] = newId
                        val inserted = p.copy(id = newId)
                        if (bKey.isNotBlank()) prodByBarcode[bKey] = inserted
                        prodByName[nKey] = inserted
                        restoredRecords++
                    }
                }

                parseJsonList(tables, "product_variants") { jsonToVariant(it) }.forEach { v ->
                    val targetPid = productIdMap[v.productId] ?: return@forEach
                    val existingVs = extDao.getVariantsForProduct(targetPid)
                    if (existingVs.none { it.name.equals(v.name, ignoreCase = true) }) {
                        extDao.insertVariant(v.copy(id = 0, productId = targetPid))
                        restoredRecords++
                    }
                }

                parseJsonList(tables, "unit_conversions") { jsonToUnitConversion(it) }.forEach { u ->
                    val targetPid = productIdMap[u.productId] ?: return@forEach
                    val existingU = productDao.getConversionsList(targetPid)
                    if (existingU.none { it.unitName.equals(u.unitName, ignoreCase = true) }) {
                        productDao.insertConversion(u.copy(id = 0, productId = targetPid))
                        restoredRecords++
                    }
                }

                val existingCusts = contactDao.getAllCustomers().first()
                val custByName = existingCusts.associateBy { it.name.trim().lowercase(Locale.US) }.toMutableMap()
                val customerIdMap = mutableMapOf<Long, Long>()
                parseJsonList(tables, "customers") { jsonToCustomer(it) }.forEach { c ->
                    val key = c.name.trim().lowercase(Locale.US)
                    val match = custByName[key]
                    if (match != null) {
                        customerIdMap[c.id] = match.id
                    } else {
                        val newId = contactDao.insertCustomer(c.copy(id = 0))
                        customerIdMap[c.id] = newId
                        custByName[key] = c.copy(id = newId)
                        restoredRecords++
                    }
                }

                val existingSups = contactDao.getAllSuppliers().first()
                val supByName = existingSups.associateBy { it.name.trim().lowercase(Locale.US) }.toMutableMap()
                val supplierIdMap = mutableMapOf<Long, Long>()
                parseJsonList(tables, "suppliers") { jsonToSupplier(it) }.forEach { sup ->
                    val key = sup.name.trim().lowercase(Locale.US)
                    val match = supByName[key]
                    if (match != null) {
                        supplierIdMap[sup.id] = match.id
                    } else {
                        val newId = contactDao.insertSupplier(sup.copy(id = 0))
                        supplierIdMap[sup.id] = newId
                        supByName[key] = sup.copy(id = newId)
                        restoredRecords++
                    }
                }

                val existingCash = cashDao.getAllAccountsList()
                val cashByName = existingCash.associateBy { it.accountName.trim().lowercase(Locale.US) }.toMutableMap()
                val cashIdMap = mutableMapOf<Long, Long>()
                parseJsonList(tables, "cash_accounts") { jsonToCashAccount(it) }.forEach { acc ->
                    val key = acc.accountName.trim().lowercase(Locale.US)
                    val match = cashByName[key]
                    if (match != null) {
                        cashIdMap[acc.id] = match.id
                    } else {
                        val newId = cashDao.insertCashAccount(acc.copy(id = 0, isDefault = false))
                        cashIdMap[acc.id] = newId
                        cashByName[key] = acc.copy(id = newId)
                        restoredRecords++
                    }
                }

                val existingInvoices = saleDao.getAllSales().first().map { it.invoiceNumber }.toMutableSet()
                val allBackupSaleItems = parseJsonList(tables, "sale_items") { jsonToSaleItem(it) }
                    .groupBy { it.saleId }
                parseJsonList(tables, "sales") { jsonToSale(it) }.forEach { s ->
                    if (existingInvoices.add(s.invoiceNumber)) {
                        val newSaleId = saleDao.insertSale(
                            s.copy(
                                id = 0,
                                customerId = s.customerId?.let { customerIdMap[it] },
                                cashAccountId = s.cashAccountId?.let { cashIdMap[it] }
                            )
                        )
                        restoredRecords++
                        val itemsForSale = allBackupSaleItems[s.id].orEmpty().map { item ->
                            item.copy(
                                id = 0,
                                saleId = newSaleId,
                                productId = productIdMap[item.productId] ?: item.productId
                            )
                        }
                        if (itemsForSale.isNotEmpty()) {
                            saleDao.insertSaleItems(itemsForSale)
                            restoredRecords += itemsForSale.size
                        }
                    }
                }

                val existingPurchaseInvs = purchaseDao.getAllPurchases().first().map { it.invoiceNumber }.toMutableSet()
                val allBackupPurchaseItems = parseJsonList(tables, "purchase_items") { jsonToPurchaseItem(it) }
                    .groupBy { it.purchaseId }
                parseJsonList(tables, "purchases") { jsonToPurchase(it) }.forEach { pur ->
                    if (existingPurchaseInvs.add(pur.invoiceNumber)) {
                        val newPid = purchaseDao.insertPurchase(
                            pur.copy(id = 0, supplierId = pur.supplierId?.let { supplierIdMap[it] })
                        )
                        restoredRecords++
                        val itemsForPur = allBackupPurchaseItems[pur.id].orEmpty().map { item ->
                            item.copy(
                                id = 0,
                                purchaseId = newPid,
                                productId = productIdMap[item.productId] ?: item.productId
                            )
                        }
                        if (itemsForPur.isNotEmpty()) {
                            purchaseDao.insertPurchaseItems(itemsForPur)
                            restoredRecords += itemsForPur.size
                        }
                    }
                }
            }
        }

        // Restore store name hint in local preferences if available
        val prefsFile = File(tempDir, "app_preferences.json")
        if (replaceExisting && prefsFile.exists()) {
            runCatching {
                val prefs = JSONObject(prefsFile.readText(StandardCharsets.UTF_8))
                val hint = prefs.optString("storeNameHint")
                if (hint.isNotBlank()) sessionStore?.setStoreNameHint(hint)
            }
        }

        KasLiteRestoreSummary(
            restoredRecords = restoredRecords,
            restoredPhotos = restoredPhotos,
            replaceMode = replaceExisting,
            message = "Restore berhasil (${if (replaceExisting) "Pulihkan Penuh" else "Gabung Data"}): $restoredRecords rekaman & $restoredPhotos foto dipulihkan."
        )
    }

    // =========================================================================
    // HELPER SERIALIZATION & UTILITIES
    // =========================================================================

    private inline fun <T> parseJsonList(
        root: JSONObject,
        key: String,
        mapper: (JSONObject) -> T
    ): List<T> {
        val arr = root.optJSONArray(key) ?: return emptyList()
        val list = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            list.add(mapper(arr.getJSONObject(i)))
        }
        return list
    }

    private suspend inline fun restoreJsonArray(
        root: JSONObject,
        key: String,
        crossinline block: suspend (JSONObject) -> Unit
    ): Int {
        val arr = root.optJSONArray(key) ?: return 0
        for (i in 0 until arr.length()) {
            block(arr.getJSONObject(i))
        }
        return arr.length()
    }

    private fun resolveReadableLocalFile(uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        val path = uriString.removePrefix("file://")
        val f = File(path)
        return if (f.exists() && f.isFile && f.canRead()) f else null
    }

    private fun remapPhotoUri(originalUri: String, photoMap: Map<String, String>, logoPrefix: Boolean = false): String {
        if (originalUri.isBlank()) return ""
        val base = originalUri.substringAfterLast('/')
        if (logoPrefix) {
            photoMap["store_logo_$base"]?.let { return it }
        }
        photoMap[base]?.let { return it }
        return originalUri
    }

    private fun writeZipText(zos: ZipOutputStream, entryName: String, content: String) {
        zos.putNextEntry(ZipEntry(entryName))
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx) ?: "backup.kaslite"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "backup.kaslite"
    }

    // --- Entity <-> JSON Mappers (all 39 Room entities) ---

    private fun JSONObject.optLongOrNull(k: String): Long? = if (has(k) && !isNull(k)) optLong(k) else null
    private fun JSONObject.optDoubleOrNull(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k) else null
    private fun JSONObject.optStringOrNull(k: String): String? = if (has(k) && !isNull(k)) optString(k) else null

    private fun productToJson(e: ProductEntity) = JSONObject().apply {
        put("id", e.id); put("barcode", e.barcode); put("name", e.name); put("category", e.category)
        put("baseUnit", e.baseUnit); put("purchasePrice", e.purchasePrice); put("sellingPrice", e.sellingPrice)
        put("stock", e.stock); put("minStock", e.minStock); put("brand", e.brand); put("isActive", e.isActive)
        put("productType", e.productType); put("isUnlimitedStock", e.isUnlimitedStock); put("isFlexiblePrice", e.isFlexiblePrice)
        put("hasNoStock", e.hasNoStock); put("allowDecimalQty", e.allowDecimalQty); put("serialOrImei", e.serialOrImei)
        put("expiryDate", e.expiryDate); put("createdAt", e.createdAt); put("autoBarcode", e.autoBarcode)
        put("specialPrice", e.specialPrice); put("strikePrice", e.strikePrice); put("hasVariants", e.hasVariants)
        put("adminTierGroupId", e.adminTierGroupId); put("serviceCashAccountId", e.serviceCashAccountId)
        put("serviceDirection", e.serviceDirection); put("imageUri", e.imageUri)
    }

    private fun jsonToProduct(o: JSONObject, photoMap: Map<String, String>) = ProductEntity(
        id = o.optLong("id", 0L), barcode = o.optString("barcode", ""), name = o.optString("name", ""),
        category = o.optString("category", "Umum"), baseUnit = o.optString("baseUnit", "pcs"),
        purchasePrice = o.optDouble("purchasePrice", 0.0), sellingPrice = o.optDouble("sellingPrice", 0.0),
        stock = o.optDouble("stock", 0.0), minStock = o.optDouble("minStock", 5.0),
        brand = o.optString("brand", ""), isActive = o.optBoolean("isActive", true),
        productType = o.optString("productType", "FISIK"), isUnlimitedStock = o.optBoolean("isUnlimitedStock", false),
        isFlexiblePrice = o.optBoolean("isFlexiblePrice", false), hasNoStock = o.optBoolean("hasNoStock", false),
        allowDecimalQty = o.optBoolean("allowDecimalQty", false), serialOrImei = o.optString("serialOrImei", ""),
        expiryDate = o.optLongOrNull("expiryDate"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        autoBarcode = o.optBoolean("autoBarcode", false), specialPrice = o.optDoubleOrNull("specialPrice"),
        strikePrice = o.optDoubleOrNull("strikePrice"), hasVariants = o.optBoolean("hasVariants", false),
        adminTierGroupId = o.optLongOrNull("adminTierGroupId"), serviceCashAccountId = o.optLongOrNull("serviceCashAccountId"),
        serviceDirection = o.optString("serviceDirection", "SETOR"),
        imageUri = remapPhotoUri(o.optString("imageUri", ""), photoMap)
    )

    private fun categoryToJson(e: CategoryEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("icon", e.icon)
    }

    private fun jsonToCategory(o: JSONObject) = CategoryEntity(
        id = o.optLong("id", 0L), name = o.optString("name", "Umum"), icon = o.optString("icon", "tag")
    )

    private fun unitConversionToJson(e: UnitConversionEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("unitName", e.unitName); put("multiplier", e.multiplier)
        put("customSellingPrice", e.customSellingPrice); put("customPurchasePrice", e.customPurchasePrice)
    }

    private fun jsonToUnitConversion(o: JSONObject) = UnitConversionEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), unitName = o.optString("unitName", "pcs"),
        multiplier = o.optDouble("multiplier", 1.0), customSellingPrice = o.optDoubleOrNull("customSellingPrice"),
        customPurchasePrice = o.optDoubleOrNull("customPurchasePrice")
    )

    private fun aliasToJson(e: ProductAliasEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("alias", e.alias)
    }

    private fun jsonToAlias(o: JSONObject) = ProductAliasEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), alias = o.optString("alias", "")
    )

    private fun customerToJson(e: CustomerEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("phone", e.phone); put("address", e.address)
        put("notes", e.notes); put("totalReceivable", e.totalReceivable); put("depositBalance", e.depositBalance)
        put("loyaltyPoints", e.loyaltyPoints)
    }

    private fun jsonToCustomer(o: JSONObject) = CustomerEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), phone = o.optString("phone", ""),
        address = o.optString("address", ""), notes = o.optString("notes", ""),
        totalReceivable = o.optDouble("totalReceivable", 0.0), depositBalance = o.optDouble("depositBalance", 0.0),
        loyaltyPoints = o.optInt("loyaltyPoints", 0)
    )

    private fun supplierToJson(e: SupplierEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("phone", e.phone); put("address", e.address)
        put("notes", e.notes); put("totalDebt", e.totalDebt)
    }

    private fun jsonToSupplier(o: JSONObject) = SupplierEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), phone = o.optString("phone", ""),
        address = o.optString("address", ""), notes = o.optString("notes", ""), totalDebt = o.optDouble("totalDebt", 0.0)
    )

    private fun saleToJson(e: SaleEntity) = JSONObject().apply {
        put("id", e.id); put("invoiceNumber", e.invoiceNumber); put("date", e.date); put("cashierName", e.cashierName)
        put("customerId", e.customerId); put("customerName", e.customerName); put("subtotal", e.subtotal)
        put("discount", e.discount); put("tax", e.tax); put("finalAmount", e.finalAmount)
        put("paymentMethod", e.paymentMethod); put("amountPaid", e.amountPaid); put("changeAmount", e.changeAmount)
        put("status", e.status); put("notes", e.notes); put("storeAdminFee", e.storeAdminFee)
        put("providerAdminFee", e.providerAdminFee); put("agentCommission", e.agentCommission)
        put("cashAccountId", e.cashAccountId); put("splitCashAmount", e.splitCashAmount)
        put("splitNonCashAmount", e.splitNonCashAmount); put("splitPaymentMethod", e.splitPaymentMethod)
        put("shippingCost", e.shippingCost); put("additionalFee", e.additionalFee); put("promoDiscount", e.promoDiscount)
        put("appliedPromoName", e.appliedPromoName); put("depositUsed", e.depositUsed); put("pointsEarned", e.pointsEarned)
    }

    private fun jsonToSale(o: JSONObject) = SaleEntity(
        id = o.optLong("id", 0L), invoiceNumber = o.optString("invoiceNumber", ""),
        date = o.optLong("date", System.currentTimeMillis()), cashierName = o.optString("cashierName", "Kasir"),
        customerId = o.optLongOrNull("customerId"), customerName = o.optString("customerName", "Pelanggan Umum"),
        subtotal = o.optDouble("subtotal", 0.0), discount = o.optDouble("discount", 0.0),
        tax = o.optDouble("tax", 0.0), finalAmount = o.optDouble("finalAmount", 0.0),
        paymentMethod = o.optString("paymentMethod", "TUNAI"), amountPaid = o.optDouble("amountPaid", 0.0),
        changeAmount = o.optDouble("changeAmount", 0.0), status = o.optString("status", "COMPLETED"),
        notes = o.optString("notes", ""), storeAdminFee = o.optDouble("storeAdminFee", 0.0),
        providerAdminFee = o.optDouble("providerAdminFee", 0.0), agentCommission = o.optDouble("agentCommission", 0.0),
        cashAccountId = o.optLongOrNull("cashAccountId"), splitCashAmount = o.optDouble("splitCashAmount", 0.0),
        splitNonCashAmount = o.optDouble("splitNonCashAmount", 0.0),
        splitPaymentMethod = o.optString("splitPaymentMethod", ""), shippingCost = o.optDouble("shippingCost", 0.0),
        additionalFee = o.optDouble("additionalFee", 0.0), promoDiscount = o.optDouble("promoDiscount", 0.0),
        appliedPromoName = o.optString("appliedPromoName", ""), depositUsed = o.optDouble("depositUsed", 0.0),
        pointsEarned = o.optInt("pointsEarned", 0)
    )

    private fun saleItemToJson(e: SaleItemEntity) = JSONObject().apply {
        put("id", e.id); put("saleId", e.saleId); put("productId", e.productId); put("productName", e.productName)
        put("quantity", e.quantity); put("unit", e.unit); put("unitPrice", e.unitPrice); put("subtotal", e.subtotal)
        put("purchasePriceSnapshot", e.purchasePriceSnapshot); put("variantId", e.variantId)
        put("variantName", e.variantName); put("priceSource", e.priceSource); put("serviceNominal", e.serviceNominal)
        put("storeAdminFee", e.storeAdminFee); put("providerAdminFee", e.providerAdminFee)
        put("agentCommission", e.agentCommission); put("adminModifiedManually", e.adminModifiedManually)
    }

    private fun jsonToSaleItem(o: JSONObject) = SaleItemEntity(
        id = o.optLong("id", 0L), saleId = o.optLong("saleId", 0L), productId = o.optLong("productId", 0L),
        productName = o.optString("productName", ""), quantity = o.optDouble("quantity", 1.0),
        unit = o.optString("unit", "pcs"), unitPrice = o.optDouble("unitPrice", 0.0),
        subtotal = o.optDouble("subtotal", 0.0), purchasePriceSnapshot = o.optDouble("purchasePriceSnapshot", 0.0),
        variantId = o.optLongOrNull("variantId"), variantName = o.optString("variantName", ""),
        priceSource = o.optString("priceSource", "tetap"), serviceNominal = o.optDoubleOrNull("serviceNominal"),
        storeAdminFee = o.optDouble("storeAdminFee", 0.0), providerAdminFee = o.optDouble("providerAdminFee", 0.0),
        agentCommission = o.optDouble("agentCommission", 0.0),
        adminModifiedManually = o.optBoolean("adminModifiedManually", false)
    )

    private fun purchaseToJson(e: PurchaseEntity) = JSONObject().apply {
        put("id", e.id); put("invoiceNumber", e.invoiceNumber); put("date", e.date); put("supplierId", e.supplierId)
        put("supplierName", e.supplierName); put("subtotal", e.subtotal); put("discount", e.discount)
        put("additionalCost", e.additionalCost); put("finalAmount", e.finalAmount); put("paymentMethod", e.paymentMethod)
        put("paymentStatus", e.paymentStatus); put("status", e.status); put("notes", e.notes)
        put("receivedDate", e.receivedDate)
    }

    private fun jsonToPurchase(o: JSONObject) = PurchaseEntity(
        id = o.optLong("id", 0L), invoiceNumber = o.optString("invoiceNumber", ""),
        date = o.optLong("date", System.currentTimeMillis()), supplierId = o.optLongOrNull("supplierId"),
        supplierName = o.optString("supplierName", "Pemasok Umum"), subtotal = o.optDouble("subtotal", 0.0),
        discount = o.optDouble("discount", 0.0), additionalCost = o.optDouble("additionalCost", 0.0),
        finalAmount = o.optDouble("finalAmount", 0.0), paymentMethod = o.optString("paymentMethod", "TUNAI"),
        paymentStatus = o.optString("paymentStatus", "LUNAS"), status = o.optString("status", "DRAFT"),
        notes = o.optString("notes", ""), receivedDate = o.optLongOrNull("receivedDate")
    )

    private fun purchaseItemToJson(e: PurchaseItemEntity) = JSONObject().apply {
        put("id", e.id); put("purchaseId", e.purchaseId); put("productId", e.productId)
        put("productName", e.productName); put("quantity", e.quantity); put("unit", e.unit)
        put("purchasePrice", e.purchasePrice); put("subtotal", e.subtotal)
    }

    private fun jsonToPurchaseItem(o: JSONObject) = PurchaseItemEntity(
        id = o.optLong("id", 0L), purchaseId = o.optLong("purchaseId", 0L), productId = o.optLong("productId", 0L),
        productName = o.optString("productName", ""), quantity = o.optDouble("quantity", 0.0),
        unit = o.optString("unit", "pcs"), purchasePrice = o.optDouble("purchasePrice", 0.0),
        subtotal = o.optDouble("subtotal", 0.0)
    )

    private fun stockMovementToJson(e: StockMovementEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("productName", e.productName); put("date", e.date)
        put("movementType", e.movementType); put("quantityChange", e.quantityChange); put("unit", e.unit)
        put("previousStock", e.previousStock); put("currentStock", e.currentStock)
        put("referenceNumber", e.referenceNumber); put("notes", e.notes); put("loggedBy", e.loggedBy)
    }

    private fun jsonToStockMovement(o: JSONObject) = StockMovementEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), productName = o.optString("productName", ""),
        date = o.optLong("date", System.currentTimeMillis()), movementType = o.optString("movementType", ""),
        quantityChange = o.optDouble("quantityChange", 0.0), unit = o.optString("unit", "pcs"),
        previousStock = o.optDouble("previousStock", 0.0), currentStock = o.optDouble("currentStock", 0.0),
        referenceNumber = o.optString("referenceNumber", ""), notes = o.optString("notes", ""),
        loggedBy = o.optString("loggedBy", "Admin")
    )

    private fun expenseToJson(e: ExpenseEntity) = JSONObject().apply {
        put("id", e.id); put("category", e.category); put("amount", e.amount); put("date", e.date)
        put("notes", e.notes); put("paymentSource", e.paymentSource); put("recordedBy", e.recordedBy)
    }

    private fun jsonToExpense(o: JSONObject) = ExpenseEntity(
        id = o.optLong("id", 0L), category = o.optString("category", "Operasional"), amount = o.optDouble("amount", 0.0),
        date = o.optLong("date", System.currentTimeMillis()), notes = o.optString("notes", ""),
        paymentSource = o.optString("paymentSource", "Kas Toko"), recordedBy = o.optString("recordedBy", "Admin")
    )

    private fun incomeToJson(e: IncomeEntity) = JSONObject().apply {
        put("id", e.id); put("source", e.source); put("amount", e.amount); put("date", e.date)
        put("notes", e.notes); put("recordedBy", e.recordedBy)
    }

    private fun jsonToIncome(o: JSONObject) = IncomeEntity(
        id = o.optLong("id", 0L), source = o.optString("source", "Lainnya"), amount = o.optDouble("amount", 0.0),
        date = o.optLong("date", System.currentTimeMillis()), notes = o.optString("notes", ""),
        recordedBy = o.optString("recordedBy", "Admin")
    )

    private fun debtPaymentToJson(e: DebtPaymentEntity) = JSONObject().apply {
        put("id", e.id); put("type", e.type); put("referenceInvoice", e.referenceInvoice); put("partyId", e.partyId)
        put("partyName", e.partyName); put("amount", e.amount); put("date", e.date)
        put("paymentMethod", e.paymentMethod); put("notes", e.notes); put("recordedBy", e.recordedBy)
    }

    private fun jsonToDebtPayment(o: JSONObject) = DebtPaymentEntity(
        id = o.optLong("id", 0L), type = o.optString("type", "PIUTANG_PELANGGAN"),
        referenceInvoice = o.optString("referenceInvoice", ""), partyId = o.optLongOrNull("partyId"),
        partyName = o.optString("partyName", ""), amount = o.optDouble("amount", 0.0),
        date = o.optLong("date", System.currentTimeMillis()), paymentMethod = o.optString("paymentMethod", "TUNAI"),
        notes = o.optString("notes", ""), recordedBy = o.optString("recordedBy", "Admin")
    )

    private fun aiLogToJson(e: AICommandLogEntity) = JSONObject().apply {
        put("id", e.id); put("source", e.source); put("senderPhone", e.senderPhone); put("rawMessage", e.rawMessage)
        put("parsedIntent", e.parsedIntent); put("structuredJson", e.structuredJson); put("status", e.status)
        put("responseText", e.responseText); put("timestamp", e.timestamp)
    }

    private fun jsonToAiLog(o: JSONObject) = AICommandLogEntity(
        id = o.optLong("id", 0L), source = o.optString("source", "APP_CHAT"),
        senderPhone = o.optString("senderPhone", ""), rawMessage = o.optString("rawMessage", ""),
        parsedIntent = o.optString("parsedIntent", ""), structuredJson = o.optString("structuredJson", ""),
        status = o.optString("status", "EXECUTED"), responseText = o.optString("responseText", ""),
        timestamp = o.optLong("timestamp", System.currentTimeMillis())
    )

    private fun appSettingToJson(e: AppSettingsEntity) = JSONObject().apply {
        put("settingKey", e.settingKey); put("settingValue", e.settingValue)
    }

    private fun jsonToAppSetting(o: JSONObject) = AppSettingsEntity(
        settingKey = o.optString("settingKey", ""), settingValue = o.optString("settingValue", "")
    )

    private fun cashAccountToJson(e: CashAccountEntity) = JSONObject().apply {
        put("id", e.id); put("accountName", e.accountName); put("balance", e.balance)
        put("accountType", e.accountType); put("isActive", e.isActive); put("color", e.color)
        put("notes", e.notes); put("isDefault", e.isDefault); put("sortOrder", e.sortOrder)
    }

    private fun jsonToCashAccount(o: JSONObject) = CashAccountEntity(
        id = o.optLong("id", 0L), accountName = o.optString("accountName", "Kas Toko"),
        balance = o.optDouble("balance", 0.0), accountType = o.optString("accountType", "CASH"),
        isActive = o.optBoolean("isActive", true), color = o.optString("color", "#2563EB"),
        notes = o.optString("notes", ""), isDefault = o.optBoolean("isDefault", false),
        sortOrder = o.optInt("sortOrder", 0)
    )

    private fun cashTransferToJson(e: CashTransferEntity) = JSONObject().apply {
        put("id", e.id); put("fromAccountId", e.fromAccountId); put("fromAccountName", e.fromAccountName)
        put("toAccountId", e.toAccountId); put("toAccountName", e.toAccountName); put("amount", e.amount)
        put("notes", e.notes); put("date", e.date); put("loggedBy", e.loggedBy)
    }

    private fun jsonToCashTransfer(o: JSONObject) = CashTransferEntity(
        id = o.optLong("id", 0L), fromAccountId = o.optLong("fromAccountId", 0L),
        fromAccountName = o.optString("fromAccountName", ""), toAccountId = o.optLong("toAccountId", 0L),
        toAccountName = o.optString("toAccountName", ""), amount = o.optDouble("amount", 0.0),
        notes = o.optString("notes", ""), date = o.optLong("date", System.currentTimeMillis()),
        loggedBy = o.optString("loggedBy", "Admin")
    )

    private fun adminServiceToJson(e: AdminTierServiceEntity) = JSONObject().apply {
        put("id", e.id); put("serviceName", e.serviceName); put("serviceCode", e.serviceCode)
        put("isActive", e.isActive)
    }

    private fun jsonToAdminService(o: JSONObject) = AdminTierServiceEntity(
        id = o.optLong("id", 0L), serviceName = o.optString("serviceName", ""),
        serviceCode = o.optString("serviceCode", "TRANSFER_BANK"), isActive = o.optBoolean("isActive", true)
    )

    private fun adminRangeToJson(e: AdminTierRangeEntity) = JSONObject().apply {
        put("id", e.id); put("serviceId", e.serviceId); put("minAmount", e.minAmount)
        put("maxAmount", e.maxAmount); put("storeAdminFee", e.storeAdminFee)
        put("providerAdminFee", e.providerAdminFee); put("agentCommission", e.agentCommission)
    }

    private fun jsonToAdminRange(o: JSONObject) = AdminTierRangeEntity(
        id = o.optLong("id", 0L), serviceId = o.optLong("serviceId", 0L),
        minAmount = o.optDouble("minAmount", 0.0), maxAmount = o.optDouble("maxAmount", 0.0),
        storeAdminFee = o.optDouble("storeAdminFee", 0.0), providerAdminFee = o.optDouble("providerAdminFee", 0.0),
        agentCommission = o.optDouble("agentCommission", 0.0)
    )

    private fun tierPriceToJson(e: ProductTierPriceEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("quantity", e.quantity); put("totalPrice", e.totalPrice)
    }

    private fun jsonToTierPrice(o: JSONObject) = ProductTierPriceEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L),
        quantity = o.optDouble("quantity", 1.0), totalPrice = o.optDouble("totalPrice", 0.0)
    )

    private fun shiftToJson(e: CashierShiftEntity) = JSONObject().apply {
        put("id", e.id); put("cashierName", e.cashierName); put("startTime", e.startTime)
        put("endTime", e.endTime); put("startingCash", e.startingCash); put("cashIn", e.cashIn)
        put("cashOut", e.cashOut); put("totalCashSales", e.totalCashSales)
        put("expectedClosingCash", e.expectedClosingCash); put("actualClosingCash", e.actualClosingCash)
        put("difference", e.difference); put("status", e.status); put("notes", e.notes)
    }

    private fun jsonToShift(o: JSONObject) = CashierShiftEntity(
        id = o.optLong("id", 0L), cashierName = o.optString("cashierName", "Kasir"),
        startTime = o.optLong("startTime", System.currentTimeMillis()), endTime = o.optLongOrNull("endTime"),
        startingCash = o.optDouble("startingCash", 0.0), cashIn = o.optDouble("cashIn", 0.0),
        cashOut = o.optDouble("cashOut", 0.0), totalCashSales = o.optDouble("totalCashSales", 0.0),
        expectedClosingCash = o.optDouble("expectedClosingCash", 0.0),
        actualClosingCash = o.optDoubleOrNull("actualClosingCash"), difference = o.optDoubleOrNull("difference"),
        status = o.optString("status", "OPEN"), notes = o.optString("notes", "")
    )

    private fun pendingSaleToJson(e: PendingSaleEntity) = JSONObject().apply {
        put("id", e.id); put("holdName", e.holdName); put("cartJson", e.cartJson)
        put("customerId", e.customerId); put("customerName", e.customerName)
        put("totalAmount", e.totalAmount); put("notes", e.notes); put("createdAt", e.createdAt)
    }

    private fun jsonToPendingSale(o: JSONObject) = PendingSaleEntity(
        id = o.optLong("id", 0L), holdName = o.optString("holdName", ""), cartJson = o.optString("cartJson", "[]"),
        customerId = o.optLongOrNull("customerId"), customerName = o.optString("customerName", "Pelanggan Umum"),
        totalAmount = o.optDouble("totalAmount", 0.0), notes = o.optString("notes", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun storeProfileToJson(e: StoreProfileEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("address", e.address); put("phone", e.phone)
        put("logoUri", e.logoUri); put("businessType", e.businessType); put("ppnBp", e.ppnBp)
        put("receiptFooter", e.receiptFooter); put("updatedAt", e.updatedAt)
    }

    private fun jsonToStoreProfile(o: JSONObject, photoMap: Map<String, String>) = StoreProfileEntity(
        id = o.optLong("id", 1L), name = o.optString("name", "Toko Saya"), address = o.optString("address", ""),
        phone = o.optString("phone", ""),
        logoUri = o.optStringOrNull("logoUri")?.let { remapPhotoUri(it, photoMap, logoPrefix = true) },
        businessType = o.optString("businessType", "keduanya"), ppnBp = o.optInt("ppnBp", 0),
        receiptFooter = o.optString("receiptFooter", "Terima kasih telah berbelanja!"),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun counterToJson(e: DocumentCounterEntity) = JSONObject().apply {
        put("counterName", e.counterName); put("value", e.value)
    }

    private fun jsonToCounter(o: JSONObject) = DocumentCounterEntity(
        counterName = o.optString("counterName", "transaksi"), value = o.optLong("value", 0L)
    )

    private fun userToJson(e: UserAccountEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("username", e.username); put("role", e.role)
        put("pinHash", e.pinHash); put("isActive", e.isActive); put("createdAt", e.createdAt)
        put("updatedAt", e.updatedAt)
    }

    private fun jsonToUser(o: JSONObject) = UserAccountEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), username = o.optString("username", ""),
        role = o.optString("role", "kasir"), pinHash = o.optString("pinHash", ""),
        isActive = o.optBoolean("isActive", true), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun permissionToJson(e: MenuPermissionEntity) = JSONObject().apply {
        put("id", e.id); put("userId", e.userId); put("menuKey", e.menuKey); put("allowed", e.allowed)
    }

    private fun jsonToPermission(o: JSONObject) = MenuPermissionEntity(
        id = o.optLong("id", 0L), userId = o.optLong("userId", 0L),
        menuKey = o.optString("menuKey", ""), allowed = o.optBoolean("allowed", true)
    )

    private fun printerToJson(e: PrinterEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("bluetoothAddress", e.bluetoothAddress); put("type", e.type)
        put("language", e.language); put("widthMm", e.widthMm); put("autoCut", e.autoCut)
        put("openDrawer", e.openDrawer); put("quickPrint", e.quickPrint); put("isActive", e.isActive)
        put("createdAt", e.createdAt)
    }

    private fun jsonToPrinter(o: JSONObject) = PrinterEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), bluetoothAddress = o.optString("bluetoothAddress", ""),
        type = o.optString("type", "struk"), language = o.optString("language", "escpos"),
        widthMm = o.optInt("widthMm", 58), autoCut = o.optBoolean("autoCut", false),
        openDrawer = o.optBoolean("openDrawer", false), quickPrint = o.optBoolean("quickPrint", true),
        isActive = o.optBoolean("isActive", true), createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun cashierDeviceToJson(e: CashierDeviceEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("role", e.role); put("lastConnected", e.lastConnected)
        put("isActive", e.isActive); put("createdAt", e.createdAt)
    }

    private fun jsonToCashierDevice(o: JSONObject) = CashierDeviceEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), role = o.optString("role", "utama"),
        lastConnected = o.optLong("lastConnected", System.currentTimeMillis()),
        isActive = o.optBoolean("isActive", true), createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun variantToJson(e: ProductVariantEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("name", e.name); put("barcode", e.barcode)
        put("sellingPrice", e.sellingPrice); put("purchasePrice", e.purchasePrice); put("stock", e.stock)
        put("isActive", e.isActive); put("createdAt", e.createdAt)
    }

    private fun jsonToVariant(o: JSONObject) = ProductVariantEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), name = o.optString("name", ""),
        barcode = o.optString("barcode", ""), sellingPrice = o.optDouble("sellingPrice", 0.0),
        purchasePrice = o.optDouble("purchasePrice", 0.0), stock = o.optDouble("stock", 0.0),
        isActive = o.optBoolean("isActive", true), createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun wholesaleTierToJson(e: WholesaleTierEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("unitName", e.unitName)
        put("minQty", e.minQty); put("pricePerUnit", e.pricePerUnit)
    }

    private fun jsonToWholesaleTier(o: JSONObject) = WholesaleTierEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), unitName = o.optString("unitName", ""),
        minQty = o.optDouble("minQty", 1.0), pricePerUnit = o.optDouble("pricePerUnit", 0.0)
    )

    private fun batchToJson(e: ProductBatchEntity) = JSONObject().apply {
        put("id", e.id); put("productId", e.productId); put("batchCode", e.batchCode)
        put("expiryDateStr", e.expiryDateStr); put("qty", e.qty); put("createdAt", e.createdAt)
    }

    private fun jsonToBatch(o: JSONObject) = ProductBatchEntity(
        id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), batchCode = o.optString("batchCode", ""),
        expiryDateStr = o.optString("expiryDateStr", ""), qty = o.optDouble("qty", 0.0),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun bundleToJson(e: BundlePackageEntity) = JSONObject().apply {
        put("id", e.id); put("name", e.name); put("sellingPrice", e.sellingPrice)
        put("isActive", e.isActive); put("createdAt", e.createdAt)
    }

    private fun jsonToBundle(o: JSONObject) = BundlePackageEntity(
        id = o.optLong("id", 0L), name = o.optString("name", ""), sellingPrice = o.optDouble("sellingPrice", 0.0),
        isActive = o.optBoolean("isActive", true), createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun bundleItemToJson(e: BundleItemEntity) = JSONObject().apply {
        put("id", e.id); put("bundleId", e.bundleId); put("productId", e.productId); put("qty", e.qty)
    }

    private fun jsonToBundleItem(o: JSONObject) = BundleItemEntity(
        id = o.optLong("id", 0L), bundleId = o.optLong("bundleId", 0L),
        productId = o.optLong("productId", 0L), qty = o.optDouble("qty", 1.0)
    )

    private fun promoToJson(e: PromoEntity) = JSONObject().apply {
        put("id", e.id); put("promoType", e.promoType); put("name", e.name); put("code", e.code)
        put("minPurchase", e.minPurchase); put("discountAmount", e.discountAmount)
        put("buyQty", e.buyQty); put("freeQty", e.freeQty); put("isActive", e.isActive)
        put("startDate", e.startDate); put("endDate", e.endDate); put("createdAt", e.createdAt)
    }

    private fun jsonToPromo(o: JSONObject) = PromoEntity(
        id = o.optLong("id", 0L), promoType = o.optString("promoType", "voucher"), name = o.optString("name", ""),
        code = o.optString("code", ""), minPurchase = o.optDouble("minPurchase", 0.0),
        discountAmount = o.optDouble("discountAmount", 0.0), buyQty = o.optInt("buyQty", 0),
        freeQty = o.optInt("freeQty", 0), isActive = o.optBoolean("isActive", true),
        startDate = o.optLongOrNull("startDate"), endDate = o.optLongOrNull("endDate"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun cashMutationToJson(e: CashMutationEntity) = JSONObject().apply {
        put("id", e.id); put("cashAccountId", e.cashAccountId); put("accountName", e.accountName)
        put("amount", e.amount); put("mutationType", e.mutationType); put("refType", e.refType)
        put("refId", e.refId); put("notes", e.notes); put("timestamp", e.timestamp)
    }

    private fun jsonToCashMutation(o: JSONObject) = CashMutationEntity(
        id = o.optLong("id", 0L), cashAccountId = o.optLong("cashAccountId", 0L),
        accountName = o.optString("accountName", ""), amount = o.optDouble("amount", 0.0),
        mutationType = o.optString("mutationType", "koreksi"), refType = o.optString("refType", ""),
        refId = o.optString("refId", ""), notes = o.optString("notes", ""),
        timestamp = o.optLong("timestamp", System.currentTimeMillis())
    )

    private fun shiftMovementToJson(e: ShiftCashMovementEntity) = JSONObject().apply {
        put("id", e.id); put("shiftId", e.shiftId); put("amount", e.amount)
        put("notes", e.notes); put("timestamp", e.timestamp)
    }

    private fun jsonToShiftMovement(o: JSONObject) = ShiftCashMovementEntity(
        id = o.optLong("id", 0L), shiftId = o.optLong("shiftId", 0L), amount = o.optDouble("amount", 0.0),
        notes = o.optString("notes", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis())
    )

    private fun cloudMappingToJson(e: CloudSyncMappingEntity) = JSONObject().apply {
        put("id", e.id); put("entityType", e.entityType); put("remoteId", e.remoteId)
        put("localId", e.localId); put("updatedAt", e.updatedAt)
    }

    private fun jsonToCloudMapping(o: JSONObject) = CloudSyncMappingEntity(
        id = o.optLong("id", 0L), entityType = o.optString("entityType", ""),
        remoteId = o.optString("remoteId", ""), localId = o.optLong("localId", 0L),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun scanLogToJson(e: BarcodeScanLogEntity) = JSONObject().apply {
        put("id", e.id); put("barcode", e.barcode); put("productId", e.productId); put("variantId", e.variantId)
        put("productName", e.productName); put("matchedPrice", e.matchedPrice); put("matchedStock", e.matchedStock)
        put("matched", e.matched); put("source", e.source); put("scannedAt", e.scannedAt)
    }

    private fun jsonToScanLog(o: JSONObject) = BarcodeScanLogEntity(
        id = o.optLong("id", 0L), barcode = o.optString("barcode", ""), productId = o.optLongOrNull("productId"),
        variantId = o.optLongOrNull("variantId"), productName = o.optString("productName", ""),
        matchedPrice = o.optDouble("matchedPrice", 0.0), matchedStock = o.optDouble("matchedStock", 0.0),
        matched = o.optBoolean("matched", false), source = o.optString("source", "CAMERA"),
        scannedAt = o.optLong("scannedAt", System.currentTimeMillis())
    )
}
