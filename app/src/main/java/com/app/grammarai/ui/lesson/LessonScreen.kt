package com.app.grammarai.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.lesson.components.CoreNotesCard
import com.app.grammarai.ui.lesson.components.CuratedVideoPlayerCard
import com.app.grammarai.ui.lesson.components.LessonStartPracticeButton
import com.app.grammarai.ui.lesson.components.LessonTopBar
import com.app.grammarai.ui.theme.DarkBackground

@Composable
fun LessonScreen(
    lessonId: String,
    viewModel: LessonViewModel,
    onNavigateToPractice: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(lessonId) {
        viewModel.processIntent(LessonUiIntent.LoadLesson(lessonId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LessonUiEffect.NavigateToPractice -> onNavigateToPractice(effect.lessonId)
                is LessonUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    val lesson = state.lesson

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF130E36), DarkBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Navigation Bar
            LessonTopBar(
                title = lesson?.title ?: "",
                onBackClick = { viewModel.processIntent(LessonUiIntent.Back) },
                isCompleted = lesson?.status == com.app.grammarai.data.model.LessonStatus.MASTERED,
                score = lesson?.score ?: 0,
                onToggleComplete = if (lesson?.type == com.app.grammarai.data.model.LessonType.SITUATIONAL_VOCABULARY) null else {
                    {
                        val id = lesson?.id ?: lessonId
                        viewModel.processIntent(LessonUiIntent.ToggleLessonCompleted(id))
                    }
                }
            )

            // Centered Video Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                if (!lesson?.youtubeId.isNullOrBlank()) {
                    CuratedVideoPlayerCard(
                        youtubeId = lesson?.youtubeId,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (lesson == null) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = com.app.grammarai.ui.theme.NeonCyan,
                        strokeWidth = 3.dp
                    )
                }
            }

            // Sticky Bottom CTA Button
            LessonStartPracticeButton(
                onClick = { viewModel.processIntent(LessonUiIntent.StartPractice) },
                exerciseCount = lesson?.exercises?.size ?: 0,
                enabled = !state.isStartingPractice,
                isLocked = lesson?.isLocked == true,
                isAudioDownloading = state.isAudioDownloading,
                downloadProgress = state.audioDownloadProgress
            )
        }
    }
}
