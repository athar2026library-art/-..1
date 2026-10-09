package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class AuthRepository(private val appContext: Context) {
    private val auth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "FirebaseAuth init failed", e)
            null
        }
    }
    private val credentialManager = CredentialManager.create(appContext)

    fun getCurrentUser() = auth?.currentUser

    suspend fun signInWithGoogle(activityContext: Context): Boolean {
        val firebaseAuth = auth ?: run {
            Log.e(
                "AuthRepository",
                "FirebaseAuth is unavailable; check app/google-services.json"
            )
            return false
        }

        return try {
            val webClientId =
                appContext.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credential =
                credentialManager.getCredential(
                    activityContext,
                    request
                ).credential

            if (
                credential is CustomCredential &&
                credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                firebaseAuth
                    .signInWithCredential(firebaseCredential)
                    .await()

                true
            } else {
                Log.w(
                    "AuthRepository",
                    "Unexpected credential type: ${credential.type}"
                )
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            false
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthRepository", "Invalid Google ID token", e)
            false
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "FirebaseAuth is not configured", e)
            false
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed", e)
            false
        }
    }

    /** Backward-compatible overload that uses the application context. */
    suspend fun signInWithGoogle(): Boolean = signInWithGoogle(appContext)

    fun signOut() {
        auth?.signOut()
    }
}
