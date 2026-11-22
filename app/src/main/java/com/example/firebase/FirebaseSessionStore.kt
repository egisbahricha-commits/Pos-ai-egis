package com.example.firebase

import android.content.Context
import android.os.Build
import java.util.UUID

class FirebaseSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gerai_una_firebase_prefs", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString("device_id", null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "dev_" + UUID.randomUUID().toString().replace("-", "").take(10)
        prefs.edit().putString("device_id", generated).apply()
        return generated
    }

    fun getDeviceName(): String {
        val model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Perangkat Android"
        return prefs.getString("device_name", model) ?: model
    }

    fun setOfflineBypass(enabled: Boolean) {
        prefs.edit().putBoolean("offline_bypass", enabled).apply()
    }

    fun isOfflineBypass(): Boolean = prefs.getBoolean("offline_bypass", false)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_sync_enabled", false)

    fun setLastSyncAt(timestamp: Long) {
        prefs.edit().putLong("last_sync_at", timestamp).apply()
    }

    fun getLastSyncAt(): Long = prefs.getLong("last_sync_at", 0L)

    fun setHasCloudSyncedBefore(synced: Boolean) {
        prefs.edit().putBoolean("has_cloud_synced_before", synced).apply()
    }

    fun hasCloudSyncedBefore(): Boolean = prefs.getBoolean("has_cloud_synced_before", false)

    fun setLastLoginEmail(email: String) {
        prefs.edit().putString("last_login_email", email).apply()
    }

    fun getLastLoginEmail(): String = prefs.getString("last_login_email", "") ?: ""

    fun setStoreNameHint(storeName: String) {
        prefs.edit().putString("store_name_hint", storeName).apply()
    }

    fun getStoreNameHint(): String = prefs.getString("store_name_hint", "Toko Saya") ?: "Toko Saya"

    fun saveLicenseSnapshot(snapshot: LocalLicenseSnapshot) {
        val now = System.currentTimeMillis()
        val oldMax = getMaxSeenClockMillis()
        val newMax = maxOf(oldMax, now, snapshot.maxSeenClockMillis)
        prefs.edit()
            .putString("lic_id", snapshot.licenseId)
            .putString("lic_key", snapshot.licenseKey)
            .putString("lic_owner_uid", snapshot.ownerUid)
            .putString("lic_owner_email", snapshot.ownerEmail)
            .putString("lic_owner_name", snapshot.ownerName)
            .putString("lic_store_name", snapshot.storeName)
            .putString("lic_phone", snapshot.phone)
            .putString("lic_device_id", snapshot.deviceId)
            .putString("lic_plan_type", snapshot.planType)
            .putString("lic_status", snapshot.status)
            .putLong("lic_demo_start", snapshot.demoStartedAtMillis)
            .putLong("lic_demo_exp", snapshot.demoExpiresAtMillis)
            .putLong("lic_activated_at", snapshot.activatedAtMillis)
            .putLong("lic_expires_at", snapshot.expiresAtMillis)
            .putLong("lic_last_verified", snapshot.lastVerifiedAtMillis)
            .putLong("lic_max_clock", newMax)
            .apply()
    }

    fun saveFromLicenseDoc(doc: LicenseDoc, nowMillis: Long = System.currentTimeMillis()): LocalLicenseSnapshot {
        val snap = LocalLicenseSnapshot(
            licenseId = doc.id.ifBlank { doc.licenseKey },
            licenseKey = doc.licenseKey.ifBlank { doc.id },
            ownerUid = doc.ownerUid,
            ownerEmail = doc.ownerEmail,
            ownerName = doc.ownerName,
            storeName = doc.storeName,
            phone = doc.phone,
            deviceId = doc.deviceId,
            planType = doc.planType.ifBlank { if (doc.status == "ACTIVE") "FULL" else "DEMO" },
            status = doc.status.ifBlank { "DEMO" },
            demoStartedAtMillis = doc.demoStartedAtMillis,
            demoExpiresAtMillis = doc.demoExpiresAtMillis,
            activatedAtMillis = doc.activatedAtMillis,
            expiresAtMillis = doc.expiresAtMillis,
            lastVerifiedAtMillis = nowMillis,
            maxSeenClockMillis = maxOf(getMaxSeenClockMillis(), nowMillis)
        )
        saveLicenseSnapshot(snap)
        return snap
    }

    fun getLicenseSnapshot(): LocalLicenseSnapshot? {
        val key = prefs.getString("lic_key", null).orEmpty()
        val id = prefs.getString("lic_id", null).orEmpty()
        val email = prefs.getString("lic_owner_email", null).orEmpty()
        if (key.isBlank() && id.isBlank() && email.isBlank()) return null
        return LocalLicenseSnapshot(
            licenseId = id.ifBlank { key },
            licenseKey = key.ifBlank { id },
            ownerUid = prefs.getString("lic_owner_uid", "") ?: "",
            ownerEmail = email,
            ownerName = prefs.getString("lic_owner_name", "") ?: "",
            storeName = prefs.getString("lic_store_name", "") ?: "",
            phone = prefs.getString("lic_phone", "") ?: "",
            deviceId = prefs.getString("lic_device_id", "") ?: "",
            planType = prefs.getString("lic_plan_type", "DEMO") ?: "DEMO",
            status = prefs.getString("lic_status", "DEMO") ?: "DEMO",
            demoStartedAtMillis = prefs.getLong("lic_demo_start", 0L),
            demoExpiresAtMillis = prefs.getLong("lic_demo_exp", 0L),
            activatedAtMillis = prefs.getLong("lic_activated_at", 0L),
            expiresAtMillis = prefs.getLong("lic_expires_at", 0L),
            lastVerifiedAtMillis = prefs.getLong("lic_last_verified", 0L),
            maxSeenClockMillis = prefs.getLong("lic_max_clock", 0L)
        )
    }

    fun updateMaxSeenClockMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val updated = maxOf(getMaxSeenClockMillis(), nowMillis)
        prefs.edit().putLong("lic_max_clock", updated).apply()
        return updated
    }

    fun getMaxSeenClockMillis(): Long = prefs.getLong("lic_max_clock", 0L)

    fun clearLicenseSnapshot() {
        prefs.edit()
            .remove("lic_id")
            .remove("lic_key")
            .remove("lic_owner_uid")
            .remove("lic_owner_email")
            .remove("lic_owner_name")
            .remove("lic_store_name")
            .remove("lic_phone")
            .remove("lic_device_id")
            .remove("lic_plan_type")
            .remove("lic_status")
            .remove("lic_demo_start")
            .remove("lic_demo_exp")
            .remove("lic_activated_at")
            .remove("lic_expires_at")
            .remove("lic_last_verified")
            .remove("lic_max_clock")
            .apply()
    }
}
