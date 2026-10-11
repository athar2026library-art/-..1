package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * يحمل applicationContext فقط (لا Activity) حتى لا يسرّب الـ Activity داخل الـ ViewModel.
 * شاشة الدخول تحتاج Activity context، لذلك يُمرَّر عند الاستدعاء.
 */
class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val auth by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    private val credentialManager = CredentialManager.create(appContext)

    fun getCurrentUser() = auth?.currentUser

    suspend fun signInWithGoogle(activityContext: Context): Boolean {
        val firebaseAuth = auth ?: return false
        return try {
            val webClientId = appContext.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credential = credentialManager.getCredential(activityContext, request).credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                true
            } else {
                Log.w("AuthRepository", "Unexpected credential type: ${credential.type}")
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            false
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthRepository", "Invalid Google ID token", e)
            false
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed", e)
            false
        }
    }

    suspend fun signOut() {
        auth?.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
