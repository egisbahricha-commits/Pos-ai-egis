package com.example.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
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

    suspend fun signInWithGoogle(activityContext: Context): Result<GoogleSignInPayload> = runCatching {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = resolveWebClientId(activityContext)

        val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = credentialManager.getCredential(
            context = activityContext,
            request = request
        )

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInPayload(
                idToken = googleCred.idToken,
                email = googleCred.id,
                displayName = googleCred.displayName ?: googleCred.givenName ?: googleCred.id.substringBefore("@")
            )
        } else {
            throw IllegalStateException("Kredensial Google tidak valid.")
        }
    }.recoverCatching { err ->
        if (err is CancellationException) throw err
        Log.w(TAG, "Interactive Google Sign-In failed", err)
        val friendly = when (err) {
            is GetCredentialCancellationException -> "Login Google dibatalkan oleh pengguna."
            is NoCredentialException -> "Tidak ada akun Google tersedia di perangkat ini. Pastikan Play Services aktif dan akun Google sudah ditambahkan."
            else -> err.localizedMessage ?: "Gagal terhubung ke Google Sign-In."
        }
        throw IllegalStateException(friendly, err)
    }

    suspend fun trySilentGoogleSignIn(activityContext: Context): GoogleSignInPayload? {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val webClientId = resolveWebClientId(activityContext)

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
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInPayload(
                    idToken = googleCred.idToken,
                    email = googleCred.id,
                    displayName = googleCred.displayName ?: googleCred.givenName ?: googleCred.id.substringBefore("@")
                )
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCredentialState(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun resolveWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        val clientId = if (resId != 0) context.getString(resId).trim() else ""
        return clientId.ifBlank {
            // Fallback generated client id placeholder to prevent crash when google-services.json isn't bound
            "70fdbae7-61cc-45ca-85cd-50c11d146935.apps.googleusercontent.com"
        }
    }
}
