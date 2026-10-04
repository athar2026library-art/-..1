package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Credential Manager يحتاج Activity لواجهة تسجيل الدخول.
 * لا نخزّن Activity كحقل — يُمرَّر عند كل استدعاء.
 */
class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val auth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }
    private val credentialManager = CredentialManager.create(appContext)

    fun getCurrentUser() = auth?.currentUser

    suspend fun signInWithGoogle(activity: Activity): Boolean {
        if (auth == null) return false
        return try {
            val webClientId = appContext.getString(com.example.R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val cred = result.credential

            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
                auth?.signInWithCredential(firebaseCredential)?.await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed", e)
            false
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}
