package com.example.data.pricing

import com.example.data.entity.AdminTierRangeEntity
import com.example.data.entity.AdminTierServiceEntity
import com.example.data.entity.AdminTierWithRanges

data class TierLookupResult(
    val service: AdminTierServiceEntity?,
    val matchedRange: AdminTierRangeEntity?,
    val nominal: Double,
    val storeAdminFee: Double,
    val providerAdminFee: Double,
    val agentCommission: Double,
    val isAutoDetected: Boolean
) {
    val netProfit: Double
        get() = storeAdminFee + agentCommission - providerAdminFee

    val totalCustomerPayable: Double
        get() = nominal + storeAdminFee
}

object TierPricingResolver {

    /**
     * Mendeteksi rentang tarif yang cocok untuk suatu nominal secara akurat.
     * - Jika `maxAmount <= 0.0`, dianggap rentang terbuka (tanpa batas atas).
     * - Mengurutkan berdasarkan `minAmount` menurun agar rentang paling spesifik/tinggi
     *   terpilih terlebih dahulu bila ada batas bersebelahan.
     */
    fun findMatchingRange(
        ranges: List<AdminTierRangeEntity>,
        nominal: Double
    ): AdminTierRangeEntity? {
        if (nominal <= 0.0 || ranges.isEmpty()) return null
        return ranges
            .sortedByDescending { it.minAmount }
            .firstOrNull { range ->
                val minOk = nominal >= range.minAmount
                val maxOk = range.maxAmount <= 0.0 || nominal <= range.maxAmount
                minOk && maxOk
            }
    }

    /**
     * Memeriksa apakah rentang kandidat bertumpukan dengan rentang lain pada grup layanan yang sama.
     */
    fun hasOverlap(
        existingRanges: List<AdminTierRangeEntity>,
        minAmount: Double,
        maxAmount: Double,
        excludeRangeId: Long = 0L
    ): Boolean {
        val candMax = if (maxAmount <= 0.0) Double.POSITIVE_INFINITY else maxAmount
        return existingRanges.any { other ->
            if (excludeRangeId != 0L && other.id == excludeRangeId) return@any false
            val otherMax = if (other.maxAmount <= 0.0) Double.POSITIVE_INFINITY else other.maxAmount
            minAmount <= otherMax && other.minAmount <= candMax
        }
    }

    /**
     * Memilih layanan tarif terbaik untuk suatu produk digital:
     * 1. Prioritaskan `preferredServiceId` (produk.adminTierGroupId) jika ada & aktif.
     * 2. Fallback ke kecocokan nama layanan vs nama produk.
     * 3. Fallback ke layanan aktif pertama.
     */
    fun resolveServiceGroup(
        adminTiers: List<AdminTierWithRanges>,
        preferredServiceId: Long?,
        productName: String
    ): AdminTierWithRanges? {
        if (adminTiers.isEmpty()) return null
        if (preferredServiceId != null) {
            adminTiers.find { it.service.id == preferredServiceId && it.service.isActive }?.let { return it }
            adminTiers.find { it.service.id == preferredServiceId }?.let { return it }
        }
        return adminTiers.find {
            it.service.isActive && (
                productName.contains(it.service.serviceName, ignoreCase = true) ||
                    it.service.serviceName.contains(productName, ignoreCase = true)
                )
        } ?: adminTiers.firstOrNull { it.service.isActive } ?: adminTiers.firstOrNull()
    }

    /**
     * Menghitung seluruh komponen tarif admin, fee agen, potongan penyedia, dan laba bersih
     * berdasarkan nominal transaksi/produk secara real-time.
     */
    fun calculate(
        serviceWithRanges: AdminTierWithRanges?,
        nominal: Double,
        manualAdminFeeOverride: Double? = null
    ): TierLookupResult {
        val matched = serviceWithRanges?.let { findMatchingRange(it.ranges, nominal) }
        val storeAdmin = manualAdminFeeOverride ?: matched?.storeAdminFee ?: 0.0
        val providerAdmin = matched?.providerAdminFee ?: 0.0
        val agentFee = matched?.agentCommission ?: 0.0

        return TierLookupResult(
            service = serviceWithRanges?.service,
            matchedRange = matched,
            nominal = nominal,
            storeAdminFee = storeAdmin,
            providerAdminFee = providerAdmin,
            agentCommission = agentFee,
            isAutoDetected = matched != null && manualAdminFeeOverride == null
        )
    }
}
