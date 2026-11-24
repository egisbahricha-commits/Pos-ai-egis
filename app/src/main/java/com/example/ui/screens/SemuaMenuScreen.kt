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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen

data class KasLiteMenuCategory(
    val title: String,
    val items: List<KasLiteMenuItem>
)

data class KasLiteMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val destination: Screen
)

@Composable
fun SemuaMenuScreen(viewModel: PosViewModel) {
    val menuPermissions by viewModel.menuPermissions.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()

    val allMenuGroups = listOf(
        KasLiteMenuCategory(
            title = "PENJUALAN & TRANSAKSI",
            items = listOf(
                KasLiteMenuItem("Kasir POS", "Buka kasir & scan barang", Icons.Default.PointOfSale, Sapphire800, Screen.POS_KASIR),
                KasLiteMenuItem("Riwayat Transaksi", "Struk selesai & retur", Icons.AutoMirrored.Filled.ReceiptLong, Emerald700, Screen.RIWAYAT_TRANSAKSI),
                KasLiteMenuItem("Shift Kasir", "Buka/tutup shift & cek kas laci kasir", Icons.Default.AccountBalanceWallet, Sapphire600, Screen.POS_KASIR)
            )
        ),
        KasLiteMenuCategory(
            title = "PRODUK & KATALOG",
            items = listOf(
                KasLiteMenuItem("Kelola Produk Manual", "Tambah, ubah harga, stok & barcode item", Icons.Default.Edit, ModuleProduk, Screen.MANAJEMEN_PRODUK),
                KasLiteMenuItem("Daftar Produk", "Katalog lengkap, paket & promo", Icons.Default.Category, Sapphire800, Screen.PRODUK),
                KasLiteMenuItem("Kategori & Satuan", "Kelola satuan & konversi", Icons.Default.Store, Sapphire600, Screen.PRODUK),
                KasLiteMenuItem("Barcode Scanner", "Scan kamera langsung masuk keranjang", Icons.Default.QrCodeScanner, Amber500, Screen.SCAN_BARCODE)
            )
        ),
        KasLiteMenuCategory(
            title = "PEMBELIAN & SUPLAI",
            items = listOf(
                KasLiteMenuItem("Pembelian Toko", "Faktur belanja ke grosir", Icons.Default.ShoppingBag, Sapphire800, Screen.PEMBELIAN),
                KasLiteMenuItem("Penerimaan Barang", "Catat kulakan (+stok otomatis)", Icons.Default.LocalShipping, Emerald700, Screen.PEMBELIAN),
                KasLiteMenuItem("Utang Pemasok", "Kelola utang belanja tempo", Icons.Default.CreditCard, Rose500, Screen.UTANG_PIUTANG)
            )
        ),
        KasLiteMenuCategory(
            title = "INVENTARIS & GUDANG",
            items = listOf(
                KasLiteMenuItem("Stok Barang", "Status & nilai persediaan fisik", Icons.Default.Inventory, Sapphire800, Screen.STOK_INVENTARIS),
                KasLiteMenuItem("Stok Opname", "Penyesuaian stok riil", Icons.Default.Edit, Amber500, Screen.STOK_INVENTARIS),
                KasLiteMenuItem("Kartu Stok", "Riwayat mutasi keluar/masuk", Icons.Default.History, Sapphire600, Screen.STOK_INVENTARIS)
            )
        ),
        KasLiteMenuCategory(
            title = "KEUANGAN & MULTI KAS",
            items = listOf(
                KasLiteMenuItem("Multi Kas & Saldo Bank", "Kas Toko, Pulsa, Saldo BRILink", Icons.Default.AccountBalanceWallet, Sapphire800, Screen.MULTI_KAS),
                KasLiteMenuItem("Tarif Admin Transfer & PPOB", "Tabel biaya admin & fee agen", Icons.Default.CreditCard, HeaderTeal, Screen.TARIF_ADMIN),
                KasLiteMenuItem("Arus Kas Toko", "Kas masuk vs kas keluar", Icons.Default.AccountBalance, Sapphire800, Screen.KEUANGAN),
                KasLiteMenuItem("Biaya Operasional", "Listrik, air & biaya usaha", Icons.Default.MoneyOff, Rose500, Screen.PENGELUARAN),
                KasLiteMenuItem("Pendapatan Jasa", "Pemasukan non-penjualan", Icons.Default.Paid, Emerald700, Screen.PENGELUARAN),
                KasLiteMenuItem("Piutang Pelanggan", "Daftar bon tempo pelanggan", Icons.Default.CreditCard, Amber500, Screen.UTANG_PIUTANG)
            )
        ),
        KasLiteMenuCategory(
            title = "LAPORAN & ANALITIK",
            items = listOf(
                KasLiteMenuItem("Laporan Penjualan", "Ringkasan omzet & faktur", Icons.Default.BarChart, Sapphire800, Screen.LAPORAN),
                KasLiteMenuItem("Laporan Laba Rugi", "Margin & laba bersih riil", Icons.Default.TrendingUp, Emerald700, Screen.LAPORAN),
                KasLiteMenuItem("Kirim Laporan WA", "Bagikan rekap ke WhatsApp", Icons.Default.Sms, Emerald700, Screen.LAPORAN)
            )
        ),
        KasLiteMenuCategory(
            title = "AI ASSISTANT & WHATSAPP",
            items = listOf(
                KasLiteMenuItem("AI Chat KASLITE", "Asisten toko cerdas", Icons.Default.AutoAwesome, Emerald700, Screen.AI_CHAT),
                KasLiteMenuItem("WhatsApp Integration", "Two-way command & webhook", Icons.Default.Chat, Sapphire800, Screen.WHATSAPP)
            )
        ),
        KasLiteMenuCategory(
            title = "SISTEM & PENGATURAN",
            items = listOf(
                KasLiteMenuItem("Kontak Pelanggan & Grosir", "Buku alamat pelanggan", Icons.Default.People, Sapphire800, Screen.KONTAK),
                KasLiteMenuItem("Donasi ke Pengembang", "Paket dukungan Rp 10rb, Rp 50rb, & Rp 250rb Selamanya", Icons.Default.Favorite, Emerald700, Screen.DONASI_PENGEMBANG),
                KasLiteMenuItem("Laporan Bug", "Laporkan error atau kendala fitur ke pengembang", Icons.Default.BugReport, Rose500, Screen.LAPOR_BUG),
                KasLiteMenuItem("Pengaturan & Hak Akses", "Profil toko, backup & role kasir", Icons.Default.Settings, Slate700, Screen.PENGATURAN)
            )
        )
    )

    val visibleGroups = allMenuGroups.mapNotNull { group ->
        val permitted = group.items.filter { viewModel.hasMenuPermission(it.destination) }
        if (permitted.isEmpty()) null else group.copy(items = permitted)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("semua_menu_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Semua Menu", tint = ModulePengaturan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Semua Menu & Modul",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        Text(
                            "Navigasi lengkap seluruh fitur POS & bisnis toko",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
            }
        }

        items(visibleGroups, key = { it.title }) { group ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        group.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.navigateTo(item.destination) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceStrong),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = item.title, tint = item.color, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
