package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        UnitConversionEntity::class,
        ProductAliasEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        StockMovementEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        DebtPaymentEntity::class,
        AICommandLogEntity::class,
        AppSettingsEntity::class,
        CashAccountEntity::class,
        CashTransferEntity::class,
        AdminTierServiceEntity::class,
        AdminTierRangeEntity::class,
        ProductTierPriceEntity::class,
        CashierShiftEntity::class,
        PendingSaleEntity::class,
        // Schema v3 entities
        StoreProfileEntity::class,
        DocumentCounterEntity::class,
        UserAccountEntity::class,
        MenuPermissionEntity::class,
        PrinterEntity::class,
        CashierDeviceEntity::class,
        ProductVariantEntity::class,
        WholesaleTierEntity::class,
        ProductBatchEntity::class,
        BundlePackageEntity::class,
        BundleItemEntity::class,
        PromoEntity::class,
        CashMutationEntity::class,
        ShiftCashMovementEntity::class,
        CloudSyncMappingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GeraiUnaDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun financeDao(): FinanceDao
    abstract fun contactDao(): ContactDao
    abstract fun aiDao(): AIDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun cashAccountDao(): CashAccountDao
    abstract fun adminTierDao(): AdminTierDao
    abstract fun shiftDao(): ShiftDao
    abstract fun extendedPosDao(): ExtendedPosDao

    companion object {
        @Volatile
        private var INSTANCE: GeraiUnaDatabase? = null

        fun hashPin(pin: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to existing tables non-destructively
                db.execSQL("ALTER TABLE products ADD COLUMN autoBarcode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN specialPrice REAL")
                db.execSQL("ALTER TABLE products ADD COLUMN strikePrice REAL")
                db.execSQL("ALTER TABLE products ADD COLUMN hasVariants INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN adminTierGroupId INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN serviceCashAccountId INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN serviceDirection TEXT NOT NULL DEFAULT 'SETOR'")

                db.execSQL("ALTER TABLE customers ADD COLUMN depositBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE customers ADD COLUMN loyaltyPoints INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE sales ADD COLUMN shippingCost REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN additionalFee REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN promoDiscount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN appliedPromoName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN depositUsed REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN pointsEarned INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE sale_items ADD COLUMN variantId INTEGER")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN variantName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN priceSource TEXT NOT NULL DEFAULT 'tetap'")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN serviceNominal REAL")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN storeAdminFee REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN providerAdminFee REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN agentCommission REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sale_items ADD COLUMN adminModifiedManually INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE cash_accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

                // Create new tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS store_profiles (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        logoUri TEXT,
                        businessType TEXT NOT NULL,
                        ppnBp INTEGER NOT NULL,
                        receiptFooter TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS document_counters (
                        counterName TEXT NOT NULL PRIMARY KEY,
                        value INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        username TEXT NOT NULL,
                        role TEXT NOT NULL,
                        pinHash TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS menu_permissions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        menuKey TEXT NOT NULL,
                        allowed INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS printers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bluetoothAddress TEXT NOT NULL,
                        type TEXT NOT NULL,
                        language TEXT NOT NULL,
                        widthMm INTEGER NOT NULL,
                        autoCut INTEGER NOT NULL,
                        openDrawer INTEGER NOT NULL,
                        quickPrint INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cashier_devices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        role TEXT NOT NULL,
                        lastConnected INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_variants (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        barcode TEXT NOT NULL,
                        sellingPrice REAL NOT NULL,
                        purchasePrice REAL NOT NULL,
                        stock REAL NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS wholesale_tiers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        unitName TEXT NOT NULL,
                        minQty REAL NOT NULL,
                        pricePerUnit REAL NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        batchCode TEXT NOT NULL,
                        expiryDateStr TEXT NOT NULL,
                        qty REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bundle_packages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sellingPrice REAL NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bundle_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bundleId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        qty REAL NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS promos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        promoType TEXT NOT NULL,
                        name TEXT NOT NULL,
                        code TEXT NOT NULL,
                        minPurchase REAL NOT NULL,
                        discountAmount REAL NOT NULL,
                        buyQty INTEGER NOT NULL,
                        freeQty INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        startDate INTEGER,
                        endDate INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cash_mutations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cashAccountId INTEGER NOT NULL,
                        accountName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        mutationType TEXT NOT NULL,
                        refType TEXT NOT NULL,
                        refId TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_cash_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shiftId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        notes TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cloud_sync_mappings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        remoteId TEXT NOT NULL,
                        localId INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_admin_tier_ranges_serviceId_minAmount_maxAmount ON admin_tier_ranges(serviceId, minAmount, maxAmount)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): GeraiUnaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeraiUnaDatabase::class.java,
                    "gerai_una_database.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: GeraiUnaDatabase) {
            val productDao = database.productDao()
            val contactDao = database.contactDao()
            val settingsDao = database.appSettingsDao()
            val extDao = database.extendedPosDao()

            // Default store profile
            extDao.upsertStoreProfile(
                StoreProfileEntity(
                    id = 1L,
                    name = "GERAI UNA SMART POS",
                    address = "Jl. Garuda No. 8, Jakarta",
                    phone = "081234567890",
                    businessType = "keduanya",
                    ppnBp = 0,
                    receiptFooter = "Terima Kasih Telah Berbelanja di Gerai Una!"
                )
            )

            // Default users & permissions
            val defaultPinHash = hashPin("123456")
            val ownerId = extDao.insertUser(
                UserAccountEntity(
                    name = "Egis Bahricha (Owner)",
                    username = "owner",
                    role = "owner",
                    pinHash = defaultPinHash,
                    isActive = true
                )
            )
            val kasirId = extDao.insertUser(
                UserAccountEntity(
                    name = "Kasir 1",
                    username = "kasir1",
                    role = "kasir",
                    pinHash = defaultPinHash,
                    isActive = true
                )
            )
            val allMenus = listOf("POS_KASIR", "RIWAYAT_TRANSAKSI", "PRODUK", "PEMBELIAN", "STOK_INVENTARIS", "MULTI_KAS", "TARIF_ADMIN", "KEUANGAN", "PENGELUARAN", "UTANG_PIUTANG", "LAPORAN", "AI_CHAT", "WHATSAPP", "KONTAK", "PENGATURAN")
            allMenus.forEach { menu ->
                extDao.insertMenuPermission(MenuPermissionEntity(userId = ownerId, menuKey = menu, allowed = true))
                val kasirAllowed = menu in listOf("POS_KASIR", "RIWAYAT_TRANSAKSI", "PRODUK", "STOK_INVENTARIS", "TARIF_ADMIN", "KONTAK")
                extDao.insertMenuPermission(MenuPermissionEntity(userId = kasirId, menuKey = menu, allowed = kasirAllowed))
            }

            // Default printer & device
            extDao.insertPrinter(
                PrinterEntity(
                    name = "Printer Thermal 58mm",
                    bluetoothAddress = "00:11:22:33:44:55",
                    type = "struk",
                    language = "escpos",
                    widthMm = 58,
                    autoCut = false,
                    openDrawer = false,
                    quickPrint = true,
                    isActive = true
                )
            )
            extDao.insertCashierDevice(
                CashierDeviceEntity(
                    name = "Kasir Utama Gerai Una",
                    role = "utama",
                    isActive = true
                )
            )

            // Default categories
            val categories = listOf(
                CategoryEntity(name = "Makanan & Minuman", icon = "restaurant"),
                CategoryEntity(name = "Sembako & Kebutuhan Pokok", icon = "store"),
                CategoryEntity(name = "Kebersihan & Rumah Tangga", icon = "cleaning_services"),
                CategoryEntity(name = "Rokok & Tembakau", icon = "smoking_rooms"),
                CategoryEntity(name = "Pulsa & Jasa Kasir", icon = "phone_android")
            )
            categories.forEach { productDao.insertCategory(it) }

            // Default products
            val p1Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899276111101",
                    name = "Aqua Botol 600 ml",
                    category = "Makanan & Minuman",
                    baseUnit = "botol",
                    purchasePrice = 3000.0,
                    sellingPrice = 4000.0,
                    strikePrice = 4500.0,
                    stock = 48.0,
                    minStock = 12.0,
                    brand = "Danone"
                )
            )
            val p2Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899886620021",
                    name = "Indomie Goreng Original 85g",
                    category = "Makanan & Minuman",
                    baseUnit = "bungkus",
                    purchasePrice = 2750.0,
                    sellingPrice = 3500.0,
                    stock = 80.0,
                    minStock = 20.0,
                    brand = "Indofood",
                    hasVariants = true
                )
            )
            val p3Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899999955512",
                    name = "Rinso Cair Konsentrat 800 ml",
                    category = "Kebersihan & Rumah Tangga",
                    baseUnit = "pouch",
                    purchasePrice = 14000.0,
                    sellingPrice = 18000.0,
                    stock = 15.0,
                    minStock = 5.0,
                    brand = "Unilever"
                )
            )
            val p4Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899100123456",
                    name = "Minyak Goreng Bimoli 2 Liter",
                    category = "Sembako & Kebutuhan Pokok",
                    baseUnit = "pouch",
                    purchasePrice = 33000.0,
                    sellingPrice = 38000.0,
                    stock = 12.0,
                    minStock = 4.0,
                    brand = "Bimoli"
                )
            )
            val p5Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899275300001",
                    name = "Gula Pasir Gulaku 1 Kg",
                    category = "Sembako & Kebutuhan Pokok",
                    baseUnit = "kg",
                    purchasePrice = 15500.0,
                    sellingPrice = 18000.0,
                    stock = 25.0,
                    minStock = 6.0,
                    brand = "Gulaku",
                    allowDecimalQty = true
                )
            )
            val p6Id = productDao.insertProduct(
                ProductEntity(
                    barcode = "899123456789",
                    name = "Kopi Kapal Api Spesial 165g",
                    category = "Makanan & Minuman",
                    baseUnit = "bungkus",
                    purchasePrice = 11000.0,
                    sellingPrice = 13500.0,
                    stock = 30.0,
                    minStock = 5.0,
                    brand = "Kapal Api"
                )
            )

            // Seed sample Product Variants for Indomie
            extDao.insertVariant(ProductVariantEntity(productId = p2Id, name = "Original 85g", barcode = "899886620021-A", sellingPrice = 3500.0, purchasePrice = 2750.0, stock = 50.0))
            extDao.insertVariant(ProductVariantEntity(productId = p2Id, name = "Rendang 91g", barcode = "899886620021-B", sellingPrice = 3800.0, purchasePrice = 2900.0, stock = 30.0))

            // Seed sample Wholesale Tiers (harga_bertingkat) for Indomie & Gula
            extDao.insertWholesaleTier(WholesaleTierEntity(productId = p2Id, unitName = "bungkus", minQty = 5.0, pricePerUnit = 3200.0))
            extDao.insertWholesaleTier(WholesaleTierEntity(productId = p2Id, unitName = "bungkus", minQty = 10.0, pricePerUnit = 3000.0))

            // Seed sample Product Batches (batch_produk)
            extDao.insertBatch(ProductBatchEntity(productId = p1Id, batchCode = "AQ-2611", expiryDateStr = "2027-06-30", qty = 48.0))
            extDao.insertBatch(ProductBatchEntity(productId = p2Id, batchCode = "IDM-2610", expiryDateStr = "2026-12-10", qty = 80.0))

            // Seed sample Bundle Package (paket)
            val bundleId = extDao.insertBundle(BundlePackageEntity(name = "Paket Sarapan Hemat", sellingPrice = 10000.0, isActive = true))
            extDao.insertBundleItem(BundleItemEntity(bundleId = bundleId, productId = p2Id, qty = 2.0))
            extDao.insertBundleItem(BundleItemEntity(bundleId = bundleId, productId = p1Id, qty = 1.0))

            // Seed sample Promos
            extDao.insertPromo(
                PromoEntity(
                    promoType = "voucher",
                    name = "Voucher Belanja Hemat",
                    code = "HEMAT5RB",
                    minPurchase = 50000.0,
                    discountAmount = 5000.0,
                    isActive = true
                )
            )
            extDao.insertPromo(
                PromoEntity(
                    promoType = "beli_x_gratis_y",
                    name = "Beli 10 Gratis 1 Minuman",
                    code = "BELI10GRATIS1",
                    minPurchase = 0.0,
                    discountAmount = 4000.0,
                    buyQty = 10,
                    freeQty = 1,
                    isActive = true
                )
            )

            // Multi-unit conversions
            productDao.insertConversion(
                UnitConversionEntity(
                    productId = p1Id,
                    unitName = "dus",
                    multiplier = 24.0,
                    customSellingPrice = 90000.0,
                    customPurchasePrice = 72000.0
                )
            )
            productDao.insertConversion(
                UnitConversionEntity(
                    productId = p2Id,
                    unitName = "karton",
                    multiplier = 40.0,
                    customSellingPrice = 135000.0,
                    customPurchasePrice = 110000.0
                )
            )

            // Product Aliases
            val aliases = listOf(
                ProductAliasEntity(productId = p1Id, alias = "aquaa"),
                ProductAliasEntity(productId = p1Id, alias = "akua"),
                ProductAliasEntity(productId = p1Id, alias = "aqua"),
                ProductAliasEntity(productId = p1Id, alias = "aqua botol"),
                ProductAliasEntity(productId = p2Id, alias = "indomi"),
                ProductAliasEntity(productId = p2Id, alias = "indomie"),
                ProductAliasEntity(productId = p2Id, alias = "indomie goreng"),
                ProductAliasEntity(productId = p3Id, alias = "rinso"),
                ProductAliasEntity(productId = p3Id, alias = "rinso kcl"),
                ProductAliasEntity(productId = p3Id, alias = "rinso cair"),
                ProductAliasEntity(productId = p4Id, alias = "bimoli"),
                ProductAliasEntity(productId = p4Id, alias = "minyak goreng"),
                ProductAliasEntity(productId = p4Id, alias = "minyak bimoli"),
                ProductAliasEntity(productId = p5Id, alias = "gulak"),
                ProductAliasEntity(productId = p5Id, alias = "gula"),
                ProductAliasEntity(productId = p5Id, alias = "gula pasir"),
                ProductAliasEntity(productId = p6Id, alias = "kapal api"),
                ProductAliasEntity(productId = p6Id, alias = "kopi kapal api")
            )
            aliases.forEach { productDao.insertAlias(it) }

            // Default Contacts
            contactDao.insertSupplier(
                SupplierEntity(
                    name = "Grosir Berkah Sentosa",
                    phone = "081234567890",
                    address = "Pasar Induk Blok A No. 12",
                    notes = "Supplier sembako, minuman, sabun",
                    totalDebt = 0.0
                )
            )
            contactDao.insertSupplier(
                SupplierEntity(
                    name = "Distributor Utama Indofood",
                    phone = "081987654321",
                    address = "Kawasan Industri Pergudangan 3",
                    notes = "Supplier aneka mie dan bumbu",
                    totalDebt = 0.0
                )
            )
            contactDao.insertCustomer(
                CustomerEntity(
                    name = "Ibu Siti (Warung Sebelah)",
                    phone = "082133445566",
                    address = "Jl. Melati No. 5",
                    notes = "Langganan rutin, kadang bayar tempo 3 hari",
                    totalReceivable = 0.0,
                    depositBalance = 50000.0,
                    loyaltyPoints = 25
                )
            )

            // Settings
            settingsDao.setSetting(AppSettingsEntity("store_name", "GERAI UNA SMART POS"))
            settingsDao.setSetting(AppSettingsEntity("store_creator", "EGIS BAHRICHA"))
            settingsDao.setSetting(AppSettingsEntity("store_address", "Jl. Garuda No. 8, Jakarta"))
            settingsDao.setSetting(AppSettingsEntity("store_phone", "081234567890"))
            settingsDao.setSetting(AppSettingsEntity("store_receipt_footer", "Terima Kasih Telah Berbelanja di Gerai Una!"))
            settingsDao.setSetting(AppSettingsEntity("owner_authorized_phone", "081234567890"))
            settingsDao.setSetting(AppSettingsEntity("active_role", "Owner"))
            settingsDao.setSetting(AppSettingsEntity("active_user_name", "Egis Bahricha (Owner)"))
            settingsDao.setSetting(AppSettingsEntity("whatsapp_webhook_status", "ACTIVE"))
            settingsDao.setSetting(AppSettingsEntity("tax_percentage", "0.0"))

            // Seed Multi Kas Accounts & Opening Mutations
            val cashAccountDao = database.cashAccountDao()
            val cashStoreId = cashAccountDao.insertCashAccount(
                CashAccountEntity(
                    accountName = "Kas Toko",
                    balance = 1500000.0,
                    accountType = "CASH",
                    color = "#16A34A",
                    notes = "Uang fisik laci kasir gerai utama",
                    isDefault = true,
                    sortOrder = 1
                )
            )
            val depositPulsaId = cashAccountDao.insertCashAccount(
                CashAccountEntity(
                    accountName = "Deposit Pulsa",
                    balance = 750000.0,
                    accountType = "PULSA_DEPOSIT",
                    color = "#EA580C",
                    notes = "Saldo server pulsa / chip konter",
                    sortOrder = 2
                )
            )
            val brilinkId = cashAccountDao.insertCashAccount(
                CashAccountEntity(
                    accountName = "Saldo BRILink / Agen Bank",
                    balance = 4250000.0,
                    accountType = "BANK_AGENT",
                    color = "#2563EB",
                    notes = "Saldo operasional transaksi transfer agen",
                    sortOrder = 3
                )
            )

            extDao.insertCashMutation(CashMutationEntity(cashAccountId = cashStoreId, accountName = "Kas Toko", amount = 1500000.0, mutationType = "saldo_awal", notes = "Saldo awal kas toko"))
            extDao.insertCashMutation(CashMutationEntity(cashAccountId = depositPulsaId, accountName = "Deposit Pulsa", amount = 750000.0, mutationType = "saldo_awal", notes = "Saldo awal deposit pulsa"))
            extDao.insertCashMutation(CashMutationEntity(cashAccountId = brilinkId, accountName = "Saldo BRILink / Agen Bank", amount = 4250000.0, mutationType = "saldo_awal", notes = "Saldo awal agen bank"))

            // Seed Admin Tier Services & Ranges (Konter PPOB)
            val adminTierDao = database.adminTierDao()
            val serviceTransferBankId = adminTierDao.insertService(
                AdminTierServiceEntity(
                    serviceName = "Transfer Antar Bank",
                    serviceCode = "TRANSFER_BANK"
                )
            )
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTransferBankId, minAmount = 0.0, maxAmount = 1000000.0, storeAdminFee = 5000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTransferBankId, minAmount = 1000001.0, maxAmount = 3000000.0, storeAdminFee = 8000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTransferBankId, minAmount = 3000001.0, maxAmount = 5000000.0, storeAdminFee = 10000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTransferBankId, minAmount = 5000001.0, maxAmount = 10000000.0, storeAdminFee = 15000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))

            val serviceTarikTunaiId = adminTierDao.insertService(
                AdminTierServiceEntity(
                    serviceName = "Tarik Tunai Kartu / Rekening",
                    serviceCode = "TARIK_TUNAI"
                )
            )
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTarikTunaiId, minAmount = 0.0, maxAmount = 1000000.0, storeAdminFee = 5000.0, providerAdminFee = 0.0, agentCommission = 1000.0))
            adminTierDao.insertRange(AdminTierRangeEntity(serviceId = serviceTarikTunaiId, minAmount = 1000001.0, maxAmount = 3000000.0, storeAdminFee = 10000.0, providerAdminFee = 0.0, agentCommission = 2000.0))

            // Seed Digital Products linked to Tarif Groups & Multi Kas Accounts
            productDao.insertProduct(
                ProductEntity(
                    barcode = "DIGI-TRF-001",
                    name = "Jasa Transfer Uang Agen",
                    category = "Pulsa & Jasa Kasir",
                    baseUnit = "transaksi",
                    purchasePrice = 0.0,
                    sellingPrice = 0.0,
                    stock = 999.0,
                    productType = "DIGITAL",
                    isUnlimitedStock = true,
                    isFlexiblePrice = true,
                    hasNoStock = true,
                    adminTierGroupId = serviceTransferBankId,
                    serviceCashAccountId = brilinkId,
                    serviceDirection = "SETOR"
                )
            )
            productDao.insertProduct(
                ProductEntity(
                    barcode = "DIGI-TRK-001",
                    name = "Jasa Tarik Tunai Agen",
                    category = "Pulsa & Jasa Kasir",
                    baseUnit = "transaksi",
                    purchasePrice = 0.0,
                    sellingPrice = 0.0,
                    stock = 999.0,
                    productType = "DIGITAL",
                    isUnlimitedStock = true,
                    isFlexiblePrice = true,
                    hasNoStock = true,
                    adminTierGroupId = serviceTarikTunaiId,
                    serviceCashAccountId = brilinkId,
                    serviceDirection = "TARIK"
                )
            )
            productDao.insertProduct(
                ProductEntity(
                    barcode = "DIGI-PLN-001",
                    name = "Token Listrik PLN Prabayar",
                    category = "Pulsa & Jasa Kasir",
                    baseUnit = "token",
                    purchasePrice = 0.0,
                    sellingPrice = 0.0,
                    stock = 999.0,
                    productType = "DIGITAL",
                    isUnlimitedStock = true,
                    isFlexiblePrice = true,
                    hasNoStock = true,
                    serviceCashAccountId = depositPulsaId,
                    serviceDirection = "SETOR"
                )
            )
            productDao.insertProduct(
                ProductEntity(
                    barcode = "DIGI-PLS-001",
                    name = "Pulsa All Operator",
                    category = "Pulsa & Jasa Kasir",
                    baseUnit = "pulsa",
                    purchasePrice = 0.0,
                    sellingPrice = 0.0,
                    stock = 999.0,
                    productType = "DIGITAL",
                    isUnlimitedStock = true,
                    isFlexiblePrice = true,
                    hasNoStock = true,
                    serviceCashAccountId = depositPulsaId,
                    serviceDirection = "SETOR"
                )
            )

            // Seed example Tier Pricing for Aqua Botol
            productDao.insertTierPrice(ProductTierPriceEntity(productId = p1Id, quantity = 1.0, totalPrice = 4000.0))
            productDao.insertTierPrice(ProductTierPriceEntity(productId = p1Id, quantity = 2.0, totalPrice = 7000.0))
            productDao.insertTierPrice(ProductTierPriceEntity(productId = p1Id, quantity = 3.0, totalPrice = 10000.0))
        }
    }
}
