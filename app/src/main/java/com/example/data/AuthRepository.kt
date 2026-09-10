package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    fun getCurrentUser() = auth.currentUser

    suspend fun signInWithGoogle(): Boolean {
        return try {
            // NOTE: In a real app, replace with actual Web Client ID from Firebase Console.
            // Using a dummy string will fail the actual runtime auth call, but fulfills the code structure.
            val webClientId = "1234567890-abcdefg.apps.googleusercontent.com" 
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is GoogleIdTokenCredential) {
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                auth.signInWithCredential(firebaseCredential).await()
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
        auth.signOut()
    }
}
