package com.app.grammarai.ui.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.radar.components.ProficiencyCard
import com.app.grammarai.ui.radar.components.RadarHeader
import com.app.grammarai.ui.radar.components.SkillHighlightsRow
import com.app.grammarai.ui.theme.DarkBackground

@Composable
fun SkillRadarScreen(
    viewModel: SkillRadarViewModel,
    onNavigateToFeed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SkillRadarUiEffect.NavigateToFeed -> onNavigateToFeed()
            }
        }
    }

    val metrics = state.metrics

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Header
            RadarHeader(
                onBackClick = onNavigateToFeed
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Glassmorphic Card (Proficiency + Pentagon Radar + Encouragement + Go to Lessons CTA)
            ProficiencyCard(
                metrics = metrics,
                onGoToLessonsClick = { viewModel.processIntent(SkillRadarUiIntent.GoToLessons) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Macro-Skill Highlights: Top Strength & Growth Priority
            SkillHighlightsRow(
                topStrength = metrics.getTopStrength(),
                growthPriority = metrics.getGrowthPriority()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
