package com.example.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.database.GeraiUnaDatabase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FirestoreSyncRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var roomDb: GeraiUnaDatabase
    private lateinit var sessionStore: FirebaseSessionStore
    private lateinit var repository: FirebaseSyncRepository

    @Before
    fun setUpRepo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        roomDb = Room.inMemoryDatabaseBuilder(context, GeraiUnaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionStore = FirebaseSessionStore(context)
        repository = FirebaseSyncRepository(firestore, roomDb, sessionStore)
    }

    @After
    fun closeRoom() {
        if (::roomDb.isInitialized) {
            roomDb.close()
        }
    }

    @Test
    fun upsertAndGetStoreProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val profile = UserStoreProfileDoc(
            id = uid,
            ownerId = uid,
            name = "Gerai Una Test",
            address = "Jl. Merdeka 10",
            phone = "0812345678",
            businessType = "keduanya",
            ppnBp = 1100L,
            receiptFooter = "Terima kasih",
            ownerName = "Alice",
            ownerEmail = ALICE_EMAIL,
            profileUpdatedAtMillis = System.currentTimeMillis()
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertStoreProfile(uid, profile)
            val fetched = repository.getStoreProfile(uid)
            assertNotNull(fetched)
            assertEquals("Gerai Una Test", fetched?.name)
            assertEquals(uid, fetched?.ownerId)
            assertNotNull(fetched?.createdAt)
        }
    }

    @Test
    fun observeCashAccounts_authenticatedOwner_emitsInsertedAccount() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val docId = "acc_${UUID.randomUUID().toString().replace("-", "")}"
        val account = CashAccountDoc(
            id = docId,
            ownerId = uid,
            deviceId = "dev_test_01",
            localId = 1L,
            accountName = "Kas Toko Utama",
            balance = 1750000.0,
            accountType = "CASH",
            isActive = true,
            color = "#16A34A",
            notes = "Saldo awal",
            isDefault = true,
            sortOrder = 1L
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertCashAccountDoc(uid, account)
        }
        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCashAccounts(uid).first { accounts ->
                accounts.any { it.id == docId }
            }
        }
        val matched = list.first { it.id == docId }
        assertEquals("Kas Toko Utama", matched.accountName)
        assertEquals(1750000.0, matched.balance, 0.01)
    }

    @Test
    fun getStoreProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertStoreProfile(
                aliceUid,
                UserStoreProfileDoc(
                    id = aliceUid,
                    ownerId = aliceUid,
                    name = "Alice Store",
                    ownerName = "Alice",
                    ownerEmail = ALICE_EMAIL,
                    profileUpdatedAtMillis = System.currentTimeMillis()
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.getStoreProfile(aliceUid)
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's store profile")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun observeCashAccounts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCashAccounts("alice_dummy_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
            assertTrue(true)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.pos@test.com"
        const val BOB_EMAIL = "bob.pos@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
