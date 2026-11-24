package com.example.firebase

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncMode {
    PUSH_ONLY,
    PULL_ONLY,
    TWO_WAY
}

data class CloudSyncSummary(
    val pushedRecords: Int,
    val pulledRecords: Int,
    val timestamp: Long,
    val message: String
)

enum class LicenseAccessStatus {
    UNREGISTERED,
    DEMO_ACTIVE,
    DEMO_EXPIRED,
    FULL_ACTIVE,
    SUSPENDED,
    DEVICE_MISMATCH
}

data class LocalLicenseSnapshot(
    val licenseId: String = "",
    val licenseKey: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val ownerName: String = "",
    val storeName: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val planType: String = "DEMO",
    val status: String = "DEMO",
    val demoStartedAtMillis: Long = 0L,
    val demoExpiresAtMillis: Long = 0L,
    val activatedAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val lastVerifiedAtMillis: Long = 0L,
    val maxSeenClockMillis: Long = 0L
)

data class LicenseUiState(
    val accessStatus: LicenseAccessStatus = LicenseAccessStatus.UNREGISTERED,
    val licenseKey: String = "",
    val ownerEmail: String = "",
    val storeName: String = "",
    val planType: String = "DEMO",
    val statusRaw: String = "DEMO",
    val demoStartedAtMillis: Long = 0L,
    val demoExpiresAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val lastVerifiedAtMillis: Long = 0L,
    val isRefreshing: Boolean = false,
    val message: String? = null
) {
    val isLockedToProductOnly: Boolean
        get() = accessStatus == LicenseAccessStatus.DEMO_EXPIRED ||
            accessStatus == LicenseAccessStatus.SUSPENDED ||
            accessStatus == LicenseAccessStatus.DEVICE_MISMATCH

    val isFullActive: Boolean
        get() = accessStatus == LicenseAccessStatus.FULL_ACTIVE

    val isDemoActive: Boolean
        get() = accessStatus == LicenseAccessStatus.DEMO_ACTIVE
}

data class FirebaseAuthUiState(
    val hasEnteredApp: Boolean = false,
    val isCloudSignedIn: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isSuperAdmin: Boolean = false,
    val userId: String = "",
    val userEmail: String = "",
    val fullName: String = "",
    val storeName: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val lastEmail: String = "",
    val verifiedGoogleUid: String = "",
    val verifiedGoogleEmail: String = "",
    val verifiedGoogleName: String = "",
    val isAuthLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val authError: String? = null,
    val authInfoMessage: String? = null,
    val autoSyncEnabled: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncMessage: String = ""
)

data class LicenseDoc(
    val id: String = "",
    val licenseKey: String = "",
    val storeName: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val ownerUid: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val planType: String = "DEMO", // "DEMO" | "FULL"
    val status: String = "DEMO", // "DEMO" | "ACTIVE" | "EXPIRED" | "SUSPENDED"
    val demoStartedAtMillis: Long = 0L,
    val demoExpiresAtMillis: Long = 0L,
    val activatedAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

enum class DonationPackage(
    val id: String,
    val title: String,
    val priceLabel: String,
    val amountRupiah: Long,
    val durationDays: Int,
    val badge: String,
    val summary: String,
    val features: List<String>
) {
    MONTHLY(
        id = "MONTHLY",
        title = "Donasi Bulanan",
        priceLabel = "Rp 10.000 / bulan",
        amountRupiah = 10_000L,
        durationDays = 30,
        badge = "Fleksibel 30 Hari",
        summary = "Dukungan pengembangan bulanan agar KASLITE terus terawat, mendapatkan kunci akses penuh 30 hari.",
        features = listOf(
            "Kasir POS lengkap multi-pembayaran (Tunai, QRIS, Transfer, Kasbon) + cetak struk thermal Bluetooth/USB/PDF",
            "Scan Barcode super cepat via Kamera & Alat Scanner eksternal langsung masuk keranjang",
            "Multi Kas Toko, Saldo Bank, Konter Pulsa & Tabel Tarif Admin PPOB bertingkat",
            "Manajemen Stok & Inventaris lengkap: stok opname, grosir bertingkat, varian & paket bundling",
            "24 Laporan Analitik + Laba Rugi harian/mingguan/bulanan/tahunan + Export Excel (.xlsx)",
            "Backup & Restore penuh (.kaslite) beserta seluruh foto produk"
        )
    ),
    YEARLY(
        id = "YEARLY",
        title = "Donasi Tahunan",
        priceLabel = "Rp 50.000 / tahun",
        amountRupiah = 50_000L,
        durationDays = 365,
        badge = "Hemat 58%",
        summary = "Kontribusi 1 tahun penuh — lebih hemat dibanding bulanan, membuka seluruh fitur toko selama 365 hari.",
        features = listOf(
            "Seluruh fitur unggulan Paket Donasi Bulanan terbuka penuh 365 hari",
            "Kelola Utang & Piutang pelanggan + rekap jatuh tempo & pengingat WhatsApp",
            "Asisten AI Chat KASLITE untuk pencatatan transaksi, stok, & tanya laporan secara cepat",
            "Manajemen multi-user (Owner & Kasir dengan PIN 6 digit & izin menu per layar)",
            "Pembaruan fitur rutin & verifikasi kunci lisensi online selama 1 tahun penuh",
            "Dukungan pemulihan device & bantuan admin pusat via email"
        )
    ),
    LIFETIME(
        id = "LIFETIME",
        title = "Donasi Selamanya",
        priceLabel = "Rp 250.000 Selamanya",
        amountRupiah = 250_000L,
        durationDays = 0,
        badge = "Selamanya • Prioritas",
        summary = "Kontribusi satu kali seumur hidup — dukungan penuh bagi pengembang untuk akses permanen tanpa perpanjangan.",
        features = listOf(
            "Seluruh fitur unggulan KASLITE aktif PERMANEN selamanya tanpa batas masa berlaku",
            "Bebas akses seluruh modul Kasir, Multi-Kas, PPOB, Laba Rugi, Excel, & Backup .kaslite",
            "Tanpa donasi ulang bulanan/tahunan di perangkat terdaftar",
            "Prioritas penanganan Menu Laporan Bug & permintaan perbaikan fitur",
            "Dukungan langsung pengembang saat ganti perangkat (Reset Device) & migrasi data",
            "Tanda apresiasi penuh untuk keberlanjutan pengembangan KASLITE jangka panjang"
        )
    );

    companion object {
        fun fromId(raw: String?): DonationPackage {
            val upper = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.id == upper } ?: MONTHLY
        }
    }
}

data class DeveloperBankConfig(
    val id: String = "payment_config",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val whatsappNumber: String = "",
    val supportEmail: String = "egisbahricha@gmail.com",
    val notes: String = "Cantumkan email akun KASLITE atau nama toko pada berita transfer agar verifikasi lebih cepat.",
    val updatedAtMillis: Long = 0L
) {
    val isConfigured: Boolean
        get() = bankName.isNotBlank() && accountNumber.isNotBlank() && accountHolder.isNotBlank()
}

data class DonationRequestDoc(
    val id: String = "",
    val userUid: String = "",
    val userEmail: String = "",
    val ownerName: String = "",
    val storeName: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val packageId: String = DonationPackage.MONTHLY.id,
    val packageTitle: String = DonationPackage.MONTHLY.title,
    val amountRupiah: Long = DonationPackage.MONTHLY.amountRupiah,
    val durationDays: Int = DonationPackage.MONTHLY.durationDays,
    val senderBankName: String = "",
    val senderAccountName: String = "",
    val transferNote: String = "",
    val status: String = "PENDING", // "PENDING", "PAID", "REJECTED"
    val licenseKey: String = "",
    val adminNotes: String = "",
    val approvedAtMillis: Long = 0L,
    val emailSentAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class DonationApprovalOutcome(
    val donation: DonationRequestDoc,
    val license: LicenseDoc,
    val emailDispatchedAutomatically: Boolean,
    val emailDispatchMessage: String
)

data class BugReportDoc(
    val id: String = "",
    val userUid: String = "",
    val userEmail: String = "",
    val reporterName: String = "",
    val storeName: String = "",
    val deviceId: String = "",
    val category: String = "Kasir POS",
    val severity: String = "SEDANG", // "RENDAH", "SEDANG", "TINGGI", "KRITIS"
    val title: String = "",
    val description: String = "",
    val appVersion: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val status: String = "BARU", // "BARU", "DIPROSES", "SELESAI", "DITOLAK"
    val adminNotes: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

object KasLiteSupportEmailHelper {
    val BUG_CATEGORIES = listOf(
        "Kasir POS",
        "Scan Barcode",
        "Printer Thermal",
        "Produk & Stok",
        "Keuangan & Laporan",
        "Lisensi & Akun",
        "Lainnya"
    )

    val BUG_SEVERITIES = listOf(
        "RENDAH" to "Rendah",
        "SEDANG" to "Sedang",
        "TINGGI" to "Tinggi",
        "KRITIS" to "Kritis"
    )

    fun formatIdr(amount: Long): String {
        val nf = NumberFormat.getIntegerInstance(Locale("id", "ID"))
        return "Rp ${nf.format(amount)}"
    }

    fun buildLicenseEmailSubject(storeName: String, licenseKey: String): String {
        val toko = storeName.trim().ifBlank { "Mitra KASLITE" }
        return "Kunci Lisensi KASLITE Penuh ($licenseKey) - $toko"
    }

    fun buildLicenseEmailBody(
        ownerName: String,
        storeName: String,
        recipientEmail: String,
        licenseKey: String,
        packageTitle: String,
        amountRupiah: Long,
        expiresAtMillis: Long,
        supportEmail: String = "egisbahricha@gmail.com"
    ): String {
        val greetingName = ownerName.trim().ifBlank { "Kak" }
        val toko = storeName.trim().ifBlank { "Toko Anda" }
        val expiryLabel = if (expiresAtMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(expiresAtMillis))
        } else {
            "PERMANEN (Selamanya)"
        }
        return buildString {
            appendLine("Halo $greetingName,")
            appendLine()
            appendLine("Terima kasih sebesar-besarnya atas donasi dukungan Anda untuk pengembang KASLITE!")
            appendLine("Donasi untuk toko \"$toko\" telah kami verifikasi sebagai SUDAH DIBAYAR.")
            appendLine()
            appendLine("=== DETAIL KUNCI LISENSI KASLITE ===")
            appendLine("Email Terdaftar : $recipientEmail")
            appendLine("Paket Dukungan  : $packageTitle (${formatIdr(amountRupiah)})")
            appendLine("Kunci Lisensi   : $licenseKey")
            appendLine("Masa Aktif      : $expiryLabel")
            appendLine()
            appendLine("Cara Mengaktifkan di Aplikasi KASLITE:")
            appendLine("1. Buka aplikasi KASLITE di perangkat Anda.")
            appendLine("2. Masuk ke menu Pengaturan -> Lisensi & Masa Aktif KASLITE -> Aktivasi Kunci Lisensi.")
            appendLine("3. Ketik atau tempel kunci lisensi di atas: $licenseKey")
            appendLine("4. Pastikan internet aktif dan gunakan email $recipientEmail, lalu tekan Aktifkan.")
            appendLine()
            appendLine("Jika mengalami kendala aktivasi atau penggantian perangkat, balas email ini ke $supportEmail.")
            appendLine()
            appendLine("Salam hangat,")
            appendLine("Pengembang KASLITE POS")
        }
    }

    fun buildBugReportEmailSubject(category: String, title: String): String =
        "[Laporan Bug KASLITE - $category] $title"

    fun buildBugReportEmailBody(report: BugReportDoc): String = buildString {
        appendLine("=== LAPORAN BUG APLIKASI KASLITE ===")
        appendLine("Kategori     : ${report.category}")
        appendLine("Prioritas    : ${report.severity}")
        appendLine("Judul        : ${report.title}")
        appendLine()
        appendLine("Deskripsi / Kronologi:")
        appendLine(report.description)
        appendLine()
        appendLine("=== INFO DIAGNOSTIK ===")
        appendLine("Pelapor      : ${report.reporterName.ifBlank { "-" }} (${report.userEmail.ifBlank { "-" }})")
        appendLine("Nama Toko    : ${report.storeName.ifBlank { "-" }}")
        appendLine("Versi App    : ${report.appVersion.ifBlank { "-" }}")
        appendLine("Perangkat    : ${report.deviceModel.ifBlank { "-" }}")
        appendLine("Android OS   : ${report.androidVersion.ifBlank { "-" }}")
        appendLine("Device ID    : ${report.deviceId.ifBlank { "-" }}")
    }

    fun buildResendEdgeFunctionSnippet(): String = """
// Opsional: Deploy Edge Function 'send-license-email' di Supabase agar tombol
// 'Sudah Dibayar & Kirim Lisensi' langsung mengirim email otomatis tanpa membuka Gmail HP.
// Secrets Supabase: RESEND_API_KEY, RESEND_FROM_EMAIL
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

serve(async (req) => {
  const { to, ownerName, storeName, licenseKey, packageTitle, expiresLabel } = await req.json();
  const apiKey = Deno.env.get("RESEND_API_KEY");
  const fromEmail = Deno.env.get("RESEND_FROM_EMAIL") ?? "KASLITE <noreply@domainanda.com>";
  if (!apiKey) return new Response(JSON.stringify({ error: "RESEND_API_KEY missing" }), { status: 500 });

  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { "Authorization": "Bearer " + apiKey, "Content-Type": "application/json" },
    body: JSON.stringify({
      from: fromEmail,
      to: [to],
      subject: "Kunci Lisensi KASLITE (" + licenseKey + ") - " + (storeName || "Toko"),
      text: "Halo " + (ownerName || "Mitra") + ",\n\nTerima kasih atas donasi Anda (" + packageTitle + ").\nKunci Lisensi Anda: " + licenseKey + "\nMasa Aktif: " + (expiresLabel || "Permanen") + "\n\nMasukkan kode ini di menu Pengaturan -> Aktivasi Kunci Lisensi."
    })
  });
  const data = await res.text();
  return new Response(data, { status: res.status, headers: { "Content-Type": "application/json" } });
});
    """.trimIndent()
}
