package com.app.grammarai.data.remote

import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

data class LiveConversationTurn(
    val role: String,
    val text: String,
    val audioBase64: String? = null
)

class GeminiLiveConversationSession(
    private val client: GeminiApiClient,
    val topic: String,
    val persona: String,
    val maxTurns: Int = 8,
    private val onModelStreaming: (String) -> Unit = {},
    private val onUserStreaming: (String) -> Unit = {},
    private val onAudioChunkStreaming: (ByteArray) -> Unit = {},
    private val onInterrupted: () -> Unit = {},
    private val onModelTurnComplete: (LiveConversationTurn, ByteArray) -> Unit = { _, _ -> },
    private val onConversationEnded: (List<LiveConversationTurn>) -> Unit = {},
    private val onError: (Throwable) -> Unit = {}
) {
    companion object {
        private const val TAG = "LiveConversationSession"
    }

    private val _history = mutableListOf<LiveConversationTurn>()
    val history: List<LiveConversationTurn> get() = _history.toList()

    private val currentAudioBuffer = ByteArrayOutputStream()
    private val currentTranscript = StringBuilder()
    private val currentUserTranscript = StringBuilder()
    private var isSessionConnected = false
    private val pendingTurns = mutableListOf<String>()
    private var isCompleted = false

    fun startSession(initialGreetingPrompt: String = "") {
        _history.clear()
        pendingTurns.clear()
        isCompleted = false
        val key = client.getApiKey()
        if (key.isBlank()) {
            val errTurn = LiveConversationTurn("model", "⚠️ [Configuration Error]: Empty Gemini API Key in PreferencesManager.")
            _history.add(errTurn)
            onModelTurnComplete(errTurn, ByteArray(0))
            return
        }

        val prefs = com.app.grammarai.GrammarApplication.instance.preferencesManager
        val customPrompt = runCatching {
            prefs.aiPersonaPrompt
        }.getOrDefault("You are a warm, helpful native English conversation partner.")

        val userName = prefs.userName.ifBlank { "neko" }
        val nativeLanguage = prefs.nativeLanguage.ifBlank { "Spanish" }
        val englishLevel = prefs.englishLevel.ifBlank { "Intermediate (B1 - B2)" }
        val primaryGoal = prefs.primaryGoal.ifBlank { "Speaking Fluency" }
        val targetSituations = prefs.targetSituations.ifBlank { "Daily Life & Casual Chats" }

        val isSpanish = nativeLanguage.contains("Span", ignoreCase = true) ||
                        nativeLanguage.contains("Español", ignoreCase = true) ||
                        prefs.appLanguage.equals("es", ignoreCase = true)
        val langContext = if (isSpanish) {
            "Language Rule: The student's native language is Spanish. All greetings, explanations of concepts, instructions, and tutor guidance MUST be delivered in Spanish, while student practice sentences are in English."
        } else {
            "Language Rule: The student prefers English immersion. Explanations, instructions, and coaching MUST be delivered in clear English."
        }

        val studentContext = "Student Profile: Name: $userName, Native Language: $nativeLanguage, Current English Level: $englishLevel, Primary Goal: $primaryGoal, Favorite Situations to talk about: $targetSituations. Always tailor sentence complexity, vocabulary, and speaking pace specifically to the student's level ($englishLevel)."
        val instruction = "$customPrompt $studentContext $langContext Specific role and task: $persona. Topic: '$topic'. Guide $userName step-by-step through this $maxTurns-turn interactive dialogue. Keep each response natural and engaging (2-3 spoken sentences). Conclude with a warm farewell on the final turn."

        Log.d(TAG, "Starting Live session. Waiting for connection...")
        val socket = client.nativeAudioClient.connectPersistentLiveSession(
            systemInstruction = instruction,
            onConnected = {
                Log.d(TAG, "Live Session connected successfully. Initializing AI...")
                isSessionConnected = true
                currentAudioBuffer.reset()
                currentTranscript.clear()
                currentUserTranscript.clear()
                
                val openingMessage = if (initialGreetingPrompt.isNotBlank()) {
                    initialGreetingPrompt
                } else {
                    if (isSpanish) {
                        "Saluda amigablemente en tu personaje a $userName en español y comienza nuestra lección sobre '$topic'."
                    } else {
                        "Please greet me in character and start our conversation about '$topic'."
                    }
                }
                
                if (pendingTurns.isEmpty()) {
                    client.nativeAudioClient.sendTurn(openingMessage)
                } else {
                    pendingTurns.forEach { client.nativeAudioClient.sendTurn(it) }
                    pendingTurns.clear()
                }
            },
            onTranscriptChunk = { chunk ->
                currentTranscript.append(chunk)
                onModelStreaming(currentTranscript.toString())
            },
            onUserTranscriptChunk = { chunk ->
                currentUserTranscript.append(chunk)
                onUserStreaming(currentUserTranscript.toString())
            },
            onAudioChunk = { pcm ->
                currentAudioBuffer.write(pcm)
                onAudioChunkStreaming(pcm)
            },
            onTurnComplete = {
                finalizeTurn()
            },
            onInterrupted = {
                Log.d(TAG, "GeminiLiveConversationSession received onInterrupted")
                currentAudioBuffer.reset()
                currentTranscript.clear()
                onInterrupted()
            },
            onError = { t ->
                Log.e(TAG, "Live Session error: ${t.message}")
                isSessionConnected = false
                val errTurn = LiveConversationTurn("model", "⚠️ [Live Connection Error]: ${t.message ?: "Failed to connect to Gemini Live"}")
                _history.add(errTurn)
                onModelTurnComplete(errTurn, ByteArray(0))
                onError(t)
            }
        )

        if (socket == null) {
            val errTurn = LiveConversationTurn("model", "⚠️ [Error]: Could not initialize WebSocket connection.")
            _history.add(errTurn)
            onModelTurnComplete(errTurn, ByteArray(0))
        }
    }

    fun sendUserTurn(userUtterance: String, isInternalCommand: Boolean = false) {
        if (isCompleted) return
        if (!isInternalCommand && userUtterance.isNotBlank()) {
            if (_history.none { it.role == "user" && it.text == userUtterance }) {
                _history.add(LiveConversationTurn("user", userUtterance))
            }
        }
        if (!isSessionConnected) {
            pendingTurns.add(userUtterance)
            return
        }

        currentAudioBuffer.reset()
        currentTranscript.clear()
        currentUserTranscript.clear()
        val sent = client.nativeAudioClient.sendTurn(userUtterance)
        if (!sent && !isInternalCommand) {
            val errTurn = LiveConversationTurn("model", "⚠️ [Error]: Failed to send message over active WebSocket.")
            _history.add(errTurn)
            onModelTurnComplete(errTurn, ByteArray(0))
        }
    }

    fun streamAudioChunk(pcmBytes: ByteArray) {
        if (!isSessionConnected || isCompleted) return
        client.nativeAudioClient.sendAudioChunk(pcmBytes)
    }

    fun commitAudioTurn() {
        // Full-duplex: Gemini VAD handles turn transitions naturally
        val userSaid = currentUserTranscript.toString().trim()
        if (userSaid.isNotBlank()) {
            _history.add(LiveConversationTurn("user", userSaid))
            currentUserTranscript.clear()
        }
    }

    private fun finalizeTurn() {
        // If there was a pending user transcript from live speech, save it
        val userText = currentUserTranscript.toString().trim()
        if (userText.isNotBlank()) {
            _history.add(LiveConversationTurn("user", userText))
            currentUserTranscript.clear()
        }

        val fullText = currentTranscript.toString().trim()
        val pcmBytes = currentAudioBuffer.toByteArray()
        val b64 = if (pcmBytes.isNotEmpty()) Base64.encodeToString(pcmBytes, Base64.NO_WRAP) else null

        if (fullText.isNotBlank()) {
            val modelTurn = LiveConversationTurn("model", fullText, b64)
            _history.add(modelTurn)
            onModelTurnComplete(modelTurn, pcmBytes)
        }
        
        currentTranscript.clear()
        currentAudioBuffer.reset()

        // Check if dialogue reached target turn count
        val modelTurnCount = _history.count { it.role == "model" && !it.text.startsWith("⚠️") }
        if (modelTurnCount >= maxTurns && !isCompleted) {
            Log.d(TAG, "Conversation reached target turns ($maxTurns). Awaiting playback before concluding dialogue...")
        }
    }

    fun isTargetTurnReached(): Boolean {
        val modelTurnCount = _history.count { it.role == "model" && !it.text.startsWith("⚠️") }
        return modelTurnCount >= maxTurns
    }

    fun addUserTranscriptToHistory(text: String) {
        if (text.isNotBlank()) {
            _history.add(LiveConversationTurn("user", text))
        }
    }

    fun endSession() {
        if (isCompleted) return
        isCompleted = true
        Log.d(TAG, "Manually ending Live Session")
        val userText = currentUserTranscript.toString().trim()
        if (userText.isNotBlank()) {
            _history.add(LiveConversationTurn("user", userText))
            currentUserTranscript.clear()
        }
        close()
        onConversationEnded(_history.toList())
    }

    fun close() {
        Log.d(TAG, "Closing Live Session")
        isSessionConnected = false
        client.nativeAudioClient.close()
    }
}
