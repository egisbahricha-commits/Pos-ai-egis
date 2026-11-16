package com.example.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SyncMode {
    PUSH_ONLY,
    PULL_ONLY,
    TWO_WAY
}

data class CloudSyncSummary(
    val pushedRecords: Int,
    val pulledRecords: Int,
    val timestamp: Long,
    val message: String
)

data class FirebaseAuthUiState(
    val hasEnteredApp: Boolean = false,
    val isCloudSignedIn: Boolean = false,
    val isOfflineMode: Boolean = false,
    val userId: String = "",
    val userEmail: String = "",
    val fullName: String = "",
    val storeName: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val lastEmail: String = "",
    val isAuthLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val authError: String? = null,
    val authInfoMessage: String? = null,
    val autoSyncEnabled: Boolean = true,
    val lastSyncAt: Long = 0L,
    val lastSyncMessage: String = ""
)

@IgnoreExtraProperties
data class UserStoreProfileDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "GERAI UNA SMART POS",
    val address: String = "",
    val phone: String = "",
    val businessType: String = "keduanya",
    val ppnBp: Long = 0L,
    val receiptFooter: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val profileUpdatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashAccountDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val accountName: String = "Kas Toko",
    val balance: Double = 0.0,
    val accountType: String = "CASH",
    @field:JvmField val isActive: Boolean = true,
    val color: String = "#2563EB",
    val notes: String = "",
    @field:JvmField val isDefault: Boolean = false,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashMutationDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val cashAccountId: Long = 1L,
    val accountName: String = "Kas Toko",
    val amount: Double = 0.0,
    val mutationType: String = "koreksi",
    val refType: String = "",
    val refId: String = "",
    val notes: String = "",
    val mutationTimeMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashTransferDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val fromAccountName: String = "",
    val toAccountName: String = "",
    val amount: Double = 0.0,
    val notes: String = "",
    val transferDateMillis: Long = 0L,
    val loggedBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SaleDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val invoiceNumber: String = "TRX-CLOUD",
    val saleDateMillis: Long = 0L,
    val cashierName: String = "Kasir",
    val customerName: String = "Pelanggan Umum",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMethod: String = "TUNAI",
    val amountPaid: Double = 0.0,
    val changeAmount: Double = 0.0,
    val status: String = "COMPLETED",
    val notes: String = "",
    val storeAdminFee: Double = 0.0,
    val providerAdminFee: Double = 0.0,
    val agentCommission: Double = 0.0,
    val shippingCost: Double = 0.0,
    val additionalFee: Double = 0.0,
    val promoDiscount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val category: String = "Operasional",
    val amount: Double = 0.0,
    val expenseDateMillis: Long = 0L,
    val notes: String = "",
    val paymentSource: String = "Kas Toko",
    val recordedBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IncomeDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val source: String = "Lainnya",
    val amount: Double = 0.0,
    val incomeDateMillis: Long = 0L,
    val notes: String = "",
    val recordedBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val barcode: String = "",
    val name: String = "Produk",
    val category: String = "Umum",
    val baseUnit: String = "pcs",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val productType: String = "FISIK",
    @field:JvmField val isActive: Boolean = true,
    @field:JvmField val hasNoStock: Boolean = false,
    @field:JvmField val allowDecimalQty: Boolean = false,
    @field:JvmField val isFlexiblePrice: Boolean = false,
    val specialPrice: Double? = null,
    val strikePrice: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val name: String = "Pelanggan",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalReceivable: Double = 0.0,
    val depositBalance: Double = 0.0,
    val loyaltyPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupplierDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val name: String = "Pemasok",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalDebt: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DebtPaymentDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val paymentType: String = "PIUTANG_PELANGGAN",
    val referenceInvoice: String = "",
    val partyName: String = "",
    val amount: Double = 0.0,
    val paymentDateMillis: Long = 0L,
    val paymentMethod: String = "TUNAI",
    val notes: String = "",
    val recordedBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashierShiftDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val localId: Long = 0L,
    val cashierName: String = "Kasir",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long? = null,
    val startingCash: Double = 0.0,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val expectedClosingCash: Double = 0.0,
    val actualClosingCash: Double? = null,
    val difference: Double? = null,
    val status: String = "OPEN",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
