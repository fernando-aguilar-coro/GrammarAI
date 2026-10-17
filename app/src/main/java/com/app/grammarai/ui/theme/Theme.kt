package com.app.grammarai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GrammarAiColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = DarkBackground,
    primaryContainer = AccentPrimaryMuted,
    onPrimaryContainer = TextPrimary,
    secondary = AccentSlate,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    tertiary = NeonEmerald,
    onTertiary = DarkBackground
)

@Composable
fun GrammarAITheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GrammarAiColorScheme,
        typography = Typography,
        content = content
    )
}
