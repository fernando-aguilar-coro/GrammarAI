package com.app.grammarai.core.translation

import android.util.Log
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OnDeviceTranslationManager {

    companion object {
        private const val TAG = "OnDeviceTranslation"

        /**
         * Checks if an exercise type is compatible with automated translation.
         * Excludes matching pairs and scrambled sentences per learning design requirements.
         */
        fun isExerciseCompatible(exercise: GeneratedExercise?): Boolean {
            if (exercise == null) return false
            return when (exercise.type) {
                ExerciseType.SELECT_PAIRS -> false
                ExerciseType.SCRAMBLED_SENTENCE,
                ExerciseType.SENTENCE_RECONSTRUCTION,
                ExerciseType.SCRAMBLE_LISTENING -> false
                ExerciseType.CONVERSATION_DIALOGUE -> false
                ExerciseType.THEORY -> false
                ExerciseType.FLASHCARDS,
                ExerciseType.FLASHCARD -> false
                else -> true
            }
        }

        /**
         * Extracts the primary text to be translated for the exercise.
         */
        fun extractTranslatableText(exercise: GeneratedExercise): String {
            val sentence = when {
                exercise.type == ExerciseType.PRONUNCIATION || exercise.type == ExerciseType.REPEAT_AFTER_ME -> {
                    exercise.targetPhrase.ifBlank { exercise.prompt }
                }
                exercise.readingPassage.isNotBlank() && !exercise.readingPassage.trim().equals(exercise.prompt.trim(), ignoreCase = true) -> {
                    exercise.readingPassage
                }
                exercise.targetPhrase.isNotBlank() && !exercise.targetPhrase.trim().equals(exercise.prompt.trim(), ignoreCase = true) -> {
                    exercise.targetPhrase
                }
                else -> null
            }

            return if (sentence != null && sentence.isNotBlank()) {
                sentence.trim()
            } else {
                exercise.prompt.trim()
            }
        }
    }

    private val translators = ConcurrentHashMap<String, Translator>()

    fun mapLanguageToMlKitCode(languageName: String): String {
        return when (languageName.trim().lowercase()) {
            "spanish", "español", "es" -> TranslateLanguage.SPANISH
            "french", "français", "fr" -> TranslateLanguage.FRENCH
            "german", "deutsch", "de" -> TranslateLanguage.GERMAN
            "portuguese", "português", "pt" -> TranslateLanguage.PORTUGUESE
            "italian", "italiano", "it" -> TranslateLanguage.ITALIAN
            "japanese", "日本語", "ja" -> TranslateLanguage.JAPANESE
            "chinese", "中文", "zh" -> TranslateLanguage.CHINESE
            "russian", "русский", "ru" -> TranslateLanguage.RUSSIAN
            else -> TranslateLanguage.SPANISH
        }
    }

    private fun getOrCreateTranslator(targetLangCode: String): Translator {
        return translators.getOrPut(targetLangCode) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLangCode)
                .build()
            Translation.getClient(options)
        }
    }

    /**
     * Translates English text to the specified target language using Google ML Kit on-device.
     */
    suspend fun translate(
        text: String,
        targetLanguageName: String = "Spanish"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val targetLangCode = mapLanguageToMlKitCode(targetLanguageName)
        val translator = getOrCreateTranslator(targetLangCode)

        try {
            val conditions = DownloadConditions.Builder().build()
            downloadModel(translator, conditions)
            val translated = translateInternal(translator, text)
            Result.success(translated)
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit translation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadModel(translator: Translator, conditions: DownloadConditions) =
        suspendCancellableCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    private suspend fun translateInternal(translator: Translator, text: String) =
        suspendCancellableCoroutine<String> { cont ->
            translator.translate(text)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
