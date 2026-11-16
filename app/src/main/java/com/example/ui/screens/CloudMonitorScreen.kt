package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.SyncMode
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatMonitorDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(timestamp))

@Composable
fun CloudMonitorScreen(viewModel: PosViewModel) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val cashAccounts by viewModel.cashAccounts.collectAsStateWithLifecycle()
    val cashMutations by viewModel.cashMutations.collectAsStateWithLifecycle()
    val cloudCashAccounts by viewModel.cloudCashAccounts.collectAsStateWithLifecycle()
    val cloudCashMutations by viewModel.cloudCashMutations.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var mutationFilter by remember { mutableStateOf("ALL") } // ALL, IN, OUT

    val totalBalance = cashAccounts.sumOf { it.balance }
    val totalCashIn = cashMutations.filter { it.amount > 0 }.sumOf { it.amount }
    val totalCashOut = cashMutations.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
    val netCashFlow = totalCashIn - totalCashOut

    val totalSalesRevenue = sales.filter { it.status != "REFUNDED" && it.status != "CANCELLED" }.sumOf { it.finalAmount }
    val totalExpensesAmt = expenses.sumOf { it.amount }
    val totalIncomesAmt = incomes.sumOf { it.amount }
    val totalReceivables = customers.sumOf { it.totalReceivable }
    val totalPayables = suppliers.sumOf { it.totalDebt }

    val filteredMutations = remember(cashMutations, mutationFilter) {
        when (mutationFilter) {
            "IN" -> cashMutations.filter { it.amount > 0 }
            "OUT" -> cashMutations.filter { it.amount < 0 }
            else -> cashMutations
        }.take(40)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("cloud_monitor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Firebase Cloud Account & Sync Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderDefault),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    if (authState.isCloudSignedIn) listOf(Sapphire900, AccentBlue, ModuleKasir)
                                    else listOf(Slate800, Slate700)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (authState.isCloudSignedIn) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (authState.isCloudSignedIn) authState.userEmail else "Mode Lokal (Belum Login Firebase)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                DesignStatusBadge(
                                    text = if (authState.isCloudSignedIn) "Firestore Aktif" else "Offline",
                                    type = if (authState.isCloudSignedIn) BadgeType.SUCCESS else BadgeType.WARNING
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Devices, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Perangkat: ${authState.deviceName} (${authState.deviceId})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                )
                            }
                            if (authState.isCloudSignedIn) {
                                Text(
                                    text = "Live Firestore: ${cloudCashAccounts.size} akun kas • ${cloudCashMutations.size} mutasi arus kas terpantau",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Emerald100, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                text = if (authState.lastSyncAt > 0L)
                                    "Sinkronisasi terakhir: ${formatMonitorDate(authState.lastSyncAt)} • ${authState.lastSyncMessage}"
                                else
                                    "Belum pernah sinkronisasi Firebase pada perangkat ini.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (authState.isCloudSignedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Sync Transaksi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                    Text("Kirim penjualan, pengeluaran & mutasi kas ke Firestore otomatis", fontSize = 11.sp, color = Slate500)
                                }
                                Switch(
                                    checked = authState.autoSyncEnabled,
                                    onCheckedChange = { viewModel.toggleAutoSync(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncNowWithFirebase(SyncMode.TWO_WAY) },
                                    enabled = !authState.isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("sync_two_way_button")
                                ) {
                                    if (authState.isSyncing) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sinkronkan", fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.syncNowWithFirebase(SyncMode.PULL_ONLY) },
                                    enabled = !authState.isSyncing,
                                    border = BorderStroke(1.dp, BorderInput),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Tarik Cloud", tint = ModuleKasir, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tarik", color = ModuleKasir, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.syncNowWithFirebase(SyncMode.PUSH_ONLY) },
                                    enabled = !authState.isSyncing,
                                    border = BorderStroke(1.dp, BorderInput),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Kirim Cloud", tint = AccentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kirim", color = AccentBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.logoutToLoginScreen() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("goto_login_button")
                            ) {
                                Text("Masuk dengan Google (Firebase)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BgCanvas,
                                border = BorderStroke(1.dp, BorderDefault),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Terlindung Security Rules Firebase",
                                        color = Slate700,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (authState.isCloudSignedIn) {
                                OutlinedButton(
                                    onClick = { viewModel.logoutToLoginScreen() },
                                    border = BorderStroke(1.dp, Rose100),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Rose500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Keluar", color = Rose500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ringkasan Arus Kas & Bisnis Terpantau
        item {
            Text(
                "Pantauan Arus Kas & Keuangan",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonitorMetricCard(
                    title = "Total Semua Kas",
                    value = "Rp ${formatRupiah(totalBalance)}",
                    subtitle = "${cashAccounts.size} akun kas aktif",
                    containerColor = ModuleKas,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                MonitorMetricCard(
                    title = "Surplus / Defisit Mutasi",
                    value = "${if (netCashFlow >= 0) "+" else "-"}Rp ${formatRupiah(kotlin.math.abs(netCashFlow))}",
                    subtitle = "Masuk Rp ${formatRupiah(totalCashIn)}",
                    containerColor = if (netCashFlow >= 0) Emerald700 else Rose500,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonitorMetricCard(
                    title = "Total Penjualan",
                    value = "Rp ${formatRupiah(totalSalesRevenue)}",
                    subtitle = "${sales.size} transaksi",
                    containerColor = SurfaceWhite,
                    contentColor = Slate900,
                    borderColor = BorderDefault,
                    modifier = Modifier.weight(1f)
                )
                MonitorMetricCard(
                    title = "Beban & Kas Keluar",
                    value = "Rp ${formatRupiah(totalExpensesAmt)}",
                    subtitle = "Jasa lain: Rp ${formatRupiah(totalIncomesAmt)}",
                    containerColor = SurfaceWhite,
                    contentColor = Rose500,
                    borderColor = BorderDefault,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonitorMetricCard(
                    title = "Piutang Pelanggan",
                    value = "Rp ${formatRupiah(totalReceivables)}",
                    subtitle = "Belum dilunasi",
                    containerColor = SurfaceWhite,
                    contentColor = Amber500,
                    borderColor = BorderDefault,
                    modifier = Modifier.weight(1f)
                )
                MonitorMetricCard(
                    title = "Utang Pemasok",
                    value = "Rp ${formatRupiah(totalPayables)}",
                    subtitle = "Kewajiban toko",
                    containerColor = SurfaceWhite,
                    contentColor = Slate900,
                    borderColor = BorderDefault,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Saldo Per Rekening Kas
        item {
            Text(
                "Saldo Per Akun Kas",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (cashAccounts.isEmpty()) {
                        Box(modifier = Modifier.padding(20.dp)) {
                            DesignEmptyState(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Belum ada akun kas",
                                subtitle = "Tarik data dari cloud atau tambahkan akun di menu Multi Kas."
                            )
                        }
                    } else {
                        cashAccounts.forEachIndexed { idx, acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.accountName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                                    Text(acc.accountType, fontSize = 11.sp, color = Slate500)
                                }
                                Text(
                                    text = "Rp ${formatRupiah(acc.balance)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (acc.balance < 0) Rose500 else Emerald700
                                )
                            }
                            if (idx < cashAccounts.lastIndex) HorizontalDivider(color = BorderDefault)
                        }
                    }
                }
            }
        }

        // Mutasi Arus Kas Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mutasi Arus Kas Terbaru",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesignFilterChip(label = "Semua", selected = mutationFilter == "ALL", onClick = { mutationFilter = "ALL" })
                    DesignFilterChip(label = "Masuk", selected = mutationFilter == "IN", onClick = { mutationFilter = "IN" })
                    DesignFilterChip(label = "Keluar", selected = mutationFilter == "OUT", onClick = { mutationFilter = "OUT" })
                }
            }
        }

        if (filteredMutations.isEmpty()) {
            item {
                DesignEmptyState(
                    icon = Icons.Default.Sync,
                    title = "Belum ada mutasi arus kas",
                    subtitle = "Transaksi penjualan, pengeluaran, dan koreksi kas akan tampil otomatis di sini."
                )
            }
        } else {
            items(filteredMutations, key = { it.id }) { m ->
                val isPositive = m.amount >= 0
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPositive) Emerald50 else Rose100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isPositive) Emerald700 else Rose500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${m.accountName} • ${m.mutationType.replace("_", " ").uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = m.notes.ifBlank { m.refId.ifBlank { "-" } },
                                fontSize = 11.sp,
                                color = Slate600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatMonitorDate(m.timestamp),
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (isPositive) "+" else "-"}Rp ${formatRupiah(kotlin.math.abs(m.amount))}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isPositive) Emerald700 else Rose500
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MonitorMetricCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderColor?.let { BorderStroke(1.dp, it) },
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (containerColor == SurfaceWhite) Slate500 else contentColor.copy(alpha = 0.85f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (containerColor == SurfaceWhite) Slate400 else contentColor.copy(alpha = 0.78f),
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
