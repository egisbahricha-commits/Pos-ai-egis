package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PriceCheck
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AdminTierRangeEntity
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignStatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun TarifAdminScreen(viewModel: PosViewModel) {
    val tierServices by viewModel.tierServices.collectAsStateWithLifecycle()
    val allRanges by viewModel.allTierRanges.collectAsStateWithLifecycle()

    var selectedServiceId by remember(tierServices) {
        mutableStateOf(tierServices.firstOrNull()?.id ?: 0L)
    }

    var simulatorNominalText by remember { mutableStateOf("1500000") }
    var showAddServiceDialog by remember { mutableStateOf(false) }

    val activeRanges = remember(allRanges, selectedServiceId) {
        allRanges.filter { it.serviceId == selectedServiceId }.sortedBy { it.minAmount }
    }

    val simNominal = simulatorNominalText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val matchedSimRange = remember(activeRanges, simNominal) {
        if (simNominal <= 0.0) null
        else activeRanges.find { range ->
            simNominal >= range.minAmount && (range.maxAmount <= 0.0 || simNominal <= range.maxAmount)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("tarif_admin_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Service Filter Chips + Tambah Layanan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layanan transfer & PPOB",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Text(
                    text = "+ Layanan",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentBlue),
                    modifier = Modifier.clickable { showAddServiceDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tierServices, key = { it.id }) { service ->
                    DesignFilterChip(
                        label = if (service.isActive) service.serviceName else "${service.serviceName} (Nonaktif)",
                        selected = service.id == selectedServiceId,
                        onClick = { selectedServiceId = service.id }
                    )
                }
            }
            val selectedService = tierServices.find { it.id == selectedServiceId }
            if (selectedService != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Status Grup: ${selectedService.serviceName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Slate800)
                        )
                        Switch(
                            checked = selectedService.isActive,
                            onCheckedChange = { checked ->
                                viewModel.updateAdminTierService(selectedService.copy(isActive = checked))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                        )
                    }
                }
            }
        }

        // LIVE TARIF OTOMATIS SIMULATOR CARD (Matches Design System Photo)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DesignLabeledTextField(
                        label = "Nominal",
                        value = simulatorNominalText,
                        onValueChange = { simulatorNominalText = it.filter { ch -> ch.isDigit() } },
                        prefixText = "Rp ",
                        placeholder = "1500000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (simNominal > 0.0 && matchedSimRange == null) {
                        DesignStatusBadge(
                            text = "Nominal ini belum ada di tabel tarif. Isi manual.",
                            type = BadgeType.WARNING
                        )
                    } else if (matchedSimRange != null) {
                        val netProfit = matchedSimRange.storeAdminFee + matchedSimRange.agentCommission - matchedSimRange.providerAdminFee
                        val customerTotal = simNominal + matchedSimRange.storeAdminFee

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Admin toko", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp))
                                Text("Rp ${formatRupiah(matchedSimRange.storeAdminFee)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate900, fontSize = 13.sp))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Admin penyedia", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp))
                                Text("Rp ${formatRupiah(matchedSimRange.providerAdminFee)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate900, fontSize = 13.sp))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fee", style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp))
                                Text("Rp ${formatRupiah(matchedSimRange.agentCommission)}", style = MaterialTheme.typography.bodySmall.copy(color = Slate900, fontSize = 13.sp))
                            }
                            HorizontalDivider(color = BorderDefault, modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dibayar pelanggan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                Text("Rp ${formatRupiah(customerTotal)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Laba layanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700))
                                Text("Rp ${formatRupiah(netProfit)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700))
                            }
                        }
                    } else {
                        Text(
                            text = "Masukkan nominal untuk menghitung tarif admin & laba otomatis.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }

        // TABEL RENTANG TARIF SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tabel rentang tarif",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Text(
                    "${activeRanges.size} rentang",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
            }
        }

        if (activeRanges.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DesignEmptyState(
                        icon = Icons.Default.PriceCheck,
                        title = "Belum ada rentang tarif",
                        subtitle = "Tambahkan rentang nominal untuk menghitung admin otomatis.",
                        ctaText = "Tambah rentang",
                        onCtaClick = {
                            if (selectedServiceId > 0L) {
                                viewModel.createAdminTierRange(selectedServiceId, 0.0, 1000000.0, 5000.0, 5000.0, 2500.0)
                            }
                        }
                    )
                }
            }
        } else {
            itemsIndexed(activeRanges, key = { _, item -> item.id }) { index, range ->
                val prevRange = if (index > 0) activeRanges[index - 1] else null
                val isOverlapping = prevRange != null && (prevRange.maxAmount <= 0.0 || range.minAmount <= prevRange.maxAmount)
                val gapWarningText = if (!isOverlapping && prevRange != null && prevRange.maxAmount > 0.0 && range.minAmount > prevRange.maxAmount + 1.0) {
                    "Celah rentang: Rp ${formatRupiah(prevRange.maxAmount + 1.0)} s/d Rp ${formatRupiah(range.minAmount - 1.0)} belum diatur"
                } else null

                if (gapWarningText != null) {
                    DesignStatusBadge(
                        text = gapWarningText,
                        type = BadgeType.WARNING,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                InlineTarifRangeCard(
                    range = range,
                    isOverlapping = isOverlapping,
                    onUpdate = { updated -> viewModel.updateAdminTierRange(updated) },
                    onDelete = { viewModel.deleteAdminTierRange(range) }
                )
            }

            // Soft "+ Tambah rentang" Button (Matches Design System Photo)
            item {
                OutlinedButton(
                    onClick = {
                        if (selectedServiceId > 0L) {
                            val lastMax = activeRanges.maxOfOrNull { it.maxAmount } ?: 0.0
                            val nextMin = if (lastMax > 0.0) lastMax + 1.0 else 0.0
                            val nextMax = nextMin + 1000000.0 - 1.0
                            viewModel.createAdminTierRange(selectedServiceId, nextMin, nextMax, 5000.0, 5000.0, 2500.0)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDefault),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceStrong, contentColor = Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah rentang", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Service Dialog
    if (showAddServiceDialog) {
        Dialog(onDismissRequest = { showAddServiceDialog = false }) {
            var serviceName by remember { mutableStateOf("") }
            var showError by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tambah Layanan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                        IconButton(onClick = { showAddServiceDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Slate400)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DesignLabeledTextField(
                        label = "Nama layanan *",
                        value = serviceName,
                        onValueChange = {
                            serviceName = it
                            showError = false
                        },
                        placeholder = "mis. Top Up OVO / Bayar PDAM",
                        isError = showError,
                        errorMessage = "Nama layanan wajib diisi"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddServiceDialog = false },
                            border = BorderStroke(1.dp, BorderInput),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", color = Slate700)
                        }
                        Button(
                            onClick = {
                                if (serviceName.isBlank()) {
                                    showError = true
                                } else {
                                    viewModel.createAdminTierService(serviceName.trim(), serviceName.trim().uppercase().replace(" ", "_"))
                                    showAddServiceDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineTarifRangeCard(
    range: AdminTierRangeEntity,
    isOverlapping: Boolean,
    onUpdate: (AdminTierRangeEntity) -> Unit,
    onDelete: () -> Unit
) {
    var minText by remember(range.id, range.minAmount) { mutableStateOf(range.minAmount.toLong().toString()) }
    var maxText by remember(range.id, range.maxAmount) { mutableStateOf(if (range.maxAmount > 0.0) range.maxAmount.toLong().toString() else "") }
    var storeText by remember(range.id, range.storeAdminFee) { mutableStateOf(range.storeAdminFee.toLong().toString()) }
    var providerText by remember(range.id, range.providerAdminFee) { mutableStateOf(range.providerAdminFee.toLong().toString()) }
    var agentText by remember(range.id, range.agentCommission) { mutableStateOf(range.agentCommission.toLong().toString()) }

    val minVal = minText.toDoubleOrNull() ?: 0.0
    val maxVal = maxText.toDoubleOrNull() ?: 0.0
    val storeVal = storeText.toDoubleOrNull() ?: 0.0
    val providerVal = providerText.toDoubleOrNull() ?: 0.0
    val agentVal = agentText.toDoubleOrNull() ?: 0.0
    val netProfit = storeVal + agentVal - providerVal

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, if (isOverlapping) Rose500.copy(alpha = 0.45f) else BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignLabeledTextField(
                    label = "Dari",
                    value = minText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        minText = digits
                        onUpdate(range.copy(minAmount = digits.toDoubleOrNull() ?: 0.0))
                    },
                    prefixText = "Rp ",
                    isError = isOverlapping,
                    errorMessage = "Rentang tumpang tindih",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                DesignLabeledTextField(
                    label = "Sampai (kosong = seterusnya)",
                    value = maxText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        maxText = digits
                        onUpdate(range.copy(maxAmount = digits.toDoubleOrNull() ?: 0.0))
                    },
                    prefixText = "Rp ",
                    placeholder = "Tak terbatas",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignLabeledTextField(
                    label = "Admin toko",
                    value = storeText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        storeText = digits
                        onUpdate(range.copy(storeAdminFee = digits.toDoubleOrNull() ?: 0.0))
                    },
                    prefixText = "Rp ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                DesignLabeledTextField(
                    label = "Admin penyedia",
                    value = providerText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        providerText = digits
                        onUpdate(range.copy(providerAdminFee = digits.toDoubleOrNull() ?: 0.0))
                    },
                    prefixText = "Rp ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                DesignLabeledTextField(
                    label = "Fee",
                    value = agentText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        agentText = digits
                        onUpdate(range.copy(agentCommission = digits.toDoubleOrNull() ?: 0.0))
                    },
                    prefixText = "Rp ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laba per transaksi: Rp ${formatRupiah(netProfit)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        fontSize = 12.sp
                    )
                )
                Row(
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Hapus", style = MaterialTheme.typography.bodySmall.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp))
                }
            }
        }
    }
}
