package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val db: GeraiUnaDatabase) {

    private val productDao by lazy { db.productDao() }
    private val saleDao by lazy { db.saleDao() }
    private val purchaseDao by lazy { db.purchaseDao() }
    private val stockMovementDao by lazy { db.stockMovementDao() }
    private val financeDao by lazy { db.financeDao() }
    private val contactDao by lazy { db.contactDao() }
    private val aiDao by lazy { db.aiDao() }
    private val settingsDao by lazy { db.appSettingsDao() }
    private val cashAccountDao by lazy { db.cashAccountDao() }
    private val adminTierDao by lazy { db.adminTierDao() }
    private val shiftDao by lazy { db.shiftDao() }
    private val extDao by lazy { db.extendedPosDao() }

    // Reactive Flows
    val allProducts: Flow<List<ProductEntity>> by lazy { productDao.getAllProducts() }
    val activeProducts: Flow<List<ProductEntity>> by lazy { productDao.getActiveProducts() }
    val lowStockProducts: Flow<List<ProductEntity>> by lazy { productDao.getLowStockProducts() }
    val categories: Flow<List<CategoryEntity>> by lazy { productDao.getAllCategories() }
    val unitConversions: Flow<List<UnitConversionEntity>> by lazy { productDao.getAllConversions() }
    val allTierPrices: Flow<List<ProductTierPriceEntity>> by lazy { productDao.getAllTierPrices() }
    val aliases: Flow<List<ProductAliasEntity>> by lazy { productDao.getAllAliases() }

    val allSales: Flow<List<SaleEntity>> by lazy { saleDao.getAllSales() }
    val allSaleItems: Flow<List<SaleItemEntity>> by lazy { saleDao.getAllSaleItems() }
    val allPurchases: Flow<List<PurchaseEntity>> by lazy { purchaseDao.getAllPurchases() }
    val stockMovements: Flow<List<StockMovementEntity>> by lazy { stockMovementDao.getAllMovements() }

    val expenses: Flow<List<ExpenseEntity>> by lazy { financeDao.getAllExpenses() }
    val incomes: Flow<List<IncomeEntity>> by lazy { financeDao.getAllIncomes() }
    val debtPayments: Flow<List<DebtPaymentEntity>> by lazy { financeDao.getAllDebtPayments() }

    val customers: Flow<List<CustomerEntity>> by lazy { contactDao.getAllCustomers() }
    val suppliers: Flow<List<SupplierEntity>> by lazy { contactDao.getAllSuppliers() }

    val aiCommandLogs: Flow<List<AICommandLogEntity>> by lazy { aiDao.getAllLogs() }
    val pendingAIDrafts: Flow<List<AICommandLogEntity>> by lazy { aiDao.getPendingDrafts() }
    val appSettings: Flow<List<AppSettingsEntity>> by lazy { settingsDao.getAllSettings() }

    // Multi Kas & Konter Flows
    val allCashAccounts: Flow<List<CashAccountEntity>> by lazy { cashAccountDao.getAllCashAccounts() }
    val activeCashAccounts: Flow<List<CashAccountEntity>> by lazy { cashAccountDao.getActiveCashAccounts() }
    val allCashTransfers: Flow<List<CashTransferEntity>> by lazy { cashAccountDao.getAllTransfers() }

    // Admin Tier Services & Ranges
    val allTierServices: Flow<List<AdminTierServiceEntity>> by lazy { adminTierDao.getAllServices() }
    val activeTierServices: Flow<List<AdminTierServiceEntity>> by lazy { adminTierDao.getActiveServices() }
    val allTierRanges: Flow<List<AdminTierRangeEntity>> by lazy { adminTierDao.getAllRanges() }

    // Shifts & Pending Sales
    val allShifts: Flow<List<CashierShiftEntity>> by lazy { shiftDao.getAllShifts() }
    val currentOpenShift: Flow<CashierShiftEntity?> by lazy { shiftDao.getCurrentOpenShift() }
    val pendingSales: Flow<List<PendingSaleEntity>> by lazy { saleDao.getAllPendingSales() }

    // Schema v3 Flows
    val storeProfile: Flow<StoreProfileEntity?> by lazy { extDao.getStoreProfile() }
    val userAccounts: Flow<List<UserAccountEntity>> by lazy { extDao.getAllUsers() }
    val menuPermissions: Flow<List<MenuPermissionEntity>> by lazy { extDao.getAllMenuPermissions() }
    val printers: Flow<List<PrinterEntity>> by lazy { extDao.getAllPrinters() }
    val cashierDevices: Flow<List<CashierDeviceEntity>> by lazy { extDao.getAllCashierDevices() }
    val productVariants: Flow<List<ProductVariantEntity>> by lazy { extDao.getAllVariants() }
    val wholesaleTiers: Flow<List<WholesaleTierEntity>> by lazy { extDao.getAllWholesaleTiers() }
    val productBatches: Flow<List<ProductBatchEntity>> by lazy { extDao.getAllBatches() }
    val bundles: Flow<List<BundlePackageEntity>> by lazy { extDao.getAllBundles() }
    val bundleItems: Flow<List<BundleItemEntity>> by lazy { extDao.getAllBundleItems() }
    val promos: Flow<List<PromoEntity>> by lazy { extDao.getAllPromos() }
    val cashMutations: Flow<List<CashMutationEntity>> by lazy { extDao.getAllCashMutations() }
    val shiftCashMovements: Flow<List<ShiftCashMovementEntity>> by lazy { extDao.getAllShiftCashMovements() }
    val recentBarcodeScans: Flow<List<BarcodeScanLogEntity>> by lazy { productDao.getRecentBarcodeScans() }

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String): ProductEntity? = productDao.getActiveProductByBarcode(barcode) ?: productDao.getProductByBarcode(barcode)
    fun observeProductByBarcode(barcode: String): Flow<ProductEntity?> = productDao.observeProductByBarcode(barcode)
    suspend fun getVariantByBarcode(barcode: String): ProductVariantEntity? = extDao.getActiveVariantByBarcode(barcode)
    suspend fun recordBarcodeScan(log: BarcodeScanLogEntity): Long = productDao.recordBarcodeScan(log)
    suspend fun clearBarcodeScanLogs() = productDao.clearBarcodeScanLogs()
    suspend fun searchProducts(query: String): List<ProductEntity> = productDao.searchProducts(query)

    suspend fun insertProduct(product: ProductEntity): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    suspend fun insertCategory(name: String, icon: String = "category") =
        productDao.insertCategory(CategoryEntity(name = name, icon = icon))

    suspend fun insertConversion(conversion: UnitConversionEntity) =
        productDao.insertConversion(conversion)

    suspend fun deleteConversion(conversion: UnitConversionEntity) =
        productDao.deleteConversion(conversion)

    suspend fun replaceUnitConversions(productId: Long, conversions: List<UnitConversionEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            productDao.deleteConversionsForProduct(productId)
            conversions.forEach { productDao.insertConversion(it.copy(id = 0L, productId = productId)) }
        }
    }

    suspend fun getConversionsList(productId: Long): List<UnitConversionEntity> = productDao.getConversionsList(productId)
    suspend fun getVariantsForProductList(productId: Long): List<ProductVariantEntity> = extDao.getVariantsForProduct(productId)
    suspend fun getWholesaleTiersList(productId: Long): List<WholesaleTierEntity> = extDao.getWholesaleTiersForProduct(productId)
    suspend fun getAliasesForProductList(productId: Long): List<ProductAliasEntity> = productDao.getAliasesForProduct(productId)

    suspend fun deleteProductCascade(product: ProductEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            productDao.deleteConversionsForProduct(product.id)
            productDao.deleteTierPricesForProduct(product.id)
            productDao.deleteAliasesForProduct(product.id)
            extDao.deleteVariantsForProduct(product.id)
            extDao.deleteWholesaleTiersForProduct(product.id)
            productDao.deleteProduct(product)
        }
    }

    suspend fun learnProductAlias(productId: Long, alias: String) {
        val cleanAlias = alias.trim().lowercase()
        if (cleanAlias.isNotBlank()) {
            val existing = productDao.getAliasesForProduct(productId)
            if (existing.none { it.alias.equals(cleanAlias, ignoreCase = true) }) {
                productDao.insertAlias(ProductAliasEntity(productId = productId, alias = cleanAlias))
            }
        }
    }

    // Sequential document counter
    suspend fun nextDocumentNumber(counterName: String, prefix: String): String = withContext(Dispatchers.IO) {
        val existing = extDao.getCounter(counterName)
        val nextVal = (existing?.value ?: 0L) + 1L
        extDao.upsertCounter(DocumentCounterEntity(counterName = counterName, value = nextVal))
        val seq = nextVal.toString().padStart(4, '0')
        "$prefix-$seq"
    }

    // POS Sale Checkout (supports Variants, Bundles, Negative Stock, Service Direction SETOR/TARIK, Deposit, Points, Promo, Ongkir, Pre-Order, Mutasi Kas)
    suspend fun completeSale(
        cashierName: String,
        customer: CustomerEntity?,
        items: List<CartItem>,
        paymentMethod: String, // TUNAI, QRIS, TRANSFER, KREDIT, SPLIT, DEPOSIT
        amountPaid: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        notes: String = "",
        storeAdminFee: Double = 0.0,
        providerAdminFee: Double = 0.0,
        agentCommission: Double = 0.0,
        cashAccountId: Long? = null,
        splitCashAmount: Double = 0.0,
        splitNonCashAmount: Double = 0.0,
        splitPaymentMethod: String = "",
        shippingCost: Double = 0.0,
        additionalFee: Double = 0.0,
        promoDiscount: Double = 0.0,
        appliedPromoName: String = "",
        depositUsed: Double = 0.0,
        isPreOrder: Boolean = false
    ): SaleEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
        val now = System.currentTimeMillis()
        val invoiceNum = nextDocumentNumber("transaksi", if (isPreOrder) "PO-TRX" else "TRX")
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val totalItemStoreAdmin = items.sumOf { it.storeAdminFee }
        val totalItemProviderAdmin = items.sumOf { it.providerAdminFee }
        val totalItemAgentFee = items.sumOf { it.agentCommission }

        val effectiveStoreAdmin = storeAdminFee + totalItemStoreAdmin
        val effectiveProviderAdmin = providerAdminFee + totalItemProviderAdmin
        val effectiveAgentFee = agentCommission + totalItemAgentFee

        val finalAmount = (subtotal - discount - promoDiscount + tax + shippingCost + additionalFee + effectiveStoreAdmin).coerceAtLeast(0.0)
        val effectiveDeposit = depositUsed.coerceAtMost(customer?.depositBalance ?: 0.0).coerceAtMost(finalAmount)
        val remainingPayable = (finalAmount - effectiveDeposit).coerceAtLeast(0.0)
        val changeAmount = when (paymentMethod) {
            "TUNAI" -> (amountPaid - remainingPayable).coerceAtLeast(0.0)
            "SPLIT" -> (splitCashAmount + splitNonCashAmount - remainingPayable).coerceAtLeast(0.0)
            else -> 0.0
        }
        val recordedAmountPaid = when (paymentMethod) {
            "KREDIT" -> effectiveDeposit
            "SPLIT" -> splitCashAmount + splitNonCashAmount + effectiveDeposit
            else -> amountPaid + effectiveDeposit
        }
        val pointsEarned = if (!isPreOrder && customer != null) (finalAmount / 10000.0).toInt() else 0

        val saleStatus = if (isPreOrder) "PRE_ORDER" else "COMPLETED"

        val sale = SaleEntity(
            invoiceNumber = invoiceNum,
            date = now,
            cashierName = cashierName,
            customerId = customer?.id,
            customerName = customer?.name ?: "Pelanggan Umum",
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            finalAmount = finalAmount,
            paymentMethod = paymentMethod,
            amountPaid = recordedAmountPaid,
            changeAmount = changeAmount,
            status = saleStatus,
            notes = notes,
            storeAdminFee = effectiveStoreAdmin,
            providerAdminFee = effectiveProviderAdmin,
            agentCommission = effectiveAgentFee,
            cashAccountId = cashAccountId,
            splitCashAmount = splitCashAmount,
            splitNonCashAmount = splitNonCashAmount,
            splitPaymentMethod = splitPaymentMethod,
            shippingCost = shippingCost,
            additionalFee = additionalFee,
            promoDiscount = promoDiscount,
            appliedPromoName = appliedPromoName,
            depositUsed = effectiveDeposit,
            pointsEarned = pointsEarned
        )
        val saleId = saleDao.insertSale(sale)

        // Insert items & deduct stock (only deduct immediately when COMPLETED; or also for pre-order if desired — let's deduct on COMPLETED or reserve cleanly)
        val saleItemEntities = items.map { cartItem ->
            if (cartItem.bundleId != null) {
                val bItems = extDao.getItemsForBundle(cartItem.bundleId)
                if (!isPreOrder) {
                    bItems.forEach { bItem ->
                        val p = productDao.getProductById(bItem.productId)
                        if (p != null && !p.hasNoStock && !p.isUnlimitedStock) {
                            val deduct = bItem.qty * cartItem.quantity
                            val oldStock = p.stock
                            val newStock = oldStock - deduct // negative stock allowed per schema.sql
                            productDao.updateStock(p.id, newStock)
                            stockMovementDao.insertMovement(
                                StockMovementEntity(
                                    productId = p.id,
                                    productName = p.name,
                                    date = now,
                                    movementType = "PENJUALAN_KELUAR",
                                    quantityChange = -deduct,
                                    unit = p.baseUnit,
                                    previousStock = oldStock,
                                    currentStock = newStock,
                                    referenceNumber = invoiceNum,
                                    notes = "Penjualan Paket ${cartItem.productName}",
                                    loggedBy = cashierName
                                )
                            )
                        }
                    }
                }
            } else {
                val product = productDao.getProductById(cartItem.productId)
                val baseUnitsToDeduct = cartItem.quantity * cartItem.multiplier
                if (!isPreOrder && product != null && !product.hasNoStock && !product.isUnlimitedStock) {
                    if (cartItem.variantId != null) {
                        val variant = extDao.getVariantById(cartItem.variantId)
                        if (variant != null) {
                            extDao.updateVariantStock(variant.id, variant.stock - baseUnitsToDeduct)
                        }
                    }
                    val oldStock = product.stock
                    val newStock = oldStock - baseUnitsToDeduct // Negative stock allowed per schema.sql
                    productDao.updateStock(product.id, newStock)

                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = if (cartItem.variantName.isNotBlank()) "${product.name} (${cartItem.variantName})" else product.name,
                            date = now,
                            movementType = "PENJUALAN_KELUAR",
                            quantityChange = -baseUnitsToDeduct,
                            unit = product.baseUnit,
                            previousStock = oldStock,
                            currentStock = newStock,
                            referenceNumber = invoiceNum,
                            notes = "Penjualan Kasir (${cartItem.quantity} ${cartItem.unitName})",
                            loggedBy = cashierName
                        )
                    )
                }

                // Digital service Multi Kas account balance adjustments (SETOR vs TARIK)
                val svcAccountId = cartItem.serviceCashAccountId ?: product?.serviceCashAccountId ?: cashAccountId
                val svcDir = cartItem.serviceDirection.ifBlank { product?.serviceDirection ?: "SETOR" }
                val nominal = cartItem.serviceNominal ?: if (product?.productType == "DIGITAL") cartItem.unitPrice * cartItem.quantity else 0.0
                if (!isPreOrder && svcAccountId != null && (nominal > 0 || cartItem.providerAdminFee > 0 || cartItem.agentCommission > 0) && product?.productType == "DIGITAL") {
                    val acc = cashAccountDao.getCashAccountById(svcAccountId)
                    if (acc != null) {
                        var delta = 0.0
                        if (svcDir.equals("TARIK", ignoreCase = true)) {
                            delta += nominal
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = acc.id,
                                    accountName = acc.accountName,
                                    amount = nominal,
                                    mutationType = "layanan",
                                    refType = "transaksi",
                                    refId = invoiceNum,
                                    notes = "Tarik Tunai ${cartItem.productName}",
                                    timestamp = now
                                )
                            )
                        } else {
                            delta -= nominal
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = acc.id,
                                    accountName = acc.accountName,
                                    amount = -nominal,
                                    mutationType = "layanan",
                                    refType = "transaksi",
                                    refId = invoiceNum,
                                    notes = "Layanan Setor ${cartItem.productName}",
                                    timestamp = now
                                )
                            )
                        }
                        if (cartItem.providerAdminFee > 0) {
                            delta -= cartItem.providerAdminFee
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = acc.id,
                                    accountName = acc.accountName,
                                    amount = -cartItem.providerAdminFee,
                                    mutationType = "admin_penyedia",
                                    refType = "transaksi",
                                    refId = invoiceNum,
                                    notes = "Admin penyedia ${cartItem.productName}",
                                    timestamp = now
                                )
                            )
                        }
                        if (cartItem.agentCommission > 0) {
                            delta += cartItem.agentCommission
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = acc.id,
                                    accountName = acc.accountName,
                                    amount = cartItem.agentCommission,
                                    mutationType = "fee",
                                    refType = "transaksi",
                                    refId = invoiceNum,
                                    notes = "Komisi/Fee agen ${cartItem.productName}",
                                    timestamp = now
                                )
                            )
                        }
                        cashAccountDao.updateBalance(acc.id, acc.balance + delta)
                    }
                }
            }

            val product = productDao.getProductById(cartItem.productId)
            SaleItemEntity(
                saleId = saleId,
                productId = cartItem.productId,
                productName = if (cartItem.variantName.isNotBlank()) "${cartItem.productName} (${cartItem.variantName})" else cartItem.productName,
                quantity = cartItem.quantity,
                unit = cartItem.unitName,
                unitPrice = cartItem.unitPrice,
                subtotal = cartItem.quantity * cartItem.unitPrice,
                purchasePriceSnapshot = (product?.purchasePrice ?: 0.0) * cartItem.multiplier,
                variantId = cartItem.variantId,
                variantName = cartItem.variantName,
                priceSource = cartItem.priceSource,
                serviceNominal = cartItem.serviceNominal,
                storeAdminFee = cartItem.storeAdminFee,
                providerAdminFee = cartItem.providerAdminFee,
                agentCommission = cartItem.agentCommission
            )
        }
        saleDao.insertSaleItems(saleItemEntities)

        // Update customer deposit, loyalty points, and receivable
        if (customer != null) {
            val newDeposit = (customer.depositBalance - effectiveDeposit).coerceAtLeast(0.0)
            val newPoints = customer.loyaltyPoints + pointsEarned
            val addedReceivable = if (paymentMethod == "KREDIT") remainingPayable else 0.0
            contactDao.updateCustomer(
                customer.copy(
                    depositBalance = newDeposit,
                    loyaltyPoints = newPoints,
                    totalReceivable = customer.totalReceivable + addedReceivable
                )
            )
        }

        // Record sale inflows into Kas accounts & mutasi_kas
        if (!isPreOrder) {
            val allAcc = cashAccountDao.getAllAccountsList()
            val defaultCashAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
            val cashPaidIn = when (paymentMethod) {
                "TUNAI" -> remainingPayable
                "SPLIT" -> splitCashAmount
                else -> 0.0
            }
            if (cashPaidIn > 0) {
                val selectedCashAcc = cashAccountId?.let { id -> cashAccountDao.getCashAccountById(id) }?.takeIf { it.accountType == "CASH" }
                val targetCashAcc = selectedCashAcc ?: defaultCashAcc
                if (targetCashAcc != null) {
                    cashAccountDao.updateBalance(targetCashAcc.id, targetCashAcc.balance + cashPaidIn)
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = targetCashAcc.id,
                            accountName = targetCashAcc.accountName,
                            amount = cashPaidIn,
                            mutationType = "penjualan",
                            refType = "transaksi",
                            refId = invoiceNum,
                            notes = "Penjualan tunai $invoiceNum",
                            timestamp = now
                        )
                    )
                }
            }

            val nonCashPaidIn = when (paymentMethod) {
                "QRIS", "TRANSFER" -> remainingPayable
                "SPLIT" -> splitNonCashAmount
                else -> 0.0
            }
            if (nonCashPaidIn > 0 && cashAccountId != null) {
                val nonCashAcc = cashAccountDao.getCashAccountById(cashAccountId)
                if (nonCashAcc != null) {
                    cashAccountDao.updateBalance(nonCashAcc.id, nonCashAcc.balance + nonCashPaidIn)
                    val methodDesc = if (paymentMethod == "SPLIT") splitPaymentMethod.ifBlank { "Non-Tunai" } else paymentMethod
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = nonCashAcc.id,
                            accountName = nonCashAcc.accountName,
                            amount = nonCashPaidIn,
                            mutationType = "penjualan",
                            refType = "transaksi",
                            refId = invoiceNum,
                            notes = "Penjualan $methodDesc $invoiceNum",
                            timestamp = now
                        )
                    )
                }
            }
        }

        // Handle sale-level provider fee / agent commission if passed explicitly via payment dialog
        if (!isPreOrder && (providerAdminFee > 0 || agentCommission > 0)) {
            val allAcc = cashAccountDao.getAllAccountsList()
            val defaultCashAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
            val acc = (cashAccountId?.let { id -> cashAccountDao.getCashAccountById(id) }) ?: defaultCashAcc
            if (acc != null) {
                cashAccountDao.updateBalance(acc.id, acc.balance - providerAdminFee + agentCommission)
                if (providerAdminFee > 0) {
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = acc.id,
                            accountName = acc.accountName,
                            amount = -providerAdminFee,
                            mutationType = "admin_penyedia",
                            refType = "transaksi",
                            refId = invoiceNum,
                            notes = "Admin penyedia $invoiceNum",
                            timestamp = now
                        )
                    )
                }
                if (agentCommission > 0) {
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = acc.id,
                            accountName = acc.accountName,
                            amount = agentCommission,
                            mutationType = "fee",
                            refType = "transaksi",
                            refId = invoiceNum,
                            notes = "Fee agen $invoiceNum",
                            timestamp = now
                        )
                    )
                }
            }
        }

        // Update active shift
        if (!isPreOrder) {
            val openShift = shiftDao.getOpenShiftSync()
            if (openShift != null) {
                val cashPaidIn = when (paymentMethod) {
                    "TUNAI" -> remainingPayable
                    "SPLIT" -> splitCashAmount
                    else -> 0.0
                }
                val newCashSales = openShift.totalCashSales + cashPaidIn
                val newExpected = openShift.startingCash + openShift.cashIn - openShift.cashOut + newCashSales
                shiftDao.updateShift(
                    openShift.copy(
                        totalCashSales = newCashSales,
                        expectedClosingCash = newExpected
                    )
                )
            }
        }

        sale.copy(id = saleId)
        }
    }

    suspend fun completePreOrder(saleId: Long, user: String = "Kasir 1") = withContext(Dispatchers.IO) {
        db.withTransaction {
            val sale = saleDao.getSaleById(saleId) ?: return@withTransaction
            if (sale.status != "PRE_ORDER") return@withTransaction
            val items = saleDao.getItemsForSale(saleId)
            val now = System.currentTimeMillis()
            val earnedPoints = if (sale.customerId != null) (sale.finalAmount / 10000.0).toInt() else 0
            val updatedPaid = if (sale.paymentMethod == "KREDIT") sale.depositUsed else sale.finalAmount
            saleDao.updateSale(
                sale.copy(
                    status = "COMPLETED",
                    amountPaid = updatedPaid,
                    pointsEarned = earnedPoints
                )
            )
            for (item in items) {
                if (item.productId < 0L) {
                    val bItems = extDao.getItemsForBundle(-item.productId)
                    for (bItem in bItems) {
                        val bp = productDao.getProductById(bItem.productId)
                        if (bp != null && !bp.hasNoStock && !bp.isUnlimitedStock) {
                            val deduct = bItem.qty * item.quantity
                            val oldStock = bp.stock
                            val newStock = oldStock - deduct
                            productDao.updateStock(bp.id, newStock)
                            stockMovementDao.insertMovement(
                                StockMovementEntity(
                                    productId = bp.id,
                                    productName = bp.name,
                                    date = now,
                                    movementType = "PENJUALAN_KELUAR",
                                    quantityChange = -deduct,
                                    unit = bp.baseUnit,
                                    previousStock = oldStock,
                                    currentStock = newStock,
                                    referenceNumber = sale.invoiceNumber,
                                    notes = "Penyelesaian Pre-Order Paket ${item.productName}",
                                    loggedBy = user
                                )
                            )
                        }
                    }
                    continue
                }
                val p = productDao.getProductById(item.productId)
                if (p != null && !p.hasNoStock && !p.isUnlimitedStock) {
                    val conversions = productDao.getConversionsList(p.id)
                    val mul = if (item.unit.equals(p.baseUnit, ignoreCase = true)) {
                        1.0
                    } else {
                        conversions.find { it.unitName.equals(item.unit, ignoreCase = true) }?.multiplier ?: 1.0
                    }
                    val deductBase = item.quantity * mul
                    if (item.variantId != null) {
                        val variant = extDao.getVariantById(item.variantId)
                        if (variant != null) {
                            extDao.updateVariantStock(variant.id, variant.stock - deductBase)
                        }
                    }
                    val oldStock = p.stock
                    val newStock = oldStock - deductBase
                    productDao.updateStock(p.id, newStock)
                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = p.id,
                            productName = item.productName,
                            date = now,
                            movementType = "PENJUALAN_KELUAR",
                            quantityChange = -deductBase,
                            unit = p.baseUnit,
                            previousStock = oldStock,
                            currentStock = newStock,
                            referenceNumber = sale.invoiceNumber,
                            notes = "Penyelesaian Pre-Order (${item.quantity} ${item.unit})",
                            loggedBy = user
                        )
                    )
                }
            }
            if (sale.customerId != null && earnedPoints > 0) {
                val cust = contactDao.getAllCustomers().first().find { it.id == sale.customerId }
                if (cust != null) {
                    contactDao.updateCustomer(cust.copy(loyaltyPoints = cust.loyaltyPoints + earnedPoints))
                }
            }
            val remainingPayable = (sale.finalAmount - sale.depositUsed).coerceAtLeast(0.0)
            val allAcc = cashAccountDao.getAllAccountsList()
            val defaultCashAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
            val cashPaidIn = when (sale.paymentMethod) {
                "TUNAI" -> remainingPayable
                "SPLIT" -> sale.splitCashAmount
                else -> 0.0
            }
            if (cashPaidIn > 0) {
                val selectedCashAcc = sale.cashAccountId?.let { id -> cashAccountDao.getCashAccountById(id) }?.takeIf { it.accountType == "CASH" }
                val targetCashAcc = selectedCashAcc ?: defaultCashAcc
                if (targetCashAcc != null) {
                    cashAccountDao.updateBalance(targetCashAcc.id, targetCashAcc.balance + cashPaidIn)
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = targetCashAcc.id,
                            accountName = targetCashAcc.accountName,
                            amount = cashPaidIn,
                            mutationType = "penjualan",
                            refType = "transaksi",
                            refId = sale.invoiceNumber,
                            notes = "Pelunasan Pre-Order ${sale.invoiceNumber}",
                            timestamp = now
                        )
                    )
                }
            }
            val nonCashPaidIn = when (sale.paymentMethod) {
                "QRIS", "TRANSFER" -> remainingPayable
                "SPLIT" -> sale.splitNonCashAmount
                else -> 0.0
            }
            if (nonCashPaidIn > 0 && sale.cashAccountId != null) {
                val nonCashAcc = cashAccountDao.getCashAccountById(sale.cashAccountId)
                if (nonCashAcc != null) {
                    cashAccountDao.updateBalance(nonCashAcc.id, nonCashAcc.balance + nonCashPaidIn)
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = nonCashAcc.id,
                            accountName = nonCashAcc.accountName,
                            amount = nonCashPaidIn,
                            mutationType = "penjualan",
                            refType = "transaksi",
                            refId = sale.invoiceNumber,
                            notes = "Pelunasan Non-Tunai Pre-Order ${sale.invoiceNumber}",
                            timestamp = now
                        )
                    )
                }
            }
            if (cashPaidIn > 0) {
                val openShift = shiftDao.getOpenShiftSync()
                if (openShift != null) {
                    val newCashSales = openShift.totalCashSales + cashPaidIn
                    val newExpected = openShift.startingCash + openShift.cashIn - openShift.cashOut + newCashSales
                    shiftDao.updateShift(openShift.copy(totalCashSales = newCashSales, expectedClosingCash = newExpected))
                }
            }
        }
    }

    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        saleDao.getItemsForSale(saleId)
    }

    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseItemEntity> = withContext(Dispatchers.IO) {
        purchaseDao.getItemsForPurchase(purchaseId)
    }

    suspend fun refundSale(saleId: Long, reason: String = "Retur Barang Pelanggan", user: String = "Kasir 1") = withContext(Dispatchers.IO) {
        db.withTransaction {
            val sale = saleDao.getSaleById(saleId) ?: return@withTransaction
            if (sale.status == "REFUNDED" || sale.status == "CANCELLED") return@withTransaction

            val wasStockDeducted = sale.status == "COMPLETED"
            val items = saleDao.getItemsForSale(saleId)
            val now = System.currentTimeMillis()

            saleDao.updateSale(sale.copy(status = "REFUNDED", notes = "${sale.notes} [RETUR: $reason]".trim()))

            if (wasStockDeducted) {
                for (item in items) {
                    if (item.productId < 0L) {
                        val bItems = extDao.getItemsForBundle(-item.productId)
                        for (bItem in bItems) {
                            val bp = productDao.getProductById(bItem.productId)
                            if (bp != null && !bp.hasNoStock && !bp.isUnlimitedStock) {
                                val restored = bItem.qty * item.quantity
                                val oldStock = bp.stock
                                val newStock = oldStock + restored
                                productDao.updateStock(bp.id, newStock)
                                stockMovementDao.insertMovement(
                                    StockMovementEntity(
                                        productId = bp.id,
                                        productName = bp.name,
                                        date = now,
                                        movementType = "RETUR",
                                        quantityChange = restored,
                                        unit = bp.baseUnit,
                                        previousStock = oldStock,
                                        currentStock = newStock,
                                        referenceNumber = sale.invoiceNumber,
                                        notes = "Retur Paket ${item.productName}: $reason",
                                        loggedBy = user
                                    )
                                )
                            }
                        }
                        continue
                    }

                    val product = productDao.getProductById(item.productId)
                    if (product != null && !product.hasNoStock && !product.isUnlimitedStock) {
                        val conversions = productDao.getConversionsList(product.id)
                        val mul = if (item.unit.equals(product.baseUnit, ignoreCase = true)) {
                            1.0
                        } else {
                            conversions.find { it.unitName.equals(item.unit, ignoreCase = true) }?.multiplier ?: 1.0
                        }
                        val restoredBase = item.quantity * mul
                        val oldStock = product.stock
                        val newStock = oldStock + restoredBase
                        productDao.updateStock(product.id, newStock)

                        if (item.variantId != null) {
                            val v = extDao.getVariantById(item.variantId)
                            if (v != null) {
                                extDao.updateVariantStock(v.id, v.stock + restoredBase)
                            }
                        }

                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = product.id,
                                productName = product.name,
                                date = now,
                                movementType = "RETUR",
                                quantityChange = restoredBase,
                                unit = product.baseUnit,
                                previousStock = oldStock,
                                currentStock = newStock,
                                referenceNumber = sale.invoiceNumber,
                                notes = "Retur Penjualan (${item.quantity} ${item.unit}): $reason",
                                loggedBy = user
                            )
                        )
                    }
                }
            }

            // Reverse customer receivable, deposit, and loyalty points
            if (sale.customerId != null) {
                val customer = contactDao.getAllCustomers().first().find { it.id == sale.customerId }
                if (customer != null) {
                    val remainingPayable = (sale.finalAmount - sale.depositUsed).coerceAtLeast(0.0)
                    val reversedReceivable = if (sale.paymentMethod == "KREDIT") {
                        (customer.totalReceivable - remainingPayable).coerceAtLeast(0.0)
                    } else {
                        customer.totalReceivable
                    }
                    contactDao.updateCustomer(
                        customer.copy(
                            depositBalance = customer.depositBalance + sale.depositUsed,
                            loyaltyPoints = (customer.loyaltyPoints - sale.pointsEarned).coerceAtLeast(0),
                            totalReceivable = reversedReceivable
                        )
                    )
                }
            }

            // Deduct cash/non-cash back from accounts & update open shift if completed
            if (wasStockDeducted) {
                val allAcc = cashAccountDao.getAllAccountsList()
                val defaultCashAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
                val netPaidOut = (sale.finalAmount - sale.depositUsed).coerceAtLeast(0.0)
                val cashRefund = when (sale.paymentMethod) {
                    "TUNAI" -> netPaidOut
                    "SPLIT" -> sale.splitCashAmount
                    else -> 0.0
                }
                if (cashRefund > 0) {
                    val selectedCashAcc = sale.cashAccountId?.let { id -> cashAccountDao.getCashAccountById(id) }?.takeIf { it.accountType == "CASH" }
                    val targetCashAcc = selectedCashAcc ?: defaultCashAcc
                    if (targetCashAcc != null) {
                        cashAccountDao.updateBalance(targetCashAcc.id, targetCashAcc.balance - cashRefund)
                        extDao.insertCashMutation(
                            CashMutationEntity(
                                cashAccountId = targetCashAcc.id,
                                accountName = targetCashAcc.accountName,
                                amount = -cashRefund,
                                mutationType = "retur",
                                refType = "transaksi",
                                refId = sale.invoiceNumber,
                                notes = "Retur tunai ${sale.invoiceNumber}: $reason",
                                timestamp = now
                            )
                        )
                    }
                    val openShift = shiftDao.getOpenShiftSync()
                    if (openShift != null) {
                        val newCashSales = (openShift.totalCashSales - cashRefund).coerceAtLeast(0.0)
                        val newExpected = openShift.startingCash + openShift.cashIn - openShift.cashOut + newCashSales
                        shiftDao.updateShift(openShift.copy(totalCashSales = newCashSales, expectedClosingCash = newExpected))
                    }
                }

                val nonCashRefund = when (sale.paymentMethod) {
                    "QRIS", "TRANSFER" -> netPaidOut
                    "SPLIT" -> sale.splitNonCashAmount
                    else -> 0.0
                }
                if (nonCashRefund > 0 && sale.cashAccountId != null) {
                    val nonCashAcc = cashAccountDao.getCashAccountById(sale.cashAccountId)
                    if (nonCashAcc != null) {
                        cashAccountDao.updateBalance(nonCashAcc.id, nonCashAcc.balance - nonCashRefund)
                        extDao.insertCashMutation(
                            CashMutationEntity(
                                cashAccountId = nonCashAcc.id,
                                accountName = nonCashAcc.accountName,
                                amount = -nonCashRefund,
                                mutationType = "retur",
                                refType = "transaksi",
                                refId = sale.invoiceNumber,
                                notes = "Retur non-tunai ${sale.invoiceNumber}: $reason",
                                timestamp = now
                            )
                        )
                    }
                }

                // Reverse digital service cash mutations & balances if any
                for (item in items) {
                    if (item.productId < 0L) continue
                    val product = productDao.getProductById(item.productId)
                    val isDigital = product?.productType == "DIGITAL" || (item.serviceNominal != null && item.serviceNominal > 0.0)
                    if (!isDigital) continue
                    val svcAccountId = product?.serviceCashAccountId ?: sale.cashAccountId ?: continue
                    val svcAcc = cashAccountDao.getCashAccountById(svcAccountId) ?: continue
                    val svcDir = product?.serviceDirection ?: "SETOR"
                    val nominal = item.serviceNominal ?: 0.0
                    val reverseNominalDelta = if (svcDir.equals("TARIK", ignoreCase = true)) -nominal else nominal
                    val totalDelta = reverseNominalDelta + item.providerAdminFee - item.agentCommission
                    if (totalDelta != 0.0) {
                        cashAccountDao.updateBalance(svcAcc.id, svcAcc.balance + totalDelta)
                        extDao.insertCashMutation(
                            CashMutationEntity(
                                cashAccountId = svcAcc.id,
                                accountName = svcAcc.accountName,
                                amount = totalDelta,
                                mutationType = "retur",
                                refType = "transaksi",
                                refId = sale.invoiceNumber,
                                notes = "Retur layanan ${item.productName}: $reason",
                                timestamp = now
                            )
                        )
                    }
                }

                // Reverse any remaining sale-level provider fee / agent commission
                val itemProvSum = items.sumOf { it.providerAdminFee }
                val itemAgentSum = items.sumOf { it.agentCommission }
                val remProv = (sale.providerAdminFee - itemProvSum).coerceAtLeast(0.0)
                val remAgent = (sale.agentCommission - itemAgentSum).coerceAtLeast(0.0)
                if (remProv > 0.0 || remAgent > 0.0) {
                    val feeAcc = (sale.cashAccountId?.let { id -> cashAccountDao.getCashAccountById(id) }) ?: defaultCashAcc
                    if (feeAcc != null) {
                        val netReverse = remProv - remAgent
                        if (netReverse != 0.0) {
                            cashAccountDao.updateBalance(feeAcc.id, feeAcc.balance + netReverse)
                            extDao.insertCashMutation(
                                CashMutationEntity(
                                    cashAccountId = feeAcc.id,
                                    accountName = feeAcc.accountName,
                                    amount = netReverse,
                                    mutationType = "retur",
                                    refType = "transaksi",
                                    refId = sale.invoiceNumber,
                                    notes = "Retur admin/komisi ${sale.invoiceNumber}",
                                    timestamp = now
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Purchase Order Draft
    suspend fun createPurchase(
        invoiceNumber: String,
        supplier: SupplierEntity?,
        items: List<PurchaseItemDraft>,
        paymentMethod: String, // TUNAI, KREDIT
        discount: Double = 0.0,
        additionalCost: Double = 0.0,
        notes: String = "",
        initialStatus: String = "DRAFT"
    ): PurchaseEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val invoice = if (invoiceNumber.isNotBlank()) invoiceNumber else nextDocumentNumber("pesanan_beli", "PO")
            val subtotal = items.sumOf { it.purchasePrice * it.quantity }
            val finalAmount = (subtotal - discount + additionalCost).coerceAtLeast(0.0)

            val purchase = PurchaseEntity(
                invoiceNumber = invoice,
                date = now,
                supplierId = supplier?.id,
                supplierName = supplier?.name ?: "Pemasok Umum",
                subtotal = subtotal,
                discount = discount,
                additionalCost = additionalCost,
                finalAmount = finalAmount,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == "TUNAI") "LUNAS" else "BELUM_LUNAS",
                status = initialStatus,
                notes = notes
            )
            val purchaseId = purchaseDao.insertPurchase(purchase)

            val purchaseItems = items.map { item ->
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    unit = item.unit,
                    purchasePrice = item.purchasePrice,
                    subtotal = item.quantity * item.purchasePrice
                )
            }
            purchaseDao.insertPurchaseItems(purchaseItems)

            if (initialStatus == "DITERIMA") {
                processGoodsReceipt(purchase.copy(id = purchaseId), purchaseItems)
            }

            purchase.copy(id = purchaseId)
        }
    }

    suspend fun confirmPurchaseReceipt(purchaseId: Long, user: String = "Petugas Stok") = withContext(Dispatchers.IO) {
        db.withTransaction {
            val purchase = purchaseDao.getPurchaseById(purchaseId) ?: return@withTransaction
            if (purchase.status == "DITERIMA") return@withTransaction

            val items = purchaseDao.getItemsForPurchase(purchaseId)
            val now = System.currentTimeMillis()
            val updatedPurchase = purchase.copy(status = "DITERIMA", receivedDate = now)
            purchaseDao.updatePurchase(updatedPurchase)

            processGoodsReceipt(updatedPurchase, items, user)
        }
    }

    private suspend fun processGoodsReceipt(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        user: String = "Petugas Stok"
    ) {
        val now = System.currentTimeMillis()
        for (item in items) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val conversions = productDao.getConversionsForProduct(product.id).first()
                val conversion = conversions.find { it.unitName.equals(item.unit, ignoreCase = true) }
                val multiplier = conversion?.multiplier ?: 1.0
                val addedBaseQty = item.quantity * multiplier

                val oldStock = product.stock
                val newStock = oldStock + addedBaseQty
                productDao.updateStock(product.id, newStock)

                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        date = now,
                        movementType = "PEMBELIAN_MASUK",
                        quantityChange = addedBaseQty,
                        unit = product.baseUnit,
                        previousStock = oldStock,
                        currentStock = newStock,
                        referenceNumber = purchase.invoiceNumber,
                        notes = "Penerimaan Barang Pembelian (${item.quantity} ${item.unit})",
                        loggedBy = user
                    )
                )
            }
        }

        if (purchase.paymentMethod == "KREDIT" && purchase.supplierId != null) {
            val supplier = contactDao.getAllSuppliers().first().find { it.id == purchase.supplierId }
            if (supplier != null) {
                val newDebt = supplier.totalDebt + purchase.finalAmount
                contactDao.updateSupplier(supplier.copy(totalDebt = newDebt))
            }
        } else if (purchase.paymentMethod == "TUNAI" && purchase.finalAmount > 0) {
            val allAcc = cashAccountDao.getAllAccountsList()
            val targetCashAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
            if (targetCashAcc != null) {
                cashAccountDao.updateBalance(targetCashAcc.id, targetCashAcc.balance - purchase.finalAmount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = targetCashAcc.id,
                        accountName = targetCashAcc.accountName,
                        amount = -purchase.finalAmount,
                        mutationType = "pembelian",
                        refType = "pembelian",
                        refId = purchase.invoiceNumber,
                        notes = "Pembelian tunai ${purchase.invoiceNumber}",
                        timestamp = now
                    )
                )
            }
        }
    }

    suspend fun cancelPurchase(purchaseId: Long) = withContext(Dispatchers.IO) {
        val purchase = purchaseDao.getPurchaseById(purchaseId) ?: return@withContext
        purchaseDao.updatePurchase(purchase.copy(status = "DIBATALKAN"))
    }

    suspend fun adjustStock(
        productId: Long,
        newStock: Double,
        reason: String,
        user: String = "Petugas Stok"
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = productDao.getProductById(productId) ?: return@withTransaction
            val oldStock = product.stock
            val diff = newStock - oldStock
            val movementType = if (diff >= 0) "PENYESUAIAN_MASUK" else "PENYESUAIAN_KELUAR"

            productDao.updateStock(productId, newStock)

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    date = System.currentTimeMillis(),
                    movementType = movementType,
                    quantityChange = diff,
                    unit = product.baseUnit,
                    previousStock = oldStock,
                    currentStock = newStock,
                    referenceNumber = "ADJ-" + System.currentTimeMillis().toString().takeLast(6),
                    notes = reason,
                    loggedBy = user
                )
            )
        }
    }

    suspend fun quickStockIn(
        productId: Long,
        quantity: Double,
        unit: String,
        reason: String = "Stok Masuk Cepat",
        user: String = "Admin"
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = productDao.getProductById(productId) ?: return@withTransaction
            val conversions = productDao.getConversionsForProduct(product.id).first()
            val conversion = conversions.find { it.unitName.equals(unit, ignoreCase = true) }
            val multiplier = conversion?.multiplier ?: 1.0
            val baseQty = quantity * multiplier

            val oldStock = product.stock
            val newStock = oldStock + baseQty
            productDao.updateStock(product.id, newStock)

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    date = System.currentTimeMillis(),
                    movementType = "PENYESUAIAN_MASUK",
                    quantityChange = baseQty,
                    unit = product.baseUnit,
                    previousStock = oldStock,
                    currentStock = newStock,
                    referenceNumber = "STK-" + System.currentTimeMillis().toString().takeLast(6),
                    notes = "$reason ($quantity $unit)",
                    loggedBy = user
                )
            )
        }
    }

    // Expense & Income (with Multi Kas Ledger)
    suspend fun recordExpense(
        category: String,
        amount: Double,
        notes: String,
        source: String = "Kas Toko",
        user: String = "Admin",
        cashAccountId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val allAcc = cashAccountDao.getAllAccountsList()
            val acc = (cashAccountId?.let { id -> allAcc.find { it.id == id } }
                ?: allAcc.find { it.accountName.equals(source, ignoreCase = true) }
                ?: allAcc.find { it.isDefault }
                ?: allAcc.firstOrNull())
            val resolvedSource = acc?.accountName ?: source
            val id = financeDao.insertExpense(
                ExpenseEntity(
                    category = category,
                    amount = amount,
                    date = now,
                    notes = notes,
                    paymentSource = resolvedSource,
                    recordedBy = user
                )
            )
            if (acc != null && amount > 0) {
                cashAccountDao.updateBalance(acc.id, acc.balance - amount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = acc.id,
                        accountName = acc.accountName,
                        amount = -amount,
                        mutationType = "pengeluaran",
                        refType = "pengeluaran",
                        refId = id.toString(),
                        notes = "$category: $notes",
                        timestamp = now
                    )
                )
            }
            id
        }
    }

    suspend fun recordIncome(
        source: String,
        amount: Double,
        notes: String,
        user: String = "Admin",
        cashAccountId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = financeDao.insertIncome(
                IncomeEntity(
                    source = source,
                    amount = amount,
                    date = now,
                    notes = notes,
                    recordedBy = user
                )
            )
            val allAcc = cashAccountDao.getAllAccountsList()
            val acc = (cashAccountId?.let { cId -> allAcc.find { it.id == cId } }
                ?: allAcc.find { it.isDefault }
                ?: allAcc.find { it.accountType == "CASH" }
                ?: allAcc.firstOrNull())
            if (acc != null && amount > 0) {
                cashAccountDao.updateBalance(acc.id, acc.balance + amount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = acc.id,
                        accountName = acc.accountName,
                        amount = amount,
                        mutationType = "penjualan",
                        refType = "pemasukan",
                        refId = id.toString(),
                        notes = "$source: $notes",
                        timestamp = now
                    )
                )
            }
            id
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val allAcc = cashAccountDao.getAllAccountsList()
            val acc = allAcc.find { it.accountName.equals(expense.paymentSource, ignoreCase = true) }
                ?: allAcc.find { it.isDefault }
            if (acc != null && expense.amount > 0) {
                cashAccountDao.updateBalance(acc.id, acc.balance + expense.amount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = acc.id,
                        accountName = acc.accountName,
                        amount = expense.amount,
                        mutationType = "koreksi",
                        refType = "hapus_pengeluaran",
                        refId = expense.id.toString(),
                        notes = "Hapus pengeluaran ${expense.category}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            financeDao.deleteExpense(expense)
        }
    }

    suspend fun deleteIncome(income: IncomeEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val allAcc = cashAccountDao.getAllAccountsList()
            val acc = allAcc.find { it.isDefault }
                ?: allAcc.find { it.accountType == "CASH" }
                ?: allAcc.firstOrNull()
            if (acc != null && income.amount > 0) {
                cashAccountDao.updateBalance(acc.id, acc.balance - income.amount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = acc.id,
                        accountName = acc.accountName,
                        amount = -income.amount,
                        mutationType = "koreksi",
                        refType = "hapus_pemasukan",
                        refId = income.id.toString(),
                        notes = "Hapus pemasukan ${income.source}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            financeDao.deleteIncome(income)
        }
    }

    // Debt Payment (with Multi Kas Ledger)
    suspend fun recordDebtPayment(
        type: String,
        partyId: Long?,
        partyName: String,
        amount: Double,
        paymentMethod: String = "TUNAI",
        notes: String = "",
        user: String = "Admin",
        cashAccountId: Long? = null
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val payId = financeDao.insertDebtPayment(
                DebtPaymentEntity(
                    type = type,
                    partyId = partyId,
                    partyName = partyName,
                    amount = amount,
                    date = now,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    recordedBy = user
                )
            )

            val allAcc = cashAccountDao.getAllAccountsList()
            val targetAcc = (cashAccountId?.let { cId -> allAcc.find { it.id == cId } }
                ?: allAcc.find { it.isDefault }
                ?: allAcc.find { it.accountType == "CASH" }
                ?: allAcc.firstOrNull())

            if (type == "UTANG_PEMASOK" && partyId != null) {
                val supplier = contactDao.getAllSuppliers().first().find { it.id == partyId }
                if (supplier != null) {
                    val updatedDebt = (supplier.totalDebt - amount).coerceAtLeast(0.0)
                    contactDao.updateSupplier(supplier.copy(totalDebt = updatedDebt))
                }
                if (targetAcc != null && amount > 0) {
                    cashAccountDao.updateBalance(targetAcc.id, targetAcc.balance - amount)
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = targetAcc.id,
                            accountName = targetAcc.accountName,
                            amount = -amount,
                            mutationType = "bayar_supplier",
                            refType = "debt_payment",
                            refId = payId.toString(),
                            notes = "Bayar utang ke $partyName",
                            timestamp = now
                        )
                    )
                }
            } else if (type == "PIUTANG_PELANGGAN" && partyId != null) {
                val customer = contactDao.getAllCustomers().first().find { it.id == partyId }
                if (customer != null) {
                    val updatedReceivable = (customer.totalReceivable - amount).coerceAtLeast(0.0)
                    contactDao.updateCustomer(customer.copy(totalReceivable = updatedReceivable))
                }
                if (targetAcc != null && amount > 0) {
                    cashAccountDao.updateBalance(targetAcc.id, targetAcc.balance + amount)
                    extDao.insertCashMutation(
                        CashMutationEntity(
                            cashAccountId = targetAcc.id,
                            accountName = targetAcc.accountName,
                            amount = amount,
                            mutationType = "piutang_bayar",
                            refType = "debt_payment",
                            refId = payId.toString(),
                            notes = "Terima piutang dari $partyName",
                            timestamp = now
                        )
                    )
                }
            }
        }
    }

    // Contacts & Customer Deposit
    suspend fun insertCustomer(customer: CustomerEntity) = contactDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = contactDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = contactDao.deleteCustomer(customer)

    suspend fun topUpCustomerDeposit(customer: CustomerEntity, amount: Double) = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext
        db.withTransaction {
            contactDao.updateCustomer(customer.copy(depositBalance = customer.depositBalance + amount))
            val allAcc = cashAccountDao.getAllAccountsList()
            val targetAcc = allAcc.find { it.isDefault } ?: allAcc.find { it.accountType == "CASH" } ?: allAcc.firstOrNull()
            if (targetAcc != null) {
                cashAccountDao.updateBalance(targetAcc.id, targetAcc.balance + amount)
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = targetAcc.id,
                        accountName = targetAcc.accountName,
                        amount = amount,
                        mutationType = "penjualan",
                        refType = "deposit_pelanggan",
                        refId = customer.id.toString(),
                        notes = "Top up deposit pelanggan ${customer.name}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun insertSupplier(supplier: SupplierEntity) = contactDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: SupplierEntity) = contactDao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: SupplierEntity) = contactDao.deleteSupplier(supplier)

    // AI Command Log
    suspend fun logAICommand(log: AICommandLogEntity): Long = aiDao.insertLog(log)
    suspend fun updateAICommand(log: AICommandLogEntity) = aiDao.updateLog(log)

    // Settings
    suspend fun getSetting(key: String): String? = settingsDao.getSettingValue(key)
    suspend fun setSetting(key: String, value: String) =
        settingsDao.setSetting(AppSettingsEntity(settingKey = key, settingValue = value))

    // Multi Kas Accounts & Ledger
    suspend fun insertCashAccount(account: CashAccountEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val id = cashAccountDao.insertCashAccount(account)
            if (account.balance != 0.0) {
                extDao.insertCashMutation(
                    CashMutationEntity(
                        cashAccountId = id,
                        accountName = account.accountName,
                        amount = account.balance,
                        mutationType = "saldo_awal",
                        notes = "Saldo awal akun ${account.accountName}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            id
        }
    }

    suspend fun updateCashAccount(account: CashAccountEntity) = cashAccountDao.updateCashAccount(account)
    suspend fun deleteCashAccount(account: CashAccountEntity) = cashAccountDao.deleteCashAccount(account)
    suspend fun setDefaultCashAccount(id: Long) = cashAccountDao.setDefaultCashAccount(id)

    suspend fun adjustCashAccountBalance(accountId: Long, newBalance: Double, reason: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val acc = cashAccountDao.getCashAccountById(accountId) ?: return@withTransaction
            val diff = newBalance - acc.balance
            if (diff == 0.0) return@withTransaction
            cashAccountDao.updateBalance(accountId, newBalance)
            extDao.insertCashMutation(
                CashMutationEntity(
                    cashAccountId = acc.id,
                    accountName = acc.accountName,
                    amount = diff,
                    mutationType = "koreksi",
                    refType = "koreksi",
                    refId = acc.id.toString(),
                    notes = reason.ifBlank { "Koreksi saldo manual" },
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun transferCash(fromId: Long, toId: Long, amount: Double, notes: String, user: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val fromAcc = cashAccountDao.getCashAccountById(fromId) ?: return@withTransaction
            val toAcc = cashAccountDao.getCashAccountById(toId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            cashAccountDao.updateBalance(fromId, fromAcc.balance - amount)
            cashAccountDao.updateBalance(toId, toAcc.balance + amount)
            val tId = cashAccountDao.insertTransfer(
                CashTransferEntity(
                    fromAccountId = fromId,
                    fromAccountName = fromAcc.accountName,
                    toAccountId = toId,
                    toAccountName = toAcc.accountName,
                    amount = amount,
                    notes = notes,
                    date = now,
                    loggedBy = user
                )
            )
            extDao.insertCashMutation(
                CashMutationEntity(
                    cashAccountId = fromId,
                    accountName = fromAcc.accountName,
                    amount = -amount,
                    mutationType = "transfer",
                    refType = "transfer_kas",
                    refId = tId.toString(),
                    notes = "Transfer ke ${toAcc.accountName}" + if (notes.isNotBlank()) " ($notes)" else "",
                    timestamp = now
                )
            )
            extDao.insertCashMutation(
                CashMutationEntity(
                    cashAccountId = toId,
                    accountName = toAcc.accountName,
                    amount = amount,
                    mutationType = "transfer",
                    refType = "transfer_kas",
                    refId = tId.toString(),
                    notes = "Transfer dari ${fromAcc.accountName}" + if (notes.isNotBlank()) " ($notes)" else "",
                    timestamp = now
                )
            )
        }
    }

    // Admin Tier Services & Ranges
    suspend fun insertAdminTierService(service: AdminTierServiceEntity): Long = adminTierDao.insertService(service)
    suspend fun updateAdminTierService(service: AdminTierServiceEntity) = adminTierDao.updateService(service)
    suspend fun deleteAdminTierService(service: AdminTierServiceEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            adminTierDao.deleteRangesForService(service.id)
            adminTierDao.deleteService(service)
        }
    }
    suspend fun insertAdminTierRange(range: AdminTierRangeEntity): Long = adminTierDao.insertRange(range)
    suspend fun updateAdminTierRange(range: AdminTierRangeEntity) = adminTierDao.updateRange(range)
    suspend fun deleteAdminTierRange(range: AdminTierRangeEntity) = adminTierDao.deleteRange(range)
    suspend fun replaceAdminTierRanges(serviceId: Long, ranges: List<AdminTierRangeEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            adminTierDao.deleteRangesForService(serviceId)
            ranges.sortedBy { it.minAmount }.forEach { r ->
                adminTierDao.insertRange(r.copy(id = 0L, serviceId = serviceId))
            }
        }
    }
    suspend fun getTierRangesForService(serviceId: Long): List<AdminTierRangeEntity> = adminTierDao.getRangesList(serviceId)
    suspend fun findMatchingAdminTierRange(serviceId: Long, nominal: Double): AdminTierRangeEntity? =
        adminTierDao.findMatchingRange(serviceId, nominal)
    suspend fun hasOverlappingAdminTierRange(
        serviceId: Long,
        excludeId: Long,
        minAmount: Double,
        maxAmount: Double
    ): Boolean = adminTierDao.countOverlappingRanges(serviceId, excludeId, minAmount, maxAmount) > 0

    // Quantity Tier Pricing (harga_per_jumlah)
    fun getTierPricesForProduct(productId: Long): Flow<List<ProductTierPriceEntity>> = productDao.getTierPricesForProduct(productId)
    suspend fun getTierPricesList(productId: Long): List<ProductTierPriceEntity> = productDao.getTierPricesList(productId)
    suspend fun insertProductTierPrice(tier: ProductTierPriceEntity): Long = productDao.insertTierPrice(tier)
    suspend fun deleteProductTierPrice(tier: ProductTierPriceEntity) = productDao.deleteTierPrice(tier)
    suspend fun replaceProductTierPrices(productId: Long, tiers: List<ProductTierPriceEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            productDao.deleteTierPricesForProduct(productId)
            tiers.forEach { productDao.insertTierPrice(it.copy(id = 0L, productId = productId)) }
        }
    }

    // Wholesale Tiers (harga_bertingkat)
    suspend fun replaceWholesaleTiers(productId: Long, tiers: List<WholesaleTierEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            extDao.deleteWholesaleTiersForProduct(productId)
            tiers.forEach { extDao.insertWholesaleTier(it.copy(id = 0L, productId = productId)) }
        }
    }

    // Product Variants
    suspend fun replaceProductVariants(productId: Long, variants: List<ProductVariantEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            extDao.deleteVariantsForProduct(productId)
            variants.forEach { extDao.insertVariant(it.copy(id = 0L, productId = productId)) }
        }
    }

    // Product Batches
    suspend fun insertBatch(batch: ProductBatchEntity): Long = extDao.insertBatch(batch)
    suspend fun updateBatch(batch: ProductBatchEntity) = extDao.updateBatch(batch)
    suspend fun deleteBatch(batch: ProductBatchEntity) = extDao.deleteBatch(batch)

    // Bundles (paket & paket_isi)
    suspend fun saveBundleWithItems(bundle: BundlePackageEntity, items: List<BundleItemEntity>): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val bundleId = if (bundle.id == 0L) extDao.insertBundle(bundle) else {
                extDao.updateBundle(bundle)
                bundle.id
            }
            extDao.deleteItemsForBundle(bundleId)
            items.forEach { extDao.insertBundleItem(it.copy(id = 0L, bundleId = bundleId)) }
            bundleId
        }
    }

    suspend fun deleteBundle(bundle: BundlePackageEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            extDao.deleteItemsForBundle(bundle.id)
            extDao.deleteBundle(bundle)
        }
    }

    // Promos
    suspend fun savePromo(promo: PromoEntity): Long = extDao.insertPromo(promo)
    suspend fun updatePromo(promo: PromoEntity) = extDao.updatePromo(promo)
    suspend fun deletePromo(promo: PromoEntity) = extDao.deletePromo(promo)

    // Store Profile, Users, Permissions, Printers, Devices
    suspend fun saveStoreProfile(profile: StoreProfileEntity) = withContext(Dispatchers.IO) {
        extDao.upsertStoreProfile(profile.copy(id = 1L, updatedAt = System.currentTimeMillis()))
        setSetting("store_name", profile.name)
        setSetting("store_address", profile.address)
        setSetting("store_phone", profile.phone)
        setSetting("store_receipt_footer", profile.receiptFooter)
        setSetting("tax_percentage", (profile.ppnBp / 100.0).toString())
    }

    suspend fun saveUserWithPermissions(
        user: UserAccountEntity,
        permissions: Map<String, Boolean>
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val userId = if (user.id == 0L) extDao.insertUser(user) else {
                extDao.updateUser(user)
                user.id
            }
            extDao.deletePermissionsForUser(userId)
            permissions.forEach { (menuKey, allowed) ->
                extDao.insertMenuPermission(MenuPermissionEntity(userId = userId, menuKey = menuKey, allowed = allowed))
            }
            userId
        }
    }

    suspend fun deleteUser(user: UserAccountEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            extDao.deletePermissionsForUser(user.id)
            extDao.deleteUser(user)
        }
    }

    suspend fun savePrinter(printer: PrinterEntity): Long = extDao.insertPrinter(printer)
    suspend fun deletePrinter(printer: PrinterEntity) = extDao.deletePrinter(printer)

    suspend fun saveCashierDevice(device: CashierDeviceEntity): Long = extDao.insertCashierDevice(device)
    suspend fun deleteCashierDevice(device: CashierDeviceEntity) = extDao.deleteCashierDevice(device)

    // Shifts
    suspend fun startShift(cashierName: String, startingCash: Double, notes: String): Long = withContext(Dispatchers.IO) {
        val shift = CashierShiftEntity(
            cashierName = cashierName,
            startTime = System.currentTimeMillis(),
            startingCash = startingCash,
            expectedClosingCash = startingCash,
            status = "OPEN",
            notes = notes
        )
        shiftDao.insertShift(shift)
    }

    suspend fun closeShift(shiftId: Long, actualClosingCash: Double, notes: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val shifts = shiftDao.getAllShifts().first()
            val shift = shifts.find { it.id == shiftId } ?: return@withTransaction
            val diff = actualClosingCash - shift.expectedClosingCash
            val updated = shift.copy(
                endTime = System.currentTimeMillis(),
                actualClosingCash = actualClosingCash,
                difference = diff,
                status = "CLOSED",
                notes = if (notes.isNotBlank()) "${shift.notes} | $notes" else shift.notes
            )
            shiftDao.updateShift(updated)
        }
    }

    suspend fun recordShiftCashMovement(amount: Double, isCashIn: Boolean, reason: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val shift = shiftDao.getOpenShiftSync() ?: return@withTransaction
            val newCashIn = if (isCashIn) shift.cashIn + amount else shift.cashIn
            val newCashOut = if (!isCashIn) shift.cashOut + amount else shift.cashOut
            val newExpected = shift.startingCash + newCashIn - newCashOut + shift.totalCashSales
            shiftDao.updateShift(
                shift.copy(
                    cashIn = newCashIn,
                    cashOut = newCashOut,
                    expectedClosingCash = newExpected,
                    notes = "${shift.notes} [${if (isCashIn) "Kas Masuk" else "Kas Keluar"}: $reason Rp ${amount.toLong()}]"
                )
            )
            extDao.insertShiftCashMovement(
                ShiftCashMovementEntity(
                    shiftId = shift.id,
                    amount = if (isCashIn) amount else -amount,
                    notes = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Pending / Hold Sales
    suspend fun holdSale(
        holdName: String,
        cart: List<CartItem>,
        customer: CustomerEntity?,
        totalAmount: Double,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        cart.forEach { item ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", item.productId)
                    put("name", item.productName)
                    put("unit", item.unitName)
                    put("mul", item.multiplier)
                    put("price", item.unitPrice)
                    put("qty", item.quantity)
                    put("maxStock", item.maxAvailableBaseStock)
                    if (item.variantId != null) put("variantId", item.variantId)
                    put("variantName", item.variantName)
                    if (item.bundleId != null) put("bundleId", item.bundleId)
                    put("priceSource", item.priceSource)
                    if (item.serviceNominal != null) put("serviceNominal", item.serviceNominal)
                    put("storeAdminFee", item.storeAdminFee)
                    put("providerAdminFee", item.providerAdminFee)
                    put("agentCommission", item.agentCommission)
                    if (item.serviceCashAccountId != null) put("serviceCashAccountId", item.serviceCashAccountId)
                    put("serviceDirection", item.serviceDirection)
                }
            )
        }
        val pending = PendingSaleEntity(
            holdName = holdName.ifBlank { "Antrian #${System.currentTimeMillis() % 1000}" },
            cartJson = jsonArray.toString(),
            customerId = customer?.id,
            customerName = customer?.name ?: "Pelanggan Umum",
            totalAmount = totalAmount,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
        saleDao.insertPendingSale(pending)
    }

    suspend fun deletePendingSale(id: Long) = withContext(Dispatchers.IO) {
        saleDao.deletePendingSaleById(id)
    }

    suspend fun resetDatabaseToInitial() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        GeraiUnaDatabase.populateInitialData(db)
    }
}

data class CartItem(
    val productId: Long,
    val productName: String,
    val unitName: String,
    val multiplier: Double = 1.0,
    val unitPrice: Double,
    val quantity: Double = 1.0,
    val maxAvailableBaseStock: Double = 999.0,
    val variantId: Long? = null,
    val variantName: String = "",
    val bundleId: Long? = null,
    val priceSource: String = "tetap",
    val serviceNominal: Double? = null,
    val storeAdminFee: Double = 0.0,
    val providerAdminFee: Double = 0.0,
    val agentCommission: Double = 0.0,
    val serviceCashAccountId: Long? = null,
    val serviceDirection: String = "SETOR"
)

data class PurchaseItemDraft(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val purchasePrice: Double
)
