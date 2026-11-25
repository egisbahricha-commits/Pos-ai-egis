package com.example.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.firebase.AdminLicensePlanOption
import com.example.firebase.BugReportDoc
import com.example.firebase.DeveloperBankConfig
import com.example.firebase.DonationApprovalOutcome
import com.example.firebase.DonationPackage
import com.example.firebase.DonationRequestDoc
import com.example.firebase.FirebaseSessionStore
import com.example.firebase.FreeLifetimePromoStatus
import com.example.firebase.KasLiteSupportEmailHelper
import com.example.firebase.LicenseDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class SupabaseAuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val expiresAtMillis: Long = 0L,
    val requiresEmailConfirmation: Boolean = false,
    val message: String = ""
)

class SuspendedLicenseException(
    val licenseDoc: LicenseDoc,
    message: String
) : IllegalStateException(message)

class SupabaseLicenseRepository(
    context: Context,
    private val sessionStore: FirebaseSessionStore,
    supabaseUrlOverride: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyOverride: String = BuildConfig.SUPABASE_ANON_KEY
) {
    private val appContext = context.applicationContext
    private val baseUrl: String = normalizeBaseUrl(supabaseUrlOverride)
    private val anonKey: String = supabaseAnonKeyOverride.trim()

    companion object {
        private const val TAG = "SupabaseLicenseRepo"
        private const val DEMO_DURATION_MILLIS = 3L * 24L * 60L * 60L * 1000L
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun normalizeBaseUrl(rawUrl: String): String = rawUrl.trim().trimEnd('/')

        fun isValidConfig(url: String, anonKey: String): Boolean {
            val cleanUrl = normalizeBaseUrl(url)
            val cleanKey = anonKey.trim()
            if (cleanUrl.isBlank() || cleanKey.isBlank()) return false
            if (cleanUrl.contains("PLACEHOLDER", ignoreCase = true) ||
                cleanUrl.contains("YOUR_PROJECT", ignoreCase = true) ||
                cleanKey.contains("PLACEHOLDER", ignoreCase = true)
            ) {
                return false
            }
            val lower = cleanUrl.lowercase(Locale.ROOT)
            val allowedScheme = lower.startsWith("https://") ||
                lower.startsWith("http://10.0.2.2") ||
                lower.startsWith("http://localhost") ||
                lower.startsWith("http://127.0.0.1")
            if (!allowedScheme) return false
            val hostPart = cleanUrl.substringAfter("://", "").substringBefore('/').substringBefore(':')
            return (hostPart.isNotBlank() && hostPart.contains('.')) ||
                hostPart == "localhost" || hostPart.startsWith("10.") || hostPart.startsWith("127.")
        }

        fun generateLicenseKey(): String {
            val code = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.ROOT)
            return "KASLITE-$code"
        }

        fun urlEncode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")

        fun isoToMillis(raw: String?): Long {
            val text = raw?.trim().orEmpty()
            if (text.isBlank() || text == "null") return 0L
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (pattern in patterns) {
                runCatching {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    sdf.parse(text)?.time
                }.getOrNull()?.let { return it }
            }
            return 0L
        }

        fun parseLicenseDocJson(obj: JSONObject): LicenseDoc = LicenseDoc(
            id = obj.optString("id", ""),
            licenseKey = obj.optString("license_key", ""),
            storeName = obj.optString("store_name", ""),
            ownerName = obj.optString("owner_name", ""),
            ownerEmail = obj.optString("owner_email", ""),
            ownerUid = obj.optString("owner_uid", ""),
            phone = obj.optString("phone", ""),
            deviceId = obj.optString("device_id", ""),
            planType = obj.optString("plan_type", "DEMO"),
            status = obj.optString("status", "DEMO"),
            demoStartedAtMillis = obj.optLong("demo_started_at_millis", 0L),
            demoExpiresAtMillis = obj.optLong("demo_expires_at_millis", 0L),
            activatedAtMillis = obj.optLong("activated_at_millis", 0L),
            expiresAtMillis = obj.optLong("expires_at_millis", 0L),
            createdAtMillis = isoToMillis(obj.optString("created_at", "")),
            updatedAtMillis = isoToMillis(obj.optString("updated_at", ""))
        )

        fun licenseDocToInsertJson(doc: LicenseDoc): JSONObject = JSONObject().apply {
            put("license_key", doc.licenseKey)
            put("store_name", doc.storeName)
            put("owner_name", doc.ownerName)
            put("owner_email", doc.ownerEmail.trim().lowercase(Locale.ROOT))
            put("owner_uid", doc.ownerUid)
            put("phone", doc.phone)
            put("device_id", doc.deviceId)
            put("plan_type", doc.planType)
            put("status", doc.status)
            put("demo_started_at_millis", doc.demoStartedAtMillis)
            put("demo_expires_at_millis", doc.demoExpiresAtMillis)
            put("activated_at_millis", doc.activatedAtMillis)
            put("expires_at_millis", doc.expiresAtMillis)
        }

        fun parseAuthSessionJson(root: JSONObject, nowMillis: Long = System.currentTimeMillis()): SupabaseAuthSession {
            val sessionObj = root.optJSONObject("session")
            val accessToken = root.optString("access_token", "")
                .ifBlank { sessionObj?.optString("access_token", "").orEmpty() }
            val refreshToken = root.optString("refresh_token", "")
                .ifBlank { sessionObj?.optString("refresh_token", "").orEmpty() }
            val expiresInSec = root.optLong("expires_in", sessionObj?.optLong("expires_in", 3600L) ?: 3600L)
            val userObj = root.optJSONObject("user") ?: sessionObj?.optJSONObject("user") ?: root
            val uid = userObj.optString("id", "")
            val email = userObj.optString("email", "")
            val metadata = userObj.optJSONObject("user_metadata")
            val fullName = metadata?.optString("full_name", "").orEmpty()
                .ifBlank { metadata?.optString("name", "").orEmpty() }

            val requiresConfirmation = accessToken.isBlank() && uid.isNotBlank()
            return SupabaseAuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                uid = uid,
                email = email,
                fullName = fullName,
                expiresAtMillis = if (accessToken.isNotBlank()) nowMillis + (expiresInSec * 1000L) else 0L,
                requiresEmailConfirmation = requiresConfirmation
            )
        }

        fun translateErrorMessage(statusCode: Int, rawBody: String): String {
            val jsonMsg = runCatching {
                val obj = JSONObject(rawBody)
                listOf("msg", "message", "error_description", "error", "details", "hint")
                    .firstNotNullOfOrNull { key ->
                        obj.optString(key, "").takeIf { it.isNotBlank() && it != "null" }
                    }
            }.getOrNull().orEmpty()

            val msgLower = (jsonMsg.ifBlank { rawBody }).lowercase(Locale.ROOT)
            return when {
                msgLower.contains("invalid login credentials") ->
                    "Email atau password salah."
                msgLower.contains("email not confirmed") ->
                    "Email belum dikonfirmasi. Cek kotak masuk atau folder spam email Anda."
                msgLower.contains("user already registered") || msgLower.contains("already been registered") ->
                    "Email sudah terdaftar. Silakan gunakan tab 'Masuk Akun Lama' dengan password Anda."
                msgLower.contains("password should be at least") || msgLower.contains("weak password") ->
                    "Password minimal 6 karakter."
                msgLower.contains("rate limit") || statusCode == 429 ->
                    "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
                msgLower.contains("jwt expired") || msgLower.contains("invalid jwt") || statusCode == 401 ->
                    "Sesi login berakhir. Silakan login ulang dengan Email & Password."
                msgLower.contains("row-level security") || statusCode == 403 ->
                    "Izin database ditolak oleh aturan keamanan Supabase."
                msgLower.contains("does not exist") || msgLower.contains("pgrst205") || msgLower.contains("42p01") || msgLower.contains("could not find the table") ->
                    "Tabel Supabase belum dibuat. Buka Admin Pusat -> Rekening & Email -> Salin SQL dan jalankan di SQL Editor Supabase."
                jsonMsg.isNotBlank() -> jsonMsg
                else -> "Permintaan Supabase gagal (HTTP $statusCode)."
            }
        }

        fun parseDeveloperBankConfigJson(obj: JSONObject): DeveloperBankConfig = DeveloperBankConfig(
            id = obj.optString("id", "payment_config").ifBlank { "payment_config" },
            bankName = obj.optString("bank_name", ""),
            accountNumber = obj.optString("account_number", ""),
            accountHolder = obj.optString("account_holder", ""),
            whatsappNumber = obj.optString("whatsapp_number", ""),
            supportEmail = obj.optString("support_email", "egisbahricha@gmail.com").ifBlank { "egisbahricha@gmail.com" },
            notes = obj.optString("notes", ""),
            freeLifetimeQuota = obj.optInt("free_lifetime_quota", 0).coerceAtLeast(0),
            freeYearlyQuota = obj.optInt("free_yearly_quota", 0).coerceAtLeast(0),
            updatedAtMillis = isoToMillis(obj.optString("updated_at", ""))
        )

        fun developerBankConfigToJson(
            cfg: DeveloperBankConfig,
            includePromoQuota: Boolean = true
        ): JSONObject = JSONObject().apply {
            put("id", cfg.id.ifBlank { "payment_config" })
            put("bank_name", cfg.bankName.trim())
            put("account_number", cfg.accountNumber.trim())
            put("account_holder", cfg.accountHolder.trim())
            put("whatsapp_number", cfg.whatsappNumber.trim())
            put("support_email", cfg.supportEmail.trim().ifBlank { "egisbahricha@gmail.com" })
            put("notes", cfg.notes.trim())
            if (includePromoQuota) {
                put("free_lifetime_quota", cfg.freeLifetimeQuota.coerceAtLeast(0))
                put("free_yearly_quota", cfg.freeYearlyQuota.coerceAtLeast(0))
            }
        }

        fun parseFreeLifetimePromoStatusJson(obj: JSONObject): FreeLifetimePromoStatus {
            val quota = obj.optInt("quota", 0).coerceAtLeast(0)
            val used = obj.optInt("used", 0).coerceAtLeast(0)
            val remaining = if (obj.has("remaining")) {
                obj.optInt("remaining", 0).coerceAtLeast(0)
            } else {
                (quota - used).coerceAtLeast(0)
            }
            val yearlyQuota = obj.optInt("yearly_quota", 0).coerceAtLeast(0)
            val yearlyRemaining = if (obj.has("yearly_remaining")) {
                obj.optInt("yearly_remaining", 0).coerceAtLeast(0)
            } else {
                ((quota + yearlyQuota) - maxOf(used, quota)).coerceAtLeast(0)
            }
            return FreeLifetimePromoStatus(
                quota = quota,
                used = used,
                remaining = remaining,
                yearlyQuota = yearlyQuota,
                yearlyRemaining = yearlyRemaining
            )
        }

        /**
         * Menghitung masa berlaku lisensi FULL berikutnya (1 bulan / 1 tahun / selamanya).
         * Jika lisensi saat ini masih FULL AKTIF berbatas waktu, durasi perpanjangan ditambahkan dari sisa masa aktif.
         */
        fun computeExtendedLicenseExpiryMillis(
            nowMillis: Long,
            currentPlanType: String,
            currentStatus: String,
            currentExpiresAtMillis: Long,
            durationDays: Int
        ): Long {
            if (durationDays <= 0) return 0L
            val isCurrentlyActiveFull = (currentPlanType.equals("FULL", ignoreCase = true) ||
                currentStatus.equals("ACTIVE", ignoreCase = true)) &&
                !currentStatus.equals("SUSPENDED", ignoreCase = true) &&
                !currentStatus.equals("EXPIRED", ignoreCase = true)
            val baseMillis = if (isCurrentlyActiveFull && currentExpiresAtMillis > nowMillis) {
                currentExpiresAtMillis
            } else {
                nowMillis
            }
            return baseMillis + durationDays.toLong() * ONE_DAY_MILLIS
        }

        fun parseDonationRequestJson(obj: JSONObject): DonationRequestDoc = DonationRequestDoc(
            id = obj.optString("id", ""),
            userUid = obj.optString("user_uid", ""),
            userEmail = obj.optString("user_email", ""),
            ownerName = obj.optString("owner_name", ""),
            storeName = obj.optString("store_name", ""),
            phone = obj.optString("phone", ""),
            deviceId = obj.optString("device_id", ""),
            packageId = obj.optString("package_id", DonationPackage.MONTHLY.id),
            packageTitle = obj.optString("package_title", ""),
            amountRupiah = obj.optLong("amount_rupiah", 0L),
            durationDays = obj.optInt("duration_days", 30),
            senderBankName = obj.optString("sender_bank_name", ""),
            senderAccountName = obj.optString("sender_account_name", ""),
            transferNote = obj.optString("transfer_note", ""),
            status = obj.optString("status", "PENDING"),
            licenseKey = obj.optString("license_key", ""),
            adminNotes = obj.optString("admin_notes", ""),
            approvedAtMillis = obj.optLong("approved_at_millis", 0L),
            emailSentAtMillis = obj.optLong("email_sent_at_millis", 0L),
            createdAtMillis = isoToMillis(obj.optString("created_at", "")),
            updatedAtMillis = isoToMillis(obj.optString("updated_at", ""))
        )

        fun donationRequestToInsertJson(doc: DonationRequestDoc): JSONObject = JSONObject().apply {
            put("user_uid", doc.userUid)
            put("user_email", doc.userEmail.trim().lowercase(Locale.ROOT))
            put("owner_name", doc.ownerName.trim())
            put("store_name", doc.storeName.trim())
            put("phone", doc.phone.trim())
            put("device_id", doc.deviceId.trim())
            put("package_id", doc.packageId)
            put("package_title", doc.packageTitle.trim())
            put("amount_rupiah", doc.amountRupiah)
            put("duration_days", doc.durationDays)
            put("sender_bank_name", doc.senderBankName.trim())
            put("sender_account_name", doc.senderAccountName.trim())
            put("transfer_note", doc.transferNote.trim())
            put("status", "PENDING")
        }

        fun parseBugReportJson(obj: JSONObject): BugReportDoc = BugReportDoc(
            id = obj.optString("id", ""),
            userUid = obj.optString("user_uid", ""),
            userEmail = obj.optString("user_email", ""),
            reporterName = obj.optString("reporter_name", ""),
            storeName = obj.optString("store_name", ""),
            deviceId = obj.optString("device_id", ""),
            category = obj.optString("category", "Kasir POS"),
            severity = obj.optString("severity", "SEDANG"),
            title = obj.optString("title", ""),
            description = obj.optString("description", ""),
            appVersion = obj.optString("app_version", ""),
            deviceModel = obj.optString("device_model", ""),
            androidVersion = obj.optString("android_version", ""),
            status = obj.optString("status", "BARU"),
            adminNotes = obj.optString("admin_notes", ""),
            createdAtMillis = isoToMillis(obj.optString("created_at", "")),
            updatedAtMillis = isoToMillis(obj.optString("updated_at", ""))
        )

        fun bugReportToInsertJson(doc: BugReportDoc): JSONObject = JSONObject().apply {
            put("user_uid", doc.userUid)
            put("user_email", doc.userEmail.trim().lowercase(Locale.ROOT))
            put("reporter_name", doc.reporterName.trim())
            put("store_name", doc.storeName.trim())
            put("device_id", doc.deviceId.trim())
            put("category", doc.category.trim())
            put("severity", doc.severity.trim().uppercase(Locale.ROOT))
            put("title", doc.title.trim())
            put("description", doc.description.trim())
            put("app_version", doc.appVersion.trim())
            put("device_model", doc.deviceModel.trim())
            put("android_version", doc.androidVersion.trim())
            put("status", "BARU")
        }

        fun resolveAuthoritativeCustomerLicense(
            byKey: LicenseDoc?,
            ownerRows: List<LicenseDoc>,
            currentDeviceId: String = ""
        ): LicenseDoc? {
            if (byKey != null) {
                if (byKey.status.equals("SUSPENDED", ignoreCase = true)) {
                    return byKey
                }
                if (byKey.deviceId.isNotBlank() &&
                    currentDeviceId.isNotBlank() &&
                    byKey.deviceId != currentDeviceId
                ) {
                    return byKey
                }
                if (byKey.planType.equals("FULL", ignoreCase = true) ||
                    byKey.status.equals("ACTIVE", ignoreCase = true)
                ) {
                    return byKey
                }
                // Currently on DEMO key: check if any row owned by this uid was upgraded/suspended as FULL
                val matchingFull = ownerRows
                    .filter {
                        (it.planType.equals("FULL", ignoreCase = true) ||
                            it.status.equals("ACTIVE", ignoreCase = true)) &&
                            (it.deviceId.isBlank() || currentDeviceId.isBlank() || it.deviceId == currentDeviceId)
                    }
                    .sortedWith(
                        compareByDescending<LicenseDoc> { it.status.equals("SUSPENDED", ignoreCase = true) }
                            .thenByDescending { it.updatedAtMillis }
                            .thenByDescending { it.activatedAtMillis }
                    )
                    .firstOrNull()
                if (matchingFull != null) return matchingFull
                return byKey
            }

            if (ownerRows.isEmpty()) return null
            val deviceEligible = ownerRows.filter {
                it.deviceId.isBlank() || currentDeviceId.isBlank() || it.deviceId == currentDeviceId
            }.ifEmpty { ownerRows }

            val bestFull = deviceEligible
                .filter {
                    it.planType.equals("FULL", ignoreCase = true) ||
                        it.status.equals("ACTIVE", ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<LicenseDoc> { it.status.equals("SUSPENDED", ignoreCase = true) }
                        .thenByDescending { it.status.equals("ACTIVE", ignoreCase = true) }
                        .thenByDescending { it.updatedAtMillis }
                        .thenByDescending { it.activatedAtMillis }
                )
                .firstOrNull()
            if (bestFull != null) return bestFull

            return deviceEligible
                .sortedWith(
                    compareByDescending<LicenseDoc> { it.status.equals("SUSPENDED", ignoreCase = true) }
                        .thenByDescending { it.updatedAtMillis }
                        .thenByDescending { it.createdAtMillis }
                )
                .firstOrNull()
        }
    }

    fun isConfigured(): Boolean = isValidConfig(baseUrl, anonKey)

    private fun requireConfigured() {
        if (!isConfigured()) {
            throw IllegalStateException(
                "Supabase belum dikonfigurasi. Isi SUPABASE_URL dan SUPABASE_ANON_KEY pada menu Secrets AI Studio."
            )
        }
    }

    private data class HttpResponseData(val code: Int, val body: String)

    private suspend fun executeHttp(
        method: String,
        pathAndQuery: String,
        bodyJson: String? = null,
        bearerToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponseData = withContext(Dispatchers.IO) {
        requireConfigured()
        val fullUrl = "$baseUrl$pathAndQuery"
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (bodyJson != null) {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            if (bodyJson != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(bodyJson)
                    writer.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val responseText = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else ""
            HttpResponseData(code, responseText)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String = ""
    ): Result<SupabaseAuthSession> = runCatching {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Format email tidak valid." }
        require(password.length >= 6) { "Password minimal 6 karakter." }

        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            if (fullName.isNotBlank()) {
                put("data", JSONObject().apply { put("full_name", fullName.trim()) })
            }
        }
        val res = executeHttp("POST", "/auth/v1/signup", payload.toString(), bearerToken = null)
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val parsed = parseAuthSessionJson(JSONObject(res.body))
        if (parsed.accessToken.isNotBlank() && parsed.uid.isNotBlank()) {
            sessionStore.saveSupabaseSession(
                accessToken = parsed.accessToken,
                refreshToken = parsed.refreshToken,
                uid = parsed.uid,
                email = parsed.email.ifBlank { cleanEmail },
                fullName = parsed.fullName.ifBlank { fullName.trim() },
                expiresAtMillis = parsed.expiresAtMillis
            )
            parsed.copy(
                email = parsed.email.ifBlank { cleanEmail },
                message = "Akun $cleanEmail berhasil didaftarkan & masuk."
            )
        } else {
            parsed.copy(
                email = parsed.email.ifBlank { cleanEmail },
                requiresEmailConfirmation = true,
                message = "Link konfirmasi dikirim ke $cleanEmail. Buka email Anda untuk verifikasi lalu login pada tab 'Masuk Akun Lama'."
            )
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseAuthSession> = runCatching {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Format email tidak valid." }
        require(password.isNotEmpty()) { "Password wajib diisi." }

        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
        }
        val res = executeHttp(
            method = "POST",
            pathAndQuery = "/auth/v1/token?grant_type=password",
            bodyJson = payload.toString(),
            bearerToken = null
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val parsed = parseAuthSessionJson(JSONObject(res.body))
        if (parsed.accessToken.isBlank() || parsed.uid.isBlank()) {
            throw IllegalStateException("Token sesi Supabase tidak diterima.")
        }
        val resolvedEmail = parsed.email.ifBlank { cleanEmail }
        sessionStore.saveSupabaseSession(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken,
            uid = parsed.uid,
            email = resolvedEmail,
            fullName = parsed.fullName,
            expiresAtMillis = parsed.expiresAtMillis
        )
        parsed.copy(email = resolvedEmail, message = "Berhasil masuk sebagai $resolvedEmail.")
    }

    suspend fun refreshSession(): Result<SupabaseAuthSession> = runCatching {
        val refreshToken = sessionStore.getSupabaseRefreshToken()
        if (refreshToken.isBlank()) {
            throw IllegalStateException("Sesi berakhir. Silakan login ulang dengan Email & Password.")
        }
        val payload = JSONObject().apply { put("refresh_token", refreshToken) }
        val res = executeHttp(
            method = "POST",
            pathAndQuery = "/auth/v1/token?grant_type=refresh_token",
            bodyJson = payload.toString(),
            bearerToken = null
        )
        if (res.code !in 200..299) {
            if (res.code == 400 || res.code == 401) {
                sessionStore.clearSupabaseSession()
            }
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val parsed = parseAuthSessionJson(JSONObject(res.body))
        if (parsed.accessToken.isBlank() || parsed.uid.isBlank()) {
            sessionStore.clearSupabaseSession()
            throw IllegalStateException("Gagal memperbarui token Supabase.")
        }
        sessionStore.saveSupabaseSession(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken.ifBlank { refreshToken },
            uid = parsed.uid,
            email = parsed.email.ifBlank { sessionStore.getSupabaseEmail() },
            fullName = parsed.fullName.ifBlank { sessionStore.getSupabaseFullName() },
            expiresAtMillis = parsed.expiresAtMillis
        )
        parsed
    }

    suspend fun ensureValidAccessToken(): String {
        val token = sessionStore.getSupabaseAccessToken()
        val exp = sessionStore.getSupabaseExpiresAtMillis()
        if (token.isNotBlank() && (exp == 0L || System.currentTimeMillis() < exp - 60_000L)) {
            return token
        }
        return refreshSession().getOrThrow().accessToken
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Masukkan email yang valid terlebih dahulu." }
        val payload = JSONObject().apply { put("email", cleanEmail) }
        val res = executeHttp("POST", "/auth/v1/recover", payload.toString(), bearerToken = null)
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
    }

    suspend fun signOut() {
        val token = sessionStore.getSupabaseAccessToken()
        if (token.isNotBlank() && isConfigured()) {
            runCatching {
                executeHttp("POST", "/auth/v1/logout", "{}", bearerToken = token)
            }.onFailure {
                Log.w(TAG, "Best-effort logout warning: ${it.message}")
            }
        }
        sessionStore.clearSupabaseSession()
    }

    private suspend fun executeRestWithRetry(
        method: String,
        pathAndQuery: String,
        bodyJson: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponseData {
        var token = ensureValidAccessToken()
        var res = executeHttp(method, pathAndQuery, bodyJson, bearerToken = token, extraHeaders = extraHeaders)
        if (res.code == 401 && sessionStore.getSupabaseRefreshToken().isNotBlank()) {
            token = refreshSession().getOrThrow().accessToken
            res = executeHttp(method, pathAndQuery, bodyJson, bearerToken = token, extraHeaders = extraHeaders)
        }
        return res
    }

    private suspend fun queryLicenseList(queryParams: String): List<LicenseDoc> {
        val res = executeRestWithRetry("GET", "/rest/v1/licenses?$queryParams")
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) {
                add(parseLicenseDocJson(array.getJSONObject(i)))
            }
        }
    }

    suspend fun fetchLicensesByOwnerUid(uid: String): List<LicenseDoc> {
        if (uid.isBlank()) return emptyList()
        return queryLicenseList("owner_uid=eq.${urlEncode(uid)}&select=*&order=updated_at.desc,created_at.desc&limit=20")
    }

    suspend fun fetchLicenseByOwnerUid(uid: String): LicenseDoc? {
        if (uid.isBlank()) return null
        return fetchLicensesByOwnerUid(uid).firstOrNull()
    }

    suspend fun fetchLicenseByEmail(email: String): LicenseDoc? {
        val clean = email.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return null
        val exact = queryLicenseList("owner_email=eq.${urlEncode(clean)}&select=*&order=updated_at.desc,created_at.desc&limit=10")
            .filter { it.ownerEmail.equals(clean, ignoreCase = true) }
        if (exact.isNotEmpty()) {
            return exact.firstOrNull { !it.planType.equals("FULL", ignoreCase = true) || it.ownerUid.isNotBlank() }
                ?: exact.firstOrNull()
        }
        val ilike = queryLicenseList("owner_email=ilike.${urlEncode(clean)}&select=*&order=updated_at.desc,created_at.desc&limit=10")
            .filter { it.ownerEmail.equals(clean, ignoreCase = true) }
        return ilike.firstOrNull { !it.planType.equals("FULL", ignoreCase = true) || it.ownerUid.isNotBlank() }
            ?: ilike.firstOrNull()
    }

    suspend fun fetchBestMatchingCustomerLicense(
        uid: String,
        email: String,
        currentLicenseKey: String,
        currentDeviceId: String = ""
    ): LicenseDoc? {
        val byKey = currentLicenseKey.trim().takeIf { it.isNotBlank() }?.let { getLicenseByKey(it) }
        val ownerRows = fetchLicensesByOwnerUid(uid)
        val resolved = resolveAuthoritativeCustomerLicense(byKey, ownerRows, currentDeviceId)
        if (resolved != null) return resolved

        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanEmail.isBlank()) return null
        val emailMatches = queryLicenseList("owner_email=eq.${urlEncode(cleanEmail)}&select=*&order=updated_at.desc,created_at.desc&limit=10")
            .filter {
                it.ownerEmail.equals(cleanEmail, ignoreCase = true) &&
                    (it.ownerUid == uid || (it.ownerUid.isBlank() && !it.planType.equals("FULL", ignoreCase = true)))
            }
        return resolveAuthoritativeCustomerLicense(null, emailMatches, currentDeviceId)
    }

    suspend fun getLicenseByKey(licenseKey: String): LicenseDoc? {
        val clean = licenseKey.trim()
        if (clean.isBlank()) return null
        return queryLicenseList("license_key=eq.${urlEncode(clean)}&select=*&limit=1").firstOrNull()
    }

    private suspend fun patchLicenseByQuery(queryFilter: String, patchBody: JSONObject): LicenseDoc? {
        val res = executeRestWithRetry(
            method = "PATCH",
            pathAndQuery = "/rest/v1/licenses?$queryFilter",
            bodyJson = patchBody.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseLicenseDocJson(array.getJSONObject(0)) else null
    }

    private suspend fun insertLicenseRow(doc: LicenseDoc): LicenseDoc {
        val res = executeRestWithRetry(
            method = "POST",
            pathAndQuery = "/rest/v1/licenses",
            bodyJson = licenseDocToInsertJson(doc).toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseLicenseDocJson(array.getJSONObject(0)) else doc
    }

    suspend fun registerOrFetchCustomerLicense(
        ownerUid: String,
        ownerEmail: String,
        ownerName: String,
        storeName: String,
        phone: String,
        deviceId: String
    ): LicenseDoc {
        val generatedKey = generateLicenseKey()
        // Gunakan RPC server-side terlebih dahulu agar Promo Gratis Selamanya (N pendaftar pertama)
        // tervalidasi atomik & aman terhadap manipulasi klien.
        val rpcPayload = JSONObject().apply {
            put("p_license_key", generatedKey)
            put("p_store_name", storeName.trim())
            put("p_owner_name", ownerName.trim())
            put("p_phone", phone.trim())
            put("p_device_id", deviceId)
        }
        val rpcRes = executeRestWithRetry(
            method = "POST",
            pathAndQuery = "/rest/v1/rpc/kaslite_register_customer_license",
            bodyJson = rpcPayload.toString()
        )
        if (rpcRes.code in 200..299) {
            val trimmed = rpcRes.body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                if (arr.length() > 0) return parseLicenseDocJson(arr.getJSONObject(0))
            } else if (trimmed.startsWith("{")) {
                return parseLicenseDocJson(JSONObject(trimmed))
            }
        } else {
            val lowerBody = rpcRes.body.lowercase(Locale.ROOT)
            val isMissingRpc = rpcRes.code == 404 ||
                lowerBody.contains("pgrst202") ||
                lowerBody.contains("could not find the function") ||
                lowerBody.contains("42883")
            if (!isMissingRpc) {
                throw IllegalStateException(translateErrorMessage(rpcRes.code, rpcRes.body))
            }
        }

        val cleanEmail = ownerEmail.trim().lowercase(Locale.ROOT)
        val existingByUid = fetchLicensesByOwnerUid(ownerUid).let { rows ->
            resolveAuthoritativeCustomerLicense(null, rows, deviceId)
        }
        // Only match by email during registration if it is already owned by this uid OR is an unclaimed DEMO row.
        // Never auto-claim an unclaimed FULL license key created by Admin Pusat — customer must enter that key manually.
        val existingByEmail = if (existingByUid == null) {
            queryLicenseList("owner_email=eq.${urlEncode(cleanEmail)}&select=*&order=updated_at.desc,created_at.desc&limit=10")
                .firstOrNull {
                    it.ownerEmail.equals(cleanEmail, ignoreCase = true) &&
                        (it.ownerUid == ownerUid || (it.ownerUid.isBlank() && it.planType.equals("DEMO", ignoreCase = true)))
                }
        } else null
        val existing = existingByUid ?: existingByEmail
        if (existing != null) {
            val needClaimOrBind = (existing.ownerUid.isBlank() || existing.deviceId.isBlank()) &&
                (existing.ownerUid.isBlank() || existing.ownerUid == ownerUid)
            if (needClaimOrBind) {
                val patch = JSONObject().apply {
                    if (existing.ownerUid.isBlank()) put("owner_uid", ownerUid)
                    if (existing.ownerEmail.isBlank()) put("owner_email", cleanEmail)
                    if (existing.ownerName.isBlank() && ownerName.isNotBlank()) put("owner_name", ownerName.trim())
                    if (existing.storeName.isBlank() && storeName.isNotBlank()) put("store_name", storeName.trim())
                    if (existing.phone.isBlank() && phone.isNotBlank()) put("phone", phone.trim())
                    if (existing.deviceId.isBlank() && deviceId.isNotBlank()) put("device_id", deviceId)
                }
                val patched = patchLicenseByQuery("id=eq.${urlEncode(existing.id)}", patch)
                if (patched != null) return patched
            }
            return existing
        }

        val now = System.currentTimeMillis()
        val demoEnd = now + DEMO_DURATION_MILLIS
        val newDoc = LicenseDoc(
            licenseKey = generatedKey,
            storeName = storeName.trim(),
            ownerName = ownerName.trim(),
            ownerEmail = cleanEmail,
            ownerUid = ownerUid,
            phone = phone.trim(),
            deviceId = deviceId,
            planType = "DEMO",
            status = "DEMO",
            demoStartedAtMillis = now,
            demoExpiresAtMillis = demoEnd,
            activatedAtMillis = 0L,
            expiresAtMillis = 0L
        )
        return insertLicenseRow(newDoc)
    }

    suspend fun activateLicenseKeyForCustomer(
        licenseKey: String,
        ownerUid: String,
        ownerEmail: String,
        ownerName: String,
        storeName: String,
        phone: String,
        deviceId: String
    ): LicenseDoc {
        val cleanKey = licenseKey.trim()
        val existing = getLicenseByKey(cleanKey)
            ?: throw IllegalStateException("Kunci lisensi \"$cleanKey\" tidak ditemukan. Hubungi Admin Pusat.")
        if (existing.status.equals("SUSPENDED", ignoreCase = true)) {
            throw SuspendedLicenseException(
                existing,
                "Lisensi \"$cleanKey\" sedang dinonaktifkan (SUSPENDED)."
            )
        }
        if (existing.ownerUid.isNotBlank() && existing.ownerUid != ownerUid) {
            throw IllegalStateException("Kunci lisensi ini terdaftar untuk akun lain (${existing.ownerEmail}).")
        }
        if (existing.ownerEmail.isNotBlank() && !existing.ownerEmail.equals(ownerEmail, ignoreCase = true)) {
            throw IllegalStateException("Kunci lisensi ini terdaftar untuk email ${existing.ownerEmail}.")
        }
        if (existing.deviceId.isNotBlank() && existing.deviceId != deviceId) {
            throw IllegalStateException("Kunci lisensi sudah aktif di perangkat lain. Hubungi Admin Pusat untuk Reset Device.")
        }

        if (existing.ownerUid.isBlank() || existing.deviceId.isBlank()) {
            val patch = JSONObject().apply {
                put("owner_uid", existing.ownerUid.ifBlank { ownerUid })
                put("owner_email", existing.ownerEmail.ifBlank { ownerEmail.trim().lowercase(Locale.ROOT) })
                if (existing.ownerName.isBlank() && ownerName.isNotBlank()) put("owner_name", ownerName.trim())
                if (existing.storeName.isBlank() && storeName.isNotBlank()) put("store_name", storeName.trim())
                if (existing.phone.isBlank() && phone.isNotBlank()) put("phone", phone.trim())
                put("device_id", existing.deviceId.ifBlank { deviceId })
            }
            return patchLicenseByQuery("id=eq.${urlEncode(existing.id)}", patch) ?: existing
        }
        return existing
    }

    suspend fun getAllLicenses(): List<LicenseDoc> =
        queryLicenseList("select=*&order=created_at.desc")

    suspend fun createSuperAdminLicense(
        storeName: String,
        ownerName: String,
        ownerEmail: String,
        phone: String = "",
        planType: String = "FULL",
        durationDays: Int = 0
    ): LicenseDoc {
        val now = System.currentTimeMillis()
        val isFull = planType.equals("FULL", ignoreCase = true)
        val generatedKey = generateLicenseKey()
        val doc = LicenseDoc(
            licenseKey = generatedKey,
            storeName = storeName.trim(),
            ownerName = ownerName.trim(),
            ownerEmail = ownerEmail.trim().lowercase(Locale.ROOT),
            ownerUid = "",
            phone = phone.trim(),
            deviceId = "",
            planType = if (isFull) "FULL" else "DEMO",
            status = if (isFull) "ACTIVE" else "DEMO",
            demoStartedAtMillis = if (isFull) 0L else now,
            demoExpiresAtMillis = if (isFull) 0L else now + DEMO_DURATION_MILLIS,
            activatedAtMillis = if (isFull) now else 0L,
            expiresAtMillis = if (isFull && durationDays > 0) now + durationDays.toLong() * ONE_DAY_MILLIS else 0L
        )
        return insertLicenseRow(doc)
    }

    suspend fun superAdminActivateFull(licenseKey: String, durationDays: Int = 0): LicenseDoc {
        val now = System.currentTimeMillis()
        val exp = if (durationDays > 0) now + durationDays.toLong() * ONE_DAY_MILLIS else 0L
        val patch = JSONObject().apply {
            put("plan_type", "FULL")
            put("status", "ACTIVE")
            put("activated_at_millis", now)
            put("expires_at_millis", exp)
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(licenseKey.trim())}", patch)
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
    }

    suspend fun superAdminApplyLicensePackage(
        licenseKey: String,
        option: AdminLicensePlanOption,
        extendExisting: Boolean = true
    ): LicenseDoc {
        if (option == AdminLicensePlanOption.DEMO_3) {
            return superAdminExtendDemo(licenseKey, option.durationDays)
        }
        val cleanKey = licenseKey.trim()
        val current = getLicenseByKey(cleanKey)
            ?: throw IllegalStateException("Lisensi $cleanKey tidak ditemukan.")
        val now = System.currentTimeMillis()
        val newExpiry = if (extendExisting) {
            computeExtendedLicenseExpiryMillis(
                nowMillis = now,
                currentPlanType = current.planType,
                currentStatus = current.status,
                currentExpiresAtMillis = current.expiresAtMillis,
                durationDays = option.durationDays
            )
        } else {
            if (option.durationDays > 0) now + option.durationDays.toLong() * ONE_DAY_MILLIS else 0L
        }
        val activatedAt = if (current.activatedAtMillis > 0L && current.planType.equals("FULL", ignoreCase = true)) {
            current.activatedAtMillis
        } else {
            now
        }
        val patch = JSONObject().apply {
            put("plan_type", "FULL")
            put("status", "ACTIVE")
            put("activated_at_millis", activatedAt)
            put("expires_at_millis", newExpiry)
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(cleanKey)}", patch)
            ?: throw IllegalStateException("Lisensi $cleanKey tidak ditemukan.")
    }

    suspend fun superAdminExtendDemo(licenseKey: String, days: Int = 3): LicenseDoc {
        val current = getLicenseByKey(licenseKey.trim())
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
        val now = System.currentTimeMillis()
        val base = maxOf(now, current.demoExpiresAtMillis)
        val newDemoExp = base + days.toLong() * ONE_DAY_MILLIS
        val patch = JSONObject().apply {
            put("plan_type", "DEMO")
            put("status", "DEMO")
            if (current.demoStartedAtMillis == 0L) {
                put("demo_started_at_millis", now)
            }
            put("demo_expires_at_millis", newDemoExp)
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(licenseKey.trim())}", patch) ?: current
    }

    suspend fun superAdminSetSuspended(licenseKey: String, suspended: Boolean): LicenseDoc {
        val current = getLicenseByKey(licenseKey.trim())
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
        val now = System.currentTimeMillis()
        val restoredStatus = when {
            current.planType.equals("FULL", ignoreCase = true) -> "ACTIVE"
            current.demoExpiresAtMillis > now -> "DEMO"
            else -> "EXPIRED"
        }
        val patch = JSONObject().apply {
            put("status", if (suspended) "SUSPENDED" else restoredStatus)
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(licenseKey.trim())}", patch) ?: current
    }

    suspend fun resetDeviceBinding(licenseKey: String): LicenseDoc {
        val patch = JSONObject().apply {
            put("device_id", "")
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(licenseKey.trim())}", patch)
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
    }

    // ========================================================================
    // PENGATURAN REKENING DONASI PENGEMBANG
    // ========================================================================
    suspend fun fetchDeveloperBankConfig(): DeveloperBankConfig {
        requireConfigured()
        val path = "/rest/v1/developer_settings?id=eq.payment_config&select=*&limit=1"
        val res = if (sessionStore.hasSupabaseSession()) {
            runCatching { executeRestWithRetry("GET", path) }
                .getOrElse { executeHttp("GET", path, bearerToken = anonKey) }
        } else {
            executeHttp("GET", path, bearerToken = anonKey)
        }
        if (res.code == 404) {
            return DeveloperBankConfig()
        }
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseDeveloperBankConfigJson(array.getJSONObject(0)) else DeveloperBankConfig()
    }

    suspend fun fetchFreeLifetimePromoStatus(): FreeLifetimePromoStatus {
        if (!isConfigured()) return FreeLifetimePromoStatus()
        val path = "/rest/v1/rpc/kaslite_get_promo_status"
        val res = runCatching {
            if (sessionStore.hasSupabaseSession()) {
                executeRestWithRetry("POST", path, "{}")
            } else {
                executeHttp("POST", path, "{}", bearerToken = anonKey)
            }
        }.getOrElse {
            return FreeLifetimePromoStatus()
        }
        if (res.code !in 200..299) {
            return FreeLifetimePromoStatus()
        }
        return runCatching {
            parseFreeLifetimePromoStatusJson(JSONObject(res.body.ifBlank { "{}" }))
        }.getOrDefault(FreeLifetimePromoStatus())
    }

    suspend fun saveDeveloperBankConfig(config: DeveloperBankConfig): DeveloperBankConfig {
        val normalized = config.copy(
            id = "payment_config",
            freeLifetimeQuota = config.freeLifetimeQuota.coerceAtLeast(0),
            freeYearlyQuota = config.freeYearlyQuota.coerceAtLeast(0)
        )
        val updatedIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        val payload = developerBankConfigToJson(normalized, includePromoQuota = true).apply {
            put("updated_at", updatedIso)
        }
        val res = executeRestWithRetry(
            method = "POST",
            pathAndQuery = "/rest/v1/developer_settings?on_conflict=id",
            bodyJson = payload.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
        )
        if (res.code !in 200..299) {
            val lower = res.body.lowercase(Locale.ROOT)
            val missingPromoCol = (lower.contains("free_lifetime_quota") || lower.contains("free_yearly_quota")) &&
                (lower.contains("pgrst204") || lower.contains("42703") || lower.contains("column") || lower.contains("schema cache"))
            if (missingPromoCol) {
                if (normalized.freeLifetimeQuota > 0 || normalized.freeYearlyQuota > 0) {
                    throw IllegalStateException(
                        "Kolom promo belum lengkap di Supabase (free_lifetime_quota / free_yearly_quota). Tekan 'Lihat & Salin SQL Promo Supabase' lalu jalankan di SQL Editor Supabase."
                    )
                }
                val fallbackPayload = developerBankConfigToJson(normalized, includePromoQuota = false).apply {
                    put("updated_at", updatedIso)
                }
                val fallbackRes = executeRestWithRetry(
                    method = "POST",
                    pathAndQuery = "/rest/v1/developer_settings?on_conflict=id",
                    bodyJson = fallbackPayload.toString(),
                    extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
                )
                if (fallbackRes.code !in 200..299) {
                    throw IllegalStateException(translateErrorMessage(fallbackRes.code, fallbackRes.body))
                }
                val arr = JSONArray(fallbackRes.body.ifBlank { "[]" })
                return if (arr.length() > 0) parseDeveloperBankConfigJson(arr.getJSONObject(0)) else normalized
            }
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseDeveloperBankConfigJson(array.getJSONObject(0)) else normalized
    }

    // ========================================================================
    // DONASI KE PENGEMBANG & VERIFIKASI LISENSI MANUAL
    // ========================================================================
    suspend fun submitDonationRequest(doc: DonationRequestDoc): DonationRequestDoc {
        val res = executeRestWithRetry(
            method = "POST",
            pathAndQuery = "/rest/v1/donation_requests",
            bodyJson = donationRequestToInsertJson(doc).toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseDonationRequestJson(array.getJSONObject(0)) else doc
    }

    suspend fun fetchMyDonationRequests(uid: String, email: String): List<DonationRequestDoc> {
        val filter = when {
            uid.isNotBlank() && email.isNotBlank() ->
                "or=(user_uid.eq.${urlEncode(uid)},user_email.eq.${urlEncode(email.trim().lowercase(Locale.ROOT))})"
            uid.isNotBlank() -> "user_uid=eq.${urlEncode(uid)}"
            email.isNotBlank() -> "user_email=eq.${urlEncode(email.trim().lowercase(Locale.ROOT))}"
            else -> return emptyList()
        }
        val res = executeRestWithRetry(
            method = "GET",
            pathAndQuery = "/rest/v1/donation_requests?$filter&select=*&order=created_at.desc&limit=30"
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) add(parseDonationRequestJson(array.getJSONObject(i)))
        }
    }

    suspend fun getAllDonationRequests(): List<DonationRequestDoc> {
        val res = executeRestWithRetry(
            method = "GET",
            pathAndQuery = "/rest/v1/donation_requests?select=*&order=created_at.desc&limit=100"
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) add(parseDonationRequestJson(array.getJSONObject(i)))
        }
    }

    suspend fun approveDonationAndIssueLicense(
        donation: DonationRequestDoc,
        supportEmail: String = "egisbahricha@gmail.com"
    ): DonationApprovalOutcome {
        val now = System.currentTimeMillis()
        val pkg = DonationPackage.fromId(donation.packageId)
        val effectiveDuration = if (donation.packageId.equals(DonationPackage.LIFETIME.id, ignoreCase = true)) {
            0
        } else {
            donation.durationDays.takeIf { it > 0 } ?: pkg.durationDays
        }

        // Reuse already issued license key if admin clicks resend/reapprove on the same PAID donation
        val existingIssuedKey = donation.licenseKey.trim().takeIf {
            donation.status.equals("PAID", ignoreCase = true) && it.isNotBlank()
        }
        val issuedLicense: LicenseDoc = if (existingIssuedKey != null) {
            getLicenseByKey(existingIssuedKey)
                ?: createSuperAdminLicense(
                    storeName = donation.storeName.ifBlank { "Toko Mitra KASLITE" },
                    ownerName = donation.ownerName,
                    ownerEmail = donation.userEmail,
                    phone = donation.phone,
                    planType = "FULL",
                    durationDays = effectiveDuration
                )
        } else {
            createSuperAdminLicense(
                storeName = donation.storeName.ifBlank { "Toko Mitra KASLITE" },
                ownerName = donation.ownerName,
                ownerEmail = donation.userEmail,
                phone = donation.phone,
                planType = "FULL",
                durationDays = effectiveDuration
            )
        }

        val edgeResult = tryDispatchLicenseEmailEdgeFunction(
            recipientEmail = donation.userEmail,
            ownerName = donation.ownerName,
            storeName = donation.storeName,
            licenseKey = issuedLicense.licenseKey,
            packageTitle = donation.packageTitle.ifBlank { pkg.title },
            amountRupiah = donation.amountRupiah.takeIf { it > 0L } ?: pkg.amountRupiah,
            expiresAtMillis = issuedLicense.expiresAtMillis,
            supportEmail = supportEmail
        )

        val patch = JSONObject().apply {
            put("status", "PAID")
            put("license_key", issuedLicense.licenseKey)
            put("approved_at_millis", if (donation.approvedAtMillis > 0L) donation.approvedAtMillis else now)
            if (edgeResult.first) {
                put("email_sent_at_millis", now)
            }
        }
        val patchRes = executeRestWithRetry(
            method = "PATCH",
            pathAndQuery = "/rest/v1/donation_requests?id=eq.${urlEncode(donation.id)}",
            bodyJson = patch.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (patchRes.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(patchRes.code, patchRes.body))
        }
        val array = JSONArray(patchRes.body.ifBlank { "[]" })
        val updatedDonation = if (array.length() > 0) {
            parseDonationRequestJson(array.getJSONObject(0))
        } else {
            donation.copy(
                status = "PAID",
                licenseKey = issuedLicense.licenseKey,
                approvedAtMillis = now,
                emailSentAtMillis = if (edgeResult.first) now else donation.emailSentAtMillis
            )
        }

        return DonationApprovalOutcome(
            donation = updatedDonation,
            license = issuedLicense,
            emailDispatchedAutomatically = edgeResult.first,
            emailDispatchMessage = edgeResult.second
        )
    }

    suspend fun tryDispatchLicenseEmailEdgeFunction(
        recipientEmail: String,
        ownerName: String,
        storeName: String,
        licenseKey: String,
        packageTitle: String,
        amountRupiah: Long,
        expiresAtMillis: Long,
        supportEmail: String = "egisbahricha@gmail.com"
    ): Pair<Boolean, String> {
        val cleanTo = recipientEmail.trim()
        if (cleanTo.isBlank() || !cleanTo.contains("@")) {
            return false to "Email tujuan tidak valid."
        }
        val expiryLabel = if (expiresAtMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(java.util.Date(expiresAtMillis))
        } else {
            "PERMANEN (Selamanya)"
        }
        val payload = JSONObject().apply {
            put("to", cleanTo)
            put("ownerName", ownerName)
            put("storeName", storeName)
            put("licenseKey", licenseKey)
            put("packageTitle", packageTitle)
            put("amountRupiah", amountRupiah)
            put("expiresAtMillis", expiresAtMillis)
            put("expiresLabel", expiryLabel)
            put("supportEmail", supportEmail)
            put("subject", KasLiteSupportEmailHelper.buildLicenseEmailSubject(storeName, licenseKey))
            put(
                "text",
                KasLiteSupportEmailHelper.buildLicenseEmailBody(
                    ownerName = ownerName,
                    storeName = storeName,
                    recipientEmail = cleanTo,
                    licenseKey = licenseKey,
                    packageTitle = packageTitle,
                    amountRupiah = amountRupiah,
                    expiresAtMillis = expiresAtMillis,
                    supportEmail = supportEmail
                )
            )
        }
        return runCatching {
            val token = ensureValidAccessToken()
            val res = executeHttp(
                method = "POST",
                pathAndQuery = "/functions/v1/send-license-email",
                bodyJson = payload.toString(),
                bearerToken = token
            )
            if (res.code in 200..299) {
                true to "Email lisensi otomatis terkirim ke $cleanTo!"
            } else {
                false to "Auto-email Edge Function belum aktif (HTTP ${res.code}); membuka aplikasi email untuk kirim ke $cleanTo."
            }
        }.getOrElse { err ->
            false to "Server email belum aktif (${err.localizedMessage}); membuka aplikasi email untuk kirim ke $cleanTo."
        }
    }

    suspend fun markDonationEmailSent(donationId: String): DonationRequestDoc? {
        if (donationId.isBlank()) return null
        val now = System.currentTimeMillis()
        val patch = JSONObject().apply { put("email_sent_at_millis", now) }
        val res = executeRestWithRetry(
            method = "PATCH",
            pathAndQuery = "/rest/v1/donation_requests?id=eq.${urlEncode(donationId)}",
            bodyJson = patch.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) return null
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseDonationRequestJson(array.getJSONObject(0)) else null
    }

    suspend fun rejectDonationRequest(donationId: String, adminNotes: String): DonationRequestDoc {
        val patch = JSONObject().apply {
            put("status", "REJECTED")
            put("admin_notes", adminNotes.trim())
        }
        val res = executeRestWithRetry(
            method = "PATCH",
            pathAndQuery = "/rest/v1/donation_requests?id=eq.${urlEncode(donationId)}",
            bodyJson = patch.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        if (array.length() == 0) throw IllegalStateException("Data donasi tidak ditemukan.")
        return parseDonationRequestJson(array.getJSONObject(0))
    }

    // ========================================================================
    // LAPORAN BUG PELANGGAN
    // ========================================================================
    suspend fun submitBugReport(doc: BugReportDoc): BugReportDoc {
        val res = executeRestWithRetry(
            method = "POST",
            pathAndQuery = "/rest/v1/bug_reports",
            bodyJson = bugReportToInsertJson(doc).toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return if (array.length() > 0) parseBugReportJson(array.getJSONObject(0)) else doc
    }

    suspend fun fetchMyBugReports(uid: String, email: String): List<BugReportDoc> {
        val filter = when {
            uid.isNotBlank() && email.isNotBlank() ->
                "or=(user_uid.eq.${urlEncode(uid)},user_email.eq.${urlEncode(email.trim().lowercase(Locale.ROOT))})"
            uid.isNotBlank() -> "user_uid=eq.${urlEncode(uid)}"
            email.isNotBlank() -> "user_email=eq.${urlEncode(email.trim().lowercase(Locale.ROOT))}"
            else -> return emptyList()
        }
        val res = executeRestWithRetry(
            method = "GET",
            pathAndQuery = "/rest/v1/bug_reports?$filter&select=*&order=created_at.desc&limit=30"
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) add(parseBugReportJson(array.getJSONObject(i)))
        }
    }

    suspend fun getAllBugReports(): List<BugReportDoc> {
        val res = executeRestWithRetry(
            method = "GET",
            pathAndQuery = "/rest/v1/bug_reports?select=*&order=created_at.desc&limit=100"
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) add(parseBugReportJson(array.getJSONObject(i)))
        }
    }

    suspend fun updateBugReportStatus(reportId: String, status: String, adminNotes: String): BugReportDoc {
        val patch = JSONObject().apply {
            put("status", status.trim().uppercase(Locale.ROOT))
            put("admin_notes", adminNotes.trim())
        }
        val res = executeRestWithRetry(
            method = "PATCH",
            pathAndQuery = "/rest/v1/bug_reports?id=eq.${urlEncode(reportId)}",
            bodyJson = patch.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        if (res.code !in 200..299) {
            throw IllegalStateException(translateErrorMessage(res.code, res.body))
        }
        val array = JSONArray(res.body.ifBlank { "[]" })
        if (array.length() == 0) throw IllegalStateException("Laporan bug tidak ditemukan.")
        return parseBugReportJson(array.getJSONObject(0))
    }
}
