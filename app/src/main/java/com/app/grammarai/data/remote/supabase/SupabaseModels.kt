package com.app.grammarai.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SupabaseAuthRequest(
    @SerialName("provider") val provider: String = "google",
    @SerialName("id_token") val idToken: String,
    @SerialName("client_id") val clientId: String? = null
)

@Serializable
data class SupabaseAuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("user") val user: SupabaseAuthUser? = null
)

@Serializable
data class SupabaseAuthUser(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, JsonElement>? = null
)

@Serializable
data class SupabaseProfileRecord(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("native_language") val nativeLanguage: String? = null,
    @SerialName("primary_goal") val primaryGoal: String? = null,
    @SerialName("app_language") val appLanguage: String? = null,
    @SerialName("lesson_ids") val lessonIds: List<String> = emptyList(),
    @SerialName("lesson_scores") val lessonScores: Map<String, Int> = emptyMap(),
    @SerialName("ai_persona_prompt") val aiPersonaPrompt: String? = null,
    @SerialName("ai_reading_prompt") val aiReadingPrompt: String? = null,
    @SerialName("ai_suggestions_prompt") val aiSuggestionsPrompt: String? = null,
    @SerialName("last_sync") val lastSync: String? = null
)

@Serializable
data class SupabaseStreakRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("highest_streak") val highestStreak: Int = 0,
    @SerialName("target_lessons_count") val targetLessonsCount: Int = 5,
    @SerialName("completed_lessons_count") val completedLessonsCount: Int = 0,
    @SerialName("last_active_date") val lastActiveDate: String? = null
)

@Serializable
data class SupabaseCurrencyRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("xp") val xp: Int = 0
)

@Serializable
data class SupabaseSkillsRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("productive") val productive: Int = 0,
    @SerialName("pragmatics") val pragmatics: Int = 0,
    @SerialName("receptive") val receptive: Int = 0,
    @SerialName("structure") val structure: Int = 0,
    @SerialName("lexicon") val lexicon: Int = 0,
    @SerialName("overall_cefr") val overallCefr: String = "A1"
) {
    val pronunciation: Int get() = productive
    val fluency: Int get() = pragmatics
    val intonation: Int get() = receptive
    val grammar: Int get() = structure
    val vocabulary: Int get() = lexicon
}

@Serializable
data class SupabaseSyncPayload(
    val userId: String,
    val userEmail: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val appLanguage: String? = null,
    val nativeLanguage: String? = null,
    val primaryGoal: String? = null,
    val aiPersonaPrompt: String? = null,
    val aiReadingPrompt: String? = null,
    val aiSuggestionsPrompt: String? = null,
    val completedLessons: List<String> = emptyList(),
    val lessonScores: Map<String, Int> = emptyMap(),
    val xp: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val targetLessonsCount: Int = 5,
    val completedLessonsCount: Int = 0,
    val lastActiveDate: String? = null,
    val productive: Int = 0,
    val pragmatics: Int = 0,
    val receptive: Int = 0,
    val structure: Int = 0,
    val lexicon: Int = 0,
    val overallCefr: String = "A1"
)

@Serializable
data class SupabaseSyncResponse(
    val success: Boolean = false,
    val userId: String? = null,
    val syncedAt: String? = null,
    val error: String? = null,
    val profile: SupabaseProfileRecord? = null,
    val streak: SupabaseStreakRecord? = null,
    val currency: SupabaseCurrencyRecord? = null,
    val skills: SupabaseSkillsRecord? = null
)
