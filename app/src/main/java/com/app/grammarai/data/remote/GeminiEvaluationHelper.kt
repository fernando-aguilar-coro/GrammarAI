package com.app.grammarai.data.remote

import com.app.grammarai.data.model.AiEvaluationResult
import com.app.grammarai.data.model.GeneratedExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiEvaluationHelper(
    private val httpClient: OkHttpClient,
    private val jsonMediaType: MediaType,
    private val apiKeyProvider: () -> String,
    private val modelName: String
) {
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    suspend fun generateErrorExplanation(
        exercise: GeneratedExercise,
        userAnswer: String,
        targetLanguage: String = "en/es"
    ): String = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank()) return@withContext exercise.explanation

        val isEnglishOnly = targetLanguage.equals("en", ignoreCase = true) ||
                            targetLanguage.equals("english", ignoreCase = true)
        val langRule = if (isEnglishOnly) {
            "in English"
        } else {
            "in bilingual EN/ES (English with Spanish explanation/support)"
        }

        val prompt = """
            You are a helpful English grammar tutor.
            The student was given this exercise:
            Target phrase: "${exercise.targetPhrase}"
            Correct answer: "${exercise.correctAnswer}"
            
            The student answered incorrectly: "$userAnswer"
            
            Briefly explain $langRule why the student's answer is wrong and why the correct answer is right. Keep it encouraging and short (1-2 sentences). Return the plain text response only.
        """.trimIndent()

        val requestBodyJson = """{"contents": [{"parts": [{"text": ${GeminiJsonUtils.escape(prompt)}}]}]}"""

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
            val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    GeminiJsonUtils.extractText(raw).trim()
                } else {
                    exercise.explanation
                }
            }
        } catch (e: Exception) {
            exercise.explanation
        }
    }

    suspend fun evaluateAndExplainAnswer(
        exercise: GeneratedExercise,
        userAnswer: String,
        lessonTitle: String? = null,
        targetLanguage: String = "en/es"
    ): AiEvaluationResult = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            return@withContext AiEvaluationResult(
                isCorrect = false,
                specificExplanation = exercise.explanation.ifBlank { "Offline mode: No AI explanation available." },
                generalExplanation = exercise.correctAnswer
            )
        }

        val contextPart = if (!lessonTitle.isNullOrBlank()) "Context of the lesson: \"$lessonTitle\"." else ""
        val targetPhrase = exercise.targetPhrase.ifBlank { exercise.prompt }

        val isEnglishOnly = targetLanguage.equals("en", ignoreCase = true) ||
                            targetLanguage.equals("english", ignoreCase = true)
        val explanationLangInstruction = if (isEnglishOnly) {
            "In clear, natural English, explain"
        } else {
            "In bilingual format (English explanation accompanied by clear Spanish translation and grammar guidance / EN-ES), explain"
        }
        val breakdownLangInstruction = if (isEnglishOnly) {
            "In English, provide a clear breakdown of the target phrase, explaining key grammar points, word meanings, and structural nuances."
        } else {
            "In bilingual format (EN-ES), provide a Spanish translation of the target phrase and explain key grammar points, word meanings, and structural nuances."
        }

        val prompt = """
            You are an expert, encouraging English language teacher evaluating a learner's answer.
            
            Exercise Question / Prompt: "${exercise.prompt}"
            Target context: "$targetPhrase"
            Expected standard answer: "${exercise.correctAnswer}"
            Student's response: "$userAnswer"
            $contextPart

            YOUR MISSION:
            1. Pedagogical & Semantic evaluation: Evaluate if the student's answer is acceptable, grammatically correct, and natural in English for this exercise.
               - IMPORTANT: If the student's answer is a valid synonym, contraction, alternate valid word order, or natural equivalent that conveys the correct meaning, MARK IT AS VALID ("isCorrect": true), even if it differs from the exact expected standard answer.
               - If it has actual grammatical errors, wrong vocabulary, or misses the core question, MARK IT AS INVALID ("isCorrect": false).
            2. Specific explanation ("specificExplanation"):
               - $explanationLangInstruction clearly and concisely what the student got wrong, OR if marked correct, why their alternate phrasing is valid and natural.
            3. General breakdown ("generalExplanation"):
               - $breakdownLangInstruction

            Return strictly a JSON object with this exact schema (no markdown fences, just pure JSON):
            {
              "isCorrect": boolean,
              "specificExplanation": "string",
              "generalExplanation": "string"
            }
        """.trimIndent()

        val requestBodyJson = """{"contents": [{"parts": [{"text": ${GeminiJsonUtils.escape(prompt)}}]}]}"""

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
            val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: ""
                    val extracted = GeminiJsonUtils.extractText(raw).trim()
                    parseAiEvaluation(extracted, exercise)
                } else {
                    AiEvaluationResult(
                        isCorrect = false,
                        specificExplanation = exercise.explanation.ifBlank { "Could not retrieve AI analysis." },
                        generalExplanation = exercise.correctAnswer
                    )
                }
            }
        } catch (e: Exception) {
            AiEvaluationResult(
                isCorrect = false,
                specificExplanation = exercise.explanation.ifBlank { "Could not connect to AI service." },
                generalExplanation = exercise.correctAnswer
            )
        }
    }

    private fun parseAiEvaluation(extractedText: String, exercise: GeneratedExercise): AiEvaluationResult {
        return try {
            val cleaned = extractedText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val element = jsonSerializer.parseToJsonElement(cleaned).jsonObject
            val isCorrect = element["isCorrect"]?.jsonPrimitive?.booleanOrNull ?: false
            val specific = element["specificExplanation"]?.jsonPrimitive?.content ?: exercise.explanation
            val general = element["generalExplanation"]?.jsonPrimitive?.content ?: exercise.correctAnswer
            AiEvaluationResult(
                isCorrect = isCorrect,
                specificExplanation = specific,
                generalExplanation = general
            )
        } catch (e: Exception) {
            val isCorrectMatch = Regex(""""isCorrect"\s*:\s*(true|false)""").find(extractedText)
            val isCorrect = isCorrectMatch?.groupValues?.get(1)?.toBoolean() ?: false
            val specificMatch = Regex(""""specificExplanation"\s*:\s*"((?:\\.|[^"\\])*)"""").find(extractedText)
            val specific = specificMatch?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
                ?: exercise.explanation.ifBlank { extractedText }
            val generalMatch = Regex(""""generalExplanation"\s*:\s*"((?:\\.|[^"\\])*)"""").find(extractedText)
            val general = generalMatch?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
                ?: exercise.correctAnswer

            AiEvaluationResult(
                isCorrect = isCorrect,
                specificExplanation = specific,
                generalExplanation = general
            )
        }
    }
}
