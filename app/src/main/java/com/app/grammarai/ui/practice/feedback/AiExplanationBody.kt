package com.app.grammarai.ui.practice.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.AiEvaluationResult
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.TextPrimary

@Composable
fun AiExplanationBody(
    aiResult: AiEvaluationResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.drill_ai_dialog_correction_title),
                modifier = Modifier.padding(start = 8.dp),
                color = AccentPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = aiResult.explanation,
            modifier = Modifier.padding(top = 8.dp),
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
