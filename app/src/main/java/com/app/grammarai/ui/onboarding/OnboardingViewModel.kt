package com.app.grammarai.ui.onboarding

import androidx.lifecycle.viewModelScope
import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.mvi.MviViewModel
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.SupabaseSyncRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefs: PreferencesManager = GrammarApplication.instance.preferencesManager,
    private val curriculumRepository: CurriculumRepository = GrammarApplication.instance.curriculumRepository,
    private val supabaseSyncRepo: SupabaseSyncRepository = GrammarApplication.instance.supabaseSyncRepository,
    private val geminiClient: GeminiApiClient = GrammarApplication.instance.geminiApiClient
) : MviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(
    OnboardingUiState()
) {

    private val audioHandler = OnboardingAudioPlayer()
    private val stepHandler = OnboardingStepHandler(prefs, curriculumRepository, ::updateState)
    private val authHelper = OnboardingCloudAuthHelper(prefs, supabaseSyncRepo, viewModelScope, ::updateState, ::sendEffect)

    init {
        val firstMessage = OnboardingScriptHelper.getPresentationMessage()
        updateState {
            it.copy(
                messages = listOf(firstMessage),
                currentStep = OnboardingStep.PRESENTATION,
                isLoadingIntro = true
            )
        }

        viewModelScope.launch {
            delay(1200)
            if (state.value.isLoadingIntro) {
                dismissIntroAndPlayGreeting()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioInternal()
        audioHandler.release()
    }

    override fun processIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.DismissIntroLoading -> dismissIntroAndPlayGreeting()
            is OnboardingUiIntent.AdvanceFromPresentation -> handleAdvanceFromPresentation()
            is OnboardingUiIntent.SelectNativeLanguage -> handleNativeLanguageSelection(intent.language)
            is OnboardingUiIntent.SelectAppLanguageOption -> handleAppLanguageSelection(intent.option)
            is OnboardingUiIntent.UpdateNameInput -> updateState { it.copy(nameInput = intent.name) }
            is OnboardingUiIntent.SubmitName -> handleNameSubmission()
            is OnboardingUiIntent.SelectGoal -> handleGoalSelection(intent.goal)
            is OnboardingUiIntent.SelectSituation -> handleSituationSelection(intent.situation)
            is OnboardingUiIntent.ToggleSituationSelection -> handleToggleSituationSelection(intent.situation)
            is OnboardingUiIntent.ConfirmSituationsSelection -> handleConfirmSituationsSelection()
            is OnboardingUiIntent.SelectLevel -> handleLevelSelection(intent.level)
            is OnboardingUiIntent.SelectWeeklyGoal -> handleWeeklyGoalSelection(intent.goalOption)
            is OnboardingUiIntent.SelectReminderOption -> handleReminderSelection(intent.option)
            is OnboardingUiIntent.SelectApiKeyOption -> handleApiKeyOptionSelection(intent.option)
            is OnboardingUiIntent.UpdateApiKeyInput -> updateState { it.copy(apiKeyInput = intent.key, keyValidationStatus = null) }
            is OnboardingUiIntent.TestApiKey -> testApiKey()
            is OnboardingUiIntent.SubmitApiKey -> submitApiKey()
            is OnboardingUiIntent.SkipApiKey -> skipApiKey()
            is OnboardingUiIntent.SignInWithGoogle -> authHelper.handleGoogleSignIn(intent.idToken)
            is OnboardingUiIntent.SkipSyncAccount -> completeOnboarding()
            is OnboardingUiIntent.SkipOnboarding -> skipOnboarding()
            is OnboardingUiIntent.PlayAudio -> playAudioInternal(intent.messageId, intent.text)
            is OnboardingUiIntent.StopAudio -> stopAudioInternal()
            is OnboardingUiIntent.ToggleAudio -> toggleAudio(intent.messageId, intent.text)
            is OnboardingUiIntent.ToggleTranslation -> toggleTranslation(intent.messageId)
        }
    }

    private fun dismissIntroAndPlayGreeting() {
        updateState { it.copy(isLoadingIntro = false) }
        val firstMsg = state.value.messages.firstOrNull()
        if (firstMsg != null) {
            playAudioInternal(firstMsg.id, firstMsg.text)
        }
    }

    private fun advanceWithAiResponse(
        userPrefix: String,
        userText: String,
        nextStep: OnboardingStep,
        getNextAiMsg: () -> ChatMessage
    ) {
        val userMsg = stepHandler.createUserMessage(userPrefix, userText)
        updateState { it.copy(messages = it.messages + userMsg, isAiTyping = true) }
        viewModelScope.launch {
            delay(500)
            val aiNextMsg = getNextAiMsg()
            updateState {
                it.copy(isAiTyping = false, currentStep = nextStep, messages = it.messages + aiNextMsg)
            }
            playAudioInternal(aiNextMsg.id, aiNextMsg.text)
        }
    }

    private fun handleAdvanceFromPresentation() {
        advanceWithAiResponse("start", "Let's get started! 🚀", OnboardingStep.NATIVE_LANGUAGE) {
            OnboardingScriptHelper.getNativeLanguageMessage()
        }
    }

    private fun handleNativeLanguageSelection(language: String) {
        val selectedNative = stepHandler.handleNativeLanguage(language)
        updateState { it.copy(nativeLanguage = selectedNative) }
        advanceWithAiResponse("nat_lang", language, OnboardingStep.APP_LANGUAGE) {
            OnboardingScriptHelper.getAppLanguageMessage()
        }
    }

    private fun handleAppLanguageSelection(option: String) {
        val newLang = stepHandler.handleAppLanguage(option)
        updateState { it.copy(appLanguage = newLang) }
        advanceWithAiResponse("app_lang", option, OnboardingStep.NAME) {
            OnboardingScriptHelper.getNameMessage(prefs.appLanguage)
        }
    }

    private fun handleNameSubmission() {
        val name = stepHandler.handleName(state.value.nameInput)
        updateState { it.copy(nameInput = "", userName = name) }
        advanceWithAiResponse("name", name, OnboardingStep.GOAL) {
            OnboardingScriptHelper.getGoalMessage(name, prefs.appLanguage)
        }
    }

    private fun handleGoalSelection(goal: String) {
        stepHandler.handleGoal(goal)
        updateState { it.copy(primaryGoal = goal) }
        advanceWithAiResponse("goal", goal, OnboardingStep.SITUATIONS) {
            OnboardingScriptHelper.getSituationsMessage(prefs.appLanguage)
        }
    }

    private fun handleSituationSelection(situation: String) {
        stepHandler.handleSituation(situation)
        updateState { it.copy(targetSituations = situation) }
        continueAfterSituations("situation", situation)
    }

    private fun handleToggleSituationSelection(situation: String) {
        updateState { s ->
            s.copy(selectedSituations = stepHandler.toggleSituation(s.selectedSituations, situation))
        }
    }

    private fun handleConfirmSituationsSelection() {
        val joined = stepHandler.confirmSituations(state.value.selectedSituations)
        updateState { it.copy(targetSituations = joined) }
        continueAfterSituations("situations", joined)
    }

    private fun continueAfterSituations(userPrefix: String, userText: String) {
        if (prefs.appLanguage.equals("en", ignoreCase = true)) {
            val level = stepHandler.handleLevel("A1")
            updateState { it.copy(englishLevel = level) }
            advanceWithAiResponse(userPrefix, userText, OnboardingStep.WEEKLY_GOAL) {
                OnboardingScriptHelper.getWeeklyGoalMessage(prefs.appLanguage)
            }
        } else {
            advanceWithAiResponse(userPrefix, userText, OnboardingStep.LEVEL) {
                OnboardingScriptHelper.getLevelMessage()
            }
        }
    }

    private fun handleLevelSelection(level: String) {
        stepHandler.handleLevel(level)
        updateState { it.copy(englishLevel = level) }
        if (level.contains("saltar pre-a1", ignoreCase = true)) {
            viewModelScope.launch {
                curriculumRepository.markPreA1LessonsCompleted(
                    throughLessonId = "pre_a1_preguntas_y_supervivencia_012"
                )
            }
        }
        advanceWithAiResponse("level", level, OnboardingStep.WEEKLY_GOAL) {
            OnboardingScriptHelper.getWeeklyGoalMessage(prefs.appLanguage)
        }
    }

    private fun handleWeeklyGoalSelection(goalOption: String) {
        val target = stepHandler.handleWeeklyGoal(goalOption)
        updateState { it.copy(lessonGoalTarget = target) }
        advanceWithAiResponse("weekly_goal", goalOption, OnboardingStep.REMINDER) {
            OnboardingScriptHelper.getReminderMessage(prefs.appLanguage)
        }
    }

    private fun handleReminderSelection(option: String) {
        val wantsReminder = stepHandler.handleReminder(option)
        updateState { it.copy(dailyReminderEnabled = wantsReminder) }
        advanceWithAiResponse("reminder", option, OnboardingStep.API_KEY) {
            OnboardingScriptHelper.getApiKeyExplanationMessage(prefs.appLanguage)
        }
    }

    private fun handleApiKeyOptionSelection(option: String) {
        val isEn = prefs.appLanguage.equals("en", ignoreCase = true)
        val isHowTo = option.contains("cómo", true) || option.contains("how", true) || option.contains("gratis", true) || option.contains("free", true)
        val isEnter = option.contains("Configurar", true) || option.contains("Set up", true) || option.contains("Ingresar", true) || option.contains("Enter", true)

        if (isHowTo) {
            advanceWithAiResponse("api_choice", option, OnboardingStep.API_KEY_INPUT) {
                OnboardingScriptHelper.getApiKeyHowToMessage(prefs.appLanguage)
            }
        } else if (isEnter) {
            advanceWithAiResponse("api_choice", option, OnboardingStep.API_KEY_INPUT) {
                OnboardingScriptHelper.getApiKeyInputPromptMessage(prefs.appLanguage)
            }
        } else {
            val userText = if (isEn) "⚡ Continue with default key" else "⚡ Continuar con clave genérica"
            advanceWithAiResponse("api_choice", userText, OnboardingStep.SYNC_ACCOUNT) {
                OnboardingScriptHelper.getSyncAccountMessage(prefs.appLanguage)
            }
        }
    }

    private fun testApiKey() {
        val candidate = state.value.apiKeyInput.trim()
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

    private fun submitApiKey() {
        val key = state.value.apiKeyInput.trim()
        val isEn = prefs.appLanguage.equals("en", ignoreCase = true)
        if (key.isNotBlank()) {
            stepHandler.handleApiKey(key)
        }
        val userText = if (key.isNotBlank()) {
            if (isEn) "🔑 API Key saved" else "🔑 Clave API guardada"
        } else {
            if (isEn) "⚡ Continue with default key" else "⚡ Continuar con clave genérica"
        }
        advanceWithAiResponse("api_submit", userText, OnboardingStep.SYNC_ACCOUNT) {
            OnboardingScriptHelper.getSyncAccountMessage(prefs.appLanguage)
        }
    }

    private fun skipApiKey() {
        val isEn = prefs.appLanguage.equals("en", ignoreCase = true)
        val userText = if (isEn) "⚡ Continue with default key" else "⚡ Continuar con clave genérica"
        advanceWithAiResponse("api_skip", userText, OnboardingStep.SYNC_ACCOUNT) {
            OnboardingScriptHelper.getSyncAccountMessage(prefs.appLanguage)
        }
    }

    private fun completeOnboarding() {
        stopAudioInternal()
        prefs.isOnboardingCompleted = true
        curriculumRepository.reloadCurriculum()
        updateState { it.copy(isFinalizing = true) }
        viewModelScope.launch {
            if (prefs.isAuthenticated) {
                supabaseSyncRepo.syncUserData()
            }
            delay(500)
            sendEffect(OnboardingUiEffect.NavigateToFeed)
        }
    }

    private fun skipOnboarding() {
        stopAudioInternal()
        if (prefs.userName.isBlank() || prefs.userName.equals("Alex", ignoreCase = true)) {
            prefs.userName = "neko"
        }
        prefs.isOnboardingCompleted = true
        curriculumRepository.reloadCurriculum()
        sendEffect(OnboardingUiEffect.NavigateToFeed)
    }

    private fun toggleAudio(messageId: String, text: String) {
        if (state.value.isPlayingAudio && state.value.currentAudioMessageId == messageId) {
            stopAudioInternal()
        } else {
            playAudioInternal(messageId, text)
        }
    }

    private fun stopAudioInternal() {
        audioHandler.stop()
        updateState { it.copy(isAudioLoading = false, isPlayingAudio = false, currentAudioMessageId = null) }
    }

    private fun playAudioInternal(messageId: String, text: String) {
        stopAudioInternal()
        updateState { it.copy(isAudioLoading = false, isPlayingAudio = false, currentAudioMessageId = messageId) }
        audioHandler.playAudio(
            messageId = messageId,
            text = text,
            appLanguage = prefs.appLanguage,
            onStart = { updateState { it.copy(isPlayingAudio = true) } },
            onCompletion = { updateState { it.copy(isPlayingAudio = false, currentAudioMessageId = null) } }
        )
    }

    private fun toggleTranslation(messageId: String) {
        updateState { s ->
            s.copy(
                messages = s.messages.map { msg ->
                    if (msg.id == messageId) msg.copy(isTranslationVisible = !msg.isTranslationVisible) else msg
                }
            )
        }
    }
}
