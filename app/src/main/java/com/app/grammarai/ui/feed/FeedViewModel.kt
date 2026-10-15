package com.app.grammarai.ui.feed

import androidx.lifecycle.viewModelScope
import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.mvi.MviViewModel
import kotlinx.coroutines.launch

import com.app.grammarai.core.srs.SpacedRepetitionEngine
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.repository.CurriculumRepository

import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.LessonStatus

import com.app.grammarai.data.repository.SupabaseSyncRepository

class FeedViewModel(
    private val repo: CurriculumRepository = GrammarApplication.instance.container.curriculumRepository,
    private val prefs: PreferencesManager = GrammarApplication.instance.container.preferencesManager,
    private val srsEngine: SpacedRepetitionEngine = GrammarApplication.instance.container.spacedRepetitionEngine,
    private val supabaseSyncRepo: SupabaseSyncRepository = GrammarApplication.instance.container.supabaseSyncRepository
) : MviViewModel<FeedUiState, FeedUiIntent, FeedUiEffect>(FeedUiState()) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }

    private var allFeedLessons: List<Lesson> = emptyList()

    init {
        loadFeed()
        viewModelScope.launch {
            repo.lessons.collect { updated ->
                if (updated.isNotEmpty()) {
                    loadFeed()
                }
            }
        }
    }

    override fun processIntent(intent: FeedUiIntent) {
        when (intent) {
            is FeedUiIntent.LoadFeed -> loadFeed()
            is FeedUiIntent.LoadNextPage -> loadNextPage()
            is FeedUiIntent.OpenLesson -> {
                val targetLesson = allFeedLessons.find { it.id == intent.lessonId }
                    ?: state.value.completedLessons.find { it.id == intent.lessonId }
                if (targetLesson?.status == LessonStatus.LOCKED) {
                    val app = com.app.grammarai.GrammarApplication.instance
                    sendEffect(FeedUiEffect.ShowToast(app.getString(com.app.grammarai.R.string.feed_lesson_locked_toast)))
                } else if (targetLesson != null && targetLesson.youtubeId.isNullOrBlank()) {
                    sendEffect(FeedUiEffect.NavigateToPractice(intent.lessonId))
                } else {
                    sendEffect(FeedUiEffect.NavigateToLesson(intent.lessonId))
                }
            }
            is FeedUiIntent.OpenSkillRadar -> sendEffect(FeedUiEffect.NavigateToRadar)
            is FeedUiIntent.OpenSettings -> sendEffect(FeedUiEffect.NavigateToSettings)
            is FeedUiIntent.ToggleCompletedLessonsDialog -> updateState { it.copy(showCompletedLessonsDialog = intent.show) }
            is FeedUiIntent.RequestToggleLessonCompleted -> updateState { it.copy(lessonToConfirmToggle = intent.lesson) }
            is FeedUiIntent.DismissToggleLessonDialog -> updateState { it.copy(lessonToConfirmToggle = null) }
            is FeedUiIntent.ConfirmToggleLessonCompleted -> {
                val lesson = state.value.lessonToConfirmToggle
                if (lesson != null) {
                    repo.toggleLessonCompleted(lesson.id)
                    updateState { it.copy(lessonToConfirmToggle = null) }
                    loadFeed()
                }
            }
            is FeedUiIntent.ToggleLessonCompleted -> {
                repo.toggleLessonCompleted(intent.lessonId)
                loadFeed()
            }
            is FeedUiIntent.OpenPractice -> sendEffect(FeedUiEffect.NavigateToPractice(intent.lessonId))
        }
    }

    private fun loadNextPage() {
        val currentState = state.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMoreLessons) return

        val nextPage = currentState.currentPage + 1
        val targetCount = nextPage * currentState.pageSize
        val nextLessons = allFeedLessons.take(targetCount)
        val hasMore = allFeedLessons.size > nextLessons.size

        updateState {
            it.copy(
                lessons = nextLessons,
                currentPage = nextPage,
                hasMoreLessons = hasMore,
                isLoadingMore = false
            )
        }
    }

    private fun loadFeed() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            val rawLessons = repo.loadLessons()
            val cefrOrder = mapOf("PRE-A1" to 0, "A0" to 0, "A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5, "C2" to 6)
            // Sort linearly by pedagogical level (CEFR) and module progression.
            // Completed (MASTERED) lessons accumulate in the progress card deck instead of sinking to the bottom.
            val sortedLessons = rawLessons.sortedWith(
                compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }
                    .thenBy { it.module }
            )
            val completedLessons = sortedLessons.filter { it.status == LessonStatus.MASTERED }
            val activeLessons = sortedLessons.filter { it.status != LessonStatus.MASTERED }
            // If all lessons are completed, show sortedLessons so feed remains browsable
            val feedLessons = if (activeLessons.isNotEmpty()) activeLessons else sortedLessons
            allFeedLessons = feedLessons

            // Preserve current depth if refreshing existing pagination
            val currentDepth = if (state.value.currentPage > 1) {
                (state.value.currentPage * DEFAULT_PAGE_SIZE).coerceAtMost(feedLessons.size)
            } else {
                DEFAULT_PAGE_SIZE
            }
            val pagedLessons = feedLessons.take(currentDepth)
            val hasMore = feedLessons.size > pagedLessons.size

            val completedCount = prefs.completedLessons.size
            val dueForReview = sortedLessons.filter { srsEngine.isLessonDue(it.id) }.map { it.id }.toSet()
            val situationalProgress = com.app.grammarai.data.catalog.SituationalCatalog.getActiveProgress(prefs.completedLessons)

            updateState {
                it.copy(
                    userName = prefs.userName,
                    lessons = pagedLessons,
                    completedLessons = completedLessons,
                    completedLessonsCount = completedCount,
                    weeklyCompletedLessonsCount = prefs.weeklyLessonsCompletedCount,
                    totalLessonsCount = sortedLessons.size.coerceAtLeast(2),
                    totalExercisesCompleted = prefs.totalExercisesCompleted,
                    userXp = prefs.userXp,
                    streakDays = prefs.streakDays,
                    lessonGoalTarget = prefs.lessonGoalTarget,
                    dueForSrsReviewIds = dueForReview,
                    situationalLesson = situationalProgress?.currentLesson,
                    situationalThemeTitle = situationalProgress?.theme?.title ?: "Cafetería",
                    situationalPartIndex = situationalProgress?.partIndex ?: 1,
                    totalSituationalParts = situationalProgress?.totalParts ?: 3,
                    isAllSituationalCompleted = situationalProgress?.isAllCompleted ?: false,
                    isLoading = false,
                    pageSize = DEFAULT_PAGE_SIZE,
                    hasMoreLessons = hasMore,
                    isLoadingMore = false
                )
            }

            // Trigger background prefetch for on-demand audio packs if user crossed progression thresholds
            runCatching {
                val currentLang = prefs.appLanguage.ifBlank { "es" }
                GrammarApplication.instance.onDemandAudioPrefetcher.checkPrefetch(prefs.highestUnlockedIndex, currentLang)
            }

            // Background sync with Supabase for user progress
            runCatching {
                supabaseSyncRepo.syncUserData()
            }.onSuccess { res ->
                if (res.isSuccess) {
                    val updatedCount = prefs.completedLessons.size
                    if (updatedCount != completedCount || prefs.userXp != state.value.userXp) {
                        repo.reloadCurriculum()
                    }
                }
            }
        }
    }
}
