package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ai.AIIntent
import com.example.ai.AIParseResult
import com.example.ai.GeraiUnaAIEngine
import com.example.data.backup.KasLiteBackupInspectResult
import com.example.data.backup.KasLiteBackupManager
import com.example.data.backup.KasLiteRestoreSummary
import com.example.data.database.GeraiUnaDatabase
import com.example.data.entity.*
import com.example.data.excel.ExcelProductImportSummary
import com.example.data.excel.ExcelProductInspectResult
import com.example.data.excel.ExcelProductReportManager
import com.example.data.pricing.TierPricingResolver
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.PurchaseItemDraft
import com.example.firebase.BugReportDoc
import com.example.firebase.DeveloperBankConfig
import com.example.firebase.DonationApprovalOutcome
import com.example.firebase.DonationPackage
import com.example.firebase.DonationRequestDoc
import com.example.firebase.FirebaseAuthUiState
import com.example.firebase.FirebaseSessionStore
import com.example.firebase.LicenseAccessStatus
import com.example.firebase.LicenseDoc
import com.example.firebase.LicenseUiState
import com.example.firebase.LocalLicenseSnapshot
import com.example.hardware.BarcodeInputSanitizer
import com.example.hardware.HardwareDevicePreferences
import com.example.hardware.ScannerSettings
import com.example.hardware.ThermalPrinterEngine
import com.example.supabase.SupabaseLicenseRepository
import com.example.supabase.SuspendedLicenseException
import com.example.ui.components.ReceiptPrintHelper
import com.example.whatsapp.WhatsAppCommandCenter
import com.example.whatsapp.WhatsAppMessageResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

const val SUPER_ADMIN_EMAIL = "egisbahricha@gmail.com"
const val DEMO_DURATION_MILLIS = 3L * 24L * 60L * 60L * 1000L

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
    SCAN_BARCODE,
    MANAJEMEN_PRODUK,
    DONASI_PENGEMBANG,
    LAPOR_BUG,
    SUPER_ADMIN
}

fun isScreenAllowedWhenDemoLocked(screen: Screen): Boolean =
    screen == Screen.PRODUK ||
        screen == Screen.MANAJEMEN_PRODUK ||
        screen == Screen.SCAN_BARCODE ||
        screen == Screen.PENGATURAN ||
        screen == Screen.DONASI_PENGEMBANG ||
        screen == Screen.LAPOR_BUG

fun evaluateLocalLicenseState(
    snapshot: LocalLicenseSnapshot?,
    nowMillis: Long,
    currentDeviceId: String
): LicenseUiState {
    if (snapshot == null) {
        return LicenseUiState(
            accessStatus = LicenseAccessStatus.UNREGISTERED,
            message = "Daftar online dengan Email & Password untuk memulai Demo 3 hari."
        )
    }
    val effectiveNow = maxOf(nowMillis, snapshot.maxSeenClockMillis)
    if (snapshot.status.equals("SUSPENDED", ignoreCase = true)) {
        return LicenseUiState(
            accessStatus = LicenseAccessStatus.SUSPENDED,
            licenseKey = snapshot.licenseKey,
            ownerEmail = snapshot.ownerEmail,
            storeName = snapshot.storeName,
            planType = snapshot.planType,
            statusRaw = snapshot.status,
            demoStartedAtMillis = snapshot.demoStartedAtMillis,
            demoExpiresAtMillis = snapshot.demoExpiresAtMillis,
            expiresAtMillis = snapshot.expiresAtMillis,
            remainingMillis = 0L,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            message = "Akun/lisensi dinonaktifkan Admin Pusat. Hubungi $SUPER_ADMIN_EMAIL."
        )
    }
    if (snapshot.deviceId.isNotBlank() &&
        currentDeviceId.isNotBlank() &&
        snapshot.deviceId != currentDeviceId
    ) {
        return LicenseUiState(
            accessStatus = LicenseAccessStatus.DEVICE_MISMATCH,
            licenseKey = snapshot.licenseKey,
            ownerEmail = snapshot.ownerEmail,
            storeName = snapshot.storeName,
            planType = snapshot.planType,
            statusRaw = snapshot.status,
            demoStartedAtMillis = snapshot.demoStartedAtMillis,
            demoExpiresAtMillis = snapshot.demoExpiresAtMillis,
            expiresAtMillis = snapshot.expiresAtMillis,
            remainingMillis = 0L,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            message = "Lisensi terikat di perangkat lain. Minta Admin Pusat ($SUPER_ADMIN_EMAIL) untuk Reset Device."
        )
    }
    val isFullPlan = snapshot.planType.equals("FULL", ignoreCase = true) ||
        snapshot.status.equals("ACTIVE", ignoreCase = true)
    if (isFullPlan) {
        val exp = snapshot.expiresAtMillis
        if (exp > 0L && effectiveNow >= exp) {
            return LicenseUiState(
                accessStatus = LicenseAccessStatus.DEMO_EXPIRED,
                licenseKey = snapshot.licenseKey,
                ownerEmail = snapshot.ownerEmail,
                storeName = snapshot.storeName,
                planType = "FULL",
                statusRaw = "EXPIRED",
                demoStartedAtMillis = snapshot.demoStartedAtMillis,
                demoExpiresAtMillis = snapshot.demoExpiresAtMillis,
                expiresAtMillis = exp,
                remainingMillis = 0L,
                lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
                message = "Masa aktif lisensi telah berakhir. Hanya menu Produk & Pengaturan yang dapat diakses."
            )
        }
        val rem = if (exp > 0L) (exp - effectiveNow).coerceAtLeast(0L) else Long.MAX_VALUE
        return LicenseUiState(
            accessStatus = LicenseAccessStatus.FULL_ACTIVE,
            licenseKey = snapshot.licenseKey,
            ownerEmail = snapshot.ownerEmail,
            storeName = snapshot.storeName,
            planType = "FULL",
            statusRaw = "ACTIVE",
            demoStartedAtMillis = snapshot.demoStartedAtMillis,
            demoExpiresAtMillis = snapshot.demoExpiresAtMillis,
            expiresAtMillis = exp,
            remainingMillis = rem,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            message = "Lisensi Penuh Aktif"
        )
    }

    val demoEnd = when {
        snapshot.demoExpiresAtMillis > 0L -> snapshot.demoExpiresAtMillis
        snapshot.demoStartedAtMillis > 0L -> snapshot.demoStartedAtMillis + DEMO_DURATION_MILLIS
        else -> 0L
    }
    val isExpired = snapshot.status.equals("EXPIRED", ignoreCase = true) ||
        (demoEnd > 0L && effectiveNow >= demoEnd)
    return if (isExpired) {
        LicenseUiState(
            accessStatus = LicenseAccessStatus.DEMO_EXPIRED,
            licenseKey = snapshot.licenseKey,
            ownerEmail = snapshot.ownerEmail,
            storeName = snapshot.storeName,
            planType = "DEMO",
            statusRaw = "EXPIRED",
            demoStartedAtMillis = snapshot.demoStartedAtMillis,
            demoExpiresAtMillis = demoEnd,
            expiresAtMillis = snapshot.expiresAtMillis,
            remainingMillis = 0L,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            message = "Masa Demo 3 hari telah berakhir. Hubungi $SUPER_ADMIN_EMAIL untuk aktivasi penuh."
        )
    } else {
        val rem = if (demoEnd > 0L) (demoEnd - effectiveNow).coerceAtLeast(0L) else DEMO_DURATION_MILLIS
        LicenseUiState(
            accessStatus = LicenseAccessStatus.DEMO_ACTIVE,
            licenseKey = snapshot.licenseKey,
            ownerEmail = snapshot.ownerEmail,
            storeName = snapshot.storeName,
            planType = "DEMO",
            statusRaw = "DEMO",
            demoStartedAtMillis = snapshot.demoStartedAtMillis,
            demoExpiresAtMillis = demoEnd,
            expiresAtMillis = snapshot.expiresAtMillis,
            remainingMillis = rem,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            message = "Mode Demo 3 Hari Aktif"
        )
    }
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
    existingProducts: List<ProductEntity> = emptyList(),
    isDigital: Boolean = false,
    isFlexiblePrice: Boolean = false
): ManualProductValidation {
    val cleanName = name.trim()
    val nameErr = if (cleanName.isEmpty()) "Nama produk wajib diisi" else null

    val parsedPrice = sellingPriceText.trim().replace(',', '.').toDoubleOrNull()
    val priceErr = when {
        isFlexiblePrice -> {
            if (sellingPriceText.isNotBlank() && (parsedPrice == null || parsedPrice < 0.0)) {
                "Nominal default tidak valid"
            } else null
        }
        sellingPriceText.isBlank() -> if (isDigital) "Nominal layanan wajib diisi (atau aktifkan fleksibel)" else "Harga jual wajib diisi"
        parsedPrice == null || parsedPrice < 0.0 -> "Harga jual tidak valid"
        else -> null
    }

    val parsedStock = stockText.trim().replace(',', '.').toDoubleOrNull()
    val stockErr = if (isDigital) {
        null
    } else {
        when {
            stockText.isBlank() -> "Stok saat ini wajib diisi"
            parsedStock == null -> "Angka stok tidak valid"
            else -> null
        }
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
    val code = BarcodeInputSanitizer.cleanBarcodeValue(rawCode, stripWhitespace = true)
    if (code.isEmpty()) return BarcodeLookupOutcome.NotFound("")

    val matchedVariant = variants.firstOrNull { variant ->
        variant.isActive &&
            variant.barcode.isNotBlank() &&
            BarcodeInputSanitizer.matchesBarcodeCode(variant.barcode, code)
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
            BarcodeInputSanitizer.matchesBarcodeCode(product.barcode, code)
    } ?: return BarcodeLookupOutcome.NotFound(code)

    val productConversions = conversions.filter { it.productId == matchedProduct.id }
    val activeVariants = variants.filter { it.productId == matchedProduct.id && it.isActive }
    return if ((matchedProduct.hasVariants && activeVariants.isNotEmpty()) ||
        matchedProduct.productType == "DIGITAL" ||
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

fun matchesProductSearchQuery(
    product: ProductEntity,
    rawQuery: String,
    variants: List<ProductVariantEntity> = emptyList()
): Boolean {
    val q = BarcodeInputSanitizer.cleanBarcodeValue(rawQuery, stripWhitespace = true)
    if (q.isEmpty()) return true
    if (product.name.contains(q, ignoreCase = true) ||
        product.barcode.contains(q, ignoreCase = true) ||
        product.category.contains(q, ignoreCase = true) ||
        BarcodeInputSanitizer.matchesBarcodeCode(product.barcode, q)
    ) {
        return true
    }
    return variants.any { variant ->
        variant.productId == product.id &&
            variant.barcode.isNotBlank() &&
            (variant.barcode.contains(q, ignoreCase = true) ||
                BarcodeInputSanitizer.matchesBarcodeCode(variant.barcode, q))
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

data class ExcelImportUiState(
    val isInspecting: Boolean = false,
    val isImporting: Boolean = false,
    val inspectResult: ExcelProductInspectResult? = null,
    val lastSummary: ExcelProductImportSummary? = null,
    val errorMessage: String? = null
)

data class KasLiteBackupUiState(
    val isWorking: Boolean = false,
    val isInspecting: Boolean = false,
    val isRestoring: Boolean = false,
    val inspectResult: KasLiteBackupInspectResult? = null,
    val lastRestoreSummary: KasLiteRestoreSummary? = null,
    val errorMessage: String? = null
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    val database = GeraiUnaDatabase.getDatabase(application)
    val repository = PosRepository(database)
    val aiEngine = GeraiUnaAIEngine(repository)
    val whatsAppCommandCenter = WhatsAppCommandCenter(repository, aiEngine)

    val firebaseSessionStore = FirebaseSessionStore(application)
    val supabaseRepository = SupabaseLicenseRepository(application, firebaseSessionStore)
    val hardwareDevicePrefs = HardwareDevicePreferences(application)

    private val _scannerSettings = MutableStateFlow(hardwareDevicePrefs.loadScannerSettings())
    val scannerSettings: StateFlow<ScannerSettings> = _scannerSettings.asStateFlow()

    private val _preferredPrinterId = MutableStateFlow(hardwareDevicePrefs.getPreferredPrinterId())
    val preferredPrinterId: StateFlow<Long> = _preferredPrinterId.asStateFlow()

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

    val isSuperAdmin: StateFlow<Boolean> = _authUiState.map {
        it.isSuperAdmin && it.userEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _licenseUiState = MutableStateFlow(LicenseUiState())
    val licenseUiState: StateFlow<LicenseUiState> = _licenseUiState.asStateFlow()

    private val _allLicenses = MutableStateFlow<List<LicenseDoc>>(emptyList())
    val allLicenses: StateFlow<List<LicenseDoc>> = _allLicenses.asStateFlow()

    private val _superAdminError = MutableStateFlow<String?>(null)
    val superAdminError: StateFlow<String?> = _superAdminError.asStateFlow()

    private val _isSuperAdminLoading = MutableStateFlow(false)
    val isSuperAdminLoading: StateFlow<Boolean> = _isSuperAdminLoading.asStateFlow()

    private var licensesJob: Job? = null
    private var silentLicenseSyncJob: Job? = null

    @Volatile
    private var lastSilentLicenseSyncMillis: Long = 0L

    private fun refreshLicenseStateFromStore() {
        val deviceId = firebaseSessionStore.getOrCreateDeviceId()
        val now = firebaseSessionStore.updateMaxSeenClockMillis()
        val snap = firebaseSessionStore.getLicenseSnapshot()
        _licenseUiState.value = evaluateLocalLicenseState(snap, now, deviceId)
    }

    fun syncCustomerLicenseSilently(force: Boolean = false) {
        if (_authUiState.value.isSuperAdmin) return
        if (!supabaseRepository.isConfigured() || !firebaseSessionStore.hasSupabaseSession()) return
        val sessionEmail = firebaseSessionStore.getSupabaseEmail()
        if (sessionEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) return

        val now = System.currentTimeMillis()
        if (!force && (now - lastSilentLicenseSyncMillis) < 15_000L) return
        if (silentLicenseSyncJob?.isActive == true && !force) return

        silentLicenseSyncJob?.cancel()
        silentLicenseSyncJob = viewModelScope.launch {
            try {
                lastSilentLicenseSyncMillis = System.currentTimeMillis()
                val uid = firebaseSessionStore.getSupabaseUid()
                val activeEmail = firebaseSessionStore.getSupabaseEmail()
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()
                val snap = firebaseSessionStore.getLicenseSnapshot()
                val previousStatus = _licenseUiState.value.accessStatus

                val fetched = supabaseRepository.fetchBestMatchingCustomerLicense(
                    uid = uid,
                    email = activeEmail,
                    currentLicenseKey = snap?.licenseKey.orEmpty(),
                    currentDeviceId = deviceId
                ) ?: return@launch

                firebaseSessionStore.saveFromLicenseDoc(fetched)
                refreshLicenseStateFromStore()

                val newStatus = _licenseUiState.value.accessStatus
                if (newStatus != previousStatus) {
                    when (newStatus) {
                        LicenseAccessStatus.SUSPENDED -> {
                            _userMessage.value = "Lisensi dinonaktifkan Admin Pusat."
                        }
                        LicenseAccessStatus.DEVICE_MISMATCH -> {
                            _userMessage.value = "Lisensi terikat di perangkat lain."
                        }
                        LicenseAccessStatus.DEMO_EXPIRED -> {
                            _userMessage.value = "Masa aktif lisensi berakhir. Hanya menu Produk & Pengaturan aktif."
                        }
                        LicenseAccessStatus.FULL_ACTIVE -> {
                            _userMessage.value = "Lisensi FULL aktif! Semua menu terbuka."
                        }
                        else -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Silent check: keep existing local snapshot if offline/transient error
            }
        }
    }

    fun loadSuperAdminLicenses() {
        val sessionEmail = firebaseSessionStore.getSupabaseEmail()
        if (!supabaseRepository.isConfigured()) {
            _superAdminError.value = "Supabase belum dikonfigurasi. Isi SUPABASE_URL dan SUPABASE_ANON_KEY di menu Secrets."
            return
        }
        if (!_authUiState.value.isSuperAdmin ||
            !firebaseSessionStore.hasSupabaseSession() ||
            !sessionEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        ) {
            _superAdminError.value = "Harap masuk menggunakan akun Email Super Admin ($SUPER_ADMIN_EMAIL) via Supabase."
            return
        }
        licensesJob?.cancel()
        licensesJob = viewModelScope.launch(Dispatchers.IO) {
            _isSuperAdminLoading.value = true
            _superAdminError.value = null
            try {
                val list = supabaseRepository.getAllLicenses()
                _allLicenses.value = list
            } catch (e: Exception) {
                val msg = "Gagal memuat database pelanggan Supabase: ${e.localizedMessage ?: e.message}"
                _superAdminError.value = msg
                _userMessage.value = msg
            } finally {
                _isSuperAdminLoading.value = false
            }
        }
    }

    fun resetCustomerDeviceBinding(licenseKey: String, onResult: (Boolean, String) -> Unit) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.resetDeviceBinding(licenseKey)
                val list = supabaseRepository.getAllLicenses()
                _allLicenses.value = list
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Perangkat di-reset untuk lisensi $licenseKey"
                    onResult(true, "Perangkat berhasil di-reset untuk lisensi $licenseKey")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal reset perangkat: ${e.localizedMessage}")
                }
            }
        }
    }

    fun generateNewLicense(
        storeName: String,
        ownerName: String,
        ownerEmail: String,
        phone: String = "",
        planType: String = "FULL",
        onResult: (String?) -> Unit
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val created = supabaseRepository.createSuperAdminLicense(
                    storeName = storeName,
                    ownerName = ownerName,
                    ownerEmail = ownerEmail,
                    phone = phone,
                    planType = planType
                )
                _allLicenses.value = supabaseRepository.getAllLicenses()
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Lisensi ${created.licenseKey} berhasil dibuat!"
                    onResult(created.licenseKey)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Gagal membuat lisensi: ${e.localizedMessage}"
                    onResult(null)
                }
            }
        }
    }

    fun superAdminActivateFull(
        licenseKey: String,
        durationDays: Int = 3650,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.superAdminActivateFull(licenseKey, durationDays)
                _allLicenses.value = supabaseRepository.getAllLicenses()
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Lisensi $licenseKey diaktifkan FULL!"
                    onResult(true, "Lisensi $licenseKey berhasil diaktifkan FULL.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal aktivasi FULL: ${e.localizedMessage}")
                }
            }
        }
    }

    fun superAdminExtendDemo(
        licenseKey: String,
        days: Int = 3,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.superAdminExtendDemo(licenseKey, days)
                _allLicenses.value = supabaseRepository.getAllLicenses()
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Demo diperpanjang +$days hari untuk $licenseKey"
                    onResult(true, "Demo berhasil diperpanjang +$days hari.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal perpanjang demo: ${e.localizedMessage}")
                }
            }
        }
    }

    fun superAdminSetSuspended(
        licenseKey: String,
        suspended: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.superAdminSetSuspended(licenseKey, suspended)
                _allLicenses.value = supabaseRepository.getAllLicenses()
                val label = if (suspended) "dinonaktifkan (SUSPENDED)" else "diaktifkan kembali"
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Lisensi $licenseKey $label"
                    onResult(true, "Lisensi $licenseKey $label")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal ubah status lisensi: ${e.localizedMessage}")
                }
            }
        }
    }

    fun superAdminSendResetPassword(
        email: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val clean = email.trim()
        if (clean.isBlank() || !clean.contains("@")) {
            onResult(false, "Email pelanggan tidak valid.")
            return
        }
        viewModelScope.launch {
            val res = supabaseRepository.sendPasswordReset(clean)
            res.onSuccess {
                val msg = "Email reset password dikirim ke $clean via Supabase Auth."
                _userMessage.value = msg
                onResult(true, msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Gagal mengirim reset password."
                _userMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun signInSuperAdminWithSupabase(
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanEmail = email.trim()
        if (!cleanEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
            val msg = "Email \"$cleanEmail\" bukan Admin Pusat resmi ($SUPER_ADMIN_EMAIL)."
            _authUiState.update { it.copy(isAuthLoading = false, isSuperAdmin = false, authError = msg) }
            onResult(false, msg)
            return
        }
        if (password.isBlank()) {
            val msg = "Masukkan password akun Admin Pusat."
            onResult(false, msg)
            return
        }

        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                val session = supabaseRepository.signInWithEmail(cleanEmail, password).getOrThrow()
                if (!session.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                    supabaseRepository.signOut()
                    val msg = "Email \"${session.email}\" bukan Admin Pusat resmi ($SUPER_ADMIN_EMAIL)."
                    _authUiState.update { it.copy(isAuthLoading = false, isSuperAdmin = false, authError = msg) }
                    onResult(false, msg)
                    return@launch
                }

                val displayName = session.fullName.ifBlank { "Admin Pusat (Egis)" }
                _activeUserName.value = displayName
                _activeRole.value = "Owner"
                _currentScreen.value = Screen.SUPER_ADMIN
                _authUiState.update {
                    it.copy(
                        hasEnteredApp = true,
                        isCloudSignedIn = true,
                        isOfflineMode = false,
                        isSuperAdmin = true,
                        userId = session.uid,
                        userEmail = session.email,
                        fullName = displayName,
                        storeName = "KASLITE Admin Pusat",
                        lastEmail = session.email,
                        isAuthLoading = false,
                        authError = null,
                        authInfoMessage = "Masuk sebagai Admin Pusat (${session.email})."
                    )
                }
                _userMessage.value = "Berhasil masuk sebagai Super Admin."
                loadSuperAdminLicenses()
                onResult(true, "Berhasil masuk sebagai Admin Pusat.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Gagal masuk Admin Pusat via Supabase."
                _authUiState.update { it.copy(isAuthLoading = false, authError = msg) }
                onResult(false, msg)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        val isSuper = _authUiState.value.isSuperAdmin
        if (!isSuper && _licenseUiState.value.isLockedToProductOnly && !isScreenAllowedWhenDemoLocked(screen)) {
            _userMessage.value = "Masa Demo 3 hari telah berakhir / akun terkunci. Hanya menu Produk & Pengaturan yang dapat diakses. Hubungi Admin Pusat ($SUPER_ADMIN_EMAIL)."
            return
        }
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
        if (screen == Screen.SUPER_ADMIN) {
            return _authUiState.value.isSuperAdmin && _authUiState.value.userEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        }
        if (!_authUiState.value.isSuperAdmin && _licenseUiState.value.isLockedToProductOnly) {
            return isScreenAllowedWhenDemoLocked(screen)
        }
        if (_activeRole.value.equals("Owner", ignoreCase = true) || _activeRole.value.equals("Admin", ignoreCase = true)) {
            return true
        }
        if (screen == Screen.DASHBOARD ||
            screen == Screen.SEMUA_MENU ||
            screen == Screen.PENGATURAN ||
            screen == Screen.DONASI_PENGEMBANG ||
            screen == Screen.LAPOR_BUG
        ) return true
        val perms = menuPermissions.value.filter { it.userId == _activeUserId.value }
        if (perms.isEmpty()) return true
        val matched = perms.find { it.menuKey == screen.name }
        return matched?.allowed ?: true
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Local-only business data in Room; customer never auto-bypasses login
                firebaseSessionStore.setOfflineBypass(false)
                firebaseSessionStore.setAutoSyncEnabled(false)

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

                // Restore any previously verified customer Supabase session for online registration step
                val existingSupaUid = firebaseSessionStore.getSupabaseUid()
                val existingSupaEmail = firebaseSessionStore.getSupabaseEmail()
                val existingSupaName = firebaseSessionStore.getSupabaseFullName()
                if (existingSupaUid.isNotBlank() &&
                    existingSupaEmail.isNotBlank() &&
                    !existingSupaEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                ) {
                    _authUiState.update {
                        it.copy(
                            verifiedGoogleUid = existingSupaUid,
                            verifiedGoogleEmail = existingSupaEmail,
                            verifiedGoogleName = existingSupaName,
                            lastEmail = existingSupaEmail
                        )
                    }
                }
                refreshLicenseStateFromStore()
                syncCustomerLicenseSilently(force = true)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        licensesJob?.cancel()
        silentLicenseSyncJob?.cancel()
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

    fun clearVerifiedRegistrationEmail() {
        viewModelScope.launch {
            supabaseRepository.signOut()
        }
        _authUiState.update {
            it.copy(
                verifiedGoogleUid = "",
                verifiedGoogleEmail = "",
                verifiedGoogleName = "",
                authInfoMessage = null
            )
        }
    }

    fun authenticateCustomerSupabase(
        email: String,
        password: String,
        isNewAccount: Boolean,
        fullNameHint: String = ""
    ) {
        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                if (!supabaseRepository.isConfigured()) {
                    throw IllegalStateException("Supabase belum dikonfigurasi pada build ini. Isi SUPABASE_URL dan SUPABASE_ANON_KEY di Secrets AI Studio.")
                }
                val session = if (isNewAccount) {
                    supabaseRepository.signUpWithEmail(email, password, fullNameHint).getOrThrow()
                } else {
                    supabaseRepository.signInWithEmail(email, password).getOrThrow()
                }

                if (session.requiresEmailConfirmation || session.accessToken.isBlank()) {
                    _authUiState.update {
                        it.copy(
                            isAuthLoading = false,
                            lastEmail = session.email.ifBlank { email.trim() },
                            authError = null,
                            authInfoMessage = session.message
                        )
                    }
                    return@launch
                }

                val currentDeviceId = firebaseSessionStore.getOrCreateDeviceId()
                val existingLicense = runCatching {
                    supabaseRepository.fetchBestMatchingCustomerLicense(
                        uid = session.uid,
                        email = session.email,
                        currentLicenseKey = "",
                        currentDeviceId = currentDeviceId
                    )
                }.getOrNull()

                val infoMsg = when {
                    existingLicense == null ->
                        "Email ${session.email} terverifikasi! Lengkapi data toko & PIN untuk aktivasi Trial Demo 3 hari."
                    existingLicense.deviceId.isNotBlank() && existingLicense.deviceId != currentDeviceId ->
                        "Email ${session.email} sudah terdaftar di perangkat lain. Hubungi Admin Pusat ($SUPER_ADMIN_EMAIL) untuk Reset Device."
                    else ->
                        "Email ${session.email} terverifikasi (Status akun: ${existingLicense.status})."
                }

                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        verifiedGoogleUid = session.uid,
                        verifiedGoogleEmail = session.email,
                        verifiedGoogleName = session.fullName,
                        lastEmail = session.email.ifBlank { it.lastEmail },
                        authError = null,
                        authInfoMessage = infoMsg
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = e.localizedMessage ?: "Gagal memverifikasi akun email di Supabase."
                    )
                }
            }
        }
    }

    fun sendSupabasePasswordReset(
        email: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val clean = email.trim()
        if (clean.isBlank() || !clean.contains("@")) {
            val msg = "Masukkan alamat email yang valid terlebih dahulu."
            _authUiState.update { it.copy(authError = msg) }
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            val res = supabaseRepository.sendPasswordReset(clean)
            res.onSuccess {
                val msg = "Link reset password dikirim ke $clean. Cek kotak masuk atau folder spam email Anda."
                _authUiState.update { it.copy(isAuthLoading = false, authInfoMessage = msg, authError = null) }
                _userMessage.value = msg
                onResult(true, msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Gagal mengirim link reset password."
                _authUiState.update { it.copy(isAuthLoading = false, authError = msg) }
                _userMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun completeOnlineCustomerRegistration(
        storeName: String,
        fullName: String,
        phone: String,
        username: String,
        pin: String,
        confirmPin: String
    ) {
        val state = _authUiState.value
        if (state.verifiedGoogleUid.isBlank() || state.verifiedGoogleEmail.isBlank()) {
            _authUiState.update {
                it.copy(authError = "Pendaftaran awal wajib verifikasi akun Email secara online terlebih dahulu.")
            }
            return
        }
        val validation = validateAdminSetupInput(storeName, fullName, username, pin, confirmPin)
        if (!validation.isValid) {
            val firstError = listOfNotNull(
                validation.storeNameError,
                validation.fullNameError,
                validation.usernameError,
                validation.pinError,
                validation.confirmPinError
            ).firstOrNull() ?: "Data pendaftaran belum lengkap."
            _authUiState.update { it.copy(authError = firstError) }
            return
        }
        if (!supabaseRepository.isConfigured()) {
            _authUiState.update {
                it.copy(authError = "Supabase belum dikonfigurasi. Isi SUPABASE_URL dan SUPABASE_ANON_KEY pada menu Secrets.")
            }
            return
        }

        viewModelScope.launch {
            _authUiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            try {
                val cleanStore = storeName.trim()
                val cleanFull = fullName.trim()
                val cleanPhone = phone.trim()
                val cleanUsername = username.trim().lowercase()
                val pinHash = GeraiUnaDatabase.hashPin(pin.trim())
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()

                val existingUsers = withContext(Dispatchers.IO) {
                    database.extendedPosDao().getAllUsersList()
                }
                if (existingUsers.isNotEmpty()) {
                    _authUiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = "Admin Utama sudah terdaftar di perangkat ini. Silakan masuk dengan username & PIN."
                        )
                    }
                    return@launch
                }

                // Online mandatory license registration into Super Admin Supabase DB (public.licenses)
                val licenseDoc = supabaseRepository.registerOrFetchCustomerLicense(
                    ownerUid = state.verifiedGoogleUid,
                    ownerEmail = state.verifiedGoogleEmail,
                    ownerName = cleanFull,
                    storeName = cleanStore,
                    phone = cleanPhone,
                    deviceId = deviceId
                )
                firebaseSessionStore.saveFromLicenseDoc(licenseDoc)
                refreshLicenseStateFromStore()

                // Local-only Room storage for all store/customer data
                val currentProfile = withContext(Dispatchers.IO) {
                    database.extendedPosDao().getStoreProfileSync() ?: StoreProfileEntity()
                }
                repository.saveStoreProfile(
                    currentProfile.copy(
                        name = cleanStore,
                        phone = cleanPhone,
                        updatedAt = System.currentTimeMillis()
                    )
                )
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
                _currentScreen.value = if (_licenseUiState.value.isLockedToProductOnly) Screen.PRODUK else Screen.DASHBOARD
                repository.setSetting("active_role", "Owner")
                repository.setSetting("active_user_name", cleanFull)

                _authUiState.update {
                    it.copy(
                        hasEnteredApp = true,
                        isOfflineMode = true,
                        isCloudSignedIn = false,
                        isSuperAdmin = false,
                        userId = state.verifiedGoogleUid,
                        userEmail = state.verifiedGoogleEmail,
                        fullName = cleanFull,
                        storeName = cleanStore,
                        isAuthLoading = false,
                        authError = null,
                        authInfoMessage = null
                    )
                }
                _userMessage.value = if (_licenseUiState.value.isLockedToProductOnly) {
                    "Akun lokal dibuat. Status lisensi: ${_licenseUiState.value.accessStatus.name}."
                } else {
                    "Pendaftaran sukses! Mode Demo 3 Hari aktif untuk $cleanStore."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = "Pendaftaran online gagal (pastikan internet aktif): ${e.localizedMessage ?: e.message}"
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
                        refreshLicenseStateFromStore()
                        val user = result.user
                        val roleDisplay = if (user.role.equals("owner", ignoreCase = true)) "Owner" else "Kasir"
                        firebaseSessionStore.setOfflineBypass(false)
                        _activeUserId.value = user.id
                        _activeRole.value = roleDisplay
                        _activeUserName.value = user.name
                        _currentScreen.value = if (_licenseUiState.value.isLockedToProductOnly) Screen.PRODUK else Screen.DASHBOARD
                        repository.setSetting("active_role", roleDisplay)
                        repository.setSetting("active_user_name", user.name)

                        val snap = firebaseSessionStore.getLicenseSnapshot()
                        _authUiState.update {
                            it.copy(
                                hasEnteredApp = true,
                                isOfflineMode = true,
                                isCloudSignedIn = false,
                                isSuperAdmin = false,
                                userEmail = snap?.ownerEmail ?: it.userEmail,
                                fullName = user.name,
                                isAuthLoading = false,
                                authError = null,
                                authInfoMessage = null
                            )
                        }
                        _userMessage.value = if (_licenseUiState.value.isLockedToProductOnly) {
                            "Selamat datang, ${user.name}. Masa Demo 3 hari telah berakhir (akses terbatas Produk & Pengaturan)."
                        } else {
                            "Selamat datang, ${user.name} ($roleDisplay)"
                        }
                        syncCustomerLicenseSilently(force = true)
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

    fun hasActiveCustomerSupabaseSession(): Boolean = firebaseSessionStore.hasSupabaseSession()

    fun refreshCustomerLicenseOnline(
        email: String? = null,
        password: String? = null,
        onRequireAuth: () -> Unit = {},
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            val msg = "Supabase belum dikonfigurasi untuk memeriksa lisensi."
            _userMessage.value = msg
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            _licenseUiState.update { it.copy(isRefreshing = true) }
            try {
                if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                    supabaseRepository.signInWithEmail(email, password).getOrThrow()
                } else if (!firebaseSessionStore.hasSupabaseSession()) {
                    _licenseUiState.update { it.copy(isRefreshing = false) }
                    onRequireAuth()
                    return@launch
                }

                val uid = firebaseSessionStore.getSupabaseUid()
                val activeEmail = firebaseSessionStore.getSupabaseEmail()
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()
                val snap = firebaseSessionStore.getLicenseSnapshot()
                val fetched = supabaseRepository.fetchBestMatchingCustomerLicense(
                    uid = uid,
                    email = activeEmail,
                    currentLicenseKey = snap?.licenseKey.orEmpty(),
                    currentDeviceId = deviceId
                )

                if (fetched == null) {
                    _licenseUiState.update { it.copy(isRefreshing = false) }
                    val msg = "Belum ada data lisensi untuk email $activeEmail."
                    _userMessage.value = msg
                    onResult(false, msg)
                    return@launch
                }

                firebaseSessionStore.saveFromLicenseDoc(fetched)
                refreshLicenseStateFromStore()
                val statusLabel = when (_licenseUiState.value.accessStatus) {
                    LicenseAccessStatus.FULL_ACTIVE -> "Lisensi FULL Aktif! Semua menu terbuka."
                    LicenseAccessStatus.DEMO_ACTIVE -> "Mode Demo aktif."
                    LicenseAccessStatus.DEMO_EXPIRED -> "Masa Demo berakhir. Hubungi $SUPER_ADMIN_EMAIL."
                    LicenseAccessStatus.SUSPENDED -> "Lisensi dinonaktifkan Admin Pusat."
                    LicenseAccessStatus.DEVICE_MISMATCH -> "Lisensi terikat perangkat lain. Minta Reset Device ke Admin Pusat."
                    LicenseAccessStatus.UNREGISTERED -> "Lisensi belum terdaftar."
                }
                _userMessage.value = statusLabel
                _authUiState.update { it.copy(authInfoMessage = statusLabel, authError = null) }
                onResult(true, statusLabel)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _licenseUiState.update { it.copy(isRefreshing = false) }
                val msg = "Gagal cek lisensi online: ${e.localizedMessage ?: e.message}"
                _userMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun activateCustomerLicenseKey(
        licenseKey: String,
        email: String? = null,
        password: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            val msg = "Supabase belum dikonfigurasi. Aktivasi kunci membutuhkan internet."
            _userMessage.value = msg
            onResult(false, msg)
            return
        }
        val cleanKey = licenseKey.trim()
        if (cleanKey.isBlank()) {
            onResult(false, "Masukkan kunci lisensi terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            _licenseUiState.update { it.copy(isRefreshing = true) }
            try {
                if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                    supabaseRepository.signInWithEmail(email, password).getOrThrow()
                } else if (!firebaseSessionStore.hasSupabaseSession()) {
                    _licenseUiState.update { it.copy(isRefreshing = false) }
                    val msg = "Masukkan Email & Password akun KASLITE Anda dahulu untuk mengklaim lisensi."
                    _userMessage.value = msg
                    onResult(false, msg)
                    return@launch
                }

                val uid = firebaseSessionStore.getSupabaseUid()
                val activeEmail = firebaseSessionStore.getSupabaseEmail()
                val profile = withContext(Dispatchers.IO) { database.extendedPosDao().getStoreProfileSync() }
                val updated = supabaseRepository.activateLicenseKeyForCustomer(
                    licenseKey = cleanKey,
                    ownerUid = uid,
                    ownerEmail = activeEmail,
                    ownerName = _activeUserName.value.ifBlank { firebaseSessionStore.getSupabaseFullName() },
                    storeName = profile?.name.orEmpty(),
                    phone = profile?.phone.orEmpty(),
                    deviceId = firebaseSessionStore.getOrCreateDeviceId()
                )
                firebaseSessionStore.saveFromLicenseDoc(updated)
                refreshLicenseStateFromStore()
                val msg = "Lisensi berhasil diaktifkan (${_licenseUiState.value.accessStatus.name})!"
                _userMessage.value = msg
                onResult(true, msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SuspendedLicenseException) {
                val uid = firebaseSessionStore.getSupabaseUid()
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()
                val currentKey = firebaseSessionStore.getLicenseSnapshot()?.licenseKey.orEmpty()
                val doc = e.licenseDoc
                if ((doc.ownerUid.isNotBlank() && doc.ownerUid == uid) ||
                    (doc.deviceId.isNotBlank() && doc.deviceId == deviceId) ||
                    (currentKey.isNotBlank() && doc.licenseKey.equals(currentKey, ignoreCase = true))
                ) {
                    firebaseSessionStore.saveFromLicenseDoc(doc)
                    refreshLicenseStateFromStore()
                } else {
                    _licenseUiState.update { it.copy(isRefreshing = false) }
                }
                val msg = e.localizedMessage ?: "Lisensi sedang dinonaktifkan (SUSPENDED)."
                _userMessage.value = msg
                onResult(false, msg)
            } catch (e: Exception) {
                _licenseUiState.update { it.copy(isRefreshing = false) }
                val msg = e.localizedMessage ?: "Gagal mengaktivasi kunci lisensi."
                _userMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun logoutToLoginScreen() {
        viewModelScope.launch {
            val wasSuperAdmin = _authUiState.value.isSuperAdmin
            withContext(NonCancellable) {
                licensesJob?.cancel()
                if (wasSuperAdmin) {
                    supabaseRepository.signOut()
                }
                firebaseSessionStore.setOfflineBypass(false)
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
                    isSuperAdmin = false,
                    userId = if (wasSuperAdmin) "" else it.userId,
                    userEmail = if (wasSuperAdmin) "" else it.userEmail,
                    authError = null,
                    authInfoMessage = "Anda telah keluar. Silakan login kembali."
                )
            }
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
    val activePrinter: StateFlow<PrinterEntity?> = combine(printers, _preferredPrinterId) { list, prefId ->
        list.firstOrNull { it.id == prefId && it.isActive }
            ?: list.firstOrNull { it.isActive }
            ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
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
        val isDigitalProduct = product.productType == "DIGITAL"
        val allowMerge = customPrice == null && !isDigitalProduct && !product.isFlexiblePrice
        val index = if (allowMerge) {
            current.indexOfFirst {
                it.productId == product.id &&
                        it.unitName.equals(unitName, ignoreCase = true) &&
                        it.variantId == targetVariantId &&
                        it.bundleId == null
            }
        } else -1

        val baseSellingPrice = variant?.sellingPrice ?: price
        val stockLimit = variant?.stock ?: product.stock

        if (index >= 0) {
            val existing = current[index]
            val updatedQty = existing.quantity + 1.0
            val (resolvedPrice, source) = resolvePriceAndSource(product.id, unitName, multiplier, updatedQty, baseSellingPrice)
            current[index] = existing.copy(
                quantity = updatedQty,
                unitPrice = resolvedPrice,
                priceSource = source
            )
        } else {
            val (resolvedPrice, source) = when {
                customPrice != null -> customPrice to if (product.isFlexiblePrice || isDigitalProduct) "fleksibel" else "manual"
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
                    serviceNominal = if (isDigitalProduct) resolvedPrice else null,
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

    fun updateDigitalCartItem(
        lineId: String,
        nominal: Double,
        storeAdminFee: Double,
        providerAdminFee: Double,
        agentCommission: Double,
        serviceCashAccountId: Long?,
        serviceDirection: String
    ) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.lineId == lineId }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                unitPrice = nominal,
                quantity = 1.0,
                priceSource = "fleksibel",
                serviceNominal = if (item.serviceNominal != null) nominal else null,
                storeAdminFee = storeAdminFee,
                providerAdminFee = providerAdminFee,
                agentCommission = agentCommission,
                serviceCashAccountId = serviceCashAccountId,
                serviceDirection = serviceDirection
            )
            _cart.value = current
        }
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
        bundleId: Long? = null,
        lineId: String? = null
    ) {
        if (newQty <= 0) {
            removeFromCart(productId, unitName, variantId, bundleId, lineId)
            return
        }
        val baseProduct = products.value.find { it.id == productId }
        val current = _cart.value.toMutableList()
        val index = if (lineId != null) {
            current.indexOfFirst { it.lineId == lineId }
        } else {
            current.indexOfFirst {
                it.productId == productId &&
                        it.unitName == unitName &&
                        (variantId == null || it.variantId == variantId) &&
                        (bundleId == null || it.bundleId == bundleId)
            }
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

    fun updateCartItemManualTotal(
        productId: Long,
        unitName: String,
        totalAmount: Double,
        lineId: String? = null
    ) {
        val current = _cart.value.toMutableList()
        val index = if (lineId != null) {
            current.indexOfFirst { it.lineId == lineId }
        } else {
            current.indexOfFirst { it.productId == productId && it.unitName == unitName }
        }
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
        bundleId: Long? = null,
        lineId: String? = null
    ) {
        _cart.value = if (lineId != null) {
            _cart.value.filterNot { it.lineId == lineId }
        } else {
            _cart.value.filterNot {
                it.productId == productId &&
                        it.unitName == unitName &&
                        (variantId == null || it.variantId == variantId) &&
                        (bundleId == null || it.bundleId == bundleId)
            }
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
        val cleaned = BarcodeInputSanitizer.cleanBarcodeValue(rawCode, stripWhitespace = true)
        val outcome = resolveBarcodeToCartMatch(
            rawCode = cleaned,
            products = products.value,
            variants = productVariants.value,
            conversions = unitConversions.value
        )
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
                                serviceDirection = obj.optString("serviceDirection", "SETOR"),
                                lineId = obj.optString("lineId").ifBlank { java.util.UUID.randomUUID().toString() }
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
        }
    }

    fun transferCash(fromId: Long, toId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            try {
                repository.transferCash(fromId, toId, amount, notes, _activeUserName.value)
                _userMessage.value = "Transfer kas sebesar Rp ${formatRupiah(amount)} berhasil!"
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
        allowDecimalQty: Boolean,
        isDigital: Boolean = existingProduct?.productType == "DIGITAL",
        isFlexiblePrice: Boolean = existingProduct?.isFlexiblePrice ?: isDigital,
        adminTierGroupId: Long? = existingProduct?.adminTierGroupId,
        serviceCashAccountId: Long? = existingProduct?.serviceCashAccountId,
        serviceDirection: String = existingProduct?.serviceDirection ?: "SETOR"
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || sellingPrice < 0.0) return

        val cleanCategory = category.trim().ifBlank { if (isDigital) "Layanan Digital" else "Umum" }
        val cleanUnit = baseUnit.trim().ifBlank { if (isDigital) "transaksi" else "pcs" }
        val cleanBarcode = barcode.trim()
        val effectiveStock = if (isDigital) 999.0 else stock
        val effectiveMinStock = if (isDigital) 0.0 else minStock.coerceAtLeast(0.0)
        val effectiveType = if (isDigital) "DIGITAL" else "FISIK"

        viewModelScope.launch {
            if (existingProduct == null) {
                val created = ProductEntity(
                    name = cleanName,
                    barcode = cleanBarcode,
                    category = cleanCategory,
                    baseUnit = cleanUnit,
                    purchasePrice = if (isDigital && isFlexiblePrice) 0.0 else purchasePrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    stock = effectiveStock,
                    minStock = effectiveMinStock,
                    productType = effectiveType,
                    isUnlimitedStock = isDigital,
                    hasNoStock = isDigital,
                    isFlexiblePrice = isFlexiblePrice,
                    allowDecimalQty = if (isDigital) false else allowDecimalQty,
                    adminTierGroupId = if (isDigital) adminTierGroupId else null,
                    serviceCashAccountId = if (isDigital) serviceCashAccountId else null,
                    serviceDirection = if (isDigital) serviceDirection else "SETOR",
                    isActive = true
                )
                repository.insertProduct(created)
                _userMessage.value = if (isDigital) {
                    "Layanan digital \"$cleanName\" disimpan ke database"
                } else {
                    "Produk \"$cleanName\" disimpan ke database"
                }
            } else {
                val stockChanged = !isDigital && existingProduct.stock != effectiveStock
                val preservePhysicalNoStock = !isDigital && existingProduct.productType != "DIGITAL" && existingProduct.hasNoStock
                val updated = existingProduct.copy(
                    name = cleanName,
                    barcode = cleanBarcode,
                    category = cleanCategory,
                    baseUnit = cleanUnit,
                    purchasePrice = if (isDigital && isFlexiblePrice) 0.0 else purchasePrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    stock = effectiveStock,
                    minStock = effectiveMinStock,
                    productType = effectiveType,
                    isUnlimitedStock = isDigital || preservePhysicalNoStock,
                    hasNoStock = isDigital || preservePhysicalNoStock,
                    isFlexiblePrice = isFlexiblePrice,
                    allowDecimalQty = if (isDigital) false else allowDecimalQty,
                    adminTierGroupId = if (isDigital) adminTierGroupId else null,
                    serviceCashAccountId = if (isDigital) serviceCashAccountId else null,
                    serviceDirection = if (isDigital) serviceDirection else "SETOR"
                )
                repository.updateProduct(updated)
                if (stockChanged) {
                    repository.adjustStock(
                        productId = updated.id,
                        newStock = effectiveStock,
                        reason = "Pembaruan stok manual",
                        user = _activeUserName.value
                    )
                }
                _userMessage.value = if (isDigital) {
                    "Layanan digital \"$cleanName\" berhasil diperbarui"
                } else {
                    "Produk \"$cleanName\" berhasil diperbarui"
                }
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

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun updateScannerSettings(newSettings: ScannerSettings) {
        hardwareDevicePrefs.saveScannerSettings(newSettings)
        _scannerSettings.value = newSettings
        _userMessage.value = "Pengaturan scanner barcode disimpan"
    }

    fun selectPreferredPrinter(printerId: Long) {
        hardwareDevicePrefs.setPreferredPrinterId(printerId)
        _preferredPrinterId.value = printerId
        val chosen = printers.value.firstOrNull { it.id == printerId }
        if (chosen != null) {
            _userMessage.value = "Printer utama: ${chosen.name}"
        }
    }

    fun savePrinter(printer: PrinterEntity, setAsPrimary: Boolean = true) {
        viewModelScope.launch {
            val savedId = repository.savePrinter(printer)
            if (setAsPrimary || _preferredPrinterId.value <= 0L) {
                hardwareDevicePrefs.setPreferredPrinterId(savedId)
                _preferredPrinterId.value = savedId
            }
            _userMessage.value = "Printer \"${printer.name}\" disimpan"
        }
    }

    fun deletePrinter(printer: PrinterEntity) {
        viewModelScope.launch {
            repository.deletePrinter(printer)
            if (_preferredPrinterId.value == printer.id) {
                hardwareDevicePrefs.setPreferredPrinterId(0L)
                _preferredPrinterId.value = 0L
            }
            _userMessage.value = "Printer dihapus"
        }
    }

    fun printSaleReceipt(
        context: Context,
        sale: SaleEntity,
        items: List<CartItem>,
        forceSystemPrint: Boolean = false
    ) {
        val printer = activePrinter.value
        ReceiptPrintHelper.printReceipt(
            context = context,
            sale = sale,
            items = items,
            storeProfile = storeProfile.value,
            printer = printer,
            forceSystemPrint = forceSystemPrint,
            onResult = { _, msg ->
                _userMessage.value = msg
            }
        )
    }

    fun printTestReceipt(
        context: Context,
        printer: PrinterEntity,
        onResult: (Boolean, String) -> Unit = { _, msg -> _userMessage.value = msg }
    ) {
        if (ThermalPrinterEngine.isSystemPrintAddress(printer.bluetoothAddress)) {
            val dummySale = SaleEntity(
                invoiceNumber = "TES-${System.currentTimeMillis() % 10000}",
                customerName = "Tes Printer",
                cashierName = _activeUserName.value,
                subtotal = 15000.0,
                finalAmount = 15000.0,
                amountPaid = 20000.0,
                changeAmount = 5000.0,
                paymentMethod = "TUNAI"
            )
            val dummyItems = listOf(
                CartItem(
                    productId = 0L,
                    productName = "Tes Koneksi Printer KASLITE",
                    unitName = "pcs",
                    quantity = 1.0,
                    unitPrice = 15000.0
                )
            )
            val ok = ReceiptPrintHelper.printViaSystemPrintManager(context, dummySale, dummyItems, storeProfile.value)
            onResult(ok, if (ok) "Membuka dialog tes cetak sistem..." else "Layanan cetak sistem tidak tersedia")
            return
        }

        viewModelScope.launch {
            val payload = ThermalPrinterEngine.buildTestPageBytes(printer, storeProfile.value)
            val res = ThermalPrinterEngine.sendToPrinter(context, printer, payload)
            res.onSuccess { msg ->
                _userMessage.value = msg
                onResult(true, msg)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Tes cetak gagal"
                _userMessage.value = msg
                onResult(false, msg)
            }
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
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _userMessage.value = "Pengeluaran dihapus & saldo kas dikembalikan"
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.deleteIncome(income)
            _userMessage.value = "Pemasukan dihapus & saldo kas disesuaikan"
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
                    senderName = "KASLITE AI",
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
                    senderName = "KASLITE AI",
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
                        senderName = "KASLITE AI",
                        text = "✅ Tindakan berhasil dieksekusi dan disimpan ke database KASLITE."
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
                    senderName = "KASLITE AI",
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
                    licensesJob?.cancel()
                    supabaseRepository.signOut()
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
                        isSuperAdmin = false,
                        verifiedGoogleUid = "",
                        verifiedGoogleEmail = "",
                        verifiedGoogleName = "",
                        userId = "",
                        userEmail = "",
                        fullName = "",
                        storeName = "Toko Saya",
                        authError = null,
                        authInfoMessage = "Database bersih tanpa data dummy. Silakan daftar atau buat akun Admin Utama."
                    )
                }
                _userMessage.value = "Database berhasil dibersihkan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal reset: ${e.message}"
            }
        }
    }

    // --- Excel Product Import & Report Export (.xlsx / .csv) ---
    private val _excelImportState = MutableStateFlow(ExcelImportUiState())
    val excelImportState: StateFlow<ExcelImportUiState> = _excelImportState.asStateFlow()

    fun inspectExcelProductsUri(uri: Uri) {
        viewModelScope.launch {
            _excelImportState.update {
                it.copy(isInspecting = true, inspectResult = null, errorMessage = null)
            }
            try {
                val res = ExcelProductReportManager.inspectProductSpreadsheet(getApplication(), uri)
                _excelImportState.update {
                    it.copy(isInspecting = false, inspectResult = res, errorMessage = null)
                }
            } catch (e: Exception) {
                _excelImportState.update {
                    it.copy(isInspecting = false, inspectResult = null, errorMessage = e.localizedMessage ?: "Gagal membaca file Excel/CSV.")
                }
            }
        }
    }

    fun confirmExcelProductImport(updateExisting: Boolean) {
        val inspect = _excelImportState.value.inspectResult ?: return
        viewModelScope.launch {
            _excelImportState.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                val summary = ExcelProductReportManager.importProductsToRoom(
                    db = database,
                    inspect = inspect,
                    updateExistingByBarcodeOrName = updateExisting
                )
                _excelImportState.value = ExcelImportUiState(lastSummary = summary)
                _userMessage.value = summary.message
            } catch (e: Exception) {
                _excelImportState.update {
                    it.copy(isImporting = false, errorMessage = "Gagal import produk Excel: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun dismissExcelImportPreview() {
        _excelImportState.update {
            it.copy(isInspecting = false, isImporting = false, inspectResult = null, errorMessage = null)
        }
    }

    fun exportExcelProductTemplate(targetUri: Uri) {
        viewModelScope.launch {
            val res = ExcelProductReportManager.exportProductTemplateXlsx(getApplication(), targetUri)
            res.onSuccess {
                _userMessage.value = "Template Excel Produk (.xlsx) berhasil disimpan!"
            }.onFailure { err ->
                _userMessage.value = "Gagal menyimpan template Excel: ${err.localizedMessage}"
            }
        }
    }

    fun exportReportsToExcel(targetUri: Uri) {
        viewModelScope.launch {
            val currentStoreName = storeProfile.value?.name?.ifBlank { "KASLITE" } ?: "KASLITE"
            val res = ExcelProductReportManager.exportReportsXlsxToUri(
                context = getApplication(),
                db = database,
                storeName = currentStoreName,
                targetUri = targetUri
            )
            res.onSuccess {
                _userMessage.value = "Laporan Excel (.xlsx) berhasil diekspor (6 sheet lengkap)!"
            }.onFailure { err ->
                _userMessage.value = "Gagal ekspor laporan Excel: ${err.localizedMessage}"
            }
        }
    }

    fun shareReportsAsExcel(onUriReady: (Uri) -> Unit) {
        viewModelScope.launch {
            val currentStoreName = storeProfile.value?.name?.ifBlank { "KASLITE" } ?: "KASLITE"
            val res = ExcelProductReportManager.createShareableReportXlsxUri(
                context = getApplication(),
                db = database,
                storeName = currentStoreName
            )
            res.onSuccess { uri ->
                onUriReady(uri)
            }.onFailure { err ->
                _userMessage.value = "Gagal menyiapkan file Excel untuk dibagikan: ${err.localizedMessage}"
            }
        }
    }

    // --- Backup & Restore Lengkap (.kaslite) ---
    private val _kasLiteBackupState = MutableStateFlow(KasLiteBackupUiState())
    val kasLiteBackupState: StateFlow<KasLiteBackupUiState> = _kasLiteBackupState.asStateFlow()

    fun exportKasLiteBackup(targetUri: Uri) {
        viewModelScope.launch {
            _kasLiteBackupState.update { it.copy(isWorking = true, errorMessage = null) }
            val res = KasLiteBackupManager.exportBackupZipToUri(
                context = getApplication(),
                db = database,
                sessionStore = firebaseSessionStore,
                targetUri = targetUri
            )
            res.onSuccess { total ->
                _kasLiteBackupState.update { it.copy(isWorking = false) }
                _userMessage.value = "Backup lengkap .kaslite berhasil disimpan ($total rekaman data + foto)!"
            }.onFailure { err ->
                _kasLiteBackupState.update {
                    it.copy(isWorking = false, errorMessage = err.localizedMessage ?: "Gagal menyimpan backup .kaslite.")
                }
                _userMessage.value = "Gagal menyimpan backup: ${err.localizedMessage}"
            }
        }
    }

    fun shareKasLiteBackup(onUriReady: (Uri) -> Unit) {
        viewModelScope.launch {
            _kasLiteBackupState.update { it.copy(isWorking = true, errorMessage = null) }
            val res = KasLiteBackupManager.createShareableBackupZip(
                context = getApplication(),
                db = database,
                sessionStore = firebaseSessionStore
            )
            res.onSuccess { (uri, total) ->
                _kasLiteBackupState.update { it.copy(isWorking = false) }
                _userMessage.value = "File backup .kaslite siap dibagikan ($total rekaman)."
                onUriReady(uri)
            }.onFailure { err ->
                _kasLiteBackupState.update {
                    it.copy(isWorking = false, errorMessage = err.localizedMessage ?: "Gagal menyiapkan backup .kaslite.")
                }
                _userMessage.value = "Gagal membuat file bagikan: ${err.localizedMessage}"
            }
        }
    }

    fun inspectKasLiteBackupUri(uri: Uri) {
        viewModelScope.launch {
            _kasLiteBackupState.update {
                it.copy(isInspecting = true, inspectResult = null, errorMessage = null)
            }
            try {
                val res = KasLiteBackupManager.inspectBackupZip(getApplication(), uri)
                _kasLiteBackupState.update {
                    it.copy(isInspecting = false, inspectResult = res, errorMessage = null)
                }
            } catch (e: Exception) {
                _kasLiteBackupState.update {
                    it.copy(isInspecting = false, inspectResult = null, errorMessage = e.localizedMessage ?: "Gagal membaca file .kaslite.")
                }
            }
        }
    }

    fun confirmKasLiteRestore(replaceExisting: Boolean) {
        val currentInspect = _kasLiteBackupState.value.inspectResult ?: return
        viewModelScope.launch {
            _kasLiteBackupState.update { it.copy(isRestoring = true, errorMessage = null) }
            try {
                val summary = KasLiteBackupManager.restoreBackupZip(
                    context = getApplication(),
                    db = database,
                    sessionStore = firebaseSessionStore,
                    inspect = currentInspect,
                    replaceExisting = replaceExisting
                )
                KasLiteBackupManager.cleanupTemp(currentInspect)
                _kasLiteBackupState.value = KasLiteBackupUiState(lastRestoreSummary = summary)
                _userMessage.value = summary.message
            } catch (e: Exception) {
                _kasLiteBackupState.update {
                    it.copy(isRestoring = false, errorMessage = "Gagal memulihkan backup: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun dismissKasLiteRestorePreview() {
        KasLiteBackupManager.cleanupTemp(_kasLiteBackupState.value.inspectResult)
        _kasLiteBackupState.update {
            it.copy(isInspecting = false, isRestoring = false, inspectResult = null, errorMessage = null)
        }
    }

    // --- Donasi Pengembang & Laporan Bug ---
    private val _developerBankConfig = MutableStateFlow(DeveloperBankConfig())
    val developerBankConfig: StateFlow<DeveloperBankConfig> = _developerBankConfig.asStateFlow()

    private val _myDonationRequests = MutableStateFlow<List<DonationRequestDoc>>(emptyList())
    val myDonationRequests: StateFlow<List<DonationRequestDoc>> = _myDonationRequests.asStateFlow()

    private val _allDonationRequests = MutableStateFlow<List<DonationRequestDoc>>(emptyList())
    val allDonationRequests: StateFlow<List<DonationRequestDoc>> = _allDonationRequests.asStateFlow()

    private val _myBugReports = MutableStateFlow<List<BugReportDoc>>(emptyList())
    val myBugReports: StateFlow<List<BugReportDoc>> = _myBugReports.asStateFlow()

    private val _allBugReports = MutableStateFlow<List<BugReportDoc>>(emptyList())
    val allBugReports: StateFlow<List<BugReportDoc>> = _allBugReports.asStateFlow()

    private val _isSupportSyncLoading = MutableStateFlow(false)
    val isSupportSyncLoading: StateFlow<Boolean> = _isSupportSyncLoading.asStateFlow()

    private val _supportError = MutableStateFlow<String?>(null)
    val supportError: StateFlow<String?> = _supportError.asStateFlow()

    fun clearSupportError() {
        _supportError.value = null
    }

    fun loadCustomerSupportData() {
        if (!supabaseRepository.isConfigured()) {
            _supportError.value = "Supabase belum dikonfigurasi pada build ini."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSupportSyncLoading.value = true
            _supportError.value = null
            try {
                val cfg = supabaseRepository.fetchDeveloperBankConfig()
                _developerBankConfig.value = cfg
                if (firebaseSessionStore.hasSupabaseSession()) {
                    val uid = firebaseSessionStore.getSupabaseUid()
                    val email = firebaseSessionStore.getSupabaseEmail()
                    runCatching { supabaseRepository.fetchMyDonationRequests(uid, email) }
                        .onSuccess { _myDonationRequests.value = it }
                        .onFailure { err -> _supportError.value = err.localizedMessage }
                    runCatching { supabaseRepository.fetchMyBugReports(uid, email) }
                        .onSuccess { _myBugReports.value = it }
                        .onFailure { err ->
                            if (_supportError.value == null) _supportError.value = err.localizedMessage
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _supportError.value = e.localizedMessage ?: "Gagal memuat data pengembang."
            } finally {
                _isSupportSyncLoading.value = false
            }
        }
    }

    fun submitCustomerDonationRequest(
        pkg: DonationPackage,
        recipientEmail: String,
        ownerName: String,
        storeName: String,
        phone: String,
        senderBankName: String,
        senderAccountName: String,
        transferNote: String,
        onRequireAuth: () -> Unit = {},
        onResult: (Boolean, String, DonationRequestDoc?) -> Unit = { _, _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi pada build ini.", null)
            return
        }
        if (!firebaseSessionStore.hasSupabaseSession()) {
            onRequireAuth()
            onResult(false, "Silakan verifikasi akun Email & Password Supabase terlebih dahulu.", null)
            return
        }
        val cleanEmail = recipientEmail.trim().lowercase(Locale.ROOT)
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            onResult(false, "Masukkan alamat email penerima lisensi yang valid.", null)
            return
        }
        if (senderBankName.isBlank() || senderAccountName.isBlank()) {
            onResult(false, "Isi nama bank pengirim dan nama pemilik rekening pengirim.", null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isSupportSyncLoading.value = true
            _supportError.value = null
            try {
                val uid = firebaseSessionStore.getSupabaseUid()
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()
                val req = DonationRequestDoc(
                    userUid = uid,
                    userEmail = cleanEmail,
                    ownerName = ownerName.trim().ifBlank { _activeUserName.value },
                    storeName = storeName.trim().ifBlank { storeProfile.value?.name.orEmpty() },
                    phone = phone.trim().ifBlank { storeProfile.value?.phone.orEmpty() },
                    deviceId = deviceId,
                    packageId = pkg.id,
                    packageTitle = pkg.title,
                    amountRupiah = pkg.amountRupiah,
                    durationDays = pkg.durationDays,
                    senderBankName = senderBankName.trim(),
                    senderAccountName = senderAccountName.trim(),
                    transferNote = transferNote.trim()
                )
                val saved = supabaseRepository.submitDonationRequest(req)
                val updatedList = supabaseRepository.fetchMyDonationRequests(uid, cleanEmail)
                _myDonationRequests.value = updatedList
                withContext(Dispatchers.Main) {
                    val msg = "Konfirmasi donasi terkirim! Admin ($SUPER_ADMIN_EMAIL) akan verifikasi dan mengirim kunci lisensi ke $cleanEmail."
                    _userMessage.value = msg
                    onResult(true, msg, saved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Gagal mengirim konfirmasi donasi."
                _supportError.value = msg
                withContext(Dispatchers.Main) {
                    onResult(false, msg, null)
                }
            } finally {
                _isSupportSyncLoading.value = false
            }
        }
    }

    fun submitCustomerBugReport(
        category: String,
        severity: String,
        title: String,
        description: String,
        contactEmail: String,
        onRequireAuth: () -> Unit = {},
        onResult: (Boolean, String, BugReportDoc?) -> Unit = { _, _, _ -> }
    ) {
        if (title.isBlank()) {
            onResult(false, "Judul laporan bug wajib diisi.", null)
            return
        }
        if (description.trim().length < 10) {
            onResult(false, "Jelaskan kronologi bug minimal 10 karakter.", null)
            return
        }
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi. Gunakan tombol Kirim via Email.", null)
            return
        }
        if (!firebaseSessionStore.hasSupabaseSession()) {
            onRequireAuth()
            onResult(false, "Masuk dengan Email & Password Supabase dulu atau gunakan Kirim via Email.", null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isSupportSyncLoading.value = true
            _supportError.value = null
            try {
                val uid = firebaseSessionStore.getSupabaseUid()
                val email = contactEmail.trim().ifBlank { firebaseSessionStore.getSupabaseEmail() }
                val deviceId = firebaseSessionStore.getOrCreateDeviceId()
                val report = BugReportDoc(
                    userUid = uid,
                    userEmail = email,
                    reporterName = _activeUserName.value.ifBlank { firebaseSessionStore.getSupabaseFullName() },
                    storeName = storeProfile.value?.name.orEmpty(),
                    deviceId = deviceId,
                    category = category,
                    severity = severity,
                    title = title.trim(),
                    description = description.trim(),
                    appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                )
                val saved = supabaseRepository.submitBugReport(report)
                val updated = supabaseRepository.fetchMyBugReports(uid, email)
                _myBugReports.value = updated
                withContext(Dispatchers.Main) {
                    val msg = "Laporan bug berhasil dikirim ke tim pengembang!"
                    _userMessage.value = msg
                    onResult(true, msg, saved)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Gagal mengirim laporan bug."
                _supportError.value = msg
                withContext(Dispatchers.Main) {
                    onResult(false, msg, null)
                }
            } finally {
                _isSupportSyncLoading.value = false
            }
        }
    }

    fun buildCurrentDeviceBugDiagnosticSummary(): String {
        val deviceId = firebaseSessionStore.getOrCreateDeviceId()
        return "Versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • ID: $deviceId"
    }

    fun loadSuperAdminDonationsAndBugs() {
        val sessionEmail = firebaseSessionStore.getSupabaseEmail()
        if (!supabaseRepository.isConfigured() ||
            !_authUiState.value.isSuperAdmin ||
            !firebaseSessionStore.hasSupabaseSession() ||
            !sessionEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        ) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runCatching { supabaseRepository.fetchDeveloperBankConfig() }
                    .onSuccess { _developerBankConfig.value = it }
                runCatching { supabaseRepository.getAllDonationRequests() }
                    .onSuccess { _allDonationRequests.value = it }
                runCatching { supabaseRepository.getAllBugReports() }
                    .onSuccess { _allBugReports.value = it }
            } catch (_: Exception) {
            }
        }
    }

    fun saveSuperAdminBankConfig(
        config: DeveloperBankConfig,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.")
            return
        }
        if (config.bankName.isBlank() || config.accountNumber.isBlank() || config.accountHolder.isBlank()) {
            onResult(false, "Nama Bank, Nomor Rekening, dan Atas Nama wajib diisi.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved = supabaseRepository.saveDeveloperBankConfig(config)
                _developerBankConfig.value = saved
                withContext(Dispatchers.Main) {
                    val msg = "Rekening donasi pengembang berhasil disimpan!"
                    _userMessage.value = msg
                    onResult(true, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal menyimpan rekening: ${e.localizedMessage}")
                }
            }
        }
    }

    fun superAdminApproveDonationAndIssueLicense(
        donation: DonationRequestDoc,
        onResult: (Boolean, String, DonationApprovalOutcome?) -> Unit
    ) {
        if (!supabaseRepository.isConfigured()) {
            onResult(false, "Supabase belum dikonfigurasi.", null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val supportEmail = _developerBankConfig.value.supportEmail.ifBlank { SUPER_ADMIN_EMAIL }
                val outcome = supabaseRepository.approveDonationAndIssueLicense(donation, supportEmail)
                runCatching { supabaseRepository.getAllLicenses() }.onSuccess { _allLicenses.value = it }
                runCatching { supabaseRepository.getAllDonationRequests() }.onSuccess { _allDonationRequests.value = it }
                withContext(Dispatchers.Main) {
                    val msg = if (outcome.emailDispatchedAutomatically) {
                        "Sudah dibayar & kunci ${outcome.license.licenseKey} otomatis dikirim ke ${donation.userEmail}!"
                    } else {
                        "Sudah dibayar! Kunci ${outcome.license.licenseKey} dibuat — lanjutkan kirim email ke ${donation.userEmail}."
                    }
                    _userMessage.value = msg
                    onResult(true, msg, outcome)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal memproses donasi: ${e.localizedMessage}", null)
                }
            }
        }
    }

    fun superAdminMarkDonationEmailSent(donationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseRepository.markDonationEmailSent(donationId) }
            runCatching { supabaseRepository.getAllDonationRequests() }.onSuccess { _allDonationRequests.value = it }
        }
    }

    fun superAdminRejectDonation(
        donationId: String,
        reason: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.rejectDonationRequest(donationId, reason)
                runCatching { supabaseRepository.getAllDonationRequests() }.onSuccess { _allDonationRequests.value = it }
                withContext(Dispatchers.Main) {
                    val msg = "Konfirmasi donasi ditandai ditolak."
                    _userMessage.value = msg
                    onResult(true, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal menolak donasi: ${e.localizedMessage}")
                }
            }
        }
    }

    fun superAdminUpdateBugReportStatus(
        reportId: String,
        status: String,
        adminNotes: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepository.updateBugReportStatus(reportId, status, adminNotes)
                runCatching { supabaseRepository.getAllBugReports() }.onSuccess { _allBugReports.value = it }
                withContext(Dispatchers.Main) {
                    val msg = "Status laporan bug diperbarui ke $status"
                    _userMessage.value = msg
                    onResult(true, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal memperbarui laporan bug: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun formatRupiah(amount: Double): String {
        return java.text.NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.toLong())
    }

    // ============================================================================
    // GITHUB APP UPDATE CHECKER & OTA INSTALLER
    // ============================================================================
    data class AppUpdateInfo(
        val latestVersionName: String,
        val latestVersionCode: Int,
        val releaseNotes: String,
        val apkDownloadUrl: String
    )

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateProgressMessage = MutableStateFlow<String?>(null)
    val updateProgressMessage: StateFlow<String?> = _updateProgressMessage.asStateFlow()

    private fun parseVersionParts(rawVersion: String): List<Int> {
        return rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() }
    }

    private fun isRemoteVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = parseVersionParts(remoteVersion)
        val currentParts = parseVersionParts(currentVersion)
        if (remoteParts.isEmpty()) return false
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun normalizeGitHubSlug(rawInput: String): String {
        val cleaned = rawInput.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .trim('/')
        val parts = cleaned.split('/').filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else cleaned
    }

    fun getSavedGitHubUpdateRepo(): String {
        val defaultSlug = "egisbahricha-commits/Pos-ai-egis"
        val prefs = getApplication<Application>().getSharedPreferences("kaslite_update_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("github_repo_slug", defaultSlug)?.trim().orEmpty()
        return if (saved.isBlank() || saved == "egisbahricha/kaslite-pos") {
            defaultSlug
        } else {
            normalizeGitHubSlug(saved)
        }
    }

    fun saveGitHubUpdateRepo(repoSlug: String) {
        val cleaned = normalizeGitHubSlug(repoSlug)
        if (cleaned.contains("/")) {
            getApplication<Application>()
                .getSharedPreferences("kaslite_update_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("github_repo_slug", cleaned)
                .apply()
        }
    }

    fun checkForGitHubAppUpdate(
        githubRepoSlug: String = getSavedGitHubUpdateRepo(),
        onResult: (Boolean, String, AppUpdateInfo?) -> Unit
    ) {
        if (_isCheckingUpdate.value) return
        val normalizedSlug = normalizeGitHubSlug(githubRepoSlug)
        if (!normalizedSlug.contains("/")) {
            onResult(false, "Format repository GitHub harus: username/nama-repo", null)
            return
        }
        _isCheckingUpdate.value = true
        _updateProgressMessage.value = "Memeriksa pembaruan dari GitHub..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val urlStr = "https://api.github.com/repos/$normalizedSlug/releases/latest"
                val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "KASLITE-POS-Android/${BuildConfig.VERSION_NAME}")
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    val hint = when (code) {
                        404 -> "Repo $normalizedSlug belum punya Release atau masih Private (HTTP 404)."
                        403 -> "Akses API GitHub dibatasi sementara (HTTP 403). Coba lagi sebentar."
                        else -> "Gagal terhubung ke GitHub (HTTP $code)."
                    }
                    throw IllegalStateException(hint)
                }
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootObj = org.json.JSONObject(responseText)
                val tagName = rootObj.optString("tag_name", "v${BuildConfig.VERSION_NAME}").trim()
                val releaseNotes = rootObj.optString("body", "Pembaruan sistem dan perbaikan bug KASLITE.")
                    .ifBlank { "Pembaruan sistem dan perbaikan bug KASLITE." }

                var apkUrl = ""
                val assets = rootObj.optJSONArray("assets") ?: org.json.JSONArray()
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
                if (apkUrl.isBlank()) {
                    apkUrl = rootObj.optString("html_url", "")
                }

                val hasNewerVersion = isRemoteVersionNewer(tagName, BuildConfig.VERSION_NAME)
                val updateInfo = AppUpdateInfo(
                    latestVersionName = tagName,
                    latestVersionCode = BuildConfig.VERSION_CODE + if (hasNewerVersion) 1 else 0,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl
                )

                withContext(Dispatchers.Main) {
                    _isCheckingUpdate.value = false
                    _updateProgressMessage.value = null
                    if (hasNewerVersion && apkUrl.isNotBlank()) {
                        onResult(true, "Versi baru $tagName tersedia!", updateInfo)
                    } else {
                        onResult(false, "Aplikasi Anda sudah menggunakan versi terbaru (${BuildConfig.VERSION_NAME}).", updateInfo)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isCheckingUpdate.value = false
                    _updateProgressMessage.value = null
                    onResult(false, "Gagal memeriksa pembaruan: ${e.localizedMessage}", null)
                }
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context, updateInfo: AppUpdateInfo) {
        val targetUrl = updateInfo.apkDownloadUrl.ifBlank { "https://github.com" }
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _userMessage.value = "Membuka unduhan APK ${updateInfo.latestVersionName}. Setelah selesai, ketuk file APK untuk menimpa tanpa hapus data."
        }.onFailure { err ->
            _userMessage.value = "Gagal membuka link unduhan: ${err.localizedMessage}"
        }
    }
}
