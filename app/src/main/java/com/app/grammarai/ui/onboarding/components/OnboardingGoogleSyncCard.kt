package com.app.grammarai.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.settings.components.GoogleGLogo
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun OnboardingGoogleSyncCard(
    isSigningIn: Boolean,
    isGoogleLinked: Boolean,
    linkedEmail: String?,
    appLanguage: String,
    onSignInWithGoogle: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = appLanguage.equals("en", ignoreCase = true)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(
                    width = 1.dp,
                    color = BorderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGoogleLinked) SuccessEmerald.copy(alpha = 0.12f)
                        else AccentPrimary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGoogleLinked) Icons.Outlined.CloudDone else Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = if (isGoogleLinked) SuccessEmerald else AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isGoogleLinked) {
                    if (isEn) "Google Account Connected" else "¡Cuenta de Google Conectada!"
                } else {
                    if (isEn) "Cloud Synchronization" else "Sincronización en la Nube"
                },
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isGoogleLinked) {
                    linkedEmail ?: (if (isEn) "Progress is synced safely" else "Progreso sincronizado")
                } else {
                    if (isEn) "Link your Google account to keep streaks, XP, and lesson progress backed up."
                    else "Vincula tu cuenta de Google con Supabase para mantener tus rachas, XP y lecciones a salvo."
                },
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!isGoogleLinked) {
                GrammarButton(
                    text = if (isEn) "Sign in with Google" else "Iniciar sesión con Google",
                    onClick = onSignInWithGoogle,
                    style = GrammarButtonStyle.OUTLINED,
                    isLoading = isSigningIn,
                    leadingContent = { GoogleGLogo(modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                GrammarButton(
                    text = if (isEn) "Skip for now" else "Omitir por ahora",
                    onClick = onSkip,
                    style = GrammarButtonStyle.GHOST,
                    enabled = !isSigningIn,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GrammarButton(
                    text = if (isEn) "Continue to App" else "Continuar a la App",
                    onClick = onContinue,
                    style = GrammarButtonStyle.SUCCESS,
                    trailingIcon = Icons.Outlined.Check,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
