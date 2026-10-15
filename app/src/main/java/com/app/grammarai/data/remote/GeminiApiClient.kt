package com.app.grammarai.data.remote

import android.util.Base64
import com.app.grammarai.data.mock.MockCurriculumData
import com.app.grammarai.data.model.AiEvaluationResult
import com.app.grammarai.data.model.GeneratedExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiClient(
    private val apiKeyProvider: () -> String,
    private val suggestionsPromptProvider: (() -> String)? = null
) {
    companion object {
        private const val MODEL_TEXT_LITE = "gemini-3.1-flash-lite"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    val nativeAudioClient = GeminiNativeAudioClient(apiKeyProvider)
    private val evaluationHelper = GeminiEvaluationHelper(httpClient, jsonMediaType, apiKeyProvider, MODEL_TEXT_LITE)

    fun getApiKey(): String = apiKeyProvider()
    fun escapeJson(str: String): String = GeminiJsonUtils.escape(str)

    suspend fun testApiKey(candidateKey: String? = null): Boolean = withContext(Dispatchers.IO) {
        val key = candidateKey?.takeIf { it.isNotBlank() } ?: apiKeyProvider()
        if (key.isBlank()) return@withContext false
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
        val request = Request.Builder().url(url).get().build()
        try {
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun createLiveConversationSession(
        topic: String,
        persona: String,
        onModelStreaming: (String) -> Unit = {},
        onModelTurnComplete: (LiveConversationTurn, ByteArray) -> Unit = { _, _ -> },
        onError: (Throwable) -> Unit = {}
    ): GeminiLiveConversationSession {
        return GeminiLiveConversationSession(
            client = this,
            topic = topic,
            persona = persona,
            onModelStreaming = onModelStreaming,
            onModelTurnComplete = onModelTurnComplete,
            onError = onError
        )
    }

    suspend fun generateConversationSuggestions(
        topic: String,
        dialogueHistory: List<LiveConversationTurn>
    ): List<String> = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            return@withContext MockCurriculumData.getFallbackSuggestions(topic, dialogueHistory.lastOrNull()?.text ?: "").take(1)
        }

        val dialogueSummary = dialogueHistory.takeLast(4).joinToString("\n") { turn ->
            "${if (turn.role == "user") "Learner" else "Partner"}: ${turn.text}"
        }

        val customPrompt = suggestionsPromptProvider?.invoke()
            ?: "Suggest exactly 1 natural response (intermediate CEFR B1 level) that the user could choose or speak next."
        val prompt = """
            You are an English language coach assisting an intermediate student in an ongoing conversation.
            Topic: "$topic"
            Recent dialogue:
            $dialogueSummary

            $customPrompt
            Return strictly a JSON array of 1 string:
            ["Your suggested reply here"]
        """.trimIndent()

        val requestBodyJson = """{"contents": [{"parts": [{"text": ${GeminiJsonUtils.escape(prompt)}}]}]}"""

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_TEXT_LITE:generateContent?key=$key"
            val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val suggestions = GeminiJsonUtils.parseStringList(GeminiJsonUtils.extractText(raw))
                    if (suggestions.isNotEmpty()) suggestions.take(1) 
                    else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun generateSpeechAudioBase64(textToSpeak: String): String? {
        return nativeAudioClient.synthesizeSpeech(textToSpeak)
    }

    suspend fun transcribeUserAudioPcm(pcmBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank() || pcmBytes.isEmpty()) return@withContext ""
        
        // Build WAV header for 16kHz, Mono, 16-bit PCM
        val totalDataLen = pcmBytes.size + 36
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = ((totalDataLen shr 8) and 0xff).toByte(); header[6] = ((totalDataLen shr 16) and 0xff).toByte(); header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0; header[22] = 1; header[23] = 0
        header[24] = (16000 and 0xff).toByte(); header[25] = ((16000 shr 8) and 0xff).toByte(); header[26] = ((16000 shr 16) and 0xff).toByte(); header[27] = ((16000 shr 24) and 0xff).toByte()
        val byteRate = 16000 * 1 * 16 / 8
        header[28] = (byteRate and 0xff).toByte(); header[29] = ((byteRate shr 8) and 0xff).toByte(); header[30] = ((byteRate shr 16) and 0xff).toByte(); header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmBytes.size and 0xff).toByte(); header[41] = ((pcmBytes.size shr 8) and 0xff).toByte(); header[42] = ((pcmBytes.size shr 16) and 0xff).toByte(); header[43] = ((pcmBytes.size shr 24) and 0xff).toByte()
        
        val wavBytes = header + pcmBytes
        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        
        val requestBodyJson = """{
            "contents": [{
                "parts": [
                    {"inlineData": {"mimeType": "audio/wav", "data": "$base64Audio"}},
                    {"text": "Transcribe this audio precisely. Do not add any commentary. Just output the exact text spoken in its original language."}
                ]
            }]
        }"""
        
        // Fast lightweight transcription using regular 2.5-flash
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
        val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()
        
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    GeminiJsonUtils.extractText(response.body?.string() ?: "").trim()
                } else ""
            }
        } catch (e: Exception) { "" }
    }

    suspend fun generateErrorExplanation(
        exercise: GeneratedExercise,
        userAnswer: String,
        targetLanguage: String = "en/es"
    ): String {
        return evaluationHelper.generateErrorExplanation(exercise, userAnswer, targetLanguage)
    }

    suspend fun evaluateAndExplainAnswer(
        exercise: GeneratedExercise,
        userAnswer: String,
        lessonTitle: String? = null,
        targetLanguage: String = "en/es"
    ): AiEvaluationResult {
        return evaluationHelper.evaluateAndExplainAnswer(exercise, userAnswer, lessonTitle, targetLanguage)
    }

    internal suspend fun executeTextModelCall(requestBodyJson: String): String = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank()) return@withContext ""

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_TEXT_LITE:generateContent?key=$key"
        val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    GeminiJsonUtils.extractText(response.body?.string() ?: "")
                } else ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getFallback7Exercises(lessonId: String, lessonTitle: String): List<GeneratedExercise> {
        return MockCurriculumData.getMockExercises(lessonId)
    }

    fun extractTextFromGeminiResponse(raw: String): String {
        return GeminiJsonUtils.extractText(raw)
    }
}
