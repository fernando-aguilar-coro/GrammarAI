package com.app.grammarai.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// MODERN UTILITY PALETTE (Linear / Raycast / Notion Dark Aesthetic)
// =============================================================================

// Background & Surfaces (Warm graphite / carbon matte)
val DarkBackground = Color(0xFF121316)       // Primary matte canvas
val DarkSurface = Color(0xFF16181C)          // Secondary surface / nested panels
val DarkSurfaceCard = Color(0xFF1A1D21)      // Card surface
val DarkSurfaceElevated = Color(0xFF22262C)  // Hover / Active / Dropdowns
val DarkSurfaceCardElevated = DarkSurfaceElevated
val DarkCardBorder = Color(0xFF262A30)       // Hairline border (subtle 1px stroke)
val BorderSubtle = Color(0xFF262A30)
val BorderMedium = Color(0xFF343A43)
val BorderDivider = Color(0xFF1F2227)
val BorderActive = Color(0xFF3E4450)

// Brand & Focus Accents (Muted Terracotta & Slate Blue)
val AccentPrimary = Color(0xFFE07A5F)        // Muted Terracotta (Primary interactive)
val AccentPrimaryHover = Color(0xFFE59866)   // Warm Amber / Terracotta light
val AccentSlate = Color(0xFF4F86C6)          // Slate Blue (Alternative utility)
val AccentPrimaryMuted = Color(0x22E07A5F)

// Semantic States (Understated, non-punitive, and professional)
val SuccessEmerald = Color(0xFF34D399)       // Crisp modern emerald
val SuccessEmeraldMuted = Color(0x1F34D399)
val ErrorCrimson = Color(0xFFEF4444)         // Refined coral red
val ErrorCrimsonMuted = Color(0x1FEF4444)
val WarningAmber = Color(0xFFF59E0B)         // Warm amber gold
val WarningAmberMuted = Color(0x1FF59E0B)

// Backward-compatible aliases for existing screens mapped to new understated palette
val NeonCyan = Color(0xFFE07A5F)             // Replaces bright cyan with terracotta
val NeonCyanLight = Color(0xFFE59866)
val CyanButton = Color(0xFFE07A5F)
val CyanButtonDark = Color(0xFFC8674E)

val NeonEmerald = Color(0xFF34D399)          // Refined modern emerald
val NeonAmber = Color(0xFFF59E0B)            // Muted amber gold
val NeonViolet = Color(0xFF7E88A6)           // Muted slate periwinkle
val NeonPurpleGlow = Color(0x1F7E88A6)
val NeonOrange = Color(0xFFE59866)           // Warm Amber
val NeonPurple = Color(0xFF7E88A6)           // Muted purple

// Editorial Typography Palette
val TextPrimary = Color(0xFFF2F4F8)          // Bone white for crisp contrast
val TextSecondary = Color(0xFF8B949E)        // Neutral graphite for body & labels
val TextMuted = Color(0xFF555C66)            // Subtle indicators and bullets
val TextGreen = Color(0xFF7AB893)            // Soft desaturated green
val TextAmber = Color(0xFFE0AD67)            // Soft desaturated warm amber

// Subtle Card Gradients (Almost flat, gentle vignette)
val CardGradientStart = Color(0xFF1A1D21)
val CardGradientEnd = Color(0xFF181B1F)