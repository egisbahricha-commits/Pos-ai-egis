package com.example.data.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class KasGoTableInfo(
    val tableName: String,
    val rowCount: Int,
    val columns: List<String>,
    val mappedRole: String
)

data class KasGoStoreSettingsPreview(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val receiptFooter: String = "",
    val logoPathHint: String = "",
    val documentCounter: Long? = null
)

data class KasGoInspectResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val fileName: String = "",
    val tempDirPath: String = "",
    val databaseFilePath: String = "",
    val manifestSummary: String = "",
    val storePreview: KasGoStoreSettingsPreview = KasGoStoreSettingsPreview(),
    val discoveredTables: List<KasGoTableInfo> = emptyList(),
    val categoryCount: Int = 0,
    val productCount: Int = 0,
    val variantCount: Int = 0,
    val wholesaleTierCount: Int = 0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val cashAccountCount: Int = 0,
    val saleCount: Int = 0,
    val saleItemCount: Int = 0,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val photoCount: Int = 0,
    val roleTableMap: Map<String, String> = emptyMap(),
    val photoFilesMap: Map<String, String> = emptyMap()
)

data class KasGoImportOptions(
    val replaceExisting: Boolean = false,
    val importStoreProfile: Boolean = true
)

data class KasGoImportSummary(
    val categoriesImported: Int = 0,
    val productsImported: Int = 0,
    val variantsImported: Int = 0,
    val wholesaleTiersImported: Int = 0,
    val customersImported: Int = 0,
    val suppliersImported: Int = 0,
    val cashAccountsImported: Int = 0,
    val salesImported: Int = 0,
    val saleItemsImported: Int = 0,
    val expensesImported: Int = 0,
    val incomesImported: Int = 0,
    val photosCopied: Int = 0,
    val storeProfileUpdated: Boolean = false
) {
    val totalRecords: Int
        get() = categoriesImported + productsImported + variantsImported +
            wholesaleTiersImported + customersImported + suppliersImported +
            cashAccountsImported + salesImported + saleItemsImported +
            expensesImported + incomesImported
}

object KasGoBackupImporter {

    private const val ROLE_CATEGORIES = "categories"
    private const val ROLE_PRODUCTS = "products"
    private const val ROLE_VARIANTS = "variants"
    private const val ROLE_WHOLESALE = "wholesale"
    private const val ROLE_CUSTOMERS = "customers"
    private const val ROLE_SUPPLIERS = "suppliers"
    private const val ROLE_CASH_ACCOUNTS = "cash_accounts"
    private const val ROLE_SALES = "sales"
    private const val ROLE_SALE_ITEMS = "sale_items"
    private const val ROLE_EXPENSES = "expenses"
    private const val ROLE_INCOMES = "incomes"
    private const val ROLE_CASH_MUTATIONS = "cash_mutations"
    private const val ROLE_ADMIN_SERVICES = "admin_services"
    private const val ROLE_ADMIN_RANGES = "admin_ranges"
    private const val ROLE_STORE_PROFILE = "store_profile"

    suspend fun inspectBackup(context: Context, uri: Uri): KasGoInspectResult = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(context, uri)
        val tempRoot = File(context.cacheDir, "kasgo_import_${System.currentTimeMillis()}").apply { mkdirs() }

        try {
            val rawInput = context.contentResolver.openInputStream(uri)
                ?: return@withContext KasGoInspectResult(
                    isValid = false,
                    errorMessage = "Tidak dapat membuka file cadangan yang dipilih.",
                    fileName = displayName,
                    tempDirPath = tempRoot.absolutePath
                )

            val stagedFile = File(tempRoot, "source_backup.bin")
            rawInput.use { input ->
                FileOutputStream(stagedFile).use { output -> input.copyTo(output) }
            }

            if (stagedFile.length() < 16L) {
                return@withContext KasGoInspectResult(
                    isValid = false,
                    errorMessage = "File cadangan kosong atau rusak.",
                    fileName = displayName,
                    tempDirPath = tempRoot.absolutePath
                )
            }

            val header = ByteArray(16)
            stagedFile.inputStream().use { it.read(header) }
            val isZip = header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
            val headerStr = String(header, Charsets.US_ASCII)
            val isSqlite = headerStr.startsWith("SQLite format 3")

            val extractDir = File(tempRoot, "unpacked").apply { mkdirs() }
            if (isZip) {
                unzipSafely(stagedFile, extractDir)
            } else if (isSqlite) {
                stagedFile.copyTo(File(extractDir, "database.db"), overwrite = true)
            } else {
                return@withContext KasGoInspectResult(
                    isValid = false,
                    errorMessage = "Format file tidak dikenali. Pastikan file berasal dari backup .kasgo (ZIP/SQLite).",
                    fileName = displayName,
                    tempDirPath = tempRoot.absolutePath
                )
            }

            // Clean staging binary to save cache space
            stagedFile.delete()

            val allExtractedFiles = extractDir.walkTopDown().filter { it.isFile }.toList()
            val dbFile = allExtractedFiles.firstOrNull { it.name.equals("database.db", ignoreCase = true) }
                ?: allExtractedFiles.firstOrNull {
                    it.extension.lowercase() in listOf("db", "sqlite", "sqlite3")
                }

            if (dbFile == null || !dbFile.exists()) {
                return@withContext KasGoInspectResult(
                    isValid = false,
                    errorMessage = "File database.db tidak ditemukan di dalam arsip $displayName.",
                    fileName = displayName,
                    tempDirPath = tempRoot.absolutePath
                )
            }

            // Parse manifest.json
            val manifestFile = allExtractedFiles.firstOrNull { it.name.equals("manifest.json", ignoreCase = true) }
            val manifestSummary = parseManifestSummary(manifestFile)

            // Parse prefs JSONs
            val settingsJsonFile = allExtractedFiles.firstOrNull {
                it.name.equals("kasgo_settings.json", ignoreCase = true) ||
                    it.name.contains("settings", ignoreCase = true) && it.extension.equals("json", true)
            }
            val docNumJsonFile = allExtractedFiles.firstOrNull {
                it.name.equals("kasgo_document_number.json", ignoreCase = true)
            }
            val storePreview = parsePrefsPreview(settingsJsonFile, docNumJsonFile)

            // Index photos
            val imageExts = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
            val photoFilesMap = linkedMapOf<String, String>()
            var photoCount = 0
            allExtractedFiles.forEach { file ->
                val rel = file.relativeTo(extractDir).invariantSeparatorsPath.lowercase()
                val inPhotosDir = rel.startsWith("photos/") || rel.contains("/photos/")
                if (inPhotosDir || file.extension.lowercase() in imageExts) {
                    photoCount++
                    photoFilesMap[rel] = file.absolutePath
                    photoFilesMap[file.name.lowercase()] = file.absolutePath
                    photoFilesMap[file.nameWithoutExtension.lowercase()] = file.absolutePath
                }
            }

            // Open SQLite DB in read-only mode
            val sqliteDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val tableInfos = mutableListOf<KasGoTableInfo>()
            val roleMap = linkedMapOf<String, String>()
            val rawTables = mutableListOf<Triple<String, Int, List<String>>>()

            try {
                sqliteDb.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name != 'room_master_table' ORDER BY name",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0) ?: continue
                        val count = queryRowCount(sqliteDb, tableName)
                        val cols = queryTableColumns(sqliteDb, tableName)
                        rawTables.add(Triple(tableName, count, cols))
                    }
                }

                // Pass 1: exact & alias table name matching
                val assignedTables = mutableSetOf<String>()
                for ((tableName, count, cols) in rawTables) {
                    val role = matchRoleByTableName(tableName)
                    if (role != null && !roleMap.containsKey(role)) {
                        roleMap[role] = tableName
                        assignedTables.add(tableName)
                    }
                }

                // Pass 2: column-signature heuristic for any still-unmapped core roles
                for ((tableName, count, cols) in rawTables) {
                    if (tableName in assignedTables || count == 0) continue
                    val role = matchRoleByColumns(cols, roleMap.keys)
                    if (role != null) {
                        roleMap[role] = tableName
                        assignedTables.add(tableName)
                    }
                }

                val reverseRole = roleMap.entries.associate { (r, t) -> t to r }
                rawTables.forEach { (tableName, count, cols) ->
                    tableInfos.add(
                        KasGoTableInfo(
                            tableName = tableName,
                            rowCount = count,
                            columns = cols,
                            mappedRole = reverseRole[tableName] ?: "-"
                        )
                    )
                }

                val countOf = { role: String ->
                    val tbl = roleMap[role]
                    rawTables.firstOrNull { it.first == tbl }?.second ?: 0
                }

                val enrichedStore = enrichStorePreviewFromDb(sqliteDb, roleMap[ROLE_STORE_PROFILE], storePreview)

                KasGoInspectResult(
                    isValid = true,
                    fileName = displayName,
                    tempDirPath = tempRoot.absolutePath,
                    databaseFilePath = dbFile.absolutePath,
                    manifestSummary = manifestSummary,
                    storePreview = enrichedStore,
                    discoveredTables = tableInfos,
                    categoryCount = countOf(ROLE_CATEGORIES),
                    productCount = countOf(ROLE_PRODUCTS),
                    variantCount = countOf(ROLE_VARIANTS),
                    wholesaleTierCount = countOf(ROLE_WHOLESALE),
                    customerCount = countOf(ROLE_CUSTOMERS),
                    supplierCount = countOf(ROLE_SUPPLIERS),
                    cashAccountCount = countOf(ROLE_CASH_ACCOUNTS),
                    saleCount = countOf(ROLE_SALES),
                    saleItemCount = countOf(ROLE_SALE_ITEMS),
                    expenseCount = countOf(ROLE_EXPENSES),
                    incomeCount = countOf(ROLE_INCOMES),
                    photoCount = photoCount,
                    roleTableMap = roleMap,
                    photoFilesMap = photoFilesMap
                )
            } finally {
                sqliteDb.close()
            }
        } catch (e: Exception) {
            tempRoot.deleteRecursively()
            KasGoInspectResult(
                isValid = false,
                errorMessage = "Gagal membaca file .kasgo: ${e.message ?: "Kesalahan tidak diketahui"}",
                fileName = displayName
            )
        }
    }

    suspend fun importBackup(
        context: Context,
        db: GeraiUnaDatabase,
        inspectResult: KasGoInspectResult,
        options: KasGoImportOptions
    ): KasGoImportSummary = withContext(Dispatchers.IO) {
        require(inspectResult.isValid && inspectResult.databaseFilePath.isNotBlank()) {
            inspectResult.errorMessage ?: "Data cadangan tidak valid."
        }

        val dbFile = File(inspectResult.databaseFilePath)
        require(dbFile.exists()) { "File sementara database.db sudah tidak tersedia, silakan pilih ulang." }

        val sqliteDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        val imagesDir = File(context.filesDir, "product_images").apply { mkdirs() }
        val copiedPhotosTracker = mutableMapOf<String, String>()
        var photosCopied = 0

        fun copyPhotoToInternal(rawPath: String, fallbackHint: String = ""): String {
            val candidates = buildList {
                if (rawPath.isNotBlank()) {
                    val norm = rawPath.replace('\\', '/').trim().lowercase()
                    add(norm)
                    add(norm.substringAfterLast('/'))
                    add(norm.substringAfterLast('/').substringBeforeLast('.'))
                }
                if (fallbackHint.isNotBlank()) {
                    add(fallbackHint.lowercase())
                }
            }
            for (cand in candidates) {
                val sourcePath = inspectResult.photoFilesMap[cand] ?: continue
                copiedPhotosTracker[sourcePath]?.let { return it }
                val srcFile = File(sourcePath)
                if (!srcFile.exists()) continue
                val ext = srcFile.extension.ifBlank { "jpg" }
                val dest = File(imagesDir, "kasgo_${UUID.randomUUID()}.$ext")
                srcFile.copyTo(dest, overwrite = true)
                copiedPhotosTracker[sourcePath] = dest.absolutePath
                photosCopied++
                return dest.absolutePath
            }
            return ""
        }

        try {
            var categoriesImported = 0
            var productsImported = 0
            var variantsImported = 0
            var wholesaleImported = 0
            var customersImported = 0
            var suppliersImported = 0
            var cashAccountsImported = 0
            var salesImported = 0
            var saleItemsImported = 0
            var expensesImported = 0
            var incomesImported = 0
            var storeProfileUpdated = false

            db.withTransaction {
                val productDao = db.productDao()
                val saleDao = db.saleDao()
                val contactDao = db.contactDao()
                val financeDao = db.financeDao()
                val cashAccountDao = db.cashAccountDao()
                val adminTierDao = db.adminTierDao()
                val extDao = db.extendedPosDao()
                val settingsDao = db.appSettingsDao()

                if (options.replaceExisting) {
                    val writable = db.openHelper.writableDatabase
                    listOf(
                        "sale_items", "sales", "purchase_items", "purchases",
                        "stock_movements", "product_variants", "wholesale_tiers",
                        "product_batches", "product_tier_prices", "unit_conversions",
                        "product_aliases", "bundle_items", "bundle_packages", "promos",
                        "products", "categories", "customers", "suppliers",
                        "expenses", "incomes", "debt_payments",
                        "cash_mutations", "cash_transfers", "cash_accounts",
                        "admin_tier_ranges", "admin_tier_services",
                        "shift_cash_movements", "cashier_shifts", "pending_sales",
                        "cloud_sync_mappings"
                    ).forEach { tbl ->
                        writable.execSQL("DELETE FROM $tbl")
                    }
                }

                // Lookup existing items for safe Merge deduplication
                val existingCategoryNames = queryStrings(db, "SELECT name FROM categories").map { it.lowercase() }.toMutableSet()
                val existingProducts = productDao.getAllProductsList()
                val existingProductByBarcode = existingProducts.filter { it.barcode.isNotBlank() }
                    .associateBy { it.barcode.trim().lowercase() }.toMutableMap()
                val existingProductByName = existingProducts.associateBy { it.name.trim().lowercase() }.toMutableMap()

                val existingCashAccounts = cashAccountDao.getAllAccountsList()
                val existingCashByName = existingCashAccounts.associateBy { it.accountName.trim().lowercase() }.toMutableMap()

                val existingCustomerByName = queryIdAndName(db, "SELECT id, name FROM customers").toMutableMap()
                val existingSupplierByName = queryIdAndName(db, "SELECT id, name FROM suppliers").toMutableMap()
                val existingInvoices = queryStrings(db, "SELECT invoiceNumber FROM sales").map { it.trim().uppercase() }.toMutableSet()

                val categoryIdToName = mutableMapOf<Long, String>()
                val cashAccountIdMap = mutableMapOf<Long, Long>()
                val customerIdMap = mutableMapOf<Long, Long>()
                val productIdMap = mutableMapOf<Long, Long>()
                val saleIdMap = mutableMapOf<Long, Long>()
                val adminServiceIdMap = mutableMapOf<Long, Long>()

                // 1. Categories
                inspectResult.roleTableMap[ROLE_CATEGORIES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id", "kategori_id", "category_id")
                        val iName = findCol(c, "name", "nama", "nama_kategori", "category_name", "title")
                        val iIcon = findCol(c, "icon", "ikon")
                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            if (oldId > 0L) categoryIdToName[oldId] = name
                            if (existingCategoryNames.add(name.lowercase())) {
                                productDao.insertCategory(
                                    CategoryEntity(
                                        name = name,
                                        icon = if (iIcon >= 0) c.getStringSafe(iIcon).ifBlank { "category" } else "category"
                                    )
                                )
                                categoriesImported++
                            }
                        }
                    }
                }

                // 2. Admin Tier Services & Ranges
                inspectResult.roleTableMap[ROLE_ADMIN_SERVICES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id")
                        val iName = findCol(c, "servicename", "service_name", "nama", "nama_layanan", "name")
                        val iCode = findCol(c, "servicecode", "service_code", "kode", "code")
                        val iActive = findCol(c, "isactive", "is_active", "aktif")
                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            val existingServices = adminTierDao.getAllServicesList()
                            val matched = existingServices.firstOrNull { it.serviceName.equals(name, ignoreCase = true) }
                            val newId = matched?.id ?: adminTierDao.insertService(
                                AdminTierServiceEntity(
                                    serviceName = name,
                                    serviceCode = if (iCode >= 0) c.getStringSafe(iCode).ifBlank { "SERVICE" } else "SERVICE",
                                    isActive = if (iActive >= 0) c.getBooleanSafe(iActive, true) else true
                                )
                            )
                            if (oldId > 0L) adminServiceIdMap[oldId] = newId
                        }
                    }
                }

                inspectResult.roleTableMap[ROLE_ADMIN_RANGES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iSvcId = findCol(c, "serviceid", "service_id", "layanan_id")
                        val iMin = findCol(c, "minamount", "min_amount", "nominal_min", "min")
                        val iMax = findCol(c, "maxamount", "max_amount", "nominal_max", "max")
                        val iStoreFee = findCol(c, "storeadminfee", "store_admin_fee", "admin_toko", "biaya_admin", "fee")
                        val iProvFee = findCol(c, "provideradminfee", "provider_admin_fee", "biaya_penyedia")
                        val iAgentFee = findCol(c, "agentcommission", "agent_commission", "komisi_agen")
                        while (c.moveToNext()) {
                            val oldSvc = if (iSvcId >= 0) c.getLongSafe(iSvcId) else 0L
                            val newSvc = adminServiceIdMap[oldSvc] ?: continue
                            val minA = if (iMin >= 0) c.getDoubleSafe(iMin) else 0.0
                            val maxA = if (iMax >= 0) c.getDoubleSafe(iMax) else 0.0
                            val storeFee = if (iStoreFee >= 0) c.getDoubleSafe(iStoreFee) else 0.0
                            val provFee = if (iProvFee >= 0) c.getDoubleSafe(iProvFee) else 0.0
                            val agentFee = if (iAgentFee >= 0) c.getDoubleSafe(iAgentFee) else 0.0
                            val existingRanges = adminTierDao.getRangesList(newSvc)
                            if (existingRanges.none { it.minAmount == minA && it.maxAmount == maxA }) {
                                adminTierDao.insertRange(
                                    AdminTierRangeEntity(
                                        serviceId = newSvc,
                                        minAmount = minA,
                                        maxAmount = maxA,
                                        storeAdminFee = storeFee,
                                        providerAdminFee = provFee,
                                        agentCommission = agentFee
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Cash Accounts
                inspectResult.roleTableMap[ROLE_CASH_ACCOUNTS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id", "account_id", "kas_id")
                        val iName = findCol(c, "accountname", "account_name", "nama", "nama_akun", "nama_kas", "name")
                        val iBalance = findCol(c, "balance", "saldo", "nominal")
                        val iType = findCol(c, "accounttype", "account_type", "tipe", "jenis", "type")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        val iColor = findCol(c, "color", "warna")
                        val iDefault = findCol(c, "isdefault", "is_default", "utama")
                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            val balance = if (iBalance >= 0) c.getDoubleSafe(iBalance) else 0.0
                            val accType = if (iType >= 0) c.getStringSafe(iType).ifBlank { "CASH" } else "CASH"
                            val existing = existingCashByName[name.lowercase()]
                            val newId = if (existing != null) {
                                if (balance != 0.0) {
                                    cashAccountDao.updateBalance(existing.id, balance)
                                }
                                existing.id
                            } else {
                                val entity = CashAccountEntity(
                                    accountName = name,
                                    balance = balance,
                                    accountType = accType.uppercase(),
                                    color = if (iColor >= 0) c.getStringSafe(iColor).ifBlank { "#2563EB" } else "#2563EB",
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                    isDefault = if (iDefault >= 0) c.getBooleanSafe(iDefault) else false,
                                    sortOrder = existingCashByName.size + 1
                                )
                                val inserted = cashAccountDao.insertCashAccount(entity)
                                existingCashByName[name.lowercase()] = entity.copy(id = inserted)
                                cashAccountsImported++
                                inserted
                            }
                            if (oldId > 0L) cashAccountIdMap[oldId] = newId
                        }
                    }
                }

                // Ensure at least one default cash account exists
                if (cashAccountDao.getAllAccountsList().isEmpty()) {
                    cashAccountDao.insertCashAccount(
                        CashAccountEntity(
                            accountName = "Kas Toko",
                            balance = 0.0,
                            accountType = "CASH",
                            color = "#16A34A",
                            isDefault = true,
                            sortOrder = 1
                        )
                    )
                }

                // 4. Customers
                inspectResult.roleTableMap[ROLE_CUSTOMERS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id", "pelanggan_id", "customer_id")
                        val iName = findCol(c, "name", "nama", "nama_pelanggan", "customer_name")
                        val iPhone = findCol(c, "phone", "no_hp", "telepon", "hp", "telp", "whatsapp")
                        val iAddr = findCol(c, "address", "alamat")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        val iRecv = findCol(c, "totalreceivable", "total_receivable", "piutang", "total_piutang", "utang", "hutang")
                        val iDep = findCol(c, "depositbalance", "deposit_balance", "deposit", "saldo_deposit")
                        val iPts = findCol(c, "loyaltypoints", "loyalty_points", "poin", "points")
                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            val existingId = existingCustomerByName[name.lowercase()]
                            val newId = existingId ?: contactDao.insertCustomer(
                                CustomerEntity(
                                    name = name,
                                    phone = if (iPhone >= 0) c.getStringSafe(iPhone) else "",
                                    address = if (iAddr >= 0) c.getStringSafe(iAddr) else "",
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                    totalReceivable = if (iRecv >= 0) c.getDoubleSafe(iRecv) else 0.0,
                                    depositBalance = if (iDep >= 0) c.getDoubleSafe(iDep) else 0.0,
                                    loyaltyPoints = if (iPts >= 0) c.getLongSafe(iPts).toInt() else 0
                                )
                            ).also {
                                existingCustomerByName[name.lowercase()] = it
                                customersImported++
                            }
                            if (oldId > 0L) customerIdMap[oldId] = newId
                        }
                    }
                }

                // 5. Suppliers
                inspectResult.roleTableMap[ROLE_SUPPLIERS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iName = findCol(c, "name", "nama", "nama_supplier", "nama_pemasok")
                        val iPhone = findCol(c, "phone", "no_hp", "telepon", "hp", "telp")
                        val iAddr = findCol(c, "address", "alamat")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        val iDebt = findCol(c, "totaldebt", "total_debt", "hutang", "total_hutang", "utang", "debt")
                        while (c.moveToNext()) {
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            if (!existingSupplierByName.containsKey(name.lowercase())) {
                                val id = contactDao.insertSupplier(
                                    SupplierEntity(
                                        name = name,
                                        phone = if (iPhone >= 0) c.getStringSafe(iPhone) else "",
                                        address = if (iAddr >= 0) c.getStringSafe(iAddr) else "",
                                        notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                        totalDebt = if (iDebt >= 0) c.getDoubleSafe(iDebt) else 0.0
                                    )
                                )
                                existingSupplierByName[name.lowercase()] = id
                                suppliersImported++
                            }
                        }
                    }
                }

                // 6. Products
                inspectResult.roleTableMap[ROLE_PRODUCTS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id", "produk_id", "product_id")
                        val iBarcode = findCol(c, "barcode", "kode_barcode", "sku", "kode", "code")
                        val iName = findCol(c, "name", "nama", "nama_produk", "nama_barang", "title")
                        val iCat = findCol(c, "category", "kategori", "nama_kategori", "category_name")
                        val iCatId = findCol(c, "categoryid", "category_id", "kategori_id")
                        val iUnit = findCol(c, "baseunit", "base_unit", "satuan", "satuan_dasar", "unit")
                        val iBuy = findCol(c, "purchaseprice", "purchase_price", "harga_beli", "harga_modal", "modal", "hpp", "cost_price", "buy_price")
                        val iSell = findCol(c, "sellingprice", "selling_price", "harga_jual", "harga", "price", "sell_price")
                        val iStock = findCol(c, "stock", "stok", "qty", "jumlah", "current_stock")
                        val iMinStock = findCol(c, "minstock", "min_stock", "stok_minimal", "min_stok")
                        val iBrand = findCol(c, "brand", "merek", "merk")
                        val iActive = findCol(c, "isactive", "is_active", "aktif")
                        val iType = findCol(c, "producttype", "product_type", "tipe", "tipe_produk", "jenis", "type")
                        val iUnlim = findCol(c, "isunlimitedstock", "is_unlimited_stock", "tanpa_stok", "unlimited_stock")
                        val iFlex = findCol(c, "isflexibleprice", "is_flexible_price", "harga_fleksibel", "flexible_price")
                        val iNoStock = findCol(c, "hasnostock", "has_no_stock", "tanpa_stok", "no_stock")
                        val iDec = findCol(c, "allowdecimalqty", "allow_decimal_qty", "desimal")
                        val iImg = findCol(c, "imageuri", "image_uri", "foto", "gambar", "photo", "image", "image_path", "photo_path", "foto_path")
                        val iTierGrp = findCol(c, "admintiergroupid", "admin_tier_group_id", "tarif_id")
                        val iCashAcc = findCol(c, "servicecashaccountid", "service_cash_account_id", "akun_kas_id")

                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            val barcode = if (iBarcode >= 0) c.getStringSafe(iBarcode).trim() else ""
                            val catNameFromCol = if (iCat >= 0) c.getStringSafe(iCat).trim() else ""
                            val catNameFromId = if (iCatId >= 0) categoryIdToName[c.getLongSafe(iCatId)] ?: "" else ""
                            val category = catNameFromCol.ifBlank { catNameFromId }.ifBlank { "Umum" }
                            if (existingCategoryNames.add(category.lowercase())) {
                                productDao.insertCategory(CategoryEntity(name = category, icon = "category"))
                                categoriesImported++
                            }

                            val rawType = if (iType >= 0) c.getStringSafe(iType).trim().uppercase() else ""
                            val isDigital = rawType.contains("DIGITAL") || rawType.contains("JASA") || rawType.contains("PPOB")
                            val rawImg = if (iImg >= 0) c.getStringSafe(iImg) else ""
                            val resolvedImg = copyPhotoToInternal(
                                rawImg,
                                fallbackHint = if (oldId > 0L) "product_$oldId" else barcode
                            )

                            val sellPrice = if (iSell >= 0) c.getDoubleSafe(iSell) else 0.0
                            val buyPrice = if (iBuy >= 0) c.getDoubleSafe(iBuy) else 0.0
                            val stock = if (iStock >= 0) c.getDoubleSafe(iStock) else 0.0
                            val minStock = if (iMinStock >= 0) c.getDoubleSafe(iMinStock) else 5.0

                            val existingMatch = (if (barcode.isNotBlank()) existingProductByBarcode[barcode.lowercase()] else null)
                                ?: existingProductByName[name.lowercase()]

                            val oldTier = if (iTierGrp >= 0) c.getLongSafe(iTierGrp) else 0L
                            val oldAcc = if (iCashAcc >= 0) c.getLongSafe(iCashAcc) else 0L

                            val newId = if (existingMatch != null) {
                                val updated = existingMatch.copy(
                                    barcode = barcode.ifBlank { existingMatch.barcode },
                                    category = if (category != "Umum") category else existingMatch.category,
                                    purchasePrice = if (buyPrice > 0.0) buyPrice else existingMatch.purchasePrice,
                                    sellingPrice = if (sellPrice > 0.0) sellPrice else existingMatch.sellingPrice,
                                    stock = stock,
                                    imageUri = resolvedImg.ifBlank { existingMatch.imageUri }
                                )
                                productDao.updateProduct(updated)
                                existingMatch.id
                            } else {
                                val entity = ProductEntity(
                                    barcode = barcode,
                                    name = name,
                                    category = category,
                                    baseUnit = if (iUnit >= 0) c.getStringSafe(iUnit).ifBlank { "pcs" } else "pcs",
                                    purchasePrice = buyPrice,
                                    sellingPrice = sellPrice,
                                    stock = stock,
                                    minStock = minStock,
                                    brand = if (iBrand >= 0) c.getStringSafe(iBrand) else "",
                                    isActive = if (iActive >= 0) c.getBooleanSafe(iActive, true) else true,
                                    productType = if (isDigital) "DIGITAL" else "FISIK",
                                    isUnlimitedStock = if (iUnlim >= 0) c.getBooleanSafe(iUnlim, isDigital) else isDigital,
                                    isFlexiblePrice = if (iFlex >= 0) c.getBooleanSafe(iFlex, isDigital && sellPrice == 0.0) else (isDigital && sellPrice == 0.0),
                                    hasNoStock = if (iNoStock >= 0) c.getBooleanSafe(iNoStock, isDigital) else isDigital,
                                    allowDecimalQty = if (iDec >= 0) c.getBooleanSafe(iDec, false) else false,
                                    adminTierGroupId = if (oldTier > 0L) adminServiceIdMap[oldTier] else null,
                                    serviceCashAccountId = if (oldAcc > 0L) cashAccountIdMap[oldAcc] else null,
                                    imageUri = resolvedImg
                                )
                                val inserted = productDao.insertProduct(entity)
                                val saved = entity.copy(id = inserted)
                                if (barcode.isNotBlank()) existingProductByBarcode[barcode.lowercase()] = saved
                                existingProductByName[name.lowercase()] = saved
                                productsImported++
                                inserted
                            }
                            if (oldId > 0L) productIdMap[oldId] = newId
                        }
                    }
                }

                // 7. Product Variants
                inspectResult.roleTableMap[ROLE_VARIANTS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iProdId = findCol(c, "productid", "product_id", "produk_id")
                        val iName = findCol(c, "name", "nama", "nama_varian", "variant_name")
                        val iBarcode = findCol(c, "barcode", "kode", "sku")
                        val iSell = findCol(c, "sellingprice", "selling_price", "harga_jual", "harga")
                        val iBuy = findCol(c, "purchaseprice", "purchase_price", "harga_beli", "modal")
                        val iStock = findCol(c, "stock", "stok", "qty")
                        while (c.moveToNext()) {
                            val oldProd = if (iProdId >= 0) c.getLongSafe(iProdId) else 0L
                            val newProd = productIdMap[oldProd] ?: continue
                            val name = if (iName >= 0) c.getStringSafe(iName).trim() else ""
                            if (name.isBlank()) continue
                            val existingVars = extDao.getVariantsForProduct(newProd)
                            if (existingVars.none { it.name.equals(name, ignoreCase = true) }) {
                                extDao.insertVariant(
                                    ProductVariantEntity(
                                        productId = newProd,
                                        name = name,
                                        barcode = if (iBarcode >= 0) c.getStringSafe(iBarcode) else "",
                                        sellingPrice = if (iSell >= 0) c.getDoubleSafe(iSell) else 0.0,
                                        purchasePrice = if (iBuy >= 0) c.getDoubleSafe(iBuy) else 0.0,
                                        stock = if (iStock >= 0) c.getDoubleSafe(iStock) else 0.0
                                    )
                                )
                                variantsImported++
                            }
                        }
                    }
                }

                // 8. Wholesale Tiers
                inspectResult.roleTableMap[ROLE_WHOLESALE]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iProdId = findCol(c, "productid", "product_id", "produk_id")
                        val iUnit = findCol(c, "unitname", "unit_name", "satuan", "unit")
                        val iMinQty = findCol(c, "minqty", "min_qty", "jumlah_min", "qty", "quantity")
                        val iPrice = findCol(c, "priceperunit", "price_per_unit", "harga", "harga_satuan", "price")
                        while (c.moveToNext()) {
                            val oldProd = if (iProdId >= 0) c.getLongSafe(iProdId) else 0L
                            val newProd = productIdMap[oldProd] ?: continue
                            val minQ = if (iMinQty >= 0) c.getDoubleSafe(iMinQty) else 0.0
                            val price = if (iPrice >= 0) c.getDoubleSafe(iPrice) else 0.0
                            if (minQ <= 0.0 || price <= 0.0) continue
                            val existingTiers = extDao.getWholesaleTiersForProduct(newProd)
                            if (existingTiers.none { it.minQty == minQ }) {
                                extDao.insertWholesaleTier(
                                    WholesaleTierEntity(
                                        productId = newProd,
                                        unitName = if (iUnit >= 0) c.getStringSafe(iUnit) else "",
                                        minQty = minQ,
                                        pricePerUnit = price
                                    )
                                )
                                wholesaleImported++
                            }
                        }
                    }
                }

                // 9. Sales / Transactions
                var maxImportedTrxSeq = 0L
                inspectResult.roleTableMap[ROLE_SALES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iId = findCol(c, "id", "_id", "transaksi_id", "sale_id", "transaction_id")
                        val iInv = findCol(c, "invoicenumber", "invoice_number", "no_nota", "no_faktur", "kode_transaksi", "no_transaksi", "nomor")
                        val iDate = findCol(c, "date", "tanggal", "waktu", "created_at", "timestamp")
                        val iCashier = findCol(c, "cashiername", "cashier_name", "kasir", "nama_kasir")
                        val iCustId = findCol(c, "customerid", "customer_id", "pelanggan_id")
                        val iCustName = findCol(c, "customername", "customer_name", "nama_pelanggan")
                        val iSub = findCol(c, "subtotal", "sub_total", "total_awal")
                        val iDisc = findCol(c, "discount", "diskon", "potongan")
                        val iTax = findCol(c, "tax", "pajak", "ppn")
                        val iFinal = findCol(c, "finalamount", "final_amount", "total", "total_akhir", "grand_total", "total_bayar")
                        val iPayMethod = findCol(c, "paymentmethod", "payment_method", "metode_bayar", "pembayaran", "metode")
                        val iPaid = findCol(c, "amountpaid", "amount_paid", "bayar", "jumlah_bayar", "uang_bayar")
                        val iChange = findCol(c, "changeamount", "change_amount", "kembalian", "kembali")
                        val iStatus = findCol(c, "status", "status_transaksi")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        val iStoreFee = findCol(c, "storeadminfee", "store_admin_fee", "biaya_admin", "admin_toko")
                        val iProvFee = findCol(c, "provideradminfee", "provider_admin_fee", "biaya_penyedia")
                        val iAgentFee = findCol(c, "agentcommission", "agent_commission", "komisi_agen")
                        val iCashAcc = findCol(c, "cashaccountid", "cash_account_id", "akun_kas_id")

                        while (c.moveToNext()) {
                            val oldId = if (iId >= 0) c.getLongSafe(iId) else 0L
                            val rawInv = if (iInv >= 0) c.getStringSafe(iInv).trim() else ""
                            val invoiceNum = rawInv.ifBlank { "KASGO-${if (oldId > 0L) oldId else (salesImported + 1)}" }
                            if (!existingInvoices.add(invoiceNum.uppercase())) {
                                continue
                            }
                            invoiceNum.substringAfterLast('-').toLongOrNull()?.let { seq ->
                                if (seq > maxImportedTrxSeq) maxImportedTrxSeq = seq
                            }

                            val finalAmt = if (iFinal >= 0) c.getDoubleSafe(iFinal) else 0.0
                            val subtotal = if (iSub >= 0) c.getDoubleSafe(iSub).let { if (it == 0.0 && finalAmt > 0.0) finalAmt else it } else finalAmt
                            val oldCust = if (iCustId >= 0) c.getLongSafe(iCustId) else 0L
                            val oldAcc = if (iCashAcc >= 0) c.getLongSafe(iCashAcc) else 0L
                            val rawStatus = if (iStatus >= 0) c.getStringSafe(iStatus).trim().uppercase() else "COMPLETED"
                            val normStatus = when {
                                rawStatus.contains("BATAL") || rawStatus.contains("CANCEL") -> "CANCELLED"
                                rawStatus.contains("RETUR") || rawStatus.contains("REFUND") -> "REFUNDED"
                                rawStatus.contains("PRE") -> "PRE_ORDER"
                                else -> "COMPLETED"
                            }
                            val rawPay = if (iPayMethod >= 0) c.getStringSafe(iPayMethod).trim().uppercase() else "TUNAI"
                            val normPay = when {
                                rawPay.contains("QRIS") -> "QRIS"
                                rawPay.contains("TRANSFER") || rawPay.contains("BANK") -> "TRANSFER"
                                rawPay.contains("KREDIT") || rawPay.contains("HUTANG") || rawPay.contains("KASBON") || rawPay.contains("TEMPO") -> "KREDIT"
                                else -> "TUNAI"
                            }

                            val newSaleId = saleDao.insertSale(
                                SaleEntity(
                                    invoiceNumber = invoiceNum,
                                    date = if (iDate >= 0) parseTimestampMillis(c.getStringSafe(iDate)) else System.currentTimeMillis(),
                                    cashierName = if (iCashier >= 0) c.getStringSafe(iCashier).ifBlank { "Kasir" } else "Kasir",
                                    customerId = if (oldCust > 0L) customerIdMap[oldCust] else null,
                                    customerName = if (iCustName >= 0) c.getStringSafe(iCustName).ifBlank { "Pelanggan Umum" } else "Pelanggan Umum",
                                    subtotal = subtotal,
                                    discount = if (iDisc >= 0) c.getDoubleSafe(iDisc) else 0.0,
                                    tax = if (iTax >= 0) c.getDoubleSafe(iTax) else 0.0,
                                    finalAmount = finalAmt,
                                    paymentMethod = normPay,
                                    amountPaid = if (iPaid >= 0) c.getDoubleSafe(iPaid).let { if (it == 0.0 && normPay != "KREDIT") finalAmt else it } else finalAmt,
                                    changeAmount = if (iChange >= 0) c.getDoubleSafe(iChange) else 0.0,
                                    status = normStatus,
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                    storeAdminFee = if (iStoreFee >= 0) c.getDoubleSafe(iStoreFee) else 0.0,
                                    providerAdminFee = if (iProvFee >= 0) c.getDoubleSafe(iProvFee) else 0.0,
                                    agentCommission = if (iAgentFee >= 0) c.getDoubleSafe(iAgentFee) else 0.0,
                                    cashAccountId = if (oldAcc > 0L) cashAccountIdMap[oldAcc] else null
                                )
                            )
                            if (oldId > 0L) saleIdMap[oldId] = newSaleId
                            salesImported++
                        }
                    }
                }

                // 10. Sale Items
                inspectResult.roleTableMap[ROLE_SALE_ITEMS]?.let { tbl ->
                    val batchItems = mutableListOf<SaleItemEntity>()
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iSaleId = findCol(c, "saleid", "sale_id", "transaksi_id", "penjualan_id", "transaction_id", "order_id")
                        val iProdId = findCol(c, "productid", "product_id", "produk_id", "barang_id", "item_id")
                        val iProdName = findCol(c, "productname", "product_name", "nama_produk", "nama_barang", "nama")
                        val iQty = findCol(c, "quantity", "qty", "jumlah")
                        val iUnit = findCol(c, "unit", "satuan")
                        val iUnitPrice = findCol(c, "unitprice", "unit_price", "harga", "harga_jual", "harga_satuan", "price")
                        val iSub = findCol(c, "subtotal", "sub_total", "total")
                        val iBuy = findCol(c, "purchasepricesnapshot", "purchase_price_snapshot", "harga_beli", "harga_modal", "modal", "hpp")
                        val iStoreFee = findCol(c, "storeadminfee", "store_admin_fee", "biaya_admin", "admin_toko")
                        val iProvFee = findCol(c, "provideradminfee", "provider_admin_fee", "biaya_penyedia")
                        val iAgentFee = findCol(c, "agentcommission", "agent_commission", "komisi_agen")

                        while (c.moveToNext()) {
                            val oldSale = if (iSaleId >= 0) c.getLongSafe(iSaleId) else 0L
                            val newSale = saleIdMap[oldSale] ?: continue
                            val oldProd = if (iProdId >= 0) c.getLongSafe(iProdId) else 0L
                            val qty = if (iQty >= 0) c.getDoubleSafe(iQty).let { if (it == 0.0) 1.0 else it } else 1.0
                            val unitPrice = if (iUnitPrice >= 0) c.getDoubleSafe(iUnitPrice) else 0.0
                            val subtotal = if (iSub >= 0) c.getDoubleSafe(iSub).let { if (it == 0.0) qty * unitPrice else it } else (qty * unitPrice)

                            batchItems.add(
                                SaleItemEntity(
                                    saleId = newSale,
                                    productId = productIdMap[oldProd] ?: 0L,
                                    productName = if (iProdName >= 0) c.getStringSafe(iProdName).ifBlank { "Item KasGo" } else "Item KasGo",
                                    quantity = qty,
                                    unit = if (iUnit >= 0) c.getStringSafe(iUnit).ifBlank { "pcs" } else "pcs",
                                    unitPrice = unitPrice,
                                    subtotal = subtotal,
                                    purchasePriceSnapshot = if (iBuy >= 0) c.getDoubleSafe(iBuy) else 0.0,
                                    storeAdminFee = if (iStoreFee >= 0) c.getDoubleSafe(iStoreFee) else 0.0,
                                    providerAdminFee = if (iProvFee >= 0) c.getDoubleSafe(iProvFee) else 0.0,
                                    agentCommission = if (iAgentFee >= 0) c.getDoubleSafe(iAgentFee) else 0.0
                                )
                            )
                            if (batchItems.size >= 250) {
                                saleDao.insertSaleItems(batchItems)
                                saleItemsImported += batchItems.size
                                batchItems.clear()
                            }
                        }
                    }
                    if (batchItems.isNotEmpty()) {
                        saleDao.insertSaleItems(batchItems)
                        saleItemsImported += batchItems.size
                    }
                }

                // 11. Expenses
                inspectResult.roleTableMap[ROLE_EXPENSES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iCat = findCol(c, "category", "kategori", "nama", "judul", "title")
                        val iAmt = findCol(c, "amount", "nominal", "jumlah", "total")
                        val iDate = findCol(c, "date", "tanggal", "created_at", "timestamp")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan", "deskripsi")
                        val iSrc = findCol(c, "paymentsource", "payment_source", "sumber_kas", "akun_kas")
                        while (c.moveToNext()) {
                            val amt = if (iAmt >= 0) c.getDoubleSafe(iAmt) else 0.0
                            if (amt <= 0.0) continue
                            financeDao.insertExpense(
                                ExpenseEntity(
                                    category = if (iCat >= 0) c.getStringSafe(iCat).ifBlank { "Operasional" } else "Operasional",
                                    amount = amt,
                                    date = if (iDate >= 0) parseTimestampMillis(c.getStringSafe(iDate)) else System.currentTimeMillis(),
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                    paymentSource = if (iSrc >= 0) c.getStringSafe(iSrc).ifBlank { "Kas Toko" } else "Kas Toko"
                                )
                            )
                            expensesImported++
                        }
                    }
                }

                // 12. Incomes
                inspectResult.roleTableMap[ROLE_INCOMES]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iSrc = findCol(c, "source", "sumber", "kategori", "nama", "judul")
                        val iAmt = findCol(c, "amount", "nominal", "jumlah", "total")
                        val iDate = findCol(c, "date", "tanggal", "created_at", "timestamp")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        while (c.moveToNext()) {
                            val amt = if (iAmt >= 0) c.getDoubleSafe(iAmt) else 0.0
                            if (amt <= 0.0) continue
                            financeDao.insertIncome(
                                IncomeEntity(
                                    source = if (iSrc >= 0) c.getStringSafe(iSrc).ifBlank { "Pemasukan Lain" } else "Pemasukan Lain",
                                    amount = amt,
                                    date = if (iDate >= 0) parseTimestampMillis(c.getStringSafe(iDate)) else System.currentTimeMillis(),
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else ""
                                )
                            )
                            incomesImported++
                        }
                    }
                }

                // 13. Cash Mutations
                inspectResult.roleTableMap[ROLE_CASH_MUTATIONS]?.let { tbl ->
                    sqliteDb.rawQuery("SELECT * FROM \"$tbl\"", null).use { c ->
                        val iAccId = findCol(c, "cashaccountid", "cash_account_id", "akun_kas_id")
                        val iAccName = findCol(c, "accountname", "account_name", "nama_akun")
                        val iAmt = findCol(c, "amount", "nominal")
                        val iType = findCol(c, "mutationtype", "mutation_type", "tipe", "jenis")
                        val iNotes = findCol(c, "notes", "catatan", "keterangan")
                        val iTime = findCol(c, "timestamp", "date", "tanggal", "created_at")
                        while (c.moveToNext()) {
                            val amt = if (iAmt >= 0) c.getDoubleSafe(iAmt) else 0.0
                            if (amt == 0.0) continue
                            val oldAcc = if (iAccId >= 0) c.getLongSafe(iAccId) else 0L
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = cashAccountIdMap[oldAcc] ?: 1L,
                                    accountName = if (iAccName >= 0) c.getStringSafe(iAccName).ifBlank { "Kas" } else "Kas",
                                    amount = amt,
                                    mutationType = if (iType >= 0) c.getStringSafe(iType).ifBlank { "koreksi" } else "koreksi",
                                    notes = if (iNotes >= 0) c.getStringSafe(iNotes) else "",
                                    timestamp = if (iTime >= 0) parseTimestampMillis(c.getStringSafe(iTime)) else System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // 14. Store profile & Document counter
                if (options.importStoreProfile) {
                    val current = extDao.getStoreProfileSync() ?: StoreProfileEntity()
                    val preview = inspectResult.storePreview
                    val resolvedLogo = if (preview.logoPathHint.isNotBlank()) {
                        copyPhotoToInternal(preview.logoPathHint)
                    } else if (inspectResult.photoCount == 1 && photosCopied == 0) {
                        val singlePhoto = inspectResult.photoFilesMap.values.firstOrNull() ?: ""
                        copyPhotoToInternal(singlePhoto)
                    } else {
                        ""
                    }

                    val updatedName = preview.storeName.ifBlank { current.name }
                    val updatedAddr = preview.storeAddress.ifBlank { current.address }
                    val updatedPhone = preview.storePhone.ifBlank { current.phone }
                    val updatedFooter = preview.receiptFooter.ifBlank { current.receiptFooter }
                    if (preview.storeName.isNotBlank() || resolvedLogo.isNotBlank()) {
                        extDao.upsertStoreProfile(
                            current.copy(
                                name = updatedName,
                                address = updatedAddr,
                                phone = updatedPhone,
                                receiptFooter = updatedFooter,
                                logoUri = resolvedLogo.ifBlank { current.logoUri },
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        if (preview.storeName.isNotBlank()) {
                            settingsDao.setSetting(AppSettingsEntity("store_name", updatedName))
                        }
                        if (preview.storeAddress.isNotBlank()) {
                            settingsDao.setSetting(AppSettingsEntity("store_address", updatedAddr))
                        }
                        if (preview.storePhone.isNotBlank()) {
                            settingsDao.setSetting(AppSettingsEntity("store_phone", updatedPhone))
                        }
                        storeProfileUpdated = true
                    }
                }

                val targetCounter = maxOf(
                    inspectResult.storePreview.documentCounter ?: 0L,
                    maxImportedTrxSeq,
                    extDao.getCounter("transaksi")?.value ?: 0L
                )
                if (targetCounter > 0L) {
                    extDao.upsertCounter(DocumentCounterEntity("transaksi", targetCounter))
                }
            }

            cleanupTemp(inspectResult)

            KasGoImportSummary(
                categoriesImported = categoriesImported,
                productsImported = productsImported,
                variantsImported = variantsImported,
                wholesaleTiersImported = wholesaleImported,
                customersImported = customersImported,
                suppliersImported = suppliersImported,
                cashAccountsImported = cashAccountsImported,
                salesImported = salesImported,
                saleItemsImported = saleItemsImported,
                expensesImported = expensesImported,
                incomesImported = incomesImported,
                photosCopied = photosCopied,
                storeProfileUpdated = storeProfileUpdated
            )
        } finally {
            sqliteDb.close()
        }
    }

    fun cleanupTemp(inspectResult: KasGoInspectResult) {
        if (inspectResult.tempDirPath.isNotBlank()) {
            runCatching { File(inspectResult.tempDirPath).deleteRecursively() }
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "backup.kasgo" } ?: "backup.kasgo"
    }

    private fun unzipSafely(zipFile: File, destDir: File) {
        val canonicalDest = destDir.canonicalPath + File.separator
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                val canonicalOut = outFile.canonicalPath
                if (!canonicalOut.startsWith(canonicalDest)) {
                    throw SecurityException("Entri ZIP tidak valid: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun parseManifestSummary(manifestFile: File?): String {
        if (manifestFile == null || !manifestFile.exists()) return ""
        return runCatching {
            val json = JSONObject(manifestFile.readText())
            val parts = mutableListOf<String>()
            val app = json.optString("appName").ifBlank { json.optString("app") }
            val ver = json.optString("versionName").ifBlank { json.optString("version") }
            val created = json.optString("createdAt").ifBlank { json.optString("exportedAt") }
            if (app.isNotBlank()) parts.add(app)
            if (ver.isNotBlank()) parts.add("v$ver")
            if (created.isNotBlank()) parts.add(created)
            parts.joinToString(" • ")
        }.getOrDefault("")
    }

    private fun parsePrefsPreview(settingsFile: File?, docNumberFile: File?): KasGoStoreSettingsPreview {
        var name = ""
        var address = ""
        var phone = ""
        var footer = ""
        var logoHint = ""
        var counter: Long? = null

        if (settingsFile != null && settingsFile.exists()) {
            runCatching {
                val root = JSONObject(settingsFile.readText())
                val obj = root.optJSONObject("entries") ?: root.optJSONObject("data") ?: root
                name = optFirstString(obj, "store_name", "storeName", "nama_toko", "shop_name", "toko_nama", "business_name", "name")
                address = optFirstString(obj, "store_address", "storeAddress", "alamat_toko", "alamat", "address")
                phone = optFirstString(obj, "store_phone", "storePhone", "no_hp", "telepon", "phone", "whatsapp")
                footer = optFirstString(obj, "receipt_footer", "footer_struk", "receiptFooter", "footer")
                logoHint = optFirstString(obj, "store_logo", "logo", "logo_uri", "logoPath", "photo")
            }
        }

        if (docNumberFile != null && docNumberFile.exists()) {
            runCatching {
                val root = JSONObject(docNumberFile.readText())
                val obj = root.optJSONObject("entries") ?: root
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.opt(k)?.toString()?.toLongOrNull()
                    if (v != null && (counter == null || v > counter!!)) {
                        counter = v
                    }
                }
            }
        }

        return KasGoStoreSettingsPreview(
            storeName = name,
            storeAddress = address,
            storePhone = phone,
            receiptFooter = footer,
            logoPathHint = logoHint,
            documentCounter = counter
        )
    }

    private fun enrichStorePreviewFromDb(
        sqliteDb: SQLiteDatabase,
        storeTable: String?,
        base: KasGoStoreSettingsPreview
    ): KasGoStoreSettingsPreview {
        if (storeTable == null) return base
        return runCatching {
            sqliteDb.rawQuery("SELECT * FROM \"$storeTable\" LIMIT 1", null).use { c ->
                if (!c.moveToFirst()) return@runCatching base
                val iName = findCol(c, "name", "nama", "nama_toko", "store_name")
                val iAddr = findCol(c, "address", "alamat")
                val iPhone = findCol(c, "phone", "no_hp", "telepon")
                val iFooter = findCol(c, "receiptfooter", "receipt_footer", "footer")
                val iLogo = findCol(c, "logouri", "logo_uri", "logo")
                base.copy(
                    storeName = base.storeName.ifBlank { if (iName >= 0) c.getStringSafe(iName) else "" },
                    storeAddress = base.storeAddress.ifBlank { if (iAddr >= 0) c.getStringSafe(iAddr) else "" },
                    storePhone = base.storePhone.ifBlank { if (iPhone >= 0) c.getStringSafe(iPhone) else "" },
                    receiptFooter = base.receiptFooter.ifBlank { if (iFooter >= 0) c.getStringSafe(iFooter) else "" },
                    logoPathHint = base.logoPathHint.ifBlank { if (iLogo >= 0) c.getStringSafe(iLogo) else "" }
                )
            }
        }.getOrDefault(base)
    }

    private fun optFirstString(obj: JSONObject, vararg keys: String): String {
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val v = obj.optString(k, "").trim()
                if (v.isNotBlank()) return v
            }
        }
        return ""
    }

    private fun queryRowCount(db: SQLiteDatabase, table: String): Int {
        return runCatching {
            db.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        }.getOrDefault(0)
    }

    private fun queryTableColumns(db: SQLiteDatabase, table: String): List<String> {
        val cols = mutableListOf<String>()
        runCatching {
            db.rawQuery("PRAGMA table_info(\"$table\")", null).use { c ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    if (nameIdx >= 0) cols.add(c.getString(nameIdx))
                }
            }
        }
        return cols
    }

    private fun matchRoleByTableName(rawTableName: String): String? {
        val t = rawTableName.lowercase().trim()
        return when (t) {
            "categories", "category", "kategori", "kategori_produk", "product_categories" -> ROLE_CATEGORIES
            "products", "product", "produk", "barang", "items", "item", "master_produk", "data_produk" -> ROLE_PRODUCTS
            "product_variants", "variants", "varian", "varian_produk" -> ROLE_VARIANTS
            "wholesale_tiers", "harga_grosir", "harga_bertingkat", "product_tier_prices", "tier_prices", "grosir" -> ROLE_WHOLESALE
            "customers", "customer", "pelanggan", "member", "members", "kontak_pelanggan" -> ROLE_CUSTOMERS
            "suppliers", "supplier", "pemasok", "distributor" -> ROLE_SUPPLIERS
            "cash_accounts", "cash_account", "akun_kas", "kas", "rekening", "dompet", "wallets" -> ROLE_CASH_ACCOUNTS
            "sales", "sale", "transactions", "transaction", "transaksi", "penjualan", "nota", "orders" -> ROLE_SALES
            "sale_items", "sale_item", "transaction_items", "transaction_item", "detail_transaksi", "detail_penjualan", "transaksi_detail", "transaksi_item", "item_transaksi", "order_items" -> ROLE_SALE_ITEMS
            "expenses", "expense", "pengeluaran", "beban", "biaya" -> ROLE_EXPENSES
            "incomes", "income", "pemasukan", "pendapatan", "pemasukan_lain" -> ROLE_INCOMES
            "cash_mutations", "mutasi_kas", "kas_mutasi" -> ROLE_CASH_MUTATIONS
            "admin_tier_services", "layanan_admin", "tarif_admin" -> ROLE_ADMIN_SERVICES
            "admin_tier_ranges", "rentang_tarif" -> ROLE_ADMIN_RANGES
            "store_profiles", "store_profile", "profil_toko", "toko" -> ROLE_STORE_PROFILE
            else -> null
        }
    }

    private fun matchRoleByColumns(columns: List<String>, existingRoles: Set<String>): String? {
        val norm = columns.map { it.lowercase().replace("_", "") }.toSet()
        fun hasAny(vararg keys: String) = keys.any { it.lowercase().replace("_", "") in norm }

        if (ROLE_SALE_ITEMS !in existingRoles &&
            hasAny("saleId", "transaksiId", "penjualanId", "transactionId") &&
            hasAny("productName", "namaProduk", "namaBarang", "productId", "produkId") &&
            hasAny("qty", "quantity", "jumlah")
        ) return ROLE_SALE_ITEMS

        if (ROLE_SALES !in existingRoles &&
            hasAny("invoiceNumber", "noNota", "noFaktur", "kodeTransaksi") &&
            hasAny("finalAmount", "total", "totalAkhir", "grandTotal")
        ) return ROLE_SALES

        if (ROLE_PRODUCTS !in existingRoles &&
            hasAny("name", "nama", "namaProduk", "namaBarang") &&
            hasAny("sellingPrice", "hargaJual", "harga") &&
            !hasAny("saleId", "transaksiId")
        ) return ROLE_PRODUCTS

        if (ROLE_CUSTOMERS !in existingRoles &&
            hasAny("name", "nama", "namaPelanggan") &&
            hasAny("piutang", "totalReceivable", "depositBalance")
        ) return ROLE_CUSTOMERS

        if (ROLE_SUPPLIERS !in existingRoles &&
            hasAny("name", "nama", "namaSupplier", "namaPemasok") &&
            hasAny("hutang", "totalDebt", "debt")
        ) return ROLE_SUPPLIERS

        return null
    }

    private fun findCol(cursor: Cursor, vararg candidates: String): Int {
        val normCandidates = candidates.map { it.lowercase().replace("_", "") }
        for (idx in 0 until cursor.columnCount) {
            val colNorm = cursor.getColumnName(idx).lowercase().replace("_", "")
            if (colNorm in normCandidates) return idx
        }
        return -1
    }

    private fun Cursor.getStringSafe(colIdx: Int): String {
        if (colIdx < 0) return ""
        return when (getType(colIdx)) {
            Cursor.FIELD_TYPE_NULL -> ""
            Cursor.FIELD_TYPE_STRING -> getString(colIdx) ?: ""
            Cursor.FIELD_TYPE_INTEGER -> getLong(colIdx).toString()
            Cursor.FIELD_TYPE_FLOAT -> {
                val d = getDouble(colIdx)
                if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
            }
            else -> ""
        }
    }

    private fun Cursor.getLongSafe(colIdx: Int): Long {
        if (colIdx < 0) return 0L
        return when (getType(colIdx)) {
            Cursor.FIELD_TYPE_NULL -> 0L
            Cursor.FIELD_TYPE_INTEGER -> getLong(colIdx)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(colIdx).toLong()
            Cursor.FIELD_TYPE_STRING -> getString(colIdx)?.trim()?.toDoubleOrNull()?.toLong() ?: 0L
            else -> 0L
        }
    }

    private fun Cursor.getDoubleSafe(colIdx: Int): Double {
        if (colIdx < 0) return 0.0
        return when (getType(colIdx)) {
            Cursor.FIELD_TYPE_NULL -> 0.0
            Cursor.FIELD_TYPE_FLOAT -> getDouble(colIdx)
            Cursor.FIELD_TYPE_INTEGER -> getLong(colIdx).toDouble()
            Cursor.FIELD_TYPE_STRING -> getString(colIdx)?.trim()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun Cursor.getBooleanSafe(colIdx: Int, default: Boolean = false): Boolean {
        if (colIdx < 0) return default
        return when (getType(colIdx)) {
            Cursor.FIELD_TYPE_NULL -> default
            Cursor.FIELD_TYPE_INTEGER -> getLong(colIdx) != 0L
            Cursor.FIELD_TYPE_FLOAT -> getDouble(colIdx) != 0.0
            Cursor.FIELD_TYPE_STRING -> {
                val s = getString(colIdx)?.trim()?.lowercase() ?: return default
                s == "1" || s == "true" || s == "ya" || s == "yes" || s == "aktif"
            }
            else -> default
        }
    }

    private fun parseTimestampMillis(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return System.currentTimeMillis()
        trimmed.toLongOrNull()?.let { num ->
            return if (num in 1L..9_999_999_999L) num * 1000L else num
        }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm"
        )
        for (p in patterns) {
            runCatching {
                SimpleDateFormat(p, Locale.US).parse(trimmed)?.time
            }.getOrNull()?.let { return it }
        }
        return System.currentTimeMillis()
    }

    private fun queryStrings(db: GeraiUnaDatabase, sql: String): List<String> {
        val list = mutableListOf<String>()
        db.openHelper.readableDatabase.query(sql).use { c ->
            while (c.moveToNext()) {
                c.getString(0)?.let { list.add(it) }
            }
        }
        return list
    }

    private fun queryIdAndName(db: GeraiUnaDatabase, sql: String): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        db.openHelper.readableDatabase.query(sql).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1)?.trim()?.lowercase() ?: continue
                if (name.isNotBlank()) map[name] = id
            }
        }
        return map
    }
}
