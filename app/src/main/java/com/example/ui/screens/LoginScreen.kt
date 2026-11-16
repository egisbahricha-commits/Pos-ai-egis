package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.GoogleAuthHelper
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()

    var storeName by remember(authState.storeName) {
        mutableStateOf(authState.storeName.ifBlank { "GERAI UNA SMART POS" })
    }
    var showLocalUserPicker by remember { mutableStateOf(false) }

    LaunchedEffect(authState.authInfoMessage) {
        if (!authState.hasEnteredApp && !authState.isOfflineMode && authState.authInfoMessage == null) {
            val silentPayload = GoogleAuthHelper.trySilentGoogleSignIn(context)
            if (silentPayload != null) {
                viewModel.signInWithGooglePayload(silentPayload, storeName)
            }
        }
    }

    val launchInteractiveGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            viewModel.setAuthLoading(true)
            val result = GoogleAuthHelper.signInWithGoogle(context)
            result.onSuccess { payload ->
                viewModel.signInWithGooglePayload(payload, storeName)
            }.onFailure { err ->
                viewModel.setAuthError(err.localizedMessage ?: "Gagal masuk dengan akun Google.")
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .testTag("login_screen")
    ) {
        val isWide = maxWidth >= 780.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Brand & Firebase Cloud Showcase Panel (Tablet)
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .background(
                            Brush.linearGradient(
                                listOf(Sapphire900, AccentBlue, ModuleKasir)
                            )
                        )
                        .padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = "Logo Gerai Una",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "GERAI UNA SMART POS AI",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Masuk dengan akun Google (Firebase) untuk menyinkronkan penjualan, stok, dan memantau arus kas toko secara real-time dari perangkat mana saja.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.88f),
                                    lineHeight = 22.sp
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrandFeatureRow(
                                icon = Icons.Default.Devices,
                                title = "Login Multi-Perangkat Instan",
                                subtitle = "Pakai akun Google yang sama di HP owner dan tablet kasir"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Pantau Arus Kas Real-Time",
                                subtitle = "Saldo kas toko, BRILink, pulsa, pemasukan & pengeluaran di Firestore"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.Security,
                                title = "Keamanan Zero-Trust Firebase",
                                subtitle = "Data toko terisolasi per akun Google & tetap lancar offline"
                            )
                        }

                        Text(
                            "Creator & Architect: EGIS BAHRICHA",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        )
                    }
                }

                // Right Login Card
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 460.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            FirebaseLoginFormCard(
                                storeName = storeName,
                                onStoreNameChange = { storeName = it },
                                isLoading = authState.isAuthLoading,
                                lastEmail = authState.lastEmail,
                                authError = authState.authError,
                                authInfo = authState.authInfoMessage,
                                onGoogleSignInClick = launchInteractiveGoogleSignIn,
                                onOfflineClick = { viewModel.enterOfflineMode() },
                                onLocalAccountClick = { showLocalUserPicker = true }
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile Vertical Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(Sapphire900, AccentBlue, ModuleKasir)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PointOfSale,
                                            contentDescription = "Gerai Una POS",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "GERAI UNA SMART POS AI",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            "By Egis Bahricha • Firebase Cloud & Offline POS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Masuk dengan Google (Firebase) untuk sinkronisasi transaksi otomatis & pantau arus kas dari HP lain.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.92f),
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                    ) {
                        FirebaseLoginFormCard(
                            storeName = storeName,
                            onStoreNameChange = { storeName = it },
                            isLoading = authState.isAuthLoading,
                            lastEmail = authState.lastEmail,
                            authError = authState.authError,
                            authInfo = authState.authInfoMessage,
                            onGoogleSignInClick = launchInteractiveGoogleSignIn,
                            onOfflineClick = { viewModel.enterOfflineMode() },
                            onLocalAccountClick = { showLocalUserPicker = true }
                        )
                    }
                }
            }
        }
    }

    if (showLocalUserPicker) {
        Dialog(onDismissRequest = { showLocalUserPicker = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pilih Akun Kasir Lokal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        "Masuk tanpa sinkronisasi cloud menggunakan profil kasir di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                    userAccounts.forEach { user ->
                        Surface(
                            onClick = {
                                viewModel.enterOfflineMode(user)
                                showLocalUserPicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = BgCanvas,
                            border = BorderStroke(1.dp, BorderDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                                    Text("@${user.username} • ${user.role.uppercase()}", color = Slate500, fontSize = 12.sp)
                                }
                                DesignStatusBadge(text = "Masuk", type = BadgeType.INFO)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showLocalUserPicker = false }) { Text("Tutup", color = Slate600) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.78f)))
        }
    }
}

@Composable
private fun FirebaseLoginFormCard(
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    isLoading: Boolean,
    lastEmail: String,
    authError: String?,
    authInfo: String?,
    onGoogleSignInClick: () -> Unit,
    onOfflineClick: () -> Unit,
    onLocalAccountClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Firebase Cloud Status Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald50,
                border = BorderStroke(1.dp, Emerald200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase Auth & Cloud Firestore Siap",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        )
                        Text(
                            text = "Login satu ketukan dengan akun Google yang sama di perangkat lain untuk memantau arus kas.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Slate700
                            )
                        )
                    }
                }
            }

            if (lastEmail.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Sapphire50,
                    border = BorderStroke(1.dp, Sapphire200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Akun terakhir: $lastEmail",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Sapphire800,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text("Nama Toko (Opsional untuk setup baru)") },
                placeholder = { Text("Contoh: GERAI UNA SMART POS") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Slate900),
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = Slate500) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = loginFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_storename_input")
            )

            if (authError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Rose100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        color = Rose500,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (authInfo != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Sapphire50,
                    border = BorderStroke(1.dp, Sapphire200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authInfo,
                        color = Sapphire800,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_signin_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menghubungkan ke Firebase...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Masuk dengan Google (Firebase)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BgCanvas,
                border = BorderStroke(1.dp, BorderDefault),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = ModuleKasir, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Otomatis sinkronkan kasir, produk, penjualan & mutasi arus kas ke Firestore tanpa setup SQL.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 11.sp)
                    )
                }
            }

            HorizontalDivider(color = BorderDefault)

            Text(
                text = "Atau lanjutkan tanpa akun cloud:",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOfflineClick,
                    border = BorderStroke(1.dp, BorderInput),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("offline_mode_button")
                ) {
                    Text(
                        "Mode Offline",
                        color = Slate800,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onLocalAccountClick,
                    border = BorderStroke(1.dp, BorderInput),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("local_account_button")
                ) {
                    Text(
                        "Pilih Kasir Lokal",
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = BorderInput,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    disabledContainerColor = SurfaceStrong,
    focusedTextColor = Slate900,
    unfocusedTextColor = Slate900,
    disabledTextColor = Slate500,
    cursorColor = AccentBlue,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = Slate600,
    focusedPlaceholderColor = Slate400,
    unfocusedPlaceholderColor = Slate400,
    focusedLeadingIconColor = AccentBlue,
    unfocusedLeadingIconColor = Slate500,
    focusedTrailingIconColor = Slate600,
    unfocusedTrailingIconColor = Slate500
)
