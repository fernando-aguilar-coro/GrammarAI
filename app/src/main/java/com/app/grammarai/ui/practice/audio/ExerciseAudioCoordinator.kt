package com.app.grammarai.ui.practice.audio

import android.content.Context
import android.util.Log
import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.audio.AudioPlayerHelper
import com.app.grammarai.core.audio.AudioRecordHelper
import com.app.grammarai.core.audio.SpeechRecognizerHelper
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.remote.GeminiApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Coordinates speech synthesis playback, sequential exercise audio,
 * and speech recognition recording without bloating the MVI ViewModel.
 */
class ExerciseAudioCoordinator(
    private val context: Context,
    private val geminiClient: GeminiApiClient,
    private val scope: CoroutineScope,
    private val onAudioLoadingChanged: (Boolean) -> Unit,
    private val onAudioPlayingChanged: (Boolean) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onSpeechResult: (String) -> Unit,
    val assetResolver: ExerciseAssetResolver = ExerciseAssetResolver(context),
    val textSanitizer: SpeechTextSanitizer = SpeechTextSanitizer
) {
    companion object {
        private const val TAG = "ExerciseAudioCoord"
    }

    val audioPlayer = AudioPlayerHelper()
    val speechHelper = SpeechRecognizerHelper(context)
    val audioRecordHelper = AudioRecordHelper(context)
    private var currentAudioJob: Job? = null

    private val currentLanguage: String
        get() = GrammarApplication.instance.preferencesManager.appLanguage
            .lowercase()
            .takeIf { it in listOf("es", "en") } ?: "en"

    // =========================================================================
    // Word Audio Playback
    // =========================================================================

    /**
     * Plays individual word audio for scrambled words or tapped vocabulary.
     * Looks up asset in audio/words/ directory from manifest, falling back to Android TTS.
     */
    fun playWordAudio(word: String, targetLocale: Locale? = null) {
        val cleanWord = textSanitizer.cleanWord(word)
        if (cleanWord.isBlank()) return

        stopAudio()
        onAudioLoadingChanged(false)

        val fullAssetPath = assetResolver.resolveWordAssetPath(cleanWord)
        val assetExists = assetResolver.assetExists(fullAssetPath)
        val effectiveLocale = targetLocale ?: if (textSanitizer.isLikelySpanish(cleanWord)) Locale("es", "ES") else Locale.US

        if (assetExists) {
            audioPlayer.playAssetAudio(
                assetPath = fullAssetPath,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = effectiveLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            speakTts(cleanWord, targetLocale = effectiveLocale)
        }
    }

    // =========================================================================
    // Option & Feedback Audio Playback
    // =========================================================================

    /**
     * Plays audio for a specific exercise option clip (ex_<id>_opt_<idx>.m4a).
     */
    fun playOptionAudio(exerciseId: String, optionIndex: Int, text: String, targetLocale: Locale? = null) {
        stopAudio()
        onAudioLoadingChanged(false)

        val lang = currentLanguage
        val optionAssetPath = assetResolver.resolveOptionAssetPath(exerciseId, optionIndex, lang)

        if (assetResolver.assetExists(optionAssetPath)) {
            audioPlayer.playAssetAudio(
                assetPath = optionAssetPath,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = targetLocale ?: Locale.US,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            speakTts(text, targetLocale = targetLocale ?: Locale.US)
        }
    }

    /**
     * Plays automated correct answer feedback audio when a user makes a mistake:
     * Plays pre-generated clip audio/<lang>/ex_<id>_correct.m4a (which already includes prefix).
     * Falls back to speaking "Respuesta correcta: ..." / "Correct answer: ..." via TTS.
     */
    fun playCorrectAnswerFeedback(exercise: GeneratedExercise, targetLocale: Locale? = null) {
        // Do not play correct answer audio for SELECT_PAIRS per user requirement
        if (exercise.type == ExerciseType.SELECT_PAIRS) return

        val answer = exercise.correctAnswer.ifBlank { exercise.targetPhrase }.trim()
        if (answer.isBlank()) return

        stopAudio()
        onAudioLoadingChanged(false)

        val lang = currentLanguage
        val feedbackAssetPath = assetResolver.resolveFeedbackAssetPath(exercise.id, lang)

        if (assetResolver.assetExists(feedbackAssetPath)) {
            audioPlayer.playAssetAudio(
                assetPath = feedbackAssetPath,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = targetLocale ?: Locale.US,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            val prefix = if (lang == "es") "Respuesta correcta: " else "Correct answer: "
            speakTts("$prefix$answer", targetLocale = targetLocale ?: Locale.US)
        }
    }

    // =========================================================================
    // Exercise Sequence Orchestration
    // =========================================================================

    /**
     * Sequential playback tailored per exercise type:
     * - THEORY: Plays lesson theory narration
     * - PRONUNCIATION / REPEAT_AFTER_ME / TRANSLATE: Plays generic prompt -> then target phrase
     * - SCRAMBLED_SENTENCE / SENTENCE_RECONSTRUCTION / SELECT_PAIRS: Plays prompt clip
     * - FILL_BLANK / MULTIPLE_CHOICE: Plays prompt clip (or specific prompt) -> then target phrase
     */
    fun playExerciseSequence(exercise: GeneratedExercise, targetLocale: Locale? = null) {
        stopAudio()
        val lang = currentLanguage

        when (exercise.type) {
            ExerciseType.FLASHCARDS,
            ExerciseType.FLASHCARD -> {
                val front = exercise.targetPhrase.ifBlank {
                    exercise.flashcards.firstOrNull()?.front.orEmpty()
                }
                playWordAudio(front)
            }
            ExerciseType.THEORY -> playTheorySequence(exercise, lang)
            ExerciseType.SCRAMBLE_LISTENING -> playScrambleListeningSequence(exercise, lang)
            ExerciseType.LISTENING_PASSAGE,
            ExerciseType.READING,
            ExerciseType.LONG_READING -> playPassageSequence(exercise, lang)
            ExerciseType.PRONUNCIATION,
            ExerciseType.REPEAT_AFTER_ME,
            ExerciseType.TRANSLATE -> playPromptAndTargetPhraseSequence(exercise, lang)
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.SELECT_PAIRS -> playPromptOnlySequence(exercise, lang)
            ExerciseType.MULTIPLE_CHOICE,
            ExerciseType.FILL_BLANK,
            ExerciseType.FILL_IN_BLANK -> playGenericPromptAndPhraseSequence(exercise, lang)
            else -> playPromptOnlySequence(exercise, lang)
        }
    }

    private fun playScrambleListeningSequence(exercise: GeneratedExercise, lang: String) {
        val targetText = exercise.targetPhrase.ifBlank { exercise.correctAnswer }.trim()
        if (targetText.isBlank()) return

        val phraseAsset = assetResolver.resolvePhraseAssetPath(exercise.id, lang)
        val specificAsset = assetResolver.resolveSpecificPromptAssetPath(exercise.id, lang)

        if (assetResolver.assetExists(phraseAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = phraseAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = Locale.US,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else if (assetResolver.assetExists(specificAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = specificAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = Locale.US,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            val speechText = if (targetText.contains("-")) {
                targetText.replace("-", ", ")
            } else {
                targetText
            }
            speakTts(speechText, targetLocale = Locale.US)
        }
    }

    private fun playPassageSequence(exercise: GeneratedExercise, lang: String) {
        val passageText = exercise.readingPassage.ifBlank { exercise.prompt }
        if (passageText.isNotBlank()) {
            speakTts(passageText, targetLocale = Locale.US)
        }
    }

    private fun playTheorySequence(exercise: GeneratedExercise, lang: String) {
        playExerciseAudio(
            exerciseId = exercise.id,
            exerciseType = exercise.type,
            text = exercise.readingPassage.ifBlank { exercise.prompt },
            targetLocale = if (lang == "es") Locale("es", "ES") else Locale.US
        )
    }

    /**
     * For PRONUNCIATION, REPEAT_AFTER_ME, and TRANSLATE:
     * 1. If specific exercise audio exists, plays it directly.
     * 2. Otherwise plays generic prompt, then target phrase.
     */
    private fun playPromptAndTargetPhraseSequence(exercise: GeneratedExercise, lang: String) {
        val targetText = exercise.targetPhrase.ifBlank { exercise.readingPassage }.ifBlank { exercise.correctAnswer }.trim()
        val phraseAsset = assetResolver.resolvePhraseAssetPath(exercise.id, lang)
        val specificAsset = assetResolver.resolveSpecificPromptAssetPath(exercise.id, lang)

        val phraseAssetExists = assetResolver.assetExists(phraseAsset)
        val specificAssetExists = assetResolver.assetExists(specificAsset)

        fun playTargetPhrase() {
            if (phraseAssetExists) {
                audioPlayer.playAssetAudio(
                    assetPath = phraseAsset,
                    fallbackText = "",
                    allowTtsFallback = false,
                    targetLocale = Locale.US,
                    onStart = { onAudioPlayingChanged(true) },
                    onCompletion = { onAudioPlayingChanged(false) }
                )
            } else if (specificAssetExists) {
                // For translate, pronunciation, and repeat_after_me, ex_<id>.m4a contains the target phrase
                audioPlayer.playAssetAudio(
                    assetPath = specificAsset,
                    fallbackText = "",
                    allowTtsFallback = false,
                    targetLocale = Locale.US,
                    onStart = { onAudioPlayingChanged(true) },
                    onCompletion = { onAudioPlayingChanged(false) }
                )
            } else if (targetText.isNotBlank()) {
                speakTts(targetText, targetLocale = Locale.US)
            } else {
                onAudioPlayingChanged(false)
            }
        }

        // If specific clip exists for this exercise, play it directly without generic prompt overhead
        if (specificAssetExists || phraseAssetExists) {
            playTargetPhrase()
            return
        }

        val genericPromptAsset = assetResolver.resolveGenericPromptAssetPath(exercise.type, lang)
        if (assetResolver.assetExists(genericPromptAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = genericPromptAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = if (lang == "es") Locale("es", "ES") else Locale.US,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { playTargetPhrase() }
            )
        } else {
            val defaultPrompt = when (exercise.type) {
                ExerciseType.TRANSLATE ->
                    if (lang == "es") "Traduce la siguiente frase." else "Translate the following phrase."
                ExerciseType.REPEAT_AFTER_ME ->
                    if (lang == "es") "Escucha y repite la frase." else "Listen and repeat the phrase."
                else ->
                    if (lang == "es") "Escucha y practica tu pronunciación." else "Listen and practice your pronunciation."
            }
            val promptLocale = if (lang == "es") Locale("es", "ES") else Locale.US
            speakTts(defaultPrompt, targetLocale = promptLocale, onCompletion = { playTargetPhrase() })
        }
    }

    /**
     * For SCRAMBLED_SENTENCE, SENTENCE_RECONSTRUCTION, and SELECT_PAIRS:
     * Plays the prompt audio (specific if exists, otherwise generic).
     */
    private fun playPromptOnlySequence(exercise: GeneratedExercise, lang: String) {
        val specificPromptAsset = assetResolver.resolveSpecificPromptAssetPath(exercise.id, lang)
        val promptLocale = if (lang == "es") Locale("es", "ES") else Locale.US

        if (assetResolver.assetExists(specificPromptAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = specificPromptAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = promptLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            val genericPromptAsset = assetResolver.resolveGenericPromptAssetPath(exercise.type, lang)
            val played = audioPlayer.playAssetAudio(
                assetPath = genericPromptAsset,
                fallbackText = exercise.prompt,
                allowTtsFallback = true,
                targetLocale = promptLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
            if (!played) {
                playExerciseAudio(
                    exerciseId = exercise.id,
                    exerciseType = exercise.type,
                    text = exercise.prompt,
                    targetLocale = promptLocale
                )
            }
        }
    }

    /**
     * For generic exercises with both prompt and target phrase (e.g. FILL_BLANK, MULTIPLE_CHOICE):
     */
    private fun playGenericPromptAndPhraseSequence(exercise: GeneratedExercise, lang: String) {
        val rawTargetPhrase = exercise.targetPhrase.ifBlank { exercise.readingPassage }.trim()
        val isAnswerLeak = exercise.type == ExerciseType.MULTIPLE_CHOICE &&
            exercise.correctAnswer.isNotBlank() &&
            rawTargetPhrase.equals(exercise.correctAnswer.trim(), ignoreCase = true)
        val validTargetPhrase = if (isAnswerLeak) "" else rawTargetPhrase

        val phraseAsset = assetResolver.resolvePhraseAssetPath(exercise.id, lang)
        val dialogueAsset = assetResolver.resolveDialogueAssetPath(exercise.id, lang)
        val phraseAssetExists = assetResolver.assetExists(phraseAsset)
        val dialogueAssetExists = assetResolver.assetExists(dialogueAsset)

        val hasTargetPhrase = (validTargetPhrase.isNotBlank() &&
            !validTargetPhrase.equals(exercise.prompt.trim(), ignoreCase = true)) ||
            phraseAssetExists || dialogueAssetExists

        fun playTargetPhrase() {
            if (phraseAssetExists) {
                audioPlayer.playAssetAudio(
                    assetPath = phraseAsset,
                    fallbackText = "",
                    allowTtsFallback = false,
                    targetLocale = Locale.US,
                    onStart = { onAudioPlayingChanged(true) },
                    onCompletion = { onAudioPlayingChanged(false) }
                )
            } else if (dialogueAssetExists) {
                audioPlayer.playAssetAudio(
                    assetPath = dialogueAsset,
                    fallbackText = "",
                    allowTtsFallback = false,
                    targetLocale = Locale.US,
                    onStart = { onAudioPlayingChanged(true) },
                    onCompletion = { onAudioPlayingChanged(false) }
                )
            } else if (validTargetPhrase.isNotBlank()) {
                playExerciseAudio(
                    exerciseId = exercise.id,
                    exerciseType = exercise.type,
                    text = validTargetPhrase,
                    targetLocale = Locale.US
                )
            } else {
                onAudioPlayingChanged(false)
            }
        }

        val onPromptCompleted: () -> Unit = {
            if (hasTargetPhrase) {
                playTargetPhrase()
            } else {
                onAudioPlayingChanged(false)
            }
        }

        val specificPromptAsset = assetResolver.resolveSpecificPromptAssetPath(exercise.id, lang)
        val genericPromptAsset = assetResolver.resolveGenericPromptAssetPath(exercise.type, lang)
        val promptLocale = if (lang == "es") Locale("es", "ES") else Locale.US

        if (assetResolver.assetExists(specificPromptAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = specificPromptAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = promptLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = onPromptCompleted
            )
        } else if (assetResolver.assetExists(genericPromptAsset)) {
            audioPlayer.playAssetAudio(
                assetPath = genericPromptAsset,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = promptLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = onPromptCompleted
            )
        } else if (exercise.prompt.isNotBlank()) {
            speakTts(exercise.prompt, targetLocale = promptLocale, onCompletion = onPromptCompleted)
        } else {
            onPromptCompleted()
        }
    }

    // =========================================================================
    // Exercise Audio Single Dispatcher
    // =========================================================================

    /**
     * Plays audio for an exercise:
     * 1. Checks pre-downloaded AAC asset ("audio/<lang>/<exerciseId>.m4a") with exact and normalized matching
     * 2. If it's a theory exercise and audio is missing, checks pre-generated spoken script ("theory_scripts/<scriptId>.txt")
     * 3. If no script exists, cleans raw markdown into natural spoken text
     * 4. Dispatches to MediaPlayer or native Android TTS with the appropriate language locale
     */
    fun playExerciseAudio(
        exerciseId: String,
        exerciseType: ExerciseType? = null,
        text: String = "",
        isRepeatAfterMe: Boolean = false,
        allowTtsFallback: Boolean = true,
        targetLocale: Locale? = null
    ) {
        stopAudio()
        onAudioLoadingChanged(false)

        val lang = currentLanguage
        val foundAssetPath = assetResolver.findAssetPathForExercise(exerciseId, exerciseType, lang)

        if (foundAssetPath != null) {
            val played = audioPlayer.playAssetAudio(
                assetPath = foundAssetPath,
                fallbackText = "",
                allowTtsFallback = false,
                targetLocale = targetLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
            if (played) return
        }

        // If theory audio is missing, attempt to read from pre-generated narration script for clean TTS
        var textToSpeak = text
        if (exerciseType == ExerciseType.THEORY) {
            val scriptContent = assetResolver.findTheoryScriptContent(exerciseId, lang)
            if (scriptContent != null && scriptContent.isNotBlank()) {
                textToSpeak = scriptContent
            } else {
                textToSpeak = textSanitizer.cleanMarkdownForSpeech(text)
            }
        }

        val cleanTextToSpeak = textSanitizer.prepareTtsText(textToSpeak)
        Log.d(TAG, "Playing with Android TTS (locale: $targetLocale): '${cleanTextToSpeak.take(120)}...'")

        if (allowTtsFallback && cleanTextToSpeak.isNotBlank()) {
            audioPlayer.playAudio(
                base64Pcm = null,
                fallbackText = cleanTextToSpeak,
                allowTtsFallback = true,
                targetLocale = targetLocale,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        } else {
            onAudioLoadingChanged(false)
            onAudioPlayingChanged(false)
        }
    }

    // =========================================================================
    // TTS & Remote Audio Generation
    // =========================================================================

    /**
     * Speaks arbitrary text using local Android TTS directly (offline, low latency).
     */
    fun speakTts(text: String, targetLocale: Locale? = null, onCompletion: () -> Unit = {}) {
        stopAudio()
        val clean = textSanitizer.prepareTtsText(text)
        if (clean.isBlank()) {
            onCompletion()
            return
        }
        audioPlayer.playAudio(
            base64Pcm = null,
            fallbackText = clean,
            allowTtsFallback = true,
            targetLocale = targetLocale,
            onStart = { onAudioPlayingChanged(true) },
            onCompletion = {
                onAudioPlayingChanged(false)
                onCompletion()
            }
        )
    }

    fun playText(text: String, allowTtsFallback: Boolean = false) {
        stopAudio()
        onAudioLoadingChanged(true)
        currentAudioJob = scope.launch {
            val audioBase64 = geminiClient.generateSpeechAudioBase64(text)
            onAudioLoadingChanged(false)
            audioPlayer.playAudio(
                base64Pcm = audioBase64,
                fallbackText = text,
                allowTtsFallback = allowTtsFallback,
                onStart = { onAudioPlayingChanged(true) },
                onCompletion = { onAudioPlayingChanged(false) }
            )
        }
    }

    fun playRawPcm(pcmBase64: String?, fallbackText: String, allowTtsFallback: Boolean = false) {
        stopAudio()
        audioPlayer.playAudio(
            base64Pcm = pcmBase64,
            fallbackText = fallbackText,
            allowTtsFallback = allowTtsFallback,
            onStart = { onAudioPlayingChanged(true) },
            onCompletion = { onAudioPlayingChanged(false) }
        )
    }

    fun playPcmBytes(pcmBytes: ByteArray, onDone: () -> Unit = {}) {
        stopAudio()
        audioRecordHelper.setMuted(true)
        audioPlayer.playPcmBytes(
            pcmBytes = pcmBytes,
            onStart = { onAudioPlayingChanged(true) },
            onCompletion = {
                audioRecordHelper.setMuted(false)
                onAudioPlayingChanged(false)
                onDone()
            }
        )
    }

    fun stopAudio() {
        currentAudioJob?.cancel()
        currentAudioJob = null
        audioPlayer.stop()
        audioRecordHelper.setMuted(false)
        onAudioLoadingChanged(false)
        onAudioPlayingChanged(false)
    }

    // =========================================================================
    // Recording & Speech Recognition Controls
    // =========================================================================

    fun toggleListening(currentlyListening: Boolean) {
        if (currentlyListening) {
            speechHelper.stopListening()
            audioRecordHelper.stopRecording()
            onListeningChanged(false)
        } else {
            stopAudio()
            speechHelper.startListening(
                onResult = { onSpeechResult(it) },
                onError = { onListeningChanged(false) },
                onListeningState = { onListeningChanged(it) }
            )
        }
    }

    fun toggleRawRecording(currentlyListening: Boolean, onChunk: (ByteArray) -> Unit) {
        if (currentlyListening) {
            audioRecordHelper.stopRecording()
            onListeningChanged(false)
        } else {
            stopAudio()
            onListeningChanged(true)
            audioRecordHelper.startRecording(
                onChunkReady = onChunk,
                onError = { onListeningChanged(false) }
            )
        }
    }

    fun stopRecording() {
        speechHelper.stopListening()
        audioRecordHelper.stopRecording()
        onListeningChanged(false)
    }

    fun release() {
        stopAudio()
        stopRecording()
        speechHelper.release()
        audioRecordHelper.release()
        audioPlayer.release()
    }
}
