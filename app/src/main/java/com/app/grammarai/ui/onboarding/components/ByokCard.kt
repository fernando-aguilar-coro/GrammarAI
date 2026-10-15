package com.app.grammarai.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.WarningAmber

@Composable
fun ByokCard(
    apiKeyInput: String,
    onApiKeyChange: (String) -> Unit,
    onTestKey: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    isTestingKey: Boolean,
    keyValidationStatus: Boolean?,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_ask_byok),
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick portal link to Google AI Studio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPrimary.copy(alpha = 0.08f))
                .border(1.dp, AccentPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable {
                    uriHandler.openUri("https://aistudio.google.com/app/apikey")
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_byok_open_portal),
                color = AccentPrimary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.onboarding_byok_hint),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            GrammarButton(
                text = stringResource(R.string.onboarding_btn_test_key),
                onClick = onTestKey,
                style = GrammarButtonStyle.OUTLINED,
                isLoading = isTestingKey,
                height = 50.dp
            )
        }

        // Validation status notification
        AnimatedVisibility(visible = keyValidationStatus != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (keyValidationStatus == true) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_key_verified),
                        color = SuccessEmerald,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_key_invalid),
                        color = WarningAmber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GrammarButton(
            text = stringResource(R.string.onboarding_btn_launch),
            onClick = onComplete,
            style = GrammarButtonStyle.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        GrammarButton(
            text = stringResource(R.string.onboarding_byok_skip_btn),
            onClick = onSkip,
            style = GrammarButtonStyle.GHOST,
            height = 36.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
