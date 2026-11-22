package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.UserAccountEntity
import com.example.firebase.GoogleAuthHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()

    val isFirstTimeSetup = userAccounts.isEmpty()

    // Setup Admin Utama state
    var setupStoreName by remember(storeProfile?.name) {
        mutableStateOf(
            storeProfile?.name?.takeIf { it.isNotBlank() && it != "Toko Saya" } ?: ""
        )
    }
    var setupFullName by remember { mutableStateOf("") }
    var setupUsername by remember { mutableStateOf("admin") }
    var setupPin by remember { mutableStateOf("") }
    var setupConfirmPin by remember { mutableStateOf("") }

    // Login user state
    var loginUsername by remember(userAccounts) {
        mutableStateOf(
            userAccounts.firstOrNull { it.isActive }?.username ?: ""
        )
    }
    var loginPin by remember { mutableStateOf("") }

    // Secret 7-tap trigger for Super Admin
    var logoTapCount by remember { mutableStateOf(0) }
    var showSecretAdminDialog by remember { mutableStateOf(false) }
    var secretEmailInput by remember { mutableStateOf("") }
    var secretError by remember { mutableStateOf<String?>(null) }

    val launchInteractiveGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            viewModel.setAuthLoading(true)
            val result = GoogleAuthHelper.signInWithGoogle(context)
            result.onSuccess { payload ->
                viewModel.signInWithGooglePayload(payload, setupStoreName.ifBlank { storeProfile?.name ?: "" })
            }.onFailure { err ->
                viewModel.setAuthError(err.localizedMessage ?: "Gagal masuk dengan akun Google.")
            }
        }
    }

    val displayBrandTitle = storeProfile?.name?.takeIf { it.isNotBlank() } ?: "Kasir POS"

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
                // Left Brand Showcase Panel (Tablet)
                Box(
                    modifier = Modifier
                        .weight(0.42f)
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
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        logoTapCount++
                                        if (logoTapCount >= 7) {
                                            logoTapCount = 0
                                            showSecretAdminDialog = true
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = "Logo Aplikasi Kasir",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = displayBrandTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isFirstTimeSetup)
                                    "Siapkan akun Admin Utama terlebih dahulu sebelum mulai mencatat produk, stok, dan transaksi toko."
                                else
                                    "Masuk menggunakan akun Admin Utama atau akun pengguna/kasir yang telah dibuat oleh Admin Utama.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.88f),
                                    lineHeight = 22.sp
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrandFeatureRow(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Kontrol Admin Utama & Staf",
                                subtitle = "Admin Utama mengelola akun kasir, PIN akses, dan batasan tiap menu"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Bersih & Siap Pakai",
                                subtitle = "Tanpa data contoh bawaan — semua produk, kas, dan laporan dimulai dari data toko Anda"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.Devices,
                                title = "Dukungan Cloud Firebase Opsional",
                                subtitle = "Hubungkan akun Google kapan saja untuk sinkronisasi multi-perangkat"
                            )
                        }

                        Text(
                            text = "Sistem Point of Sale Terintegrasi",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        )
                    }
                }

                // Right Login / Setup Card
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 480.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            AuthContentCard(
                                isFirstTimeSetup = isFirstTimeSetup,
                                setupStoreName = setupStoreName,
                                onSetupStoreNameChange = {
                                    setupStoreName = it
                                    viewModel.clearAuthMessages()
                                },
                                setupFullName = setupFullName,
                                onSetupFullNameChange = {
                                    setupFullName = it
                                    viewModel.clearAuthMessages()
                                },
                                setupUsername = setupUsername,
                                onSetupUsernameChange = {
                                    setupUsername = it.filter { ch -> !ch.isWhitespace() }
                                    viewModel.clearAuthMessages()
                                },
                                setupPin = setupPin,
                                onSetupPinChange = {
                                    setupPin = it.filter { ch -> ch.isDigit() }.take(6)
                                    viewModel.clearAuthMessages()
                                },
                                setupConfirmPin = setupConfirmPin,
                                onSetupConfirmPinChange = {
                                    setupConfirmPin = it.filter { ch -> ch.isDigit() }.take(6)
                                    viewModel.clearAuthMessages()
                                },
                                onSubmitAdminSetup = {
                                    viewModel.setupMainAdmin(
                                        storeName = setupStoreName,
                                        fullName = setupFullName,
                                        username = setupUsername,
                                        pin = setupPin,
                                        confirmPin = setupConfirmPin
                                    )
                                },
                                userAccounts = userAccounts,
                                loginUsername = loginUsername,
                                onLoginUsernameChange = {
                                    loginUsername = it.filter { ch -> !ch.isWhitespace() }
                                    viewModel.clearAuthMessages()
                                },
                                loginPin = loginPin,
                                onLoginPinChange = {
                                    loginPin = it.filter { ch -> ch.isDigit() }.take(6)
                                    viewModel.clearAuthMessages()
                                },
                                onSubmitLogin = {
                                    viewModel.loginWithLocalCredentials(loginUsername, loginPin)
                                },
                                isLoading = authState.isAuthLoading,
                                lastEmail = authState.lastEmail,
                                authError = authState.authError,
                                authInfo = authState.authInfoMessage,
                                onGoogleSignInClick = launchInteractiveGoogleSignIn
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
                            .widthIn(max = 480.dp)
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
                                            .background(Color.White.copy(alpha = 0.16f))
                                            .clickable {
                                                logoTapCount++
                                                if (logoTapCount >= 7) {
                                                    logoTapCount = 0
                                                    showSecretAdminDialog = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PointOfSale,
                                            contentDescription = "Logo Kasir",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = displayBrandTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = if (isFirstTimeSetup)
                                                "Aktivasi Awal Admin Utama"
                                            else
                                                "Autentikasi Pengguna Wajib",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFirstTimeSetup)
                                        "Buat akun Admin Utama untuk mulai menggunakan kasir. Anda juga dapat menambahkan akun kasir nanti di Pengaturan."
                                    else
                                        "Silakan login dengan Username & PIN akun yang dibuat oleh Admin Utama atau masuk via Google (Firebase).",
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
                            .widthIn(max = 480.dp)
                    ) {
                        AuthContentCard(
                            isFirstTimeSetup = isFirstTimeSetup,
                            setupStoreName = setupStoreName,
                            onSetupStoreNameChange = {
                                setupStoreName = it
                                viewModel.clearAuthMessages()
                            },
                            setupFullName = setupFullName,
                            onSetupFullNameChange = {
                                setupFullName = it
                                viewModel.clearAuthMessages()
                            },
                            setupUsername = setupUsername,
                            onSetupUsernameChange = {
                                setupUsername = it.filter { ch -> !ch.isWhitespace() }
                                viewModel.clearAuthMessages()
                            },
                            setupPin = setupPin,
                            onSetupPinChange = {
                                setupPin = it.filter { ch -> ch.isDigit() }.take(6)
                                viewModel.clearAuthMessages()
                            },
                            setupConfirmPin = setupConfirmPin,
                            onSetupConfirmPinChange = {
                                setupConfirmPin = it.filter { ch -> ch.isDigit() }.take(6)
                                viewModel.clearAuthMessages()
                            },
                            onSubmitAdminSetup = {
                                viewModel.setupMainAdmin(
                                    storeName = setupStoreName,
                                    fullName = setupFullName,
                                    username = setupUsername,
                                    pin = setupPin,
                                    confirmPin = setupConfirmPin
                                )
                            },
                            userAccounts = userAccounts,
                            loginUsername = loginUsername,
                            onLoginUsernameChange = {
                                loginUsername = it.filter { ch -> !ch.isWhitespace() }
                                viewModel.clearAuthMessages()
                            },
                            loginPin = loginPin,
                            onLoginPinChange = {
                                loginPin = it.filter { ch -> ch.isDigit() }.take(6)
                                viewModel.clearAuthMessages()
                            },
                            onSubmitLogin = {
                                viewModel.loginWithLocalCredentials(loginUsername, loginPin)
                            },
                            isLoading = authState.isAuthLoading,
                            lastEmail = authState.lastEmail,
                            authError = authState.authError,
                            authInfo = authState.authInfoMessage,
                            onGoogleSignInClick = launchInteractiveGoogleSignIn
                        )
                    }
                }
            }
        }

        if (showSecretAdminDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSecretAdminDialog = false
                    secretEmailInput = ""
                    secretError = null
                },
                title = { Text("Login Rahasia Admin Pusat") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Masukkan email resmi Admin Pusat (egisbahricha@gmail.com):", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = secretEmailInput,
                            onValueChange = { secretEmailInput = it; secretError = null },
                            label = { Text("Email Super Admin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (secretError != null) {
                            Text(secretError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (secretEmailInput.trim().equals("egisbahricha@gmail.com", ignoreCase = true)) {
                            showSecretAdminDialog = false
                            secretEmailInput = ""
                            secretError = null
                            viewModel.loginAsSuperAdmin()
                        } else {
                            secretError = "Email Admin Pusat tidak valid."
                        }
                    }) {
                        Text("Masuk")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSecretAdminDialog = false
                        secretEmailInput = ""
                        secretError = null
                    }) {
                        Text("Batal")
                    }
                }
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthContentCard(
    isFirstTimeSetup: Boolean,
    setupStoreName: String,
    onSetupStoreNameChange: (String) -> Unit,
    setupFullName: String,
    onSetupFullNameChange: (String) -> Unit,
    setupUsername: String,
    onSetupUsernameChange: (String) -> Unit,
    setupPin: String,
    onSetupPinChange: (String) -> Unit,
    setupConfirmPin: String,
    onSetupConfirmPinChange: (String) -> Unit,
    onSubmitAdminSetup: () -> Unit,
    userAccounts: List<UserAccountEntity>,
    loginUsername: String,
    onLoginUsernameChange: (String) -> Unit,
    loginPin: String,
    onLoginPinChange: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    isLoading: Boolean,
    lastEmail: String,
    authError: String?,
    authInfo: String?,
    onGoogleSignInClick: () -> Unit
) {
    var showPinText by remember { mutableStateOf(false) }
    var showConfirmPinText by remember { mutableStateOf(false) }
    val pinFocusRequester = remember { FocusRequester() }

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
            if (isFirstTimeSetup) {
                // First-time Admin Utama Setup Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Sapphire50,
                    border = BorderStroke(1.dp, Sapphire200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Admin Utama (Penggunaan Pertama)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Sapphire800
                                )
                            )
                            Text(
                                text = "Belum ada pengguna terdaftar. Buat akun Admin Utama dengan PIN pribadi Anda.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = setupStoreName,
                    onValueChange = onSetupStoreNameChange,
                    label = { Text("Nama Toko") },
                    placeholder = { Text("Contoh: Toko Berkah Jaya") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_store_name_input")
                )

                OutlinedTextField(
                    value = setupFullName,
                    onValueChange = onSetupFullNameChange,
                    label = { Text("Nama Lengkap Admin Utama") },
                    placeholder = { Text("Contoh: Budi Santoso") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_name_input")
                )

                OutlinedTextField(
                    value = setupUsername,
                    onValueChange = onSetupUsernameChange,
                    label = { Text("Username Admin Utama") },
                    placeholder = { Text("Contoh: admin") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_username_input")
                )

                OutlinedTextField(
                    value = setupPin,
                    onValueChange = onSetupPinChange,
                    label = { Text("Buat PIN 6 Digit Angka") },
                    placeholder = { Text("6 digit angka") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPinText = !showPinText },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPinText) "Sembunyikan PIN" else "Lihat PIN"
                            )
                        }
                    },
                    visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_pin_input")
                )

                OutlinedTextField(
                    value = setupConfirmPin,
                    onValueChange = onSetupConfirmPinChange,
                    label = { Text("Konfirmasi PIN 6 Digit") },
                    placeholder = { Text("Ulangi 6 digit PIN") },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showConfirmPinText = !showConfirmPinText },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                if (showConfirmPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPinText) "Sembunyikan PIN" else "Lihat PIN"
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPinText) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isLoading) onSubmitAdminSetup() }),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_confirm_pin_input")
                )

                AuthMessageBanners(authError = authError, authInfo = authInfo)

                Button(
                    onClick = onSubmitAdminSetup,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_setup_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyiapkan Akun...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buat Admin Utama & Masuk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                // Existing Users: Mandatory Credential Login
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Masuk dengan Akun Terdaftar",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald800
                                )
                            )
                            Text(
                                text = "Gunakan akun Admin Utama atau user yang telah dibuat oleh Admin Utama.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            )
                        }
                    }
                }

                val activeUsers = remember(userAccounts) { userAccounts.filter { it.isActive } }
                if (activeUsers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pilih cepat username (tetap wajib masukkan PIN):",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.Medium)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeUsers.forEach { user ->
                                val selected = loginUsername.equals(user.username, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onLoginUsernameChange(user.username)
                                        onLoginPinChange("")
                                        pinFocusRequester.requestFocus()
                                    },
                                    label = {
                                        Text(
                                            text = "${user.name} (${user.role.uppercase()})",
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Sapphire100,
                                        selectedLabelColor = Sapphire900
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("login_user_chip_${user.username}")
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = loginUsername,
                    onValueChange = onLoginUsernameChange,
                    label = { Text("Username") },
                    placeholder = { Text("Masukkan username akun") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                OutlinedTextField(
                    value = loginPin,
                    onValueChange = onLoginPinChange,
                    label = { Text("PIN (6 Digit)") },
                    placeholder = { Text("Masukkan 6 digit PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPinText = !showPinText },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPinText) "Sembunyikan PIN" else "Lihat PIN"
                            )
                        }
                    },
                    visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isLoading) onSubmitLogin() }),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pinFocusRequester)
                        .testTag("login_pin_input")
                )

                AuthMessageBanners(authError = authError, authInfo = authInfo)

                Button(
                    onClick = onSubmitLogin,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memverifikasi Akun...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Masuk ke Aplikasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            HorizontalDivider(color = BorderDefault)

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
                            text = "Akun Google terakhir: $lastEmail",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Sapphire800,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                border = BorderStroke(1.dp, BorderInput),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_signin_button")
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Masuk dengan Google (Firebase Cloud)",
                    color = Slate800,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AuthMessageBanners(authError: String?, authInfo: String?) {
    if (authError != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Rose100,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_error_banner")
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
