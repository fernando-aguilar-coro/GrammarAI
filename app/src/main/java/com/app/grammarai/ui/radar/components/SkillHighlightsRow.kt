package com.app.grammarai.ui.radar.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.app.grammarai.R
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
private fun localizedSkillName(skill: String): String {
    return when (skill.lowercase()) {
        "productive", "pronunciation" -> stringResource(R.string.radar_skill_productive)
        "pragmatics", "fluency" -> stringResource(R.string.radar_skill_pragmatics)
        "receptive", "intonation" -> stringResource(R.string.radar_skill_receptive)
        "structure", "grammar" -> stringResource(R.string.radar_skill_structure)
        "lexicon", "vocabulary" -> stringResource(R.string.radar_skill_lexicon)
        else -> skill
    }
}

@Composable
fun SkillHighlightsRow(
    topStrength: Pair<String, Int>,
    growthPriority: Pair<String, Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Strength Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SuccessEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.radar_top_strength_label),
                    color = SuccessEmerald,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val strengthName = localizedSkillName(topStrength.first)
                Text(
                    text = "$strengthName: ${topStrength.second} pts",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Growth Priority Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.radar_growth_priority_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val priorityName = localizedSkillName(growthPriority.first)
                Text(
                    text = "$priorityName: ${growthPriority.second} pts",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
