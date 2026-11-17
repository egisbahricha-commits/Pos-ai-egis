package com.example

import com.example.ai.GeraiUnaAIEngine
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.AdminTierRangeEntity
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.AdminTierWithRanges
import com.example.data.entity.ProductAliasEntity
import com.example.data.entity.ProductEntity
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.PosRepository
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
}
