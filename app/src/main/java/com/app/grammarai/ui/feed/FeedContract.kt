package com.app.grammarai.ui.feed

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState
import com.app.grammarai.data.model.Lesson

data class FeedUiState(
    val userName: String = "neko",
    val lessons: List<Lesson> = emptyList(),
    val completedLessons: List<Lesson> = emptyList(),
    val completedLessonsCount: Int = 0,
    val weeklyCompletedLessonsCount: Int = 0,
    val totalLessonsCount: Int = 2,
    val lessonGoalTarget: Int = 5,
    val totalExercisesCompleted: Int = 0,
    val userXp: Int = 0,
    val streakDays: Int = 1,
    val dueForSrsReviewIds: Set<String> = emptySet(),
    val showCompletedLessonsDialog: Boolean = false,
    val isLoading: Boolean = false,
    val situationalLesson: Lesson? = null,
    val situationalThemeTitle: String = "Cafetería",
    val situationalPartIndex: Int = 1,
    val totalSituationalParts: Int = 3,
    val isAllSituationalCompleted: Boolean = false,
    val lessonToConfirmToggle: Lesson? = null,
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val hasMoreLessons: Boolean = true,
    val isLoadingMore: Boolean = false
) : UiState {
    val progressFraction: Float
        get() = if (totalLessonsCount > 0) completedLessonsCount.toFloat() / totalLessonsCount.toFloat() else 0f

    val goalProgressFraction: Float
        get() = if (lessonGoalTarget > 0) (weeklyCompletedLessonsCount.toFloat() / lessonGoalTarget.toFloat()).coerceIn(0f, 1f) else 0f

    val goalPercentage: Int
        get() = if (lessonGoalTarget > 0) ((weeklyCompletedLessonsCount.coerceAtLeast(0).toFloat() / lessonGoalTarget.toFloat()) * 100).toInt() else 0
}

sealed interface FeedUiIntent : UiIntent {
    object LoadFeed : FeedUiIntent
    object LoadNextPage : FeedUiIntent
    data class OpenLesson(val lessonId: String) : FeedUiIntent
    object OpenSkillRadar : FeedUiIntent
    object OpenSettings : FeedUiIntent
    data class ToggleCompletedLessonsDialog(val show: Boolean) : FeedUiIntent
    data class RequestToggleLessonCompleted(val lesson: Lesson) : FeedUiIntent
    object ConfirmToggleLessonCompleted : FeedUiIntent
    object DismissToggleLessonDialog : FeedUiIntent
    data class ToggleLessonCompleted(val lessonId: String) : FeedUiIntent
    data class OpenPractice(val lessonId: String) : FeedUiIntent
}

sealed interface FeedUiEffect : UiEffect {
    data class NavigateToLesson(val lessonId: String) : FeedUiEffect
    data class NavigateToPractice(val lessonId: String) : FeedUiEffect
    object NavigateToRadar : FeedUiEffect
    object NavigateToSettings : FeedUiEffect
    data class ShowToast(val message: String) : FeedUiEffect
}
