package com.app.grammarai.ui.practice

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.audio.PronunciationScorer
import com.app.grammarai.core.mvi.MviViewModel
import com.app.grammarai.core.srs.SpacedRepetitionEngine
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.ExerciseAttempt
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.PracticeHistoryRepository
import com.app.grammarai.ui.practice.audio.ExerciseAudioCoordinator
import com.app.grammarai.ui.practice.conversation.LiveConversationCoordinator
import com.app.grammarai.ui.practice.evaluator.DrillPairMatcher
import com.app.grammarai.ui.practice.evaluator.ExerciseAnswerEvaluator
import com.app.grammarai.ui.practice.evaluator.ExerciseFlowCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ExerciseRunnerViewModel(
    context: Context = GrammarApplication.instance,
    private val repo: CurriculumRepository = GrammarApplication.instance.container.curriculumRepository,
    private val prefs: PreferencesManager = GrammarApplication.instance.container.preferencesManager,
    private val historyRepo: PracticeHistoryRepository = GrammarApplication.instance.container.practiceHistoryRepository,
    private val srsEngine: SpacedRepetitionEngine = GrammarApplication.instance.container.spacedRepetitionEngine,
    private val geminiClient: GeminiApiClient = GrammarApplication.instance.container.geminiApiClient,
    private val supabaseSyncRepo: com.app.grammarai.data.repository.SupabaseSyncRepository = GrammarApplication.instance.container.supabaseSyncRepository,
    private val translationManager: com.app.grammarai.core.translation.OnDeviceTranslationManager = GrammarApplication.instance.container.onDeviceTranslationManager
) : MviViewModel<ExerciseUiState, ExerciseUiIntent, ExerciseUiEffect>(
    ExerciseUiState()
) {
    private val audioCoordinator = ExerciseAudioCoordinator(
        context = context,
        geminiClient = geminiClient,
        scope = viewModelScope,
        onAudioLoadingChanged = { l -> processIntent(ExerciseUiIntent.AudioLoadingChanged(l)) },
        onAudioPlayingChanged = { p -> processIntent(ExerciseUiIntent.AudioPlayingChanged(p)) },
        onListeningChanged = { list -> processIntent(ExerciseUiIntent.ListeningChanged(list)) },
        onSpeechResult = { transcript -> processIntent(ExerciseUiIntent.OnSpokenResult(transcript)) }
    )

    private val liveCoordinator = LiveConversationCoordinator(
        geminiClient = geminiClient,
        audioCoordinator = audioCoordinator,
        prefs = prefs,
        scope = viewModelScope,
        stateProvider = { state.value },
        updateState = ::updateState,
        processIntent = ::processIntent
    )

    private val answerEvaluator = ExerciseAnswerEvaluator(
        prefs = prefs,
        historyRepo = historyRepo,
        geminiClient = geminiClient,
        scope = viewModelScope,
        stateProvider = { state.value },
        updateState = ::updateState,
        onIncorrectAnswer = { exercise ->
            audioCoordinator.playCorrectAnswerFeedback(exercise)
        }
    )

    private val pairMatcher = DrillPairMatcher(
        scope = viewModelScope,
        stateProvider = { state.value },
        updateState = ::updateState,
        onAllPairsMatched = { answerEvaluator.checkCurrentAnswer() }
    )

    private val flowCoordinator = ExerciseFlowCoordinator(
        prefs = prefs,
        repo = repo,
        historyRepo = historyRepo,
        srsEngine = srsEngine,
        supabaseSyncRepo = supabaseSyncRepo,
        scope = viewModelScope,
        stateProvider = { state.value },
        updateState = ::updateState,
        onSpeakExercise = ::speakCurrentExercise
    )

    private var loadLessonJob: Job? = null
    private var loadedLessonId: String? = null

    override fun onCleared() {
        super.onCleared()
        loadLessonJob?.cancel()
        loadLessonJob = null
        liveCoordinator.close()
        audioCoordinator.release()
    }

    override fun processIntent(intent: ExerciseUiIntent) {
        when (intent) {
            is ExerciseUiIntent.LoadLesson -> loadLessonExercises(intent.lessonId)
            is ExerciseUiIntent.SelectOption -> {
                handleSelectOption(intent.option)
                val currentEx = state.value.currentExercise
                val optIdx = currentEx?.options?.indexOf(intent.option) ?: -1
                if (currentEx != null && optIdx >= 0) {
                    audioCoordinator.playOptionAudio(currentEx.id, optIdx, intent.option)
                }
            }
            is ExerciseUiIntent.InputText -> {
                handleInputText(intent.text)
            }
            is ExerciseUiIntent.SetTranslationAnswerMode -> if (!state.value.isAnswerChecked) {
                updateState { it.copy(translationAnswerMode = intent.mode) }
            }
            is ExerciseUiIntent.InputTranslationAnswer -> if (!state.value.isAnswerChecked) {
                updateState { it.copy(translationTextAnswer = intent.text) }
            }
            is ExerciseUiIntent.AddTranslationWord -> addTranslationWord(intent.word)
            is ExerciseUiIntent.RemoveTranslationWord -> if (!state.value.isAnswerChecked) {
                val words = state.value.translationOrderedWords.toMutableList()
                if (intent.index in words.indices) {
                    words.removeAt(intent.index)
                    updateState { it.copy(translationOrderedWords = words) }
                }
            }
            is ExerciseUiIntent.ClearTranslationWords -> if (!state.value.isAnswerChecked) {
                updateState { it.copy(translationOrderedWords = emptyList()) }
            }
            is ExerciseUiIntent.TapReconstructWord -> if (!state.value.isAnswerChecked) {
                updateState { it.copy(reconstructedWords = it.reconstructedWords + intent.word) }
                audioCoordinator.playWordAudio(intent.word)
            }
            is ExerciseUiIntent.RemoveReconstructWord -> if (!state.value.isAnswerChecked) {
                val current = state.value.reconstructedWords.toMutableList()
                if (intent.index in current.indices) {
                    val removed = current.removeAt(intent.index)
                    updateState { it.copy(reconstructedWords = current) }
                    audioCoordinator.playWordAudio(removed)
                }
            }
            is ExerciseUiIntent.ClearReconstruction -> if (!state.value.isAnswerChecked) {
                updateState { it.copy(reconstructedWords = emptyList()) }
            }
            is ExerciseUiIntent.CheckAnswer -> answerEvaluator.checkCurrentAnswer()
            is ExerciseUiIntent.ToggleExerciseTranslation -> toggleExerciseTranslation()
            is ExerciseUiIntent.DismissExerciseTranslation -> updateState { it.copy(isTranslationVisible = false) }
            is ExerciseUiIntent.GiveUpExercise -> answerEvaluator.giveUpCurrentAnswer()
            is ExerciseUiIntent.FinishPractice -> if (state.value.isCompleted) sendEffect(ExerciseUiEffect.NavigateToFeed) else finishPractice()
            is ExerciseUiIntent.NextExercise -> advanceToExercise(state.value.currentExerciseIndex + 1)
            is ExerciseUiIntent.ContinueFromConversation -> {
                flowCoordinator.onConversationExerciseFinished()
                advanceToExercise(state.value.currentExerciseIndex + 1)
            }
            is ExerciseUiIntent.RequestAiEvaluation -> answerEvaluator.requestAiEvaluation()
            is ExerciseUiIntent.DismissAiExplanationDialog -> updateState { it.copy(showAiExplanationDialog = false) }
            is ExerciseUiIntent.DismissCelebration -> {
                updateState { it.copy(showCompletionCelebration = false) }
                sendEffect(ExerciseUiEffect.NavigateToFeed)
            }
            is ExerciseUiIntent.Back -> {
                liveCoordinator.close()
                sendEffect(ExerciseUiEffect.NavigateBack)
            }
            is ExerciseUiIntent.PlayExerciseAudio -> speakCurrentExercise()
            is ExerciseUiIntent.SpeakText -> audioCoordinator.playWordAudio(intent.text)
            is ExerciseUiIntent.StopAudio -> audioCoordinator.stopAudio()
            is ExerciseUiIntent.ToggleAudio -> if (state.value.isPlayingAudio) audioCoordinator.stopAudio() else speakCurrentExercise()
            is ExerciseUiIntent.ToggleSpeechRecording -> handleToggleSpeechRecording()
            is ExerciseUiIntent.OnSpokenResult -> handleSpokenResult(intent.transcript)
            is ExerciseUiIntent.AudioLoadingChanged -> updateState { it.copy(isAudioLoading = intent.isLoading) }
            is ExerciseUiIntent.AudioPlayingChanged -> updateState { it.copy(isPlayingAudio = intent.isPlaying) }
            is ExerciseUiIntent.ListeningChanged -> updateState { it.copy(isListening = intent.isListening) }
            is ExerciseUiIntent.SelectPairLeft -> {
                audioCoordinator.playWordAudio(intent.text)
                pairMatcher.selectLeft(intent.id, intent.text)
            }
            is ExerciseUiIntent.SelectPairRight -> {
                val currentEx = state.value.currentExercise
                val matchingPair = currentEx?.pairs?.firstOrNull {
                    it.right.trim().equals(intent.text.trim(), ignoreCase = true)
                }
                val audioText = if (matchingPair != null && (
                        currentEx.id.contains("pre_a1_01") ||
                        currentEx.id.contains("pre_a1_02") ||
                        (matchingPair.left.trim().length == 1 && matchingPair.left.trim()[0].isLetter())
                    )) {
                    matchingPair.left.trim()
                } else {
                    intent.text
                }
                audioCoordinator.playWordAudio(audioText)
                pairMatcher.selectRight(intent.id, intent.text)
            }

            // Live Conversation Intents delegated to LiveConversationCoordinator
            is ExerciseUiIntent.StartLiveConversation -> {
                val ex = state.value.currentExercise
                if (ex != null && ex.type == ExerciseType.CONVERSATION_DIALOGUE) {
                    liveCoordinator.startSession(ex)
                }
            }
            is ExerciseUiIntent.EndLiveConversation -> liveCoordinator.endSession()
            is ExerciseUiIntent.SendConversationTurn -> liveCoordinator.sendUserTurn(intent.text)
            is ExerciseUiIntent.LiveStreamingTextUpdated -> updateState {
                it.copy(liveStreamingAiText = intent.aiText, liveStreamingUserText = intent.userText)
            }
            is ExerciseUiIntent.LiveTurnsUpdated -> {
                val modelCount = intent.turns.count { it.role == "model" && !it.text.startsWith("⚠️") }
                updateState {
                    it.copy(
                        conversationTurns = intent.turns,
                        conversationTurnCount = modelCount,
                        isConversationEnded = intent.isEnded,
                        isLiveConversationActive = !intent.isEnded,
                        liveStreamingAiText = "",
                        liveStreamingUserText = ""
                    )
                }
            }
            is ExerciseUiIntent.ChangeConversationTopic -> {
                val currentEx = state.value.currentExercise
                if (currentEx != null && currentEx.type == ExerciseType.CONVERSATION_DIALOGUE) {
                    liveCoordinator.startSession(currentEx, customTopic = intent.newTopic)
                }
            }
        }
    }

    private fun handleToggleSpeechRecording() {
        val currentEx = state.value.currentExercise
        if (currentEx?.type == ExerciseType.CONVERSATION_DIALOGUE) {
            liveCoordinator.handleToggleMic()
        } else {
            audioCoordinator.stopAudio()
            audioCoordinator.toggleListening(state.value.isListening)
        }
    }

    private fun handleSelectOption(option: String) {
        if (state.value.isAnswerChecked) return
        updateState { it.copy(selectedOption = option) }
    }

    private fun handleInputText(text: String) {
        if (state.value.isAnswerChecked) return
        updateState { it.copy(selectedOption = text) }
    }

    private fun addTranslationWord(word: String) {
        if (state.value.isAnswerChecked) return
        val exercise = state.value.currentExercise ?: return
        if (exercise.type != ExerciseType.TRANSLATE) return
        val availableCount = translationAnswerTokens(exercise).count { it == word }
        val selectedCount = state.value.translationOrderedWords.count { it == word }
        if (selectedCount >= availableCount) return
        updateState { it.copy(translationOrderedWords = it.translationOrderedWords + word) }
        audioCoordinator.playWordAudio(word)
    }

    private fun translationAnswerTokens(exercise: com.app.grammarai.data.model.GeneratedExercise): List<String> {
        val answer = exercise.correctAnswer.ifBlank { exercise.targetPhrase }
        return answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    private fun handleSpokenResult(transcript: String) {
        if (transcript.isBlank()) return
        val currentEx = state.value.currentExercise ?: return
        val target = currentEx.targetPhrase.ifBlank { currentEx.correctAnswer }
        val score = PronunciationScorer.calculateMatchScore(transcript, target)
        updateState {
            it.copy(
                spokenTranscript = transcript,
                pronunciationMatchScore = score,
                isListening = false
            )
        }
        answerEvaluator.checkCurrentAnswer()
    }

    private fun loadLessonExercises(lessonId: String) {
        if (loadedLessonId == lessonId && state.value.exercises.isNotEmpty()) return
        loadedLessonId = lessonId
        loadLessonJob?.cancel()

        // Immediate retrieval from cache to eliminate transition lag and flash of loading screen
        val cached = repo.getLesson(lessonId)
        if (cached != null && cached.exercises.isNotEmpty()) {
            updateState {
                it.copy(
                    lesson = cached,
                    exercises = cached.exercises,
                    currentExerciseIndex = 0,
                    score = 0,
                    exerciseResults = emptyMap(),
                    selectedOption = null,
                    reconstructedWords = emptyList(),
                    translationAnswerMode = TranslationAnswerMode.ORDER,
                    translationTextAnswer = "",
                    translationOrderedWords = emptyList(),
                    spokenTranscript = "",
                    pronunciationMatchScore = null,
                    isAnswerChecked = false,
                    isCorrect = false,
                    isLoadingExercises = false,
                    isCompleted = false,
                    showCompletionCelebration = false,
                    isTranslationVisible = false,
                    isTranslating = false
                )
            }
            loadLessonJob = viewModelScope.launch {
                kotlinx.coroutines.delay(200)
                speakCurrentExercise()
            }
            return
        }

        loadLessonJob = viewModelScope.launch {
            updateState { it.copy(isLoadingExercises = true) }
            val lesson = repo.getLessonAsync(lessonId) ?: repo.getLesson(lessonId)
            val exercises = lesson?.exercises ?: emptyList()
            updateState {
                it.copy(
                    lesson = lesson,
                    exercises = exercises,
                    currentExerciseIndex = 0,
                    score = 0,
                    exerciseResults = emptyMap(),
                    selectedOption = null,
                    reconstructedWords = emptyList(),
                    translationAnswerMode = TranslationAnswerMode.ORDER,
                    translationTextAnswer = "",
                    translationOrderedWords = emptyList(),
                    spokenTranscript = "",
                    pronunciationMatchScore = null,
                    isAnswerChecked = false,
                    isCorrect = false,
                    isLoadingExercises = false,
                    isCompleted = false,
                    showCompletionCelebration = false,
                    isTranslationVisible = false,
                    isTranslating = false
                )
            }
            if (exercises.isNotEmpty()) {
                kotlinx.coroutines.delay(200)
                speakCurrentExercise()
            }
        }
    }

    private fun toggleExerciseTranslation() {
        val currentEx = state.value.currentExercise ?: return
        if (!com.app.grammarai.core.translation.OnDeviceTranslationManager.isExerciseCompatible(currentEx)) return

        if (state.value.isTranslationVisible) {
            updateState { it.copy(isTranslationVisible = false) }
            return
        }

        updateState { it.copy(isTranslationVisible = true) }

        val cachedTranslation = state.value.exerciseTranslations[currentEx.id]
        if (!cachedTranslation.isNullOrBlank()) {
            return
        }

        val textToTranslate = com.app.grammarai.core.translation.OnDeviceTranslationManager.extractTranslatableText(currentEx)
        if (textToTranslate.isBlank()) return

        val targetLang = if (prefs.appLanguage.equals("en", ignoreCase = true)) "English" else prefs.nativeLanguage.ifBlank { "Spanish" }

        viewModelScope.launch {
            updateState { it.copy(isTranslating = true) }
            val mlKitResult = translationManager.translate(textToTranslate, targetLang)
            val translated = mlKitResult.getOrNull()?.takeIf { it.isNotBlank() } ?: textToTranslate

            updateState {
                it.copy(
                    isTranslating = false,
                    exerciseTranslations = it.exerciseTranslations + (currentEx.id to translated)
                )
            }
        }
    }

    private fun advanceToExercise(nextIndex: Int) {
        liveCoordinator.close()
        audioCoordinator.stopAudio()
        audioCoordinator.stopRecording()
        updateState { it.copy(isTranslationVisible = false, isTranslating = false) }
        flowCoordinator.advanceToExercise(nextIndex)
    }

    private fun finishPractice() {
        liveCoordinator.close()
        flowCoordinator.finishPractice()
    }

    private fun speakCurrentExercise() {
        val exercise = state.value.currentExercise ?: return
        if (exercise.type == ExerciseType.CONVERSATION_DIALOGUE) {
            liveCoordinator.startSession(exercise)
            return
        }
        audioCoordinator.playExerciseSequence(exercise)
    }
}
