package com.app.grammarai.ui.settings

import androidx.lifecycle.viewModelScope
import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.mvi.MviViewModel
import com.app.grammarai.core.notification.DailyReminderManager
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.SupabaseSyncRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: PreferencesManager = GrammarApplication.instance.preferencesManager,
    private val geminiClient: GeminiApiClient = GrammarApplication.instance.geminiApiClient,
    private val curriculumRepository: CurriculumRepository = GrammarApplication.instance.curriculumRepository,
    private val supabaseSyncRepo: SupabaseSyncRepository = GrammarApplication.instance.supabaseSyncRepository
) : MviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(
    SettingsUiState()
) {

    init {
        updateState {
            it.copy(
                apiKey = prefs.apiKey,
                appLanguage = prefs.appLanguage,
                personaPrompt = prefs.aiPersonaPrompt,
                readingTonePrompt = prefs.aiReadingTonePrompt,
                suggestionsPrompt = prefs.aiSuggestionsPrompt,
                syncUserId = prefs.userId,
                syncUserEmail = prefs.userEmail,
                userName = prefs.userName,
                userAvatarUrl = prefs.userAvatarUrl,
                lessonGoalTarget = prefs.lessonGoalTarget,
                isAuthenticated = prefs.isAuthenticated,
                lastSyncTimestamp = prefs.lastSyncTimestamp,
                isCloudSyncEnabled = prefs.isCloudSyncEnabled,
                dailyReminderEnabled = prefs.dailyReminderEnabled,
                dailyReminderTime = prefs.dailyReminderTime
            )
        }
    }

    override fun processIntent(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.UpdateUserName -> updateState {
                it.copy(userName = intent.name, isSavedSuccess = false)
            }
            is SettingsUiIntent.UpdateLessonGoalTarget -> updateState {
                it.copy(lessonGoalTarget = intent.target, isSavedSuccess = false)
            }
            is SettingsUiIntent.UpdateApiKey -> updateState {
                it.copy(apiKey = intent.key, keyValidationStatus = null, isSavedSuccess = false)
            }
            is SettingsUiIntent.TestApiKey -> testApiKey()
            is SettingsUiIntent.SelectLanguage -> {
                val prev = prefs.appLanguage
                prefs.appLanguage = intent.language
                prefs.nativeLanguage = if (intent.language.equals("en", ignoreCase = true)) "English" else "Spanish"
                curriculumRepository.reloadCurriculum()
                updateState {
                    it.copy(appLanguage = intent.language, isSavedSuccess = false)
                }
                if (prev != intent.language) {
                    sendEffect(SettingsUiEffect.LanguageChanged(intent.language))
                }
            }
            is SettingsUiIntent.UpdatePersonaPrompt -> updateState {
                it.copy(personaPrompt = intent.prompt, isSavedSuccess = false)
            }
            is SettingsUiIntent.UpdateReadingTonePrompt -> updateState {
                it.copy(readingTonePrompt = intent.prompt, isSavedSuccess = false)
            }
            is SettingsUiIntent.UpdateSuggestionsPrompt -> updateState {
                it.copy(suggestionsPrompt = intent.prompt, isSavedSuccess = false)
            }
            is SettingsUiIntent.ResetPrompts -> resetPrompts()
            is SettingsUiIntent.SaveSettings -> saveSettings()
            is SettingsUiIntent.Back -> sendEffect(SettingsUiEffect.NavigateBack)
            is SettingsUiIntent.SignInWithGoogle -> signInWithGoogle(intent.idToken)
            is SettingsUiIntent.SignOut -> signOut()
            is SettingsUiIntent.SyncNow -> performSync()
            is SettingsUiIntent.RestoreFromCloud -> restoreFromCloud(intent.accountIdentifier)
            is SettingsUiIntent.ToggleCloudSync -> {
                prefs.isCloudSyncEnabled = intent.enabled
                updateState { it.copy(isCloudSyncEnabled = intent.enabled) }
            }
            is SettingsUiIntent.ToggleDailyReminder -> {
                prefs.dailyReminderEnabled = intent.enabled
                updateState { it.copy(dailyReminderEnabled = intent.enabled, isSavedSuccess = false) }
                val (hour, minute) = DailyReminderManager.parseReminderTime(state.value.dailyReminderTime)
                if (intent.enabled) {
                    DailyReminderManager.scheduleDailyReminder(GrammarApplication.instance, hour, minute)
                } else {
                    DailyReminderManager.cancelDailyReminder(GrammarApplication.instance)
                }
            }
            is SettingsUiIntent.UpdateDailyReminderTime -> {
                prefs.dailyReminderTime = intent.time
                updateState { it.copy(dailyReminderTime = intent.time, isSavedSuccess = false) }
                if (state.value.dailyReminderEnabled) {
                    val (hour, minute) = DailyReminderManager.parseReminderTime(intent.time)
                    DailyReminderManager.scheduleDailyReminder(GrammarApplication.instance, hour, minute)
                }
            }
        }
    }

    private fun signInWithGoogle(idToken: String) {
        updateState { it.copy(isAuthenticating = true, syncStatusMessage = null) }
        viewModelScope.launch {
            val result = supabaseSyncRepo.signInWithGoogle(idToken)
            result.onSuccess {
                updateState {
                    it.copy(
                        isAuthenticating = false,
                        isAuthenticated = true,
                        syncUserId = prefs.userId,
                        syncUserEmail = prefs.userEmail,
                        userAvatarUrl = prefs.userAvatarUrl,
                        lastSyncTimestamp = prefs.lastSyncTimestamp,
                        syncStatusMessage = "Signed in as ${prefs.userEmail}"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Google Sign-In successful!"))
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Google Sign-In failed"
                updateState {
                    it.copy(
                        isAuthenticating = false,
                        syncStatusMessage = "Auth error: $errorMsg"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Sign-In error: $errorMsg"))
            }
        }
    }

    private fun signOut() {
        supabaseSyncRepo.signOut()
        updateState {
            it.copy(
                isAuthenticated = false,
                userAvatarUrl = null,
                syncStatusMessage = "Signed out."
            )
        }
        sendEffect(SettingsUiEffect.ShowSnackbar("Signed out successfully."))
    }

    private fun testApiKey() {
        val candidate = state.value.apiKey.trim()
        if (candidate.isBlank()) {
            updateState { it.copy(keyValidationStatus = false) }
            return
        }
        updateState { it.copy(isTestingKey = true, keyValidationStatus = null) }
        viewModelScope.launch {
            val valid = geminiClient.testApiKey(candidate)
            updateState { it.copy(isTestingKey = false, keyValidationStatus = valid) }
        }
    }

    private fun resetPrompts() {
        updateState {
            it.copy(
                personaPrompt = PreferencesManager.DEFAULT_PERSONA_PROMPT,
                readingTonePrompt = PreferencesManager.DEFAULT_READING_PROMPT,
                suggestionsPrompt = PreferencesManager.DEFAULT_SUGGESTIONS_PROMPT,
                isSavedSuccess = false
            )
        }
    }

    private fun saveSettings() {
        val s = state.value
        val previousLang = prefs.appLanguage
        val newName = s.userName.trim().ifBlank { "neko" }
        prefs.userName = newName
        prefs.apiKey = s.apiKey.trim()
        prefs.appLanguage = s.appLanguage
        prefs.nativeLanguage = if (s.appLanguage.equals("en", ignoreCase = true)) "English" else "Spanish"
        prefs.aiPersonaPrompt = s.personaPrompt.trim()
        prefs.aiReadingTonePrompt = s.readingTonePrompt.trim()
        prefs.aiSuggestionsPrompt = s.suggestionsPrompt.trim()
        prefs.lessonGoalTarget = s.lessonGoalTarget
        prefs.dailyReminderEnabled = s.dailyReminderEnabled
        prefs.dailyReminderTime = s.dailyReminderTime
        updateState { it.copy(userName = newName, isSavedSuccess = true) }

        if (prefs.isAuthenticated && prefs.isCloudSyncEnabled) {
            viewModelScope.launch {
                supabaseSyncRepo.syncUserData()
            }
        }

        if (previousLang != s.appLanguage) {
            sendEffect(SettingsUiEffect.LanguageChanged(s.appLanguage))
        }
    }

    private fun performSync() {
        if (state.value.isSyncing) return
        updateState { it.copy(isSyncing = true, syncStatusMessage = null) }
        viewModelScope.launch {
            val result = supabaseSyncRepo.syncUserData()
            result.onSuccess {
                updateState {
                    it.copy(
                        isSyncing = false,
                        lastSyncTimestamp = prefs.lastSyncTimestamp,
                        syncUserId = prefs.userId,
                        syncStatusMessage = "Successfully synchronized with Supabase!"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Data synced with Supabase successfully!"))
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Sync failed"
                updateState {
                    it.copy(
                        isSyncing = false,
                        syncStatusMessage = "Sync error: $errorMsg"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Sync error: $errorMsg"))
            }
        }
    }

    private fun restoreFromCloud(identifier: String) {
        val target = identifier.ifBlank { prefs.userId }
        if (!prefs.isAuthenticated || target.isBlank()) {
            sendEffect(SettingsUiEffect.ShowSnackbar("Sign in with Google first to restore your cloud data."))
            return
        }
        updateState { it.copy(isSyncing = true, syncStatusMessage = null) }
        viewModelScope.launch {
            val result = supabaseSyncRepo.restoreFromCloud(target)
            result.onSuccess {
                updateState {
                    it.copy(
                        isSyncing = false,
                        lastSyncTimestamp = prefs.lastSyncTimestamp,
                        syncUserId = prefs.userId,
                        syncUserEmail = prefs.userEmail,
                        userName = prefs.userName,
                        personaPrompt = prefs.aiPersonaPrompt,
                        readingTonePrompt = prefs.aiReadingTonePrompt,
                        suggestionsPrompt = prefs.aiSuggestionsPrompt,
                        appLanguage = prefs.appLanguage,
                        syncStatusMessage = "Account restored from Supabase!"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Cloud data restored successfully!"))
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Restore failed"
                updateState {
                    it.copy(
                        isSyncing = false,
                        syncStatusMessage = "Restore error: $errorMsg"
                    )
                }
                sendEffect(SettingsUiEffect.ShowSnackbar("Restore error: $errorMsg"))
            }
        }
    }
}
