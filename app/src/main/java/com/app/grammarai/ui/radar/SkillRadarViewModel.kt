package com.app.grammarai.ui.radar

import com.app.grammarai.GrammarApplication
import com.app.grammarai.core.mvi.MviViewModel

import com.app.grammarai.data.local.PreferencesManager

class SkillRadarViewModel(
    private val prefs: PreferencesManager = GrammarApplication.instance.preferencesManager
) : MviViewModel<SkillRadarUiState, SkillRadarUiIntent, SkillRadarUiEffect>(
    SkillRadarUiState()
) {

    init {
        loadMetrics()
    }

    override fun processIntent(intent: SkillRadarUiIntent) {
        when (intent) {
            is SkillRadarUiIntent.LoadMetrics -> loadMetrics()
            is SkillRadarUiIntent.GoToLessons -> sendEffect(SkillRadarUiEffect.NavigateToFeed)
        }
    }

    private fun loadMetrics() {
        val savedMetrics = prefs.skillMetrics
        updateState {
            it.copy(
                metrics = savedMetrics,
                userName = prefs.userName
            )
        }
    }
}
