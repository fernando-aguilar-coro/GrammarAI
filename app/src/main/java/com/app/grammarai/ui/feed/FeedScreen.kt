package com.app.grammarai.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.feed.components.CompletedLessonsDialog
import com.app.grammarai.ui.feed.components.ConfirmLessonToggleDialog
import com.app.grammarai.ui.feed.components.FeedTopBar
import com.app.grammarai.ui.feed.components.LargeFluentlyCard
import com.app.grammarai.ui.feed.components.SituationalVocabularyCard
import com.app.grammarai.ui.feed.components.UserProgressCard
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.NeonCyan

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToPractice: (String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()

    // Trigger loading next page when user scrolls near the bottom (3 items from end)
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.hasMoreLessons && !state.isLoadingMore && !state.isLoading) {
            viewModel.processIntent(FeedUiIntent.LoadNextPage)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.processIntent(FeedUiIntent.LoadFeed)
        viewModel.effect.collect { effect ->
            when (effect) {
                is FeedUiEffect.NavigateToLesson -> onNavigateToLesson(effect.lessonId)
                is FeedUiEffect.NavigateToPractice -> onNavigateToPractice(effect.lessonId)
                is FeedUiEffect.NavigateToRadar -> onNavigateToRadar()
                is FeedUiEffect.NavigateToSettings -> onNavigateToSettings()
                is FeedUiEffect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Bar
            FeedTopBar(
                userName = state.userName,
                onRadarClick = { viewModel.processIntent(FeedUiIntent.OpenSkillRadar) },
                onSettingsClick = { viewModel.processIntent(FeedUiIntent.OpenSettings) }
            )

            // User Progress Tracking Dashboard Card with physical stacked deck
            Box(
                modifier = Modifier
                    .padding(top = if (state.completedLessonsCount > 0) 12.dp else 4.dp)
            ) {
                UserProgressCard(
                    streakDays = state.streakDays,
                    completedLessonsCount = state.completedLessonsCount,
                    weeklyCompletedLessonsCount = state.weeklyCompletedLessonsCount,
                    totalLessonsCount = state.totalLessonsCount,
                    lessonGoalTarget = state.lessonGoalTarget,
                    totalExercisesCompleted = state.totalExercisesCompleted,
                    userXp = state.userXp,
                    completedLessons = state.completedLessons,
                    onStackClick = {
                        viewModel.processIntent(FeedUiIntent.ToggleCompletedLessonsDialog(true))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cards Feed
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                val situational = state.situationalLesson
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Situational Micro-Lesson placed first (Low-friction daily warm-up)
                    if (situational != null) {
                        item(key = "situational_${situational.id}") {
                            SituationalVocabularyCard(
                                modifier = Modifier.animateItem(),
                                lesson = situational,
                                themeTitle = state.situationalThemeTitle,
                                partIndex = state.situationalPartIndex,
                                totalParts = state.totalSituationalParts,
                                isAllCompleted = state.isAllSituationalCompleted,
                                onClick = { viewModel.processIntent(FeedUiIntent.OpenPractice(situational.id)) }
                            )
                        }
                    }

                    // Main Progressive Grammar Lessons
                    items(
                        items = state.lessons,
                        key = { it.id }
                    ) { lesson ->
                        LargeFluentlyCard(
                            modifier = Modifier.animateItem(),
                            lesson = lesson,
                            onClick = { viewModel.processIntent(FeedUiIntent.OpenLesson(lesson.id)) },
                            onToggleComplete = { viewModel.processIntent(FeedUiIntent.RequestToggleLessonCompleted(lesson)) }
                        )
                    }

                    // Pagination Footer
                    if (state.isLoadingMore) {
                        item(key = "pagination_loading_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Cargando más lecciones...",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (state.hasMoreLessons && state.lessons.isNotEmpty()) {
                        item(key = "pagination_load_more_btn") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceCard)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.processIntent(FeedUiIntent.LoadNextPage) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Cargar más lecciones (${state.lessons.size} de ${state.totalLessonsCount})",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else if (!state.hasMoreLessons && state.lessons.isNotEmpty()) {
                        item(key = "pagination_end_of_feed") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Has explorado todas las lecciones (${state.lessons.size} lecciones)",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Confirmation dialog before toggling lesson completion
        val lessonToConfirm = state.lessonToConfirmToggle
        if (lessonToConfirm != null) {
            ConfirmLessonToggleDialog(
                lesson = lessonToConfirm,
                onConfirm = {
                    viewModel.processIntent(FeedUiIntent.ConfirmToggleLessonCompleted)
                },
                onDismiss = {
                    viewModel.processIntent(FeedUiIntent.DismissToggleLessonDialog)
                }
            )
        }

        // Mastered Lessons Deck Dialog
        if (state.showCompletedLessonsDialog) {
            CompletedLessonsDialog(
                completedLessons = state.completedLessons,
                onDismiss = {
                    viewModel.processIntent(FeedUiIntent.ToggleCompletedLessonsDialog(false))
                },
                onOpenLesson = { lessonId ->
                    viewModel.processIntent(FeedUiIntent.OpenLesson(lessonId))
                }
            )
        }
    }
}
