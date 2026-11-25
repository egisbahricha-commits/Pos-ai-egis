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

enum class AdminLicensePlanOption(
    val id: String,
    val label: String,
    val shortLabel: String,
    val planType: String,
    val durationDays: Int
) {
    MONTHLY_30(
        id = "MONTHLY_30",
        label = "Lisensi 1 Bulan (30 Hari)",
        shortLabel = "1 Bulan",
        planType = "FULL",
        durationDays = 30
    ),
    YEARLY_365(
        id = "YEARLY_365",
        label = "Lisensi 1 Tahun (365 Hari)",
        shortLabel = "1 Tahun",
        planType = "FULL",
        durationDays = 365
    ),
    LIFETIME(
        id = "LIFETIME",
        label = "Lisensi Selamanya (Permanen)",
        shortLabel = "Selamanya",
        planType = "FULL",
        durationDays = 0
    ),
    DEMO_3(
        id = "DEMO_3",
        label = "Demo Trial (3 Hari)",
        shortLabel = "Demo 3 Hr",
        planType = "DEMO",
        durationDays = 3
    );

    companion object {
        fun fromDuration(planType: String, durationDays: Int): AdminLicensePlanOption = when {
            planType.equals("DEMO", ignoreCase = true) -> DEMO_3
            durationDays == 30 -> MONTHLY_30
            durationDays == 365 -> YEARLY_365
            else -> LIFETIME
        }
    }
}

data class FreeLifetimePromoStatus(
    val quota: Int = 0,
    val used: Int = 0,
    val remaining: Int = 0,
    val yearlyQuota: Int = 0,
    val yearlyRemaining: Int = 0
) {
    val isLifetimeActiveWithSlots: Boolean
        get() = quota > 0 && remaining > 0

    val isYearlyActiveWithSlots: Boolean
        get() = yearlyQuota > 0 && yearlyRemaining > 0

    val isActiveWithSlots: Boolean
        get() = isLifetimeActiveWithSlots || isYearlyActiveWithSlots
}

data class DeveloperBankConfig(
    val id: String = "payment_config",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val whatsappNumber: String = "",
    val supportEmail: String = "egisbahricha@gmail.com",
    val notes: String = "Cantumkan email akun KASLITE atau nama toko pada berita transfer agar verifikasi lebih cepat.",
    val freeLifetimeQuota: Int = 0,
    val freeYearlyQuota: Int = 0,
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

    fun buildPromoAndLicenseSqlSnippet(): String = """
-- Jalankan SQL ini di Supabase Dashboard -> SQL Editor -> New Query
-- Menambah kuota Promo Gratis Selamanya & Promo Gratis 1 Tahun secara aman di server Supabase.

alter table public.developer_settings
  add column if not exists free_lifetime_quota integer not null default 0;

alter table public.developer_settings
  add column if not exists free_yearly_quota integer not null default 0;

create or replace function public.kaslite_get_promo_status()
returns jsonb
language plpgsql
security definer
set search_path = public
as ${'$'}${'$'}
declare
  v_lifetime_quota integer := 0;
  v_yearly_quota integer := 0;
  v_used integer := 0;
  v_lifetime_remaining integer := 0;
  v_yearly_remaining integer := 0;
begin
  select coalesce(free_lifetime_quota, 0),
         coalesce(free_yearly_quota, 0)
    into v_lifetime_quota, v_yearly_quota
    from public.developer_settings
   where id = 'payment_config'
   limit 1;

  v_lifetime_quota := greatest(0, coalesce(v_lifetime_quota, 0));
  v_yearly_quota := greatest(0, coalesce(v_yearly_quota, 0));

  select count(*)::integer
    into v_used
    from public.licenses
   where lower(coalesce(owner_email, '')) <> 'egisbahricha@gmail.com';

  v_lifetime_remaining := greatest(0, v_lifetime_quota - v_used);
  v_yearly_remaining := greatest(0, (v_lifetime_quota + v_yearly_quota) - greatest(v_used, v_lifetime_quota));

  return jsonb_build_object(
    'quota', v_lifetime_quota,
    'used', v_used,
    'remaining', v_lifetime_remaining,
    'yearly_quota', v_yearly_quota,
    'yearly_remaining', v_yearly_remaining
  );
end;
${'$'}${'$'};

revoke all on function public.kaslite_get_promo_status() from public;
grant execute on function public.kaslite_get_promo_status() to anon, authenticated;

create or replace function public.kaslite_register_customer_license(
  p_license_key text,
  p_store_name text,
  p_owner_name text,
  p_phone text,
  p_device_id text
)
returns setof public.licenses
language plpgsql
security definer
set search_path = public
as ${'$'}${'$'}
declare
  v_uid text := coalesce(auth.uid()::text, '');
  v_email text := lower(trim(coalesce(auth.jwt() ->> 'email', '')));
  v_existing public.licenses%rowtype;
  v_inserted public.licenses%rowtype;
  v_lifetime_quota integer := 0;
  v_yearly_quota integer := 0;
  v_used integer := 0;
  v_now_millis bigint := (extract(epoch from now()) * 1000)::bigint;
  v_demo_end_millis bigint := ((extract(epoch from now()) * 1000)::bigint) + 259200000;
  v_yearly_end_millis bigint := ((extract(epoch from now()) * 1000)::bigint) + 31536000000;
  v_promo_tier text := 'NONE';
  v_clean_key text := nullif(trim(coalesce(p_license_key, '')), '');
begin
  if v_uid = '' or v_email = '' then
    raise exception 'Sesi login Supabase diperlukan untuk registrasi lisensi.';
  end if;

  perform pg_advisory_xact_lock(hashtext('kaslite_free_lifetime_promo_lock'));

  select *
    into v_existing
    from public.licenses
   where owner_uid = v_uid
      or (lower(owner_email) = v_email and (coalesce(owner_uid, '') = '' and plan_type = 'DEMO'))
   order by
     case when plan_type = 'FULL' or status = 'ACTIVE' then 0 else 1 end,
     updated_at desc,
     created_at desc
   limit 1;

  if found then
    if coalesce(v_existing.device_id, '') <> ''
       and coalesce(trim(p_device_id), '') <> ''
       and v_existing.device_id <> trim(p_device_id) then
      return next v_existing;
      return;
    end if;

    if coalesce(v_existing.owner_uid, '') = '' or coalesce(v_existing.device_id, '') = '' then
      update public.licenses
         set owner_uid = case when coalesce(owner_uid, '') = '' then v_uid else owner_uid end,
             owner_email = case when coalesce(owner_email, '') = '' then v_email else owner_email end,
             owner_name = case when coalesce(owner_name, '') = '' and trim(coalesce(p_owner_name, '')) <> '' then trim(p_owner_name) else owner_name end,
             store_name = case when coalesce(store_name, '') = '' and trim(coalesce(p_store_name, '')) <> '' then trim(p_store_name) else store_name end,
             phone = case when coalesce(phone, '') = '' and trim(coalesce(p_phone, '')) <> '' then trim(p_phone) else phone end,
             device_id = case when coalesce(device_id, '') = '' and trim(coalesce(p_device_id, '')) <> '' then trim(p_device_id) else device_id end,
             updated_at = now()
       where id = v_existing.id
       returning * into v_existing;
    end if;
    return next v_existing;
    return;
  end if;

  if v_clean_key is null then
    v_clean_key := 'KASLITE-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
  end if;

  select coalesce(free_lifetime_quota, 0),
         coalesce(free_yearly_quota, 0)
    into v_lifetime_quota, v_yearly_quota
    from public.developer_settings
   where id = 'payment_config'
   limit 1;

  v_lifetime_quota := greatest(0, coalesce(v_lifetime_quota, 0));
  v_yearly_quota := greatest(0, coalesce(v_yearly_quota, 0));

  if (v_lifetime_quota > 0 or v_yearly_quota > 0) and v_email <> 'egisbahricha@gmail.com' then
    select count(*)::integer
      into v_used
      from public.licenses
     where lower(coalesce(owner_email, '')) <> 'egisbahricha@gmail.com';
    if v_lifetime_quota > 0 and v_used < v_lifetime_quota then
      v_promo_tier := 'LIFETIME';
    elsif v_yearly_quota > 0 and v_used < (v_lifetime_quota + v_yearly_quota) then
      v_promo_tier := 'YEARLY';
    end if;
  end if;

  insert into public.licenses (
    license_key, store_name, owner_name, owner_email, owner_uid, phone, device_id,
    plan_type, status, demo_started_at_millis, demo_expires_at_millis, activated_at_millis, expires_at_millis
  ) values (
    v_clean_key, trim(coalesce(p_store_name, '')), trim(coalesce(p_owner_name, '')),
    v_email, v_uid, trim(coalesce(p_phone, '')), trim(coalesce(p_device_id, '')),
    case when v_promo_tier <> 'NONE' then 'FULL' else 'DEMO' end,
    case when v_promo_tier <> 'NONE' then 'ACTIVE' else 'DEMO' end,
    case when v_promo_tier <> 'NONE' then 0 else v_now_millis end,
    case when v_promo_tier <> 'NONE' then 0 else v_demo_end_millis end,
    case when v_promo_tier <> 'NONE' then v_now_millis else 0 end,
    case when v_promo_tier = 'YEARLY' then v_yearly_end_millis else 0 end
  )
  returning * into v_inserted;

  return next v_inserted;
  return;
end;
${'$'}${'$'};

revoke all on function public.kaslite_register_customer_license(text, text, text, text, text) from public, anon;
grant execute on function public.kaslite_register_customer_license(text, text, text, text, text) to authenticated;
    """.trimIndent()
}
