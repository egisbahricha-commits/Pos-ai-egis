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

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_sync_enabled", true)

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
}
