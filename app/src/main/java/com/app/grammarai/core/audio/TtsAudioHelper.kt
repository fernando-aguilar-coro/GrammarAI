package com.app.grammarai.core.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.app.grammarai.GrammarApplication
import java.util.Locale

class TtsAudioHelper(
    private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsAudioHelper"
        private const val MAX_CHUNK_LENGTH = 3000
    }

    private var tts: TextToSpeech? = null
    var isReady: Boolean = false
        private set
    private var isInitializing = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingInitCallbacks = mutableListOf<() -> Unit>()

    init {
        ensureInitialized()
    }

    fun ensureInitialized(onReady: (() -> Unit)? = null) {
        if (isReady && tts != null) {
            onReady?.invoke()
            return
        }

        if (onReady != null) {
            synchronized(pendingInitCallbacks) {
                pendingInitCallbacks.add(onReady)
            }
        }

        if (isInitializing) return

        isInitializing = true
        Thread {
            try {
                var newTts: TextToSpeech? = null
                newTts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        applyDefaultLanguage(newTts)
                        isReady = true
                        Log.d(TAG, "TTS initialized successfully in background")
                        dispatchPendingCallbacks()
                    } else {
                        Log.w(TAG, "TTS initialization failed with status $status")
                    }
                    isInitializing = false
                }
                tts = newTts
            } catch (e: Exception) {
                isInitializing = false
                Log.w(TAG, "Background TTS initialization error: ${e.message}")
            }
        }.start()
    }

    private fun dispatchPendingCallbacks() {
        val callbacks = synchronized(pendingInitCallbacks) {
            val copy = ArrayList(pendingInitCallbacks)
            pendingInitCallbacks.clear()
            copy
        }
        mainHandler.post {
            callbacks.forEach { it.invoke() }
        }
    }

    private fun applyDefaultLanguage(engine: TextToSpeech?) {
        val appLang = runCatching {
            GrammarApplication.instance.preferencesManager.appLanguage.lowercase()
        }.getOrDefault("es")

        val targetLocale = if (appLang == "es") Locale("es", "ES") else Locale.US
        val res = engine?.setLanguage(targetLocale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (targetLocale.language == "es") {
                engine?.setLanguage(Locale("es"))
            } else {
                engine?.setLanguage(Locale.US)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyDefaultLanguage(tts)
            isReady = true
            Log.d(TAG, "TTS initialized successfully in onInit")
            dispatchPendingCallbacks()
        }
    }

    /**
     * Determines whether text should be spoken in Spanish or English
     * based on user preferences and lexical content.
     */
    fun determineLocaleForText(text: String): Locale {
        val appLang = runCatching {
            GrammarApplication.instance.preferencesManager.appLanguage.lowercase()
        }.getOrDefault("es")

        // 1. Explicit Spanish diacritics
        if (text.any { it in "áéíóúñÁÉÍÓÚÑ¿¡" }) {
            return Locale("es", "ES")
        }

        // 2. Common Spanish lexical tokens
        val commonSpanishWords = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "en", "para", "por",
            "con", "como", "que", "qué", "es", "son", "fue", "ser", "estar", "este", "esta", "estos",
            "estas", "verbo", "pronunciación", "ejemplo", "oración", "sujeto", "tiempo", "lección",
            "hola", "significa", "adjetivo", "sustantivo", "regla", "forma", "completa", "selecciona"
        )
        val tokens = text.lowercase().split(Regex("[^a-záéíóúñ]+")).filter { it.isNotBlank() }
        val spanishCount = tokens.count { it in commonSpanishWords }

        if (spanishCount >= 2 || (tokens.size <= 4 && spanishCount >= 1)) {
            return Locale("es", "ES")
        }

        if (appLang == "es" && tokens.size > 8 && spanishCount > 0) {
            return Locale("es", "ES")
        }

        return Locale.US
    }

    private fun setEngineLocale(targetLocale: Locale) {
        runCatching {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (targetLocale.language == "es") {
                    val fallbackEs = Locale("es")
                    val res2 = tts?.setLanguage(fallbackEs)
                    if (res2 == TextToSpeech.LANG_MISSING_DATA || res2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Spanish TTS not supported on device; keeping current language")
                    }
                } else {
                    tts?.setLanguage(Locale.US)
                }
            }
        }
    }

    fun speak(
        text: String,
        token: Long,
        currentTokenProvider: () -> Long,
        targetLocale: Locale? = null,
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ) {
        val sanitized = text.replace(Regex("_+"), "blank").trim()
        if (sanitized.isBlank() || token != currentTokenProvider()) {
            mainHandler.post { onCompletion() }
            return
        }

        if (!isReady || tts == null) {
            ensureInitialized {
                if (token == currentTokenProvider()) {
                    speak(sanitized, token, currentTokenProvider, targetLocale, onStart, onCompletion)
                }
            }
            return
        }

        // Apply appropriate language before speaking
        val effectiveLocale = targetLocale ?: determineLocaleForText(sanitized)
        setEngineLocale(effectiveLocale)

        val chunks = splitIntoSpeechChunks(sanitized, MAX_CHUNK_LENGTH)
        if (chunks.isEmpty()) {
            mainHandler.post { onCompletion() }
            return
        }

        val baseUtteranceId = "tts_${System.currentTimeMillis()}"
        val lastUtteranceId = "${baseUtteranceId}_${chunks.size - 1}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            private var hasNotifiedStart = false

            override fun onStart(id: String?) {
                if (token == currentTokenProvider() && !hasNotifiedStart) {
                    hasNotifiedStart = true
                    mainHandler.post { onStart() }
                }
            }

            override fun onDone(id: String?) {
                if (token == currentTokenProvider() && id == lastUtteranceId) {
                    mainHandler.post { onCompletion() }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                Log.e(TAG, "Android TTS playback error for utterance: $id")
                if (token == currentTokenProvider()) {
                    mainHandler.post {
                        onCompletion()
                    }
                }
            }
        })

        for ((index, chunk) in chunks.withIndex()) {
            val utteranceId = "${baseUtteranceId}_$index"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(chunk, queueMode, null, utteranceId)
        }
    }

    private fun splitIntoSpeechChunks(text: String, maxChunkSize: Int): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        val current = StringBuilder()

        for (para in paragraphs) {
            if (current.length + para.length + 2 <= maxChunkSize) {
                if (current.isNotEmpty()) current.append("\n\n")
                current.append(para)
            } else {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                if (para.length <= maxChunkSize) {
                    current.append(para)
                } else {
                    // Split long paragraph by sentence boundaries
                    val sentences = para.split(Regex("(?<=[.!?])\\s+"))
                    for (sentence in sentences) {
                        if (current.length + sentence.length + 1 <= maxChunkSize) {
                            if (current.isNotEmpty()) current.append(" ")
                            current.append(sentence)
                        } else {
                            if (current.isNotEmpty()) {
                                chunks.add(current.toString().trim())
                                current.clear()
                            }
                            current.append(sentence.take(maxChunkSize))
                        }
                    }
                }
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun release() {
        stop()
        runCatching { tts?.shutdown(); tts = null }
        isReady = false
    }
}

