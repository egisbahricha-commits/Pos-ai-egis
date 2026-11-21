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
        CloudSyncMappingEntity::class,
        BarcodeScanLogEntity::class
    ],
    version = 7,
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_name ON products(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_variants_barcode ON product_variants(barcode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_variants_productId ON product_variants(productId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS barcode_scan_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        barcode TEXT NOT NULL,
                        productId INTEGER,
                        variantId INTEGER,
                        productName TEXT NOT NULL,
                        matchedPrice REAL NOT NULL,
                        matchedStock REAL NOT NULL,
                        matched INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        scannedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_barcode_scan_logs_barcode ON barcode_scan_logs(barcode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_barcode_scan_logs_scannedAt ON barcode_scan_logs(scannedAt)")
            }
        }

        fun getDatabase(context: Context): GeraiUnaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeraiUnaDatabase::class.java,
                    "gerai_una_database.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
            val settingsDao = database.appSettingsDao()
            val extDao = database.extendedPosDao()
            val cashAccountDao = database.cashAccountDao()

            // Clean neutral store profile (updated when Admin Utama completes first-time setup)
            if (extDao.getStoreProfileSync() == null) {
                extDao.upsertStoreProfile(
                    StoreProfileEntity(
                        id = 1L,
                        name = "Toko Saya",
                        address = "",
                        phone = "",
                        businessType = "keduanya",
                        ppnBp = 0,
                        receiptFooter = "Terima kasih telah berbelanja!"
                    )
                )
            }

            // Starter product categories (no dummy products)
            if (productDao.getAllCategoriesList().isEmpty()) {
                val categories = listOf(
                    CategoryEntity(name = "Umum", icon = "store"),
                    CategoryEntity(name = "Makanan & Minuman", icon = "restaurant"),
                    CategoryEntity(name = "Sembako & Kebutuhan Pokok", icon = "shopping_basket"),
                    CategoryEntity(name = "Elektronik & Aksesoris", icon = "devices"),
                    CategoryEntity(name = "Jasa & Digital", icon = "phone_android")
                )
                categories.forEach { productDao.insertCategory(it) }
            }

            // Single clean default cash account with Rp 0 initial balance
            if (cashAccountDao.getAllAccountsList().isEmpty()) {
                cashAccountDao.insertCashAccount(
                    CashAccountEntity(
                        accountName = "Kas Toko",
                        balance = 0.0,
                        accountType = "CASH",
                        color = "#16A34A",
                        notes = "Kas utama laci kasir",
                        isDefault = true,
                        sortOrder = 1
                    )
                )
            }

            // Clean baseline settings (user_accounts intentionally empty until Admin Utama setup)
            settingsDao.setSetting(AppSettingsEntity("db_initialized", "true"))
            settingsDao.setSetting(AppSettingsEntity("store_name", "Toko Saya"))
            settingsDao.setSetting(AppSettingsEntity("store_address", ""))
            settingsDao.setSetting(AppSettingsEntity("store_phone", ""))
            settingsDao.setSetting(AppSettingsEntity("store_receipt_footer", "Terima kasih telah berbelanja!"))
            settingsDao.setSetting(AppSettingsEntity("tax_percentage", "0.0"))
            settingsDao.setSetting(AppSettingsEntity("whatsapp_webhook_status", "ACTIVE"))
        }
    }
}
