package com.app.grammarai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.app.grammarai.BuildConfig
import com.app.grammarai.data.model.SkillCategory
import com.app.grammarai.data.model.SkillMetrics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("grammar_ai_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "neko") ?: "neko"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userId: String
        get() {
            val existing = prefs.getString(KEY_USER_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, newId).apply()
            return newId
        }
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var supabaseAccessToken: String?
        get() = prefs.getString(KEY_SUPABASE_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SUPABASE_ACCESS_TOKEN, value).apply()

    var supabaseRefreshToken: String?
        get() = prefs.getString(KEY_SUPABASE_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SUPABASE_REFRESH_TOKEN, value).apply()

    var userAvatarUrl: String?
        get() = prefs.getString(KEY_USER_AVATAR_URL, null)
        set(value) = prefs.edit().putString(KEY_USER_AVATAR_URL, value).apply()

    val isAuthenticated: Boolean
        get() = !supabaseAccessToken.isNullOrBlank()

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_SUPABASE_ACCESS_TOKEN)
            .remove(KEY_SUPABASE_REFRESH_TOKEN)
            .remove(KEY_USER_AVATAR_URL)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var isCloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, value).apply()

    var nativeLanguage: String
        get() = prefs.getString(KEY_NATIVE_LANG, "Spanish") ?: "Spanish"
        set(value) = prefs.edit().putString(KEY_NATIVE_LANG, value).apply()

    var primaryGoal: String
        get() = prefs.getString(KEY_GOAL, "Speaking Fluency") ?: "Speaking Fluency"
        set(value) = prefs.edit().putString(KEY_GOAL, value).apply()

    var apiKey: String
        get() {
            val savedKey = prefs.getString(KEY_API_KEY, "")?.trim() ?: ""
            if (savedKey.isNotEmpty()) return savedKey
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildKey.isNotEmpty()) return buildKey
            return DEFAULT_API_KEY
        }
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANG, "es") ?: "es"
        set(value) = prefs.edit().putString(KEY_APP_LANG, value).apply()

    var targetSituations: String
        get() = prefs.getString(KEY_TARGET_SITUATIONS, "Daily Life & Casual Chats") ?: "Daily Life & Casual Chats"
        set(value) = prefs.edit().putString(KEY_TARGET_SITUATIONS, value).apply()

    var englishLevel: String
        get() = prefs.getString(KEY_ENGLISH_LEVEL, "Intermediate (B1 - B2)") ?: "Intermediate (B1 - B2)"
        set(value) = prefs.edit().putString(KEY_ENGLISH_LEVEL, value).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, value).apply()

    var dailyReminderTime: String
        get() = prefs.getString(KEY_DAILY_REMINDER_TIME, "20:00") ?: "20:00"
        set(value) = prefs.edit().putString(KEY_DAILY_REMINDER_TIME, value).apply()

    var aiPersonaPrompt: String
        get() = prefs.getString(KEY_AI_PERSONA, DEFAULT_PERSONA_PROMPT) ?: DEFAULT_PERSONA_PROMPT
        set(value) = prefs.edit().putString(KEY_AI_PERSONA, value).apply()

    var aiReadingTonePrompt: String
        get() = prefs.getString(KEY_AI_READING, DEFAULT_READING_PROMPT) ?: DEFAULT_READING_PROMPT
        set(value) = prefs.edit().putString(KEY_AI_READING, value).apply()

    var aiSuggestionsPrompt: String
        get() = prefs.getString(KEY_AI_SUGGESTIONS, DEFAULT_SUGGESTIONS_PROMPT) ?: DEFAULT_SUGGESTIONS_PROMPT
        set(value) = prefs.edit().putString(KEY_AI_SUGGESTIONS, value).apply()

    fun resetAiPrompts() {
        prefs.edit()
            .putString(KEY_AI_PERSONA, DEFAULT_PERSONA_PROMPT)
            .putString(KEY_AI_READING, DEFAULT_READING_PROMPT)
            .putString(KEY_AI_SUGGESTIONS, DEFAULT_SUGGESTIONS_PROMPT)
            .apply()
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var completedLessons: Set<String>
        get() = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_COMPLETED_LESSONS, value).apply()

    var hasRequestedReviewAtFirstLesson: Boolean
        get() = prefs.getBoolean(KEY_REVIEW_REQUESTED_FIRST_LESSON, false)
        set(value) = prefs.edit().putBoolean(KEY_REVIEW_REQUESTED_FIRST_LESSON, value).apply()

    var hasRequestedReviewAtThirteenthLesson: Boolean
        get() = prefs.getBoolean(KEY_REVIEW_REQUESTED_THIRTEENTH_LESSON, false)
        set(value) = prefs.edit().putBoolean(KEY_REVIEW_REQUESTED_THIRTEENTH_LESSON, value).apply()

    var highestUnlockedIndex: Int
        get() = prefs.getInt(KEY_HIGHEST_UNLOCKED_INDEX, 2)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_UNLOCKED_INDEX, value.coerceAtLeast(2)).apply()

    fun advanceUnlockFrontier(index: Int) {
        if (index > highestUnlockedIndex) {
            highestUnlockedIndex = index
        }
    }

    var lessonScores: Map<String, Int>
        get() {
            val raw = prefs.getString(KEY_LESSON_SCORES, null) ?: return emptyMap()
            return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrElse { emptyMap() }
        }
        set(value) {
            prefs.edit().putString(KEY_LESSON_SCORES, json.encodeToString(value)).apply()
        }

    fun getLessonScore(lessonId: String): Int {
        return lessonScores[lessonId] ?: if (completedLessons.contains(lessonId)) 100 else 0
    }

    var totalExercisesCompleted: Int
        get() = prefs.getInt(KEY_EXERCISES_COMPLETED, 0)
        set(value) = prefs.edit().putInt(KEY_EXERCISES_COMPLETED, value).apply()

    var userXp: Int
        get() = prefs.getInt(KEY_USER_XP, 0)
        set(value) = prefs.edit().putInt(KEY_USER_XP, value).apply()

    var streakDays: Int
        get() = prefs.getInt(KEY_STREAK_DAYS, 1)
        set(value) = prefs.edit().putInt(KEY_STREAK_DAYS, value).apply()

    var lessonGoalTarget: Int
        get() = prefs.getInt(KEY_LESSON_GOAL_TARGET, 5)
        set(value) = prefs.edit().putInt(KEY_LESSON_GOAL_TARGET, value.coerceAtLeast(1)).apply()

    var weeklyLessonsCompletedCount: Int
        get() {
            checkAndResetWeekIfNeeded()
            return prefs.getInt(KEY_WEEKLY_COMPLETED_COUNT, 0)
        }
        set(value) {
            checkAndResetWeekIfNeeded()
            prefs.edit().putInt(KEY_WEEKLY_COMPLETED_COUNT, value.coerceAtLeast(0)).apply()
        }

    private fun checkAndResetWeekIfNeeded() {
        val cal = java.util.Calendar.getInstance()
        val currentWeekYear = "${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.WEEK_OF_YEAR)}"
        val savedWeekYear = prefs.getString(KEY_CURRENT_WEEK_YEAR, null)
        if (savedWeekYear != currentWeekYear) {
            prefs.edit()
                .putString(KEY_CURRENT_WEEK_YEAR, currentWeekYear)
                .putInt(KEY_WEEKLY_COMPLETED_COUNT, 0)
                .apply()
        }
    }

    fun recordExerciseCompleted(category: SkillCategory, isCorrect: Boolean, accuracyScore: Int = if (isCorrect) 100 else 40) {
        totalExercisesCompleted += 1
        if (isCorrect) {
            userXp += 10
            val delta = 30 // +30 points per correct exercise
            val canonicalCategory = category.canonical
            val skillKey = when (canonicalCategory) {
                SkillCategory.LEXICON -> "lexicon"
                SkillCategory.STRUCTURE -> "structure"
                SkillCategory.RECEPTIVE -> "receptive"
                SkillCategory.PRODUCTIVE -> "productive"
                SkillCategory.PRAGMATICS -> "pragmatics"
                else -> "structure"
            }
            updateScore(skillKey, delta)
        }
        // If incorrect: no deduction or penalty (0 points)
    }

    fun recordExerciseCompleted(isCorrect: Boolean) = recordExerciseCompleted(SkillCategory.STRUCTURE, isCorrect)

    fun recordLessonCompleted(lessonId: String, scorePercent: Int = 100) {
        completedLessons = completedLessons.toMutableSet().apply { add(lessonId) }
        val currentScores = lessonScores.toMutableMap()
        currentScores[lessonId] = scorePercent.coerceIn(0, 100)
        lessonScores = currentScores
        userXp += 50
        weeklyLessonsCompletedCount += 1
    }

    fun removeLessonCompleted(lessonId: String) {
        completedLessons = completedLessons.toMutableSet().apply { remove(lessonId) }
        val currentScores = lessonScores.toMutableMap()
        currentScores.remove(lessonId)
        lessonScores = currentScores
    }

    var skillMetrics: SkillMetrics
        get() {
            val raw = prefs.getString(KEY_SKILL_METRICS, null) ?: return SkillMetrics()
            return runCatching { json.decodeFromString<SkillMetrics>(raw) }.getOrElse { SkillMetrics() }
        }
        set(value) {
            prefs.edit().putString(KEY_SKILL_METRICS, json.encodeToString(value)).apply()
        }

    fun updateScore(skill: String, delta: Int) {
        if (delta <= 0) return
        val current = skillMetrics
        val updated = when (skill.lowercase()) {
            "lexicon", "vocabulary" -> current.copy(lexicon = current.lexicon + delta)
            "structure", "grammar" -> current.copy(structure = current.structure + delta)
            "receptive", "intonation" -> current.copy(receptive = current.receptive + delta)
            "productive", "pronunciation" -> current.copy(productive = current.productive + delta)
            "pragmatics", "fluency" -> current.copy(pragmatics = current.pragmatics + delta)
            else -> current
        }
        val finalized = updated.copy(overallCefr = updated.calculateCefr())
        skillMetrics = finalized
    }

    fun getSrsData(lessonId: String): String? = prefs.getString("srs_$lessonId", null)
    fun saveSrsData(lessonId: String, data: String) = prefs.edit().putString("srs_$lessonId", data).apply()

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_SUPABASE_ACCESS_TOKEN = "key_supabase_access_token"
        private const val KEY_SUPABASE_REFRESH_TOKEN = "key_supabase_refresh_token"
        private const val KEY_USER_AVATAR_URL = "key_user_avatar_url"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_CLOUD_SYNC_ENABLED = "key_cloud_sync_enabled"
        private const val KEY_NATIVE_LANG = "key_native_lang"
        private const val KEY_GOAL = "key_goal"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_APP_LANG = "key_app_lang"
        private const val KEY_TARGET_SITUATIONS = "key_target_situations"
        private const val KEY_ENGLISH_LEVEL = "key_english_level"
        private const val KEY_DAILY_REMINDER_ENABLED = "key_daily_reminder_enabled"
        private const val KEY_DAILY_REMINDER_TIME = "key_daily_reminder_time"
        private const val KEY_AI_PERSONA = "key_ai_persona"
        private const val KEY_AI_READING = "key_ai_reading"
        private const val KEY_AI_SUGGESTIONS = "key_ai_suggestions"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_SKILL_METRICS = "key_skill_metrics"
        private const val KEY_COMPLETED_LESSONS = "key_completed_lessons"
        private const val KEY_REVIEW_REQUESTED_FIRST_LESSON = "key_review_requested_first_lesson"
        private const val KEY_REVIEW_REQUESTED_THIRTEENTH_LESSON = "key_review_requested_thirteenth_lesson"
        private const val KEY_LESSON_SCORES = "key_lesson_scores"
        private const val KEY_EXERCISES_COMPLETED = "key_exercises_completed"
        private const val KEY_USER_XP = "key_user_xp"
        private const val KEY_STREAK_DAYS = "key_streak_days"
        private const val KEY_LESSON_GOAL_TARGET = "key_lesson_goal_target"
        private const val KEY_WEEKLY_COMPLETED_COUNT = "key_weekly_completed_count"
        private const val KEY_CURRENT_WEEK_YEAR = "key_current_week_year"
        private const val KEY_HIGHEST_UNLOCKED_INDEX = "key_highest_unlocked_index"
        private val DEFAULT_API_KEY: String by lazy {
            String(java.util.Base64.getUrlDecoder().decode("QVEuQWI4Uk42TDBSR3JIM0RKWE4wQ08xN3VGT2xyLW5yZTd6dy1RNDk2cFp1X0FLOTZOWUE="), Charsets.UTF_8)
        }
        const val DEFAULT_PERSONA_PROMPT = "You are a warm, helpful native English conversation partner in a practice drill."
        const val DEFAULT_READING_PROMPT = "You are a native English speaker. Always read the provided sentence aloud clearly and naturally."
        const val DEFAULT_SUGGESTIONS_PROMPT = "Suggest exactly 1 natural response (intermediate CEFR B1 level) that the user could choose or speak next."
    }
}
