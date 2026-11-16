package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE stock <= minStock ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    // Unit conversions
    @Query("SELECT * FROM unit_conversions WHERE productId = :productId")
    fun getConversionsForProduct(productId: Long): Flow<List<UnitConversionEntity>>

    @Query("SELECT * FROM unit_conversions")
    fun getAllConversions(): Flow<List<UnitConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: UnitConversionEntity): Long

    @Delete
    suspend fun deleteConversion(conversion: UnitConversionEntity)

    // Aliases
    @Query("SELECT * FROM product_aliases")
    fun getAllAliases(): Flow<List<ProductAliasEntity>>

    @Query("SELECT * FROM product_aliases WHERE productId = :productId")
    suspend fun getAliasesForProduct(productId: Long): List<ProductAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: ProductAliasEntity): Long

    // Quantity Tier Prices
    @Query("SELECT * FROM product_tier_prices ORDER BY productId ASC, quantity ASC")
    fun getAllTierPrices(): Flow<List<ProductTierPriceEntity>>

    @Query("SELECT * FROM product_tier_prices WHERE productId = :productId ORDER BY quantity ASC")
    fun getTierPricesForProduct(productId: Long): Flow<List<ProductTierPriceEntity>>

    @Query("SELECT * FROM product_tier_prices WHERE productId = :productId ORDER BY quantity ASC")
    suspend fun getTierPricesList(productId: Long): List<ProductTierPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTierPrice(tier: ProductTierPriceEntity): Long

    @Delete
    suspend fun deleteTierPrice(tier: ProductTierPriceEntity)

    @Query("DELETE FROM product_tier_prices WHERE productId = :productId")
    suspend fun deleteTierPricesForProduct(productId: Long)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSalesBetween(startDate: Long, endDate: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItems(): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    // Pending / Hold Sales
    @Query("SELECT * FROM pending_sales ORDER BY createdAt DESC")
    fun getAllPendingSales(): Flow<List<PendingSaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSale(pending: PendingSaleEntity): Long

    @Delete
    suspend fun deletePendingSale(pending: PendingSaleEntity)

    @Query("DELETE FROM pending_sales WHERE id = :id")
    suspend fun deletePendingSaleById(id: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY date DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getPurchaseById(purchaseId: Long): PurchaseEntity?

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY date DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("SELECT * FROM debt_payments ORDER BY date DESC")
    fun getAllDebtPayments(): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)
}

@Dao
interface AIDao {
    @Query("SELECT * FROM ai_command_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AICommandLogEntity>>

    @Query("SELECT * FROM ai_command_logs WHERE status = 'DRAFT' ORDER BY timestamp DESC")
    fun getPendingDrafts(): Flow<List<AICommandLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AICommandLogEntity): Long

    @Update
    suspend fun updateLog(log: AICommandLogEntity)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Query("SELECT settingValue FROM app_settings WHERE settingKey = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingsEntity)
}

@Dao
interface CashAccountDao {
    @Query("SELECT * FROM cash_accounts ORDER BY sortOrder ASC, id ASC")
    fun getAllCashAccounts(): Flow<List<CashAccountEntity>>

    @Query("SELECT * FROM cash_accounts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllAccountsList(): List<CashAccountEntity>

    @Query("SELECT * FROM cash_accounts WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun getActiveCashAccounts(): Flow<List<CashAccountEntity>>

    @Query("SELECT * FROM cash_accounts WHERE id = :id LIMIT 1")
    suspend fun getCashAccountById(id: Long): CashAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashAccount(account: CashAccountEntity): Long

    @Update
    suspend fun updateCashAccount(account: CashAccountEntity)

    @Delete
    suspend fun deleteCashAccount(account: CashAccountEntity)

    @Query("UPDATE cash_accounts SET balance = :newBalance WHERE id = :id")
    suspend fun updateBalance(id: Long, newBalance: Double)

    @Query("UPDATE cash_accounts SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultCashAccount(id: Long)

    @Query("SELECT * FROM cash_transfers ORDER BY date DESC")
    fun getAllTransfers(): Flow<List<CashTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: CashTransferEntity): Long
}

@Dao
interface AdminTierDao {
    @Query("SELECT * FROM admin_tier_services ORDER BY id ASC")
    fun getAllServices(): Flow<List<AdminTierServiceEntity>>

    @Query("SELECT * FROM admin_tier_services ORDER BY id ASC")
    suspend fun getAllServicesList(): List<AdminTierServiceEntity>

    @Query("SELECT * FROM admin_tier_services WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveServices(): Flow<List<AdminTierServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: AdminTierServiceEntity): Long

    @Update
    suspend fun updateService(service: AdminTierServiceEntity)

    @Delete
    suspend fun deleteService(service: AdminTierServiceEntity)

    @Query("SELECT * FROM admin_tier_ranges WHERE serviceId = :serviceId ORDER BY minAmount ASC")
    fun getRangesForService(serviceId: Long): Flow<List<AdminTierRangeEntity>>

    @Query("SELECT * FROM admin_tier_ranges WHERE serviceId = :serviceId ORDER BY minAmount ASC")
    suspend fun getRangesList(serviceId: Long): List<AdminTierRangeEntity>

    @Query("SELECT * FROM admin_tier_ranges ORDER BY minAmount ASC")
    fun getAllRanges(): Flow<List<AdminTierRangeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRange(range: AdminTierRangeEntity): Long

    @Update
    suspend fun updateRange(range: AdminTierRangeEntity)

    @Delete
    suspend fun deleteRange(range: AdminTierRangeEntity)

    @Query("DELETE FROM admin_tier_ranges WHERE serviceId = :serviceId")
    suspend fun deleteRangesForService(serviceId: Long)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM cashier_shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<CashierShiftEntity>>

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    fun getCurrentOpenShift(): Flow<CashierShiftEntity?>

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenShiftSync(): CashierShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashierShiftEntity): Long

    @Update
    suspend fun updateShift(shift: CashierShiftEntity)
}

@Dao
interface ExtendedPosDao {
    // Store Profile
    @Query("SELECT * FROM store_profiles WHERE id = 1 LIMIT 1")
    fun getStoreProfile(): Flow<StoreProfileEntity?>

    @Query("SELECT * FROM store_profiles WHERE id = 1 LIMIT 1")
    suspend fun getStoreProfileSync(): StoreProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStoreProfile(profile: StoreProfileEntity)

    // Document Counters
    @Query("SELECT * FROM document_counters WHERE counterName = :name LIMIT 1")
    suspend fun getCounter(name: String): DocumentCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: DocumentCounterEntity)

    // Users & Permissions
    @Query("SELECT * FROM user_accounts ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts ORDER BY id ASC")
    suspend fun getAllUsersList(): List<UserAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity): Long

    @Update
    suspend fun updateUser(user: UserAccountEntity)

    @Delete
    suspend fun deleteUser(user: UserAccountEntity)

    @Query("SELECT * FROM menu_permissions")
    fun getAllMenuPermissions(): Flow<List<MenuPermissionEntity>>

    @Query("SELECT * FROM menu_permissions WHERE userId = :userId")
    suspend fun getPermissionsForUserList(userId: Long): List<MenuPermissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuPermission(permission: MenuPermissionEntity): Long

    @Query("DELETE FROM menu_permissions WHERE userId = :userId")
    suspend fun deletePermissionsForUser(userId: Long)

    // Printers & Cashier Devices
    @Query("SELECT * FROM printers ORDER BY id ASC")
    fun getAllPrinters(): Flow<List<PrinterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinter(printer: PrinterEntity): Long

    @Update
    suspend fun updatePrinter(printer: PrinterEntity)

    @Delete
    suspend fun deletePrinter(printer: PrinterEntity)

    @Query("SELECT * FROM cashier_devices ORDER BY id ASC")
    fun getAllCashierDevices(): Flow<List<CashierDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashierDevice(device: CashierDeviceEntity): Long

    @Update
    suspend fun updateCashierDevice(device: CashierDeviceEntity)

    @Delete
    suspend fun deleteCashierDevice(device: CashierDeviceEntity)

    // Product Variants
    @Query("SELECT * FROM product_variants ORDER BY productId ASC, id ASC")
    fun getAllVariants(): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE productId = :productId")
    suspend fun getVariantsForProduct(productId: Long): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE id = :id LIMIT 1")
    suspend fun getVariantById(id: Long): ProductVariantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity): Long

    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)

    @Query("UPDATE product_variants SET stock = :newStock WHERE id = :variantId")
    suspend fun updateVariantStock(variantId: Long, newStock: Double)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: Long)

    // Wholesale Tiers (harga_bertingkat)
    @Query("SELECT * FROM wholesale_tiers ORDER BY productId ASC, minQty ASC")
    fun getAllWholesaleTiers(): Flow<List<WholesaleTierEntity>>

    @Query("SELECT * FROM wholesale_tiers WHERE productId = :productId ORDER BY minQty ASC")
    suspend fun getWholesaleTiersForProduct(productId: Long): List<WholesaleTierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWholesaleTier(tier: WholesaleTierEntity): Long

    @Query("DELETE FROM wholesale_tiers WHERE productId = :productId")
    suspend fun deleteWholesaleTiersForProduct(productId: Long)

    // Product Batches (batch_produk)
    @Query("SELECT * FROM product_batches ORDER BY expiryDateStr ASC")
    fun getAllBatches(): Flow<List<ProductBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ProductBatchEntity): Long

    @Update
    suspend fun updateBatch(batch: ProductBatchEntity)

    @Delete
    suspend fun deleteBatch(batch: ProductBatchEntity)

    // Bundle Packages (paket & paket_isi)
    @Query("SELECT * FROM bundle_packages ORDER BY name ASC")
    fun getAllBundles(): Flow<List<BundlePackageEntity>>

    @Query("SELECT * FROM bundle_items")
    fun getAllBundleItems(): Flow<List<BundleItemEntity>>

    @Query("SELECT * FROM bundle_items WHERE bundleId = :bundleId")
    suspend fun getItemsForBundle(bundleId: Long): List<BundleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundle(bundle: BundlePackageEntity): Long

    @Update
    suspend fun updateBundle(bundle: BundlePackageEntity)

    @Delete
    suspend fun deleteBundle(bundle: BundlePackageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundleItem(item: BundleItemEntity): Long

    @Query("DELETE FROM bundle_items WHERE bundleId = :bundleId")
    suspend fun deleteItemsForBundle(bundleId: Long)

    // Promos
    @Query("SELECT * FROM promos ORDER BY id DESC")
    fun getAllPromos(): Flow<List<PromoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromo(promo: PromoEntity): Long

    @Update
    suspend fun updatePromo(promo: PromoEntity)

    @Delete
    suspend fun deletePromo(promo: PromoEntity)

    // Cash Mutations Ledger (mutasi_kas)
    @Query("SELECT * FROM cash_mutations ORDER BY timestamp DESC, id DESC")
    fun getAllCashMutations(): Flow<List<CashMutationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMutation(mutation: CashMutationEntity): Long

    // Shift Cash Movements (shift_kas)
    @Query("SELECT * FROM shift_cash_movements ORDER BY timestamp DESC")
    fun getAllShiftCashMovements(): Flow<List<ShiftCashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftCashMovement(movement: ShiftCashMovementEntity): Long

    // Cloud Sync Mappings
    @Query("SELECT * FROM cloud_sync_mappings WHERE entityType = :entityType AND remoteId = :remoteId LIMIT 1")
    suspend fun getMappingByRemote(entityType: String, remoteId: String): CloudSyncMappingEntity?

    @Query("SELECT * FROM cloud_sync_mappings WHERE entityType = :entityType AND localId = :localId LIMIT 1")
    suspend fun getMappingByLocal(entityType: String, localId: Long): CloudSyncMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCloudMapping(mapping: CloudSyncMappingEntity): Long

    @Query("DELETE FROM cloud_sync_mappings")
    suspend fun clearAllCloudMappings()
}


