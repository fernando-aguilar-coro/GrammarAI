package com.app.grammarai.ui.practice.conversation

import android.util.Log
import com.app.grammarai.core.audio.PcmStreamAudioPlayer
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.remote.GeminiLiveConversationSession
import com.app.grammarai.data.remote.LiveConversationTurn
import com.app.grammarai.ui.practice.ExerciseUiIntent
import com.app.grammarai.ui.practice.ExerciseUiState
import com.app.grammarai.ui.practice.LiveVoiceState
import com.app.grammarai.ui.practice.audio.ExerciseAudioCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coordinates native full-duplex Gemini Live conversation drills.
 * Features:
 * 1. Continuous, always-active microphone streaming (full duplex).
 * 2. Native Gemini VAD barge-in: user can speak and interrupt the AI at any time.
 * 3. Unified state machine driven by [LiveVoiceState].
 */
class LiveConversationCoordinator(
    private val geminiClient: GeminiApiClient,
    private val audioCoordinator: ExerciseAudioCoordinator,
    private val prefs: PreferencesManager,
    private val scope: CoroutineScope,
    private val stateProvider: () -> ExerciseUiState,
    private val updateState: ((ExerciseUiState) -> ExerciseUiState) -> Unit,
    private val processIntent: (ExerciseUiIntent) -> Unit
) {
    companion object {
        private const val TAG = "LiveConvCoordinator"
    }

    private val pcmStreamPlayer = PcmStreamAudioPlayer()
    private val audioRecordHelper get() = audioCoordinator.audioRecordHelper

    var session: GeminiLiveConversationSession? = null
        private set

    fun startSession(exercise: GeneratedExercise, customTopic: String? = null) {
        session?.close()
        audioCoordinator.stopAudio()
        pcmStreamPlayer.stop()
        audioRecordHelper.stopRecording()

        val maxTurns = 8
        val currentLesson = stateProvider().lesson
        val lessonTitle = currentLesson?.title?.ifBlank { null } ?: exercise.targetPhrase.ifBlank { "English Grammar" }
        val userName = prefs.userName.ifBlank { "neko" }
        val englishLevel = prefs.englishLevel.ifBlank { "Intermediate (B1 - B2)" }
        val nativeLanguage = prefs.nativeLanguage.ifBlank { "Spanish" }
        val appLanguage = prefs.appLanguage.ifBlank { "es" }
        val targetSituations = prefs.targetSituations.ifBlank { "Daily Life & Casual Chats" }

        val isSituationalLesson = currentLesson?.type == com.app.grammarai.data.model.LessonType.SITUATIONAL_VOCABULARY ||
                exercise.category.canonical == com.app.grammarai.data.model.SkillCategory.LEXICON

        val situationOptions = targetSituations.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val chosenSituation = if (isSituationalLesson) {
            currentLesson?.title?.ifBlank { null } ?: exercise.targetPhrase.ifBlank { "Real-World Situation" }
        } else {
            customTopic?.ifBlank { null } ?: (if (situationOptions.isNotEmpty()) situationOptions.random() else targetSituations)
        }

        updateState {
            it.copy(
                isLiveConversationActive = true,
                isConversationEnded = false,
                isAiRespondingLive = true,
                liveVoiceState = LiveVoiceState.CONNECTING,
                isMicMuted = false,
                isListening = true,
                conversationTurns = emptyList(),
                conversationTurnCount = 0,
                maxConversationTurns = maxTurns,
                liveStreamingAiText = "",
                liveStreamingUserText = "",
                userReplySuggestions = emptyList(),
                currentConversationTopic = chosenSituation
            )
        }

        val prompts = LiveConversationPromptBuilder.build(
            userName = userName,
            englishLevel = englishLevel,
            nativeLanguage = nativeLanguage,
            appLanguage = appLanguage,
            lessonTitle = lessonTitle,
            chosenSituation = chosenSituation,
            isSituationalLesson = isSituationalLesson,
            maxTurns = maxTurns
        )

        pcmStreamPlayer.prepare(
            onPlaybackStarted = {
                updateState {
                    it.copy(
                        isPlayingAudio = true,
                        isAiRespondingLive = true,
                        liveVoiceState = LiveVoiceState.AI_SPEAKING
                    )
                }
            },
            onTurnFinished = {
                updateState {
                    it.copy(
                        isPlayingAudio = false,
                        isAiRespondingLive = false,
                        liveVoiceState = if (it.isMicMuted) LiveVoiceState.IDLE else LiveVoiceState.LISTENING
                    )
                }
            }
        )

        val newSession = GeminiLiveConversationSession(
            client = geminiClient,
            topic = prompts.topic,
            persona = prompts.persona,
            maxTurns = maxTurns,
            onModelStreaming = { aiStream ->
                updateState {
                    it.copy(
                        isAiRespondingLive = true,
                        liveVoiceState = LiveVoiceState.AI_SPEAKING,
                        liveStreamingAiText = aiStream
                    )
                }
                processIntent(
                    ExerciseUiIntent.LiveStreamingTextUpdated(
                        aiText = aiStream,
                        userText = stateProvider().liveStreamingUserText
                    )
                )
            },
            onUserStreaming = { userStream ->
                // Gemini server-side ASR streaming user speech
                updateState {
                    it.copy(
                        liveStreamingUserText = userStream,
                        liveVoiceState = LiveVoiceState.USER_SPEAKING
                    )
                }
                processIntent(
                    ExerciseUiIntent.LiveStreamingTextUpdated(
                        aiText = stateProvider().liveStreamingAiText,
                        userText = userStream
                    )
                )
            },
            onAudioChunkStreaming = { chunk ->
                pcmStreamPlayer.writeChunk(chunk)
            },
            onInterrupted = {
                // Native barge-in: user spoke while AI was generating/playing
                Log.d(TAG, "Native barge-in detected! Stopping speaker and switching to user speech.")
                pcmStreamPlayer.handleInterruption()
                updateState {
                    it.copy(
                        isPlayingAudio = false,
                        isAiRespondingLive = false,
                        liveStreamingAiText = "",
                        liveVoiceState = LiveVoiceState.USER_SPEAKING
                    )
                }
            },
            onModelTurnComplete = { turn, _ ->
                val isTargetReached = session?.isTargetTurnReached() == true

                pcmStreamPlayer.onTurnComplete {
                    updateState {
                        it.copy(
                            isAiRespondingLive = false,
                            isPlayingAudio = false,
                            liveVoiceState = if (it.isMicMuted) LiveVoiceState.IDLE else LiveVoiceState.LISTENING
                        )
                    }
                    if (isTargetReached) {
                        scope.launch {
                            delay(1200)
                            session?.endSession()
                        }
                    }
                }
                val turns = session?.history ?: listOf(turn)
                processIntent(ExerciseUiIntent.LiveTurnsUpdated(turns = turns, isEnded = false))
            },
            onConversationEnded = { finalTurns ->
                audioRecordHelper.stopRecording()
                pcmStreamPlayer.stop()
                updateState {
                    it.copy(
                        isAiRespondingLive = false,
                        isPlayingAudio = false,
                        isLiveConversationActive = false,
                        isListening = false,
                        liveVoiceState = LiveVoiceState.ENDED
                    )
                }
                processIntent(ExerciseUiIntent.LiveTurnsUpdated(turns = finalTurns, isEnded = false))
                processIntent(ExerciseUiIntent.NextExercise)
            },
            onError = { err ->
                Log.e(TAG, "Live conversation error: ${err.message}", err)
                audioRecordHelper.stopRecording()
                pcmStreamPlayer.stop()
                updateState {
                    it.copy(
                        isListening = false,
                        isAiRespondingLive = false,
                        isPlayingAudio = false,
                        isLiveConversationActive = false,
                        liveVoiceState = LiveVoiceState.ENDED
                    )
                }
            }
        )

        session = newSession
        newSession.startSession(initialGreetingPrompt = prompts.initialGreetingPrompt)

        // Always-active full-duplex microphone recording
        audioRecordHelper.startRecording(
            onChunkReady = { chunk ->
                if (!audioRecordHelper.isMuted) {
                    session?.streamAudioChunk(chunk)
                }
            },
            onError = { e ->
                Log.e(TAG, "AudioRecordHelper error: ${e.message}", e)
                updateState { it.copy(isListening = false) }
            }
        )
    }

    /**
     * Mute or un-mute microphone toggle for user privacy.
     */
    fun handleToggleMic() {
        val newMuted = !stateProvider().isMicMuted
        audioRecordHelper.setMuted(newMuted)
        updateState {
            it.copy(
                isMicMuted = newMuted,
                isListening = !newMuted,
                liveVoiceState = if (newMuted) LiveVoiceState.IDLE else LiveVoiceState.LISTENING
            )
        }
    }

    fun sendUserTurn(text: String) {
        pcmStreamPlayer.handleInterruption()
        audioCoordinator.stopAudio()
        val currentTurns = stateProvider().conversationTurns
        val newUserTurn = LiveConversationTurn("user", text)
        updateState {
            it.copy(
                liveStreamingUserText = text,
                isAudioLoading = true,
                isAiRespondingLive = true,
                liveVoiceState = LiveVoiceState.AI_THINKING,
                conversationTurns = currentTurns + newUserTurn
            )
        }
        session?.sendUserTurn(text)
    }

    fun endSession() {
        audioRecordHelper.stopRecording()
        pcmStreamPlayer.stop()
        session?.endSession()
    }

    fun close() {
        audioRecordHelper.stopRecording()
        pcmStreamPlayer.stop()
        session?.close()
        session = null
    }
}
