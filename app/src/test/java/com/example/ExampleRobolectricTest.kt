package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeraiUnaAIEngine
import com.example.data.backup.KasLiteBackupManager
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.AdminTierRangeEntity
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.AdminTierWithRanges
import com.example.data.entity.BarcodeScanLogEntity
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProductAliasEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ProductVariantEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.entity.UnitConversionEntity
import com.example.data.entity.UserAccountEntity
import com.example.data.excel.ExcelProductReportManager
import com.example.data.pricing.ProfitLossCalculator
import com.example.data.pricing.ProfitLossPeriod
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.PosRepository
import com.example.firebase.FirebaseSessionStore
import com.example.firebase.LicenseAccessStatus
import com.example.firebase.LocalLicenseSnapshot
import com.example.ui.viewmodel.ALL_PERMISSION_MENU_KEYS
import com.example.ui.viewmodel.BarcodeLookupOutcome
import com.example.ui.viewmodel.DEMO_DURATION_MILLIS
import com.example.ui.viewmodel.LocalAuthResult
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.authenticateLocalUser
import com.example.ui.viewmodel.evaluateLocalLicenseState
import com.example.ui.viewmodel.isScreenAllowedWhenDemoLocked
import com.example.ui.viewmodel.matchesProductSearchQuery
import com.example.ui.viewmodel.resolveBarcodeToCartMatch
import com.example.ui.viewmodel.validateAdminSetupInput
import com.example.ui.viewmodel.validateManualProductInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipInputStream
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
    fun `test Supabase LicenseDoc JSON parsing, ISO timestamps, session store, and config validation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionStore = FirebaseSessionStore(context)
        sessionStore.clearSupabaseSession()

        // 1. Config validation
        assertTrue(
            com.example.supabase.SupabaseLicenseRepository.isValidConfig(
                "https://xyzcompany.supabase.co/",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
            )
        )
        assertFalse(
            com.example.supabase.SupabaseLicenseRepository.isValidConfig("", "anon")
        )
        assertFalse(
            com.example.supabase.SupabaseLicenseRepository.isValidConfig(
                "https://SUPABASE_URL_PLACEHOLDER.supabase.co",
                "SUPABASE_ANON_KEY_PLACEHOLDER"
            )
        )
        assertFalse(
            com.example.supabase.SupabaseLicenseRepository.isValidConfig("http://evil.com", "anon")
        )

        // 2. ISO-8601 timestamp parsing
        val parsedIso = com.example.supabase.SupabaseLicenseRepository.isoToMillis("2026-11-23T01:05:30.123456Z")
        assertTrue(parsedIso > 0L)
        assertEquals(0L, com.example.supabase.SupabaseLicenseRepository.isoToMillis("null"))

        // 3. LicenseDoc JSON serialization & parsing roundtrip
        val doc = com.example.firebase.LicenseDoc(
            licenseKey = "KASLITE-ABC123",
            storeName = "Toko Berkah",
            ownerName = "Budi",
            ownerEmail = "BUDI@GMAIL.COM",
            ownerUid = "uuid-user-1",
            phone = "08123456789",
            deviceId = "dev_001",
            planType = "DEMO",
            status = "DEMO",
            demoStartedAtMillis = 1_700_000_000_000L,
            demoExpiresAtMillis = 1_700_259_200_000L
        )
        val insertJson = com.example.supabase.SupabaseLicenseRepository.licenseDocToInsertJson(doc)
        assertEquals("budi@gmail.com", insertJson.getString("owner_email"))
        assertEquals("KASLITE-ABC123", insertJson.getString("license_key"))
        insertJson.put("id", "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        insertJson.put("created_at", "2026-11-23T01:05:30Z")

        val parsedDoc = com.example.supabase.SupabaseLicenseRepository.parseLicenseDocJson(insertJson)
        assertEquals("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", parsedDoc.id)
        assertEquals("KASLITE-ABC123", parsedDoc.licenseKey)
        assertEquals("budi@gmail.com", parsedDoc.ownerEmail)
        assertEquals("DEMO", parsedDoc.planType)
        assertEquals(1_700_259_200_000L, parsedDoc.demoExpiresAtMillis)
        assertTrue(parsedDoc.createdAtMillis > 0L)

        // 4. GoTrue session parsing & persistence
        val authJson = org.json.JSONObject(
            """
            {
              "access_token": "jwt.access.token",
              "refresh_token": "refresh_tok",
              "expires_in": 3600,
              "user": {
                "id": "uid-supabase-99",
                "email": "budi@gmail.com",
                "user_metadata": { "full_name": "Budi Santoso" }
              }
            }
            """.trimIndent()
        )
        val authSession = com.example.supabase.SupabaseLicenseRepository.parseAuthSessionJson(authJson, 1_700_000_000_000L)
        assertEquals("jwt.access.token", authSession.accessToken)
        assertEquals("uid-supabase-99", authSession.uid)
        assertEquals("budi@gmail.com", authSession.email)
        assertEquals("Budi Santoso", authSession.fullName)
        assertFalse(authSession.requiresEmailConfirmation)

        sessionStore.saveSupabaseSession(
            accessToken = authSession.accessToken,
            refreshToken = authSession.refreshToken,
            uid = authSession.uid,
            email = authSession.email,
            fullName = authSession.fullName,
            expiresAtMillis = authSession.expiresAtMillis
        )
        assertTrue(sessionStore.hasSupabaseSession())
        assertEquals("budi@gmail.com", sessionStore.getSupabaseEmail())
        sessionStore.clearSupabaseSession()
        assertFalse(sessionStore.hasSupabaseSession())
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
    fun `test KASLITE 3-day demo licensing, clock tamper protection, and product-only feature lock`() {
        assertEquals("egisbahricha@gmail.com", SUPER_ADMIN_EMAIL)
        assertEquals(3L * 24L * 60L * 60L * 1000L, DEMO_DURATION_MILLIS)

        // 1. Unregistered snapshot
        val unregistered = evaluateLocalLicenseState(null, 1_700_000_000_000L, "dev-1")
        assertEquals(LicenseAccessStatus.UNREGISTERED, unregistered.accessStatus)

        val start = 1_700_000_000_000L
        val demoEnd = start + DEMO_DURATION_MILLIS
        val demoSnap = LocalLicenseSnapshot(
            licenseId = "lic_1",
            licenseKey = "KASLITE-DEMO1",
            ownerUid = "uid_cust",
            ownerEmail = "pelanggan@gmail.com",
            storeName = "Toko KASLITE",
            deviceId = "dev-1",
            planType = "DEMO",
            status = "DEMO",
            demoStartedAtMillis = start,
            demoExpiresAtMillis = demoEnd,
            maxSeenClockMillis = start + 1_000L
        )

        // 2. Demo active before expiry
        val activeDemo = evaluateLocalLicenseState(demoSnap, start + 60_000L, "dev-1")
        assertEquals(LicenseAccessStatus.DEMO_ACTIVE, activeDemo.accessStatus)
        assertFalse(activeDemo.isLockedToProductOnly)
        assertTrue(activeDemo.remainingMillis > 0L)

        // 3. Demo expired after 3 days
        val expiredDemo = evaluateLocalLicenseState(demoSnap, demoEnd + 1_000L, "dev-1")
        assertEquals(LicenseAccessStatus.DEMO_EXPIRED, expiredDemo.accessStatus)
        assertTrue(expiredDemo.isLockedToProductOnly)

        // 4. Clock rollback blocked by monotonic maxSeenClockMillis
        val rollbackSnap = demoSnap.copy(maxSeenClockMillis = demoEnd + 5_000L)
        val rolledBack = evaluateLocalLicenseState(rollbackSnap, start + 10_000L, "dev-1")
        assertEquals(LicenseAccessStatus.DEMO_EXPIRED, rolledBack.accessStatus)
        assertTrue(rolledBack.isLockedToProductOnly)

        // 5. Device mismatch & suspended locks
        val mismatch = evaluateLocalLicenseState(demoSnap, start + 60_000L, "dev-other")
        assertEquals(LicenseAccessStatus.DEVICE_MISMATCH, mismatch.accessStatus)
        assertTrue(mismatch.isLockedToProductOnly)

        val suspended = evaluateLocalLicenseState(demoSnap.copy(status = "SUSPENDED"), start + 60_000L, "dev-1")
        assertEquals(LicenseAccessStatus.SUSPENDED, suspended.accessStatus)
        assertTrue(suspended.isLockedToProductOnly)

        // 6. FULL license unlocks all menus
        val fullActive = evaluateLocalLicenseState(
            demoSnap.copy(planType = "FULL", status = "ACTIVE", expiresAtMillis = 0L),
            demoEnd + 10_000_000L,
            "dev-1"
        )
        assertEquals(LicenseAccessStatus.FULL_ACTIVE, fullActive.accessStatus)
        assertFalse(fullActive.isLockedToProductOnly)

        // 7. Screen gating while locked: only Produk + Pengaturan
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.PRODUK))
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.MANAJEMEN_PRODUK))
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.SCAN_BARCODE))
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.PENGATURAN))
        assertFalse(isScreenAllowedWhenDemoLocked(Screen.POS_KASIR))
        assertFalse(isScreenAllowedWhenDemoLocked(Screen.DASHBOARD))
        assertFalse(isScreenAllowedWhenDemoLocked(Screen.LAPORAN))
        assertFalse(isScreenAllowedWhenDemoLocked(Screen.KEUANGAN))
    }

    @Test
    fun `test Excel template export, CSV product import upsert, and 6-sheet XLSX report export`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = File(context.cacheDir, "kaslite_excel_test").apply {
            deleteRecursively()
            mkdirs()
        }
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // 1. Export & inspect .xlsx product template
            val templateFile = File(tempDir, "Template_Produk_KASLITE.xlsx")
            val tplRes = ExcelProductReportManager.exportProductTemplateXlsx(context, Uri.fromFile(templateFile))
            assertTrue("Template export failed", tplRes.isSuccess)
            assertTrue(templateFile.exists() && templateFile.length() > 0L)

            val tplInspect = ExcelProductReportManager.inspectProductSpreadsheet(context, Uri.fromFile(templateFile))
            assertEquals("XLSX", tplInspect.fileFormat)
            assertEquals(2, tplInspect.rows.size)
            assertEquals("Beras Ramos 5kg", tplInspect.rows.first().name)

            // 2. Import template + custom CSV upsert
            val tplSummary = ExcelProductReportManager.importProductsToRoom(db, tplInspect, updateExistingByBarcodeOrName = true)
            assertEquals(2, tplSummary.insertedCount)

            val csvFile = File(tempDir, "produk_baru.csv")
            csvFile.writeText(
                """
                Nama Produk;Barcode;Kategori;Satuan;Harga Modal;Harga Jual;Stok;Tipe
                Beras Ramos 5kg;8991002100011;Sembako;sak;65000;75000;30;FISIK
                Gula Pasir 1kg;8991002100099;Sembako;pcs;15000;18000;40;FISIK
                """.trimIndent()
            )
            val csvInspect = ExcelProductReportManager.inspectProductSpreadsheet(context, Uri.fromFile(csvFile))
            assertEquals("CSV", csvInspect.fileFormat)
            val csvSummary = ExcelProductReportManager.importProductsToRoom(db, csvInspect, updateExistingByBarcodeOrName = true)
            assertEquals(1, csvSummary.insertedCount)
            assertEquals(1, csvSummary.updatedCount)

            val allProducts = db.productDao().getAllProductsList()
            assertEquals(3, allProducts.size)
            val updatedBeras = allProducts.first { it.barcode == "8991002100011" }
            assertEquals(75000.0, updatedBeras.sellingPrice, 0.01)
            assertEquals(30.0, updatedBeras.stock, 0.01)

            // 3. Populate sample sale & expense, then export 6-sheet Excel report
            val now = System.currentTimeMillis()
            val saleId = db.saleDao().insertSale(
                SaleEntity(
                    invoiceNumber = "TRX-XLSX-01",
                    date = now,
                    customerName = "Pelanggan Uji",
                    cashierName = "Kasir",
                    subtotal = 75000.0,
                    finalAmount = 75000.0,
                    amountPaid = 75000.0,
                    status = "COMPLETED"
                )
            )
            db.saleDao().insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId,
                        productId = updatedBeras.id,
                        productName = updatedBeras.name,
                        quantity = 1.0,
                        unit = "sak",
                        unitPrice = 75000.0,
                        subtotal = 75000.0,
                        purchasePriceSnapshot = 65000.0
                    )
                )
            )
            db.financeDao().insertExpense(
                ExpenseEntity(category = "Listrik", amount = 25000.0, date = now, notes = "Token")
            )

            val reportFile = File(tempDir, "Laporan_KASLITE_Test.xlsx")
            val reportRes = ExcelProductReportManager.exportReportsXlsxToUri(
                context = context,
                db = db,
                storeName = "Toko KASLITE",
                targetUri = Uri.fromFile(reportFile)
            )
            assertTrue("Report XLSX export failed", reportRes.isSuccess)
            val reportBytes = reportFile.readBytes()
            assertTrue(reportBytes.size > 500)
            assertEquals(0x50.toByte(), reportBytes[0]) // 'P'
            assertEquals(0x4B.toByte(), reportBytes[1]) // 'K'

            val zipEntries = mutableSetOf<String>()
            ZipInputStream(reportFile.inputStream()).use { zis ->
                while (true) {
                    val e = zis.nextEntry ?: break
                    zipEntries.add(e.name)
                    zis.closeEntry()
                }
            }
            assertTrue(zipEntries.contains("[Content_Types].xml"))
            assertTrue(zipEntries.contains("xl/workbook.xml"))
            for (i in 1..6) {
                assertTrue("Missing sheet $i", zipEntries.contains("xl/worksheets/sheet$i.xml"))
            }
        } finally {
            db.close()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test full kaslite backup export, inspect, and restore replace vs merge`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = File(context.cacheDir, "kaslite_backup_test").apply {
            deleteRecursively()
            mkdirs()
        }
        val sourceDb = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val destDb = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sessionStore = FirebaseSessionStore(context)
        val photoSrc = File(File(context.filesDir, "product_images").apply { mkdirs() }, "test_produk.jpg")

        try {
            photoSrc.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))

            sourceDb.extendedPosDao().upsertStoreProfile(
                StoreProfileEntity(id = 1L, name = "Toko KASLITE Jaya", phone = "08123456789")
            )
            sourceDb.cashAccountDao().insertCashAccount(
                CashAccountEntity(accountName = "Kas Utama", balance = 500000.0, isDefault = true)
            )
            val prodId = sourceDb.productDao().insertProduct(
                ProductEntity(
                    name = "Minyak Goreng 2L",
                    barcode = "899000999",
                    sellingPrice = 36000.0,
                    purchasePrice = 31000.0,
                    stock = 12.0,
                    imageUri = photoSrc.absolutePath
                )
            )
            sourceDb.contactDao().insertCustomer(CustomerEntity(name = "Ibu Ani", phone = "081111222"))
            val saleId = sourceDb.saleDao().insertSale(
                SaleEntity(invoiceNumber = "TRX-BK-001", date = System.currentTimeMillis(), subtotal = 36000.0, finalAmount = 36000.0, status = "COMPLETED")
            )
            sourceDb.saleDao().insertSaleItems(
                listOf(
                    SaleItemEntity(
                        saleId = saleId,
                        productId = prodId,
                        productName = "Minyak Goreng 2L",
                        quantity = 1.0,
                        unit = "pcs",
                        unitPrice = 36000.0,
                        subtotal = 36000.0,
                        purchasePriceSnapshot = 31000.0
                    )
                )
            )
            sourceDb.extendedPosDao().insertUser(
                UserAccountEntity(
                    name = "Owner KASLITE",
                    username = "owner",
                    role = "owner",
                    pinHash = GeraiUnaDatabase.hashPin("123456"),
                    isActive = true
                )
            )

            // 1. Export .kaslite archive
            val backupFile = File(tempDir, "backup_toko.kaslite")
            val exportRes = KasLiteBackupManager.exportBackupZipToUri(context, sourceDb, sessionStore, Uri.fromFile(backupFile))
            assertTrue("Backup export failed", exportRes.isSuccess)
            assertTrue((exportRes.getOrNull() ?: 0) >= 6)

            // 2. Inspect .kaslite
            val inspect = KasLiteBackupManager.inspectBackupZip(context, Uri.fromFile(backupFile))
            assertEquals("Toko KASLITE Jaya", inspect.storeName)
            assertEquals(1, inspect.productCount)
            assertEquals(1, inspect.saleCount)
            assertEquals(1, inspect.customerCount)
            assertEquals(1, inspect.userAccountCount)
            assertTrue(inspect.photoCount >= 1)

            // 3. Full Replace restore into destDb
            val replaceSummary = KasLiteBackupManager.restoreBackupZip(
                context = context,
                db = destDb,
                sessionStore = sessionStore,
                inspect = inspect,
                replaceExisting = true
            )
            assertTrue(replaceSummary.replaceMode)
            assertTrue(replaceSummary.restoredRecords >= 6)
            assertEquals("Toko KASLITE Jaya", destDb.extendedPosDao().getStoreProfileSync()?.name)
            val restoredProducts = destDb.productDao().getAllProductsList()
            assertEquals(1, restoredProducts.size)
            assertEquals("Minyak Goreng 2L", restoredProducts.first().name)
            assertTrue(File(restoredProducts.first().imageUri).exists())
            assertEquals(1, destDb.saleDao().getAllSales().first().size)
            assertEquals(1, destDb.extendedPosDao().getAllUsersList().size)

            // 4. Merge restore into destDb preserves existing and avoids duplicates
            val mergeSummary = KasLiteBackupManager.restoreBackupZip(
                context = context,
                db = destDb,
                sessionStore = sessionStore,
                inspect = inspect,
                replaceExisting = false
            )
            assertFalse(mergeSummary.replaceMode)
            assertEquals(1, destDb.productDao().getAllProductsList().size)
            assertEquals(1, destDb.saleDao().getAllSales().first().size)

            KasLiteBackupManager.cleanupTemp(inspect)
        } finally {
            photoSrc.delete()
            sourceDb.close()
            destDb.close()
            tempDir.deleteRecursively()
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

    @Test
    fun `test HardwareDevicePreferences saves and loads ScannerSettings and preferredPrinterId`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.hardware.HardwareDevicePreferences(context)

        val customSettings = com.example.hardware.ScannerSettings(
            preferredMode = "HARDWARE",
            selectedScannerId = "BT:00:11:22:33:44:55",
            selectedScannerName = "Honeywell Xenon BT",
            continuousMode = false,
            beepOnScan = false,
            vibrateOnScan = true,
            defaultTorchOn = true,
            scanCooldownMs = 1800L,
            autoCaptureHardwareInput = true,
            stripWhitespace = false
        )
        prefs.saveScannerSettings(customSettings)
        prefs.setPreferredPrinterId(42L)

        val loaded = prefs.loadScannerSettings()
        assertEquals("HARDWARE", loaded.preferredMode)
        assertEquals("BT:00:11:22:33:44:55", loaded.selectedScannerId)
        assertEquals("Honeywell Xenon BT", loaded.selectedScannerName)
        assertFalse(loaded.continuousMode)
        assertFalse(loaded.beepOnScan)
        assertTrue(loaded.vibrateOnScan)
        assertTrue(loaded.defaultTorchOn)
        assertEquals(1800L, loaded.scanCooldownMs)
        assertTrue(loaded.autoCaptureHardwareInput)
        assertFalse(loaded.stripWhitespace)
        assertEquals(42L, prefs.getPreferredPrinterId())
    }

    @Test
    fun `test ThermalPrinterEngine ESC POS and TSPL byte formatting for 40mm 58mm and 80mm printers`() {
        val sale = SaleEntity(
            id = 1L,
            invoiceNumber = "INV-2026-009",
            date = 1_763_890_000_000L,
            customerName = "Bapak Andi",
            cashierName = "Kasir Utama",
            subtotal = 50000.0,
            discount = 2000.0,
            finalAmount = 48000.0,
            amountPaid = 50000.0,
            changeAmount = 2000.0,
            paymentMethod = "TUNAI"
        )
        val items = listOf(
            com.example.data.repository.CartItem(
                productId = 1L,
                productName = "Beras Pulen 5kg",
                unitName = "sak",
                quantity = 1.0,
                unitPrice = 50000.0
            )
        )
        val profile = StoreProfileEntity(
            name = "Toko Berkah",
            address = "Jl. Merdeka No. 10",
            phone = "081234567890",
            receiptFooter = "Selamat Belanja"
        )

        assertEquals(24, com.example.hardware.ThermalPrinterEngine.columnsForWidthMm(40))
        assertEquals(32, com.example.hardware.ThermalPrinterEngine.columnsForWidthMm(58))
        assertEquals(48, com.example.hardware.ThermalPrinterEngine.columnsForWidthMm(80))
        assertTrue(com.example.hardware.ThermalPrinterEngine.isUsbAddress("USB:1234:5678:/dev/bus/usb/001/002"))
        assertTrue(com.example.hardware.ThermalPrinterEngine.isSystemPrintAddress("SYSTEM_PRINT"))
        assertFalse(com.example.hardware.ThermalPrinterEngine.isSystemPrintAddress("AA:BB:CC:DD:EE:FF"))

        val escPrinter = com.example.data.entity.PrinterEntity(
            id = 1L,
            name = "RPP02N 58mm",
            bluetoothAddress = "AA:BB:CC:DD:EE:FF",
            type = "struk",
            widthMm = 58,
            language = "escpos",
            autoCut = true,
            openDrawer = true
        )
        val escBytes = com.example.hardware.ThermalPrinterEngine.buildEscPosReceiptBytes(
            printer = escPrinter,
            sale = sale,
            items = items,
            storeProfile = profile
        )
        val escText = String(escBytes, Charsets.ISO_8859_1)
        assertTrue(escText.contains("Toko Berkah"))
        assertTrue(escText.contains("INV-2026-009"))
        assertTrue(escText.contains("Beras Pulen 5kg"))
        assertTrue(escText.contains("Selamat Belanja"))
        // Verify ESC @ init header & GS V auto-cut trailer exist
        assertEquals(0x1B.toByte(), escBytes[0])
        assertEquals(0x40.toByte(), escBytes[1])

        val tsplPrinter = escPrinter.copy(
            name = "Xprinter Label 40mm",
            type = "stiker",
            widthMm = 40,
            language = "tspl"
        )
        val tsplBytes = com.example.hardware.ThermalPrinterEngine.buildTsplReceiptBytes(
            printer = tsplPrinter,
            sale = sale,
            items = items,
            storeProfile = profile
        )
        val tsplText = String(tsplBytes, Charsets.ISO_8859_1)
        assertTrue(tsplText.contains("SIZE 40 mm"))
        assertTrue(tsplText.contains("INV-2026-009"))
        assertTrue(tsplText.contains("PRINT 1,1"))

        val testEscBytes = com.example.hardware.ThermalPrinterEngine.buildTestPageBytes(escPrinter, profile)
        assertTrue(String(testEscBytes, Charsets.ISO_8859_1).contains("TES PRINTER BERHASIL"))

        val testTsplBytes = com.example.hardware.ThermalPrinterEngine.buildTestPageBytes(tsplPrinter, profile)
        assertTrue(String(testTsplBytes, Charsets.ISO_8859_1).contains("TES CETAK STIKER OK"))
    }

    @Test
    fun `test HardwareDeviceDiscovery inspectHardware runs safely on JVM context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = com.example.hardware.HardwareDeviceDiscovery.inspectHardware(context)
        assertNotNull(snapshot)
        assertNotNull(snapshot.scanners)
        assertNotNull(snapshot.printers)
    }

    @Test
    fun `test resolveAuthoritativeCustomerLicense enforces SUSPENDED FULL lock over fallback rows`() {
        val demoRow = com.example.firebase.LicenseDoc(
            id = "row-demo",
            licenseKey = "KASLITE-DEMO01",
            ownerUid = "uid-1",
            ownerEmail = "toko@gmail.com",
            deviceId = "dev-A",
            planType = "DEMO",
            status = "DEMO",
            demoExpiresAtMillis = System.currentTimeMillis() + 86_400_000L
        )
        val suspendedFullRow = com.example.firebase.LicenseDoc(
            id = "row-full",
            licenseKey = "KASLITE-3AB5EB",
            ownerUid = "uid-1",
            ownerEmail = "toko@gmail.com",
            deviceId = "dev-A",
            planType = "FULL",
            status = "SUSPENDED"
        )
        val activeFullRow = suspendedFullRow.copy(status = "ACTIVE")

        // 1. If active key KASLITE-3AB5EB was suspended, it must override demoRow
        val resolvedByKey = com.example.supabase.SupabaseLicenseRepository.resolveAuthoritativeCustomerLicense(
            byKey = suspendedFullRow,
            ownerRows = listOf(demoRow, suspendedFullRow),
            currentDeviceId = "dev-A"
        )
        assertEquals("KASLITE-3AB5EB", resolvedByKey?.licenseKey)
        assertEquals("SUSPENDED", resolvedByKey?.status)

        // 2. Fresh reinstall (byKey == null) with both demoRow & suspendedFullRow still locks as SUSPENDED
        val resolvedWithoutKey = com.example.supabase.SupabaseLicenseRepository.resolveAuthoritativeCustomerLicense(
            byKey = null,
            ownerRows = listOf(demoRow, suspendedFullRow),
            currentDeviceId = "dev-A"
        )
        assertEquals("KASLITE-3AB5EB", resolvedWithoutKey?.licenseKey)
        assertEquals("SUSPENDED", resolvedWithoutKey?.status)

        // 3. When current demo row exists and Admin activates a claimed FULL row, it upgrades to ACTIVE FULL
        val upgraded = com.example.supabase.SupabaseLicenseRepository.resolveAuthoritativeCustomerLicense(
            byKey = demoRow,
            ownerRows = listOf(demoRow, activeFullRow),
            currentDeviceId = "dev-A"
        )
        assertEquals("KASLITE-3AB5EB", upgraded?.licenseKey)
        assertEquals("ACTIVE", upgraded?.status)

        // 4. Local evaluator locks immediately when FULL plan is marked SUSPENDED
        val store = FirebaseSessionStore(ApplicationProvider.getApplicationContext())
        store.clearLicenseSnapshot()
        val snap = store.saveFromLicenseDoc(suspendedFullRow)
        val uiState = evaluateLocalLicenseState(snap, System.currentTimeMillis(), "dev-A")
        assertEquals(LicenseAccessStatus.SUSPENDED, uiState.accessStatus)
        assertTrue(uiState.isLockedToProductOnly)
        assertFalse(uiState.isFullActive)
        store.clearLicenseSnapshot()
    }

    @Test
    fun `test hardware barcode sanitizer, SPP terminators, and product or variant search matching`() {
        val sanitizer = com.example.hardware.BarcodeInputSanitizer

        // 1. Clean control characters, BOM, CR/LF/Tab
        assertEquals("8991002101234", sanitizer.cleanBarcodeValue("\u0002 8991002101234\r\n\u0003\t"))
        assertTrue(sanitizer.hasTerminatorChar("8991002101234\t"))
        assertTrue(sanitizer.hasTerminatorChar("8991002101234\r"))

        // 2. Chunked stream extraction handles CR-only, LF, Tab, ETX, and leaves partial tokens buffered
        val streamBuf = StringBuilder()
        val firstChunk = sanitizer.extractTerminatedBarcodes(streamBuf, "\u0002899100210001\r89910021")
        assertEquals(listOf("899100210001"), firstChunk)
        assertEquals("89910021", streamBuf.toString())

        val secondChunk = sanitizer.extractTerminatedBarcodes(streamBuf, "0002\t899100210003\u0003")
        assertEquals(listOf("899100210002", "899100210003"), secondChunk)
        assertEquals("", streamBuf.toString())

        // 3. Barcode equality handles formatting hyphens/spaces and UPC-A <-> EAN-13 leading-zero equivalence
        assertTrue(sanitizer.matchesBarcodeCode("899-1002-001", "8991002001\r\n"))
        assertTrue(sanitizer.matchesBarcodeCode("896860109475", "0896860109475"))
        assertTrue(sanitizer.matchesBarcodeCode("0896860109475", "896860109475"))
        assertFalse(sanitizer.matchesBarcodeCode("8991002001", "8991002999"))

        // 4. resolveBarcodeToCartMatch & matchesProductSearchQuery find products + variants reliably
        val kopiProduct = ProductEntity(
            id = 10L,
            name = "Kopi Susu Arena",
            barcode = "8992775001122",
            category = "Minuman",
            sellingPrice = 5000.0,
            stock = 20.0,
            hasVariants = true,
            isActive = true
        )
        val kopiVariant = ProductVariantEntity(
            id = 101L,
            productId = 10L,
            name = "Botol 250ml",
            barcode = "8992775009988",
            sellingPrice = 8500.0,
            stock = 12.0,
            isActive = true
        )
        val upcProduct = ProductEntity(
            id = 20L,
            name = "Biskuit Kaleng",
            barcode = "896860109475", // 12-digit UPC-A
            category = "Snack",
            sellingPrice = 24000.0,
            stock = 8.0,
            isActive = true
        )

        val variantOutcome = resolveBarcodeToCartMatch(
            rawCode = "8992775009988\t",
            products = listOf(kopiProduct, upcProduct),
            variants = listOf(kopiVariant),
            conversions = emptyList()
        )
        assertTrue(variantOutcome is BarcodeLookupOutcome.AddedVariant)
        assertEquals(101L, (variantOutcome as BarcodeLookupOutcome.AddedVariant).variant.id)

        val upcOutcome = resolveBarcodeToCartMatch(
            rawCode = "0896860109475\r",
            products = listOf(kopiProduct, upcProduct),
            variants = listOf(kopiVariant),
            conversions = emptyList()
        )
        assertTrue(upcOutcome is BarcodeLookupOutcome.AddedProduct)
        assertEquals(20L, (upcOutcome as BarcodeLookupOutcome.AddedProduct).product.id)

        // Catalog search finds parent product even when scanned code belongs to a variant or UPC/EAN equivalent
        assertTrue(matchesProductSearchQuery(kopiProduct, "8992775009988\t", listOf(kopiVariant)))
        assertTrue(matchesProductSearchQuery(upcProduct, "0896860109475\r\n", listOf(kopiVariant)))
        assertFalse(matchesProductSearchQuery(upcProduct, "999999999", listOf(kopiVariant)))
    }

    @Test
    fun `test developer donation packages, manual transfer JSON roundtrip, license durations, and bug reports`() {
        // 1. Verify exact 3 donation packages (10rb/month, 50rb/year, 250rb/lifetime) & flagship features
        val packages = com.example.firebase.DonationPackage.entries
        assertEquals(3, packages.size)

        assertEquals(10_000L, com.example.firebase.DonationPackage.MONTHLY.amountRupiah)
        assertEquals(30, com.example.firebase.DonationPackage.MONTHLY.durationDays)

        assertEquals(50_000L, com.example.firebase.DonationPackage.YEARLY.amountRupiah)
        assertEquals(365, com.example.firebase.DonationPackage.YEARLY.durationDays)

        assertEquals(250_000L, com.example.firebase.DonationPackage.LIFETIME.amountRupiah)
        assertEquals(0, com.example.firebase.DonationPackage.LIFETIME.durationDays)

        packages.forEach { pkg ->
            assertTrue("Flagship features must be populated for ${pkg.id}", pkg.features.size >= 5)
            val allText = (listOf(pkg.title, pkg.priceLabel, pkg.summary) + pkg.features).joinToString(" ").lowercase(Locale.ROOT)
            assertFalse("Must never refer to purchase/pembelian", allText.contains("pembelian lisensi"))
            assertFalse("Must never refer to beli lisensi", allText.contains("beli lisensi"))
        }

        // 2. Demo lock still permits access to Donasi ke Pengembang & Laporan Bug
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.DONASI_PENGEMBANG))
        assertTrue(isScreenAllowedWhenDemoLocked(Screen.LAPOR_BUG))

        // 3. Time-limited FULL licenses (30d / 365d) vs Lifetime (0L)
        val now = 1_750_000_000_000L
        val monthlySnap = LocalLicenseSnapshot(
            licenseKey = "KASLITE-BLN001",
            deviceId = "dev-x",
            planType = "FULL",
            status = "ACTIVE",
            expiresAtMillis = now + 30L * 86_400_000L
        )
        assertEquals(
            LicenseAccessStatus.FULL_ACTIVE,
            evaluateLocalLicenseState(monthlySnap, now + 5L * 86_400_000L, "dev-x").accessStatus
        )
        assertEquals(
            LicenseAccessStatus.DEMO_EXPIRED,
            evaluateLocalLicenseState(monthlySnap, now + 31L * 86_400_000L, "dev-x").accessStatus
        )

        val lifetimeSnap = monthlySnap.copy(expiresAtMillis = 0L)
        assertEquals(
            LicenseAccessStatus.FULL_ACTIVE,
            evaluateLocalLicenseState(lifetimeSnap, now + 3_650L * 86_400_000L, "dev-x").accessStatus
        )

        // 4. DeveloperBankConfig JSON roundtrip
        val bankCfg = com.example.firebase.DeveloperBankConfig(
            bankName = "BCA",
            accountNumber = "1234567890",
            accountHolder = "Egis Bahricha",
            whatsappNumber = "08123456789",
            supportEmail = SUPER_ADMIN_EMAIL
        )
        assertTrue(bankCfg.isConfigured)
        val parsedBank = com.example.supabase.SupabaseLicenseRepository.parseDeveloperBankConfigJson(
            com.example.supabase.SupabaseLicenseRepository.developerBankConfigToJson(bankCfg)
        )
        assertEquals("BCA", parsedBank.bankName)
        assertEquals("1234567890", parsedBank.accountNumber)
        assertEquals("Egis Bahricha", parsedBank.accountHolder)

        // 5. DonationRequestDoc & BugReportDoc JSON roundtrip + Email formatting
        val donation = com.example.firebase.DonationRequestDoc(
            userUid = "uid-donatur",
            userEmail = "TOKO@GMAIL.COM",
            ownerName = "Pak Budi",
            storeName = "Toko Berkah",
            packageId = com.example.firebase.DonationPackage.YEARLY.id,
            packageTitle = com.example.firebase.DonationPackage.YEARLY.title,
            amountRupiah = 50_000L,
            durationDays = 365,
            senderBankName = "BRI",
            senderAccountName = "Budi Santoso",
            transferNote = "Donasi KASLITE tahunan"
        )
        val donationJson = com.example.supabase.SupabaseLicenseRepository.donationRequestToInsertJson(donation).apply {
            put("id", "don-uuid-1")
            put("status", "PAID")
            put("license_key", "KASLITE-THN999")
        }
        val parsedDonation = com.example.supabase.SupabaseLicenseRepository.parseDonationRequestJson(donationJson)
        assertEquals("don-uuid-1", parsedDonation.id)
        assertEquals("toko@gmail.com", parsedDonation.userEmail)
        assertEquals(50_000L, parsedDonation.amountRupiah)
        assertEquals(365, parsedDonation.durationDays)
        assertEquals("PAID", parsedDonation.status)
        assertEquals("KASLITE-THN999", parsedDonation.licenseKey)

        val emailBody = com.example.firebase.KasLiteSupportEmailHelper.buildLicenseEmailBody(
            ownerName = parsedDonation.ownerName,
            storeName = parsedDonation.storeName,
            recipientEmail = parsedDonation.userEmail,
            licenseKey = parsedDonation.licenseKey,
            packageTitle = parsedDonation.packageTitle,
            amountRupiah = parsedDonation.amountRupiah,
            expiresAtMillis = 0L
        )
        assertTrue(emailBody.contains("KASLITE-THN999"))
        assertTrue(emailBody.contains("donasi dukungan"))
        assertTrue(emailBody.contains("PERMANEN"))

        val bug = com.example.firebase.BugReportDoc(
            userUid = "uid-donatur",
            userEmail = "toko@gmail.com",
            reporterName = "Pak Budi",
            storeName = "Toko Berkah",
            category = "Scan Barcode",
            severity = "TINGGI",
            title = "Scanner HID tidak membaca enter",
            description = "Setelah scan angka muncul di kotak tapi tidak masuk keranjang",
            appVersion = "1.2 (3)",
            deviceModel = "Samsung Tab A9",
            androidVersion = "Android 14"
        )
        val parsedBug = com.example.supabase.SupabaseLicenseRepository.parseBugReportJson(
            com.example.supabase.SupabaseLicenseRepository.bugReportToInsertJson(bug).apply {
                put("id", "bug-uuid-1")
                put("status", "SELESAI")
                put("admin_notes", "Sudah diperbaiki di versi terbaru")
            }
        )
        assertEquals("bug-uuid-1", parsedBug.id)
        assertEquals("Scan Barcode", parsedBug.category)
        assertEquals("SELESAI", parsedBug.status)
        assertEquals("Sudah diperbaiki di versi terbaru", parsedBug.adminNotes)
        assertTrue(
            com.example.firebase.KasLiteSupportEmailHelper.buildBugReportEmailBody(parsedBug)
                .contains("Samsung Tab A9")
        )
    }

    @Test
    fun `test digital product validation, barcode lookup, distinct cart lines, and multi-kas checkout`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = PosRepository(db)

        // 1. validateManualProductInput skips stock & sellingPrice when isDigital = true and isFlexiblePrice = true
        val digitalFlexValid = validateManualProductInput(
            name = "Transfer Bank BRI",
            sellingPriceText = "",
            stockText = "",
            barcode = "DIG-BRI-01",
            isDigital = true,
            isFlexiblePrice = true
        )
        assertTrue("Digital flexible product should be valid with empty price & stock", digitalFlexValid.isValid)

        val digitalFixedMissingPrice = validateManualProductInput(
            name = "Pulsa 50rb",
            sellingPriceText = "",
            stockText = "",
            barcode = "DIG-PLS-50",
            isDigital = true,
            isFlexiblePrice = false
        )
        assertFalse("Digital fixed product without price must require nominal", digitalFixedMissingPrice.isValid)
        assertNotNull(digitalFixedMissingPrice.priceError)
        assertNull(digitalFixedMissingPrice.stockError)

        // 2. resolveBarcodeToCartMatch always requires nominal/tier confirmation for DIGITAL products
        val digitalProd = ProductEntity(
            id = 501L,
            name = "Transfer Bank BRI",
            barcode = "DIG-BRI-01",
            category = "Layanan Digital",
            baseUnit = "transaksi",
            sellingPrice = 0.0,
            stock = 999.0,
            productType = "DIGITAL",
            isUnlimitedStock = true,
            hasNoStock = true,
            isFlexiblePrice = false, // even when false, DIGITAL must never silently add Rp 0
            serviceDirection = "SETOR",
            isActive = true
        )
        val barcodeOutcome = resolveBarcodeToCartMatch(
            rawCode = "DIG-BRI-01",
            products = listOf(digitalProd),
            variants = emptyList(),
            conversions = emptyList()
        )
        assertTrue(
            "Scanning a DIGITAL product must route to NeedsSelection instead of silent Rp 0 add",
            barcodeOutcome is BarcodeLookupOutcome.NeedsSelection
        )

        // 3. Seed CashAccounts (Kas Toko default CASH + BRI Agen BANK) + AdminTier ranges + Digital product in DB
        val cashTokoId = db.cashAccountDao().insertCashAccount(
            CashAccountEntity(
                accountName = "Kas Toko",
                accountType = "CASH",
                balance = 0.0,
                isDefault = true,
                isActive = true
            )
        )
        val bankAccountId = db.cashAccountDao().insertCashAccount(
            CashAccountEntity(
                accountName = "BRI Agen",
                accountType = "BANK",
                balance = 10_000_000.0,
                isDefault = false,
                isActive = true
            )
        )
        val tierServiceId = db.adminTierDao().insertService(
            AdminTierServiceEntity(serviceName = "Transfer Bank", isActive = true)
        )
        db.adminTierDao().insertRange(
            AdminTierRangeEntity(
                serviceId = tierServiceId,
                minAmount = 10_000.0,
                maxAmount = 1_000_000.0,
                storeAdminFee = 5_000.0,
                providerAdminFee = 1_000.0,
                agentCommission = 1_500.0
            )
        )
        db.adminTierDao().insertRange(
            AdminTierRangeEntity(
                serviceId = tierServiceId,
                minAmount = 1_000_001.0,
                maxAmount = 5_000_000.0,
                storeAdminFee = 10_000.0,
                providerAdminFee = 1_500.0,
                agentCommission = 2_000.0
            )
        )
        val savedDigitalId = repo.insertProduct(
            ProductEntity(
                name = "Transfer Bank BRI",
                barcode = "DIG-BRI-01",
                category = "Layanan Digital",
                baseUnit = "transaksi",
                sellingPrice = 0.0,
                stock = 999.0,
                productType = "DIGITAL",
                isUnlimitedStock = true,
                hasNoStock = true,
                isFlexiblePrice = true,
                adminTierGroupId = tierServiceId,
                serviceCashAccountId = bankAccountId,
                serviceDirection = "SETOR",
                isActive = true
            )
        )
        val persistedDigital = db.productDao().getProductById(savedDigitalId)!!

        // 4. Verify TierPricingResolver auto-calculates tiered admin fees by nominal
        val svcEntity = db.adminTierDao().getAllServicesList().first { it.id == tierServiceId }
        val loadedTiers = listOf(AdminTierWithRanges(svcEntity, db.adminTierDao().getRangesList(tierServiceId)))
        val tierGroup = TierPricingResolver.resolveServiceGroup(loadedTiers, persistedDigital.adminTierGroupId, persistedDigital.name)
        val calc500k = TierPricingResolver.calculate(tierGroup, 500_000.0)
        val calc2_5m = TierPricingResolver.calculate(tierGroup, 2_500_000.0)
        assertEquals(5_000.0, calc500k.storeAdminFee, 0.01)
        assertEquals(10_000.0, calc2_5m.storeAdminFee, 0.01)

        // 5. Verify PosViewModel keeps two digital transfers of the same service as distinct CartItem lines
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.PosViewModel(app)
        vm.clearCart()
        vm.addToCart(
            product = persistedDigital,
            customPrice = 500_000.0,
            storeAdminFee = calc500k.storeAdminFee,
            providerAdminFee = calc500k.providerAdminFee,
            agentCommission = calc500k.agentCommission,
            serviceCashAccountId = bankAccountId,
            serviceDirection = "SETOR"
        )
        vm.addToCart(
            product = persistedDigital,
            customPrice = 2_500_000.0,
            storeAdminFee = calc2_5m.storeAdminFee,
            providerAdminFee = calc2_5m.providerAdminFee,
            agentCommission = calc2_5m.agentCommission,
            serviceCashAccountId = bankAccountId,
            serviceDirection = "SETOR"
        )
        val twoLines = vm.cart.value
        assertEquals("Two digital transfers must stay separate cart lines", 2, twoLines.size)
        assertTrue("Each cart item must have a distinct lineId", twoLines[0].lineId != twoLines[1].lineId)
        assertEquals(500_000.0, twoLines[0].serviceNominal!!, 0.01)
        assertEquals(2_500_000.0, twoLines[1].serviceNominal!!, 0.01)

        // 6. Edit first line via updateDigitalCartItem & remove second line by lineId cleanly
        vm.updateDigitalCartItem(
            lineId = twoLines[0].lineId,
            nominal = 1_500_000.0,
            storeAdminFee = 10_000.0,
            providerAdminFee = 1_500.0,
            agentCommission = 2_000.0,
            serviceCashAccountId = bankAccountId,
            serviceDirection = "SETOR"
        )
        assertEquals(1_500_000.0, vm.cart.value[0].unitPrice, 0.01)
        assertEquals(10_000.0, vm.cart.value[0].storeAdminFee, 0.01)

        vm.removeFromCart(
            productId = twoLines[1].productId,
            unitName = twoLines[1].unitName,
            lineId = twoLines[1].lineId
        )
        assertEquals(1, vm.cart.value.size)
        assertEquals(twoLines[0].lineId, vm.cart.value[0].lineId)

        // 7. Complete digital SETOR sale & verify stock untouched + bank balance deducted cleanly
        val completed = repo.completeSale(
            cashierName = "Kasir Test",
            customer = null,
            items = vm.cart.value,
            paymentMethod = "TUNAI",
            amountPaid = 1_510_000.0
        )
        assertEquals(1_510_000.0, completed.finalAmount, 0.01)
        assertEquals(10_000.0, completed.storeAdminFee, 0.01)
        assertEquals(999.0, db.productDao().getProductById(savedDigitalId)!!.stock, 0.01)
        val updatedCashToko = db.cashAccountDao().getCashAccountById(cashTokoId)!!
        val updatedBank = db.cashAccountDao().getCashAccountById(bankAccountId)!!
        // Customer paid 1.510.000 TUNAI -> enters Kas Toko (0 + 1.510.000 = 1.510.000)
        assertEquals(1_510_000.0, updatedCashToko.balance, 0.01)
        // Bank BRI Agen: 10.000.000 - 1.500.000 (transfer) - 1.500 (provider admin) + 2.000 (agent commission) = 8.500.500
        assertEquals(8_500_500.0, updatedBank.balance, 0.01)
        vm.clearCart()
        db.close()
    }

    @Test
    fun `test GitHub release parser prefers kaslite-pos apk and detects versioned and Kode updates`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.PosViewModel(app)

        // Multi-asset JSON matching screenshot (stale gerai-una-pos-ai.apk listed before kaslite-pos.apk)
        val multiAssetRelease = org.json.JSONObject(
            """
            {
              "tag_name": "v1.4",
              "name": "KASLITE POS v1.4 (Kode 5)",
              "body": "Pembaruan KASLITE POS Versi 1.4 (Kode 5).",
              "html_url": "https://github.com/egisbahricha-commits/Pos-ai-egis/releases/tag/v1.4",
              "assets": [
                {
                  "name": "gerai-una-pos-ai.apk",
                  "updated_at": "2026-11-22T10:00:00Z",
                  "browser_download_url": "https://github.com/egisbahricha-commits/Pos-ai-egis/releases/download/v1.4/gerai-una-pos-ai.apk"
                },
                {
                  "name": "kaslite-pos.apk",
                  "updated_at": "2026-11-24T19:00:00Z",
                  "browser_download_url": "https://github.com/egisbahricha-commits/Pos-ai-egis/releases/download/v1.4/kaslite-pos.apk"
                }
              ]
            }
            """.trimIndent()
        )
        val (hasNewer14, info14) = vm.parseReleaseToUpdateInfo(multiAssetRelease, "1.3", 4)
        assertTrue("v1.4 should be detected as newer than 1.3", hasNewer14)
        assertEquals("v1.4", info14.latestVersionName)
        assertEquals(5, info14.latestVersionCode)
        assertTrue(
            "Must select kaslite-pos.apk instead of stale gerai-una-pos-ai.apk",
            info14.apkDownloadUrl.endsWith("/kaslite-pos.apk")
        )

        // Legacy tag 'latest-apk' with version embedded in title
        val legacyTagRelease = org.json.JSONObject(
            """
            {
              "tag_name": "latest-apk",
              "name": "KASLITE POS (Latest APK - v1.4)",
              "body": "Kode 5",
              "assets": [
                {
                  "name": "kaslite-pos.apk",
                  "updated_at": "2026-11-24T19:00:00Z",
                  "browser_download_url": "https://github.com/egisbahricha-commits/Pos-ai-egis/releases/download/latest-apk/kaslite-pos.apk"
                }
              ]
            }
            """.trimIndent()
        )
        val (hasNewerLegacy, infoLegacy) = vm.parseReleaseToUpdateInfo(legacyTagRelease, "1.3", 4)
        assertTrue("Title version v1.4 should be detected even on latest-apk tag", hasNewerLegacy)
        assertEquals("v1.4", infoLegacy.latestVersionName)
        assertEquals(5, infoLegacy.latestVersionCode)

        // Same version should not trigger false update
        val (hasNewerSame, _) = vm.parseReleaseToUpdateInfo(multiAssetRelease, "1.4", 5)
        assertFalse("Same version and code must not prompt update", hasNewerSame)
    }
}
