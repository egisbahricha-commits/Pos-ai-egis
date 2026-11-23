package com.example.firebase

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
