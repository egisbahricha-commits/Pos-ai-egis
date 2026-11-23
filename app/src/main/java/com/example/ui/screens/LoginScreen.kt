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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.UserAccountEntity
import com.example.firebase.LicenseAccessStatus
import com.example.firebase.LicenseUiState
import com.example.ui.components.DesignSegmentedTabs
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL

@Composable
fun LoginScreen(viewModel: PosViewModel) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncCustomerLicenseSilently()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isFirstTimeSetup = userAccounts.isEmpty()

    // Setup Admin Utama / Registrasi Pelanggan state
    var customerEmail by remember(authState.lastEmail) {
        mutableStateOf(authState.lastEmail)
    }
    var customerPassword by remember { mutableStateOf("") }
    var isNewCustomerAccount by remember { mutableStateOf(true) }

    var setupStoreName by remember(storeProfile?.name) {
        mutableStateOf(
            storeProfile?.name?.takeIf { it.isNotBlank() && it != "Toko Saya" } ?: ""
        )
    }
    var setupFullName by remember(authState.verifiedGoogleName) {
        mutableStateOf(authState.verifiedGoogleName.ifBlank { "" })
    }
    var setupPhone by remember(storeProfile?.phone) {
        mutableStateOf(storeProfile?.phone ?: "")
    }
    var setupUsername by remember { mutableStateOf("admin") }
    var setupPin by remember { mutableStateOf("") }
    var setupConfirmPin by remember { mutableStateOf("") }

    // Login user state (harian offline)
    var loginUsername by remember(userAccounts) {
        mutableStateOf(
            userAccounts.firstOrNull { it.isActive }?.username ?: ""
        )
    }
    var loginPin by remember { mutableStateOf("") }

    // Returning user online license check dialog (when Supabase session needs Email & Password)
    var showLicenseAuthDialog by remember { mutableStateOf(false) }
    var licenseEmailInput by remember(authState.lastEmail) { mutableStateOf(authState.lastEmail) }
    var licensePasswordInput by remember { mutableStateOf("") }
    var licenseAuthDialogError by remember { mutableStateOf<String?>(null) }
    var licenseAuthDialogInfo by remember { mutableStateOf<String?>(null) }
    var isLicenseDialogBusy by remember { mutableStateOf(false) }
    var showLicensePass by remember { mutableStateOf(false) }

    // Secret 7-tap trigger for Super Admin (Supabase Email + Password)
    var logoTapCount by remember { mutableStateOf(0) }
    var showSecretAdminDialog by remember { mutableStateOf(false) }
    var adminEmailInput by remember { mutableStateOf(SUPER_ADMIN_EMAIL) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var showAdminPassword by remember { mutableStateOf(false) }
    var isSecretAdminBusy by remember { mutableStateOf(false) }
    var secretAdminError by remember { mutableStateOf<String?>(null) }
    var secretAdminInfo by remember { mutableStateOf<String?>(null) }

    val handleVerifyCustomerEmail: () -> Unit = {
        viewModel.authenticateCustomerSupabase(
            email = customerEmail,
            password = customerPassword,
            isNewAccount = isNewCustomerAccount,
            fullNameHint = setupFullName
        )
    }

    val handleRefreshLicenseOnline: () -> Unit = {
        if (viewModel.hasActiveCustomerSupabaseSession()) {
            viewModel.refreshCustomerLicenseOnline(
                onRequireAuth = {
                    licenseAuthDialogError = null
                    licenseAuthDialogInfo = null
                    showLicenseAuthDialog = true
                }
            )
        } else {
            licenseAuthDialogError = null
            licenseAuthDialogInfo = null
            showLicenseAuthDialog = true
        }
    }

    val displayBrandTitle = storeProfile?.name?.takeIf { it.isNotBlank() && it != "Toko Saya" } ?: "KASLITE"

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
                                            secretAdminError = null
                                            secretAdminInfo = null
                                            showSecretAdminDialog = true
                                        }
                                    }
                                    .testTag("kaslite_logo_tap_tablet")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = "Logo KASLITE",
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
                                    "Daftar wajib online dengan Email & Password (Supabase) untuk mengaktifkan Demo 3 Hari. Setelah aktif, seluruh data toko berjalan 100% lokal di perangkat."
                                else
                                    "Masuk menggunakan Username & PIN lokal Anda. Data transaksi, produk, dan kas tersimpan aman 100% lokal di perangkat.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.88f),
                                    lineHeight = 22.sp
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrandFeatureRow(
                                icon = Icons.Default.VerifiedUser,
                                title = "Daftar Online • Trial Demo 3 Hari",
                                subtitle = "Registrasi Email & Password tercatat otomatis di pusat lisensi Supabase untuk keamanan toko Anda"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Data Toko 100% Lokal di HP",
                                subtitle = "Transaksi kasir, stok, Excel & backup .kaslite tersimpan penuh di perangkat tanpa ketergantungan cloud"
                            )
                            BrandFeatureRow(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Kontrol Admin & Staf Kasir",
                                subtitle = "Kelola akun kasir, PIN 6 digit, dan izin akses menu secara mandiri"
                            )
                        }

                        Text(
                            text = "KASLITE • Data Toko Tersimpan 100% Lokal di Perangkat",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.78f)
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
                        modifier = Modifier.widthIn(max = 500.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            AuthContentCard(
                                isFirstTimeSetup = isFirstTimeSetup,
                                verifiedEmail = authState.verifiedGoogleEmail,
                                customerEmail = customerEmail,
                                onCustomerEmailChange = {
                                    customerEmail = it.trim()
                                    viewModel.clearAuthMessages()
                                },
                                customerPassword = customerPassword,
                                onCustomerPasswordChange = {
                                    customerPassword = it
                                    viewModel.clearAuthMessages()
                                },
                                isNewCustomerAccount = isNewCustomerAccount,
                                onToggleCustomerMode = { isNew ->
                                    isNewCustomerAccount = isNew
                                    viewModel.clearAuthMessages()
                                },
                                onVerifyCustomerEmailClick = handleVerifyCustomerEmail,
                                onForgotCustomerPasswordClick = {
                                    viewModel.sendSupabasePasswordReset(customerEmail)
                                },
                                onChangeEmailClick = { viewModel.clearVerifiedRegistrationEmail() },
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
                                setupPhone = setupPhone,
                                onSetupPhoneChange = {
                                    setupPhone = it
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
                                onSubmitCustomerRegistration = {
                                    viewModel.completeOnlineCustomerRegistration(
                                        storeName = setupStoreName,
                                        fullName = setupFullName,
                                        phone = setupPhone,
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
                                licenseUiState = licenseUiState,
                                onRefreshLicenseClick = handleRefreshLicenseOnline,
                                isLoading = authState.isAuthLoading,
                                lastEmail = authState.lastEmail,
                                authError = authState.authError,
                                authInfo = authState.authInfoMessage
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
                            .widthIn(max = 500.dp)
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
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.16f))
                                            .clickable {
                                                logoTapCount++
                                                if (logoTapCount >= 7) {
                                                    logoTapCount = 0
                                                    secretAdminError = null
                                                    secretAdminInfo = null
                                                    showSecretAdminDialog = true
                                                }
                                            }
                                            .testTag("kaslite_logo_tap_mobile"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PointOfSale,
                                            contentDescription = "Logo KASLITE",
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
                                                "Pendaftaran Online • Demo 3 Hari"
                                            else
                                                "Data Toko Tersimpan 100% Lokal",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.88f)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFirstTimeSetup)
                                        "Daftar online dengan Email & Password (Supabase) untuk mengaktifkan Demo 3 Hari KASLITE. Data produk & transaksi tersimpan 100% lokal di HP Anda."
                                    else
                                        "Masuk dengan Username & PIN lokal Anda. Koneksi internet hanya dibutuhkan untuk cek atau aktivasi lisensi.",
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
                            .widthIn(max = 500.dp)
                    ) {
                        AuthContentCard(
                            isFirstTimeSetup = isFirstTimeSetup,
                            verifiedEmail = authState.verifiedGoogleEmail,
                            customerEmail = customerEmail,
                            onCustomerEmailChange = {
                                customerEmail = it.trim()
                                viewModel.clearAuthMessages()
                            },
                            customerPassword = customerPassword,
                            onCustomerPasswordChange = {
                                customerPassword = it
                                viewModel.clearAuthMessages()
                            },
                            isNewCustomerAccount = isNewCustomerAccount,
                            onToggleCustomerMode = { isNew ->
                                isNewCustomerAccount = isNew
                                viewModel.clearAuthMessages()
                            },
                            onVerifyCustomerEmailClick = handleVerifyCustomerEmail,
                            onForgotCustomerPasswordClick = {
                                viewModel.sendSupabasePasswordReset(customerEmail)
                            },
                            onChangeEmailClick = { viewModel.clearVerifiedRegistrationEmail() },
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
                            setupPhone = setupPhone,
                            onSetupPhoneChange = {
                                setupPhone = it
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
                            onSubmitCustomerRegistration = {
                                viewModel.completeOnlineCustomerRegistration(
                                    storeName = setupStoreName,
                                    fullName = setupFullName,
                                    phone = setupPhone,
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
                            licenseUiState = licenseUiState,
                            onRefreshLicenseClick = handleRefreshLicenseOnline,
                            isLoading = authState.isAuthLoading,
                            lastEmail = authState.lastEmail,
                            authError = authState.authError,
                            authInfo = authState.authInfoMessage
                        )
                    }
                }
            }
        }

        // Returning user Supabase license refresh dialog
        if (showLicenseAuthDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isLicenseDialogBusy) showLicenseAuthDialog = false
                },
                icon = {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                },
                title = {
                    Text("Cek Status Lisensi Online", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Masukkan Email & Password akun KASLITE Anda untuk menyinkronkan status Demo/Full dari Supabase.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                        )
                        OutlinedTextField(
                            value = licenseEmailInput,
                            onValueChange = { licenseEmailInput = it.trim(); licenseAuthDialogError = null },
                            label = { Text("Email Terdaftar") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate500) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = licensePasswordInput,
                            onValueChange = { licensePasswordInput = it; licenseAuthDialogError = null },
                            label = { Text("Password Akun") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showLicensePass = !showLicensePass },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        if (showLicensePass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showLicensePass) "Sembunyikan password" else "Lihat password"
                                    )
                                }
                            },
                            visualTransformation = if (showLicensePass) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                enabled = !isLicenseDialogBusy,
                                onClick = {
                                    isLicenseDialogBusy = true
                                    licenseAuthDialogError = null
                                    viewModel.sendSupabasePasswordReset(licenseEmailInput) { ok, msg ->
                                        isLicenseDialogBusy = false
                                        if (ok) licenseAuthDialogInfo = msg else licenseAuthDialogError = msg
                                    }
                                }
                            ) {
                                Text("Lupa Password?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!licenseAuthDialogInfo.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Sapphire50, modifier = Modifier.fillMaxWidth()) {
                                Text(licenseAuthDialogInfo ?: "", color = Sapphire800, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                            }
                        }
                        if (!licenseAuthDialogError.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Rose100, modifier = Modifier.fillMaxWidth()) {
                                Text(licenseAuthDialogError ?: "", color = Rose500, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isLicenseDialogBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        onClick = {
                            if (licenseEmailInput.isBlank() || licensePasswordInput.isBlank()) {
                                licenseAuthDialogError = "Email dan password wajib diisi."
                                return@Button
                            }
                            isLicenseDialogBusy = true
                            licenseAuthDialogError = null
                            viewModel.refreshCustomerLicenseOnline(
                                email = licenseEmailInput,
                                password = licensePasswordInput,
                                onResult = { ok, msg ->
                                    isLicenseDialogBusy = false
                                    if (ok) {
                                        licensePasswordInput = ""
                                        showLicenseAuthDialog = false
                                    } else {
                                        licenseAuthDialogError = msg
                                    }
                                }
                            )
                        }
                    ) {
                        if (isLicenseDialogBusy) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memeriksa...")
                        } else {
                            Text("Cek Lisensi")
                        }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !isLicenseDialogBusy, onClick = { showLicenseAuthDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Secret 7-Tap Super Admin Dialog (Supabase Email + Password)
        if (showSecretAdminDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSecretAdminBusy) {
                        showSecretAdminDialog = false
                        secretAdminError = null
                        secretAdminInfo = null
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text("Mode Rahasia Admin Pusat", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Akses khusus Admin Pusat KASLITE ($SUPER_ADMIN_EMAIL). Autentikasi melalui Supabase Auth untuk melihat & mengelola seluruh pelanggan live.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                        )
                        OutlinedTextField(
                            value = adminEmailInput,
                            onValueChange = { adminEmailInput = it.trim(); secretAdminError = null },
                            label = { Text("Email Admin Pusat") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate500) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("super_admin_email_input")
                        )
                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = { adminPasswordInput = it; secretAdminError = null },
                            label = { Text("Password Admin Pusat") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showAdminPassword = !showAdminPassword },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        if (showAdminPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showAdminPassword) "Sembunyikan password" else "Lihat password"
                                    )
                                }
                            },
                            visualTransformation = if (showAdminPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = loginFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("super_admin_password_input")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                enabled = !isSecretAdminBusy,
                                onClick = {
                                    isSecretAdminBusy = true
                                    secretAdminError = null
                                    viewModel.sendSupabasePasswordReset(adminEmailInput) { ok, msg ->
                                        isSecretAdminBusy = false
                                        if (ok) secretAdminInfo = msg else secretAdminError = msg
                                    }
                                }
                            ) {
                                Text("Lupa Password?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!secretAdminInfo.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Sapphire50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = secretAdminInfo ?: "",
                                    color = Sapphire800,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (!secretAdminError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Rose100,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = secretAdminError ?: "",
                                    color = Rose500,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSecretAdminBusy = true
                            secretAdminError = null
                            viewModel.signInSuperAdminWithSupabase(
                                email = adminEmailInput,
                                password = adminPasswordInput
                            ) { ok, msg ->
                                isSecretAdminBusy = false
                                if (ok) {
                                    adminPasswordInput = ""
                                    showSecretAdminDialog = false
                                } else {
                                    secretAdminError = msg
                                }
                            }
                        },
                        enabled = !isSecretAdminBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        modifier = Modifier.testTag("super_admin_supabase_login_btn")
                    ) {
                        if (isSecretAdminBusy) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memverifikasi...")
                        } else {
                            Text("Login Super Admin")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (!isSecretAdminBusy) {
                                showSecretAdminDialog = false
                                secretAdminError = null
                                secretAdminInfo = null
                            }
                        }
                    ) {
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
    verifiedEmail: String,
    customerEmail: String,
    onCustomerEmailChange: (String) -> Unit,
    customerPassword: String,
    onCustomerPasswordChange: (String) -> Unit,
    isNewCustomerAccount: Boolean,
    onToggleCustomerMode: (Boolean) -> Unit,
    onVerifyCustomerEmailClick: () -> Unit,
    onForgotCustomerPasswordClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    setupStoreName: String,
    onSetupStoreNameChange: (String) -> Unit,
    setupFullName: String,
    onSetupFullNameChange: (String) -> Unit,
    setupPhone: String,
    onSetupPhoneChange: (String) -> Unit,
    setupUsername: String,
    onSetupUsernameChange: (String) -> Unit,
    setupPin: String,
    onSetupPinChange: (String) -> Unit,
    setupConfirmPin: String,
    onSetupConfirmPinChange: (String) -> Unit,
    onSubmitCustomerRegistration: () -> Unit,
    userAccounts: List<UserAccountEntity>,
    loginUsername: String,
    onLoginUsernameChange: (String) -> Unit,
    loginPin: String,
    onLoginPinChange: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    licenseUiState: LicenseUiState,
    onRefreshLicenseClick: () -> Unit,
    isLoading: Boolean,
    lastEmail: String,
    authError: String?,
    authInfo: String?
) {
    var showCustomerPasswordText by remember { mutableStateOf(false) }
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
                // Step 1: Mandatory Online Supabase Email + Password Verification Banner
                val isEmailVerified = verifiedEmail.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEmailVerified) Emerald50 else Sapphire50,
                    border = BorderStroke(1.dp, if (isEmailVerified) Emerald200 else Sapphire200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isEmailVerified) Icons.Default.CheckCircle else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isEmailVerified) Emerald700 else AccentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEmailVerified)
                                        "Langkah 1 Selesai • Email Terverifikasi"
                                    else
                                        "Langkah 1 (Wajib Online) • Daftar / Masuk Email",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmailVerified) Emerald800 else Sapphire800
                                    )
                                )
                                Text(
                                    text = if (isEmailVerified)
                                        verifiedEmail
                                    else
                                        "Daftarkan Email & Password Anda ke Supabase agar lisensi Trial Demo 3 Hari & dukungan Admin Pusat aktif.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                )
                            }
                        }

                        if (!isEmailVerified) {
                            DesignSegmentedTabs(
                                options = listOf("SIGNUP" to "Daftar Baru", "SIGNIN" to "Masuk Akun Lama"),
                                selectedKey = if (isNewCustomerAccount) "SIGNUP" else "SIGNIN",
                                onSelect = { onToggleCustomerMode(it == "SIGNUP") }
                            )

                            OutlinedTextField(
                                value = customerEmail,
                                onValueChange = onCustomerEmailChange,
                                label = { Text("Email Pemilik Toko *") },
                                placeholder = { Text("contoh@gmail.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate500) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = loginFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_supabase_email_input")
                            )

                            OutlinedTextField(
                                value = customerPassword,
                                onValueChange = onCustomerPasswordChange,
                                label = { Text("Password Akun Email (min. 6 karakter) *") },
                                placeholder = { Text("Masukkan password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showCustomerPasswordText = !showCustomerPasswordText },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            if (showCustomerPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showCustomerPasswordText) "Sembunyikan password" else "Lihat password"
                                        )
                                    }
                                },
                                visualTransformation = if (showCustomerPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { if (!isLoading) onVerifyCustomerEmailClick() }),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = loginFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_supabase_password_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onForgotCustomerPasswordClick,
                                    enabled = !isLoading
                                ) {
                                    Text("Lupa Password?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = onVerifyCustomerEmailClick,
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("customer_supabase_verify_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menghubungkan Supabase...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isNewCustomerAccount) "Daftar & Aktifkan Email" else "Masuk Akun Email",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Siap aktifkan Demo 3 Hari",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Emerald700, fontWeight = FontWeight.Bold)
                                )
                                TextButton(
                                    onClick = onChangeEmailClick,
                                    enabled = !isLoading
                                ) {
                                    Text("Ganti Email", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Langkah 2 • Data Toko & Akun Pemilik Lokal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                )

                OutlinedTextField(
                    value = setupStoreName,
                    onValueChange = onSetupStoreNameChange,
                    label = { Text("Nama Toko *") },
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
                    label = { Text("Nama Lengkap Pemilik *") },
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
                    value = setupPhone,
                    onValueChange = onSetupPhoneChange,
                    label = { Text("No. HP / WhatsApp") },
                    placeholder = { Text("Contoh: 081234567890") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate500) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_phone_input")
                )

                OutlinedTextField(
                    value = setupUsername,
                    onValueChange = onSetupUsernameChange,
                    label = { Text("Username Login Harian *") },
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
                    label = { Text("Buat PIN 6 Digit Angka *") },
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
                    label = { Text("Konfirmasi PIN 6 Digit *") },
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
                    keyboardActions = KeyboardActions(onDone = { if (!isLoading) onSubmitCustomerRegistration() }),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_setup_confirm_pin_input")
                )

                AuthMessageBanners(authError = authError, authInfo = authInfo)

                Button(
                    onClick = onSubmitCustomerRegistration,
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
                        Text("Mendaftarkan Toko...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daftar Trial 3 Hari & Masuk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                // Returning Local Customer Login + License Status
                LicenseStatusCard(licenseUiState = licenseUiState, lastEmail = lastEmail)

                val activeUsers = remember(userAccounts) { userAccounts.filter { it.isActive } }
                if (activeUsers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pilih cepat username (wajib masukkan PIN):",
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

                HorizontalDivider(color = BorderDefault)

                OutlinedButton(
                    onClick = onRefreshLicenseClick,
                    enabled = !isLoading && !licenseUiState.isRefreshing,
                    border = BorderStroke(1.dp, BorderInput),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("refresh_license_online_button")
                ) {
                    if (licenseUiState.isRefreshing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = AccentBlue)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cek / Aktivasi Lisensi Online",
                        color = Slate800,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseStatusCard(licenseUiState: LicenseUiState, lastEmail: String) {
    val (bg, border, iconTint, icon) = when (licenseUiState.accessStatus) {
        LicenseAccessStatus.FULL_ACTIVE -> Quad(Emerald50, Emerald200, Emerald700, Icons.Default.CheckCircle)
        LicenseAccessStatus.DEMO_ACTIVE -> Quad(Sapphire50, Sapphire200, AccentBlue, Icons.Default.VerifiedUser)
        LicenseAccessStatus.DEMO_EXPIRED,
        LicenseAccessStatus.SUSPENDED,
        LicenseAccessStatus.DEVICE_MISMATCH -> Quad(Rose100, Rose500.copy(alpha = 0.35f), Rose500, Icons.Default.Warning)
        LicenseAccessStatus.UNREGISTERED -> Quad(AmberWarningBg, AmberWarningBorder, Amber500, Icons.Default.Warning)
    }

    val daysRemaining = ((licenseUiState.remainingMillis.coerceAtLeast(0L) + 86_399_999L) / 86_400_000L)
    val title = when (licenseUiState.accessStatus) {
        LicenseAccessStatus.FULL_ACTIVE -> "Lisensi KASLITE Penuh Aktif"
        LicenseAccessStatus.DEMO_ACTIVE -> "Mode Demo 3 Hari • Sisa $daysRemaining hari"
        LicenseAccessStatus.DEMO_EXPIRED -> "Masa Demo Berakhir (Hanya Menu Produk)"
        LicenseAccessStatus.SUSPENDED -> "Lisensi Dinonaktifkan Admin Pusat"
        LicenseAccessStatus.DEVICE_MISMATCH -> "Lisensi Terikat Perangkat Lain"
        LicenseAccessStatus.UNREGISTERED -> "Mode Lokal • Belum Sinkron Lisensi"
    }

    val displayEmail = licenseUiState.ownerEmail.ifBlank { lastEmail }
    val subtitle = when {
        licenseUiState.isLockedToProductOnly ->
            "Hubungi Admin Pusat ($SUPER_ADMIN_EMAIL) untuk aktivasi penuh. Menu Produk & Backup tetap terbuka."
        displayEmail.isNotBlank() ->
            "Terdaftar: $displayEmail • Data Toko 100% Lokal"
        else ->
            "Data transaksi & stok tersimpan 100% lokal di perangkat."
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Slate700
                    )
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
