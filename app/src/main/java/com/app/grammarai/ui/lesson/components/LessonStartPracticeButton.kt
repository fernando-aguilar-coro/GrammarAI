package com.app.grammarai.ui.lesson.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun LessonStartPracticeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    exerciseCount: Int = 0,
    enabled: Boolean = true,
    isLocked: Boolean = false,
    isAudioDownloading: Boolean = false,
    downloadProgress: Float = 0f
) {
    val buttonText = if (isLocked) {
        stringResource(R.string.lesson_btn_locked)
    } else if (exerciseCount > 0) {
        stringResource(R.string.lesson_btn_start_practice_count, exerciseCount)
    } else {
        stringResource(R.string.lesson_btn_start_practice)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isAudioDownloading) {
            val percent = (downloadProgress * 100).toInt().coerceIn(0, 100)
            Text(
                text = stringResource(R.string.lesson_audio_downloading_pack, percent),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { downloadProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AccentPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        GrammarButton(
            text = buttonText,
            onClick = onClick,
            enabled = enabled && !isLocked,
            style = GrammarButtonStyle.PRIMARY,
            height = 52.dp,
            shapeRadius = 12.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
