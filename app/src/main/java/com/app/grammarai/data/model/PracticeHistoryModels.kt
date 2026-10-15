package com.app.grammarai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseAttempt(
    val id: String,
    val lessonId: String,
    val exerciseId: String,
    val category: SkillCategory,
    val wasCorrect: Boolean,
    val accuracyScore: Int, // 0-100%
    val userAnswer: String,
    val expectedAnswer: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LessonSrsProgress(
    val lessonId: String,
    val srsStage: Int = 0, // 0 to 5 (Leitner boxes)
    val nextReviewTimestamp: Long = 0L,
    val lastReviewedTimestamp: Long = 0L,
    val totalReviews: Int = 0
)
