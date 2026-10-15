package com.app.grammarai.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun SettingsSupabaseRestoreConfirmDialog(
    userEmail: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        title = {
            Text(text = "Restore Cloud Progress", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                Text(
                    text = if (userEmail.isNotBlank()) {
                        "Pull down your latest saved progress, streaks, XP, and settings from Supabase Cloud for $userEmail?"
                    } else {
                        "Pull down your latest saved progress, streaks, XP, and settings from your connected Google account?"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            GrammarButton(
                text = "Restore Now",
                onClick = onConfirm,
                style = GrammarButtonStyle.PRIMARY
            )
        },
        dismissButton = {
            GrammarButton(
                text = "Cancel",
                onClick = onDismiss,
                style = GrammarButtonStyle.GHOST
            )
        }
    )
}
