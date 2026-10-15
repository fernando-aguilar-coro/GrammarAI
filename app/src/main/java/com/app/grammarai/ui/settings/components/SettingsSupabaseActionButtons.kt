package com.app.grammarai.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.common.components.GrammarIconButton
import com.app.grammarai.ui.theme.ErrorCrimson

@Composable
fun SettingsSupabaseActionButtons(
    isSyncing: Boolean,
    isCloudSyncEnabled: Boolean,
    isAuthenticated: Boolean,
    onSyncNow: () -> Unit,
    onRestoreClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GrammarButton(
            text = if (isSyncing) "Syncing..." else "Sync Now",
            onClick = onSyncNow,
            style = GrammarButtonStyle.PRIMARY,
            leadingIcon = Icons.Outlined.Sync,
            enabled = !isSyncing && isCloudSyncEnabled,
            isLoading = isSyncing,
            height = 44.dp,
            modifier = Modifier.weight(1f)
        )

        GrammarButton(
            text = "Restore",
            onClick = onRestoreClick,
            style = GrammarButtonStyle.OUTLINED,
            leadingIcon = Icons.Outlined.CloudDownload,
            enabled = !isSyncing,
            height = 44.dp,
            modifier = Modifier.weight(1f)
        )

        if (isAuthenticated) {
            GrammarIconButton(
                icon = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Sign Out",
                onClick = onSignOut,
                tint = ErrorCrimson,
                size = 44.dp
            )
        }
    }
}
