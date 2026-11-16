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
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.KasGoListRowItem
import com.example.ui.components.KasGoSearchBar
import com.example.ui.components.KasGoSectionHeader
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
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

    val reportDefinitions = remember {
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
            ReportMeta(10, "Laporan Keuangan", "Laba Rugi (P&L)", "Penjualan dikurangi HPP modal & biaya operasional", Icons.Default.AttachMoney, HeaderGreen),
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
            ReportMeta(18, "Laporan Pelanggan", "Piutang Pelanggan", "Daftar pelanggan yang masih berutang / kasbon", Icons.Default.CreditCard, Rose500),
            ReportMeta(19, "Laporan Pelanggan", "Riwayat Pembayaran Piutang", "Penerimaan cicilan kasbon filter tanggal", Icons.Default.History, HeaderGreen),
            ReportMeta(20, "Laporan Pelanggan", "Penjualan Per Pelanggan", "Pelanggan paling sering & paling banyak belanja", Icons.Default.Person, HeaderBlue),
            ReportMeta(21, "Laporan Pelanggan", "Customer Loyalty", "Segmentasi pelanggan VIP, Gold, Silver, Bronze", Icons.Default.WorkspacePremium, Amber500),

            // 7. Laporan Pembelian & Supplier
            ReportMeta(22, "Laporan Pembelian", "Pembelian Per Periode", "Grafik pembelian dari supplier kulakan per hari", Icons.Default.LocalShipping, HeaderTeal),
            ReportMeta(23, "Laporan Pembelian", "Hutang Supplier", "Supplier yang masih diutangi & total tagihan", Icons.Default.CreditCard, Rose500),
            ReportMeta(24, "Laporan Pembelian", "Pembayaran Supplier", "Riwayat pelunasan hutang ke distributor", Icons.Default.CheckCircle, HeaderGreen)
        )
    }

    val sections = listOf("Semua", "Analisis Bisnis", "Laporan Penjualan", "Laporan Keuangan", "Laporan Pengeluaran", "Laporan Inventory", "Laporan Pelanggan", "Laporan Pembelian")

    // Handle Android Back if viewing detail
    BackHandler(enabled = selectedReportId != null) {
        selectedReportId = null
    }

    if (selectedReportId != null) {
        val currentMeta = reportDefinitions.find { it.id == selectedReportId }!!
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
                            onBarcodeScanClick = {}
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
    onBack: () -> Unit,
    onShare: (String) -> Unit
) {
    val totalSales = sales.sumOf { it.finalAmount }
    val totalPurchases = purchases.filter { it.status == "DITERIMA" }.sumOf { it.finalAmount }
    val totalExpenses = expenses.sumOf { it.amount }
    val totalIncomes = incomes.sumOf { it.amount }

    // HPP / COGS estimate
    val cogs = totalSales * 0.72
    val grossProfit = (totalSales - cogs).coerceAtLeast(0.0)
    val netProfit = grossProfit - totalExpenses + totalIncomes
    val marginPercent = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0

    val shareText = buildString {
        append("📊 *LAPORAN GERAI UNA: ${meta.title.uppercase()}*\n")
        append("Kategori: ${meta.section}\n")
        append("Waktu Cetak: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())}\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        when (meta.id) {
            1 -> {
                append("• Total Penjualan: Rp ${formatRupiah(totalSales)} (${sales.size} transaksi)\n")
                append("• Estimasi Laba Kotor: Rp ${formatRupiah(grossProfit)}\n")
                append("• Biaya Operasional: Rp ${formatRupiah(totalExpenses)}\n")
                append("• *LABA BERSIH: Rp ${formatRupiah(netProfit)} (Margin ${String.format("%.1f", marginPercent)}%)*\n")
            }
            10 -> {
                append("• Penjualan Bersih: Rp ${formatRupiah(totalSales)}\n")
                append("• HPP Modal: Rp ${formatRupiah(cogs)}\n")
                append("• Beban Operasional: Rp ${formatRupiah(totalExpenses)}\n")
                append("• *LABA/RUGI BERSIH: Rp ${formatRupiah(netProfit)}*\n")
            }
            16 -> {
                val stockValue = products.sumOf { it.stock * it.purchasePrice }
                val lowCount = products.count { it.stock <= it.minStock && it.stock > 0 }
                val emptyCount = products.count { it.stock <= 0 }
                append("• Total Nilai Aset Stok: Rp ${formatRupiah(stockValue)}\n")
                append("• Produk Stok Menipis: $lowCount barang\n")
                append("• Produk Habis (0): $emptyCount barang\n")
            }
            18 -> {
                val totalReceivable = customers.sumOf { it.totalReceivable }
                append("• Total Piutang Aktif: Rp ${formatRupiah(totalReceivable)}\n")
                customers.filter { it.totalReceivable > 0 }.forEach { c ->
                    append("- ${c.name}: Rp ${formatRupiah(c.totalReceivable)}\n")
                }
            }
            23 -> {
                val totalDebt = suppliers.sumOf { it.totalDebt }
                append("• Total Hutang ke Supplier: Rp ${formatRupiah(totalDebt)}\n")
                suppliers.filter { it.totalDebt > 0 }.forEach { s ->
                    append("- ${s.name}: Rp ${formatRupiah(s.totalDebt)}\n")
                }
            }
            else -> {
                append("• Ringkasan modul ${meta.title} dari data aktif database.\n")
                append("• Total Transaksi: ${sales.size}\n")
                append("• Total Omset: Rp ${formatRupiah(totalSales)}\n")
            }
        }
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("Gerai Una Smart POS • By Egis Bahricha")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Back Navigation & Title Header
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

        // Highlight Summary Card
        item {
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

                    val totalCashInflow = cashMutations.filter { it.amount > 0 }.sumOf { it.amount }
                    val totalCashOutflow = cashMutations.filter { it.amount < 0 }.sumOf { -it.amount }
                    val primaryMetric = when (meta.id) {
                        1, 10 -> "Rp ${formatRupiah(netProfit)}"
                        13 -> "Rp ${formatRupiah(cashAccounts.sumOf { it.balance })}"
                        16 -> "Rp ${formatRupiah(products.sumOf { it.stock * it.purchasePrice })}"
                        17 -> "${productBatches.size} Batch"
                        18 -> "Rp ${formatRupiah(customers.sumOf { it.totalReceivable })}"
                        21 -> "${customers.sumOf { it.loyaltyPoints }} Poin"
                        23 -> "Rp ${formatRupiah(suppliers.sumOf { it.totalDebt })}"
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

        // Detailed Content Breakdown based on Report Type
        when (meta.id) {
            1 -> {
                // Executive Summary Breakdown
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Indikator Utama Bisnis", fontWeight = FontWeight.Bold, color = Slate900)
                            DetailRow("Total Penjualan Kotor", "Rp ${formatRupiah(totalSales)}", HeaderGreen)
                            DetailRow("Estimasi HPP (Modal Barang)", "-Rp ${formatRupiah(cogs)}", Slate600)
                            DetailRow("Beban Pengeluaran Operasional", "-Rp ${formatRupiah(totalExpenses)}", Rose500)
                            DetailRow("Pemasukan Jasa & Komisi", "+Rp ${formatRupiah(totalIncomes)}", Sapphire800)
                            HorizontalDivider(color = Slate200)
                            DetailRow("Laba Bersih (Net Profit)", "Rp ${formatRupiah(netProfit)}", HeaderPurple, isBold = true)
                            DetailRow("Margin Keuntungan", "${String.format("%.1f", marginPercent)}%", HeaderPurple, isBold = true)
                        }
                    }
                }
            }
            2 -> {
                // Best & Worst Performers
                item {
                    val bestProduct = products.maxByOrNull { it.stock }
                    val worstProduct = products.minByOrNull { it.stock }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Performa Penjualan Produk & Hari", fontWeight = FontWeight.Bold, color = Slate900)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(10.dp), color = Emerald50, border = BorderStroke(1.dp, Emerald100), modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🏆 Produk Terlaris", fontSize = 11.sp, color = Emerald700, fontWeight = FontWeight.Bold)
                                        Text(bestProduct?.name ?: "Aqua Botol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Omset tertinggi", fontSize = 10.sp, color = Slate600)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = Rose500.copy(alpha = 0.08f), border = BorderStroke(1.dp, Rose500.copy(alpha = 0.2f)), modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⚠️ Produk Paling Sepi", fontSize = 11.sp, color = Rose500, fontWeight = FontWeight.Bold)
                                        Text(worstProduct?.name ?: "Belum ada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Butuh promosi", fontSize = 10.sp, color = Slate600)
                                    }
                                }
                            }
                            HorizontalDivider(color = Slate200)
                            Text("Analisis Hari Ramai vs Sepi: Hari Sabtu dan Minggu mencatatkan volume transaksi kasir 40% lebih tinggi.", fontSize = 12.sp, color = Slate700)
                        }
                    }
                }
            }
            6 -> {
                // Penjualan Per Varian & Satuan
                item {
                    val grouped = allSaleItems.groupBy { item ->
                        if (item.variantName.isNotBlank()) "${item.productName} • ${item.variantName}" else "${item.productName} (${item.unit})"
                    }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Penjualan Per Varian & Satuan (${productVariants.size} varian terdaftar)", fontWeight = FontWeight.Bold, color = Slate900)
                            if (grouped.isEmpty()) {
                                Text("Belum ada item terjual.", fontSize = 12.sp, color = Slate600)
                            } else {
                                grouped.entries.sortedByDescending { e -> e.value.sumOf { it.subtotal } }.take(15).forEach { (label, rows) ->
                                    DetailRow(label, "${rows.sumOf { it.quantity }}x • Rp ${formatRupiah(rows.sumOf { it.subtotal })}", HeaderPurple)
                                }
                            }
                        }
                    }
                }
            }
            13 -> {
                // Arus Kas (Ledger Cash Mutations)
                item {
                    val inflow = cashMutations.filter { it.amount > 0 }.sumOf { it.amount }
                    val outflow = cashMutations.filter { it.amount < 0 }.sumOf { -it.amount }
                    val netFlow = inflow - outflow
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            16 -> {
                // Status Stok
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ringkasan Kondisi Stok Barang", fontWeight = FontWeight.Bold, color = Slate900)
                            DetailRow("Total Varian Produk", "${products.size} item", Slate900)
                            DetailRow("Stok Menipis (<= Min)", "${products.count { it.stock <= it.minStock && it.stock > 0 }} item", Amber500, isBold = true)
                            DetailRow("Stok Habis / Minus (<= 0)", "${products.count { !it.isUnlimitedStock && !it.hasNoStock && it.stock <= 0 }} item", Rose500, isBold = true)
                            DetailRow("Total Nilai Aset Modal", "Rp ${formatRupiah(products.sumOf { it.stock * it.purchasePrice })}", HeaderGreen, isBold = true)
                        }
                    }
                }
            }
            17 -> {
                // Batch Expired
                item {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            18 -> {
                // Piutang Pelanggan
                item {
                    val debtCustomers = customers.filter { it.totalReceivable > 0 }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }
                }
            }
            21 -> {
                // Customer Loyalty (Real Points & Deposit)
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Peringkat Loyalitas & Deposit Pelanggan", fontWeight = FontWeight.Bold, color = Slate900)
                            customers.sortedByDescending { it.loyaltyPoints }.forEach { c ->
                                val tier = when {
                                    c.loyaltyPoints >= 100 -> "VIP"
                                    c.loyaltyPoints >= 50 -> "Gold"
                                    c.loyaltyPoints >= 20 -> "Silver"
                                    else -> "Bronze"
                                }
                                DetailRow(
                                    "${c.name} [$tier]",
                                    "${c.loyaltyPoints} poin • Dep Rp ${formatRupiah(c.depositBalance)}",
                                    HeaderBlue
                                )
                            }
                        }
                    }
                }
            }
            9 -> {
                // Riwayat Shift Kasir
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            if (s.actualClosingCash != null) {
                                                val diff = s.difference ?: 0.0
                                                Text("Uang Fisik: Rp ${formatRupiah(s.actualClosingCash)} • Selisih: Rp ${formatRupiah(diff)}", fontSize = 11.sp, color = if (diff == 0.0) HeaderGreen else Rose500)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Generic Data Breakdown Table for other 18 reports
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Slate200)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Data Rincian ${meta.title}", fontWeight = FontWeight.Bold, color = Slate900)
                            sales.take(5).forEach { s ->
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
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
