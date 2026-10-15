package com.app.grammarai.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GeminiJsonUtils {

    private val json = Json { ignoreUnknownKeys = true }

    fun extractText(rawJson: String): String {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            text ?: ""
        } catch (e: Exception) {
            val match = Regex(""""text":\s*"((?:\\.|[^"\\])*)"""").find(rawJson)
            match?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
        }
    }

    fun extractInlineAudio(rawJson: String): String? {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val inlineData = parts?.firstOrNull()?.jsonObject?.get("inlineData")?.jsonObject
            inlineData?.get("data")?.jsonPrimitive?.content
        } catch (e: Exception) {
            val match = Regex(""""data":\s*"([^"]+)"""").find(rawJson)
            match?.groupValues?.get(1)
        }
    }

    fun parseStringList(jsonText: String): List<String> {
        return try {
            val cleaned = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = json.parseToJsonElement(cleaned).jsonArray
            parsed.map { it.jsonPrimitive.content.trim() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun escape(str: String): String {
        return buildString {
            append('"')
            for (c in str) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append('"')
        }
    }
}
