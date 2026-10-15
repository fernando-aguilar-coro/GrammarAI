package com.app.grammarai.ui.onboarding

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState

enum class OnboardingStep {
    PRESENTATION,
    NATIVE_LANGUAGE,
    APP_LANGUAGE,
    NAME,
    GOAL,
    SITUATIONS,
    LEVEL,
    WEEKLY_GOAL,
    REMINDER,
    API_KEY,
    API_KEY_INPUT,
    SYNC_ACCOUNT
}

data class ChatMessage(
    val id: String,
    val isFromAi: Boolean,
    val text: String,
    val translation: String? = null,
    val isTranslationVisible: Boolean = false,
    val chips: List<String> = emptyList(),
    val selectedChip: String? = null,
    val timestamp: String = "Just now"
)

data class OnboardingUiState(
    val isLoadingIntro: Boolean = true,
    val isFinalizing: Boolean = false,
    val isAiTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val currentStep: OnboardingStep = OnboardingStep.PRESENTATION,
    val nameInput: String = "",
    val userName: String = "",
    val nativeLanguage: String = "Spanish",
    val appLanguage: String = "es",
    val primaryGoal: String = "",
    val targetSituations: String = "",
    val selectedSituations: Set<String> = emptySet(),
    val englishLevel: String = "",
    val lessonGoalTarget: Int = 5,
    val dailyReminderEnabled: Boolean = false,
    val isAudioLoading: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val currentAudioMessageId: String? = null,
    val isSigningInWithGoogle: Boolean = false,
    val isGoogleLinked: Boolean = false,
    val linkedGoogleEmail: String? = null,
    val apiKeyInput: String = "",
    val isTestingKey: Boolean = false,
    val keyValidationStatus: Boolean? = null
) : UiState

sealed interface OnboardingUiIntent : UiIntent {
    object DismissIntroLoading : OnboardingUiIntent
    object AdvanceFromPresentation : OnboardingUiIntent
    data class SelectNativeLanguage(val language: String) : OnboardingUiIntent
    data class SelectAppLanguageOption(val option: String) : OnboardingUiIntent
    data class UpdateNameInput(val name: String) : OnboardingUiIntent
    object SubmitName : OnboardingUiIntent
    data class SelectGoal(val goal: String) : OnboardingUiIntent
    data class SelectSituation(val situation: String) : OnboardingUiIntent
    data class ToggleSituationSelection(val situation: String) : OnboardingUiIntent
    object ConfirmSituationsSelection : OnboardingUiIntent
    data class SelectLevel(val level: String) : OnboardingUiIntent
    data class SelectWeeklyGoal(val goalOption: String) : OnboardingUiIntent
    data class SelectReminderOption(val option: String) : OnboardingUiIntent
    data class SelectApiKeyOption(val option: String) : OnboardingUiIntent
    data class UpdateApiKeyInput(val key: String) : OnboardingUiIntent
    object TestApiKey : OnboardingUiIntent
    object SubmitApiKey : OnboardingUiIntent
    object SkipApiKey : OnboardingUiIntent
    data class SignInWithGoogle(val idToken: String) : OnboardingUiIntent
    object SkipSyncAccount : OnboardingUiIntent
    object SkipOnboarding : OnboardingUiIntent
    data class PlayAudio(val messageId: String, val text: String) : OnboardingUiIntent
    object StopAudio : OnboardingUiIntent
    data class ToggleAudio(val messageId: String, val text: String) : OnboardingUiIntent
    data class ToggleTranslation(val messageId: String) : OnboardingUiIntent
}

sealed interface OnboardingUiEffect : UiEffect {
    object NavigateToFeed : OnboardingUiEffect
    data class ShowToast(val message: String) : OnboardingUiEffect
}
