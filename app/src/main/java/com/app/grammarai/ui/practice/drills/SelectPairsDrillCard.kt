package com.app.grammarai.ui.practice.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard

@Composable
fun SelectPairsDrillCard(
    exercise: GeneratedExercise,
    selectedLeftId: String?,
    selectedRightId: String?,
    matchedLeftIds: Set<String>,
    matchedRightIds: Set<String>,
    pairError: Pair<String, String>?,
    onSelectLeft: (String, String) -> Unit,
    onSelectRight: (String, String) -> Unit,
    onPlayAudio: (() -> Unit)? = null,
    isTranslationSupported: Boolean = false,
    isTranslationVisible: Boolean = false,
    onToggleTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            ExercisePromptHeader(
                prompt = exercise.prompt,
                category = exercise.category,
                onPlayAudio = onPlayAudio,
                isTranslationSupported = isTranslationSupported,
                isTranslationVisible = isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SelectPairsView(
            pairs = exercise.pairs,
            selectedLeftId = selectedLeftId,
            selectedRightId = selectedRightId,
            matchedLeftIds = matchedLeftIds,
            matchedRightIds = matchedRightIds,
            pairError = pairError,
            onSelectLeft = onSelectLeft,
            onSelectRight = onSelectRight
        )
    }
}
