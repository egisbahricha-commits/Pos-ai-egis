package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeraiUnaAIEngine
import com.example.data.backup.KasGoBackupImporter
import com.example.data.backup.KasGoImportOptions
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.AdminTierRangeEntity
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.AdminTierWithRanges
import com.example.data.entity.BarcodeScanLogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProductAliasEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.entity.UserAccountEntity
import com.example.data.pricing.ProfitLossCalculator
import com.example.data.pricing.ProfitLossPeriod
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.PosRepository
import com.example.ui.viewmodel.ALL_PERMISSION_MENU_KEYS
import com.example.ui.viewmodel.BarcodeLookupOutcome
import com.example.ui.viewmodel.LocalAuthResult
import com.example.ui.viewmodel.authenticateLocalUser
import com.example.ui.viewmodel.resolveBarcodeToCartMatch
import com.example.ui.viewmodel.validateAdminSetupInput
import com.example.ui.viewmodel.validateManualProductInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ExampleRobolectricTest {

    private fun createDummyEngine(): GeraiUnaAIEngine {
        val dummyRepo = PosRepository(
            object : GeraiUnaDatabase() {
                override fun productDao() = throw UnsupportedOperationException()
                override fun saleDao() = throw UnsupportedOperationException()
                override fun purchaseDao() = throw UnsupportedOperationException()
                override fun stockMovementDao() = throw UnsupportedOperationException()
                override fun financeDao() = throw UnsupportedOperationException()
                override fun contactDao() = throw UnsupportedOperationException()
                override fun aiDao() = throw UnsupportedOperationException()
                override fun appSettingsDao() = throw UnsupportedOperationException()
                override fun cashAccountDao() = throw UnsupportedOperationException()
                override fun adminTierDao() = throw UnsupportedOperationException()
                override fun shiftDao() = throw UnsupportedOperationException()
                override fun extendedPosDao() = throw UnsupportedOperationException()
                override fun clearAllTables() {}
                override fun createInvalidationTracker() = throw UnsupportedOperationException()
                override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
            }
        )
        return GeraiUnaAIEngine(dummyRepo)
    }

    @Test
    fun `test AI amount parsing logic`() {
        val aiEngine = createDummyEngine()

        assertEquals(250000.0, aiEngine.extractAmount("pengeluaran listrik 250 ribu"), 0.01)
        assertEquals(45000.0, aiEngine.extractAmount("harga 45rb per dus"), 0.01)
        assertEquals(110000.0, aiEngine.extractAmount("harga 110 ribu"), 0.01)
        assertEquals(1500000.0, aiEngine.extractAmount("total 1.5jt"), 0.01)
        assertEquals(500.0, aiEngine.extractAmount("bayar admin Rp 500"), 0.01)
        assertEquals(1000.0, aiEngine.extractAmount("admin Rp1.000"), 0.01)
    }

    @Test
    fun `test AI typo and fuzzy matching`() {
        val aiEngine = createDummyEngine()

        val products = listOf(
            ProductEntity(id = 1L, name = "Aqua Botol 600 ml"),
            ProductEntity(id = 2L, name = "Indomie Goreng Original 85g"),
            ProductEntity(id = 3L, name = "Rinso Cair Konsentrat 800 ml")
        )
        val aliases = listOf(
            ProductAliasEntity(id = 1L, productId = 1L, alias = "aquaa"),
            ProductAliasEntity(id = 2L, productId = 2L, alias = "indomi")
        )

        val matchedAqua = aiEngine.findBestMatchingProduct("aquaa", products, aliases)
        assertNotNull(matchedAqua)
        assertEquals("Aqua Botol 600 ml", matchedAqua?.name)

        val matchedIndomie = aiEngine.findBestMatchingProduct("indomi", products, aliases)
        assertNotNull(matchedIndomie)
        assertEquals("Indomie Goreng Original 85g", matchedIndomie?.name)
    }

    @Test
    fun `test pin hash consistency`() {
        val h1 = GeraiUnaDatabase.hashPin("123456")
        val h2 = GeraiUnaDatabase.hashPin("123456")
        assertEquals(64, h1.length)
        assertEquals(h1, h2)
    }

    @Test
    fun `test firebase cloud models default values and sync state`() {
        val storeDoc = com.example.firebase.UserStoreProfileDoc(
            id = "uid_owner_1",
            ownerId = "uid_owner_1",
            name = "Gerai Una",
            ownerEmail = "egisbahricha@gmail.com"
        )
        assertEquals("uid_owner_1", storeDoc.ownerId)
        assertEquals("Gerai Una", storeDoc.name)

        val accountDoc = com.example.firebase.CashAccountDoc(
            id = "acc_1",
            ownerId = "uid_owner_1",
            deviceId = "dev_123",
            localId = 1L,
            accountName = "Kas Toko",
            balance = 1500000.0
        )
        assertEquals("Kas Toko", accountDoc.accountName)
        assertEquals(1500000.0, accountDoc.balance, 0.01)

        val authState = com.example.firebase.FirebaseAuthUiState(
            hasEnteredApp = true,
            isCloudSignedIn = true,
            userEmail = "egisbahricha@gmail.com"
        )
        assertEquals(true, authState.isCloudSignedIn)
        assertEquals("egisbahricha@gmail.com", authState.userEmail)
    }

    @Test
    fun `test TierPricingResolver range detection boundaries and overlap`() {
        val service = AdminTierServiceEntity(id = 1L, serviceName = "Transfer Bank", serviceCode = "TF_BANK")
        val ranges = listOf(
            AdminTierRangeEntity(id = 10L, serviceId = 1L, minAmount = 1.0, maxAmount = 1_000_000.0, storeAdminFee = 5_000.0, providerAdminFee = 2_500.0, agentCommission = 1_000.0),
            AdminTierRangeEntity(id = 11L, serviceId = 1L, minAmount = 1_000_001.0, maxAmount = 3_000_000.0, storeAdminFee = 10_000.0, providerAdminFee = 3_000.0, agentCommission = 1_500.0),
            AdminTierRangeEntity(id = 12L, serviceId = 1L, minAmount = 5_000_001.0, maxAmount = 0.0, storeAdminFee = 20_000.0, providerAdminFee = 5_000.0, agentCommission = 2_000.0)
        )
        val tierGroup = AdminTierWithRanges(service = service, ranges = ranges)

        // 345.000 -> matches 1..1.000.000 => 5.000
        val calc345k = TierPricingResolver.calculate(tierGroup, 345_000.0)
        assertTrue(calc345k.isAutoDetected)
        assertEquals(5_000.0, calc345k.storeAdminFee, 0.01)
        assertEquals(350_000.0, calc345k.totalCustomerPayable, 0.01)
        assertEquals(3_500.0, calc345k.netProfit, 0.01)

        // Exact boundary checks
        assertEquals(5_000.0, TierPricingResolver.calculate(tierGroup, 1_000_000.0).storeAdminFee, 0.01)
        assertEquals(10_000.0, TierPricingResolver.calculate(tierGroup, 1_000_001.0).storeAdminFee, 0.01)
        assertEquals(10_000.0, TierPricingResolver.calculate(tierGroup, 1_500_000.0).storeAdminFee, 0.01)

        // Open-ended upper bound (maxAmount == 0.0)
        assertEquals(20_000.0, TierPricingResolver.calculate(tierGroup, 12_500_000.0).storeAdminFee, 0.01)

        // Gap / unmatched nominal (4.000.000 between 3jt and 5jt+1)
        val unmatched = TierPricingResolver.calculate(tierGroup, 4_000_000.0)
        assertNull(unmatched.matchedRange)
        assertFalse(unmatched.isAutoDetected)
        assertEquals(0.0, unmatched.storeAdminFee, 0.01)

        // Manual override takes precedence
        val overridden = TierPricingResolver.calculate(tierGroup, 345_000.0, manualAdminFeeOverride = 7_500.0)
        assertFalse(overridden.isAutoDetected)
        assertEquals(7_500.0, overridden.storeAdminFee, 0.01)

        // Overlap validation
        assertTrue(TierPricingResolver.hasOverlap(ranges, 500_000.0, 1_500_000.0))
        assertTrue(TierPricingResolver.hasOverlap(ranges, 6_000_000.0, 0.0))
        assertFalse(TierPricingResolver.hasOverlap(ranges, 3_000_001.0, 5_000_000.0))
        assertFalse(TierPricingResolver.hasOverlap(ranges, 1.0, 1_000_000.0, excludeRangeId = 10L))
    }

    @Test
    fun `test dynamic multi-tier add and remove within single digital service`() {
        val service = AdminTierServiceEntity(id = 2L, serviceName = "Top Up E-Wallet", serviceCode = "EWALLET")
        val mutableRanges = mutableListOf(
            AdminTierRangeEntity(id = 1L, serviceId = 2L, minAmount = 1.0, maxAmount = 1_000_000.0, storeAdminFee = 5_000.0, providerAdminFee = 0.0, agentCommission = 0.0),
            AdminTierRangeEntity(id = 2L, serviceId = 2L, minAmount = 1_000_001.0, maxAmount = 3_000_000.0, storeAdminFee = 10_000.0, providerAdminFee = 0.0, agentCommission = 0.0)
        )

        // Tambahkan 2 tingkat admin tambahan
        mutableRanges.add(
            AdminTierRangeEntity(id = 3L, serviceId = 2L, minAmount = 3_000_001.0, maxAmount = 5_000_000.0, storeAdminFee = 15_000.0, providerAdminFee = 0.0, agentCommission = 0.0)
        )
        mutableRanges.add(
            AdminTierRangeEntity(id = 4L, serviceId = 2L, minAmount = 5_000_001.0, maxAmount = 0.0, storeAdminFee = 20_000.0, providerAdminFee = 0.0, agentCommission = 0.0)
        )

        val expandedGroup = AdminTierWithRanges(service = service, ranges = mutableRanges.toList())
        assertEquals(5_000.0, TierPricingResolver.calculate(expandedGroup, 500_000.0).storeAdminFee, 0.01)
        assertEquals(10_000.0, TierPricingResolver.calculate(expandedGroup, 2_500_000.0).storeAdminFee, 0.01)
        assertEquals(15_000.0, TierPricingResolver.calculate(expandedGroup, 4_200_000.0).storeAdminFee, 0.01)
        assertEquals(20_000.0, TierPricingResolver.calculate(expandedGroup, 10_000_000.0).storeAdminFee, 0.01)

        // Kurangkan / hapus tingkat ke-2 (1.000.001 - 3.000.000)
        mutableRanges.removeAll { it.id == 2L }
        val reducedGroup = AdminTierWithRanges(service = service, ranges = mutableRanges.toList())
        val removedNominal = TierPricingResolver.calculate(reducedGroup, 2_500_000.0)
        assertNull(removedNominal.matchedRange)
        assertEquals(0.0, removedNominal.storeAdminFee, 0.01)
        // Tingkat lainnya tetap aktif
        assertEquals(15_000.0, TierPricingResolver.calculate(reducedGroup, 4_200_000.0).storeAdminFee, 0.01)
    }

    @Test
    fun `test ProfitLossCalculator harian mingguan bulanan tahunan`() {
        val idLocale = Locale("id", "ID")
        val refCal = Calendar.getInstance(idLocale).apply {
            set(2026, Calendar.NOVEMBER, 17, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refTime = refCal.timeInMillis

        // Kemarin: 16 Nov 2026 (masih dalam minggu yg sama 16-22 Nov 2026)
        val yesterdayCal = (refCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        // Bulan lalu: 10 Okt 2026 (masih dalam tahun 2026)
        val lastMonthCal = Calendar.getInstance(idLocale).apply {
            set(2026, Calendar.OCTOBER, 10, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val products = listOf(
            ProductEntity(id = 1L, name = "Beras 5kg", purchasePrice = 60_000.0, sellingPrice = 75_000.0, productType = "FISIK"),
            ProductEntity(id = 2L, name = "Transfer Bank", purchasePrice = 0.0, sellingPrice = 0.0, productType = "DIGITAL", isFlexiblePrice = true)
        )

        val sales = listOf(
            // Hari ini (17 Nov): jual 2 Beras (150rb, HPP 120rb -> laba 30rb) + Transfer 345rb (admin toko 5rb, penyedia 2.5rb, fee 1rb -> laba 3.5rb)
            SaleEntity(
                id = 101L,
                invoiceNumber = "TRX-001",
                date = refTime,
                subtotal = 495_000.0,
                finalAmount = 500_000.0,
                storeAdminFee = 5_000.0,
                providerAdminFee = 2_500.0,
                agentCommission = 1_000.0,
                status = "COMPLETED"
            ),
            // Hari ini REFUNDED (harus diabaikan)
            SaleEntity(
                id = 102L,
                invoiceNumber = "TRX-002",
                date = refTime,
                subtotal = 100_000.0,
                finalAmount = 100_000.0,
                status = "REFUNDED"
            ),
            // Kemarin (16 Nov): jual 1 Beras (75rb, HPP 60rb -> laba 15rb)
            SaleEntity(
                id = 103L,
                invoiceNumber = "TRX-003",
                date = yesterdayCal.timeInMillis,
                subtotal = 75_000.0,
                finalAmount = 75_000.0,
                status = "COMPLETED"
            ),
            // Bulan lalu (10 Okt): jual 10 Beras (750rb, HPP 600rb -> laba 150rb)
            SaleEntity(
                id = 104L,
                invoiceNumber = "TRX-004",
                date = lastMonthCal.timeInMillis,
                subtotal = 750_000.0,
                finalAmount = 750_000.0,
                status = "COMPLETED"
            )
        )

        val saleItems = listOf(
            SaleItemEntity(id = 1L, saleId = 101L, productId = 1L, productName = "Beras 5kg", quantity = 2.0, unit = "pcs", unitPrice = 75_000.0, subtotal = 150_000.0, purchasePriceSnapshot = 60_000.0),
            SaleItemEntity(id = 2L, saleId = 101L, productId = 2L, productName = "Transfer Bank", quantity = 1.0, unit = "trx", unitPrice = 345_000.0, subtotal = 345_000.0, purchasePriceSnapshot = 0.0, serviceNominal = 345_000.0, storeAdminFee = 5_000.0, providerAdminFee = 2_500.0, agentCommission = 1_000.0),
            SaleItemEntity(id = 3L, saleId = 102L, productId = 1L, productName = "Beras 5kg", quantity = 1.0, unit = "pcs", unitPrice = 100_000.0, subtotal = 100_000.0, purchasePriceSnapshot = 60_000.0),
            SaleItemEntity(id = 4L, saleId = 103L, productId = 1L, productName = "Beras 5kg", quantity = 1.0, unit = "pcs", unitPrice = 75_000.0, subtotal = 75_000.0, purchasePriceSnapshot = 60_000.0),
            SaleItemEntity(id = 5L, saleId = 104L, productId = 1L, productName = "Beras 5kg", quantity = 10.0, unit = "pcs", unitPrice = 75_000.0, subtotal = 750_000.0, purchasePriceSnapshot = 60_000.0)
        )

        val expenses = listOf(
            ExpenseEntity(id = 1L, category = "Listrik", amount = 10_000.0, date = refTime),
            ExpenseEntity(id = 2L, category = "Sewa", amount = 25_000.0, date = yesterdayCal.timeInMillis)
        )
        val incomes = listOf(
            IncomeEntity(id = 1L, source = "Jasa Parkir", amount = 5_000.0, date = refTime)
        )

        // 1. HARIAN (17 Nov): netSales 500k, COGS 120k + 345k = 465k, prov 2.5k => grossProfit 32.5k; + agent 1k + income 5k - expense 10k => netProfit 28.5k
        val daily = ProfitLossCalculator.calculate(ProfitLossPeriod.HARIAN, 0, sales, saleItems, expenses, incomes, products, refTime)
        assertEquals(1, daily.transactionCount)
        assertEquals(500_000.0, daily.netSales, 0.01)
        assertEquals(465_000.0, daily.cogs, 0.01)
        assertEquals(28_500.0, daily.netProfit, 0.01)
        assertEquals(150_000.0, daily.physicalSales, 0.01)
        assertEquals(30_000.0, daily.physicalNetProfit, 0.01)
        assertEquals(350_000.0, daily.digitalSales, 0.01)
        assertEquals(3_500.0, daily.digitalNetProfit, 0.01)
        assertEquals("", ProductEntity(name = "Test").imageUri)
        assertTrue(daily.isProfit)
        assertEquals(7, daily.subBuckets.size)

        // Kemarin (16 Nov): laba kotor 15k - expense 25k => RUGI -10k
        val yesterday = ProfitLossCalculator.calculate(ProfitLossPeriod.HARIAN, -1, sales, saleItems, expenses, incomes, products, refTime)
        assertEquals(-10_000.0, yesterday.netProfit, 0.01)
        assertFalse(yesterday.isProfit)

        // 2. MINGGUAN (16-22 Nov): hari ini (28.5k) + kemarin (-10k) = 18.5k
        val weekly = ProfitLossCalculator.calculate(ProfitLossPeriod.MINGGUAN, 0, sales, saleItems, expenses, incomes, products, refTime)
        assertEquals(2, weekly.transactionCount)
        assertEquals(575_000.0, weekly.netSales, 0.01)
        assertEquals(18_500.0, weekly.netProfit, 0.01)
        assertEquals(7, weekly.subBuckets.size)

        // 3. BULANAN (Nov 2026): sama dgn mingguan = 18.5k
        val monthly = ProfitLossCalculator.calculate(ProfitLossPeriod.BULANAN, 0, sales, saleItems, expenses, incomes, products, refTime)
        assertEquals(2, monthly.transactionCount)
        assertEquals(18_500.0, monthly.netProfit, 0.01)
        assertEquals(5, monthly.subBuckets.size)

        // 4. TAHUNAN (2026): Nov (18.5k) + Okt (150k) = 168.5k
        val yearly = ProfitLossCalculator.calculate(ProfitLossPeriod.TAHUNAN, 0, sales, saleItems, expenses, incomes, products, refTime)
        assertEquals(3, yearly.transactionCount)
        assertEquals(1_325_000.0, yearly.netSales, 0.01)
        assertEquals(168_500.0, yearly.netProfit, 0.01)
        assertEquals(12, yearly.subBuckets.size)
    }

    @Test
    fun `test KasGo kasgo backup inspect and import into Room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempBuildDir = File(context.cacheDir, "kasgo_test_builder").apply {
            deleteRecursively()
            mkdirs()
        }

        // 1. Create sample SQLite database.db with Indonesian KasGo-style tables
        val sampleDbFile = File(tempBuildDir, "database.db")
        val sqlite = SQLiteDatabase.openOrCreateDatabase(sampleDbFile, null)
        try {
            sqlite.execSQL("CREATE TABLE kategori (id INTEGER PRIMARY KEY, nama TEXT)")
            sqlite.execSQL("INSERT INTO kategori (id, nama) VALUES (1, 'Aksesoris HP')")

            sqlite.execSQL("CREATE TABLE produk (id INTEGER PRIMARY KEY, nama_produk TEXT, kode_barcode TEXT, kategori_id INTEGER, satuan TEXT, harga_beli REAL, harga_jual REAL, stok REAL, foto TEXT)")
            sqlite.execSQL("INSERT INTO produk VALUES (10, 'Kabel Data Type-C Fast', '899001122', 1, 'pcs', 12000, 25000, 18, 'photos/kabel.jpg')")

            sqlite.execSQL("CREATE TABLE pelanggan (id INTEGER PRIMARY KEY, nama TEXT, no_hp TEXT, piutang REAL)")
            sqlite.execSQL("INSERT INTO pelanggan VALUES (20, 'Pak Budi', '081299887766', 15000)")

            sqlite.execSQL("CREATE TABLE akun_kas (id INTEGER PRIMARY KEY, nama_akun TEXT, saldo REAL, tipe TEXT)")
            sqlite.execSQL("INSERT INTO akun_kas VALUES (1, 'Kas Konter Cell', 2500000, 'CASH')")

            sqlite.execSQL("CREATE TABLE transaksi (id INTEGER PRIMARY KEY, no_nota TEXT, tanggal TEXT, nama_pelanggan TEXT, total_akhir REAL, metode_bayar TEXT, status TEXT)")
            sqlite.execSQL("INSERT INTO transaksi VALUES (100, 'TRX-0099', '2026-11-18 10:30:00', 'Pak Budi', 50000, 'TUNAI', 'selesai')")

            sqlite.execSQL("CREATE TABLE detail_transaksi (id INTEGER PRIMARY KEY, transaksi_id INTEGER, produk_id INTEGER, nama_produk TEXT, qty REAL, harga_jual REAL, subtotal REAL, harga_beli REAL)")
            sqlite.execSQL("INSERT INTO detail_transaksi VALUES (1, 100, 10, 'Kabel Data Type-C Fast', 2, 25000, 50000, 12000)")

            sqlite.execSQL("CREATE TABLE pengeluaran (id INTEGER PRIMARY KEY, kategori TEXT, nominal REAL, catatan TEXT)")
            sqlite.execSQL("INSERT INTO pengeluaran VALUES (1, 'Token Listrik', 50000, 'Isi ulang konter')")
        } finally {
            sqlite.close()
        }

        // 2. Pack into kasgo-cell-data-test.kasgo ZIP archive
        val kasgoArchive = File(tempBuildDir, "kasgo-cell-data-18112026-1703.kasgo")
        ZipOutputStream(FileOutputStream(kasgoArchive)).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write("""{"appName":"KasGo Cell","versionName":"2.4","createdAt":"2026-11-18"}""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("prefs/kasgo_settings.json"))
            zos.write("""{"store_name":"UNA CELL BACKUP","store_phone":"0855123456","store_address":"Jl. Merdeka 1"}""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("prefs/kasgo_document_number.json"))
            zos.write("""{"last_trx":99}""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("photos/kabel.jpg"))
            zos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("database.db"))
            zos.write(sampleDbFile.readBytes())
            zos.closeEntry()
        }

        // 3. Inspect .kasgo
        val inspect = KasGoBackupImporter.inspectBackup(context, Uri.fromFile(kasgoArchive))
        assertTrue("Inspect failed: ${inspect.errorMessage}", inspect.isValid)
        assertEquals("UNA CELL BACKUP", inspect.storePreview.storeName)
        assertEquals(1, inspect.productCount)
        assertEquals(1, inspect.customerCount)
        assertEquals(1, inspect.saleCount)
        assertEquals(1, inspect.saleItemCount)
        assertEquals(1, inspect.expenseCount)
        assertEquals(1, inspect.photoCount)

        // 4. Import into Room in-memory DB
        val roomDb = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val summary = KasGoBackupImporter.importBackup(
                context = context,
                db = roomDb,
                inspectResult = inspect,
                options = KasGoImportOptions(replaceExisting = false, importStoreProfile = true)
            )
            assertEquals(1, summary.productsImported)
            assertEquals(1, summary.customersImported)
            assertEquals(1, summary.salesImported)
            assertEquals(1, summary.saleItemsImported)
            assertEquals(1, summary.expensesImported)
            assertEquals(1, summary.photosCopied)
            assertTrue(summary.storeProfileUpdated)

            val importedProducts = roomDb.productDao().getAllProductsList()
            val importedProduct = importedProducts.first { it.name == "Kabel Data Type-C Fast" }
            assertEquals("Aksesoris HP", importedProduct.category)
            assertEquals(25000.0, importedProduct.sellingPrice, 0.01)
            assertTrue(importedProduct.imageUri.isNotBlank())
            assertTrue(File(importedProduct.imageUri).exists())

            val store = roomDb.extendedPosDao().getStoreProfileSync()
            assertEquals("UNA CELL BACKUP", store?.name)
            assertEquals(99L, roomDb.extendedPosDao().getCounter("transaksi")?.value)

            val importedSales = roomDb.saleDao().getAllSales().first()
            assertEquals(1, importedSales.size)
            assertEquals("TRX-0099", importedSales.first().invoiceNumber)
            assertEquals(50000.0, importedSales.first().finalAmount, 0.01)
        } finally {
            roomDb.close()
            tempBuildDir.deleteRecursively()
        }
    }

    @Test
    fun `test resolveBarcodeToCartMatch matches product variant unit conversions and unknown codes`() {
        val simpleProduct = ProductEntity(
            id = 10L,
            name = "Aqua 600ml",
            barcode = "8991001001001",
            sellingPrice = 4000.0,
            isActive = true
        )
        val multiUnitProduct = ProductEntity(
            id = 20L,
            name = "Kopi Sachet",
            barcode = "SKU-KOPI-01",
            sellingPrice = 2000.0,
            isActive = true
        )
        val inactiveProduct = ProductEntity(
            id = 30L,
            name = "Produk Nonaktif",
            barcode = "9999999999",
            sellingPrice = 5000.0,
            isActive = false
        )
        val variantProduct = ProductEntity(
            id = 40L,
            name = "Kaos Polos",
            barcode = "KAOS-PARENT",
            hasVariants = true,
            sellingPrice = 45000.0,
            isActive = true
        )
        val variantXl = ProductVariantEntity(
            id = 401L,
            productId = 40L,
            name = "Hitam XL",
            barcode = "8994000401",
            sellingPrice = 48000.0,
            isActive = true
        )
        val conversionDus = UnitConversionEntity(
            id = 501L,
            productId = 20L,
            unitName = "dus",
            multiplier = 12.0,
            customSellingPrice = 22000.0
        )

        val products = listOf(simpleProduct, multiUnitProduct, inactiveProduct, variantProduct)
        val variants = listOf(variantXl)
        val conversions = listOf(conversionDus)

        // Exact match with surrounding whitespace & different casing
        val directMatch = resolveBarcodeToCartMatch(" 8991001001001 ", products, variants, conversions)
        assertTrue(directMatch is BarcodeLookupOutcome.AddedProduct)
        assertEquals(10L, (directMatch as BarcodeLookupOutcome.AddedProduct).product.id)

        // Variant barcode resolves straight to AddedVariant
        val variantMatch = resolveBarcodeToCartMatch("8994000401", products, variants, conversions)
        assertTrue(variantMatch is BarcodeLookupOutcome.AddedVariant)
        assertEquals(401L, (variantMatch as BarcodeLookupOutcome.AddedVariant).variant.id)

        // Multi-unit product triggers NeedsSelection
        val unitMatch = resolveBarcodeToCartMatch("sku-kopi-01", products, variants, conversions)
        assertTrue(unitMatch is BarcodeLookupOutcome.NeedsSelection)
        assertEquals(1, (unitMatch as BarcodeLookupOutcome.NeedsSelection).conversions.size)

        // Inactive or missing barcode returns NotFound
        assertTrue(resolveBarcodeToCartMatch("9999999999", products, variants, conversions) is BarcodeLookupOutcome.NotFound)
        val unknown = resolveBarcodeToCartMatch("404040404", products, variants, conversions)
        assertTrue(unknown is BarcodeLookupOutcome.NotFound)
        assertEquals("404040404", (unknown as BarcodeLookupOutcome.NotFound).barcode)
    }

    @Test
    fun `test Room product barcode schema, indexed lookup, stock decrement and scan log`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val productDao = db.productDao()
            val extDao = db.extendedPosDao()

            val insertedId = productDao.insertProduct(
                ProductEntity(
                    name = "Teh Botol Sosro 450ml",
                    barcode = "8996001600123",
                    sellingPrice = 5500.0,
                    purchasePrice = 4100.0,
                    stock = 12.0,
                    isActive = true
                )
            )
            extDao.insertVariant(
                ProductVariantEntity(
                    productId = insertedId,
                    name = "Varian Kotak 250ml",
                    barcode = "VAR-TEH-250",
                    sellingPrice = 3500.0,
                    purchasePrice = 2500.0,
                    stock = 8.0,
                    isActive = true
                )
            )

            // Normalized lookup (whitespace & case-insensitive)
            val foundProduct = productDao.getActiveProductByBarcode(" 8996001600123 ")
            assertNotNull(foundProduct)
            assertEquals("Teh Botol Sosro 450ml", foundProduct?.name)
            assertEquals(5500.0, foundProduct?.sellingPrice ?: 0.0, 0.01)
            assertEquals(12.0, foundProduct?.stock ?: 0.0, 0.01)

            // Observe Flow by barcode
            val observed = productDao.observeProductByBarcode("8996001600123").first()
            assertEquals(insertedId, observed?.id)

            // Indexed variant barcode lookup
            val foundVariant = extDao.getActiveVariantByBarcode("var-teh-250")
            assertNotNull(foundVariant)
            assertEquals("Varian Kotak 250ml", foundVariant?.name)
            assertEquals(3500.0, foundVariant?.sellingPrice ?: 0.0, 0.01)

            // Atomic stock decrement respects available balance
            val updatedRows = productDao.decrementStockIfAvailable(insertedId, 2.0)
            assertEquals(1, updatedRows)
            assertEquals(10.0, productDao.getProductById(insertedId)?.stock ?: 0.0, 0.01)

            val overDecrement = productDao.decrementStockIfAvailable(insertedId, 50.0)
            assertEquals(0, overDecrement)
            assertEquals(10.0, productDao.getProductById(insertedId)?.stock ?: 0.0, 0.01)

            // Scan log persistence
            productDao.recordBarcodeScan(
                BarcodeScanLogEntity(
                    barcode = "8996001600123",
                    productId = insertedId,
                    productName = "Teh Botol Sosro 450ml",
                    matchedPrice = 5500.0,
                    matchedStock = 10.0,
                    matched = true,
                    source = "CAMERA"
                )
            )
            val logs = productDao.getRecentBarcodeScans().first()
            assertEquals(1, logs.size)
            assertEquals("8996001600123", logs.first().barcode)
            assertTrue(logs.first().matched)
        } finally {
            db.close()
        }
    }

    @Test
    fun `test manual product management validation and Room add update delete lifecycle`() = runBlocking {
        val existingList = listOf(
            ProductEntity(id = 1L, name = "Kopi Susu", barcode = "899111222", sellingPrice = 5000.0, stock = 10.0)
        )

        // Blank name / negative price / duplicate barcode caught
        val invalid = validateManualProductInput(
            name = " ",
            sellingPriceText = "-100",
            stockText = "",
            barcode = "899111222",
            editingProductId = null,
            existingProducts = existingList
        )
        assertFalse(invalid.isValid)
        assertNotNull(invalid.nameError)
        assertNotNull(invalid.priceError)
        assertNotNull(invalid.stockError)
        assertNotNull(invalid.barcodeError)

        // Editing the same product with its own barcode passes validation
        val validEdit = validateManualProductInput(
            name = "Kopi Susu Gula Aren",
            sellingPriceText = "6000",
            stockText = "15",
            barcode = "899111222",
            editingProductId = 1L,
            existingProducts = existingList
        )
        assertTrue(validEdit.isValid)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PosRepository(db)

            // 1. Add item
            val insertedId = repo.insertProduct(
                ProductEntity(
                    name = "Indomie Goreng 85g",
                    sellingPrice = 3500.0,
                    purchasePrice = 2800.0,
                    stock = 24.0,
                    barcode = "089686010947"
                )
            )
            val created = repo.getProductById(insertedId)
            assertNotNull(created)
            assertEquals("Indomie Goreng 85g", created?.name)
            assertEquals(3500.0, created?.sellingPrice ?: 0.0, 0.01)
            assertEquals(24.0, created?.stock ?: 0.0, 0.01)

            // 2. Update name, price, and current stock level + record stock movement
            repo.updateProduct(
                created!!.copy(
                    name = "Indomie Goreng Jumbo",
                    sellingPrice = 4500.0,
                    stock = 30.0
                )
            )
            repo.adjustStock(insertedId, 30.0, "Pembaruan stok manual")
            val updated = repo.getProductById(insertedId)
            assertEquals("Indomie Goreng Jumbo", updated?.name)
            assertEquals(4500.0, updated?.sellingPrice ?: 0.0, 0.01)
            assertEquals(30.0, updated?.stock ?: 0.0, 0.01)

            // 3. Delete item cleanly from Room
            repo.deleteProductCascade(updated!!)
            assertNull(repo.getProductById(insertedId))
            assertNull(repo.getProductByBarcode("089686010947"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `test populateInitialData has zero dummy data and clean zero cash balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            GeraiUnaDatabase.populateInitialData(db)

            // Zero dummy products, suppliers, customers, printers, devices, or users
            assertTrue(db.productDao().getAllProductsList().isEmpty())
            assertTrue(db.contactDao().getAllCustomers().first().isEmpty())
            assertTrue(db.contactDao().getAllSuppliers().first().isEmpty())
            assertTrue(db.extendedPosDao().getAllUsersList().isEmpty())
            assertTrue(db.extendedPosDao().getAllPrinters().first().isEmpty())
            assertTrue(db.extendedPosDao().getAllCashierDevices().first().isEmpty())
            assertTrue(db.adminTierDao().getAllServicesList().isEmpty())

            // Clean neutral store profile & Kas Toko initialized at Rp 0
            val profile = db.extendedPosDao().getStoreProfileSync()
            assertNotNull(profile)
            assertEquals("Toko Saya", profile?.name)
            assertEquals("", profile?.phone)

            val accounts = db.cashAccountDao().getAllAccountsList()
            assertEquals(1, accounts.size)
            assertEquals("Kas Toko", accounts.first().accountName)
            assertEquals(0.0, accounts.first().balance, 0.01)

            assertEquals("true", db.appSettingsDao().getSettingValue("db_initialized"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `test Admin Utama setup validation, Room user creation, and mandatory login verification`() = runBlocking {
        // 1. Validate first-time Admin Utama setup errors
        val badSetup = validateAdminSetupInput(
            storeName = "",
            fullName = " ",
            username = "ab",
            pin = "123",
            confirmPin = "456"
        )
        assertFalse(badSetup.isValid)
        assertNotNull(badSetup.storeNameError)
        assertNotNull(badSetup.fullNameError)
        assertNotNull(badSetup.usernameError)
        assertNotNull(badSetup.pinError)
        assertNotNull(badSetup.confirmPinError)

        val goodSetup = validateAdminSetupInput(
            storeName = "Toko Maju Jaya",
            fullName = "Andi Pratama",
            username = "admin",
            pin = "482910",
            confirmPin = "482910"
        )
        assertTrue(goodSetup.isValid)

        // 2. Persist Admin Utama + Staff user created by Admin Utama into Room
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PosRepository(db)
            val adminId = repo.saveUserWithPermissions(
                UserAccountEntity(
                    name = "Andi Pratama",
                    username = "admin",
                    role = "owner",
                    pinHash = GeraiUnaDatabase.hashPin("482910"),
                    isActive = true
                ),
                ALL_PERMISSION_MENU_KEYS.associateWith { true }
            )
            val kasirId = repo.saveUserWithPermissions(
                UserAccountEntity(
                    name = "Sari Kasir",
                    username = "sari",
                    role = "kasir",
                    pinHash = GeraiUnaDatabase.hashPin("654321"),
                    isActive = true
                ),
                mapOf("POS_KASIR" to true, "SCAN_BARCODE" to true, "LAPORAN" to false)
            )
            repo.saveUserWithPermissions(
                UserAccountEntity(
                    name = "Budi Nonaktif",
                    username = "budi",
                    role = "kasir",
                    pinHash = GeraiUnaDatabase.hashPin("112233"),
                    isActive = false
                ),
                mapOf("POS_KASIR" to true)
            )

            val allUsers = db.extendedPosDao().getAllUsersList()
            assertEquals(3, allUsers.size)

            // Rejects wrong PIN, unknown username, inactive user, and invalid format
            assertTrue(authenticateLocalUser("", "482910", allUsers) is LocalAuthResult.Error)
            assertTrue(authenticateLocalUser("admin", "123456", allUsers) is LocalAuthResult.Error)
            assertTrue(authenticateLocalUser("unknown", "482910", allUsers) is LocalAuthResult.Error)
            assertTrue(authenticateLocalUser("budi", "112233", allUsers) is LocalAuthResult.Error)

            // Succeeds with valid Admin Utama & Kasir accounts (case-insensitive username)
            val adminLogin = authenticateLocalUser(" ADMIN ", "482910", allUsers)
            assertTrue(adminLogin is LocalAuthResult.Success)
            assertEquals(adminId, (adminLogin as LocalAuthResult.Success).user.id)

            val kasirLogin = authenticateLocalUser("sari", "654321", allUsers)
            assertTrue(kasirLogin is LocalAuthResult.Success)
            assertEquals(kasirId, (kasirLogin as LocalAuthResult.Success).user.id)
            assertEquals("kasir", kasirLogin.user.role)
        } finally {
            db.close()
        }
    }
}
