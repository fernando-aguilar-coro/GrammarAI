package com.app.grammarai.ui.lesson

import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.mvi.MviViewModel

import com.app.grammarai.data.repository.CurriculumRepository

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LessonViewModel(
    private val repo: CurriculumRepository = GrammarApplication.instance.curriculumRepository
) : MviViewModel<LessonUiState, LessonUiIntent, LessonUiEffect>(LessonUiState()) {

    override fun processIntent(intent: LessonUiIntent) {
        when (intent) {
            is LessonUiIntent.LoadLesson -> {
                val cached = repo.getLesson(intent.lessonId)
                if (cached != null) {
                    if (cached.youtubeId.isNullOrBlank()) {
                        sendEffect(LessonUiEffect.NavigateToPractice(intent.lessonId))
                        return
                    }
                    updateState { it.copy(lesson = cached, isStartingPractice = false) }
                }
                viewModelScope.launch {
                    val lesson = cached ?: repo.getLessonAsync(intent.lessonId) ?: repo.getLesson(intent.lessonId)
                    if (lesson != null && lesson.youtubeId.isNullOrBlank()) {
                        sendEffect(LessonUiEffect.NavigateToPractice(intent.lessonId))
                        return@launch
                    }
                    val lessonsList = repo.lessons.value
                    val currentLang = GrammarApplication.instance.preferencesManager.appLanguage.ifBlank { "es" }
                    val deliveryMgr = GrammarApplication.instance.playAssetDeliveryManager
                    val targetPack = com.app.grammarai.core.audio.delivery.PlayAssetDeliveryManager.getPackNameForLesson(currentLang, lesson, lessonsList)

                    val isAvailable = targetPack == null || deliveryMgr.isPackInstalled(targetPack)
                    if (targetPack != null && !isAvailable) {
                        deliveryMgr.requestPackDownload(targetPack)
                    }

                    updateState {
                        it.copy(
                            lesson = lesson,
                            isStartingPractice = false,
                            isAudioAvailable = isAvailable,
                            isAudioDownloading = !isAvailable
                        )
                    }

                    // Observe delivery manager for installation updates
                    deliveryMgr.packStatuses.collect { statuses ->
                        if (targetPack != null) {
                            val packStatus = statuses[targetPack]
                            when (packStatus) {
                                is com.app.grammarai.core.audio.delivery.PackDeliveryStatus.Installed -> {
                                    updateState { it.copy(isAudioAvailable = true, isAudioDownloading = false, audioDownloadProgress = 1f) }
                                }
                                is com.app.grammarai.core.audio.delivery.PackDeliveryStatus.Downloading -> {
                                    updateState { it.copy(isAudioAvailable = false, isAudioDownloading = true, audioDownloadProgress = packStatus.progress) }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            is LessonUiIntent.TogglePlay -> {
                updateState { it.copy(isPlaying = !it.isPlaying) }
            }

            is LessonUiIntent.SetSpeed -> {
                updateState { it.copy(playbackSpeed = intent.speed) }
            }

            is LessonUiIntent.StartPractice -> {
                if (state.value.isStartingPractice) return
                val id = state.value.lesson?.id
                if (id != null) {
                    updateState { it.copy(isStartingPractice = true) }
                    sendEffect(LessonUiEffect.NavigateToPractice(id))
                    viewModelScope.launch {
                        delay(1200)
                        updateState { it.copy(isStartingPractice = false) }
                    }
                }
            }

            is LessonUiIntent.Back -> {
                sendEffect(LessonUiEffect.NavigateBack)
            }

            is LessonUiIntent.ToggleLessonCompleted -> {
                viewModelScope.launch {
                    repo.toggleLessonCompleted(intent.lessonId)
                    val updated = repo.getLesson(intent.lessonId)
                    val lessonsList = repo.lessons.value
                    val lessonIndex = lessonsList.indexOfFirst { it.id == intent.lessonId }
                    val currentLang = GrammarApplication.instance.preferencesManager.appLanguage.ifBlank { "es" }
                    val isAvailable = if (lessonIndex >= 0) {
                        GrammarApplication.instance.onDemandAudioPrefetcher.isAudioAvailableForLessonIndex(lessonIndex, currentLang)
                    } else true
                    updateState { it.copy(lesson = updated, isAudioAvailable = isAvailable) }
                }
            }
        }
    }
}
