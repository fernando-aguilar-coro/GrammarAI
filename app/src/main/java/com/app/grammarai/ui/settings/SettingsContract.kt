package com.app.grammarai.ui.settings

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState

data class SettingsUiState(
    val apiKey: String = "",
    val isTestingKey: Boolean = false,
    val keyValidationStatus: Boolean? = null,
    val appLanguage: String = "es",
    val personaPrompt: String = "",
    val readingTonePrompt: String = "",
    val suggestionsPrompt: String = "",
    val isSavedSuccess: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isExitConfirmationVisible: Boolean = false,
    val isSyncing: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isAuthenticated: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val syncStatusMessage: String? = null,
    val syncUserId: String = "",
    val syncUserEmail: String = "",
    val userName: String = "neko",
    val userAvatarUrl: String? = null,
    val lessonGoalTarget: Int = 5,
    val isCloudSyncEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String = "20:00"
) : UiState

sealed class SettingsUiIntent : UiIntent {
    data class UpdateUserName(val name: String) : SettingsUiIntent()
    data class UpdateLessonGoalTarget(val target: Int) : SettingsUiIntent()
    data class ToggleDailyReminder(val enabled: Boolean) : SettingsUiIntent()
    data class UpdateDailyReminderTime(val time: String) : SettingsUiIntent()
    data class UpdateApiKey(val key: String) : SettingsUiIntent()
    object TestApiKey : SettingsUiIntent()
    data class SelectLanguage(val language: String) : SettingsUiIntent()
    data class UpdatePersonaPrompt(val prompt: String) : SettingsUiIntent()
    data class UpdateReadingTonePrompt(val prompt: String) : SettingsUiIntent()
    data class UpdateSuggestionsPrompt(val prompt: String) : SettingsUiIntent()
    object ResetPrompts : SettingsUiIntent()
    object SaveSettings : SettingsUiIntent()
    object Back : SettingsUiIntent()
    object KeepEditing : SettingsUiIntent()
    object DiscardAndNavigateBack : SettingsUiIntent()
    object SaveAndNavigateBack : SettingsUiIntent()
    object SyncNow : SettingsUiIntent()
    data class SignInWithGoogle(val idToken: String) : SettingsUiIntent()
    object SignOut : SettingsUiIntent()
    data class RestoreFromCloud(val accountIdentifier: String = "") : SettingsUiIntent()
    data class ToggleCloudSync(val enabled: Boolean) : SettingsUiIntent()
}

sealed class SettingsUiEffect : UiEffect {
    object NavigateBack : SettingsUiEffect()
    data class LanguageChanged(val newLanguage: String) : SettingsUiEffect()
    data class ShowSnackbar(val message: String) : SettingsUiEffect()
}

internal data class SettingsDraftSnapshot(
    val apiKey: String,
    val appLanguage: String,
    val personaPrompt: String,
    val readingTonePrompt: String,
    val suggestionsPrompt: String,
    val userName: String,
    val lessonGoalTarget: Int,
    val isCloudSyncEnabled: Boolean,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: String
) {
    companion object {
        fun from(state: SettingsUiState) = SettingsDraftSnapshot(
            apiKey = state.apiKey,
            appLanguage = state.appLanguage,
            personaPrompt = state.personaPrompt,
            readingTonePrompt = state.readingTonePrompt,
            suggestionsPrompt = state.suggestionsPrompt,
            userName = state.userName,
            lessonGoalTarget = state.lessonGoalTarget,
            isCloudSyncEnabled = state.isCloudSyncEnabled,
            dailyReminderEnabled = state.dailyReminderEnabled,
            dailyReminderTime = state.dailyReminderTime
        )
    }
}
