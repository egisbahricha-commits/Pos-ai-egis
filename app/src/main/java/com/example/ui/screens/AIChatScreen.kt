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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.PosViewModel

@Composable
fun AIChatScreen(viewModel: PosViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAILoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val samplePrompts = listOf(
        "Baru belanja di grosir aqua 2 dus harga 45 ribu, indomie goreng 5 karton harga 110 ribu",
        "Tambahkan produk baru Rinso cair 800 ml, harga modal 14 ribu, harga jual 18 ribu",
        "Hari ini pengeluaran listrik 250 ribu",
        "Tambah stok Aqua 600 ml sebanyak 20 botol",
        "Ada pemasukan tambahan 100 ribu dari jasa transfer",
        "Stok Aqua sekarang berapa?",
        "Berapa utang saya ke grosir?",
        "Berapa total pembelian minggu ini?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .testTag("ai_chat_screen")
    ) {
        Surface(
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Command Center • Bahasa sehari-hari & koreksi typo",
                    style = MaterialTheme.typography.labelMedium.copy(color = Slate700, fontWeight = FontWeight.SemiBold)
                )
            }
        }
        HorizontalDivider(color = BorderDefault)

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    UserMessageBubble(msg)
                } else {
                    AIMessageBubble(
                        message = msg,
                        onConfirmAction = { viewModel.confirmAIDraft(msg.id) },
                        onRejectAction = { viewModel.rejectAIDraft(msg.id) }
                    )
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gerai Una AI sedang menganalisis perintah...", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samplePrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.clickable { inputText = prompt }
                ) {
                    Text(
                        prompt,
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate700),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalDivider(color = BorderDefault)
        Surface(
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ketik perintah toko apa saja...", style = MaterialTheme.typography.bodyMedium.copy(color = Slate400)) },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderInput
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendUserChatMessage(text)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                        .testTag("ai_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun UserMessageBubble(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp),
            color = AccentBlue,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun AIMessageBubble(
    message: ChatMessage,
    onConfirmAction: () -> Unit,
    onRejectAction: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ModuleKasir),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Card(
                shape = RoundedCornerShape(2.dp, 14.dp, 14.dp, 14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderDefault)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate900
                    )

                    if (message.isActionPending && message.draftResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BgCanvas),
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Konfirmasi Tindakan Toko",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Intent: ${message.draftResult.intent.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onRejectAction,
                                        border = BorderStroke(1.dp, BorderInput),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Batal", color = Slate700)
                                    }

                                    Button(
                                        onClick = onConfirmAction,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.5f).testTag("confirm_ai_action_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Konfirmasi", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
