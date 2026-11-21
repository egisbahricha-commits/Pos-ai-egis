package com.example.data.pricing

import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ProfitLossPeriod(val key: String, val label: String) {
    HARIAN("HARIAN", "Harian"),
    MINGGUAN("MINGGUAN", "Mingguan"),
    BULANAN("BULANAN", "Bulanan"),
    TAHUNAN("TAHUNAN", "Tahunan")
}

data class ProfitLossWindow(
    val period: ProfitLossPeriod,
    val offset: Int,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val rangeLabel: String
)

data class ProfitLossBucket(
    val label: String,
    val subLabel: String,
    val startMs: Long,
    val endMs: Long,
    val transactionCount: Int,
    val netSales: Double,
    val totalCost: Double,
    val netProfit: Double
)

data class ProfitLossSummary(
    val window: ProfitLossWindow,
    val transactionCount: Int,
    val grossSales: Double,
    val totalDiscounts: Double,
    val netSales: Double,
    val cogs: Double,
    val storeAdminRevenue: Double,
    val providerAdminCost: Double,
    val grossProfit: Double,
    val agentCommissionIncome: Double,
    val otherIncomes: Double,
    val operatingExpenses: Double,
    val netProfit: Double,
    val marginPercent: Double,
    val previousNetProfit: Double,
    val profitDeltaVsPrevious: Double,
    val physicalSales: Double = 0.0,
    val digitalSales: Double = 0.0,
    val physicalNetProfit: Double = 0.0,
    val digitalNetProfit: Double = 0.0,
    val subBuckets: List<ProfitLossBucket>
) {
    val isProfit: Boolean get() = netProfit >= 0.0
}

object ProfitLossCalculator {

    private val idLocale = Locale("id", "ID")

    fun resolveWindow(
        period: ProfitLossPeriod,
        offset: Int = 0,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): ProfitLossWindow {
        val cal = Calendar.getInstance(idLocale).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = referenceTimeMs
            setStartOfDay(this)
        }

        return when (period) {
            ProfitLossPeriod.HARIAN -> {
                cal.add(Calendar.DAY_OF_YEAR, offset)
                val startMs = cal.timeInMillis
                val endCal = (cal.clone() as Calendar).also { setEndOfDay(it) }
                val endMs = endCal.timeInMillis
                val dateFmt = SimpleDateFormat("dd MMM yyyy", idLocale)
                val fullDayFmt = SimpleDateFormat("EEEE, dd MMM yyyy", idLocale)
                val title = when (offset) {
                    0 -> "Hari Ini"
                    -1 -> "Kemarin"
                    else -> fullDayFmt.format(Date(startMs))
                }
                ProfitLossWindow(
                    period = period,
                    offset = offset,
                    startMs = startMs,
                    endMs = endMs,
                    title = title,
                    rangeLabel = dateFmt.format(Date(startMs))
                )
            }

            ProfitLossPeriod.MINGGUAN -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
                cal.add(Calendar.DAY_OF_YEAR, diffToMonday)
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                val startMs = cal.timeInMillis
                val endCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    setEndOfDay(this)
                }
                val endMs = endCal.timeInMillis
                val startFmt = SimpleDateFormat("dd MMM", idLocale)
                val endFmt = SimpleDateFormat("dd MMM yyyy", idLocale)
                val title = when (offset) {
                    0 -> "Minggu Ini"
                    -1 -> "Minggu Lalu"
                    else -> "Minggu"
                }
                ProfitLossWindow(
                    period = period,
                    offset = offset,
                    startMs = startMs,
                    endMs = endMs,
                    title = title,
                    rangeLabel = "${startFmt.format(Date(startMs))} – ${endFmt.format(Date(endMs))}"
                )
            }

            ProfitLossPeriod.BULANAN -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offset)
                val startMs = cal.timeInMillis
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    setEndOfDay(this)
                }
                val endMs = endCal.timeInMillis
                val monthFmt = SimpleDateFormat("MMMM yyyy", idLocale)
                val formattedMonth = monthFmt.format(Date(startMs))
                val title = when (offset) {
                    0 -> "Bulan Ini"
                    -1 -> "Bulan Lalu"
                    else -> formattedMonth
                }
                ProfitLossWindow(
                    period = period,
                    offset = offset,
                    startMs = startMs,
                    endMs = endMs,
                    title = title,
                    rangeLabel = formattedMonth
                )
            }

            ProfitLossPeriod.TAHUNAN -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.YEAR, offset)
                val startMs = cal.timeInMillis
                val year = cal.get(Calendar.YEAR)
                val endCal = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    setEndOfDay(this)
                }
                val endMs = endCal.timeInMillis
                val title = when (offset) {
                    0 -> "Tahun Ini"
                    -1 -> "Tahun Lalu"
                    else -> "Tahun $year"
                }
                ProfitLossWindow(
                    period = period,
                    offset = offset,
                    startMs = startMs,
                    endMs = endMs,
                    title = title,
                    rangeLabel = "Jan – Des $year"
                )
            }
        }
    }

    fun calculate(
        period: ProfitLossPeriod,
        offset: Int = 0,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        products: List<ProductEntity>,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): ProfitLossSummary {
        val window = resolveWindow(period, offset, referenceTimeMs)
        val prevWindow = resolveWindow(period, offset - 1, referenceTimeMs)

        val itemsBySaleId = saleItems.groupBy { it.saleId }
        val productsById = products.associateBy { it.id }

        val currentMetrics = computeRangeMetrics(
            startMs = window.startMs,
            endMs = window.endMs,
            sales = sales,
            itemsBySaleId = itemsBySaleId,
            expenses = expenses,
            incomes = incomes,
            productsById = productsById
        )

        val prevMetrics = computeRangeMetrics(
            startMs = prevWindow.startMs,
            endMs = prevWindow.endMs,
            sales = sales,
            itemsBySaleId = itemsBySaleId,
            expenses = expenses,
            incomes = incomes,
            productsById = productsById
        )

        val subBuckets = buildSubBuckets(
            window = window,
            sales = sales,
            itemsBySaleId = itemsBySaleId,
            expenses = expenses,
            incomes = incomes,
            productsById = productsById
        )

        val marginPercent = if (currentMetrics.netSales > 0.0) {
            (currentMetrics.netProfit / currentMetrics.netSales) * 100.0
        } else {
            0.0
        }

        return ProfitLossSummary(
            window = window,
            transactionCount = currentMetrics.transactionCount,
            grossSales = currentMetrics.grossSales,
            totalDiscounts = currentMetrics.totalDiscounts,
            netSales = currentMetrics.netSales,
            cogs = currentMetrics.cogs,
            storeAdminRevenue = currentMetrics.storeAdminRevenue,
            providerAdminCost = currentMetrics.providerAdminCost,
            grossProfit = currentMetrics.grossProfit,
            agentCommissionIncome = currentMetrics.agentCommissionIncome,
            otherIncomes = currentMetrics.otherIncomes,
            operatingExpenses = currentMetrics.operatingExpenses,
            netProfit = currentMetrics.netProfit,
            marginPercent = marginPercent,
            previousNetProfit = prevMetrics.netProfit,
            profitDeltaVsPrevious = currentMetrics.netProfit - prevMetrics.netProfit,
            physicalSales = currentMetrics.physicalSales,
            digitalSales = currentMetrics.digitalSales,
            physicalNetProfit = currentMetrics.physicalNetProfit,
            digitalNetProfit = currentMetrics.digitalNetProfit,
            subBuckets = subBuckets
        )
    }

    private data class RangeMetrics(
        val transactionCount: Int,
        val grossSales: Double,
        val totalDiscounts: Double,
        val netSales: Double,
        val cogs: Double,
        val storeAdminRevenue: Double,
        val providerAdminCost: Double,
        val grossProfit: Double,
        val agentCommissionIncome: Double,
        val otherIncomes: Double,
        val operatingExpenses: Double,
        val netProfit: Double,
        val physicalSales: Double,
        val digitalSales: Double,
        val physicalNetProfit: Double,
        val digitalNetProfit: Double
    )

    private fun computeRangeMetrics(
        startMs: Long,
        endMs: Long,
        sales: List<SaleEntity>,
        itemsBySaleId: Map<Long, List<SaleItemEntity>>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        productsById: Map<Long, ProductEntity>
    ): RangeMetrics {
        val activeSales = sales.filter {
            it.status == "COMPLETED" &&
                it.date in startMs..endMs
        }
        val periodExpenses = expenses.filter { it.date in startMs..endMs }
        val periodIncomes = incomes.filter { it.date in startMs..endMs }

        var grossSales = 0.0
        var totalDiscounts = 0.0
        var netSales = 0.0
        var totalCogs = 0.0
        var storeAdminRevenue = 0.0
        var providerAdminCost = 0.0
        var agentCommissionIncome = 0.0
        var physicalSales = 0.0
        var digitalSales = 0.0
        var physicalGrossProfit = 0.0
        var digitalGrossProfit = 0.0

        activeSales.forEach { sale ->
            val items = itemsBySaleId[sale.id].orEmpty()
            grossSales += sale.subtotal + sale.storeAdminFee + sale.tax + sale.shippingCost + sale.additionalFee
            totalDiscounts += sale.discount + sale.promoDiscount
            netSales += sale.finalAmount
            storeAdminRevenue += sale.storeAdminFee
            providerAdminCost += sale.providerAdminFee
            agentCommissionIncome += sale.agentCommission

            if (items.isNotEmpty()) {
                val saleDiscounts = sale.discount + sale.promoDiscount
                var saleDigitalFlag = false
                items.forEach { item ->
                    val product = productsById[item.productId]
                    val isDigitalItem = item.storeAdminFee > 0.0 ||
                        (item.serviceNominal != null && item.serviceNominal > 0.0) ||
                        product?.productType == "DIGITAL"
                    val itemCost = when {
                        item.purchasePriceSnapshot > 0.0 -> item.purchasePriceSnapshot * item.quantity
                        item.serviceNominal != null && item.serviceNominal > 0.0 -> item.serviceNominal
                        product != null && product.purchasePrice > 0.0 -> product.purchasePrice * item.quantity
                        isDigitalItem -> item.subtotal
                        else -> item.subtotal * 0.72
                    }
                    totalCogs += itemCost

                    if (isDigitalItem) {
                        saleDigitalFlag = true
                        val itemRevenue = item.subtotal + item.storeAdminFee
                        digitalSales += itemRevenue
                        val itemProvCost = if (item.providerAdminFee > 0.0) item.providerAdminFee else 0.0
                        val itemAgentComm = if (item.agentCommission > 0.0) item.agentCommission else 0.0
                        digitalGrossProfit += (itemRevenue - itemCost - itemProvCost + itemAgentComm)
                    } else {
                        physicalSales += item.subtotal
                        physicalGrossProfit += (item.subtotal - itemCost)
                    }
                }
                // Adjust sale-level fees if items didn't store them individually
                val itemsAdminSum = items.sumOf { it.storeAdminFee }
                val itemsProvSum = items.sumOf { it.providerAdminFee }
                val itemsAgentSum = items.sumOf { it.agentCommission }
                if (saleDigitalFlag) {
                    digitalSales += (sale.storeAdminFee - itemsAdminSum).coerceAtLeast(0.0)
                    digitalGrossProfit += (sale.storeAdminFee - itemsAdminSum) -
                        (sale.providerAdminFee - itemsProvSum) +
                        (sale.agentCommission - itemsAgentSum)
                }
                if (saleDiscounts > 0.0) {
                    val hasPhysicalItems = items.any { item ->
                        val p = productsById[item.productId]
                        !(item.storeAdminFee > 0.0 || (item.serviceNominal != null && item.serviceNominal > 0.0) || p?.productType == "DIGITAL")
                    }
                    when {
                        hasPhysicalItems && !saleDigitalFlag -> physicalGrossProfit -= saleDiscounts
                        !hasPhysicalItems && saleDigitalFlag -> digitalGrossProfit -= saleDiscounts
                        hasPhysicalItems && saleDigitalFlag -> {
                            val totalGross = (physicalGrossProfit.coerceAtLeast(0.0) + digitalGrossProfit.coerceAtLeast(0.0)).coerceAtLeast(1.0)
                            val physShare = (physicalGrossProfit.coerceAtLeast(0.0) / totalGross)
                            physicalGrossProfit -= saleDiscounts * physShare
                            digitalGrossProfit -= saleDiscounts * (1.0 - physShare)
                        }
                        else -> physicalGrossProfit -= saleDiscounts
                    }
                }
            } else {
                val estCost = sale.subtotal * 0.72
                totalCogs += estCost
                if (sale.storeAdminFee > 0.0 || sale.providerAdminFee > 0.0 || sale.agentCommission > 0.0) {
                    digitalSales += sale.finalAmount
                    digitalGrossProfit += (sale.storeAdminFee + sale.agentCommission - sale.providerAdminFee)
                } else {
                    physicalSales += sale.finalAmount
                    physicalGrossProfit += (sale.finalAmount - estCost)
                }
            }
        }

        val grossProfit = netSales - totalCogs - providerAdminCost
        val otherIncomes = periodIncomes.sumOf { it.amount }
        val operatingExpenses = periodExpenses.sumOf { it.amount }
        val netProfit = grossProfit + agentCommissionIncome + otherIncomes - operatingExpenses

        return RangeMetrics(
            transactionCount = activeSales.size,
            grossSales = grossSales,
            totalDiscounts = totalDiscounts,
            netSales = netSales,
            cogs = totalCogs,
            storeAdminRevenue = storeAdminRevenue,
            providerAdminCost = providerAdminCost,
            grossProfit = grossProfit,
            agentCommissionIncome = agentCommissionIncome,
            otherIncomes = otherIncomes,
            operatingExpenses = operatingExpenses,
            netProfit = netProfit,
            physicalSales = physicalSales,
            digitalSales = digitalSales,
            physicalNetProfit = physicalGrossProfit,
            digitalNetProfit = digitalGrossProfit
        )
    }

    private fun buildSubBuckets(
        window: ProfitLossWindow,
        sales: List<SaleEntity>,
        itemsBySaleId: Map<Long, List<SaleItemEntity>>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        productsById: Map<Long, ProductEntity>
    ): List<ProfitLossBucket> {
        val buckets = mutableListOf<ProfitLossBucket>()
        val dayShortFmt = SimpleDateFormat("EEE", idLocale)
        val dateShortFmt = SimpleDateFormat("dd MMM", idLocale)
        val monthNameFmt = SimpleDateFormat("MMMM", idLocale)

        when (window.period) {
            ProfitLossPeriod.HARIAN -> {
                // 7-day comparison ending on selected day
                for (delta in 0 downTo -6) {
                    val cal = Calendar.getInstance(idLocale).apply {
                        timeInMillis = window.startMs
                        add(Calendar.DAY_OF_YEAR, delta)
                        setStartOfDay(this)
                    }
                    val startMs = cal.timeInMillis
                    val endMs = (cal.clone() as Calendar).also { setEndOfDay(it) }.timeInMillis
                    val metrics = computeRangeMetrics(startMs, endMs, sales, itemsBySaleId, expenses, incomes, productsById)
                    val label = if (delta == 0 && window.offset == 0) "Hari Ini" else dayShortFmt.format(Date(startMs))
                    buckets.add(
                        ProfitLossBucket(
                            label = label,
                            subLabel = dateShortFmt.format(Date(startMs)),
                            startMs = startMs,
                            endMs = endMs,
                            transactionCount = metrics.transactionCount,
                            netSales = metrics.netSales,
                            totalCost = metrics.cogs + metrics.providerAdminCost + metrics.operatingExpenses,
                            netProfit = metrics.netProfit
                        )
                    )
                }
            }

            ProfitLossPeriod.MINGGUAN -> {
                val fullDayFmt = SimpleDateFormat("EEEE", idLocale)
                for (dayIdx in 0..6) {
                    val cal = Calendar.getInstance(idLocale).apply {
                        timeInMillis = window.startMs
                        add(Calendar.DAY_OF_YEAR, dayIdx)
                        setStartOfDay(this)
                    }
                    val startMs = cal.timeInMillis
                    val endMs = (cal.clone() as Calendar).also { setEndOfDay(it) }.timeInMillis
                    val metrics = computeRangeMetrics(startMs, endMs, sales, itemsBySaleId, expenses, incomes, productsById)
                    buckets.add(
                        ProfitLossBucket(
                            label = fullDayFmt.format(Date(startMs)),
                            subLabel = dateShortFmt.format(Date(startMs)),
                            startMs = startMs,
                            endMs = endMs,
                            transactionCount = metrics.transactionCount,
                            netSales = metrics.netSales,
                            totalCost = metrics.cogs + metrics.providerAdminCost + metrics.operatingExpenses,
                            netProfit = metrics.netProfit
                        )
                    )
                }
            }

            ProfitLossPeriod.BULANAN -> {
                val monthCal = Calendar.getInstance(idLocale).apply {
                    timeInMillis = window.startMs
                    setStartOfDay(this)
                }
                val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                var startDay = 1
                var weekIndex = 1
                while (startDay <= maxDay) {
                    val endDay = (startDay + 6).coerceAtMost(maxDay)
                    val sCal = (monthCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, startDay)
                        setStartOfDay(this)
                    }
                    val eCal = (monthCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, endDay)
                        setEndOfDay(this)
                    }
                    val startMs = sCal.timeInMillis
                    val endMs = eCal.timeInMillis
                    val metrics = computeRangeMetrics(startMs, endMs, sales, itemsBySaleId, expenses, incomes, productsById)
                    buckets.add(
                        ProfitLossBucket(
                            label = "Minggu $weekIndex",
                            subLabel = "${dateShortFmt.format(Date(startMs))} – ${dateShortFmt.format(Date(endMs))}",
                            startMs = startMs,
                            endMs = endMs,
                            transactionCount = metrics.transactionCount,
                            netSales = metrics.netSales,
                            totalCost = metrics.cogs + metrics.providerAdminCost + metrics.operatingExpenses,
                            netProfit = metrics.netProfit
                        )
                    )
                    startDay = endDay + 1
                    weekIndex++
                }
            }

            ProfitLossPeriod.TAHUNAN -> {
                val yearCal = Calendar.getInstance(idLocale).apply {
                    timeInMillis = window.startMs
                }
                val year = yearCal.get(Calendar.YEAR)
                for (m in Calendar.JANUARY..Calendar.DECEMBER) {
                    val sCal = Calendar.getInstance(idLocale).apply {
                        clear()
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                        setStartOfDay(this)
                    }
                    val eCal = (sCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        setEndOfDay(this)
                    }
                    val startMs = sCal.timeInMillis
                    val endMs = eCal.timeInMillis
                    val metrics = computeRangeMetrics(startMs, endMs, sales, itemsBySaleId, expenses, incomes, productsById)
                    buckets.add(
                        ProfitLossBucket(
                            label = monthNameFmt.format(Date(startMs)),
                            subLabel = "$year",
                            startMs = startMs,
                            endMs = endMs,
                            transactionCount = metrics.transactionCount,
                            netSales = metrics.netSales,
                            totalCost = metrics.cogs + metrics.providerAdminCost + metrics.operatingExpenses,
                            netProfit = metrics.netProfit
                        )
                    )
                }
            }
        }

        return buckets
    }

    private fun setStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun setEndOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
    }
}
