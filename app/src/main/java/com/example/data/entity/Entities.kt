package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String = "",
    val name: String,
    val category: String = "Umum",
    val baseUnit: String = "pcs",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val brand: String = "",
    val isActive: Boolean = true,
    val productType: String = "FISIK", // "FISIK", "DIGITAL" (PPOB, pulsa, token, tagihan)
    val isUnlimitedStock: Boolean = false,
    val isFlexiblePrice: Boolean = false, // Harga diisi fleksibel saat transaksi di kasir
    val hasNoStock: Boolean = false, // Saklar tanpa stok (jasa)
    val allowDecimalQty: Boolean = false, // Desimal kg, meter, liter
    val serialOrImei: String = "",
    val expiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Schema v3 additions
    val autoBarcode: Boolean = false,
    val specialPrice: Double? = null,
    val strikePrice: Double? = null,
    val hasVariants: Boolean = false,
    val adminTierGroupId: Long? = null,
    val serviceCashAccountId: Long? = null,
    val serviceDirection: String = "SETOR" // "SETOR" (saldo layanan turun) atau "TARIK" (saldo layanan naik)
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "tag"
)

@Entity(tableName = "unit_conversions")
data class UnitConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val unitName: String, // e.g. "dus", "karton", "pack"
    val multiplier: Double, // e.g. 1 dus = 40 pcs -> multiplier = 40.0
    val customSellingPrice: Double? = null,
    val customPurchasePrice: Double? = null
)

@Entity(tableName = "product_aliases")
data class ProductAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val alias: String // e.g. "aquaa", "indomi", "rinso cair"
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalReceivable: Double = 0.0,
    // Schema v3 additions
    val depositBalance: Double = 0.0,
    val loyaltyPoints: Int = 0
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalDebt: Double = 0.0
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val date: Long = System.currentTimeMillis(),
    val cashierName: String = "Kasir 1",
    val customerId: Long? = null,
    val customerName: String = "Pelanggan Umum",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMethod: String = "TUNAI", // TUNAI, QRIS, TRANSFER, KREDIT, SPLIT, DEPOSIT
    val amountPaid: Double = 0.0,
    val changeAmount: Double = 0.0,
    val status: String = "COMPLETED", // COMPLETED, PRE_ORDER, REFUNDED, CANCELLED
    val notes: String = "",
    // Multi Kas & Admin Fee Konter
    val storeAdminFee: Double = 0.0,
    val providerAdminFee: Double = 0.0,
    val agentCommission: Double = 0.0,
    val cashAccountId: Long? = null,
    // Split Payment (e.g. Tunai + QRIS)
    val splitCashAmount: Double = 0.0,
    val splitNonCashAmount: Double = 0.0,
    val splitPaymentMethod: String = "",
    // Schema v3 additions
    val shippingCost: Double = 0.0,
    val additionalFee: Double = 0.0,
    val promoDiscount: Double = 0.0,
    val appliedPromoName: String = "",
    val depositUsed: Double = 0.0,
    val pointsEarned: Int = 0
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val subtotal: Double,
    val purchasePriceSnapshot: Double = 0.0,
    // Schema v3 additions
    val variantId: Long? = null,
    val variantName: String = "",
    val priceSource: String = "tetap", // tetap, grosir, per_jumlah, fleksibel, manual
    val serviceNominal: Double? = null,
    val storeAdminFee: Double = 0.0,
    val providerAdminFee: Double = 0.0,
    val agentCommission: Double = 0.0,
    val adminModifiedManually: Boolean = false
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val date: Long = System.currentTimeMillis(),
    val supplierId: Long? = null,
    val supplierName: String = "Pemasok Umum",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val additionalCost: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMethod: String = "TUNAI", // TUNAI, KREDIT
    val paymentStatus: String = "LUNAS", // LUNAS, SEBAGIAN, BELUM_LUNAS
    val status: String = "DRAFT", // DRAFT, MENUNGGU, DIPESAN, DITERIMA, DIBATALKAN
    val notes: String = "",
    val receivedDate: Long? = null
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val purchasePrice: Double,
    val subtotal: Double
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val date: Long = System.currentTimeMillis(),
    val movementType: String, // PEMBELIAN_MASUK, PENJUALAN_KELUAR, PENYESUAIAN_MASUK, PENYESUAIAN_KELUAR, RETUR
    val quantityChange: Double,
    val unit: String = "pcs",
    val previousStock: Double,
    val currentStock: Double,
    val referenceNumber: String = "",
    val notes: String = "",
    val loggedBy: String = "Admin"
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // Listrik, Air, Transportasi, Konsumsi, Perlengkapan, ATK, Perawatan, Operasional
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentSource: String = "Kas Toko",
    val recordedBy: String = "Admin"
)

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String, // Jasa Transfer, Komisi, Titipan, Lainnya
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val recordedBy: String = "Admin"
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // UTANG_PEMASOK or PIUTANG_PELANGGAN
    val referenceInvoice: String = "",
    val partyId: Long? = null,
    val partyName: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = "TUNAI",
    val notes: String = "",
    val recordedBy: String = "Admin"
)

@Entity(tableName = "ai_command_logs")
data class AICommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String = "APP_CHAT", // APP_CHAT, WHATSAPP
    val senderPhone: String = "",
    val rawMessage: String,
    val parsedIntent: String,
    val structuredJson: String,
    val status: String = "DRAFT", // DRAFT, CONFIRMED, EXECUTED, REJECTED
    val responseText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String
)

@Entity(tableName = "cash_accounts")
data class CashAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountName: String, // "Kas Toko", "Deposit Pulsa", "Saldo BRILink / Agen Bank", "Bank BCA"
    val balance: Double = 0.0, // Saldo boleh negatif (e.g. deposit pulsa / agen bank defisit)
    val accountType: String = "CASH", // "CASH", "PULSA_DEPOSIT", "BANK_AGENT", "BANK_ACCOUNT"
    val isActive: Boolean = true,
    val color: String = "#2563EB",
    val notes: String = "",
    // Schema v3 additions
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "cash_transfers")
data class CashTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val fromAccountName: String,
    val toAccountId: Long,
    val toAccountName: String,
    val amount: Double,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val loggedBy: String = "Admin"
)

@Entity(tableName = "admin_tier_services")
data class AdminTierServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceName: String, // e.g. "Transfer Antar Bank", "Transfer Sesama Bank", "Tarik Tunai Tunai", "Bayar Tagihan PPOB"
    val serviceCode: String = "TRANSFER_BANK",
    val isActive: Boolean = true
)

@Entity(
    tableName = "admin_tier_ranges",
    indices = [Index(value = ["serviceId", "minAmount", "maxAmount"])]
)
data class AdminTierRangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val minAmount: Double,
    val maxAmount: Double,
    val storeAdminFee: Double, // Dikenakan ke pelanggan (pendapatan toko)
    val providerAdminFee: Double, // Dipotong penyedia dari Saldo BRILink/Penyedia
    val agentCommission: Double // Fee yang diberikan penyedia ke toko
)

@Entity(tableName = "product_tier_prices")
data class ProductTierPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Double, // Jumlah beli: misal 1, 2, 3, 4
    val totalPrice: Double // Total harga untuk jumlah itu: misal beli 1=2000, beli 2=3000, dst.
)

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val startingCash: Double = 0.0, // Kas awal di laci
    val cashIn: Double = 0.0, // Tambahan kas masuk selama shift
    val cashOut: Double = 0.0, // Kas keluar selama shift
    val totalCashSales: Double = 0.0, // Total transaksi tunai sistem
    val expectedClosingCash: Double = 0.0, // startingCash + cashIn - cashOut + totalCashSales
    val actualClosingCash: Double? = null, // Uang fisik dihitung saat tutup
    val difference: Double? = null, // Selisih uang laci vs catatan sistem
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val notes: String = ""
)

@Entity(tableName = "pending_sales")
data class PendingSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdName: String, // e.g. "Antrian Meja 1", "Pelanggan Jaket Biru"
    val cartJson: String,
    val customerId: Long? = null,
    val customerName: String = "Pelanggan Umum",
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// =========================================================================
// Schema v3 Entities (Full schema.sql Parity)
// =========================================================================

@Entity(tableName = "store_profiles")
data class StoreProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "GERAI UNA SMART POS",
    val address: String = "Jl. Garuda No. 8, Jakarta",
    val phone: String = "081234567890",
    val logoUri: String? = null,
    val businessType: String = "keduanya", // retail, grosir, keduanya
    val ppnBp: Int = 0, // basis poin: 1100 = 11%
    val receiptFooter: String = "Terima Kasih Telah Berbelanja di Gerai Una!",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_counters")
data class DocumentCounterEntity(
    @PrimaryKey val counterName: String, // 'transaksi', 'pesanan_beli'
    val value: Long = 0L
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val username: String,
    val role: String = "kasir", // owner, kasir
    val pinHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "menu_permissions")
data class MenuPermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val menuKey: String, // e.g. "LAPORAN", "MULTI_KAS", "PENGATURAN", "RETUR", "HAPUS_PRODUK"
    val allowed: Boolean = true
)

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bluetoothAddress: String = "00:11:22:33:44:55",
    val type: String = "struk", // struk, stiker
    val language: String = "escpos", // escpos, tspl
    val widthMm: Int = 58, // 40, 58, 80
    val autoCut: Boolean = false,
    val openDrawer: Boolean = false,
    val quickPrint: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashier_devices")
data class CashierDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "utama", // utama, kasir
    val lastConnected: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val name: String,
    val barcode: String = "",
    val sellingPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val stock: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wholesale_tiers")
data class WholesaleTierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val unitName: String = "", // kosong = satuan dasar
    val minQty: Double,
    val pricePerUnit: Double
)

@Entity(tableName = "product_batches")
data class ProductBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val batchCode: String = "",
    val expiryDateStr: String, // 'YYYY-MM-DD'
    val qty: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bundle_packages")
data class BundlePackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sellingPrice: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bundle_items")
data class BundleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bundleId: Long,
    val productId: Long,
    val qty: Double = 1.0
)

@Entity(tableName = "promos")
data class PromoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val promoType: String, // tebus_murah, beli_x_gratis_y, poin, stempel, voucher
    val name: String,
    val code: String = "",
    val minPurchase: Double = 0.0,
    val discountAmount: Double = 0.0,
    val buyQty: Int = 0,
    val freeQty: Int = 0,
    val isActive: Boolean = true,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_mutations")
data class CashMutationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashAccountId: Long,
    val accountName: String,
    val amount: Double, // bertanda: + masuk, - keluar
    val mutationType: String, // saldo_awal, penjualan, layanan, admin_penyedia, fee, piutang_bayar, pengeluaran, transfer, pembelian, bayar_supplier, retur, shift, koreksi
    val refType: String = "",
    val refId: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shift_cash_movements")
data class ShiftCashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val amount: Double, // + kas masuk, - kas keluar
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cloud_sync_mappings")
data class CloudSyncMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val remoteId: String,
    val localId: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AdminTierWithRanges(
    val service: AdminTierServiceEntity,
    val ranges: List<AdminTierRangeEntity>
)
