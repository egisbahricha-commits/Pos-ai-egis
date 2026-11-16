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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.KasGoSearchBar
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesHistoryScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    var selectedSaleForDetail by remember { mutableStateOf<SaleEntity?>(null) }
    var saleItemsDetail by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    var showRefundDialog by remember { mutableStateOf(false) }

    val filteredSales = remember(sales, searchQuery, selectedFilter) {
        sales.filter { sale ->
            val matchQuery = searchQuery.isBlank() ||
                    sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.customerName.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "SELESAI" -> sale.status == "COMPLETED"
                "PRE_ORDER" -> sale.status == "PRE_ORDER"
                "RETUR" -> sale.status == "REFUNDED"
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("sales_history_screen")
    ) {
        // Search & Filter Header Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KasGoSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari no. faktur atau nama pelanggan...",
                onBarcodeScanClick = {
                    val firstInv = sales.firstOrNull()?.invoiceNumber
                    if (firstInv != null) searchQuery = firstInv
                }
            )

            DesignSegmentedTabs(
                options = listOf(
                    "SEMUA" to "Semua",
                    "SELESAI" to "Lunas",
                    "PRE_ORDER" to "Pre-Order",
                    "RETUR" to "Retur"
                ),
                selectedKey = selectedFilter,
                onSelect = { selectedFilter = it }
            )
        }
        HorizontalDivider(color = BorderDefault)

        // Sales List
        if (filteredSales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DesignEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Belum ada riwayat transaksi",
                    subtitle = "Transaksi kasir akan muncul di halaman ini."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSales, key = { it.id }) { sale ->
                    val isRefunded = sale.status == "REFUNDED"
                    val isPreOrder = sale.status == "PRE_ORDER"
                    val isCredit = sale.paymentMethod.equals("KREDIT", true) || (sale.amountPaid < sale.finalAmount && sale.paymentMethod != "TUNAI" && !isPreOrder)
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(sale.date))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, BorderDefault),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSaleForDetail = sale
                                coroutineScope.launch {
                                    saleItemsDetail = viewModel.repository.getItemsForSale(sale.id)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sale.invoiceNumber,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$dateFormatted • ${sale.customerName}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp)
                                )
                                Text(
                                    text = "${sale.paymentMethod} • Kasir: ${sale.cashierName}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 11.sp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rp ${formatRupiah(sale.finalAmount)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DesignStatusBadge(
                                    text = when {
                                        isRefunded -> "Retur"
                                        isPreOrder -> "Pre-Order"
                                        isCredit -> "Piutang"
                                        else -> "Lunas"
                                    },
                                    type = when {
                                        isRefunded || isCredit -> BadgeType.DANGER
                                        isPreOrder -> BadgeType.WARNING
                                        else -> BadgeType.SUCCESS
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail & Retur Dialog
    if (selectedSaleForDetail != null) {
        val sale = selectedSaleForDetail!!
        Dialog(onDismissRequest = { selectedSaleForDetail = null }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detail Transaksi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        IconButton(onClick = { selectedSaleForDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                        }
                    }

                    HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 8.dp))

                    Text("Faktur: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, color = AccentBlue)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Pelanggan: ${sale.customerName} • Kasir: ${sale.cashierName}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    Text("Tanggal: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(sale.date))}", style = MaterialTheme.typography.bodySmall, color = Slate500)

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = BgCanvas,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDefault),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daftar Barang", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate600))
                            saleItemsDetail.forEach { itm ->
                                val variantSuffix = if (itm.variantName.isNotBlank()) " [${itm.variantName}]" else ""
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${itm.productName}$variantSuffix (${itm.quantity} ${itm.unit})", style = MaterialTheme.typography.bodySmall, color = Slate800, modifier = Modifier.weight(1f))
                                    Text("Rp ${formatRupiah(itm.subtotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Slate900)
                                }
                            }
                            if (sale.shippingCost > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ongkir", style = MaterialTheme.typography.bodySmall, color = Slate600)
                                    Text("+Rp ${formatRupiah(sale.shippingCost)}", style = MaterialTheme.typography.bodySmall, color = Slate800)
                                }
                            }
                            if (sale.additionalFee > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Biaya Tambahan", style = MaterialTheme.typography.bodySmall, color = Slate600)
                                    Text("+Rp ${formatRupiah(sale.additionalFee)}", style = MaterialTheme.typography.bodySmall, color = Slate800)
                                }
                            }
                            if (sale.promoDiscount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Promo (${sale.appliedPromoName})", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                                    Text("-Rp ${formatRupiah(sale.promoDiscount)}", style = MaterialTheme.typography.bodySmall, color = Emerald700)
                                }
                            }
                            if (sale.depositUsed > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Potong Deposit", style = MaterialTheme.typography.bodySmall, color = AccentBlue)
                                    Text("-Rp ${formatRupiah(sale.depositUsed)}", style = MaterialTheme.typography.bodySmall, color = AccentBlue)
                                }
                            }
                            HorizontalDivider(color = BorderDefault)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Tagihan", fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Rp ${formatRupiah(sale.finalAmount)}", fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (sale.status == "PRE_ORDER") {
                        Button(
                            onClick = {
                                viewModel.completePreOrder(sale.id)
                                selectedSaleForDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Selesaikan Pre-Order & Potong Stok", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sale.status != "REFUNDED") {
                            OutlinedButton(
                                onClick = { showRefundDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retur", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val text = "🧾 *STRUK PEMBELIAN GERAI UNA*\nFaktur: ${sale.invoiceNumber}\nPelanggan: ${sale.customerName}\nTotal: Rp ${formatRupiah(sale.finalAmount)}\nMetode: ${sale.paymentMethod}"
                                viewModel.whatsAppCommandCenter.shareViaWhatsApp(context, text)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim WA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Retur Penjualan Confirmation
    if (showRefundDialog && selectedSaleForDetail != null) {
        val sale = selectedSaleForDetail!!
        var reason by remember { mutableStateOf("Retur Barang Rusak / Salah Beli") }

        Dialog(onDismissRequest = { showRefundDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Konfirmasi Retur Penjualan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Rose500))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Transaksi ${sale.invoiceNumber} akan ditandai RETUR dan jumlah barang otomatis dikembalikan ke stok.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DesignLabeledTextField(
                        label = "Alasan Retur",
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRefundDialog = false }) { Text("Batal", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.refundSale(sale.id, reason)
                                showRefundDialog = false
                                selectedSaleForDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Konfirmasi Retur", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
