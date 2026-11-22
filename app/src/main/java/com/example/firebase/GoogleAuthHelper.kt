package com.example.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

data class GoogleSignInPayload(
    val idToken: String,
    val email: String,
    val displayName: String
)

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    suspend fun signInWithGoogle(context: Context): Result<GoogleSignInPayload> {
        val activityContext: Context = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = resolveWebClientId(activityContext)

        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalStateException("Konfigurasi Web Client ID Firebase tidak ditemukan di aplikasi.")
            )
        }

        // Attempt 1: Interactive Google Sign-In sheet (strictly 1 CredentialOption per request)
        val startedAt = System.currentTimeMillis()
        try {
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            return Result.success(extractPayload(result.credential))
        } catch (e: CancellationException) {
            throw e
        } catch (err: Exception) {
            val elapsedMs = System.currentTimeMillis() - startedAt
            Log.w(TAG, "GetSignInWithGoogleOption failed in ${elapsedMs}ms (${err::class.java.simpleName})", err)

            // Fallback via separate single-option GetGoogleIdOption if no cached credential or Play Services aborted immediately (< 450ms)
            val shouldFallback = err is NoCredentialException ||
                (err is GetCredentialCancellationException && elapsedMs < 450L) ||
                (err is GetCredentialException && err !is GetCredentialCancellationException)

            if (!shouldFallback) {
                return Result.failure(IllegalStateException(mapFriendlyError(err), err))
            }

            return runCatching {
                val idOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(idOption)
                    .build()
                val fallbackResult = credentialManager.getCredential(
                    context = activityContext,
                    request = fallbackRequest
                )
                extractPayload(fallbackResult.credential)
            }.recoverCatching { fallbackErr ->
                if (fallbackErr is CancellationException) throw fallbackErr
                Log.w(TAG, "Fallback GetGoogleIdOption failed (${fallbackErr::class.java.simpleName})", fallbackErr)
                throw IllegalStateException(mapFriendlyError(fallbackErr), fallbackErr)
            }
        }
    }

    suspend fun trySilentGoogleSignIn(context: Context): GoogleSignInPayload? {
        return try {
            val activityContext: Context = context.findActivity() ?: context
            val webClientId = resolveWebClientId(activityContext)
            if (webClientId.isBlank()) return null

            val credentialManager = CredentialManager.create(activityContext)
            val autoOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(autoOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                extractPayload(credential)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCredentialState(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context.applicationContext)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun extractPayload(credential: androidx.credentials.Credential): GoogleSignInPayload {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val cleanEmail = googleCred.id.trim()
            return GoogleSignInPayload(
                idToken = googleCred.idToken,
                email = cleanEmail,
                displayName = googleCred.displayName?.takeIf { it.isNotBlank() }
                    ?: googleCred.givenName?.takeIf { it.isNotBlank() }
                    ?: cleanEmail.substringBefore("@").ifBlank { "Pemilik Toko" }
            )
        }
        throw IllegalStateException("Kredensial Google tidak valid.")
    }

    private fun mapFriendlyError(err: Throwable): String {
        return when (err) {
            is GetCredentialCancellationException ->
                "Pemilihan akun Google ditutup atau dibatalkan. Pastikan akun Google aktif di HP Anda dan pilih email saat pop-up muncul."
            is NoCredentialException ->
                "Tidak ditemukan akun Google aktif di perangkat. Tambahkan akun Google di Pengaturan HP lalu pastikan Google Play Services & internet aktif."
            else ->
                err.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Gagal terhubung ke Google Sign-In. Periksa koneksi internet dan Google Play Services."
        }
    }

    private fun resolveWebClientId(context: Context): String {
        return runCatching {
            context.applicationContext.getString(R.string.default_web_client_id).trim()
        }.getOrDefault("")
    }
}
