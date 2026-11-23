package com.example.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.firebase.FirebaseSessionStore
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
                jsonMsg.isNotBlank() -> jsonMsg
                else -> "Permintaan Supabase gagal (HTTP $statusCode)."
            }
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
            sessionStore.clearSupabaseSession()
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

    suspend fun fetchLicenseByOwnerUid(uid: String): LicenseDoc? {
        if (uid.isBlank()) return null
        return queryLicenseList("owner_uid=eq.${urlEncode(uid)}&select=*&limit=1").firstOrNull()
    }

    suspend fun fetchLicenseByEmail(email: String): LicenseDoc? {
        val clean = email.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return null
        val exact = queryLicenseList("owner_email=eq.${urlEncode(clean)}&select=*&limit=5")
            .firstOrNull { it.ownerEmail.equals(clean, ignoreCase = true) }
        if (exact != null) return exact
        return queryLicenseList("owner_email=ilike.${urlEncode(clean)}&select=*&limit=5")
            .firstOrNull { it.ownerEmail.equals(clean, ignoreCase = true) }
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
        val cleanEmail = ownerEmail.trim().lowercase(Locale.ROOT)
        val existing = fetchLicenseByOwnerUid(ownerUid) ?: fetchLicenseByEmail(cleanEmail)
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
        val generatedKey = generateLicenseKey()
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
            throw IllegalStateException("Lisensi \"$cleanKey\" sedang dinonaktifkan (SUSPENDED).")
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
        durationDays: Int = 3650
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
            expiresAtMillis = if (isFull && durationDays > 0) now + durationDays * ONE_DAY_MILLIS else 0L
        )
        return insertLicenseRow(doc)
    }

    suspend fun superAdminActivateFull(licenseKey: String, durationDays: Int = 3650): LicenseDoc {
        val now = System.currentTimeMillis()
        val exp = if (durationDays > 0) now + durationDays * ONE_DAY_MILLIS else 0L
        val patch = JSONObject().apply {
            put("plan_type", "FULL")
            put("status", "ACTIVE")
            put("activated_at_millis", now)
            put("expires_at_millis", exp)
        }
        return patchLicenseByQuery("license_key=eq.${urlEncode(licenseKey.trim())}", patch)
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
    }

    suspend fun superAdminExtendDemo(licenseKey: String, days: Int = 3): LicenseDoc {
        val current = getLicenseByKey(licenseKey.trim())
            ?: throw IllegalStateException("Lisensi $licenseKey tidak ditemukan.")
        val now = System.currentTimeMillis()
        val base = maxOf(now, current.demoExpiresAtMillis)
        val newDemoExp = base + days * ONE_DAY_MILLIS
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
}
