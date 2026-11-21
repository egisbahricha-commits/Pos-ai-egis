package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.*
import com.example.data.pricing.ProfitLossCalculator
import com.example.data.pricing.ProfitLossPeriod
import com.example.data.pricing.ProfitLossSummary
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.KasGoListRowItem
import com.example.ui.components.KasGoSearchBar
import com.example.ui.components.KasGoSectionHeader
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReportMeta(
    val id: Int,
    val section: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ReportsScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val debtPayments by viewModel.debtPayments.collectAsStateWithLifecycle()
    val shifts by viewModel.allShifts.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val allSaleItems by viewModel.allSaleItems.collectAsStateWithLifecycle()
    val productVariants by viewModel.productVariants.collectAsStateWithLifecycle()
    val productBatches by viewModel.productBatches.collectAsStateWithLifecycle()
    val cashMutations by viewModel.cashMutations.collectAsStateWithLifecycle()

    var selectedReportId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("Semua") }
    var pnlPeriod by remember { mutableStateOf(ProfitLossPeriod.HARIAN) }
    var pnlOffset by remember { mutableStateOf(0) }

    val pnlSummary = remember(pnlPeriod, pnlOffset, sales, allSaleItems, expenses, incomes, products) {
        ProfitLossCalculator.calculate(
            period = pnlPeriod,
            offset = pnlOffset,
            sales = sales,
            saleItems = allSaleItems,
            expenses = expenses,
            incomes = incomes,
            products = products
        )
    }

    val dangerReportColor = Rose500
    val reportDefinitions = remember(dangerReportColor) {
        listOf(
            // 1. Analisis Bisnis
            ReportMeta(1, "Analisis Bisnis", "Executive Summary", "Total penjualan, laba, pengeluaran & margin toko", Icons.Default.QueryStats, HeaderPurple),
            ReportMeta(2, "Analisis Bisnis", "Best & Worst Performers", "Produk paling laku & sepi, hari paling ramai", Icons.Default.TrendingUp, HeaderOrange),

            // 2. Laporan Penjualan
            ReportMeta(3, "Laporan Penjualan", "Penjualan Per Periode", "Grafik omset penjualan per hari & tanggal", Icons.Default.BarChart, HeaderGreen),
            ReportMeta(4, "Laporan Penjualan", "Penjualan Per Produk", "Produk terbanyak terjual & omset tertinggi", Icons.Default.ShoppingBag, HeaderBlue),
            ReportMeta(5, "Laporan Penjualan", "Penjualan Detail", "Setiap item terjual: harga, jumlah, keuntungan", Icons.Default.Receipt, HeaderTeal),
            ReportMeta(6, "Laporan Penjualan", "Penjualan Per Varian", "Performa varian & multi satuan (pcs, pack, dus)", Icons.Default.Category, HeaderPurple),
            ReportMeta(7, "Laporan Penjualan", "Penjualan Per Kategori", "Persentase & kontribusi omset per kategori", Icons.Default.Category, HeaderOrange),
            ReportMeta(8, "Laporan Penjualan", "Penjualan Per Pengguna", "Bandingkan performa & omset antar kasir", Icons.Default.People, HeaderBlue),
            ReportMeta(9, "Laporan Penjualan", "Riwayat Shift", "Catatan buka-tutup kasir & cocokkan uang laci", Icons.Default.EventNote, HeaderGreen),

            // 3. Laporan Keuangan
            ReportMeta(10, "Laporan Keuangan", "Laba Rugi (P&L)", "Cek laba rugi harian, mingguan, bulanan & tahunan", Icons.Default.AttachMoney, HeaderGreen),
            ReportMeta(11, "Laporan Keuangan", "Margin Produk", "Persentase keuntungan rata-rata tiap barang", Icons.Default.TrendingUp, HeaderPurple),
            ReportMeta(12, "Laporan Keuangan", "Profit Per Transaksi", "Keuntungan dari setiap nomor nota / struk", Icons.Default.PointOfSale, HeaderTeal),
            ReportMeta(13, "Laporan Keuangan", "Arus Kas", "Uang masuk vs keluar, surplus atau defisit kas", Icons.Default.AccountBalanceWallet, HeaderBlue),

            // 4. Laporan Pengeluaran
            ReportMeta(14, "Laporan Pengeluaran", "Pengeluaran Per Periode", "Grafik & rekap pengeluaran toko per hari", Icons.Default.CalendarMonth, HeaderRed),
            ReportMeta(15, "Laporan Pengeluaran", "Pengeluaran Per Kategori", "Porsi biaya listrik, sewa, gaji, dan operasional", Icons.Default.MoneyOff, HeaderRed),

            // 5. Laporan Inventory
            ReportMeta(16, "Laporan Inventory", "Status Stok", "Total nilai aset modal stok, menipis & habis", Icons.Default.Inventory, HeaderOrange),
            ReportMeta(17, "Laporan Inventory", "Batch Expired / Kedaluwarsa", "Monitoring barang kedaluwarsa & mendekati expired", Icons.Default.Warning, HeaderRed),

            // 6. Laporan Pelanggan
            ReportMeta(18, "Laporan Pelanggan", "Piutang Pelanggan", "Daftar pelanggan yang masih berutang / kasbon", Icons.Default.CreditCard, dangerReportColor),
            ReportMeta(19, "Laporan Pelanggan", "Riwayat Pembayaran Piutang", "Penerimaan cicilan kasbon filter tanggal", Icons.Default.History, HeaderGreen),
            ReportMeta(20, "Laporan Pelanggan", "Penjualan Per Pelanggan", "Pelanggan paling sering & paling banyak belanja", Icons.Default.Person, HeaderBlue),
            ReportMeta(21, "Laporan Pelanggan", "Customer Loyalty", "Segmentasi pelanggan VIP, Gold, Silver, Bronze", Icons.Default.WorkspacePremium, Amber500),

            // 7. Laporan Pembelian & Supplier
            ReportMeta(22, "Laporan Pembelian", "Pembelian Per Periode", "Grafik pembelian dari supplier kulakan per hari", Icons.Default.LocalShipping, HeaderTeal),
            ReportMeta(23, "Laporan Pembelian", "Hutang Supplier", "Supplier yang masih diutangi & total tagihan", Icons.Default.CreditCard, dangerReportColor),
            ReportMeta(24, "Laporan Pembelian", "Pembayaran Supplier", "Riwayat pelunasan hutang ke distributor", Icons.Default.CheckCircle, HeaderGreen)
        )
    }

    val sections = listOf("Semua", "Analisis Bisnis", "Laporan Penjualan", "Laporan Keuangan", "Laporan Pengeluaran", "Laporan Inventory", "Laporan Pelanggan", "Laporan Pembelian")

    // Handle Android Back if viewing detail
    BackHandler(enabled = selectedReportId != null) {
        selectedReportId = null
    }

    val currentMeta = reportDefinitions.find { it.id == selectedReportId }
    if (selectedReportId != null && currentMeta != null) {
        ReportDetailView(
            meta = currentMeta,
            sales = sales,
            allSaleItems = allSaleItems,
            purchases = purchases,
            expenses = expenses,
            incomes = incomes,
            products = products,
            productVariants = productVariants,
            productBatches = productBatches,
            customers = customers,
            suppliers = suppliers,
            debtPayments = debtPayments,
            shifts = shifts,
            cashAccounts = cashAccounts,
            cashMutations = cashMutations,
            pnlSummary = pnlSummary,
            pnlPeriod = pnlPeriod,
            pnlOffset = pnlOffset,
            onSelectPnlPeriod = { newPeriod ->
                pnlPeriod = newPeriod
                pnlOffset = 0
            },
            onChangePnlOffset = { newOffset -> pnlOffset = newOffset },
            onDrillDownPnl = { newPeriod, newOffset ->
                pnlPeriod = newPeriod
                pnlOffset = newOffset
            },
            onBack = { selectedReportId = null },
            onShare = { text ->
                viewModel.whatsAppCommandCenter.shareViaWhatsApp(context, text)
            }
        )
    } else {
        // Main 24 Reports Directory
        val filteredReports = remember(searchQuery, selectedSection) {
            reportDefinitions.filter { item ->
                (selectedSection == "Semua" || item.section == selectedSection) &&
                        (searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                item.subtitle.contains(searchQuery, ignoreCase = true))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCanvas)
                .testTag("reports_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ModuleLaporan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueryStats, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "24 Laporan Bisnis & Analitik",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    "Ringkasan operasional, keuangan, stok & pelanggan",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        KasGoSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Cari dari 24 laporan...",
                            onBarcodeScanClick = null
                        )
                    }
                }
            }

            // Category Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sections) { sec ->
                        DesignFilterChip(
                            label = sec,
                            selected = selectedSection == sec,
                            onClick = { selectedSection = sec }
                        )
                    }
                }
            }

            // Quick Cek Laba Rugi (Harian, Mingguan, Bulanan, Tahunan)
            if (searchQuery.isBlank() && (selectedSection == "Semua" || selectedSection == "Laporan Keuangan")) {
                item {
                    ProfitLossQuickCheckCard(
                        summary = pnlSummary,
                        selectedPeriod = pnlPeriod,
                        periodOffset = pnlOffset,
                        onSelectPeriod = { newPeriod ->
                            pnlPeriod = newPeriod
                            pnlOffset = 0
                        },
                        onChangeOffset = { newOffset -> pnlOffset = newOffset },
                        onOpenDetail = { selectedReportId = 10 }
                    )
                }
            }

            // Grouped Reports
            items(filteredReports) { report ->
                KasGoListRowItem(
                    title = report.title,
                    subtitle = report.subtitle,
                    icon = report.icon,
                    iconColor = report.color,
                    trailingBadge = report.section,
                    badgeColor = report.color,
                    onClick = { selectedReportId = report.id }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReportDetailView(
    meta: ReportMeta,
    sales: List<SaleEntity>,
    allSaleItems: List<SaleItemEntity>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity>,
    products: List<ProductEntity>,
    productVariants: List<ProductVariantEntity>,
    productBatches: List<ProductBatchEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    debtPayments: List<DebtPaymentEntity>,
    shifts: List<CashierShiftEntity>,
    cashAccounts: List<CashAccountEntity>,
    cashMutations: List<CashMutationEntity>,
    pnlSummary: ProfitLossSummary,
    pnlPeriod: ProfitLossPeriod,
    pnlOffset: Int,
    onSelectPnlPeriod: (ProfitLossPeriod) -> Unit,
    onChangePnlOffset: (Int) -> Unit,
    onDrillDownPnl: (ProfitLossPeriod, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onShare: (String) -> Unit
) {
    val activeSales = remember(sales) { sales.filter { it.status == "COMPLETED" } }
    val activeSaleIds = remember(activeSales) { activeSales.map { it.id }.toSet() }
    val activeSaleItems = remember(allSaleItems, activeSaleIds) { allSaleItems.filter { it.saleId in activeSaleIds } }
    val productsById = remember(products) { products.associateBy { it.id } }
    val physicalProducts = remember(products) {
        products.filter { !it.isUnlimitedStock && !it.hasNoStock && it.productType != "DIGITAL" }
    }

    val totalSales = activeSales.sumOf { it.finalAmount }
    val totalPurchases = purchases.filter { it.status == "DITERIMA" }.sumOf { it.finalAmount }
    val totalExpenses = expenses.sumOf { it.amount }
    val totalIncomes = incomes.sumOf { it.amount }

    // Actual HPP / COGS, provider fees, and agent commissions across completed sales
    val itemsBySaleId = remember(activeSaleItems) { activeSaleItems.groupBy { it.saleId } }
    val cogs = remember(activeSales, itemsBySaleId, productsById) {
        activeSales.sumOf { s ->
            val items = itemsBySaleId[s.id].orEmpty()
            items.sumOf { item ->
                val prod = productsById[item.productId]
                val isDigitalItem = prod?.productType == "DIGITAL" || (item.serviceNominal != null && item.serviceNominal > 0.0)
                if (isDigitalItem) {
                    (item.serviceNominal ?: item.subtotal).coerceAtLeast(0.0)
                } else {
                    val unitBuyCost = if (item.purchasePriceSnapshot > 0.0) item.purchasePriceSnapshot
                    else prod?.purchasePrice ?: 0.0
                    unitBuyCost * item.quantity
                }
            }
        }
    }
    val totalProviderCost = activeSales.sumOf { it.providerAdminFee }
    val totalAgentCommission = activeSales.sumOf { it.agentCommission }
    val grossProfit = totalSales - cogs - totalProviderCost
    val netProfit = grossProfit + totalAgentCommission + totalIncomes - totalExpenses
    val marginPercent = if (totalSales > 0.0) (netProfit / totalSales) * 100.0 else 0.0

    val stockAssetValue = physicalProducts.sumOf { it.stock.coerceAtLeast(0.0) * it.purchasePrice }
    val lowStockCount = physicalProducts.count { it.stock > 0.0 && it.stock <= it.minStock }
    val emptyStockCount = physicalProducts.count { it.stock <= 0.0 }

    val shareText = if (meta.id == 10) {
        buildProfitLossShareText(pnlSummary)
    } else {
        buildGeneralReportShareText(
            meta = meta,
            activeSalesCount = activeSales.size,
            totalSales = totalSales,
            cogs = cogs,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            marginPercent = marginPercent,
            stockAssetValue = stockAssetValue,
            lowStockCount = lowStockCount,
            emptyStockCount = emptyStockCount,
            totalPurchases = totalPurchases,
            expenses = expenses,
            customers = customers,
            suppliers = suppliers,
            debtPayments = debtPayments,
            purchases = purchases
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Slate800)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meta.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = meta.section,
                        style = MaterialTheme.typography.bodySmall.copy(color = meta.color, fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = { onShare(shareText) },
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 12.sp)
                }
            }
        }

        if (meta.id != 10) {
            item {
                ReportHeroSummaryCard(
                    meta = meta,
                    netProfit = netProfit,
                    cashAccounts = cashAccounts,
                    stockAssetValue = stockAssetValue,
                    productBatchesCount = productBatches.size,
                    customers = customers,
                    suppliers = suppliers,
                    debtPayments = debtPayments,
                    totalExpenses = totalExpenses,
                    totalPurchases = totalPurchases,
                    totalSales = totalSales
                )
            }
        }

        item {
            ReportDetailBodyCard(
                meta = meta,
                activeSales = activeSales,
                activeSaleItems = activeSaleItems,
                allSaleItems = allSaleItems,
                itemsBySaleId = itemsBySaleId,
                products = products,
                physicalProducts = physicalProducts,
                productsById = productsById,
                productVariants = productVariants,
                productBatches = productBatches,
                purchases = purchases,
                expenses = expenses,
                customers = customers,
                suppliers = suppliers,
                debtPayments = debtPayments,
                shifts = shifts,
                cashAccounts = cashAccounts,
                cashMutations = cashMutations,
                totalSales = totalSales,
                cogs = cogs,
                totalProviderCost = totalProviderCost,
                totalAgentCommission = totalAgentCommission,
                totalExpenses = totalExpenses,
                totalIncomes = totalIncomes,
                netProfit = netProfit,
                marginPercent = marginPercent,
                stockAssetValue = stockAssetValue,
                lowStockCount = lowStockCount,
                emptyStockCount = emptyStockCount,
                pnlSummary = pnlSummary,
                pnlPeriod = pnlPeriod,
                pnlOffset = pnlOffset,
                onSelectPnlPeriod = onSelectPnlPeriod,
                onChangePnlOffset = onChangePnlOffset,
                onDrillDownPnl = onDrillDownPnl
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun buildGeneralReportShareText(
    meta: ReportMeta,
    activeSalesCount: Int,
    totalSales: Double,
    cogs: Double,
    grossProfit: Double,
    totalExpenses: Double,
    netProfit: Double,
    marginPercent: Double,
    stockAssetValue: Double,
    lowStockCount: Int,
    emptyStockCount: Int,
    totalPurchases: Double,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    debtPayments: List<DebtPaymentEntity>,
    purchases: List<PurchaseEntity>
): String = buildString {
    append("📊 *LAPORAN GERAI UNA: ${meta.title.uppercase()}*\n")
    append("Kategori: ${meta.section}\n")
    append("Waktu Cetak: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())}\n")
    append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    when (meta.id) {
        1 -> {
            append("• Total Penjualan: Rp ${formatRupiah(totalSales)} ($activeSalesCount transaksi)\n")
            append("• HPP / Modal Pokok: Rp ${formatRupiah(cogs)}\n")
            append("• Laba Kotor: Rp ${formatRupiah(grossProfit)}\n")
            append("• Biaya Operasional: Rp ${formatRupiah(totalExpenses)}\n")
            append("• *LABA BERSIH: Rp ${formatRupiah(netProfit)} (Margin ${String.format(Locale.US, "%.1f", marginPercent)}%)*\n")
        }
        14, 15 -> {
            append("• Total Pengeluaran: Rp ${formatRupiah(totalExpenses)} (${expenses.size} catatan)\n")
            expenses.groupBy { it.category }.entries
                .sortedByDescending { e -> e.value.sumOf { it.amount } }
                .take(6)
                .forEach { (cat, list) ->
                    append("- $cat: Rp ${formatRupiah(list.sumOf { it.amount })}\n")
                }
        }
        16 -> {
            append("• Total Nilai Aset Stok Fisik: Rp ${formatRupiah(stockAssetValue)}\n")
            append("• Produk Stok Menipis: $lowStockCount barang\n")
            append("• Produk Habis / Minus: $emptyStockCount barang\n")
        }
        18 -> {
            val totalReceivable = customers.sumOf { it.totalReceivable }
            append("• Total Piutang Aktif: Rp ${formatRupiah(totalReceivable)}\n")
            customers.filter { it.totalReceivable > 0 }.forEach { c ->
                append("- ${c.name}: Rp ${formatRupiah(c.totalReceivable)}\n")
            }
        }
        19 -> {
            val recPay = debtPayments.filter { it.type == "PIUTANG_PELANGGAN" }
            append("• Total Cicilan Piutang Diterima: Rp ${formatRupiah(recPay.sumOf { it.amount })}\n")
            recPay.take(5).forEach { p ->
                append("- ${p.partyName}: Rp ${formatRupiah(p.amount)}\n")
            }
        }
        22 -> {
            append("• Total Pembelian Diterima: Rp ${formatRupiah(totalPurchases)}\n")
            purchases.filter { it.status == "DITERIMA" }.take(5).forEach { p ->
                append("- ${p.invoiceNumber} (${p.supplierName}): Rp ${formatRupiah(p.finalAmount)}\n")
            }
        }
        23 -> {
            val totalDebt = suppliers.sumOf { it.totalDebt }
            append("• Total Hutang ke Supplier: Rp ${formatRupiah(totalDebt)}\n")
            suppliers.filter { it.totalDebt > 0 }.forEach { s ->
                append("- ${s.name}: Rp ${formatRupiah(s.totalDebt)}\n")
            }
        }
        24 -> {
            val supPay = debtPayments.filter { it.type == "UTANG_PEMASOK" }
            append("• Total Pelunasan ke Supplier: Rp ${formatRupiah(supPay.sumOf { it.amount })}\n")
            supPay.take(5).forEach { p ->
                append("- ${p.partyName}: Rp ${formatRupiah(p.amount)}\n")
            }
        }
        else -> {
            append("• Total Transaksi Selesai: $activeSalesCount\n")
            append("• Total Omset: Rp ${formatRupiah(totalSales)}\n")
            append("• Laba Bersih: Rp ${formatRupiah(netProfit)}\n")
        }
    }
    append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    append("Laporan Resmi Kasir POS")
}

@Composable
private fun ReportHeroSummaryCard(
    meta: ReportMeta,
    netProfit: Double,
    cashAccounts: List<CashAccountEntity>,
    stockAssetValue: Double,
    productBatchesCount: Int,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    debtPayments: List<DebtPaymentEntity>,
    totalExpenses: Double,
    totalPurchases: Double,
    totalSales: Double
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = meta.color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = meta.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.85f))
            )
            Spacer(modifier = Modifier.height(6.dp))

            val primaryMetric = when (meta.id) {
                1 -> "Rp ${formatRupiah(netProfit)}"
                13 -> "Rp ${formatRupiah(cashAccounts.sumOf { it.balance })}"
                16 -> "Rp ${formatRupiah(stockAssetValue)}"
                17 -> "$productBatchesCount Batch"
                18 -> "Rp ${formatRupiah(customers.sumOf { it.totalReceivable })}"
                19 -> "Rp ${formatRupiah(debtPayments.filter { it.type == "PIUTANG_PELANGGAN" }.sumOf { it.amount })}"
                21 -> "${customers.sumOf { it.loyaltyPoints }} Poin"
                23 -> "Rp ${formatRupiah(suppliers.sumOf { it.totalDebt })}"
                24 -> "Rp ${formatRupiah(debtPayments.filter { it.type == "UTANG_PEMASOK" }.sumOf { it.amount })}"
                14, 15 -> "Rp ${formatRupiah(totalExpenses)}"
                22 -> "Rp ${formatRupiah(totalPurchases)}"
                else -> "Rp ${formatRupiah(totalSales)}"
            }

            Text(
                text = primaryMetric,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Data sinkron otomatis dari database lokal Gerai Una POS",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun ReportDetailBodyCard(
    meta: ReportMeta,
    activeSales: List<SaleEntity>,
    activeSaleItems: List<SaleItemEntity>,
    allSaleItems: List<SaleItemEntity>,
    itemsBySaleId: Map<Long, List<SaleItemEntity>>,
    products: List<ProductEntity>,
    physicalProducts: List<ProductEntity>,
    productsById: Map<Long, ProductEntity>,
    productVariants: List<ProductVariantEntity>,
    productBatches: List<ProductBatchEntity>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    debtPayments: List<DebtPaymentEntity>,
    shifts: List<CashierShiftEntity>,
    cashAccounts: List<CashAccountEntity>,
    cashMutations: List<CashMutationEntity>,
    totalSales: Double,
    cogs: Double,
    totalProviderCost: Double,
    totalAgentCommission: Double,
    totalExpenses: Double,
    totalIncomes: Double,
    netProfit: Double,
    marginPercent: Double,
    stockAssetValue: Double,
    lowStockCount: Int,
    emptyStockCount: Int,
    pnlSummary: ProfitLossSummary,
    pnlPeriod: ProfitLossPeriod,
    pnlOffset: Int,
    onSelectPnlPeriod: (ProfitLossPeriod) -> Unit,
    onChangePnlOffset: (Int) -> Unit,
    onDrillDownPnl: (ProfitLossPeriod, Int) -> Unit
) {
    when (meta.id) {
        10 -> ReportPnlCard(
            pnlSummary = pnlSummary,
            pnlPeriod = pnlPeriod,
            pnlOffset = pnlOffset,
            onSelectPnlPeriod = onSelectPnlPeriod,
            onChangePnlOffset = onChangePnlOffset,
            onDrillDownPnl = onDrillDownPnl
        )
        1 -> ReportExecutiveSummaryCard(
            activeSalesCount = activeSales.size,
            totalSales = totalSales,
            cogs = cogs,
            totalProviderCost = totalProviderCost,
            totalAgentCommission = totalAgentCommission,
            totalExpenses = totalExpenses,
            totalIncomes = totalIncomes,
            netProfit = netProfit,
            marginPercent = marginPercent
        )
        2 -> ReportBestWorstPerformersCard(activeSales, activeSaleItems, products)
        3, 4, 5, 6, 7, 8, 9 -> ReportSalesSectionCard(
            reportId = meta.id,
            activeSales = activeSales,
            activeSaleItems = activeSaleItems,
            allSaleItems = allSaleItems,
            productsById = productsById,
            productVariantsCount = productVariants.size,
            shifts = shifts,
            totalSales = totalSales
        )
        11, 12, 13 -> ReportFinanceSectionCard(
            reportId = meta.id,
            activeSales = activeSales,
            itemsBySaleId = itemsBySaleId,
            physicalProducts = physicalProducts,
            productsById = productsById,
            cashAccounts = cashAccounts,
            cashMutations = cashMutations
        )
        14, 15, 16, 17 -> ReportExpenseAndInventoryCard(
            reportId = meta.id,
            expenses = expenses,
            products = products,
            physicalProductsCount = physicalProducts.size,
            productBatches = productBatches,
            stockAssetValue = stockAssetValue,
            lowStockCount = lowStockCount,
            emptyStockCount = emptyStockCount
        )
        18, 19, 20, 21, 22, 23, 24 -> ReportPartnerSectionCard(
            reportId = meta.id,
            activeSales = activeSales,
            customers = customers,
            suppliers = suppliers,
            debtPayments = debtPayments,
            purchases = purchases
        )
        else -> ReportFallbackCard(metaTitle = meta.title, activeSales = activeSales)
    }
}

@Composable
private fun ReportPnlCard(
    pnlSummary: ProfitLossSummary,
    pnlPeriod: ProfitLossPeriod,
    pnlOffset: Int,
    onSelectPnlPeriod: (ProfitLossPeriod) -> Unit,
    onChangePnlOffset: (Int) -> Unit,
    onDrillDownPnl: (ProfitLossPeriod, Int) -> Unit
) {
    val idLocale = remember { Locale("id", "ID") }
    ProfitLossDetailContent(
        summary = pnlSummary,
        selectedPeriod = pnlPeriod,
        periodOffset = pnlOffset,
        onSelectPeriod = onSelectPnlPeriod,
        onChangeOffset = onChangePnlOffset,
        onBucketClick = { bucket ->
            val nowCal = Calendar.getInstance(idLocale).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val bCal = Calendar.getInstance(idLocale).apply {
                firstDayOfWeek = Calendar.MONDAY
                timeInMillis = bucket.startMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            when (pnlPeriod) {
                ProfitLossPeriod.TAHUNAN -> {
                    val monthsDiff = (bCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)) * 12 +
                        (bCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH))
                    onDrillDownPnl(ProfitLossPeriod.BULANAN, monthsDiff.coerceAtMost(0))
                }
                ProfitLossPeriod.BULANAN -> {
                    nowCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    bCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    val weeksDiff = ((bCal.timeInMillis - nowCal.timeInMillis) / 604_800_000L).toInt()
                    onDrillDownPnl(ProfitLossPeriod.MINGGUAN, weeksDiff.coerceAtMost(0))
                }
                ProfitLossPeriod.MINGGUAN, ProfitLossPeriod.HARIAN -> {
                    val daysDiff = ((bCal.timeInMillis - nowCal.timeInMillis) / 86_400_000L).toInt()
                    onDrillDownPnl(ProfitLossPeriod.HARIAN, daysDiff.coerceAtMost(0))
                }
            }
        }
    )
}

@Composable
private fun ReportExecutiveSummaryCard(
    activeSalesCount: Int,
    totalSales: Double,
    cogs: Double,
    totalProviderCost: Double,
    totalAgentCommission: Double,
    totalExpenses: Double,
    totalIncomes: Double,
    netProfit: Double,
    marginPercent: Double
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Indikator Utama Bisnis ($activeSalesCount Transaksi)", fontWeight = FontWeight.Bold, color = Slate900)
            DetailRow("Total Penjualan Bersih", "Rp ${formatRupiah(totalSales)}", HeaderGreen)
            DetailRow("HPP / Modal Pokok", "-Rp ${formatRupiah(cogs)}", Slate600)
            if (totalProviderCost > 0) {
                DetailRow("Admin Penyedia / Bank", "-Rp ${formatRupiah(totalProviderCost)}", Rose500)
            }
            if (totalAgentCommission > 0) {
                DetailRow("Komisi / Fee Agen", "+Rp ${formatRupiah(totalAgentCommission)}", HeaderTeal)
            }
            DetailRow("Beban Pengeluaran Operasional", "-Rp ${formatRupiah(totalExpenses)}", Rose500)
            DetailRow("Pemasukan Jasa & Komisi", "+Rp ${formatRupiah(totalIncomes)}", Sapphire800)
            HorizontalDivider(color = Slate200)
            DetailRow("Laba Bersih (Net Profit)", "${if (netProfit < 0) "-" else ""}Rp ${formatRupiah(kotlin.math.abs(netProfit))}", if (netProfit >= 0) HeaderPurple else Rose500, isBold = true)
            DetailRow("Margin Keuntungan", "${String.format(Locale.US, "%.1f", marginPercent)}%", HeaderPurple, isBold = true)
        }
    }
}

@Composable
private fun ReportBestWorstPerformersCard(
    activeSales: List<SaleEntity>,
    activeSaleItems: List<SaleItemEntity>,
    products: List<ProductEntity>
) {
    val salesByProduct = activeSaleItems.groupBy { it.productId }
        .mapValues { (_, items) -> items.sumOf { it.subtotal } to items.sumOf { it.quantity } }
    val rankedProducts = products.map { p ->
        val (rev, qty) = salesByProduct[p.id] ?: (0.0 to 0.0)
        Triple(p, rev, qty)
    }
    val bestEntry = rankedProducts.filter { it.second > 0.0 }.maxByOrNull { it.second }
    val worstEntry = rankedProducts.minByOrNull { it.second }
    val idLocale = Locale("id", "ID")
    val dayNames = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val dayCounts = activeSales.groupBy { s ->
        val cal = Calendar.getInstance(idLocale).apply { timeInMillis = s.date }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }
    }.mapValues { it.value.size to it.value.sumOf { s -> s.finalAmount } }
    val busiestDay = dayCounts.maxByOrNull { it.value.first }

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Performa Penjualan Produk & Hari", fontWeight = FontWeight.Bold, color = Slate900)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = Emerald50, border = BorderStroke(1.dp, Emerald100), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🏆 Produk Terlaris", fontSize = 11.sp, color = Emerald700, fontWeight = FontWeight.Bold)
                        Text(bestEntry?.first?.name ?: "Belum terjual", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (bestEntry != null) "Rp ${formatRupiah(bestEntry.second)} (${bestEntry.third.toInt()}x)" else "Belum ada penjualan",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = Rose500.copy(alpha = 0.08f), border = BorderStroke(1.dp, Rose500.copy(alpha = 0.2f)), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ Produk Paling Sepi", fontSize = 11.sp, color = Rose500, fontWeight = FontWeight.Bold)
                        Text(worstEntry?.first?.name ?: "Belum ada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (worstEntry != null && worstEntry.second > 0) "Rp ${formatRupiah(worstEntry.second)}" else "Belum terjual (0x)",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }
            }
            HorizontalDivider(color = Slate200)
            if (busiestDay == null) {
                Text("Analisis Hari Ramai: Belum ada transaksi selesai untuk dianalisis.", fontSize = 12.sp, color = Slate600)
            } else {
                Text(
                    "Hari Paling Ramai: ${busiestDay.key} (${busiestDay.value.first} transaksi • Rp ${formatRupiah(busiestDay.value.second)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800
                )
                dayNames.forEach { d ->
                    val stats = dayCounts[d] ?: (0 to 0.0)
                    DetailRow(d, "${stats.first} trx • Rp ${formatRupiah(stats.second)}", if (d == busiestDay.key) Emerald700 else Slate600)
                }
            }
        }
    }
}

@Composable
private fun ReportSalesSectionCard(
    reportId: Int,
    activeSales: List<SaleEntity>,
    activeSaleItems: List<SaleItemEntity>,
    allSaleItems: List<SaleItemEntity>,
    productsById: Map<Long, ProductEntity>,
    productVariantsCount: Int,
    shifts: List<CashierShiftEntity>,
    totalSales: Double
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (reportId) {
                3 -> {
                    val fmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    val byDay = activeSales.groupBy { fmt.format(Date(it.date)) }
                    Text("Omset Penjualan Per Tanggal", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byDay.isEmpty()) Text("Belum ada transaksi selesai.", fontSize = 12.sp, color = Slate600)
                    else byDay.entries.take(20).forEach { (dateLabel, list) ->
                        DetailRow("$dateLabel (${list.size} trx)", "Rp ${formatRupiah(list.sumOf { it.finalAmount })}", HeaderGreen)
                    }
                }
                4 -> {
                    val byProduct = activeSaleItems.groupBy { it.productName }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.subtotal } }
                    Text("Peringkat Penjualan Produk", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byProduct.isEmpty()) Text("Belum ada produk terjual.", fontSize = 12.sp, color = Slate600)
                    else byProduct.take(20).forEach { (name, rows) ->
                        DetailRow(name, "${rows.sumOf { it.quantity }.toInt()}x • Rp ${formatRupiah(rows.sumOf { it.subtotal })}", HeaderBlue)
                    }
                }
                5 -> {
                    Text("Detail Item Terjual (${activeSaleItems.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (activeSaleItems.isEmpty()) Text("Belum ada item terjual.", fontSize = 12.sp, color = Slate600)
                    else activeSaleItems.take(25).forEach { itm ->
                        DetailRow("${itm.productName} (${itm.quantity} ${itm.unit})", "Rp ${formatRupiah(itm.subtotal)}", HeaderTeal)
                    }
                }
                6 -> {
                    val grouped = allSaleItems.groupBy { item ->
                        if (item.variantName.isNotBlank()) "${item.productName} • ${item.variantName}" else "${item.productName} (${item.unit})"
                    }
                    Text("Penjualan Per Varian & Satuan ($productVariantsCount varian terdaftar)", fontWeight = FontWeight.Bold, color = Slate900)
                    if (grouped.isEmpty()) Text("Belum ada item terjual.", fontSize = 12.sp, color = Slate600)
                    else grouped.entries.sortedByDescending { e -> e.value.sumOf { it.subtotal } }.take(15).forEach { (label, rows) ->
                        DetailRow(label, "${rows.sumOf { it.quantity }}x • Rp ${formatRupiah(rows.sumOf { it.subtotal })}", HeaderPurple)
                    }
                }
                7 -> {
                    val byCat = activeSaleItems.groupBy { productsById[it.productId]?.category ?: "Lainnya" }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.subtotal } }
                    Text("Omset Per Kategori", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byCat.isEmpty()) Text("Belum ada penjualan per kategori.", fontSize = 12.sp, color = Slate600)
                    else byCat.forEach { (cat, rows) ->
                        val sum = rows.sumOf { it.subtotal }
                        val pct = if (totalSales > 0) (sum / totalSales) * 100 else 0.0
                        DetailRow("$cat (${String.format(Locale.US, "%.1f", pct)}%)", "Rp ${formatRupiah(sum)}", HeaderOrange)
                    }
                }
                8 -> {
                    val byCashier = activeSales.groupBy { it.cashierName.ifBlank { "Kasir" } }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.finalAmount } }
                    Text("Performa Omset Per Kasir", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byCashier.isEmpty()) Text("Belum ada transaksi kasir.", fontSize = 12.sp, color = Slate600)
                    else byCashier.forEach { (cashier, list) ->
                        DetailRow("$cashier (${list.size} trx)", "Rp ${formatRupiah(list.sumOf { it.finalAmount })}", HeaderBlue)
                    }
                }
                else -> {
                    Text("Daftar Shift Kasir Tercatat (${shifts.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (shifts.isEmpty()) {
                        Text("Belum ada shift yang dicatat. Buka shift di menu kasir.", fontSize = 12.sp, color = Slate600)
                    } else {
                        shifts.forEach { s ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Slate50, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(s.cashierName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(s.status, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (s.status == "OPEN") HeaderGreen else Slate600)
                                    }
                                    Text("Kas Awal: Rp ${formatRupiah(s.startingCash)} • Penjualan: Rp ${formatRupiah(s.totalCashSales)}", fontSize = 11.sp, color = Slate600)
                                    s.actualClosingCash?.let { closing ->
                                        val diff = s.difference ?: 0.0
                                        Text("Uang Fisik: Rp ${formatRupiah(closing)} • Selisih: Rp ${formatRupiah(diff)}", fontSize = 11.sp, color = if (diff == 0.0) HeaderGreen else Rose500)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFinanceSectionCard(
    reportId: Int,
    activeSales: List<SaleEntity>,
    itemsBySaleId: Map<Long, List<SaleItemEntity>>,
    physicalProducts: List<ProductEntity>,
    productsById: Map<Long, ProductEntity>,
    cashAccounts: List<CashAccountEntity>,
    cashMutations: List<CashMutationEntity>
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (reportId) {
                11 -> {
                    val margins = physicalProducts.filter { it.sellingPrice > 0 }
                        .sortedByDescending { (it.sellingPrice - it.purchasePrice) / it.sellingPrice }
                    Text("Margin Keuntungan Produk Fisik", fontWeight = FontWeight.Bold, color = Slate900)
                    if (margins.isEmpty()) Text("Belum ada produk dengan harga jual.", fontSize = 12.sp, color = Slate600)
                    else margins.take(20).forEach { p ->
                        val diff = p.sellingPrice - p.purchasePrice
                        val pct = (diff / p.sellingPrice) * 100.0
                        DetailRow(p.name, "+Rp ${formatRupiah(diff)} (${String.format(Locale.US, "%.1f", pct)}%)", if (diff >= 0) HeaderPurple else Rose500)
                    }
                }
                12 -> {
                    Text("Estimasi Laba Per Transaksi (${activeSales.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (activeSales.isEmpty()) Text("Belum ada transaksi selesai.", fontSize = 12.sp, color = Slate600)
                    else activeSales.take(20).forEach { s ->
                        val sCogs = itemsBySaleId[s.id].orEmpty().sumOf { item ->
                            val prod = productsById[item.productId]
                            val isDigital = prod?.productType == "DIGITAL" || (item.serviceNominal != null && item.serviceNominal > 0.0)
                            if (isDigital) (item.serviceNominal ?: item.subtotal).coerceAtLeast(0.0)
                            else (if (item.purchasePriceSnapshot > 0) item.purchasePriceSnapshot else prod?.purchasePrice ?: 0.0) * item.quantity
                        }
                        val sProfit = s.finalAmount - sCogs - s.providerAdminFee + s.agentCommission
                        DetailRow(
                            "${s.invoiceNumber} • ${s.customerName}",
                            "${if (sProfit < 0) "-" else "+"}Rp ${formatRupiah(kotlin.math.abs(sProfit))}",
                            if (sProfit >= 0) HeaderTeal else Rose500
                        )
                    }
                }
                else -> {
                    val inflow = cashMutations.filter { it.amount > 0 }.sumOf { it.amount }
                    val outflow = cashMutations.filter { it.amount < 0 }.sumOf { -it.amount }
                    val netFlow = inflow - outflow
                    Text("Arus Kas Aktual (Mutasi Kas)", fontWeight = FontWeight.Bold, color = Slate900)
                    DetailRow("Total Kas Masuk", "+Rp ${formatRupiah(inflow)}", HeaderGreen, isBold = true)
                    DetailRow("Total Kas Keluar", "-Rp ${formatRupiah(outflow)}", Rose500, isBold = true)
                    HorizontalDivider(color = Slate200)
                    DetailRow("Surplus / Defisit", "Rp ${formatRupiah(netFlow)}", if (netFlow >= 0) HeaderGreen else Rose500, isBold = true)
                    DetailRow("Total Saldo Semua Akun Kas", "Rp ${formatRupiah(cashAccounts.sumOf { it.balance })}", HeaderBlue, isBold = true)
                }
            }
        }
    }
}

@Composable
private fun ReportExpenseAndInventoryCard(
    reportId: Int,
    expenses: List<ExpenseEntity>,
    products: List<ProductEntity>,
    physicalProductsCount: Int,
    productBatches: List<ProductBatchEntity>,
    stockAssetValue: Double,
    lowStockCount: Int,
    emptyStockCount: Int
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (reportId) {
                14 -> {
                    val fmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    val byDay = expenses.groupBy { fmt.format(Date(it.date)) }
                    Text("Rekap Pengeluaran Per Tanggal", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byDay.isEmpty()) Text("Belum ada catatan pengeluaran.", fontSize = 12.sp, color = Slate600)
                    else byDay.entries.forEach { (d, list) ->
                        DetailRow("$d (${list.size} catatan)", "Rp ${formatRupiah(list.sumOf { it.amount })}", Rose500)
                    }
                }
                15 -> {
                    val byCat = expenses.groupBy { it.category }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.amount } }
                    Text("Porsi Pengeluaran Per Kategori", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byCat.isEmpty()) Text("Belum ada catatan pengeluaran.", fontSize = 12.sp, color = Slate600)
                    else byCat.forEach { (cat, list) ->
                        DetailRow("$cat (${list.size}x)", "Rp ${formatRupiah(list.sumOf { it.amount })}", Rose500)
                    }
                }
                16 -> {
                    Text("Ringkasan Kondisi Stok Barang Fisik", fontWeight = FontWeight.Bold, color = Slate900)
                    DetailRow("Total Produk Fisik Terpantau", "$physicalProductsCount item", Slate900)
                    DetailRow("Stok Menipis (<= Min)", "$lowStockCount item", Amber500, isBold = true)
                    DetailRow("Stok Habis / Minus (<= 0)", "$emptyStockCount item", Rose500, isBold = true)
                    DetailRow("Total Nilai Aset Modal", "Rp ${formatRupiah(stockAssetValue)}", HeaderGreen, isBold = true)
                }
                else -> {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    Text("Monitoring Batch Expired (${productBatches.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (productBatches.isEmpty()) {
                        Text("Belum ada data batch kedaluwarsa.", fontSize = 12.sp, color = Slate600)
                    } else {
                        productBatches.sortedBy { it.expiryDateStr }.forEach { b ->
                            val pname = products.find { it.id == b.productId }?.name ?: "Produk #${b.productId}"
                            val isExp = b.expiryDateStr <= todayStr
                            DetailRow(
                                "$pname (${b.batchCode})",
                                "Exp ${b.expiryDateStr} • ${b.qty.toInt()} pcs",
                                if (isExp) Rose500 else Amber500,
                                isBold = isExp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportPartnerSectionCard(
    reportId: Int,
    activeSales: List<SaleEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    debtPayments: List<DebtPaymentEntity>,
    purchases: List<PurchaseEntity>
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (reportId) {
                18 -> {
                    val debtCustomers = customers.filter { it.totalReceivable > 0 }
                    Text("Daftar Pelanggan Kasbon / Piutang (${debtCustomers.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (debtCustomers.isEmpty()) {
                        Text("Tidak ada piutang pelanggan yang tertunggak. Semua lunas!", fontSize = 12.sp, color = HeaderGreen)
                    } else {
                        debtCustomers.forEach { c ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Rp ${formatRupiah(c.totalReceivable)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Rose500)
                            }
                        }
                    }
                }
                19 -> {
                    val rows = debtPayments.filter { it.type == "PIUTANG_PELANGGAN" }
                    Text("Riwayat Pembayaran Piutang (${rows.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (rows.isEmpty()) Text("Belum ada pembayaran piutang tercatat.", fontSize = 12.sp, color = Slate600)
                    else rows.forEach { r ->
                        val d = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(r.date))
                        DetailRow("${r.partyName} ($d)", "+Rp ${formatRupiah(r.amount)}", HeaderGreen)
                    }
                }
                20 -> {
                    val byCust = activeSales.groupBy { it.customerName.ifBlank { "Pelanggan Umum" } }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.finalAmount } }
                    Text("Belanja Per Pelanggan", fontWeight = FontWeight.Bold, color = Slate900)
                    if (byCust.isEmpty()) Text("Belum ada transaksi pelanggan.", fontSize = 12.sp, color = Slate600)
                    else byCust.forEach { (name, list) ->
                        DetailRow("$name (${list.size}x)", "Rp ${formatRupiah(list.sumOf { it.finalAmount })}", HeaderBlue)
                    }
                }
                21 -> {
                    Text("Peringkat Loyalitas & Deposit Pelanggan", fontWeight = FontWeight.Bold, color = Slate900)
                    customers.sortedByDescending { it.loyaltyPoints }.forEach { c ->
                        val tier = when {
                            c.loyaltyPoints >= 100 -> "VIP"
                            c.loyaltyPoints >= 50 -> "Gold"
                            c.loyaltyPoints >= 20 -> "Silver"
                            else -> "Bronze"
                        }
                        DetailRow("${c.name} [$tier]", "${c.loyaltyPoints} poin • Dep Rp ${formatRupiah(c.depositBalance)}", HeaderBlue)
                    }
                }
                22 -> {
                    val received = purchases.filter { it.status == "DITERIMA" }
                    Text("Pembelian Supplier Diterima (${received.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (received.isEmpty()) Text("Belum ada pembelian yang diterima.", fontSize = 12.sp, color = Slate600)
                    else received.forEach { p ->
                        DetailRow("${p.invoiceNumber} • ${p.supplierName}", "Rp ${formatRupiah(p.finalAmount)}", HeaderTeal)
                    }
                }
                23 -> {
                    val debtSuppliers = suppliers.filter { it.totalDebt > 0 }
                    Text("Daftar Hutang Supplier (${debtSuppliers.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (debtSuppliers.isEmpty()) Text("Tidak ada hutang ke supplier. Semua lunas!", fontSize = 12.sp, color = HeaderGreen)
                    else debtSuppliers.forEach { s ->
                        DetailRow(s.name, "Rp ${formatRupiah(s.totalDebt)}", Rose500, isBold = true)
                    }
                }
                else -> {
                    val rows = debtPayments.filter { it.type == "UTANG_PEMASOK" }
                    Text("Riwayat Pelunasan Supplier (${rows.size})", fontWeight = FontWeight.Bold, color = Slate900)
                    if (rows.isEmpty()) Text("Belum ada pembayaran utang supplier.", fontSize = 12.sp, color = Slate600)
                    else rows.forEach { r ->
                        val d = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(r.date))
                        DetailRow("${r.partyName} ($d)", "Rp ${formatRupiah(r.amount)}", HeaderGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFallbackCard(metaTitle: String, activeSales: List<SaleEntity>) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderDefault)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data Rincian $metaTitle", fontWeight = FontWeight.Bold, color = Slate900)
            if (activeSales.isEmpty()) Text("Belum ada transaksi.", fontSize = 12.sp, color = Slate600)
            else activeSales.take(10).forEach { s ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(s.invoiceNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(s.date)), fontSize = 10.sp, color = Slate400)
                    }
                    Text("Rp ${formatRupiah(s.finalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HeaderBlue)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Slate900, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = valueColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium))
    }
}
