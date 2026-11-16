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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DesignEmptyState
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesIncomesScreen(viewModel: PosViewModel) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME
    var showAddDialog by remember { mutableStateOf(false) }

    val totalExpenses = expenses.sumOf { it.amount }
    val totalIncomes = incomes.sumOf { it.amount }

    Scaffold(
        containerColor = BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_finance_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCanvas)
                .padding(innerPadding)
                .testTag("expenses_incomes_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Rose500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pengeluaran", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Rp ${formatRupiah(totalExpenses)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text("${expenses.size} catatan", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f)))
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald700),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pemasukan Jasa", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Rp ${formatRupiah(totalIncomes)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text("${incomes.size} catatan", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f)))
                        }
                    }
                }

                DesignSegmentedTabs(
                    options = listOf(
                        "EXPENSE" to "Pengeluaran",
                        "INCOME" to "Pemasukan Jasa"
                    ),
                    selectedKey = activeTab,
                    onSelect = { activeTab = it }
                )
            }
            HorizontalDivider(color = BorderDefault)

            if (activeTab == "EXPENSE") {
                if (expenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.MoneyOff,
                            title = "Belum ada pengeluaran",
                            subtitle = "Catat biaya listrik, sewa, perlengkapan, atau gaji.",
                            ctaText = "Catat pengeluaran",
                            onCtaClick = { showAddDialog = true }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(expenses, key = { it.id }) { exp ->
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
                                        Text(exp.category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                        Text(
                                            "${SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(exp.date))} • ${exp.paymentSource}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                        )
                                        if (exp.notes.isNotBlank()) {
                                            Text(exp.notes, style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                        }
                                    }
                                    Text(
                                        "-Rp ${formatRupiah(exp.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose500)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (incomes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        DesignEmptyState(
                            icon = Icons.Default.Paid,
                            title = "Belum ada pemasukan jasa",
                            subtitle = "Catat pemasukan non-penjualan atau komisi layanan.",
                            ctaText = "Catat pemasukan",
                            onCtaClick = { showAddDialog = true }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(incomes, key = { it.id }) { inc ->
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
                                        Text(inc.source, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                        Text(
                                            SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(inc.date)),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                        )
                                        if (inc.notes.isNotBlank()) {
                                            Text(inc.notes, style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                        }
                                    }
                                    Text(
                                        "+Rp ${formatRupiah(inc.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Emerald700)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFinanceDialog(
            isExpense = activeTab == "EXPENSE",
            onDismiss = { showAddDialog = false },
            onSave = { categoryOrSource, amount, notes, source ->
                if (activeTab == "EXPENSE") {
                    viewModel.recordExpense(categoryOrSource, amount, notes, source)
                } else {
                    viewModel.recordIncome(categoryOrSource, amount, notes)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddFinanceDialog(
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onSave: (cat: String, amount: Double, notes: String, src: String) -> Unit
) {
    val expenseCategories = listOf("Listrik", "Air", "Transportasi", "Konsumsi", "Perlengkapan", "ATK", "Perawatan", "Operasional")
    val incomeCategories = listOf("Jasa Transfer", "Komisi Penjualan", "Jasa Titip", "Pendapatan Lain")

    var selectedCategory by remember { mutableStateOf(if (isExpense) expenseCategories.first() else incomeCategories.first()) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paymentSource by remember { mutableStateOf("Kas Toko") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (isExpense) "Catat Pengeluaran Baru" else "Catat Pemasukan Jasa",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Kategori", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(if (isExpense) expenseCategories else incomeCategories) { cat ->
                        DesignFilterChip(
                            label = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DesignLabeledTextField(
                    label = "Nominal *",
                    value = amountText,
                    onValueChange = { amountText = it },
                    prefixText = "Rp ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                DesignLabeledTextField(
                    label = "Keterangan",
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onSave(selectedCategory, amt, notes, paymentSource)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
