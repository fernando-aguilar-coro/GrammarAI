package com.app.grammarai.core.srs

import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.LessonSrsProgress
import com.app.grammarai.data.repository.PracticeHistoryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class SpacedRepetitionEngine(
    private val prefs: PreferencesManager,
    private val historyRepo: PracticeHistoryRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Intervals corresponding to SRS Leitner stages
    private val stageIntervalsMs = listOf(
        TimeUnit.HOURS.toMillis(4),    // Stage 0: 4 hours
        TimeUnit.DAYS.toMillis(1),     // Stage 1: 1 day
        TimeUnit.DAYS.toMillis(3),     // Stage 2: 3 days
        TimeUnit.DAYS.toMillis(7),     // Stage 3: 7 days
        TimeUnit.DAYS.toMillis(14),    // Stage 4: 14 days
        TimeUnit.DAYS.toMillis(30)     // Stage 5: 30 days
    )

    fun getSrsProgress(lessonId: String): LessonSrsProgress {
        val raw = prefs.getSrsData(lessonId)
        return if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<LessonSrsProgress>(raw) }.getOrElse {
                LessonSrsProgress(lessonId)
            }
        } else {
            LessonSrsProgress(lessonId)
        }
    }

    fun recordReviewResult(lessonId: String, allCorrect: Boolean) {
        val current = getSrsProgress(lessonId)
        val newStage = if (allCorrect) {
            (current.srsStage + 1).coerceAtMost(stageIntervalsMs.size - 1)
        } else {
            1 // Demote to stage 1 for quick reinforcement
        }
        val interval = stageIntervalsMs[newStage]
        val now = System.currentTimeMillis()
        val updated = current.copy(
            srsStage = newStage,
            lastReviewedTimestamp = now,
            nextReviewTimestamp = now + interval,
            totalReviews = current.totalReviews + 1
        )
        prefs.saveSrsData(lessonId, json.encodeToString(updated))
    }

    fun isLessonDue(lessonId: String): Boolean {
        val progress = getSrsProgress(lessonId)
        if (progress.lastReviewedTimestamp == 0L) return false
        return System.currentTimeMillis() >= progress.nextReviewTimestamp
    }
}
