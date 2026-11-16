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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignFilterChip
import com.example.ui.components.DesignLabeledTextField
import com.example.ui.components.DesignStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@Composable
fun WhatsAppScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val testReply by viewModel.whatsAppTestReply.collectAsStateWithLifecycle()

    var senderPhone by remember { mutableStateOf("081234567890") }
    var incomingMessage by remember { mutableStateOf("Baru belanja di grosir, aqua 2 dus harga 45 ribu, indomie goreng 5 karton harga 110 ribu") }
    var sharePhone by remember { mutableStateOf("081234567890") }
    var shareText by remember { mutableStateOf("Halo, ini ringkasan penjualan toko hari ini dari Gerai Una Smart POS AI!") }

    val presets = listOf(
        "Belanja Aqua & Indomie" to "Baru belanja di grosir aqua 2 dus harga 45 ribu, indomie goreng 5 karton harga 110 ribu",
        "Tambah Produk Rinso" to "Tambahkan produk baru Rinso cair 800 ml, harga modal 14 ribu, harga jual 18 ribu",
        "Pengeluaran Listrik 250rb" to "Hari ini pengeluaran listrik 250 ribu",
        "Cek Stok Aqua" to "Stok Aqua sekarang berapa?",
        "Cek Utang Grosir" to "Berapa utang saya ke grosir?"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("whatsapp_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Integration Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceStrong),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("WhatsApp Business Platform", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                                Text("Two-Way Command System & Webhook", style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
                            }
                        }

                        DesignStatusBadge(text = "Siap", type = BadgeType.SUCCESS)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Alur: HP Pemilik Toko ➔ WhatsApp Webhook ➔ Gerai Una NLU ➔ Validasi Database ➔ Balasan Otomatis.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                }
            }
        }

        // Owner Whitelist Security Info
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Sapphire50),
                border = BorderStroke(1.dp, Sapphire200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Otentikasi Nomor Pemilik Terdaftar",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                        )
                        Text(
                            "Nomor terotorisasi: 081234567890 (Egis Bahricha - Owner). Nomor asing otomatis ditolak.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                        )
                    }
                }
            }
        }

        item {
            Text("Simulasi perintah WhatsApp", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        DesignLabeledTextField(
                            label = "No. HP Pengirim",
                            value = senderPhone,
                            onValueChange = { senderPhone = it },
                            modifier = Modifier.weight(1.5f)
                        )

                        Button(
                            onClick = { senderPhone = "081234567890" },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Owner", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { senderPhone = "089988776655" },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                            border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Asing", fontWeight = FontWeight.Bold)
                        }
                    }

                    Column {
                        Text("Template Pesan", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presets) { (label, text) ->
                                DesignFilterChip(
                                    label = label,
                                    selected = incomingMessage == text,
                                    onClick = { incomingMessage = text }
                                )
                            }
                        }
                    }

                    DesignLabeledTextField(
                        label = "Teks Pesan WhatsApp Masuk",
                        value = incomingMessage,
                        onValueChange = { incomingMessage = it },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().testTag("wa_simulator_input")
                    )

                    Button(
                        onClick = { viewModel.simulateIncomingWhatsApp(senderPhone, incomingMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("simulate_wa_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proses Pesan Masuk", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Simulation Result Display
        if (testReply != null) {
            val r = testReply!!
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (r.isAuthorized) Emerald50 else Rose100.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, if (r.isAuthorized) Emerald200 else Rose500.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (r.isAuthorized) "Otorisasi Berhasil: Balasan Terkirim" else "Akses Ditolak (Nomor Asing)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (r.isAuthorized) Emerald700 else Rose500
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = r.replyText,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Slate900),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp Share Intent Section
        item {
            Text("Bagikan langsung ke WhatsApp", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesignLabeledTextField(
                        label = "Nomor Tujuan",
                        value = sharePhone,
                        onValueChange = { sharePhone = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DesignLabeledTextField(
                        label = "Teks Pesan",
                        value = shareText,
                        onValueChange = { shareText = it },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.whatsAppCommandCenter.shareViaWhatsApp(context, shareText, sharePhone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka WhatsApp & Kirim", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
