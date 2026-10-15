package com.app.grammarai.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.grammarai.GrammarApplication
import com.app.grammarai.ui.feed.FeedViewModel
import com.app.grammarai.ui.lesson.LessonViewModel
import com.app.grammarai.ui.onboarding.OnboardingViewModel
import com.app.grammarai.ui.practice.ExerciseRunnerViewModel
import com.app.grammarai.ui.radar.SkillRadarViewModel
import com.app.grammarai.ui.settings.SettingsViewModel

fun CreationExtras.grammarApplication(): GrammarApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GrammarApplication)

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = grammarApplication()
            val container = app.container
            FeedViewModel(
                repo = container.curriculumRepository,
                prefs = container.preferencesManager,
                srsEngine = container.spacedRepetitionEngine,
                supabaseSyncRepo = container.supabaseSyncRepository
            )
        }
        initializer {
            val app = grammarApplication()
            val container = app.container
            LessonViewModel(
                repo = container.curriculumRepository
            )
        }
        initializer {
            val app = grammarApplication()
            val container = app.container
            ExerciseRunnerViewModel(
                context = app,
                repo = container.curriculumRepository,
                prefs = container.preferencesManager,
                historyRepo = container.practiceHistoryRepository,
                srsEngine = container.spacedRepetitionEngine,
                geminiClient = container.geminiApiClient,
                supabaseSyncRepo = container.supabaseSyncRepository,
                translationManager = container.onDeviceTranslationManager
            )
        }
        initializer {
            val app = grammarApplication()
            val container = app.container
            SkillRadarViewModel(
                prefs = container.preferencesManager
            )
        }
        initializer {
            val app = grammarApplication()
            val container = app.container
            SettingsViewModel(
                prefs = container.preferencesManager,
                geminiClient = container.geminiApiClient,
                curriculumRepository = container.curriculumRepository,
                supabaseSyncRepo = container.supabaseSyncRepository
            )
        }
        initializer {
            val app = grammarApplication()
            val container = app.container
            OnboardingViewModel(
                prefs = container.preferencesManager,
                curriculumRepository = container.curriculumRepository,
                supabaseSyncRepo = container.supabaseSyncRepository
            )
        }
    }
}
