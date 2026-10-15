package com.app.grammarai.core.auth

import android.content.Context
import android.content.Intent
import com.app.grammarai.data.remote.supabase.SupabaseConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuthManager {

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun parseSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.idToken.isNullOrBlank()) {
                Result.success(account)
            } else {
                Result.failure(IllegalStateException("No Google ID token returned from Sign-In"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getClient(context).signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
