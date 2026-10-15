package com.app.grammarai.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextGreen
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsSupabaseSyncCard(
    userId: String,
    userEmail: String,
    isAuthenticated: Boolean,
    isAuthenticating: Boolean,
    lastSyncTimestamp: Long,
    isSyncing: Boolean,
    isCloudSyncEnabled: Boolean,
    syncStatusMessage: String?,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onToggleCloudSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRestoreDialog by remember { mutableStateOf(false) }

    val formattedSyncTime = remember(lastSyncTimestamp) {
        if (lastSyncTimestamp <= 0L) {
            "Never"
        } else {
            val diffSec = (System.currentTimeMillis() - lastSyncTimestamp) / 1000
            when {
                diffSec < 60 -> "Just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                diffSec < 86400 -> "${diffSec / 3600}h ago"
                else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastSyncTimestamp))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPrimary.copy(alpha = 0.12f))
                        .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = "Supabase Cloud Sync",
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Supabase Cloud Sync",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Google Auth & Cross-device backup",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isCloudSyncEnabled,
                onCheckedChange = onToggleCloudSync,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentPrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isAuthenticated) {
            com.app.grammarai.ui.common.components.GrammarButton(
                text = if (isAuthenticating) "Authenticating with Google..." else "Sign in with Google",
                onClick = onSignInWithGoogle,
                style = com.app.grammarai.ui.common.components.GrammarButtonStyle.OUTLINED,
                isLoading = isAuthenticating,
                leadingContent = { GoogleGLogo(modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        SettingsSupabaseAccountStatus(
            userId = userId,
            userEmail = userEmail,
            isAuthenticated = isAuthenticated,
            isCloudSyncEnabled = isCloudSyncEnabled,
            formattedSyncTime = formattedSyncTime
        )

        AnimatedVisibility(visible = !syncStatusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val isErr = syncStatusMessage?.startsWith("Sync error") == true ||
                syncStatusMessage?.startsWith("Auth error") == true ||
                syncStatusMessage?.startsWith("Restore error") == true
            Text(
                text = syncStatusMessage.orEmpty(),
                color = if (isErr) com.app.grammarai.ui.theme.ErrorCrimson else com.app.grammarai.ui.theme.SuccessEmerald,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSupabaseActionButtons(
            isSyncing = isSyncing,
            isCloudSyncEnabled = isCloudSyncEnabled,
            isAuthenticated = isAuthenticated,
            onSyncNow = onSyncNow,
            onRestoreClick = { showRestoreDialog = true },
            onSignOut = onSignOut
        )
    }

    if (showRestoreDialog) {
        SettingsSupabaseRestoreConfirmDialog(
            userEmail = userEmail,
            onConfirm = {
                onRestoreFromCloud()
                showRestoreDialog = false
            },
            onDismiss = { showRestoreDialog = false }
        )
    }
}
