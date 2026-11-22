package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.pricing.ProfitLossCalculator
import com.example.data.pricing.ProfitLossPeriod
import com.example.firebase.LicenseAccessStatus
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.KasLiteSectionHeader
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: PosViewModel) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val allSaleItems by viewModel.allSaleItems.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()

    val currentDateStr = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date())
    }

    // Real business KPIs from Room database
    val todaySummary = remember(sales, allSaleItems, expenses, incomes, products) {
        ProfitLossCalculator.calculate(
            period = ProfitLossPeriod.HARIAN,
            offset = 0,
            sales = sales,
            saleItems = allSaleItems,
            expenses = expenses,
            incomes = incomes,
            products = products
        )
    }
    val totalSales = remember(sales) {
        sales.filter { it.status == "COMPLETED" }.sumOf { it.finalAmount }
    }
    val totalReceivables = remember(customers) { customers.sumOf { it.totalReceivable } }
    val physicalActiveProducts = remember(products) {
        products.filter { it.isActive && !it.isUnlimitedStock && !it.hasNoStock && it.productType != "DIGITAL" }
    }
    val outOfStockCount = remember(physicalActiveProducts) { physicalActiveProducts.count { it.stock <= 0.0 } }
    val lowStockCount = lowStockProducts.size
    val activeCashAccounts = remember(cashAccounts) { cashAccounts.filter { it.isActive } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Store Date & User Info Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(storeProfile?.name?.ifBlank { "Toko Saya" } ?: "Toko Saya").uppercase()} • $currentDateStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = activeUserName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // KASLITE Local Mode & License Status Banner
        item {
            val isFull = licenseUiState.accessStatus == LicenseAccessStatus.FULL_ACTIVE
            val isDemo = licenseUiState.accessStatus == LicenseAccessStatus.DEMO_ACTIVE
            val remainingDays = ((licenseUiState.remainingMillis + 86_399_999L) / 86_400_000L).toInt().coerceAtLeast(0)
            Surface(
                onClick = { viewModel.navigateTo(Screen.PENGATURAN) },
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isFull -> Emerald50
                    isDemo -> Sapphire50
                    else -> Rose100
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isFull -> Emerald700.copy(alpha = 0.35f)
                        isDemo -> Sapphire200
                        else -> Rose500.copy(alpha = 0.45f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isFull || isDemo) Icons.Default.VerifiedUser else Icons.Default.Lock,
                            contentDescription = null,
                            tint = when {
                                isFull -> Emerald700
                                isDemo -> AccentBlue
                                else -> Rose500
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (licenseUiState.accessStatus) {
                                LicenseAccessStatus.FULL_ACTIVE ->
                                    "KASLITE Lisensi Penuh • Data 100% Lokal & Backup Siap"
                                LicenseAccessStatus.DEMO_ACTIVE ->
                                    "Mode Demo KASLITE (Sisa $remainingDays hari) • Ketuk untuk Aktivasi / Backup"
                                else ->
                                    "Demo Habis / Terkunci • Hubungi Super Admin atau Ketuk Pengaturan"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = when {
                                    isFull -> Emerald800
                                    isDemo -> Sapphire800
                                    else -> Rose500
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Pengaturan & Backup",
                        tint = when {
                            isFull -> Emerald700
                            isDemo -> AccentBlue
                            else -> Rose500
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // BERANDA: 3x2 MENU GRID (Matches Design System Photo)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DesignMenuGridTile(
                            title = "Kasir",
                            icon = Icons.Default.ShoppingCart,
                            color = ModuleKasir,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_pos_button"),
                            onClick = { viewModel.navigateTo(Screen.POS_KASIR) }
                        )
                        DesignMenuGridTile(
                            title = "Produk",
                            icon = Icons.Default.Inventory2,
                            color = ModuleProduk,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.PRODUK) }
                        )
                        DesignMenuGridTile(
                            title = "Laporan",
                            icon = Icons.Default.BarChart,
                            color = ModuleLaporan,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.LAPORAN) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DesignMenuGridTile(
                            title = "Multi Kas",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = ModuleKas,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.MULTI_KAS) }
                        )
                        DesignMenuGridTile(
                            title = "Tarif Admin",
                            icon = Icons.Default.Percent,
                            color = ModuleKasir,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.TARIF_ADMIN) }
                        )
                        DesignMenuGridTile(
                            title = "Pengaturan",
                            icon = Icons.Default.Lock,
                            color = ModuleBeranda,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(Screen.PENGATURAN) }
                        )
                    }
                }
            }
        }

        // BERANDA: 3 SUMMARY TILES (Penjualan hari ini, Laba hari ini, Piutang)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. White Surface Card: Penjualan hari ini
                Card(
                    onClick = { viewModel.navigateTo(Screen.RIWAYAT_TRANSAKSI) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Penjualan hari ini",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rp ${formatRupiah(todaySummary.netSales)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                fontSize = 15.sp
                            ).tabular(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${todaySummary.transactionCount} transaksi",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp).tabular(),
                            maxLines = 1
                        )
                    }
                }

                // 2. Solid Emerald / Rose Card: Laba hari ini
                Card(
                    onClick = { viewModel.navigateTo(Screen.LAPORAN) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todaySummary.isProfit) Emerald800 else Color(0xFF991B1B)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (todaySummary.isProfit) "Laba hari ini" else "Rugi hari ini",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.85f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatSignedRupiah(todaySummary.netProfit),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ).tabular(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aktual hari ini",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp),
                            maxLines = 1
                        )
                    }
                }

                // 3. Solid Purple Card: Piutang
                Card(
                    onClick = { viewModel.navigateTo(Screen.UTANG_PIUTANG) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ModuleLaporan),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Piutang",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.85f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rp ${formatRupiah(totalReceivables)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ).tabular(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Belum lunas",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Saldo Multi-Kas Horizontal Strip
        if (activeCashAccounts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KasLiteSectionHeader(
                        title = "Saldo Multi-Kas",
                        actionText = "Kelola",
                        onActionClick = { viewModel.navigateTo(Screen.MULTI_KAS) }
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeCashAccounts, key = { it.id }) { acc ->
                            Surface(
                                onClick = { viewModel.navigateTo(Screen.MULTI_KAS) },
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceWhite,
                                border = BorderStroke(1.dp, BorderDefault),
                                modifier = Modifier.width(156.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = acc.accountName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Slate500,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Rp ${formatRupiah(acc.balance)}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = ModuleKas,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = acc.accountType,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Slate400,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unpaid Receivables Alert Pill
        if (totalReceivables > 0.0) {
            val debtorCount = customers.count { it.totalReceivable > 0.0 }
            item {
                Surface(
                    onClick = { viewModel.navigateTo(Screen.UTANG_PIUTANG) },
                    shape = RoundedCornerShape(12.dp),
                    color = Rose100,
                    border = BorderStroke(1.dp, Rose500.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$debtorCount Pelanggan Masih Kasbon",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Rose500
                                )
                            )
                            Text(
                                text = "Total piutang aktif: Rp ${formatRupiah(totalReceivables)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate700,
                                    fontSize = 11.sp
                                ).tabular()
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Lihat Piutang",
                            tint = Rose500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Secondary Quick Shortcuts Row (AI Chat, Pembelian, Stok, Pengeluaran)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickShortcutButton(
                    title = "AI Chat",
                    icon = Icons.Default.AutoAwesome,
                    color = ModuleKasir,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.AI_CHAT) }
                )
                QuickShortcutButton(
                    title = "Belanja",
                    icon = Icons.Default.ShoppingBag,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.PEMBELIAN) }
                )
                QuickShortcutButton(
                    title = "Stok Gudang",
                    icon = Icons.Default.Warehouse,
                    color = ModuleProduk,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.STOK_INVENTARIS) }
                )
                QuickShortcutButton(
                    title = "Pengeluaran",
                    icon = Icons.Default.MoneyOff,
                    color = Rose500,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.PENGELUARAN) }
                )
            }
        }

        // Low Stock Alert Strip
        if (lowStockCount > 0 || outOfStockCount > 0) {
            item {
                Surface(
                    onClick = { viewModel.navigateTo(Screen.STOK_INVENTARIS) },
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarningBg,
                    border = BorderStroke(1.dp, AmberWarningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber500, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$lowStockCount produk stok menipis" + if (outOfStockCount > 0) " • $outOfStockCount habis" else "",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                            )
                            Text(
                                text = "Ketuk untuk periksa inventaris atau buat pembelian",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 11.sp)
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Sales Trend Chart Card
        item {
            SalesTrendChartCard(sales = sales, totalSales = totalSales)
        }

        // Transaksi Terakhir (Matches Design System List Pattern)
        item {
            KasLiteSectionHeader(
                title = "Transaksi terakhir",
                actionText = "Lihat semua",
                onActionClick = { viewModel.navigateTo(Screen.RIWAYAT_TRANSAKSI) }
            )
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DesignEmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Belum ada transaksi",
                        subtitle = "Buka menu Kasir untuk mencatat penjualan pertama.",
                        ctaText = "Buka Kasir",
                        onCtaClick = { viewModel.navigateTo(Screen.POS_KASIR) }
                    )
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val recent = sales.take(5)
                        recent.forEachIndexed { index, sale ->
                            val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(sale.date))
                            val isRefunded = sale.status == "REFUNDED"
                            val isCancelled = sale.status == "CANCELLED"
                            val isPreOrder = sale.status == "PRE_ORDER"
                            val isCredit = sale.status == "COMPLETED" && (
                                sale.paymentMethod.equals("KREDIT", true) ||
                                    (sale.amountPaid < sale.finalAmount && sale.paymentMethod !in listOf("TUNAI", "SPLIT"))
                                )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.navigateTo(Screen.RIWAYAT_TRANSAKSI) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sale.invoiceNumber,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Rp ${formatRupiah(sale.finalAmount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        ).tabular()
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    DesignStatusBadge(
                                        text = when {
                                            isRefunded -> "Retur"
                                            isCancelled -> "Batal"
                                            isPreOrder -> "Pre-Order"
                                            isCredit -> "Piutang"
                                            else -> "Lunas"
                                        },
                                        type = when {
                                            isRefunded || isCancelled || isCredit -> BadgeType.DANGER
                                            isPreOrder -> BadgeType.WARNING
                                            else -> BadgeType.SUCCESS
                                        }
                                    )
                                }
                            }
                            if (index < recent.lastIndex) {
                                HorizontalDivider(color = BorderDefault)
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
fun DesignMenuGridTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = Slate800,
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickShortcutButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(SurfaceStrong),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SalesTrendChartCard(sales: List<com.example.data.entity.SaleEntity>, totalSales: Double) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tren Volume Penjualan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        "Aktivitas transaksi per jam",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                }
                DesignStatusBadge(text = "Hari Ini", type = BadgeType.SUCCESS)
            }

            Spacer(modifier = Modifier.height(14.dp))

            val periods = listOf("08-10", "10-12", "12-14", "14-16", "16-18", "18-20", "20-22")
            val (bucketTotals, maxBucket) = remember(sales) {
                val cal = Calendar.getInstance()
                val todayYear = cal.get(Calendar.YEAR)
                val todayDay = cal.get(Calendar.DAY_OF_YEAR)
                val totals = DoubleArray(7)
                sales.filter { it.status == "COMPLETED" }.forEach { sale ->
                    cal.timeInMillis = sale.date
                    if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        val idx = when {
                            hour < 10 -> 0
                            hour < 12 -> 1
                            hour < 14 -> 2
                            hour < 16 -> 3
                            hour < 18 -> 4
                            hour < 20 -> 5
                            else -> 6
                        }
                        totals[idx] += sale.finalAmount
                    }
                }
                val peak = totals.maxOrNull() ?: 0.0
                totals to peak
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                periods.forEachIndexed { index, period ->
                    val amount = bucketTotals[index]
                    val factor = if (maxBucket > 0.0) (amount / maxBucket).toFloat().coerceIn(0f, 1f) else 0f
                    val barHeightDp = if (amount > 0.0) (10f + 56f * factor).dp else 4.dp
                    val isPeak = maxBucket > 0.0 && amount == maxBucket

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(barHeightDp)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(
                                    when {
                                        isPeak -> Emerald700
                                        amount > 0.0 -> AccentBlue
                                        else -> Slate200
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = period,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = if (isPeak) Emerald700 else Slate500,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
            if (maxBucket <= 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Belum ada transaksi selesai hari ini (total selesai: Rp ${formatRupiah(totalSales)})",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 11.sp)
                )
            }
        }
    }
}
