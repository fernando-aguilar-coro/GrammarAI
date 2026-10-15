package com.app.grammarai.data.repository

import android.content.Context
import com.app.grammarai.data.model.ExerciseAttempt
import com.app.grammarai.data.model.SkillCategory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PracticeHistoryRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val historyFile by lazy { File(context.filesDir, "exercise_attempts.json") }
    private val legacyPrefs by lazy { context.getSharedPreferences("grammar_ai_history", Context.MODE_PRIVATE) }

    private val _attempts = MutableStateFlow<List<ExerciseAttempt>>(emptyList())
    val attempts: StateFlow<List<ExerciseAttempt>> = _attempts.asStateFlow()

    init {
        coroutineScope.launch {
            loadAttemptsInternal()
        }
    }

    private suspend fun loadAttemptsInternal() = withContext(Dispatchers.IO) {
        try {
            if (historyFile.exists()) {
                val raw = historyFile.readText()
                _attempts.value = json.decodeFromString<List<ExerciseAttempt>>(raw)
            } else {
                // Migrate from legacy SharedPreferences if present
                val legacyRaw = legacyPrefs.getString("exercise_attempts_log", null)
                if (!legacyRaw.isNullOrBlank()) {
                    val parsed = json.decodeFromString<List<ExerciseAttempt>>(legacyRaw)
                    _attempts.value = parsed
                    historyFile.writeText(legacyRaw)
                    legacyPrefs.edit().remove("exercise_attempts_log").apply()
                }
            }
        } catch (_: Exception) {
            // Keep default empty list on parsing failure
        }
    }

    suspend fun recordAttempt(attempt: ExerciseAttempt) = withContext(Dispatchers.IO) {
        val updated = (_attempts.value + attempt).takeLast(200) // Keep last 200 attempts
        _attempts.value = updated
        try {
            val raw = json.encodeToString(updated)
            historyFile.writeText(raw)
        } catch (_: Exception) {}
    }

    fun getMistakes(): List<ExerciseAttempt> {
        return _attempts.value.filter { !it.wasCorrect }.takeLast(20)
    }

    fun getCategoryAccuracy(category: SkillCategory): Float {
        val canonicalTarget = category.canonical
        val catAttempts = _attempts.value.filter { it.category.canonical == canonicalTarget }
        if (catAttempts.isEmpty()) return 0.75f
        val correctCount = catAttempts.count { it.wasCorrect }
        return correctCount.toFloat() / catAttempts.size.toFloat()
    }

    fun getWeakestCategory(): SkillCategory {
        val canonicalCategories = listOf(
            SkillCategory.PRODUCTIVE,
            SkillCategory.PRAGMATICS,
            SkillCategory.RECEPTIVE,
            SkillCategory.STRUCTURE,
            SkillCategory.LEXICON
        )
        return canonicalCategories.minByOrNull { getCategoryAccuracy(it) } ?: SkillCategory.PRODUCTIVE
    }
}
