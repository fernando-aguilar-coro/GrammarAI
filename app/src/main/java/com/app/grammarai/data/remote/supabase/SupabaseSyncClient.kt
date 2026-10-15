package com.app.grammarai.data.remote.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SupabaseSyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signInWithGoogleIdToken(idToken: String): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${SupabaseConfig.PROJECT_URL}/auth/v1/token?grant_type=id_token"
            val authRequest = SupabaseAuthRequest(
                provider = "google",
                idToken = idToken,
                clientId = SupabaseConfig.GOOGLE_WEB_CLIENT_ID
            )
            val bodyJson = json.encodeToString(authRequest)
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(jsonMediaType))
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Supabase Auth failed [HTTP ${response.code}]: $body")
                }
                json.decodeFromString<SupabaseAuthResponse>(body)
            }
        }
    }

    suspend fun fetchUserData(userId: String, accessToken: String?): Result<SupabaseSyncResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val authBearer = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer ${SupabaseConfig.ANON_KEY}"
            
            val profile = executeGet<List<SupabaseProfileRecord>>(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/profiles?id=eq.$userId&select=*",
                authBearer = authBearer
            ).firstOrNull()

            val streak = executeGet<List<SupabaseStreakRecord>>(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_streaks?user_id=eq.$userId&select=*",
                authBearer = authBearer
            ).firstOrNull()

            val currency = executeGet<List<SupabaseCurrencyRecord>>(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_currencies?user_id=eq.$userId&select=*",
                authBearer = authBearer
            ).firstOrNull()

            val skills = executeGet<List<SupabaseSkillsRecord>>(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_skills?user_id=eq.$userId&select=*",
                authBearer = authBearer
            ).firstOrNull()

            val isoDate = getIsoTimestamp()
            SupabaseSyncResponse(
                success = true,
                userId = userId,
                syncedAt = isoDate,
                profile = profile,
                streak = streak,
                currency = currency,
                skills = skills
            )
        }
    }

    suspend fun upsertUserData(payload: SupabaseSyncPayload, accessToken: String?): Result<SupabaseSyncResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val authBearer = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer ${SupabaseConfig.ANON_KEY}"
            val isoDate = getIsoTimestamp()

            // 1. Upsert Profile
            val profileJson = buildJsonObject {
                put("id", payload.userId)
                payload.username?.let { put("username", it) }
                payload.avatarUrl?.let { put("avatar_url", it) }
                payload.appLanguage?.let { put("app_language", it) }
                payload.nativeLanguage?.let { put("native_language", it) }
                payload.primaryGoal?.let { put("primary_goal", it) }
                payload.aiPersonaPrompt?.let { put("ai_persona_prompt", it) }
                payload.aiReadingPrompt?.let { put("ai_reading_prompt", it) }
                payload.aiSuggestionsPrompt?.let { put("ai_suggestions_prompt", it) }
                putJsonArray("lesson_ids") {
                    payload.completedLessons.forEach { add(it) }
                }
                putJsonObject("lesson_scores") {
                    payload.lessonScores.forEach { (lessonId, score) ->
                        put(lessonId, score)
                    }
                }
                put("last_sync", isoDate)
                put("updated_at", isoDate)
            }.toString()

            executeUpsert(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/profiles",
                jsonBody = profileJson,
                authBearer = authBearer
            )

            // 2. Upsert Streaks / Goals
            val streakJson = buildJsonObject {
                put("user_id", payload.userId)
                put("current_streak", payload.currentStreak)
                put("highest_streak", payload.highestStreak)
                put("target_lessons_count", payload.targetLessonsCount)
                put("completed_lessons_count", payload.completedLessonsCount)
                payload.lastActiveDate?.let { put("last_active_date", it) }
                put("updated_at", isoDate)
            }.toString()

            executeUpsert(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_streaks",
                jsonBody = streakJson,
                authBearer = authBearer
            )

            // 3. Upsert Currencies (XP)
            val currencyJson = buildJsonObject {
                put("user_id", payload.userId)
                put("xp", payload.xp)
                put("updated_at", isoDate)
            }.toString()

            executeUpsert(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_currencies",
                jsonBody = currencyJson,
                authBearer = authBearer
            )

            // 4. Upsert Skills
            val skillsJson = buildJsonObject {
                put("user_id", payload.userId)
                put("productive", payload.productive)
                put("pragmatics", payload.pragmatics)
                put("receptive", payload.receptive)
                put("structure", payload.structure)
                put("lexicon", payload.lexicon)
                put("overall_cefr", payload.overallCefr)
                put("updated_at", isoDate)
            }.toString()

            executeUpsert(
                url = "${SupabaseConfig.PROJECT_URL}/rest/v1/user_skills",
                jsonBody = skillsJson,
                authBearer = authBearer
            )

            SupabaseSyncResponse(
                success = true,
                userId = payload.userId,
                syncedAt = isoDate
            )
        }
    }

    private inline fun <reified T> executeGet(url: String, authBearer: String): T {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("apikey", SupabaseConfig.ANON_KEY)
            .header("Authorization", authBearer)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Supabase GET failed [HTTP ${response.code}] on $url: $body")
            }
            return json.decodeFromString<T>(body)
        }
    }

    private fun executeUpsert(url: String, jsonBody: String, authBearer: String) {
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .header("apikey", SupabaseConfig.ANON_KEY)
            .header("Authorization", authBearer)
            .header("Prefer", "resolution=merge-duplicates")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                throw IllegalStateException("Supabase UPSERT failed [HTTP ${response.code}] on $url: $body")
            }
        }
    }

    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
