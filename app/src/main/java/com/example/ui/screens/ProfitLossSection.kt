package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pricing.ProfitLossBucket
import com.example.data.pricing.ProfitLossPeriod
import com.example.data.pricing.ProfitLossSummary
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatSignedRupiah(amount: Double): String {
    val absText = formatRupiah(abs(amount))
    return if (amount < -0.5) "-Rp $absText" else "Rp $absText"
}

fun buildProfitLossShareText(summary: ProfitLossSummary): String {
    val statusLabel = if (summary.isProfit) "LABA BERSIH" else "RUGI BERSIH"
    return buildString {
        append("📊 *LAPORAN LABA RUGI (${summary.window.period.label.uppercase()})*\n")
        append("Periode: ${summary.window.title} (${summary.window.rangeLabel})\n")
        append("Waktu Cetak: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())}\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("• Jumlah Transaksi: ${summary.transactionCount} nota\n")
        append("• Penjualan Kotor: Rp ${formatRupiah(summary.grossSales)}\n")
        if (summary.totalDiscounts > 0.0) {
            append("• Diskon & Promo: -Rp ${formatRupiah(summary.totalDiscounts)}\n")
        }
        append("• Penjualan Bersih: Rp ${formatRupiah(summary.netSales)}\n")
        append("• HPP Modal & Pokok: -Rp ${formatRupiah(summary.cogs)}\n")
        if (summary.providerAdminCost > 0.0) {
            append("• Biaya Penyedia Digital: -Rp ${formatRupiah(summary.providerAdminCost)}\n")
        }
        append("• Laba Kotor: ${formatSignedRupiah(summary.grossProfit)}\n")
        if (summary.agentCommissionIncome > 0.0 || summary.otherIncomes > 0.0) {
            append("• Komisi & Jasa Lain: +Rp ${formatRupiah(summary.agentCommissionIncome + summary.otherIncomes)}\n")
        }
        append("• Beban Operasional: -Rp ${formatRupiah(summary.operatingExpenses)}\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("*$statusLabel: ${formatSignedRupiah(summary.netProfit)}*\n")
        append("Margin: ${String.format(Locale("id", "ID"), "%.1f", summary.marginPercent)}%\n")
        append("KASLITE POS")
    }
}

@Composable
fun ProfitLossQuickCheckCard(
    summary: ProfitLossSummary,
    selectedPeriod: ProfitLossPeriod,
    periodOffset: Int,
    onSelectPeriod: (ProfitLossPeriod) -> Unit,
    onChangeOffset: (Int) -> Unit,
    onOpenDetail: () -> Unit
) {
    val isProfit = summary.isProfit
    val statusColor = if (isProfit) Emerald700 else Rose500
    val statusBg = if (isProfit) Emerald50 else Rose100.copy(alpha = 0.55f)
    val statusBorder = if (isProfit) Emerald200 else Rose500.copy(alpha = 0.3f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pnl_quick_check_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cek Laba Rugi Otomatis",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Pantau untung/rugi harian, mingguan, bulanan & tahunan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 11.sp
                        )
                    )
                }
                DesignStatusBadge(
                    text = if (isProfit) "LABA" else "RUGI",
                    type = if (isProfit) BadgeType.SUCCESS else BadgeType.DANGER
                )
            }

            PeriodModeSelector(
                selectedPeriod = selectedPeriod,
                onSelectPeriod = onSelectPeriod,
                tagPrefix = "pnl_quick_tab"
            )

            PeriodStepperBar(
                title = summary.window.title,
                rangeLabel = summary.window.rangeLabel,
                offset = periodOffset,
                onPrev = { onChangeOffset(periodOffset - 1) },
                onNext = { if (periodOffset < 0) onChangeOffset(periodOffset + 1) },
                onResetCurrent = { onChangeOffset(0) }
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProfit) "Laba Bersih (${selectedPeriod.label})" else "Rugi Bersih (${selectedPeriod.label})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = formatSignedRupiah(summary.netProfit),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                ).tabular(),
                                modifier = Modifier.testTag("pnl_quick_net_profit")
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, statusBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Margin ${String.format(Locale("id", "ID"), "%.1f", summary.marginPercent)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniMetricBox(
                            label = "Omset",
                            value = "Rp ${formatRupiah(summary.netSales)}",
                            valueColor = Slate900,
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetricBox(
                            label = "HPP & Penyedia",
                            value = "-Rp ${formatRupiah(summary.cogs + summary.providerAdminCost)}",
                            valueColor = Slate700,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniMetricBox(
                            label = "Beban Ops",
                            value = "-Rp ${formatRupiah(summary.operatingExpenses)}",
                            valueColor = Rose500,
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetricBox(
                            label = "Transaksi",
                            value = "${summary.transactionCount} nota",
                            valueColor = AccentBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Button(
                onClick = onOpenDetail,
                colors = ButtonDefaults.buttonColors(containerColor = ModuleLaporan, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_pnl_detail_btn")
            ) {
                Text(
                    text = "Lihat Rincian Laba Rugi ${selectedPeriod.label}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ProfitLossDetailContent(
    summary: ProfitLossSummary,
    selectedPeriod: ProfitLossPeriod,
    periodOffset: Int,
    onSelectPeriod: (ProfitLossPeriod) -> Unit,
    onChangeOffset: (Int) -> Unit,
    onBucketClick: (ProfitLossBucket) -> Unit = {}
) {
    val isProfit = summary.isProfit
    val heroGradient = if (isProfit) {
        Brush.linearGradient(listOf(Emerald800, Emerald600))
    } else {
        Brush.linearGradient(listOf(Color(0xFF991B1B), Rose500))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Period Tabs (Harian, Mingguan, Bulanan, Tahunan)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih Periode Laba Rugi",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                )
                PeriodModeSelector(
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = onSelectPeriod,
                    tagPrefix = "pnl_detail_tab"
                )
                PeriodStepperBar(
                    title = summary.window.title,
                    rangeLabel = summary.window.rangeLabel,
                    offset = periodOffset,
                    onPrev = { onChangeOffset(periodOffset - 1) },
                    onNext = { if (periodOffset < 0) onChangeOffset(periodOffset + 1) },
                    onResetCurrent = { onChangeOffset(0) }
                )
            }
        }

        // Dynamic Profit/Loss Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(heroGradient)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isProfit) "LABA BERSIH" else "RUGI BERSIH"} • ${selectedPeriod.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.88f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${summary.transactionCount} Transaksi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = formatSignedRupiah(summary.netProfit),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ).tabular(),
                    modifier = Modifier.testTag("pnl_detail_net_profit")
                )

                val delta = summary.profitDeltaVsPrevious
                val deltaSign = if (delta >= 0.0) "▲ +" else "▼ -"
                val prevPeriodWord = when (selectedPeriod) {
                    ProfitLossPeriod.HARIAN -> "hari sebelumnya"
                    ProfitLossPeriod.MINGGUAN -> "minggu sebelumnya"
                    ProfitLossPeriod.BULANAN -> "bulan sebelumnya"
                    ProfitLossPeriod.TAHUNAN -> "tahun sebelumnya"
                }
                Text(
                    text = "Margin ${String.format(Locale("id", "ID"), "%.1f", summary.marginPercent)}%  •  ${deltaSign}Rp ${formatRupiah(abs(delta))} vs $prevPeriodWord",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.sp
                    ).tabular()
                )
            }
        }

        // Physical vs Digital Split Comparison Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = ModuleProduk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Produk Fisik",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                    }
                    Text(
                        text = "Omset: Rp ${formatRupiah(summary.physicalSales)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            fontSize = 11.sp
                        ).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Laba: ${formatSignedRupiah(summary.physicalNetProfit)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (summary.physicalNetProfit >= 0.0) Emerald700 else Rose500
                        ).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Devices,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Layanan Digital",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                    }
                    Text(
                        text = "Omset: Rp ${formatRupiah(summary.digitalSales)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            fontSize = 11.sp
                        ).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Laba: ${formatSignedRupiah(summary.digitalNetProfit)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (summary.digitalNetProfit >= 0.0) AccentBlue else Rose500
                        ).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Compose Canvas Bar Chart Card (Omset vs Laba Bersih)
        if (summary.subBuckets.isNotEmpty()) {
            ProfitLossBarChartCard(buckets = summary.subBuckets)
        }

        // Income Statement Breakdown (Laporan Laba Rugi)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Rincian Laba Rugi (${summary.window.rangeLabel})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )

                DetailRow(
                    label = "Penjualan Kotor & Admin Toko",
                    value = "Rp ${formatRupiah(summary.grossSales)}",
                    valueColor = Emerald700
                )
                if (summary.totalDiscounts > 0.0) {
                    DetailRow(
                        label = "Potongan Diskon & Promo",
                        value = "-Rp ${formatRupiah(summary.totalDiscounts)}",
                        valueColor = Rose500
                    )
                }
                DetailRow(
                    label = "Penjualan Bersih (Omset)",
                    value = "Rp ${formatRupiah(summary.netSales)}",
                    valueColor = Slate900,
                    isBold = true
                )

                if (summary.storeAdminRevenue > 0.0) {
                    DetailRow(
                        label = "  • Termasuk Admin Layanan Digital",
                        value = "Rp ${formatRupiah(summary.storeAdminRevenue)}",
                        valueColor = AccentBlue
                    )
                }

                HorizontalDivider(color = Slate200)

                DetailRow(
                    label = "HPP Modal Barang & Pokok Digital",
                    value = "-Rp ${formatRupiah(summary.cogs)}",
                    valueColor = Slate700
                )
                if (summary.providerAdminCost > 0.0) {
                    DetailRow(
                        label = "Biaya Penyedia / Admin Bank",
                        value = "-Rp ${formatRupiah(summary.providerAdminCost)}",
                        valueColor = Rose500
                    )
                }
                DetailRow(
                    label = "Laba Kotor (Gross Profit)",
                    value = formatSignedRupiah(summary.grossProfit),
                    valueColor = if (summary.grossProfit >= 0.0) Emerald700 else Rose500,
                    isBold = true
                )

                HorizontalDivider(color = Slate200)

                DetailRow(
                    label = "Komisi Agen & Pemasukan Jasa",
                    value = "+Rp ${formatRupiah(summary.agentCommissionIncome + summary.otherIncomes)}",
                    valueColor = Sapphire800
                )
                DetailRow(
                    label = "Beban Pengeluaran Operasional",
                    value = "-Rp ${formatRupiah(summary.operatingExpenses)}",
                    valueColor = Rose500
                )

                HorizontalDivider(color = Slate200)

                DetailRow(
                    label = if (isProfit) "LABA BERSIH" else "RUGI BERSIH",
                    value = formatSignedRupiah(summary.netProfit),
                    valueColor = if (isProfit) Emerald700 else Rose500,
                    isBold = true
                )
                DetailRow(
                    label = "Margin Keuntungan Bersih",
                    value = "${String.format(Locale("id", "ID"), "%.1f", summary.marginPercent)}%",
                    valueColor = if (isProfit) ModuleLaporan else Rose500,
                    isBold = true
                )
            }
        }

        // Sub-Period Breakdown Timeline
        val breakdownTitle = when (selectedPeriod) {
            ProfitLossPeriod.HARIAN -> "Rekap 7 Hari Terakhir"
            ProfitLossPeriod.MINGGUAN -> "Rincian Harian (Senin – Minggu)"
            ProfitLossPeriod.BULANAN -> "Rincian Mingguan dalam Bulan"
            ProfitLossPeriod.TAHUNAN -> "Rincian Bulanan (Januari – Desember)"
        }
        val maxAbsProfit = summary.subBuckets.maxOfOrNull { abs(it.netProfit) }?.coerceAtLeast(1.0) ?: 1.0

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breakdownTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    if (selectedPeriod != ProfitLossPeriod.HARIAN) {
                        Text(
                            text = "Ketuk untuk drill-down",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                summary.subBuckets.forEach { bucket ->
                    SubBucketRow(
                        bucket = bucket,
                        maxAbsProfit = maxAbsProfit,
                        onClick = { onBucketClick(bucket) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitLossBarChartCard(buckets: List<ProfitLossBucket>) {
    val omsetBarColor = AccentBlue
    val profitBarColor = Emerald500
    val lossBarColor = Rose500
    val baselineColor = BorderDefault
    val maxVal = buckets.maxOfOrNull { maxOf(it.netSales, abs(it.netProfit)) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grafik Omset vs Laba Bersih",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(omsetBarColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Omset", fontSize = 10.sp, color = Slate600)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(profitBarColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Laba", fontSize = 10.sp, color = Slate600)
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val n = buckets.size.coerceAtLeast(1)
                val groupWidth = size.width / n
                val barWidth = (groupWidth * 0.28f).coerceIn(6f, 24f)
                val gap = (barWidth * 0.25f).coerceAtLeast(2f)
                val bottomY = size.height - 4f
                val usableHeight = (size.height - 12f).coerceAtLeast(20f)

                drawLine(
                    color = baselineColor,
                    start = Offset(0f, bottomY),
                    end = Offset(size.width, bottomY),
                    strokeWidth = 2f
                )

                buckets.forEachIndexed { idx, bucket ->
                    val centerX = idx * groupWidth + groupWidth / 2f
                    val omsetFrac = (bucket.netSales / maxVal).toFloat().coerceIn(0f, 1f)
                    val profitFrac = (abs(bucket.netProfit) / maxVal).toFloat().coerceIn(0f, 1f)
                    val omsetH = if (bucket.netSales > 0.0) (usableHeight * omsetFrac).coerceAtLeast(6f) else 2f
                    val profitH = if (abs(bucket.netProfit) > 0.5) (usableHeight * profitFrac).coerceAtLeast(6f) else 2f
                    val pColor = if (bucket.netProfit >= 0.0) profitBarColor else lossBarColor

                    drawRoundRect(
                        color = omsetBarColor,
                        topLeft = Offset(centerX - barWidth - gap / 2f, bottomY - omsetH),
                        size = Size(barWidth, omsetH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = pColor,
                        topLeft = Offset(centerX + gap / 2f, bottomY - profitH),
                        size = Size(barWidth, profitH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                buckets.forEach { bucket ->
                    Text(
                        text = bucket.label.take(4),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Slate500
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodModeSelector(
    selectedPeriod: ProfitLossPeriod,
    onSelectPeriod: (ProfitLossPeriod) -> Unit,
    tagPrefix: String
) {
    val options = ProfitLossPeriod.entries.map { it.key to it.label }
    DesignSegmentedTabs(
        options = options,
        selectedKey = selectedPeriod.key,
        onSelect = { key ->
            ProfitLossPeriod.entries.find { it.key == key }?.let(onSelectPeriod)
        },
        modifier = Modifier.testTag(tagPrefix)
    )
}

@Composable
private fun PeriodStepperBar(
    title: String,
    rangeLabel: String,
    offset: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onResetCurrent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate50,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pnl_prev_period_btn")
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Periode sebelumnya",
                    tint = Slate800
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (offset < 0) {
                    TextButton(
                        onClick = onResetCurrent,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Kembali ke saat ini",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }
            }

            IconButton(
                onClick = onNext,
                enabled = offset < 0,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pnl_next_period_btn")
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Periode berikutnya",
                    tint = if (offset < 0) Slate800 else Slate300
                )
            }
        }
    }
}

@Composable
private fun MiniMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderDefault.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate500,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                ).tabular(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubBucketRow(
    bucket: ProfitLossBucket,
    maxAbsProfit: Double,
    onClick: () -> Unit = {}
) {
    val isBucketProfit = bucket.netProfit >= 0.0
    val barColor = if (bucket.netProfit == 0.0 && bucket.netSales == 0.0 && bucket.totalCost == 0.0) {
        Slate300
    } else if (isBucketProfit) {
        Emerald500
    } else {
        Rose500
    }
    val profitColor = if (bucket.netProfit == 0.0 && bucket.netSales == 0.0 && bucket.totalCost == 0.0) {
        Slate500
    } else if (isBucketProfit) {
        Emerald700
    } else {
        Rose500
    }
    val fraction = (abs(bucket.netProfit) / maxAbsProfit).toFloat().coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Slate50,
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "${bucket.subLabel} • ${bucket.transactionCount} trx • Omset Rp ${formatRupiah(bucket.netSales)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontSize = 10.sp
                        ).tabular(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatSignedRupiah(bucket.netProfit),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    ).tabular()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Slate200)
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}
