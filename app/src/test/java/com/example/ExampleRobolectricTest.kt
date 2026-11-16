package com.example

import com.example.ai.GeraiUnaAIEngine
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.ProductAliasEntity
import com.example.data.entity.ProductEntity
import com.example.data.repository.PosRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
