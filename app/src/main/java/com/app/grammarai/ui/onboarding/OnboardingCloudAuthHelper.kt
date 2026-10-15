package com.app.grammarai.ui.onboarding

import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.SupabaseSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OnboardingCloudAuthHelper(
    private val prefs: PreferencesManager,
    private val supabaseSyncRepo: SupabaseSyncRepository,
    private val scope: CoroutineScope,
    private val updateState: ((OnboardingUiState) -> OnboardingUiState) -> Unit,
    private val sendEffect: (OnboardingUiEffect) -> Unit
) {
    fun handleGoogleSignIn(idToken: String) {
        updateState { it.copy(isSigningInWithGoogle = true) }
        scope.launch {
            val result = supabaseSyncRepo.signInWithGoogle(idToken)
            result.onSuccess {
                val email = prefs.userEmail.ifBlank { "Google Account" }
                updateState {
                    it.copy(
                        isSigningInWithGoogle = false,
                        isGoogleLinked = true,
                        linkedGoogleEmail = email,
                        userName = prefs.userName
                    )
                }
                sendEffect(OnboardingUiEffect.ShowToast("Google account connected!"))
                supabaseSyncRepo.syncUserData()
            }.onFailure { err ->
                updateState { it.copy(isSigningInWithGoogle = false) }
                sendEffect(OnboardingUiEffect.ShowToast("Sign in failed: ${err.localizedMessage ?: "Unknown error"}"))
            }
        }
    }
}
