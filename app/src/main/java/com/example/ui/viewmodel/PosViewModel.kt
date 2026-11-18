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
    CLOUD_MONITOR
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
            if (firebaseSessionStore.isOfflineBypass()) {
                _authUiState.update {
                    it.copy(
                        hasEnteredApp = true,
                        isOfflineMode = true,
                        isCloudSignedIn = false,
                        userId = "",
                        userEmail = ""
                    )
                }
            }
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

    // Active User Role & Account
    private val _activeRole = MutableStateFlow("Owner")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _activeUserName = MutableStateFlow("Egis Bahricha (Owner)")
    val activeUserName: StateFlow<String> = _activeUserName.asStateFlow()

    private val _activeUserId = MutableStateFlow(1L)
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
        val hash = GeraiUnaDatabase.hashPin(pin)
        val activeUser = userAccounts.value.find { it.id == _activeUserId.value }
        return if (activeUser != null) {
            activeUser.pinHash == hash || pin == "123456" && activeUser.pinHash == GeraiUnaDatabase.hashPin("123456")
        } else {
            pin == "123456"
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
                val existingProducts = database.productDao().getAllProductsList()
                if (existingProducts.isEmpty()) {
                    GeraiUnaDatabase.populateInitialData(database)
                } else {
                    val existingAccounts = database.cashAccountDao().getAllAccountsList()
                    if (existingAccounts.isEmpty()) {
                        val cashAccountDao = database.cashAccountDao()
                        cashAccountDao.insertCashAccount(
                            CashAccountEntity(accountName = "Kas Toko", balance = 1500000.0, accountType = "CASH", color = "#16A34A", notes = "Uang fisik laci kasir gerai utama", isDefault = true, sortOrder = 1)
                        )
                        cashAccountDao.insertCashAccount(
                            CashAccountEntity(accountName = "Deposit Pulsa", balance = 750000.0, accountType = "PULSA_DEPOSIT", color = "#EA580C", notes = "Saldo server pulsa / chip konter", sortOrder = 2)
                        )
                        cashAccountDao.insertCashAccount(
                            CashAccountEntity(accountName = "Saldo BRILink / Agen Bank", balance = 4250000.0, accountType = "BANK_AGENT", color = "#2563EB", notes = "Saldo operasional transaksi transfer agen", sortOrder = 3)
                        )
                    }
                    val existingServices = database.adminTierDao().getAllServicesList()
                    if (existingServices.isEmpty()) {
                        val adminTierDao = database.adminTierDao()
                        val s1 = adminTierDao.insertService(AdminTierServiceEntity(serviceName = "Transfer Antar Bank", serviceCode = "TRANSFER_BANK"))
                        adminTierDao.insertRange(AdminTierRangeEntity(serviceId = s1, minAmount = 0.0, maxAmount = 1000000.0, storeAdminFee = 5000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))
                        adminTierDao.insertRange(AdminTierRangeEntity(serviceId = s1, minAmount = 1000001.0, maxAmount = 3000000.0, storeAdminFee = 8000.0, providerAdminFee = 5000.0, agentCommission = 2500.0))
                    }
                    val extDao = database.extendedPosDao()
                    if (extDao.getStoreProfileSync() == null) {
                        extDao.upsertStoreProfile(StoreProfileEntity())
                    }
                    if (extDao.getAllUsersList().isEmpty()) {
                        val defaultHash = GeraiUnaDatabase.hashPin("123456")
                        val ownerId = extDao.insertUser(UserAccountEntity(name = "Egis Bahricha (Owner)", username = "owner", role = "owner", pinHash = defaultHash))
                        val kasirId = extDao.insertUser(UserAccountEntity(name = "Kasir 1", username = "kasir1", role = "kasir", pinHash = defaultHash))
                        val allMenus = listOf("POS_KASIR", "RIWAYAT_TRANSAKSI", "PRODUK", "PEMBELIAN", "STOK_INVENTARIS", "MULTI_KAS", "TARIF_ADMIN", "KEUANGAN", "PENGELUARAN", "UTANG_PIUTANG", "LAPORAN", "AI_CHAT", "WHATSAPP", "KONTAK", "PENGATURAN")
                        allMenus.forEach { m ->
                            extDao.insertMenuPermission(MenuPermissionEntity(userId = ownerId, menuKey = m, allowed = true))
                            extDao.insertMenuPermission(MenuPermissionEntity(userId = kasirId, menuKey = m, allowed = m in listOf("POS_KASIR", "RIWAYAT_TRANSAKSI", "PRODUK", "STOK_INVENTARIS", "TARIF_ADMIN", "KONTAK")))
                        }
                        extDao.insertPrinter(PrinterEntity(name = "Printer Thermal 58mm"))
                        extDao.insertCashierDevice(CashierDeviceEntity(name = "Kasir Utama Gerai Una", role = "utama"))
                    }
                }
                // Sync tax rate from store profile
                val profile = database.extendedPosDao().getStoreProfileSync()
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
                    } else if (firebaseSessionStore.isOfflineBypass()) {
                        _authUiState.update {
                            it.copy(
                                hasEnteredApp = true,
                                isOfflineMode = true,
                                isCloudSignedIn = false
                            )
                        }
                    }
                } else if (firebaseSessionStore.isOfflineBypass()) {
                    _authUiState.update {
                        it.copy(
                            hasEnteredApp = true,
                            isOfflineMode = true,
                            isCloudSignedIn = false
                        )
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

    fun enterOfflineMode(localUser: UserAccountEntity? = null) {
        firebaseSessionStore.setOfflineBypass(true)
        if (localUser != null) {
            switchUserAccount(localUser)
        }
        _authUiState.update {
            it.copy(
                hasEnteredApp = true,
                isOfflineMode = true,
                isCloudSignedIn = false,
                authError = null,
                authInfoMessage = null
            )
        }
    }

    fun logoutToLoginScreen() {
        viewModelScope.launch {
            stopCloudListeners()
            auth?.signOut()
            firebaseSessionStore.setOfflineBypass(false)
            GoogleAuthHelper.clearCredentialState(getApplication())
            _currentScreen.value = Screen.DASHBOARD
            _authUiState.update {
                it.copy(
                    hasEnteredApp = false,
                    isCloudSignedIn = false,
                    isOfflineMode = false,
                    userId = "",
                    userEmail = "",
                    authError = null,
                    authInfoMessage = "Anda telah keluar. Pilih akun Google lain atau masuk di perangkat lain."
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
                senderName = "Gerai Una AI",
                text = "Halo Egis Bahricha! Saya Asisten AI Gerai Una.\nKetik perintah operasional toko apa saja, misalnya:\n• \"Baru belanja di grosir aqua 2 dus harga 45 ribu, indomie goreng 5 karton harga 110 ribu\"\n• \"Tambah produk baru Rinso cair 800 ml modal 14 ribu jual 18 ribu\"\n• \"Pengeluaran listrik 250 ribu\"\n• \"Tambah stok Aqua 20 botol\"\n• \"Berapa stok Aqua?\"\n• \"Berapa utang saya ke grosir?\""
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
                val tax = ((subtotal - discount - promoDiscount).coerceAtLeast(0.0)) * _cartTaxRate.value
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
                val tax = (subtotal - discount) * _cartTaxRate.value
                val total = (subtotal - discount + tax).coerceAtLeast(0.0)
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
                val regex = Regex("""\{"id":(\d+),"name":"(.*?)","unit":"(.*?)","qty":([\d.]+),"price":([\d.]+),"mul":([\d.]+)\}""")
                regex.findAll(json).forEach { match ->
                    val id = match.groupValues[1].toLong()
                    val name = match.groupValues[2].replace("\\\"", "\"")
                    val unit = match.groupValues[3]
                    val qty = match.groupValues[4].toDouble()
                    val price = match.groupValues[5].toDouble()
                    val mul = match.groupValues[6].toDouble()
                    items.add(
                        CartItem(
                            productId = id,
                            productName = name,
                            unitName = unit,
                            multiplier = mul,
                            unitPrice = price,
                            quantity = qty
                        )
                    )
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
            conversions.forEach { repository.insertConversion(it.copy(productId = savedId)) }
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

    private var lastDeletedProduct: ProductEntity? = null

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            lastDeletedProduct = product
            repository.deleteProduct(product)
            _userMessage.value = "Produk dihapus"
        }
    }

    fun undoDeleteProduct() {
        val deleted = lastDeletedProduct ?: return
        viewModelScope.launch {
            repository.insertProduct(deleted)
            lastDeletedProduct = null
            _userMessage.value = "Produk \"${deleted.name}\" dikembalikan"
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
            repository.saveUserWithPermissions(user, permissions)
            _userMessage.value = "Pengguna \"${user.name}\" & izin menu disimpan"
        }
    }

    fun deleteUserAccount(user: UserAccountEntity) {
        viewModelScope.launch {
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

    fun recordExpense(category: String, amount: Double, notes: String, source: String = "Kas Toko") {
        viewModelScope.launch {
            repository.recordExpense(category, amount, notes, source, _activeUserName.value)
            _userMessage.value = "Pengeluaran Rp ${formatRupiah(amount)} berhasil dicatat"
            scheduleAutoSync()
        }
    }

    fun recordIncome(source: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.recordIncome(source, amount, notes, _activeUserName.value)
            _userMessage.value = "Pemasukan Rp ${formatRupiah(amount)} berhasil dicatat"
            scheduleAutoSync()
        }
    }

    fun paySupplierDebt(supplierId: Long, amount: Double, invoice: String, method: String, notes: String) {
        viewModelScope.launch {
            val supplier = suppliers.value.find { it.id == supplierId } ?: return@launch
            repository.recordDebtPayment(
                type = "UTANG_PEMASOK",
                partyId = supplierId,
                partyName = supplier.name,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                user = _activeUserName.value
            )
            _userMessage.value = "Pembayaran utang Rp ${formatRupiah(amount)} ke ${supplier.name} berhasil"
            scheduleAutoSync()
        }
    }

    fun payCustomerReceivable(customerId: Long, amount: Double, invoice: String, method: String, notes: String) {
        viewModelScope.launch {
            val customer = customers.value.find { it.id == customerId } ?: return@launch
            repository.recordDebtPayment(
                type = "PIUTANG_PELANGGAN",
                partyId = customerId,
                partyName = customer.name,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                user = _activeUserName.value
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
                            _userMessage.value = "Stok ${item.matchedProduct.name} (+${item.quantity.toInt()} ${item.unit}) berhasil diperbarui!"
                        }
                    }
                    AIIntent.PURCHASE_ORDER -> {
                        val supplier = suppliers.value.find { it.name.equals(draft.supplierName, ignoreCase = true) }
                            ?: suppliers.value.firstOrNull()
                        val drafts = draft.items.map {
                            PurchaseItemDraft(
                                productId = it.matchedProduct?.id ?: 1L,
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
                            val newProduct = ProductEntity(
                                name = item.rawName,
                                category = "Umum",
                                baseUnit = item.unit,
                                purchasePrice = draft.notes.substringAfter("Modal: ").substringBefore(",").toDoubleOrNull() ?: 0.0,
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

    fun resetDatabaseToDemo() {
        viewModelScope.launch {
            try {
                repository.resetDatabaseToInitial()
                clearCart()
                _userMessage.value = "Database Gerai Una berhasil di-reset ke data bawaan!"
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
