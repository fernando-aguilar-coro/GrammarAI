package com.app.grammarai.ui.radar

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState
import com.app.grammarai.data.model.SkillMetrics

data class SkillRadarUiState(
    val metrics: SkillMetrics = SkillMetrics(),
    val userName: String = "neko",
    val isLoading: Boolean = false
) : UiState

sealed interface SkillRadarUiIntent : UiIntent {
    object LoadMetrics : SkillRadarUiIntent
    object GoToLessons : SkillRadarUiIntent
}

sealed interface SkillRadarUiEffect : UiEffect {
    object NavigateToFeed : SkillRadarUiEffect
}
