package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(viewModel: PosViewModel) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val debtPayments by viewModel.debtPayments.collectAsStateWithLifecycle()

    // Cash Inflows: Completed Cash Sales (minus deposit used) + Split Cash + Other Incomes + Receivable Payments
    val completedSales = sales.filter { it.status == "COMPLETED" }
    val cashSales = completedSales.sumOf { s ->
        when (s.paymentMethod) {
            "TUNAI" -> (s.finalAmount - s.depositUsed).coerceAtLeast(0.0)
            "SPLIT" -> s.splitCashAmount
            else -> 0.0
        }
    }
    val otherIncomes = incomes.sumOf { it.amount }
    val receivedReceivables = debtPayments.filter { it.type == "PIUTANG_PELANGGAN" }.sumOf { it.amount }
    val totalCashIn = cashSales + otherIncomes + receivedReceivables

    // Cash Outflows: Cash Purchases (Received) + Expenses + Supplier Debt Payments
    val cashPurchases = purchases.filter { it.status == "DITERIMA" && it.paymentMethod == "TUNAI" }.sumOf { it.finalAmount }
    val operationalExpenses = expenses.sumOf { it.amount }
    val paidSupplierDebts = debtPayments.filter { it.type == "UTANG_PEMASOK" }.sumOf { it.amount }
    val totalCashOut = cashPurchases + operationalExpenses + paidSupplierDebts

    val netCashFlow = totalCashIn - totalCashOut

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("finance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (netCashFlow >= 0) ModuleKas else Rose500
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Arus kas bersih toko",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${if (netCashFlow < 0) "-" else ""}Rp ${formatRupiah(kotlin.math.abs(netCashFlow))}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Kas masuk", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.78f)))
                            Text("Rp ${formatRupiah(totalCashIn)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                        Column {
                            Text("Kas keluar", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.78f)))
                            Text("Rp ${formatRupiah(totalCashOut)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }
        }

        item {
            Text("Arus kas masuk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CashFlowRow("Penjualan Tunai Kasir", cashSales, Emerald700)
                    HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 10.dp))
                    CashFlowRow("Pemasukan Jasa & Komisi", otherIncomes, Emerald700)
                    HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 10.dp))
                    CashFlowRow("Pelunasan Piutang", receivedReceivables, Emerald700)
                }
            }
        }

        item {
            Text("Arus kas keluar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CashFlowRow("Belanja Barang Tunai", cashPurchases, Rose500)
                    HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 10.dp))
                    CashFlowRow("Pengeluaran Operasional", operationalExpenses, Rose500)
                    HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 10.dp))
                    CashFlowRow("Pembayaran Utang Grosir", paidSupplierDebts, Rose500)
                }
            }
        }

        item {
            Text("Riwayat pelunasan utang & piutang", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }

        if (debtPayments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Belum ada mutasi pelunasan utang atau piutang.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(debtPayments, key = { it.id }) { p ->
                val isReceivable = p.type == "PIUTANG_PELANGGAN"
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.partyName, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(
                                SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(p.date)),
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Rp ${formatRupiah(p.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isReceivable) Emerald700 else Rose500
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            DesignStatusBadge(
                                text = if (isReceivable) "Piutang masuk" else "Bayar utang",
                                type = if (isReceivable) BadgeType.SUCCESS else BadgeType.DANGER
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowRow(title: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(color = Slate700))
        Text("Rp ${formatRupiah(amount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
    }
}
