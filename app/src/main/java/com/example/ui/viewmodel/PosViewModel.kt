package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIIntent
import com.example.ai.AIParseResult
import com.example.ai.GeraiUnaAIEngine
import com.example.data.backup.KasGoBackupImporter
import com.example.data.backup.KasGoImportOptions
import com.example.data.backup.KasGoImportSummary
import com.example.data.backup.KasGoInspectResult
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.*
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.PurchaseItemDraft
import com.example.firebase.CashAccountDoc
import com.example.firebase.CashMutationDoc
import com.example.firebase.FirebaseAuthUiState
import com.example.firebase.FirebaseSessionStore
import com.example.firebase.FirebaseSyncRepository
import com.example.firebase.GoogleAuthHelper
import com.example.firebase.GoogleSignInPayload
import com.example.firebase.SyncMode
import com.example.whatsapp.WhatsAppCommandCenter
import com.example.whatsapp.WhatsAppMessageResponse
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

enum class Screen {
    DASHBOARD,
    POS_KASIR,
    RIWAYAT_TRANSAKSI,
    PRODUK,
    PEMBELIAN,
    PENERIMAAN_BARANG,
    STOK_INVENTARIS,
    PENGELUARAN,
    PENDAPATAN,
    UTANG_PIUTANG,
    KEUANGAN,
    LAPORAN,
    AI_CHAT,
    WHATSAPP,
    KONTAK,
    PENGATURAN,
    SEMUA_MENU,
    MULTI_KAS,
    TARIF_ADMIN,
    CLOUD_MONITOR,
    SCAN_BARCODE,
    MANAJEMEN_PRODUK
}

data class ManualProductValidation(
    val nameError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
    val barcodeError: String? = null
) {
    val isValid: Boolean
        get() = nameError == null && priceError == null && stockError == null && barcodeError == null
}

fun validateManualProductInput(
    name: String,
    sellingPriceText: String,
    stockText: String,
    barcode: String,
    editingProductId: Long? = null,
    existingProducts: List<ProductEntity> = emptyList()
): ManualProductValidation {
    val cleanName = name.trim()
    val nameErr = if (cleanName.isEmpty()) "Nama produk wajib diisi" else null

    val parsedPrice = sellingPriceText.trim().replace(',', '.').toDoubleOrNull()
    val priceErr = when {
        sellingPriceText.isBlank() -> "Harga jual wajib diisi"
        parsedPrice == null || parsedPrice < 0.0 -> "Harga jual tidak valid"
        else -> null
    }

    val parsedStock = stockText.trim().replace(',', '.').toDoubleOrNull()
    val stockErr = when {
        stockText.isBlank() -> "Stok saat ini wajib diisi"
        parsedStock == null -> "Angka stok tidak valid"
        else -> null
    }

    val cleanBarcode = barcode.trim()
    val duplicateBarcode = cleanBarcode.isNotEmpty() && existingProducts.any { product ->
        product.id != editingProductId &&
            product.barcode.trim().equals(cleanBarcode, ignoreCase = true)
    }
    val barcodeErr = if (duplicateBarcode) "Barcode sudah digunakan produk lain" else null

    return ManualProductValidation(
        nameError = nameErr,
        priceError = priceErr,
        stockError = stockErr,
        barcodeError = barcodeErr
    )
}

sealed class BarcodeLookupOutcome {
    data class AddedVariant(val product: ProductEntity, val variant: ProductVariantEntity) : BarcodeLookupOutcome()
    data class AddedProduct(val product: ProductEntity) : BarcodeLookupOutcome()
    data class NeedsSelection(
        val product: ProductEntity,
        val conversions: List<UnitConversionEntity>,
        val variants: List<ProductVariantEntity>
    ) : BarcodeLookupOutcome()
    data class NotFound(val barcode: String) : BarcodeLookupOutcome()
}

fun resolveBarcodeToCartMatch(
    rawCode: String,
    products: List<ProductEntity>,
    variants: List<ProductVariantEntity>,
    conversions: List<UnitConversionEntity>
): BarcodeLookupOutcome {
    val code = rawCode.trim()
    if (code.isEmpty()) return BarcodeLookupOutcome.NotFound("")

    val matchedVariant = variants.firstOrNull { variant ->
        variant.isActive &&
            variant.barcode.isNotBlank() &&
            variant.barcode.trim().equals(code, ignoreCase = true)
    }
    if (matchedVariant != null) {
        val parentProduct = products.firstOrNull { it.id == matchedVariant.productId && it.isActive }
        if (parentProduct != null) {
            return BarcodeLookupOutcome.AddedVariant(parentProduct, matchedVariant)
        }
    }

    val matchedProduct = products.firstOrNull { product ->
        product.isActive &&
            product.barcode.isNotBlank() &&
            product.barcode.trim().equals(code, ignoreCase = true)
    } ?: return BarcodeLookupOutcome.NotFound(code)

    val productConversions = conversions.filter { it.productId == matchedProduct.id }
    val activeVariants = variants.filter { it.productId == matchedProduct.id && it.isActive }
    return if ((matchedProduct.hasVariants && activeVariants.isNotEmpty()) ||
        matchedProduct.isFlexiblePrice ||
        productConversions.isNotEmpty()
    ) {
        BarcodeLookupOutcome.NeedsSelection(
            product = matchedProduct,
            conversions = productConversions,
            variants = activeVariants
        )
    } else {
        BarcodeLookupOutcome.AddedProduct(matchedProduct)
    }
}

sealed class LocalAuthResult {
    data class Success(val user: UserAccountEntity) : LocalAuthResult()
    data class Error(val message: String) : LocalAuthResult()
}

data class AdminSetupValidation(
    val storeNameError: String? = null,
    val fullNameError: String? = null,
    val usernameError: String? = null,
    val pinError: String? = null,
    val confirmPinError: String? = null
) {
    val isValid: Boolean
        get() = storeNameError == null &&
            fullNameError == null &&
            usernameError == null &&
            pinError == null &&
            confirmPinError == null
}

val ALL_PERMISSION_MENU_KEYS = listOf(
    "POS_KASIR",
    "SCAN_BARCODE",
    "RIWAYAT_TRANSAKSI",
    "PRODUK",
    "MANAJEMEN_PRODUK",
    "PEMBELIAN",
    "STOK_INVENTARIS",
    "MULTI_KAS",
    "TARIF_ADMIN",
    "KEUANGAN",
    "PENGELUARAN",
    "UTANG_PIUTANG",
    "LAPORAN",
    "AI_CHAT",
    "WHATSAPP",
    "KONTAK",
    "PENGATURAN"
)

fun validateAdminSetupInput(
    storeName: String,
    fullName: String,
    username: String,
    pin: String,
    confirmPin: String
): AdminSetupValidation {
    val cleanStore = storeName.trim()
    val cleanFull = fullName.trim()
    val cleanUser = username.trim()
    val cleanPin = pin.trim()
    val cleanConfirm = confirmPin.trim()

    val storeErr = if (cleanStore.isEmpty()) "Nama toko wajib diisi" else null
    val fullErr = if (cleanFull.isEmpty()) "Nama lengkap Admin Utama wajib diisi" else null
    val userErr = when {
        cleanUser.isEmpty() -> "Username Admin wajib diisi"
        cleanUser.length < 3 -> "Username minimal 3 karakter"
        cleanUser.contains(" ") -> "Username tidak boleh mengandung spasi"
        else -> null
    }
    val pinErr = if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) {
        "PIN wajib 6 digit angka"
    } else null
    val confirmErr = if (cleanConfirm != cleanPin) {
        "Konfirmasi PIN tidak sama"
    } else null

    return AdminSetupValidation(
        storeNameError = storeErr,
        fullNameError = fullErr,
        usernameError = userErr,
        pinError = pinErr,
        confirmPinError = confirmErr
    )
}

fun authenticateLocalUser(
    usernameInput: String,
    pinInput: String,
    users: List<UserAccountEntity>
): LocalAuthResult {
    val cleanUsername = usernameInput.trim()
    val cleanPin = pinInput.trim()
    if (cleanUsername.isEmpty()) {
        return LocalAuthResult.Error("Username wajib diisi")
    }
    if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) {
        return LocalAuthResult.Error("PIN harus 6 digit angka")
    }
    val matched = users.firstOrNull {
        it.username.trim().equals(cleanUsername, ignoreCase = true)
    } ?: return LocalAuthResult.Error("Username \"$cleanUsername\" belum terdaftar. Minta Admin Utama membuat akun.")
    if (!matched.isActive) {
        return LocalAuthResult.Error("Akun \"${matched.username}\" sedang dinonaktifkan oleh Admin Utama")
    }
    if (matched.pinHash != GeraiUnaDatabase.hashPin(cleanPin)) {
        return LocalAuthResult.Error("PIN salah. Silakan coba lagi")
    }
    return LocalAuthResult.Success(matched)
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val draftResult: AIParseResult? = null,
    val isActionPending: Boolean = false,
    val isActionResolved: Boolean = false
)

data class KasGoImportUiState(
    val isInspecting: Boolean = false,
    val isImporting: Boolean = false,
    val inspectResult: KasGoInspectResult? = null,
    val lastImportSummary: KasGoImportSummary? = null,
    val errorMessage: String? = null
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    val database = GeraiUnaDatabase.getDatabase(application)
    val repository = PosRepository(database)
    val aiEngine = GeraiUnaAIEngine(repository)
    val whatsAppCommandCenter = WhatsAppCommandCenter(repository, aiEngine)

    val firebaseSessionStore = FirebaseSessionStore(application)
    private val isFirebaseConfigured: Boolean = runCatching {
        FirebaseApp.getApps(application).isNotEmpty() || FirebaseApp.initializeApp(application) != null
    }.getOrDefault(false)
    val firebaseSyncRepository: FirebaseSyncRepository? = if (isFirebaseConfigured) {
        runCatching { FirebaseSyncRepository(application, database, firebaseSessionStore) }.getOrNull()
    } else null
    private val auth: FirebaseAuth? = if (isFirebaseConfigured) {
        runCatching { Firebase.auth }.getOrNull()
    } else null

    private val _authUiState = MutableStateFlow(
        FirebaseAuthUiState(
            deviceId = firebaseSessionStore.getOrCreateDeviceId(),
            deviceName = firebaseSessionStore.getDeviceName(),
            lastEmail = firebaseSessionStore.getLastLoginEmail(),
            storeName = firebaseSessionStore.getStoreNameHint(),
            autoSyncEnabled = firebaseSessionStore.isAutoSyncEnabled(),
            lastSyncAt = firebaseSessionStore.getLastSyncAt()
        )
    )
    val authUiState: StateFlow<FirebaseAuthUiState> = _authUiState.asStateFlow()

    private val _cloudCashAccounts = MutableStateFlow<List<CashAccountDoc>>(emptyList())
    val cloudCashAccounts: StateFlow<List<CashAccountDoc>> = _cloudCashAccounts.asStateFlow()

    private val _cloudCashMutations = MutableStateFlow<List<CashMutationDoc>>(emptyList())
    val cloudCashMutations: StateFlow<List<CashMutationDoc>> = _cloudCashMutations.asStateFlow()

    private var cloudAccountsJob: Job? = null
    private var cloudMutationsJob: Job? = null
    private var listeningUid: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            applyAuthenticatedUser(user)
            startCloudListeners(user.uid)
        } else {
            stopCloudListeners()
        }
    }

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        if (!hasMenuPermission(screen)) {
            _userMessage.value = "Akses menu ${screen.name} dibatasi untuk peran $_activeRole"
            return
        }
        _currentScreen.value = screen
    }

    // Active User Role & Account (empty until authenticated via LoginScreen)
    private val _activeRole = MutableStateFlow("")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _activeUserName = MutableStateFlow("")
    val activeUserName: StateFlow<String> = _activeUserName.asStateFlow()

    private val _activeUserId = MutableStateFlow(0L)
    val activeUserId: StateFlow<Long> = _activeUserId.asStateFlow()

    private val _themeMode = MutableStateFlow("SYSTEM") // "SYSTEM", "LIGHT", "DARK"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        val normalized = when (mode.uppercase()) {
            "DARK", "LIGHT" -> mode.uppercase()
            else -> "SYSTEM"
        }
        _themeMode.value = normalized
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("theme_mode", normalized)
        }
        val label = when (normalized) {
            "DARK" -> "Mode Gelap"
            "LIGHT" -> "Mode Terang"
            else -> "Ikuti Sistem"
        }
        _userMessage.value = "Tema diubah ke $label"
    }

    fun switchRole(role: String, userName: String) {
        _activeRole.value = role
        _activeUserName.value = userName
        val matchedUser = userAccounts.value.find {
            it.name.equals(userName, ignoreCase = true) || it.role.equals(role, ignoreCase = true)
        }
        if (matchedUser != null) {
            _activeUserId.value = matchedUser.id
        }
        viewModelScope.launch {
            repository.setSetting("active_role", role)
            repository.setSetting("active_user_name", userName)
        }
    }

    fun switchUserAccount(user: UserAccountEntity) {
        _activeUserId.value = user.id
        val roleDisplay = if (user.role.equals("owner", true)) "Owner" else "Kasir"
        _activeRole.value = roleDisplay
        _activeUserName.value = user.name
        viewModelScope.launch {
            repository.setSetting("active_role", roleDisplay)
            repository.setSetting("active_user_name", user.name)
        }
        _userMessage.value = "Login sebagai ${user.name} ($roleDisplay)"
    }

    fun verifyPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) return false
        val hash = GeraiUnaDatabase.hashPin(cleanPin)
        val activeUser = userAccounts.value.find { it.id == _activeUserId.value && it.isActive }
        if (activeUser != null) {
            return activeUser.pinHash == hash
        }
        // If signed in via Google Cloud without selecting a local user, match against any active Owner account
        return userAccounts.value.any {
            it.isActive && it.role.equals("owner", ignoreCase = true) && it.pinHash == hash
        }
    }

    fun hasMenuPermission(screen: Screen): Boolean {
        if (_activeRole.value.equals("Owner", ignoreCase = true) || _activeRole.value.equals("Admin", ignoreCase = true)) {
            return true
        }
        if (screen == Screen.DASHBOARD || screen == Screen.SEMUA_MENU || screen == Screen.PENGATURAN) return true
        val perms = menuPermissions.value.filter { it.userId == _activeUserId.value }
        if (perms.isEmpty()) return true
        val matched = perms.find { it.menuKey == screen.name }
        return matched?.allowed ?: true
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure offline bypass flag never auto-enters app without login
                firebaseSessionStore.setOfflineBypass(false)

                val initializedFlag = repository.getSetting("db_initialized")
                val extDao = database.extendedPosDao()
                val cashAccountDao = database.cashAccountDao()
                val existingProfile = extDao.getStoreProfileSync()
                val existingAccounts = cashAccountDao.getAllAccountsList()

                if (initializedFlag != "true" && existingProfile == null && existingAccounts.isEmpty()) {
                    GeraiUnaDatabase.populateInitialData(database)
                } else {
                    if (existingAccounts.isEmpty()) {
                        cashAccountDao.insertCashAccount(
                            CashAccountEntity(
                                accountName = "Kas Toko",
                                balance = 0.0,
                                accountType = "CASH",
                                color = "#16A34A",
                                notes = "Kas utama laci kasir",
                                isDefault = true,
                                sortOrder = 1
                            )
                        )
                    }
                    if (existingProfile == null) {
                        extDao.upsertStoreProfile(StoreProfileEntity())
                    }
                    if (initializedFlag != "true") {
                        repository.setSetting("db_initialized", "true")
                    }
                }

                // Sync tax rate from store profile
                val profile = extDao.getStoreProfileSync()
                if (profile != null && profile.ppnBp > 0) {
                    _cartTaxRate.value = profile.ppnBp / 10000.0
                }
                val savedTheme = repository.getSetting("theme_mode")
                if (!savedTheme.isNullOrBlank()) {
                    _themeMode.value = savedTheme
                }
                if (profile != null && profile.name.isNotBlank()) {
                    firebaseSessionStore.setStoreNameHint(profile.name)
                    _authUiState.update { it.copy(storeName = profile.name) }
                }

                val activeAuth = auth
                if (activeAuth != null) {
                    activeAuth.addAuthStateListener(authStateListener)
                    val currentUser = activeAuth.currentUser
                    if (currentUser != null) {
                        applyAuthenticatedUser(currentUser)
                        startCloudListeners(currentUser.uid)
                        syncNowWithFirebase(SyncMode.TWO_WAY, silent = true)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun applyAuthenticatedUser(user: FirebaseUser) {
        val email = user.email.orEmpty()
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "Pemilik Toko" }
        if (email.isNotBlank()) {
            firebaseSessionStore.setLastLoginEmail(email)
        }
        firebaseSessionStore.setOfflineBypass(false)
        _activeUserName.value = displayName
        _activeRole.value = "Owner"
        _authUiState.update {
            it.copy(
                hasEnteredApp = true,
                isCloudSignedIn = true,
                isOfflineMode = false,
                userId = user.uid,
                userEmail = email,
                fullName = displayName,
                storeName = firebaseSessionStore.getStoreNameHint(),
                lastEmail = email.ifBlank { it.lastEmail },
                isAuthLoading = false,
                authError = null
            )
        }
    }

    private fun startCloudListeners(userId: String) {
        val syncRepo = firebaseSyncRepository ?: return
        if (listeningUid == userId && cloudAccountsJob != null) return
        stopCloudListeners()
        listeningUid = userId

        cloudAccountsJob = viewModelScope.launch {
            syncRepo.observeCashAccounts(userId)
                .catch { /* logged by handler or ignored on tear-down */ }
                .collect { accounts ->
                    _cloudCashAccounts.value = accounts
                }
        }

        cloudMutationsJob = viewModelScope.launch {
            syncRepo.observeCashMutations(userId)
                .catch { /* logged by handler or ignored on tear-down */ }
                .collect { mutations ->
                    _cloudCashMutations.value = mutations
                }
        }
    }

    private fun stopCloudListeners() {
        cloudAccountsJob?.cancel()
        cloudMutationsJob?.cancel()
        cloudAccountsJob = null
        cloudMutationsJob = null
        listeningUid = null
        _cloudCashAccounts.value = emptyList()
        _cloudCashMutations.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        auth?.removeAuthStateListener(authStateListener)
        stopCloudListeners()
    }

    fun clearAuthMessages() {
        _authUiState.update { it.copy(authError = null, authInfoMessage = null) }
    }

    fun setAuthLoading(loading: Boolean) {
        _authUiState.update { it.copy(isAuthLoading = loading, authError = if (loading) null else it.authError) }
    }

    fun setAuthError(message: String) {
        _authUiState.update { it.copy(isAuthLoading = false, authError = message) }
    }

    fun signInWithGooglePayload(payload: GoogleSignInPayload, storeNameInput: String = "") {
        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                val trimmedStore = storeNameInput.trim()
                if (trimmedStore.isNotBlank()) {
                    firebaseSessionStore.setStoreNameHint(trimmedStore)
                    val currentProfile = database.extendedPosDao().getStoreProfileSync() ?: StoreProfileEntity()
                    if (currentProfile.name != trimmedStore) {
                        repository.saveStoreProfile(currentProfile.copy(name = trimmedStore))
                    }
                }
                val activeAuth = auth
                    ?: throw IllegalStateException("Firebase belum terkonfigurasi di build APK ini. Gunakan Mode Offline.")
                val credential = GoogleAuthProvider.getCredential(payload.idToken, null)
                val authResult = activeAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                    ?: throw IllegalStateException("Akun Firebase tidak ditemukan setelah login Google.")
                applyAuthenticatedUser(firebaseUser)
                startCloudListeners(firebaseUser.uid)
                syncNowWithFirebase(SyncMode.TWO_WAY, isInitialLoginPull = true, silent = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = e.localizedMessage ?: "Gagal masuk dengan akun Google / Firebase."
                    )
                }
            }
        }
    }

    fun setupMainAdmin(
        storeName: String,
        fullName: String,
        username: String,
        pin: String,
        confirmPin: String
    ) {
        val validation = validateAdminSetupInput(storeName, fullName, username, pin, confirmPin)
        if (!validation.isValid) {
            val firstError = listOfNotNull(
                validation.storeNameError,
                validation.fullNameError,
                validation.usernameError,
                validation.pinError,
                validation.confirmPinError
            ).firstOrNull() ?: "Data Admin Utama belum lengkap"
            _authUiState.update { it.copy(authError = firstError) }
            return
        }

        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                val cleanStore = storeName.trim()
                val cleanFull = fullName.trim()
                val cleanUsername = username.trim().lowercase()
                val pinHash = GeraiUnaDatabase.hashPin(pin.trim())

                val existingUsers = withContext(Dispatchers.IO) {
                    database.extendedPosDao().getAllUsersList()
                }
                if (existingUsers.isNotEmpty()) {
                    _authUiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = "Admin Utama sudah terdaftar. Silakan login menggunakan username & PIN."
                        )
                    }
                    return@launch
                }

                val currentProfile = withContext(Dispatchers.IO) {
                    database.extendedPosDao().getStoreProfileSync() ?: StoreProfileEntity()
                }
                repository.saveStoreProfile(currentProfile.copy(name = cleanStore))
                firebaseSessionStore.setStoreNameHint(cleanStore)
                repository.setSetting("db_initialized", "true")

                val adminUser = UserAccountEntity(
                    name = cleanFull,
                    username = cleanUsername,
                    role = "owner",
                    pinHash = pinHash,
                    isActive = true
                )
                val fullPermissions = ALL_PERMISSION_MENU_KEYS.associateWith { true }
                val newUserId = repository.saveUserWithPermissions(adminUser, fullPermissions)

                firebaseSessionStore.setOfflineBypass(false)
                _activeUserId.value = newUserId
                _activeRole.value = "Owner"
                _activeUserName.value = cleanFull
                _currentScreen.value = Screen.DASHBOARD
                repository.setSetting("active_role", "Owner")
                repository.setSetting("active_user_name", cleanFull)

                _authUiState.update {
                    it.copy(
                        hasEnteredApp = true,
                        isOfflineMode = true,
                        isCloudSignedIn = false,
                        fullName = cleanFull,
                        storeName = cleanStore,
                        isAuthLoading = false,
                        authError = null,
                        authInfoMessage = null
                    )
                }
                _userMessage.value = "Admin Utama \"$cleanFull\" berhasil dibuat!"
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = "Gagal membuat Admin Utama: ${e.message}"
                    )
                }
            }
        }
    }

    fun loginWithLocalCredentials(username: String, pin: String) {
        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                val allUsers = withContext(Dispatchers.IO) {
                    database.extendedPosDao().getAllUsersList()
                }
                when (val result = authenticateLocalUser(username, pin, allUsers)) {
                    is LocalAuthResult.Error -> {
                        _authUiState.update {
                            it.copy(isAuthLoading = false, authError = result.message)
                        }
                    }
                    is LocalAuthResult.Success -> {
                        val user = result.user
                        val roleDisplay = if (user.role.equals("owner", ignoreCase = true)) "Owner" else "Kasir"
                        firebaseSessionStore.setOfflineBypass(false)
                        _activeUserId.value = user.id
                        _activeRole.value = roleDisplay
                        _activeUserName.value = user.name
                        _currentScreen.value = Screen.DASHBOARD
                        repository.setSetting("active_role", roleDisplay)
                        repository.setSetting("active_user_name", user.name)

                        _authUiState.update {
                            it.copy(
                                hasEnteredApp = true,
                                isOfflineMode = true,
                                isCloudSignedIn = false,
                                fullName = user.name,
                                isAuthLoading = false,
                                authError = null,
                                authInfoMessage = null
                            )
                        }
                        _userMessage.value = "Selamat datang, ${user.name} ($roleDisplay)"
                    }
                }
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = "Gagal memverifikasi akun: ${e.message}"
                    )
                }
            }
        }
    }

    fun logoutToLoginScreen() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                stopCloudListeners()
                auth?.signOut()
                firebaseSessionStore.setOfflineBypass(false)
                GoogleAuthHelper.clearCredentialState(getApplication())
            }
            _activeUserId.value = 0L
            _activeRole.value = ""
            _activeUserName.value = ""
            _currentScreen.value = Screen.DASHBOARD
            _authUiState.update {
                it.copy(
                    hasEnteredApp = false,
                    isCloudSignedIn = false,
                    isOfflineMode = false,
                    userId = "",
                    userEmail = "",
                    authError = null,
                    authInfoMessage = "Anda telah keluar. Silakan login kembali."
                )
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        firebaseSessionStore.setAutoSyncEnabled(enabled)
        _authUiState.update { it.copy(autoSyncEnabled = enabled) }
    }

    fun syncNowWithFirebase(
        mode: SyncMode = SyncMode.TWO_WAY,
        isInitialLoginPull: Boolean = false,
        silent: Boolean = false
    ) {
        val currentUser = auth?.currentUser
        val syncRepo = firebaseSyncRepository
        if (currentUser == null || syncRepo == null) {
            if (!silent) {
                _userMessage.value = if (syncRepo == null) {
                    "Sinkronisasi cloud belum aktif karena google-services.json belum terpasang di build APK ini."
                } else {
                    "Silakan login dengan akun Google (Firebase) untuk menyinkronkan data."
                }
            }
            return
        }
        if (_authUiState.value.isSyncing) return
        viewModelScope.launch {
            _authUiState.update { it.copy(isSyncing = true) }
            val ownerName = currentUser.displayName?.takeIf { it.isNotBlank() }
                ?: _authUiState.value.fullName.ifBlank { "Pemilik Toko" }
            val ownerEmail = currentUser.email ?: _authUiState.value.userEmail
            val result = syncRepo.sync(
                userId = currentUser.uid,
                ownerName = ownerName,
                ownerEmail = ownerEmail,
                mode = mode,
                isInitialLoginPull = isInitialLoginPull
            )
            result.onSuccess { summary ->
                _authUiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncAt = summary.timestamp,
                        lastSyncMessage = summary.message
                    )
                }
                if (!silent) {
                    _userMessage.value = summary.message
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Gagal sinkronisasi dengan Firebase Firestore"
                _authUiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncMessage = "Gagal: $msg"
                    )
                }
                if (!silent) {
                    _userMessage.value = msg
                }
            }
        }
    }

    private fun scheduleAutoSync() {
        val state = _authUiState.value
        if (state.isCloudSignedIn && state.autoSyncEnabled && auth?.currentUser != null && !state.isSyncing) {
            syncNowWithFirebase(SyncMode.TWO_WAY, silent = true)
        }
    }

    // Repository Flows
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = repository.allSales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSaleItems = repository.allSaleItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchases = repository.allPurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockMovements = repository.stockMovements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomes = repository.incomes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debtPayments = repository.debtPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unitConversions = repository.unitConversions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allProductTierPrices: StateFlow<List<ProductTierPriceEntity>> = repository.allTierPrices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentBarcodeScans: StateFlow<List<BarcodeScanLogEntity>> = repository.recentBarcodeScans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val aiLogs = repository.aiCommandLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi Kas & Konter Flows
    val cashAccounts: StateFlow<List<CashAccountEntity>> = repository.allCashAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashTransfers: StateFlow<List<CashTransferEntity>> = repository.allCashTransfers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashMutations: StateFlow<List<CashMutationEntity>> = repository.cashMutations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tierServices: StateFlow<List<AdminTierServiceEntity>> = repository.allTierServices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTierRanges: StateFlow<List<AdminTierRangeEntity>> = repository.allTierRanges.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminTiers: StateFlow<List<AdminTierWithRanges>> = combine(tierServices, allTierRanges) { services, ranges ->
        services.map { s ->
            AdminTierWithRanges(service = s, ranges = ranges.filter { it.serviceId == s.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shift & Pending Sales Flows
    val allShifts: StateFlow<List<CashierShiftEntity>> = repository.allShifts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentOpenShift: StateFlow<CashierShiftEntity?> = repository.currentOpenShift.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeShift: StateFlow<CashierShiftEntity?> = currentOpenShift
    val shiftCashMovements: StateFlow<List<ShiftCashMovementEntity>> = repository.shiftCashMovements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingSales: StateFlow<List<PendingSaleEntity>> = repository.pendingSales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Schema v3 Flows
    val storeProfile: StateFlow<StoreProfileEntity?> = repository.storeProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val userAccounts: StateFlow<List<UserAccountEntity>> = repository.userAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val menuPermissions: StateFlow<List<MenuPermissionEntity>> = repository.menuPermissions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val printers: StateFlow<List<PrinterEntity>> = repository.printers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashierDevices: StateFlow<List<CashierDeviceEntity>> = repository.cashierDevices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productVariants: StateFlow<List<ProductVariantEntity>> = repository.productVariants.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wholesaleTiers: StateFlow<List<WholesaleTierEntity>> = repository.wholesaleTiers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productBatches: StateFlow<List<ProductBatchEntity>> = repository.productBatches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bundles: StateFlow<List<BundlePackageEntity>> = repository.bundles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bundleItems: StateFlow<List<BundleItemEntity>> = repository.bundleItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val promos: StateFlow<List<PromoEntity>> = repository.promos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val appSettings: StateFlow<List<AppSettingsEntity>> = repository.appSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State for POS Kasir
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _cartDiscount = MutableStateFlow(0.0)
    val cartDiscount: StateFlow<Double> = _cartDiscount.asStateFlow()

    private val _cartTaxRate = MutableStateFlow(0.0)
    val cartTaxRate: StateFlow<Double> = _cartTaxRate.asStateFlow()

    private val _lastCompletedSale = MutableStateFlow<SaleEntity?>(null)
    val lastCompletedSale: StateFlow<SaleEntity?> = _lastCompletedSale.asStateFlow()

    private val _lastCompletedSaleItems = MutableStateFlow<List<CartItem>>(emptyList())
    val lastCompletedSaleItems: StateFlow<List<CartItem>> = _lastCompletedSaleItems.asStateFlow()

    // AI Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                senderName = "Asisten AI Toko",
                text = "Halo! Saya Asisten AI POS.\nKetik perintah operasional toko apa saja, misalnya:\n• \"Tambah produk baru Kopi Hitam 100g modal 8 ribu jual 12 ribu\"\n• \"Pengeluaran listrik 250 ribu\"\n• \"Tambah stok Kopi Hitam 20 pcs\"\n• \"Berapa stok produk?\""
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Helper: Resolve effective unit price & price source (per_jumlah -> grosir -> tetap) ---
    private fun resolvePriceAndSource(
        productId: Long,
        unitName: String,
        multiplier: Double,
        qty: Double,
        fallbackPrice: Double
    ): Pair<Double, String> {
        // 1. Check harga_per_jumlah (total harga untuk jumlah exact)
        if (multiplier == 1.0) {
            val perJumlah = allProductTierPrices.value.filter { it.productId == productId }
                .find { kotlin.math.abs(it.quantity - qty) < 0.01 }
            if (perJumlah != null && qty > 0) {
                return (perJumlah.totalPrice / qty) to "per_jumlah"
            }
        }
        // 2. Check harga_bertingkat (grosir per satuan ketika qty >= minQty)
        val wholesales = wholesaleTiers.value.filter {
            it.productId == productId &&
                    (it.unitName.isBlank() || it.unitName.equals(unitName, ignoreCase = true)) &&
                    qty >= it.minQty
        }.maxByOrNull { it.minQty }
        if (wholesales != null) {
            return wholesales.pricePerUnit to "grosir"
        }
        return fallbackPrice to "tetap"
    }

    // --- POS Cart Operations ---

    fun addToCart(
        product: ProductEntity,
        unitName: String = product.baseUnit,
        multiplier: Double = 1.0,
        price: Double = product.sellingPrice,
        customPrice: Double? = null,
        storeAdminFee: Double = 0.0,
        providerAdminFee: Double = 0.0,
        agentCommission: Double = 0.0,
        variant: ProductVariantEntity? = null,
        serviceCashAccountId: Long? = product.serviceCashAccountId,
        serviceDirection: String = product.serviceDirection
    ) {
        val current = _cart.value.toMutableList()
        val targetVariantId = variant?.id
        val index = current.indexOfFirst {
            it.productId == product.id &&
                    it.unitName.equals(unitName, ignoreCase = true) &&
                    it.variantId == targetVariantId &&
                    it.bundleId == null
        }

        val baseSellingPrice = variant?.sellingPrice ?: price
        val stockLimit = variant?.stock ?: product.stock

        if (index >= 0 && customPrice == null) {
            val existing = current[index]
            val updatedQty = existing.quantity + 1.0
            val (resolvedPrice, source) = if (product.isFlexiblePrice) {
                existing.unitPrice to "fleksibel"
            } else {
                resolvePriceAndSource(product.id, unitName, multiplier, updatedQty, baseSellingPrice)
            }
            val isDigital = product.productType == "DIGITAL"
            current[index] = existing.copy(
                quantity = updatedQty,
                unitPrice = resolvedPrice,
                priceSource = source,
                serviceNominal = if (isDigital) resolvedPrice * updatedQty else existing.serviceNominal,
                storeAdminFee = if (isDigital) existing.storeAdminFee + storeAdminFee else existing.storeAdminFee,
                providerAdminFee = if (isDigital) existing.providerAdminFee + providerAdminFee else existing.providerAdminFee,
                agentCommission = if (isDigital) existing.agentCommission + agentCommission else existing.agentCommission
            )
        } else {
            val (resolvedPrice, source) = when {
                customPrice != null -> customPrice to if (product.isFlexiblePrice) "fleksibel" else "manual"
                else -> resolvePriceAndSource(product.id, unitName, multiplier, 1.0, baseSellingPrice)
            }
            current.add(
                CartItem(
                    productId = product.id,
                    productName = product.name,
                    unitName = unitName,
                    multiplier = multiplier,
                    unitPrice = resolvedPrice,
                    quantity = 1.0,
                    maxAvailableBaseStock = stockLimit,
                    variantId = variant?.id,
                    variantName = variant?.name ?: "",
                    priceSource = source,
                    serviceNominal = if (product.productType == "DIGITAL") resolvedPrice else null,
                    storeAdminFee = storeAdminFee,
                    providerAdminFee = providerAdminFee,
                    agentCommission = agentCommission,
                    serviceCashAccountId = serviceCashAccountId,
                    serviceDirection = serviceDirection
                )
            )
        }
        _cart.value = current
    }

    fun addBundleToCart(bundle: BundlePackageEntity) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.bundleId == bundle.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            current.add(
                CartItem(
                    productId = -bundle.id,
                    productName = bundle.name,
                    unitName = "paket",
                    multiplier = 1.0,
                    unitPrice = bundle.sellingPrice,
                    quantity = 1.0,
                    maxAvailableBaseStock = 999.0,
                    bundleId = bundle.id,
                    priceSource = "tetap"
                )
            )
        }
        _cart.value = current
    }

    fun updateCartItemQuantity(
        productId: Long,
        unitName: String,
        newQty: Double,
        variantId: Long? = null,
        bundleId: Long? = null
    ) {
        if (newQty <= 0) {
            removeFromCart(productId, unitName, variantId, bundleId)
            return
        }
        val baseProduct = products.value.find { it.id == productId }
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst {
            it.productId == productId &&
                    it.unitName == unitName &&
                    (variantId == null || it.variantId == variantId) &&
                    (bundleId == null || it.bundleId == bundleId)
        }
        if (index >= 0) {
            val item = current[index]
            val ratio = if (item.quantity > 0.0) newQty / item.quantity else 1.0
            val isDigitalItem = baseProduct?.productType == "DIGITAL" || item.serviceNominal != null
            if (item.bundleId != null || item.priceSource == "fleksibel" || item.priceSource == "manual") {
                current[index] = item.copy(
                    quantity = newQty,
                    serviceNominal = if (isDigitalItem) item.unitPrice * newQty else item.serviceNominal,
                    storeAdminFee = if (isDigitalItem) item.storeAdminFee * ratio else item.storeAdminFee,
                    providerAdminFee = if (isDigitalItem) item.providerAdminFee * ratio else item.providerAdminFee,
                    agentCommission = if (isDigitalItem) item.agentCommission * ratio else item.agentCommission
                )
            } else {
                val variantPrice = item.variantId?.let { vid -> productVariants.value.find { it.id == vid }?.sellingPrice }
                val fallback = variantPrice ?: baseProduct?.sellingPrice ?: item.unitPrice
                val (updatedUnitPrice, source) = resolvePriceAndSource(productId, unitName, item.multiplier, newQty, fallback)
                current[index] = item.copy(
                    quantity = newQty,
                    unitPrice = updatedUnitPrice,
                    priceSource = source,
                    serviceNominal = if (isDigitalItem) updatedUnitPrice * newQty else item.serviceNominal,
                    storeAdminFee = if (isDigitalItem) item.storeAdminFee * ratio else item.storeAdminFee,
                    providerAdminFee = if (isDigitalItem) item.providerAdminFee * ratio else item.providerAdminFee,
                    agentCommission = if (isDigitalItem) item.agentCommission * ratio else item.agentCommission
                )
            }
            _cart.value = current
        }
    }

    fun updateCartItemManualTotal(productId: Long, unitName: String, totalAmount: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.productId == productId && it.unitName == unitName }
        if (index >= 0 && current[index].quantity > 0) {
            val item = current[index]
            current[index] = item.copy(unitPrice = totalAmount / item.quantity, priceSource = "manual")
            _cart.value = current
        }
    }

    fun removeFromCart(
        productId: Long,
        unitName: String,
        variantId: Long? = null,
        bundleId: Long? = null
    ) {
        _cart.value = _cart.value.filterNot {
            it.productId == productId &&
                    it.unitName == unitName &&
                    (variantId == null || it.variantId == variantId) &&
                    (bundleId == null || it.bundleId == bundleId)
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartDiscount.value = 0.0
        _selectedCustomer.value = null
    }

    fun selectCustomerForSale(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun setDiscount(amount: Double) {
        _cartDiscount.value = amount
    }

    fun setTaxRate(rate: Double) {
        _cartTaxRate.value = rate
    }

    fun scanBarcodeAndAddToCart(rawCode: String, source: String = "CAMERA"): BarcodeLookupOutcome {
        val outcome = resolveBarcodeToCartMatch(
            rawCode = rawCode,
            products = products.value,
            variants = productVariants.value,
            conversions = unitConversions.value
        )
        val cleaned = rawCode.trim()
        when (outcome) {
            is BarcodeLookupOutcome.AddedVariant -> {
                addToCart(product = outcome.product, variant = outcome.variant)
                logBarcodeScanAsync(
                    BarcodeScanLogEntity(
                        barcode = cleaned,
                        productId = outcome.product.id,
                        variantId = outcome.variant.id,
                        productName = "${outcome.product.name} (${outcome.variant.name})",
                        matchedPrice = outcome.variant.sellingPrice,
                        matchedStock = outcome.variant.stock,
                        matched = true,
                        source = source
                    )
                )
            }
            is BarcodeLookupOutcome.AddedProduct -> {
                val product = outcome.product
                if (product.productType == "DIGITAL" && product.adminTierGroupId != null) {
                    val tierSvc = adminTiers.value.find { it.service.id == product.adminTierGroupId }
                    val calc = TierPricingResolver.calculate(tierSvc, product.sellingPrice)
                    addToCart(
                        product = product,
                        storeAdminFee = calc.storeAdminFee,
                        providerAdminFee = calc.providerAdminFee,
                        agentCommission = calc.agentCommission
                    )
                } else {
                    addToCart(product = product)
                }
                logBarcodeScanAsync(
                    BarcodeScanLogEntity(
                        barcode = cleaned,
                        productId = product.id,
                        productName = product.name,
                        matchedPrice = product.sellingPrice,
                        matchedStock = product.stock,
                        matched = true,
                        source = source
                    )
                )
            }
            is BarcodeLookupOutcome.NeedsSelection -> {
                logBarcodeScanAsync(
                    BarcodeScanLogEntity(
                        barcode = cleaned,
                        productId = outcome.product.id,
                        productName = outcome.product.name,
                        matchedPrice = outcome.product.sellingPrice,
                        matchedStock = outcome.product.stock,
                        matched = true,
                        source = source
                    )
                )
            }
            is BarcodeLookupOutcome.NotFound -> {
                if (cleaned.isNotEmpty()) {
                    logBarcodeScanAsync(
                        BarcodeScanLogEntity(
                            barcode = cleaned,
                            matched = false,
                            source = source
                        )
                    )
                }
            }
        }
        return outcome
    }

    private fun logBarcodeScanAsync(log: BarcodeScanLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.recordBarcodeScan(log) }
        }
    }

    fun quickCreateAndAddScannedProduct(
        name: String,
        barcode: String,
        sellingPrice: Double,
        costPrice: Double,
        stock: Double = 10.0,
        category: String = "Umum",
        baseUnit: String = "pcs"
    ) {
        val cleanName = name.trim()
        val cleanBarcode = barcode.trim()
        if (cleanName.isBlank() || sellingPrice <= 0.0) return
        viewModelScope.launch {
            val newProduct = ProductEntity(
                name = cleanName,
                barcode = cleanBarcode,
                category = category.ifBlank { "Umum" },
                baseUnit = baseUnit.ifBlank { "pcs" },
                sellingPrice = sellingPrice,
                purchasePrice = costPrice.coerceAtLeast(0.0),
                stock = stock.coerceAtLeast(0.0),
                productType = "FISIK",
                isActive = true
            )
            val insertedId = withContext(Dispatchers.IO) {
                repository.insertProduct(newProduct)
            }
            addToCart(newProduct.copy(id = insertedId))
            _userMessage.value = "Produk \"$cleanName\" disimpan & ditambahkan ke keranjang"
        }
    }

    fun checkoutSale(
        paymentMethod: String,
        amountPaid: Double,
        notes: String = "",
        storeAdminFee: Double = 0.0,
        providerAdminFee: Double = 0.0,
        agentCommission: Double = 0.0,
        cashAccountId: Long? = null,
        splitCashAmount: Double = 0.0,
        splitNonCashAmount: Double = 0.0,
        splitPaymentMethod: String = "",
        shippingCost: Double = 0.0,
        additionalFee: Double = 0.0,
        promoDiscount: Double = 0.0,
        appliedPromoName: String = "",
        depositUsed: Double = 0.0,
        isPreOrder: Boolean = false
    ) {
        val currentItems = _cart.value
        if (currentItems.isEmpty()) {
            _userMessage.value = "Keranjang belanja masih kosong!"
            return
        }

        viewModelScope.launch {
            try {
                val subtotal = currentItems.sumOf { it.unitPrice * it.quantity }
                val discount = _cartDiscount.value
                val tax = ((subtotal - discount).coerceAtLeast(0.0)) * _cartTaxRate.value
                val totalItemStoreAdmin = currentItems.sumOf { it.storeAdminFee }
                val finalAmount = (subtotal - discount - promoDiscount + tax + shippingCost + additionalFee + storeAdminFee + totalItemStoreAdmin).coerceAtLeast(0.0)
                val remainingPayable = (finalAmount - depositUsed).coerceAtLeast(0.0)

                if (!isPreOrder && paymentMethod == "TUNAI" && amountPaid < remainingPayable) {
                    _userMessage.value = "Uang tunai kurang dari total belanja!"
                    return@launch
                }

                val sale = repository.completeSale(
                    cashierName = _activeUserName.value,
                    customer = _selectedCustomer.value,
                    items = currentItems,
                    paymentMethod = paymentMethod,
                    amountPaid = amountPaid,
                    discount = discount,
                    tax = tax,
                    notes = notes,
                    storeAdminFee = storeAdminFee,
                    providerAdminFee = providerAdminFee,
                    agentCommission = agentCommission,
                    cashAccountId = cashAccountId,
                    splitCashAmount = splitCashAmount,
                    splitNonCashAmount = splitNonCashAmount,
                    splitPaymentMethod = splitPaymentMethod,
                    shippingCost = shippingCost,
                    additionalFee = additionalFee,
                    promoDiscount = promoDiscount,
                    appliedPromoName = appliedPromoName,
                    depositUsed = depositUsed,
                    isPreOrder = isPreOrder
                )

                _lastCompletedSale.value = sale
                _lastCompletedSaleItems.value = currentItems
                clearCart()
                _userMessage.value = if (isPreOrder) "Pre-Order ${sale.invoiceNumber} disimpan!" else "Transaksi ${sale.invoiceNumber} Berhasil Disimpan!"
                scheduleAutoSync()
            } catch (e: Exception) {
                _userMessage.value = "Gagal checkout: ${e.message}"
            }
        }
    }

    fun completePreOrder(saleId: Long) {
        viewModelScope.launch {
            try {
                repository.completePreOrder(saleId, _activeUserName.value)
                _userMessage.value = "Pre-Order diselesaikan & stok telah dipotong"
            } catch (e: Exception) {
                _userMessage.value = "Gagal menyelesaikan Pre-Order: ${e.message}"
            }
        }
    }

    fun dismissReceiptModal() {
        _lastCompletedSale.value = null
    }

    fun refundSale(saleId: Long, reason: String = "Retur Barang Pelanggan") {
        viewModelScope.launch {
            try {
                repository.refundSale(saleId, reason, _activeUserName.value)
                _userMessage.value = "Transaksi berhasil di-retur dan stok barang telah dikembalikan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal memproses retur: ${e.message}"
            }
        }
    }

    fun generateRandomBarcode(): String {
        val rand = (100000000..999999999).random()
        return "UNA$rand"
    }

    // Hold / Pending Sales
    fun holdCurrentCart(holdName: String, notes: String = "") {
        val currentItems = _cart.value
        if (currentItems.isEmpty()) {
            _userMessage.value = "Keranjang kosong, tidak ada transaksi untuk ditahan!"
            return
        }
        viewModelScope.launch {
            try {
                val subtotal = currentItems.sumOf { it.unitPrice * it.quantity }
                val discount = _cartDiscount.value
                val tax = ((subtotal - discount).coerceAtLeast(0.0)) * _cartTaxRate.value
                val itemStoreAdmin = currentItems.sumOf { it.storeAdminFee }
                val total = (subtotal - discount + tax + itemStoreAdmin).coerceAtLeast(0.0)
                repository.holdSale(holdName, currentItems, _selectedCustomer.value, total, notes)
                clearCart()
                _userMessage.value = "Transaksi berhasil ditahan: $holdName"
            } catch (e: Exception) {
                _userMessage.value = "Gagal menahan transaksi: ${e.message}"
            }
        }
    }

    fun resumeHoldSale(pending: PendingSaleEntity) {
        viewModelScope.launch {
            try {
                val json = pending.cartJson
                val items = mutableListOf<CartItem>()
                val parsedJsonArray = runCatching { JSONArray(json) }.getOrNull()
                if (parsedJsonArray != null) {
                    for (i in 0 until parsedJsonArray.length()) {
                        val obj = parsedJsonArray.optJSONObject(i) ?: continue
                        items.add(
                            CartItem(
                                productId = obj.optLong("id", 0L),
                                productName = obj.optString("name", "Produk"),
                                unitName = obj.optString("unit", "pcs"),
                                multiplier = obj.optDouble("mul", 1.0),
                                unitPrice = obj.optDouble("price", 0.0),
                                quantity = obj.optDouble("qty", 1.0),
                                maxAvailableBaseStock = obj.optDouble("maxStock", 999.0),
                                variantId = if (obj.has("variantId") && !obj.isNull("variantId")) obj.optLong("variantId") else null,
                                variantName = obj.optString("variantName", ""),
                                bundleId = if (obj.has("bundleId") && !obj.isNull("bundleId")) obj.optLong("bundleId") else null,
                                priceSource = obj.optString("priceSource", "tetap"),
                                serviceNominal = if (obj.has("serviceNominal") && !obj.isNull("serviceNominal")) obj.optDouble("serviceNominal") else null,
                                storeAdminFee = obj.optDouble("storeAdminFee", 0.0),
                                providerAdminFee = obj.optDouble("providerAdminFee", 0.0),
                                agentCommission = obj.optDouble("agentCommission", 0.0),
                                serviceCashAccountId = if (obj.has("serviceCashAccountId") && !obj.isNull("serviceCashAccountId")) obj.optLong("serviceCashAccountId") else null,
                                serviceDirection = obj.optString("serviceDirection", "SETOR")
                            )
                        )
                    }
                } else {
                    val regex = Regex("""\{"id":(-?\d+),"name":"(.*?)","unit":"(.*?)","qty":([\d.]+),"price":([\d.]+),"mul":([\d.]+)\}""")
                    regex.findAll(json).forEach { match ->
                        items.add(
                            CartItem(
                                productId = match.groupValues[1].toLong(),
                                productName = match.groupValues[2].replace("\\\"", "\""),
                                unitName = match.groupValues[3],
                                multiplier = match.groupValues[6].toDouble(),
                                unitPrice = match.groupValues[5].toDouble(),
                                quantity = match.groupValues[4].toDouble()
                            )
                        )
                    }
                }
                _cart.value = items
                if (pending.customerId != null) {
                    _selectedCustomer.value = customers.value.find { it.id == pending.customerId }
                }
                repository.deletePendingSale(pending.id)
                _userMessage.value = "Transaksi ${pending.holdName} dipulihkan ke kasir!"
                navigateTo(Screen.POS_KASIR)
            } catch (e: Exception) {
                _userMessage.value = "Gagal memulihkan transaksi: ${e.message}"
            }
        }
    }

    fun deletePendingSale(id: Long) {
        viewModelScope.launch {
            repository.deletePendingSale(id)
            _userMessage.value = "Transaksi pending dihapus"
        }
    }

    // Shift Operations
    fun startShift(cashierName: String, startingCash: Double, notes: String = "") {
        viewModelScope.launch {
            try {
                repository.startShift(cashierName, startingCash, notes)
                _userMessage.value = "Shift kasir berhasil dibuka (Modal awal Rp ${formatRupiah(startingCash)})"
            } catch (e: Exception) {
                _userMessage.value = "Gagal buka shift: ${e.message}"
            }
        }
    }

    fun closeShift(shiftId: Long, actualCash: Double, notes: String = "") {
        viewModelScope.launch {
            try {
                repository.closeShift(shiftId, actualCash, notes)
                _userMessage.value = "Shift kasir berhasil ditutup!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal tutup shift: ${e.message}"
            }
        }
    }

    fun recordShiftCashMovement(amount: Double, isCashIn: Boolean, reason: String) {
        viewModelScope.launch {
            try {
                repository.recordShiftCashMovement(amount, isCashIn, reason)
                _userMessage.value = "Kas ${if (isCashIn) "masuk" else "keluar"} shift tercatat!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal catat pergerakan kas shift: ${e.message}"
            }
        }
    }

    // Multi Kas
    fun createCashAccount(name: String, type: String, initialBalance: Double, color: String = "#2563EB", notes: String = "") {
        viewModelScope.launch {
            try {
                repository.insertCashAccount(
                    CashAccountEntity(
                        accountName = name,
                        balance = initialBalance,
                        accountType = type,
                        color = color,
                        notes = notes
                    )
                )
                _userMessage.value = "Akun kas $name berhasil dibuat!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal buat akun kas: ${e.message}"
            }
        }
    }

    fun setDefaultCashAccount(id: Long) {
        viewModelScope.launch {
            repository.setDefaultCashAccount(id)
            _userMessage.value = "Akun kas utama diperbarui"
        }
    }

    fun adjustCashAccountBalance(accountId: Long, newBalance: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustCashAccountBalance(accountId, newBalance, reason)
            _userMessage.value = "Saldo akun kas dikoreksi & dicatat di mutasi kas"
            scheduleAutoSync()
        }
    }

    fun transferCash(fromId: Long, toId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            try {
                repository.transferCash(fromId, toId, amount, notes, _activeUserName.value)
                _userMessage.value = "Transfer kas sebesar Rp ${formatRupiah(amount)} berhasil!"
                scheduleAutoSync()
            } catch (e: Exception) {
                _userMessage.value = "Gagal transfer kas: ${e.message}"
            }
        }
    }

    // Admin Tier Services & Ranges
    fun createAdminTierService(name: String, code: String) {
        viewModelScope.launch {
            try {
                repository.insertAdminTierService(AdminTierServiceEntity(serviceName = name, serviceCode = code))
                _userMessage.value = "Layanan $name berhasil ditambahkan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal tambah layanan: ${e.message}"
            }
        }
    }

    fun updateAdminTierService(service: AdminTierServiceEntity) {
        viewModelScope.launch {
            repository.updateAdminTierService(service)
            _userMessage.value = "Layanan ${service.serviceName} diperbarui"
        }
    }

    fun deleteAdminTierService(service: AdminTierServiceEntity) {
        viewModelScope.launch {
            repository.deleteAdminTierService(service)
            _userMessage.value = "Layanan ${service.serviceName} dan seluruh rentang tarifnya dihapus"
        }
    }

    fun createAdminTierRange(serviceId: Long, minAmt: Double, maxAmt: Double, storeFee: Double, providerFee: Double, agentFee: Double) {
        viewModelScope.launch {
            try {
                if (minAmt < 0.0 || (maxAmt > 0.0 && minAmt > maxAmt)) {
                    _userMessage.value = "Rentang tidak valid: nominal 'Dari' tidak boleh melebihi 'Sampai'"
                    return@launch
                }
                if (repository.hasOverlappingAdminTierRange(serviceId, 0L, minAmt, maxAmt)) {
                    _userMessage.value = "Rentang bertumpukan dengan aturan yang sudah ada!"
                    return@launch
                }
                repository.insertAdminTierRange(
                    AdminTierRangeEntity(
                        serviceId = serviceId,
                        minAmount = minAmt,
                        maxAmount = maxAmt,
                        storeAdminFee = storeFee,
                        providerAdminFee = providerFee,
                        agentCommission = agentFee
                    )
                )
                _userMessage.value = "Rentang tarif admin berhasil disimpan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal simpan rentang: ${e.message}"
            }
        }
    }

    fun updateAdminTierRange(range: AdminTierRangeEntity) {
        viewModelScope.launch {
            if (range.minAmount < 0.0 || (range.maxAmount > 0.0 && range.minAmount > range.maxAmount)) {
                _userMessage.value = "Rentang tidak valid: nominal 'Dari' melebihi 'Sampai'"
                return@launch
            }
            if (repository.hasOverlappingAdminTierRange(range.serviceId, range.id, range.minAmount, range.maxAmount)) {
                _userMessage.value = "Rentang bertumpukan dengan aturan lain di layanan ini!"
                return@launch
            }
            repository.updateAdminTierRange(range)
        }
    }

    fun deleteAdminTierRange(range: AdminTierRangeEntity) {
        viewModelScope.launch {
            repository.deleteAdminTierRange(range)
            _userMessage.value = "Rentang tarif dihapus"
        }
    }

    fun replaceAdminTierRanges(serviceId: Long, ranges: List<AdminTierRangeEntity>) {
        viewModelScope.launch {
            repository.replaceAdminTierRanges(serviceId, ranges)
            _userMessage.value = "Daftar tarif berjenjang (${ranges.size} rentang) diperbarui!"
        }
    }

    fun capAndAppendAdminTierRange(
        previousRange: AdminTierRangeEntity,
        cappedMaxAmount: Double,
        nextMinAmount: Double,
        nextMaxAmount: Double,
        nextStoreFee: Double,
        nextProviderFee: Double,
        nextAgentFee: Double
    ) {
        viewModelScope.launch {
            val capped = previousRange.copy(maxAmount = cappedMaxAmount)
            if (repository.hasOverlappingAdminTierRange(capped.serviceId, capped.id, capped.minAmount, capped.maxAmount)) {
                _userMessage.value = "Rentang bertumpukan dengan aturan lain di layanan ini!"
                return@launch
            }
            repository.updateAdminTierRange(capped)
            repository.insertAdminTierRange(
                AdminTierRangeEntity(
                    serviceId = previousRange.serviceId,
                    minAmount = nextMinAmount,
                    maxAmount = nextMaxAmount,
                    storeAdminFee = nextStoreFee,
                    providerAdminFee = nextProviderFee,
                    agentCommission = nextAgentFee
                )
            )
            _userMessage.value = "Rentang tarif admin baru ditambahkan!"
        }
    }

    // --- Product, Variants, Wholesale, Batches, Bundles, Promos Management ---

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
                _userMessage.value = "Produk \"${product.name}\" berhasil ditambahkan"
            } else {
                repository.updateProduct(product)
                _userMessage.value = "Produk \"${product.name}\" berhasil diperbarui"
            }
        }
    }

    fun saveManualProduct(
        existingProduct: ProductEntity?,
        name: String,
        sellingPrice: Double,
        purchasePrice: Double,
        stock: Double,
        minStock: Double,
        barcode: String,
        category: String,
        baseUnit: String,
        allowDecimalQty: Boolean
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || sellingPrice < 0.0) return

        val cleanCategory = category.trim().ifBlank { "Umum" }
        val cleanUnit = baseUnit.trim().ifBlank { "pcs" }
        val cleanBarcode = barcode.trim()

        viewModelScope.launch {
            if (existingProduct == null) {
                val created = ProductEntity(
                    name = cleanName,
                    barcode = cleanBarcode,
                    category = cleanCategory,
                    baseUnit = cleanUnit,
                    purchasePrice = purchasePrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    stock = stock,
                    minStock = minStock.coerceAtLeast(0.0),
                    productType = "FISIK",
                    allowDecimalQty = allowDecimalQty,
                    isActive = true
                )
                repository.insertProduct(created)
                _userMessage.value = "Produk \"$cleanName\" disimpan ke database"
            } else {
                val stockChanged = existingProduct.stock != stock
                val updated = existingProduct.copy(
                    name = cleanName,
                    barcode = cleanBarcode,
                    category = cleanCategory,
                    baseUnit = cleanUnit,
                    purchasePrice = purchasePrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    stock = stock,
                    minStock = minStock.coerceAtLeast(0.0),
                    allowDecimalQty = allowDecimalQty
                )
                repository.updateProduct(updated)
                if (stockChanged) {
                    repository.adjustStock(
                        productId = updated.id,
                        newStock = stock,
                        reason = "Pembaruan stok manual",
                        user = _activeUserName.value
                    )
                }
                _userMessage.value = "Produk \"$cleanName\" berhasil diperbarui"
            }
        }
    }

    fun stepProductStock(product: ProductEntity, delta: Double) {
        viewModelScope.launch {
            val nextStock = (product.stock + delta).coerceAtLeast(0.0)
            repository.adjustStock(
                productId = product.id,
                newStock = nextStock,
                reason = if (delta >= 0) "Tambah stok cepat" else "Kurangi stok cepat",
                user = _activeUserName.value
            )
            _userMessage.value = "Stok ${product.name}: ${if (product.allowDecimalQty) nextStock else nextStock.toInt()} ${product.baseUnit}"
        }
    }

    fun saveProductWithDetails(
        product: ProductEntity,
        conversions: List<UnitConversionEntity>,
        tierPrices: List<ProductTierPriceEntity>,
        variants: List<ProductVariantEntity> = emptyList(),
        wholesaleTiers: List<WholesaleTierEntity> = emptyList(),
        digitalTierRanges: List<AdminTierRangeEntity>? = null
    ) {
        viewModelScope.launch {
            var resolvedTierGroupId = product.adminTierGroupId
            if (product.productType == "DIGITAL" && digitalTierRanges != null) {
                if (resolvedTierGroupId != null) {
                    repository.replaceAdminTierRanges(resolvedTierGroupId, digitalTierRanges)
                } else if (digitalTierRanges.isNotEmpty()) {
                    val code = product.name.trim().uppercase(Locale.ROOT)
                        .replace(Regex("[^A-Z0-9]+"), "_")
                        .trim('_')
                        .ifBlank { "DIGITAL_SERVICE" }
                    val newServiceId = repository.insertAdminTierService(
                        AdminTierServiceEntity(
                            serviceName = product.name.trim(),
                            serviceCode = code,
                            isActive = true
                        )
                    )
                    repository.replaceAdminTierRanges(newServiceId, digitalTierRanges)
                    resolvedTierGroupId = newServiceId
                }
            }

            val finalProduct = product.copy(
                hasVariants = variants.isNotEmpty(),
                adminTierGroupId = if (product.productType == "DIGITAL") resolvedTierGroupId else null
            )
            val savedId = if (finalProduct.id == 0L) {
                repository.insertProduct(finalProduct)
            } else {
                repository.updateProduct(finalProduct)
                finalProduct.id
            }
            repository.replaceUnitConversions(savedId, conversions)
            repository.replaceProductTierPrices(savedId, tierPrices)
            repository.replaceProductVariants(savedId, variants)
            repository.replaceWholesaleTiers(savedId, wholesaleTiers)
            _userMessage.value = if (finalProduct.id == 0L) {
                "Produk \"${finalProduct.name}\" berhasil ditambahkan"
            } else {
                "Produk \"${finalProduct.name}\" berhasil diperbarui"
            }
        }
    }

    private data class DeletedProductSnapshot(
        val product: ProductEntity,
        val conversions: List<UnitConversionEntity>,
        val tierPrices: List<ProductTierPriceEntity>,
        val variants: List<ProductVariantEntity>,
        val wholesaleTiers: List<WholesaleTierEntity>,
        val aliases: List<ProductAliasEntity>
    )

    private var lastDeletedProduct: DeletedProductSnapshot? = null

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            lastDeletedProduct = DeletedProductSnapshot(
                product = product,
                conversions = repository.getConversionsList(product.id),
                tierPrices = repository.getTierPricesList(product.id),
                variants = repository.getVariantsForProductList(product.id),
                wholesaleTiers = repository.getWholesaleTiersList(product.id),
                aliases = repository.getAliasesForProductList(product.id)
            )
            repository.deleteProductCascade(product)
            _userMessage.value = "Produk dihapus"
        }
    }

    fun undoDeleteProduct() {
        val deleted = lastDeletedProduct ?: return
        viewModelScope.launch {
            val restoredId = repository.insertProduct(deleted.product)
            repository.replaceUnitConversions(restoredId, deleted.conversions)
            repository.replaceProductTierPrices(restoredId, deleted.tierPrices)
            repository.replaceProductVariants(restoredId, deleted.variants)
            repository.replaceWholesaleTiers(restoredId, deleted.wholesaleTiers)
            deleted.aliases.forEach { repository.learnProductAlias(restoredId, it.alias) }
            lastDeletedProduct = null
            _userMessage.value = "Produk \"${deleted.product.name}\" dikembalikan"
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(name)
            _userMessage.value = "Kategori \"$name\" berhasil ditambahkan"
        }
    }

    fun addUnitConversion(conversion: UnitConversionEntity) {
        viewModelScope.launch {
            repository.insertConversion(conversion)
            _userMessage.value = "Satuan ${conversion.unitName} berhasil disimpan"
        }
    }

    fun saveProductBatch(batch: ProductBatchEntity) {
        viewModelScope.launch {
            repository.insertBatch(batch)
            _userMessage.value = "Batch kedaluwarsa berhasil disimpan"
        }
    }

    fun deleteProductBatch(batch: ProductBatchEntity) {
        viewModelScope.launch {
            repository.deleteBatch(batch)
            _userMessage.value = "Batch dihapus"
        }
    }

    fun saveBundle(bundle: BundlePackageEntity, items: List<BundleItemEntity>) {
        viewModelScope.launch {
            repository.saveBundleWithItems(bundle, items)
            _userMessage.value = "Paket \"${bundle.name}\" berhasil disimpan"
        }
    }

    fun deleteBundle(bundle: BundlePackageEntity) {
        viewModelScope.launch {
            repository.deleteBundle(bundle)
            _userMessage.value = "Paket \"${bundle.name}\" dihapus"
        }
    }

    fun savePromo(promo: PromoEntity) {
        viewModelScope.launch {
            repository.savePromo(promo)
            _userMessage.value = "Promo \"${promo.name}\" berhasil disimpan"
        }
    }

    fun togglePromoActive(promo: PromoEntity) {
        viewModelScope.launch {
            repository.updatePromo(promo.copy(isActive = !promo.isActive))
        }
    }

    fun deletePromo(promo: PromoEntity) {
        viewModelScope.launch {
            repository.deletePromo(promo)
            _userMessage.value = "Promo dihapus"
        }
    }

    // --- Store Profile, Users, Permissions, Printers, Devices ---

    fun saveStoreProfile(profile: StoreProfileEntity) {
        viewModelScope.launch {
            repository.saveStoreProfile(profile)
            _cartTaxRate.value = profile.ppnBp / 10000.0
            _userMessage.value = "Profil toko & pengaturan PPN disimpan"
        }
    }

    fun saveUserAccount(user: UserAccountEntity, permissions: Map<String, Boolean>) {
        viewModelScope.launch {
            if (!(_activeRole.value.equals("Owner", ignoreCase = true) || _activeRole.value.equals("Admin", ignoreCase = true))) {
                _userMessage.value = "Hanya Admin Utama / Owner yang dapat mengelola akun pengguna"
                return@launch
            }
            val normalizedUsername = user.username.trim().lowercase()
            if (normalizedUsername.length < 3 || normalizedUsername.contains(" ")) {
                _userMessage.value = "Username minimal 3 karakter dan tanpa spasi"
                return@launch
            }
            val existingUsers = withContext(Dispatchers.IO) {
                database.extendedPosDao().getAllUsersList()
            }
            val duplicate = existingUsers.any {
                it.id != user.id && it.username.trim().equals(normalizedUsername, ignoreCase = true)
            }
            if (duplicate) {
                _userMessage.value = "Username \"$normalizedUsername\" sudah digunakan"
                return@launch
            }
            val activeOwnersAfter = existingUsers.count {
                it.id != user.id && it.isActive && it.role.equals("owner", ignoreCase = true)
            } + if (user.isActive && user.role.equals("owner", ignoreCase = true)) 1 else 0
            if (activeOwnersAfter == 0) {
                _userMessage.value = "Minimal harus ada 1 akun Admin Utama / Owner yang aktif"
                return@launch
            }

            val sanitized = user.copy(
                name = user.name.trim(),
                username = normalizedUsername,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveUserWithPermissions(sanitized, permissions)
            if (sanitized.id == _activeUserId.value && sanitized.id != 0L) {
                _activeUserName.value = sanitized.name
                _activeRole.value = if (sanitized.role.equals("owner", ignoreCase = true)) "Owner" else "Kasir"
            }
            _userMessage.value = "Pengguna \"${sanitized.name}\" (@$normalizedUsername) disimpan"
        }
    }

    fun deleteUserAccount(user: UserAccountEntity) {
        viewModelScope.launch {
            if (!(_activeRole.value.equals("Owner", ignoreCase = true) || _activeRole.value.equals("Admin", ignoreCase = true))) {
                _userMessage.value = "Hanya Admin Utama / Owner yang dapat menghapus akun"
                return@launch
            }
            if (user.id == _activeUserId.value) {
                _userMessage.value = "Tidak dapat menghapus akun yang sedang aktif digunakan"
                return@launch
            }
            val existingUsers = withContext(Dispatchers.IO) {
                database.extendedPosDao().getAllUsersList()
            }
            val remainingActiveOwners = existingUsers.count {
                it.id != user.id && it.isActive && it.role.equals("owner", ignoreCase = true)
            }
            if (user.role.equals("owner", ignoreCase = true) && remainingActiveOwners == 0) {
                _userMessage.value = "Minimal harus ada 1 Admin Utama / Owner tersisa"
                return@launch
            }
            repository.deleteUser(user)
            _userMessage.value = "Akun ${user.name} dihapus"
        }
    }

    fun savePrinter(printer: PrinterEntity) {
        viewModelScope.launch {
            repository.savePrinter(printer)
            _userMessage.value = "Printer \"${printer.name}\" disimpan"
        }
    }

    fun deletePrinter(printer: PrinterEntity) {
        viewModelScope.launch {
            repository.deletePrinter(printer)
            _userMessage.value = "Printer dihapus"
        }
    }

    fun saveCashierDevice(device: CashierDeviceEntity) {
        viewModelScope.launch {
            repository.saveCashierDevice(device)
            _userMessage.value = "Perangkat \"${device.name}\" disimpan"
        }
    }

    fun deleteCashierDevice(device: CashierDeviceEntity) {
        viewModelScope.launch {
            repository.deleteCashierDevice(device)
            _userMessage.value = "Perangkat dihapus"
        }
    }

    // --- Purchase & Goods Receipt ---

    fun createPurchaseOrder(
        invoiceNumber: String,
        supplier: SupplierEntity?,
        items: List<PurchaseItemDraft>,
        paymentMethod: String,
        discount: Double = 0.0,
        additionalCost: Double = 0.0,
        notes: String = "",
        initialStatus: String = "DRAFT"
    ) {
        viewModelScope.launch {
            try {
                val purchase = repository.createPurchase(
                    invoiceNumber = invoiceNumber,
                    supplier = supplier,
                    items = items,
                    paymentMethod = paymentMethod,
                    discount = discount,
                    additionalCost = additionalCost,
                    notes = notes,
                    initialStatus = initialStatus
                )
                val msg = if (initialStatus == "DITERIMA")
                    "Pembelian & Penerimaan Barang Berhasil! Stok telah bertambah."
                else
                    "Draft Pembelian ${purchase.invoiceNumber} Dibuat! (Stok belum bertambah sampai barang diterima)"
                _userMessage.value = msg
            } catch (e: Exception) {
                _userMessage.value = "Gagal membuat pembelian: ${e.message}"
            }
        }
    }

    fun confirmGoodsReceipt(purchaseId: Long) {
        viewModelScope.launch {
            try {
                repository.confirmPurchaseReceipt(purchaseId, _activeUserName.value)
                _userMessage.value = "Penerimaan Barang Selesai! Stok gudang telah diperbarui."
            } catch (e: Exception) {
                _userMessage.value = "Gagal memproses penerimaan: ${e.message}"
            }
        }
    }

    fun cancelPurchase(purchaseId: Long) {
        viewModelScope.launch {
            repository.cancelPurchase(purchaseId)
            _userMessage.value = "Pembelian berhasil dibatalkan"
        }
    }

    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseItemEntity> =
        repository.getItemsForPurchase(purchaseId)

    // --- Stock Inventory Adjustments ---

    fun adjustStock(productId: Long, newStock: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, newStock, reason, _activeUserName.value)
            _userMessage.value = "Penyesuaian stok berhasil disimpan"
        }
    }

    fun quickStockIn(productId: Long, qty: Double, unit: String, reason: String) {
        viewModelScope.launch {
            repository.quickStockIn(productId, qty, unit, reason, _activeUserName.value)
            _userMessage.value = "Stok masuk berhasil dicatat"
        }
    }

    // --- Financials ---

    fun recordExpense(
        category: String,
        amount: Double,
        notes: String,
        source: String = "Kas Toko",
        cashAccountId: Long? = null
    ) {
        viewModelScope.launch {
            repository.recordExpense(category, amount, notes, source, _activeUserName.value, cashAccountId)
            _userMessage.value = "Pengeluaran Rp ${formatRupiah(amount)} berhasil dicatat"
            scheduleAutoSync()
        }
    }

    fun recordIncome(
        source: String,
        amount: Double,
        notes: String,
        cashAccountId: Long? = null
    ) {
        viewModelScope.launch {
            repository.recordIncome(source, amount, notes, _activeUserName.value, cashAccountId)
            _userMessage.value = "Pemasukan Rp ${formatRupiah(amount)} berhasil dicatat"
            scheduleAutoSync()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _userMessage.value = "Pengeluaran dihapus & saldo kas dikembalikan"
            scheduleAutoSync()
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.deleteIncome(income)
            _userMessage.value = "Pemasukan dihapus & saldo kas disesuaikan"
            scheduleAutoSync()
        }
    }

    fun paySupplierDebt(
        supplierId: Long,
        amount: Double,
        invoice: String,
        method: String,
        notes: String,
        cashAccountId: Long? = null
    ) {
        viewModelScope.launch {
            val supplier = suppliers.value.find { it.id == supplierId } ?: return@launch
            repository.recordDebtPayment(
                type = "UTANG_PEMASOK",
                partyId = supplierId,
                partyName = supplier.name,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                user = _activeUserName.value,
                cashAccountId = cashAccountId
            )
            _userMessage.value = "Pembayaran utang Rp ${formatRupiah(amount)} ke ${supplier.name} berhasil"
            scheduleAutoSync()
        }
    }

    fun payCustomerReceivable(
        customerId: Long,
        amount: Double,
        invoice: String,
        method: String,
        notes: String,
        cashAccountId: Long? = null
    ) {
        viewModelScope.launch {
            val customer = customers.value.find { it.id == customerId } ?: return@launch
            repository.recordDebtPayment(
                type = "PIUTANG_PELANGGAN",
                partyId = customerId,
                partyName = customer.name,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                user = _activeUserName.value,
                cashAccountId = cashAccountId
            )
            _userMessage.value = "Penerimaan piutang Rp ${formatRupiah(amount)} dari ${customer.name} berhasil"
            scheduleAutoSync()
        }
    }

    // --- Contacts & Deposit ---

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            if (customer.id == 0L) repository.insertCustomer(customer)
            else repository.updateCustomer(customer)
            _userMessage.value = "Pelanggan \"${customer.name}\" disimpan"
        }
    }

    fun topUpCustomerDeposit(customer: CustomerEntity, amount: Double) {
        viewModelScope.launch {
            repository.topUpCustomerDeposit(customer, amount)
            _userMessage.value = "Deposit ${customer.name} ditambah Rp ${formatRupiah(amount)}"
        }
    }

    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            if (supplier.id == 0L) repository.insertSupplier(supplier)
            else repository.updateSupplier(supplier)
            _userMessage.value = "Pemasok \"${supplier.name}\" disimpan"
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _userMessage.value = "Pelanggan \"${customer.name}\" dihapus"
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            _userMessage.value = "Pemasok \"${supplier.name}\" dihapus"
        }
    }

    // --- AI Chat Command Center ---

    fun sendUserChatMessage(text: String) {
        if (text.isBlank()) return
        val current = _chatMessages.value.toMutableList()
        current.add(ChatMessage(isUser = true, senderName = _activeUserName.value, text = text))
        _chatMessages.value = current
        _isAILoading.value = true

        viewModelScope.launch {
            try {
                val parseResult = aiEngine.processCommand(text)
                val aiMsg = ChatMessage(
                    isUser = false,
                    senderName = "Gerai Una AI",
                    text = parseResult.summary,
                    draftResult = parseResult,
                    isActionPending = parseResult.confirmationRequired && parseResult.isExecutable,
                    isActionResolved = false
                )
                val updated = _chatMessages.value.toMutableList()
                updated.add(aiMsg)
                _chatMessages.value = updated

                repository.logAICommand(
                    AICommandLogEntity(
                        source = "APP_CHAT",
                        senderPhone = "LOCAL_USER",
                        rawMessage = text,
                        parsedIntent = parseResult.intent.name,
                        structuredJson = parseResult.draftJson,
                        status = if (parseResult.confirmationRequired) "DRAFT" else "EXECUTED",
                        responseText = parseResult.summary
                    )
                )
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    isUser = false,
                    senderName = "Gerai Una AI",
                    text = "Terjadi kendala saat memproses perintah: ${e.message}"
                )
                val updated = _chatMessages.value.toMutableList()
                updated.add(errorMsg)
                _chatMessages.value = updated
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun confirmAIDraft(messageId: String) {
        val current = _chatMessages.value.toMutableList()
        val index = current.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val msg = current[index]
        val draft = msg.draftResult ?: return

        viewModelScope.launch {
            try {
                when (draft.intent) {
                    AIIntent.EXPENSE -> {
                        repository.recordExpense(
                            category = draft.category,
                            amount = draft.amount,
                            notes = draft.notes,
                            source = "Kas Toko",
                            user = _activeUserName.value
                        )
                        _userMessage.value = "Pengeluaran Rp ${formatRupiah(draft.amount)} berhasil disimpan!"
                    }
                    AIIntent.INCOME -> {
                        repository.recordIncome(
                            source = draft.category,
                            amount = draft.amount,
                            notes = draft.notes,
                            user = _activeUserName.value
                        )
                        _userMessage.value = "Pemasukan Rp ${formatRupiah(draft.amount)} berhasil disimpan!"
                    }
                    AIIntent.STOCK_IN -> {
                        val item = draft.items.firstOrNull()
                        if (item?.matchedProduct != null) {
                            repository.quickStockIn(
                                productId = item.matchedProduct.id,
                                quantity = item.quantity,
                                unit = item.unit,
                                reason = "Stok masuk via AI Chat",
                                user = _activeUserName.value
                            )
                            val qtyLabel = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString()
                            else String.format(Locale.US, "%.2f", item.quantity).trimEnd('0').trimEnd('.')
                            _userMessage.value = "Stok ${item.matchedProduct.name} (+$qtyLabel ${item.unit}) berhasil diperbarui!"
                        }
                    }
                    AIIntent.PURCHASE_ORDER -> {
                        val supplier = suppliers.value.find { it.name.equals(draft.supplierName, ignoreCase = true) }
                            ?: suppliers.value.firstOrNull()
                        val drafts = draft.items.map {
                            PurchaseItemDraft(
                                productId = it.matchedProduct?.id ?: 0L,
                                productName = it.rawName,
                                quantity = it.quantity,
                                unit = it.unit,
                                purchasePrice = it.price
                            )
                        }
                        repository.createPurchase(
                            invoiceNumber = "",
                            supplier = supplier,
                            items = drafts,
                            paymentMethod = "TUNAI",
                            notes = draft.notes,
                            initialStatus = "DRAFT"
                        )
                        _userMessage.value = "Draft Pembelian berhasil disimpan ke menu Pembelian!"
                    }
                    AIIntent.CREATE_PRODUCT -> {
                        val item = draft.items.firstOrNull()
                        if (item != null) {
                            val parsedPurchase = runCatching {
                                org.json.JSONObject(draft.draftJson).optDouble("purchase_price", Double.NaN)
                            }.getOrNull()?.takeIf { !it.isNaN() }
                                ?: draft.notes.substringAfter("Modal: ").substringBefore(",").toDoubleOrNull()
                                ?: 0.0
                            val newProduct = ProductEntity(
                                name = item.rawName,
                                category = "Umum",
                                baseUnit = item.unit,
                                purchasePrice = parsedPurchase,
                                sellingPrice = item.price,
                                stock = 0.0,
                                minStock = 5.0
                            )
                            val insertedId = repository.insertProduct(newProduct)
                            repository.learnProductAlias(insertedId, item.rawName)
                            _userMessage.value = "Produk baru \"${item.rawName}\" berhasil ditambahkan ke katalog!"
                        }
                    }
                    else -> {}
                }

                current[index] = msg.copy(isActionPending = false, isActionResolved = true)
                current.add(
                    ChatMessage(
                        isUser = false,
                        senderName = "Gerai Una AI",
                        text = "✅ Tindakan berhasil dieksekusi dan disimpan ke database Gerai Una."
                    )
                )
                _chatMessages.value = current
            } catch (e: Exception) {
                _userMessage.value = "Gagal mengeksekusi draft: ${e.message}"
            }
        }
    }

    fun rejectAIDraft(messageId: String) {
        val current = _chatMessages.value.toMutableList()
        val index = current.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            val msg = current[index]
            current[index] = msg.copy(isActionPending = false, isActionResolved = true)
            current.add(
                ChatMessage(
                    isUser = false,
                    senderName = "Gerai Una AI",
                    text = "❌ Tindakan dibatalkan oleh pengguna. Database tidak diubah."
                )
            )
            _chatMessages.value = current
            _userMessage.value = "Tindakan dibatalkan"
        }
    }

    // --- WhatsApp Command Simulator ---
    private val _whatsAppTestReply = MutableStateFlow<WhatsAppMessageResponse?>(null)
    val whatsAppTestReply: StateFlow<WhatsAppMessageResponse?> = _whatsAppTestReply.asStateFlow()

    fun simulateIncomingWhatsApp(senderPhone: String, messageText: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val response = whatsAppCommandCenter.processIncomingWhatsAppMessage(senderPhone, messageText)
                _whatsAppTestReply.value = response
                _userMessage.value = if (response.isAuthorized) "Pesan WhatsApp Berhasil Diproses!" else "Akses WhatsApp Ditolak!"
            } catch (e: Exception) {
                _userMessage.value = "Error WhatsApp: ${e.message}"
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun saveOwnerWhatsAppPhone(phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) {
            _userMessage.value = "Nomor WhatsApp pemilik tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            repository.setSetting("owner_authorized_phone", cleaned)
            _userMessage.value = "Nomor WhatsApp pemilik diperbarui ke $cleaned"
        }
    }

    fun resetDatabaseToClean() {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    stopCloudListeners()
                    auth?.signOut()
                    firebaseSessionStore.setOfflineBypass(false)
                    firebaseSessionStore.setStoreNameHint("Toko Saya")
                }
                repository.resetDatabaseToInitial()
                clearCart()
                _activeUserId.value = 0L
                _activeRole.value = ""
                _activeUserName.value = ""
                _currentScreen.value = Screen.DASHBOARD
                _authUiState.update {
                    it.copy(
                        hasEnteredApp = false,
                        isCloudSignedIn = false,
                        isOfflineMode = false,
                        userId = "",
                        userEmail = "",
                        fullName = "",
                        storeName = "Toko Saya",
                        authError = null,
                        authInfoMessage = "Database bersih tanpa data dummy. Silakan buat akun Admin Utama."
                    )
                }
                _userMessage.value = "Database berhasil dibersihkan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal reset: ${e.message}"
            }
        }
    }

    // --- Import Backup KasGo (.kasgo) ---
    private val _kasGoImportState = MutableStateFlow(KasGoImportUiState())
    val kasGoImportState: StateFlow<KasGoImportUiState> = _kasGoImportState.asStateFlow()

    fun inspectKasGoBackupUri(uri: Uri) {
        viewModelScope.launch {
            _kasGoImportState.update {
                it.copy(isInspecting = true, inspectResult = null, errorMessage = null)
            }
            val res = KasGoBackupImporter.inspectBackup(getApplication(), uri)
            if (res.isValid) {
                _kasGoImportState.update {
                    it.copy(isInspecting = false, inspectResult = res, errorMessage = null)
                }
            } else {
                _kasGoImportState.update {
                    it.copy(isInspecting = false, inspectResult = null, errorMessage = res.errorMessage ?: "Gagal membaca file .kasgo")
                }
            }
        }
    }

    fun confirmKasGoImport(options: KasGoImportOptions) {
        val currentInspect = _kasGoImportState.value.inspectResult ?: return
        viewModelScope.launch {
            _kasGoImportState.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                val summary = KasGoBackupImporter.importBackup(
                    context = getApplication(),
                    db = database,
                    inspectResult = currentInspect,
                    options = options
                )
                _kasGoImportState.value = KasGoImportUiState(lastImportSummary = summary)
                _userMessage.value = "Import .kasgo berhasil: ${summary.productsImported} produk, ${summary.salesImported} transaksi, ${summary.photosCopied} foto!"
                scheduleAutoSync()
            } catch (e: Exception) {
                _kasGoImportState.update {
                    it.copy(isImporting = false, errorMessage = "Gagal mengimpor backup: ${e.message}")
                }
            }
        }
    }

    fun dismissKasGoImportPreview() {
        _kasGoImportState.value.inspectResult?.let { KasGoBackupImporter.cleanupTemp(it) }
        _kasGoImportState.update {
            it.copy(isInspecting = false, isImporting = false, inspectResult = null, errorMessage = null)
        }
    }

    private fun formatRupiah(amount: Double): String {
        return java.text.NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.toLong())
    }
}
