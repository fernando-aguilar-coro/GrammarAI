package com.app.grammarai.ui.lesson

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState
import com.app.grammarai.data.model.Lesson

data class LessonUiState(
    val lesson: Lesson? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null,
    val isStartingPractice: Boolean = false,
    val isAudioAvailable: Boolean = true,
    val isAudioDownloading: Boolean = false,
    val audioDownloadProgress: Float = 0f
) : UiState

sealed interface LessonUiIntent : UiIntent {
    data class LoadLesson(val lessonId: String) : LessonUiIntent
    object TogglePlay : LessonUiIntent
    data class SetSpeed(val speed: Float) : LessonUiIntent
    object StartPractice : LessonUiIntent
    object Back : LessonUiIntent
    data class ToggleLessonCompleted(val lessonId: String) : LessonUiIntent
}

sealed interface LessonUiEffect : UiEffect {
    data class NavigateToPractice(val lessonId: String) : LessonUiEffect
    object NavigateBack : LessonUiEffect
}
