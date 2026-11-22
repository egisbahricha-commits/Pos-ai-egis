package com.example.firebase

import android.content.Context
import androidx.room.withTransaction
import com.example.R
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CashMutationEntity
import com.example.data.entity.CashTransferEntity
import com.example.data.entity.CashierShiftEntity
import com.example.data.entity.CloudSyncMappingEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.StoreProfileEntity
import com.example.data.entity.SupplierEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncRepository(
    private val db: FirebaseFirestore,
    private val roomDb: GeraiUnaDatabase,
    private val sessionStore: FirebaseSessionStore
) {
    constructor(
        context: Context,
        roomDb: GeraiUnaDatabase,
        sessionStore: FirebaseSessionStore
    ) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        roomDb,
        sessionStore
    )

    private val productDao by lazy { roomDb.productDao() }
    private val saleDao by lazy { roomDb.saleDao() }
    private val financeDao by lazy { roomDb.financeDao() }
    private val contactDao by lazy { roomDb.contactDao() }
    private val cashAccountDao by lazy { roomDb.cashAccountDao() }
    private val shiftDao by lazy { roomDb.shiftDao() }
    private val extDao by lazy { roomDb.extendedPosDao() }

    private fun userDocRef(userId: String) = db.collection("users").document(userId)

    fun observeCashAccounts(userId: String): Flow<List<CashAccountDoc>> =
        userDocRef(userId).collection("cashAccounts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CashAccountDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeCashMutations(userId: String): Flow<List<CashMutationDoc>> =
        userDocRef(userId).collection("cashMutations")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CashMutationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.mutationTimeMillis }
            }

    suspend fun getStoreProfile(userId: String): UserStoreProfileDoc? {
        val docRef = userDocRef(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    UserStoreProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertStoreProfile(userId: String, profile: UserStoreProfileDoc) {
        val docRef = userDocRef(userId)
        try {
            val existingSnap = docRef.get().await()
            val existingCreatedAt = existingSnap.getTimestamp("createdAt")
            val payload = profile.copy(
                id = userId,
                ownerId = userId,
                createdAt = existingCreatedAt,
                updatedAt = null
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun upsertCashAccountDoc(userId: String, account: CashAccountDoc) {
        val colRef = userDocRef(userId).collection("cashAccounts")
        val docId = account.id.ifBlank { "${userId}_manual_${account.localId}" }
        val docRef = colRef.document(docId)
        try {
            val existingSnap = docRef.get().await()
            val existingCreatedAt = existingSnap.getTimestamp("createdAt")
            val payload = account.copy(
                id = docId,
                ownerId = userId,
                createdAt = existingCreatedAt,
                updatedAt = null
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    private suspend fun getOrCreateRemoteId(
        entityType: String,
        localId: Long,
        ownerId: String,
        deviceId: String
    ): String {
        val existing = extDao.getMappingByLocal(entityType, localId)
        if (existing != null && existing.remoteId.matches(VALID_ID_REGEX)) {
            return existing.remoteId
        }
        val safeOwner = ownerId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val safeDevice = deviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val remoteId = "${safeOwner}_${safeDevice}_${entityType}_$localId".take(120)
        extDao.upsertCloudMapping(
            CloudSyncMappingEntity(
                entityType = entityType,
                remoteId = remoteId,
                localId = localId
            )
        )
        return remoteId
    }

    private suspend fun fetchCreatedAtMap(colRef: CollectionReference): Map<String, Timestamp?> {
        return try {
            colRef.get().await().documents.associate { doc ->
                doc.id to doc.getTimestamp("createdAt")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    private suspend fun <T : Any> commitDocsInBatches(
        colRef: CollectionReference,
        items: List<Pair<String, T>>
    ): Int {
        if (items.isEmpty()) return 0
        var count = 0
        for (chunk in items.chunked(400)) {
            val batch = db.batch()
            for ((docId, payload) in chunk) {
                batch.set(colRef.document(docId), payload)
            }
            try {
                batch.commit().await()
                count += chunk.size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, colRef.path)
                throw e
            }
        }
        return count
    }

    suspend fun sync(
        userId: String,
        ownerName: String,
        ownerEmail: String,
        mode: SyncMode,
        isInitialLoginPull: Boolean = false
    ): Result<CloudSyncSummary> = withContext(Dispatchers.IO) {
        runCatching {
            require(userId.isNotBlank()) { "Belum masuk dengan akun Google / Firebase." }
            var pushed = 0
            var pulled = 0

            if (mode == SyncMode.PULL_ONLY || mode == SyncMode.TWO_WAY) {
                pulled = pullFromCloud(
                    userId = userId,
                    replaceDemoIfFirstPull = isInitialLoginPull && !sessionStore.hasCloudSyncedBefore()
                )
            }
            if (mode == SyncMode.PUSH_ONLY || mode == SyncMode.TWO_WAY) {
                pushed = pushToCloud(
                    userId = userId,
                    ownerName = ownerName,
                    ownerEmail = ownerEmail
                )
            }

            val now = System.currentTimeMillis()
            sessionStore.setLastSyncAt(now)
            sessionStore.setHasCloudSyncedBefore(true)

            val summaryMsg = when (mode) {
                SyncMode.PUSH_ONLY -> "Terkirim $pushed data ke Firebase Firestore"
                SyncMode.PULL_ONLY -> "Terunduh $pulled data dari Firebase Firestore"
                SyncMode.TWO_WAY -> "Sinkronisasi Firebase sukses ($pushed dikirim, $pulled diperbarui)"
            }
            CloudSyncSummary(
                pushedRecords = pushed,
                pulledRecords = pulled,
                timestamp = now,
                message = summaryMsg
            )
        }
    }

    suspend fun pushToCloud(
        userId: String,
        ownerName: String,
        ownerEmail: String
    ): Int = withContext(Dispatchers.IO) {
        val deviceId = sessionStore.getOrCreateDeviceId()
        var total = 0

        // 1. Store Profile (always ensure /users/{userId} exists)
        val store = extDao.getStoreProfileSync() ?: StoreProfileEntity()
        val storeDoc = UserStoreProfileDoc(
            id = userId,
            ownerId = userId,
            name = store.name.ifBlank { sessionStore.getStoreNameHint() }.take(120),
            address = store.address.take(300),
            phone = store.phone.take(40),
            businessType = store.businessType.ifBlank { "keduanya" }.take(40),
            ppnBp = store.ppnBp.toLong().coerceIn(0L, 10000L),
            receiptFooter = store.receiptFooter.take(300),
            ownerName = ownerName.take(120),
            ownerEmail = ownerEmail.take(200),
            profileUpdatedAtMillis = store.updatedAt.coerceAtLeast(0L)
        )
        upsertStoreProfile(userId, storeDoc)
        total += 1

        // 2. Cash Accounts
        val accounts = cashAccountDao.getAllAccountsList()
        if (accounts.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("cashAccounts")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = accounts.map { acc ->
                val rid = getOrCreateRemoteId("CASH_ACCOUNT", acc.id, userId, deviceId)
                rid to CashAccountDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = acc.id,
                    accountName = acc.accountName.ifBlank { "Kas" }.take(120),
                    balance = acc.balance,
                    accountType = acc.accountType.ifBlank { "CASH" }.take(40),
                    isActive = acc.isActive,
                    color = acc.color.take(30),
                    notes = acc.notes.take(500),
                    isDefault = acc.isDefault,
                    sortOrder = acc.sortOrder.toLong(),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 3. Cash Mutations (Arus Kas Ledger)
        val mutations = extDao.getAllCashMutations().first()
        if (mutations.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("cashMutations")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = mutations.map { m ->
                val rid = getOrCreateRemoteId("CASH_MUTATION", m.id, userId, deviceId)
                rid to CashMutationDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = m.id,
                    cashAccountId = m.cashAccountId.coerceAtLeast(0L),
                    accountName = m.accountName.ifBlank { "Kas Toko" }.take(120),
                    amount = m.amount,
                    mutationType = m.mutationType.ifBlank { "koreksi" }.take(60),
                    refType = m.refType.take(60),
                    refId = m.refId.take(120),
                    notes = m.notes.take(500),
                    mutationTimeMillis = m.timestamp.coerceAtLeast(0L),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 4. Cash Transfers
        val transfers = cashAccountDao.getAllTransfers().first()
        if (transfers.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("cashTransfers")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = transfers.map { t ->
                val rid = getOrCreateRemoteId("CASH_TRANSFER", t.id, userId, deviceId)
                rid to CashTransferDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = t.id,
                    fromAccountName = t.fromAccountName.take(120),
                    toAccountName = t.toAccountName.take(120),
                    amount = t.amount.coerceAtLeast(0.0),
                    notes = t.notes.take(500),
                    transferDateMillis = t.date.coerceAtLeast(0L),
                    loggedBy = t.loggedBy.take(120),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 5. Sales
        val sales = saleDao.getAllSales().first()
        if (sales.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("sales")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = sales.map { s ->
                val rid = getOrCreateRemoteId("SALE", s.id, userId, deviceId)
                rid to SaleDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = s.id,
                    invoiceNumber = s.invoiceNumber.ifBlank { "TRX-${s.id}" }.take(100),
                    saleDateMillis = s.date.coerceAtLeast(0L),
                    cashierName = s.cashierName.take(120),
                    customerName = s.customerName.take(120),
                    subtotal = s.subtotal,
                    discount = s.discount,
                    tax = s.tax,
                    finalAmount = s.finalAmount,
                    paymentMethod = s.paymentMethod.ifBlank { "TUNAI" }.take(60),
                    amountPaid = s.amountPaid,
                    changeAmount = s.changeAmount,
                    status = s.status.ifBlank { "COMPLETED" }.take(40),
                    notes = s.notes.take(500),
                    storeAdminFee = s.storeAdminFee,
                    providerAdminFee = s.providerAdminFee,
                    agentCommission = s.agentCommission,
                    shippingCost = s.shippingCost,
                    additionalFee = s.additionalFee,
                    promoDiscount = s.promoDiscount,
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 6. Expenses
        val expenses = financeDao.getAllExpenses().first()
        if (expenses.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("expenses")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = expenses.map { e ->
                val rid = getOrCreateRemoteId("EXPENSE", e.id, userId, deviceId)
                rid to ExpenseDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = e.id,
                    category = e.category.ifBlank { "Operasional" }.take(100),
                    amount = e.amount.coerceAtLeast(0.0),
                    expenseDateMillis = e.date.coerceAtLeast(0L),
                    notes = e.notes.take(500),
                    paymentSource = e.paymentSource.take(120),
                    recordedBy = e.recordedBy.take(120),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 7. Incomes
        val incomes = financeDao.getAllIncomes().first()
        if (incomes.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("incomes")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = incomes.map { inc ->
                val rid = getOrCreateRemoteId("INCOME", inc.id, userId, deviceId)
                rid to IncomeDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = inc.id,
                    source = inc.source.ifBlank { "Lainnya" }.take(120),
                    amount = inc.amount.coerceAtLeast(0.0),
                    incomeDateMillis = inc.date.coerceAtLeast(0L),
                    notes = inc.notes.take(500),
                    recordedBy = inc.recordedBy.take(120),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 8. Products
        val products = productDao.getAllProductsList()
        if (products.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("products")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = products.map { p ->
                val rid = getOrCreateRemoteId("PRODUCT", p.id, userId, deviceId)
                rid to ProductDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = p.id,
                    barcode = p.barcode.take(120),
                    name = p.name.ifBlank { "Produk" }.take(200),
                    category = p.category.take(100),
                    baseUnit = p.baseUnit.ifBlank { "pcs" }.take(40),
                    purchasePrice = p.purchasePrice,
                    sellingPrice = p.sellingPrice,
                    stock = p.stock,
                    minStock = p.minStock,
                    productType = p.productType.ifBlank { "FISIK" }.take(40),
                    isActive = p.isActive,
                    hasNoStock = p.hasNoStock,
                    allowDecimalQty = p.allowDecimalQty,
                    isFlexiblePrice = p.isFlexiblePrice,
                    specialPrice = p.specialPrice,
                    strikePrice = p.strikePrice,
                    imageUri = p.imageUri,
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 9. Customers
        val customers = contactDao.getAllCustomers().first()
        if (customers.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("customers")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = customers.map { c ->
                val rid = getOrCreateRemoteId("CUSTOMER", c.id, userId, deviceId)
                rid to CustomerDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = c.id,
                    name = c.name.ifBlank { "Pelanggan" }.take(160),
                    phone = c.phone.take(40),
                    address = c.address.take(300),
                    notes = c.notes.take(500),
                    totalReceivable = c.totalReceivable,
                    depositBalance = c.depositBalance,
                    loyaltyPoints = c.loyaltyPoints.toLong().coerceAtLeast(0L),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 10. Suppliers
        val suppliers = contactDao.getAllSuppliers().first()
        if (suppliers.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("suppliers")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = suppliers.map { sup ->
                val rid = getOrCreateRemoteId("SUPPLIER", sup.id, userId, deviceId)
                rid to SupplierDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = sup.id,
                    name = sup.name.ifBlank { "Pemasok" }.take(160),
                    phone = sup.phone.take(40),
                    address = sup.address.take(300),
                    notes = sup.notes.take(500),
                    totalDebt = sup.totalDebt,
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 11. Debt Payments
        val debtPayments = financeDao.getAllDebtPayments().first()
        if (debtPayments.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("debtPayments")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = debtPayments.map { dp ->
                val rid = getOrCreateRemoteId("DEBT_PAYMENT", dp.id, userId, deviceId)
                rid to DebtPaymentDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = dp.id,
                    paymentType = dp.type.ifBlank { "PIUTANG_PELANGGAN" }.take(60),
                    referenceInvoice = dp.referenceInvoice.take(120),
                    partyName = dp.partyName.take(160),
                    amount = dp.amount.coerceAtLeast(0.0),
                    paymentDateMillis = dp.date.coerceAtLeast(0L),
                    paymentMethod = dp.paymentMethod.take(60),
                    notes = dp.notes.take(500),
                    recordedBy = dp.recordedBy.take(120),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        // 12. Cashier Shifts
        val shifts = shiftDao.getAllShifts().first()
        if (shifts.isNotEmpty()) {
            val colRef = userDocRef(userId).collection("shifts")
            val createdMap = fetchCreatedAtMap(colRef)
            val batchItems = shifts.map { sh ->
                val rid = getOrCreateRemoteId("SHIFT", sh.id, userId, deviceId)
                rid to CashierShiftDoc(
                    id = rid,
                    ownerId = userId,
                    deviceId = deviceId,
                    localId = sh.id,
                    cashierName = sh.cashierName.ifBlank { "Kasir" }.take(120),
                    startTimeMillis = sh.startTime.coerceAtLeast(0L),
                    endTimeMillis = sh.endTime?.coerceAtLeast(0L),
                    startingCash = sh.startingCash,
                    cashIn = sh.cashIn,
                    cashOut = sh.cashOut,
                    totalCashSales = sh.totalCashSales,
                    expectedClosingCash = sh.expectedClosingCash,
                    actualClosingCash = sh.actualClosingCash,
                    difference = sh.difference,
                    status = sh.status.ifBlank { "OPEN" }.take(40),
                    notes = sh.notes.take(500),
                    createdAt = createdMap[rid],
                    updatedAt = null
                )
            }
            total += commitDocsInBatches(colRef, batchItems)
        }

        total
    }

    private suspend inline fun <reified T : Any> fetchCollection(
        userId: String,
        collectionName: String
    ): List<T> {
        val colRef = userDocRef(userId).collection(collectionName)
        return try {
            colRef.get().await().toObjects(
                T::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun pullFromCloud(
        userId: String,
        replaceDemoIfFirstPull: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val remoteStoreProfile = getStoreProfile(userId)
        val remoteCashAccounts = fetchCollection<CashAccountDoc>(userId, "cashAccounts")
        val remoteMutations = fetchCollection<CashMutationDoc>(userId, "cashMutations")
        val remoteTransfers = fetchCollection<CashTransferDoc>(userId, "cashTransfers")
        val remoteSales = fetchCollection<SaleDoc>(userId, "sales")
        val remoteExpenses = fetchCollection<ExpenseDoc>(userId, "expenses")
        val remoteIncomes = fetchCollection<IncomeDoc>(userId, "incomes")
        val remoteProducts = fetchCollection<ProductDoc>(userId, "products")
        val remoteCustomers = fetchCollection<CustomerDoc>(userId, "customers")
        val remoteSuppliers = fetchCollection<SupplierDoc>(userId, "suppliers")
        val remoteDebts = fetchCollection<DebtPaymentDoc>(userId, "debtPayments")
        val remoteShifts = fetchCollection<CashierShiftDoc>(userId, "shifts")

        val cloudHasData = remoteCashAccounts.isNotEmpty() ||
            remoteSales.isNotEmpty() ||
            remoteProducts.isNotEmpty() ||
            remoteMutations.isNotEmpty()

        if (!cloudHasData && remoteStoreProfile == null) return@withContext 0

        var pulledCount = 0
        roomDb.withTransaction {
            if (replaceDemoIfFirstPull && cloudHasData) {
                roomDb.openHelper.writableDatabase.apply {
                    execSQL("DELETE FROM cash_accounts")
                    execSQL("DELETE FROM cash_mutations")
                    execSQL("DELETE FROM cash_transfers")
                    execSQL("DELETE FROM sales")
                    execSQL("DELETE FROM sale_items")
                    execSQL("DELETE FROM expenses")
                    execSQL("DELETE FROM incomes")
                    execSQL("DELETE FROM products")
                    execSQL("DELETE FROM customers")
                    execSQL("DELETE FROM suppliers")
                    execSQL("DELETE FROM debt_payments")
                    execSQL("DELETE FROM cashier_shifts")
                    execSQL("DELETE FROM cloud_sync_mappings")
                }
            }

            if (remoteStoreProfile != null) {
                extDao.upsertStoreProfile(
                    StoreProfileEntity(
                        id = 1L,
                        name = remoteStoreProfile.name.ifBlank { "Toko Saya" },
                        address = remoteStoreProfile.address,
                        phone = remoteStoreProfile.phone,
                        businessType = remoteStoreProfile.businessType.ifBlank { "keduanya" },
                        ppnBp = remoteStoreProfile.ppnBp.toInt(),
                        receiptFooter = remoteStoreProfile.receiptFooter,
                        updatedAt = remoteStoreProfile.profileUpdatedAtMillis
                    )
                )
                pulledCount++
            }

            val existingCashAccounts = cashAccountDao.getAllAccountsList()
            val cashByName = existingCashAccounts.associateBy { it.accountName.trim().lowercase() }.toMutableMap()
            var hasDefaultAccount = existingCashAccounts.any { it.isDefault }

            for (item in remoteCashAccounts) {
                val mapped = extDao.getMappingByRemote("CASH_ACCOUNT", item.id)
                val nameKey = item.accountName.trim().lowercase()
                val existingByName = cashByName[nameKey]
                val resolvedId = mapped?.localId ?: existingByName?.id ?: 0L
                val isDefaultResolved = if (item.isDefault && (!hasDefaultAccount || existingByName?.isDefault == true || mapped?.localId != null && existingCashAccounts.any { it.id == mapped.localId && it.isDefault })) {
                    hasDefaultAccount = true
                    true
                } else {
                    false
                }
                val entity = CashAccountEntity(
                    id = resolvedId,
                    accountName = item.accountName,
                    balance = item.balance,
                    accountType = item.accountType,
                    isActive = item.isActive,
                    color = item.color,
                    notes = item.notes,
                    isDefault = isDefaultResolved,
                    sortOrder = item.sortOrder.toInt()
                )
                val localId = cashAccountDao.insertCashAccount(entity)
                cashByName[nameKey] = entity.copy(id = localId)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "CASH_ACCOUNT",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            val allCashById = cashByName.values.associateBy { it.id }
            for (item in remoteMutations) {
                val mapped = extDao.getMappingByRemote("CASH_MUTATION", item.id)
                val resolvedCashId = cashByName[item.accountName.trim().lowercase()]?.id
                    ?: allCashById[item.cashAccountId]?.id
                    ?: item.cashAccountId
                val entity = CashMutationEntity(
                    id = mapped?.localId ?: 0L,
                    cashAccountId = resolvedCashId,
                    accountName = item.accountName,
                    amount = item.amount,
                    mutationType = item.mutationType,
                    refType = item.refType,
                    refId = item.refId,
                    notes = item.notes,
                    timestamp = item.mutationTimeMillis
                )
                val localId = extDao.insertCashMutation(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "CASH_MUTATION",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteTransfers) {
                val mapped = extDao.getMappingByRemote("CASH_TRANSFER", item.id)
                val fromId = cashByName[item.fromAccountName.trim().lowercase()]?.id ?: 0L
                val toId = cashByName[item.toAccountName.trim().lowercase()]?.id ?: 0L
                val entity = CashTransferEntity(
                    id = mapped?.localId ?: 0L,
                    fromAccountId = fromId,
                    fromAccountName = item.fromAccountName,
                    toAccountId = toId,
                    toAccountName = item.toAccountName,
                    amount = item.amount,
                    notes = item.notes,
                    date = item.transferDateMillis,
                    loggedBy = item.loggedBy
                )
                val localId = cashAccountDao.insertTransfer(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "CASH_TRANSFER",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteSales) {
                val mapped = extDao.getMappingByRemote("SALE", item.id)
                val entity = SaleEntity(
                    id = mapped?.localId ?: 0L,
                    invoiceNumber = item.invoiceNumber,
                    date = item.saleDateMillis,
                    cashierName = item.cashierName,
                    customerName = item.customerName,
                    subtotal = item.subtotal,
                    discount = item.discount,
                    tax = item.tax,
                    finalAmount = item.finalAmount,
                    paymentMethod = item.paymentMethod,
                    amountPaid = item.amountPaid,
                    changeAmount = item.changeAmount,
                    status = item.status,
                    notes = item.notes,
                    storeAdminFee = item.storeAdminFee,
                    providerAdminFee = item.providerAdminFee,
                    agentCommission = item.agentCommission,
                    shippingCost = item.shippingCost,
                    additionalFee = item.additionalFee,
                    promoDiscount = item.promoDiscount
                )
                val localId = saleDao.insertSale(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "SALE",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteExpenses) {
                val mapped = extDao.getMappingByRemote("EXPENSE", item.id)
                val entity = ExpenseEntity(
                    id = mapped?.localId ?: 0L,
                    category = item.category,
                    amount = item.amount,
                    date = item.expenseDateMillis,
                    notes = item.notes,
                    paymentSource = item.paymentSource,
                    recordedBy = item.recordedBy
                )
                val localId = financeDao.insertExpense(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "EXPENSE",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteIncomes) {
                val mapped = extDao.getMappingByRemote("INCOME", item.id)
                val entity = IncomeEntity(
                    id = mapped?.localId ?: 0L,
                    source = item.source,
                    amount = item.amount,
                    date = item.incomeDateMillis,
                    notes = item.notes,
                    recordedBy = item.recordedBy
                )
                val localId = financeDao.insertIncome(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "INCOME",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteProducts) {
                val mapped = extDao.getMappingByRemote("PRODUCT", item.id)
                val entity = ProductEntity(
                    id = mapped?.localId ?: 0L,
                    barcode = item.barcode,
                    name = item.name,
                    category = item.category,
                    baseUnit = item.baseUnit,
                    purchasePrice = item.purchasePrice,
                    sellingPrice = item.sellingPrice,
                    stock = item.stock,
                    minStock = item.minStock,
                    productType = item.productType,
                    isActive = item.isActive,
                    hasNoStock = item.hasNoStock,
                    allowDecimalQty = item.allowDecimalQty,
                    isFlexiblePrice = item.isFlexiblePrice,
                    specialPrice = item.specialPrice,
                    strikePrice = item.strikePrice,
                    imageUri = item.imageUri
                )
                val localId = productDao.insertProduct(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "PRODUCT",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteCustomers) {
                val mapped = extDao.getMappingByRemote("CUSTOMER", item.id)
                val entity = CustomerEntity(
                    id = mapped?.localId ?: 0L,
                    name = item.name,
                    phone = item.phone,
                    address = item.address,
                    notes = item.notes,
                    totalReceivable = item.totalReceivable,
                    depositBalance = item.depositBalance,
                    loyaltyPoints = item.loyaltyPoints.toInt()
                )
                val localId = contactDao.insertCustomer(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "CUSTOMER",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteSuppliers) {
                val mapped = extDao.getMappingByRemote("SUPPLIER", item.id)
                val entity = SupplierEntity(
                    id = mapped?.localId ?: 0L,
                    name = item.name,
                    phone = item.phone,
                    address = item.address,
                    notes = item.notes,
                    totalDebt = item.totalDebt
                )
                val localId = contactDao.insertSupplier(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "SUPPLIER",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteDebts) {
                val mapped = extDao.getMappingByRemote("DEBT_PAYMENT", item.id)
                val entity = DebtPaymentEntity(
                    id = mapped?.localId ?: 0L,
                    type = item.paymentType,
                    referenceInvoice = item.referenceInvoice,
                    partyName = item.partyName,
                    amount = item.amount,
                    date = item.paymentDateMillis,
                    paymentMethod = item.paymentMethod,
                    notes = item.notes,
                    recordedBy = item.recordedBy
                )
                val localId = financeDao.insertDebtPayment(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "DEBT_PAYMENT",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }

            for (item in remoteShifts) {
                val mapped = extDao.getMappingByRemote("SHIFT", item.id)
                val entity = CashierShiftEntity(
                    id = mapped?.localId ?: 0L,
                    cashierName = item.cashierName,
                    startTime = item.startTimeMillis,
                    endTime = item.endTimeMillis,
                    startingCash = item.startingCash,
                    cashIn = item.cashIn,
                    cashOut = item.cashOut,
                    totalCashSales = item.totalCashSales,
                    expectedClosingCash = item.expectedClosingCash,
                    actualClosingCash = item.actualClosingCash,
                    difference = item.difference,
                    status = item.status,
                    notes = item.notes
                )
                val localId = shiftDao.insertShift(entity)
                if (mapped == null) {
                    extDao.upsertCloudMapping(
                        CloudSyncMappingEntity(
                            entityType = "SHIFT",
                            remoteId = item.id,
                            localId = localId
                        )
                    )
                }
                pulledCount++
            }
        }
        pulledCount
    }

    suspend fun getLicenseByKey(key: String): LicenseDoc? {
        val cleanKey = key.trim()
        if (cleanKey.isBlank()) return null
        val docRef = db.collection("licenses").document(cleanKey)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(LicenseDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun fetchLicenseByOwnerUid(ownerUid: String): LicenseDoc? {
        if (ownerUid.isBlank()) return null
        val colRef = db.collection("licenses")
        return try {
            val snap = colRef.whereEqualTo("ownerUid", ownerUid).limit(1).get().await()
            snap.documents.firstOrNull()?.toObject(
                LicenseDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun getAllLicenses(): List<LicenseDoc> {
        val colRef = db.collection("licenses")
        return try {
            val snap = colRef.get().await()
            snap.toObjects(LicenseDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { maxOf(it.demoStartedAtMillis, it.activatedAtMillis) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun registerOrFetchCustomerLicense(
        ownerUid: String,
        ownerEmail: String,
        ownerName: String,
        storeName: String,
        phone: String,
        deviceId: String
    ): LicenseDoc = withContext(Dispatchers.IO) {
        val existing = fetchLicenseByOwnerUid(ownerUid)
        if (existing != null) {
            val targetDocId = existing.id.ifBlank { existing.licenseKey }
            val docRef = db.collection("licenses").document(targetDocId)
            val updates = mutableMapOf<String, Any>(
                "storeName" to storeName.ifBlank { existing.storeName }.take(120),
                "ownerName" to ownerName.ifBlank { existing.ownerName }.take(120),
                "phone" to phone.ifBlank { existing.phone }.take(40),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing.deviceId.isBlank() && deviceId.isNotBlank()) {
                updates["deviceId"] = deviceId.take(128)
            }
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
            return@withContext getLicenseByKey(targetDocId) ?: existing.copy(
                deviceId = existing.deviceId.ifBlank { deviceId },
                storeName = storeName.ifBlank { existing.storeName },
                ownerName = ownerName.ifBlank { existing.ownerName },
                phone = phone.ifBlank { existing.phone }
            )
        }

        val now = System.currentTimeMillis()
        val demoExpires = now + DEMO_DURATION_MILLIS
        val suffix = ownerUid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase().take(6).ifBlank {
            (100000..999999).random().toString()
        }
        val key = "KSL-$suffix-${(1000..9999).random()}"
        val docRef = db.collection("licenses").document(key)
        val payloadMap = hashMapOf<String, Any>(
            "licenseKey" to key,
            "storeName" to storeName.ifBlank { "Toko KASLITE" }.take(120),
            "ownerName" to ownerName.ifBlank { ownerEmail.substringBefore("@") }.take(120),
            "ownerEmail" to ownerEmail.trim().take(200),
            "ownerUid" to ownerUid.take(128),
            "phone" to phone.trim().take(40),
            "deviceId" to deviceId.take(128),
            "planType" to "DEMO",
            "status" to "DEMO",
            "demoStartedAtMillis" to now,
            "demoExpiresAtMillis" to demoExpires,
            "activatedAtMillis" to 0L,
            "expiresAtMillis" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payloadMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        getLicenseByKey(key) ?: LicenseDoc(
            id = key,
            licenseKey = key,
            storeName = storeName.ifBlank { "Toko KASLITE" },
            ownerName = ownerName.ifBlank { ownerEmail.substringBefore("@") },
            ownerEmail = ownerEmail.trim(),
            ownerUid = ownerUid,
            phone = phone.trim(),
            deviceId = deviceId,
            planType = "DEMO",
            status = "DEMO",
            demoStartedAtMillis = now,
            demoExpiresAtMillis = demoExpires,
            activatedAtMillis = 0L,
            expiresAtMillis = 0L
        )
    }

    suspend fun activateLicenseKeyForCustomer(
        licenseKey: String,
        ownerUid: String,
        ownerEmail: String,
        ownerName: String,
        storeName: String,
        phone: String,
        deviceId: String
    ): LicenseDoc = withContext(Dispatchers.IO) {
        val cleanKey = licenseKey.trim()
        require(cleanKey.matches(VALID_ID_REGEX)) { "Format kunci lisensi tidak valid." }
        val existing = getLicenseByKey(cleanKey)
            ?: throw IllegalStateException("Kunci lisensi \"$cleanKey\" tidak ditemukan di server Admin Pusat.")

        if (existing.status == "SUSPENDED") {
            throw IllegalStateException("Lisensi ini sedang dinonaktifkan oleh Admin Pusat.")
        }
        if (existing.deviceId.isNotBlank() && existing.deviceId != deviceId) {
            throw IllegalStateException("Lisensi sudah terikat di perangkat lain. Hubungi Admin Pusat (egisbahricha@gmail.com) untuk reset perangkat.")
        }
        if (existing.ownerUid.isNotBlank() && existing.ownerUid != ownerUid) {
            throw IllegalStateException("Lisensi ini terdaftar atas akun Google lain (${existing.ownerEmail}).")
        }

        val docId = existing.id.ifBlank { cleanKey }
        val docRef = db.collection("licenses").document(docId)
        val updates = mutableMapOf<String, Any>(
            "deviceId" to deviceId.take(128),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (existing.ownerUid.isBlank()) {
            updates["ownerUid"] = ownerUid.take(128)
            updates["ownerEmail"] = ownerEmail.trim().take(200)
        }
        if (storeName.isNotBlank()) updates["storeName"] = storeName.take(120)
        if (ownerName.isNotBlank()) updates["ownerName"] = ownerName.take(120)
        if (phone.isNotBlank()) updates["phone"] = phone.take(40)

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
        getLicenseByKey(docId) ?: existing.copy(deviceId = deviceId, ownerUid = ownerUid)
    }

    suspend fun createSuperAdminLicense(
        storeName: String,
        ownerName: String,
        ownerEmail: String,
        phone: String = "",
        planType: String = "FULL"
    ): LicenseDoc = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val key = "KSL-" + (1000..9999).random() + "-" + (1000..9999).random()
        val isFull = planType.equals("FULL", ignoreCase = true)
        val status = if (isFull) "ACTIVE" else "DEMO"
        val demoStart = if (isFull) 0L else now
        val demoExp = if (isFull) 0L else now + DEMO_DURATION_MILLIS
        val activatedAt = if (isFull) now else 0L
        val expiresAt = if (isFull) now + 3650L * 24 * 60 * 60 * 1000L else 0L

        val docRef = db.collection("licenses").document(key)
        val payloadMap = hashMapOf<String, Any>(
            "licenseKey" to key,
            "storeName" to storeName.trim().ifBlank { "Toko KASLITE" }.take(120),
            "ownerName" to ownerName.trim().take(120),
            "ownerEmail" to ownerEmail.trim().take(200),
            "ownerUid" to "",
            "phone" to phone.trim().take(40),
            "deviceId" to "",
            "planType" to if (isFull) "FULL" else "DEMO",
            "status" to status,
            "demoStartedAtMillis" to demoStart,
            "demoExpiresAtMillis" to demoExp,
            "activatedAtMillis" to activatedAt,
            "expiresAtMillis" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payloadMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        getLicenseByKey(key) ?: LicenseDoc(
            id = key,
            licenseKey = key,
            storeName = storeName.trim(),
            ownerName = ownerName.trim(),
            ownerEmail = ownerEmail.trim(),
            phone = phone.trim(),
            planType = if (isFull) "FULL" else "DEMO",
            status = status,
            demoStartedAtMillis = demoStart,
            demoExpiresAtMillis = demoExp,
            activatedAtMillis = activatedAt,
            expiresAtMillis = expiresAt
        )
    }

    suspend fun superAdminActivateFull(licenseKey: String, durationDays: Int = 3650) {
        val now = System.currentTimeMillis()
        val exp = now + durationDays.toLong() * 24L * 60L * 60L * 1000L
        val docRef = db.collection("licenses").document(licenseKey)
        try {
            docRef.update(
                mapOf(
                    "planType" to "FULL",
                    "status" to "ACTIVE",
                    "activatedAtMillis" to now,
                    "expiresAtMillis" to exp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun superAdminExtendDemo(licenseKey: String, days: Int = 3) {
        val now = System.currentTimeMillis()
        val docRef = db.collection("licenses").document(licenseKey)
        try {
            val snap = docRef.get().await()
            val currentExp = snap.getLong("demoExpiresAtMillis") ?: 0L
            val base = maxOf(now, currentExp)
            val newExp = base + days.toLong() * 24L * 60L * 60L * 1000L
            docRef.update(
                mapOf(
                    "planType" to "DEMO",
                    "status" to "DEMO",
                    "demoStartedAtMillis" to (snap.getLong("demoStartedAtMillis")?.takeIf { it > 0L } ?: now),
                    "demoExpiresAtMillis" to newExp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun superAdminSetSuspended(licenseKey: String, suspended: Boolean) {
        val docRef = db.collection("licenses").document(licenseKey)
        try {
            val snap = docRef.get().await()
            val planType = snap.getString("planType") ?: "DEMO"
            val nextStatus = if (suspended) "SUSPENDED" else if (planType == "FULL") "ACTIVE" else "DEMO"
            docRef.update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun resetDeviceBinding(licenseKey: String) {
        val docRef = db.collection("licenses").document(licenseKey)
        try {
            docRef.update(
                mapOf(
                    "deviceId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeAllLicenses(): Flow<List<LicenseDoc>> =
        db.collection("licenses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LicenseDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
        const val DEMO_DURATION_MILLIS = 3L * 24L * 60L * 60L * 1000L
    }
}
