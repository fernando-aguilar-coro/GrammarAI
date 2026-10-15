package com.app.grammarai.ui.common.animation

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean tactile feedback helpers providing standardized haptic intensities
 * across GrammarAI.
 */

/**
 * Standard subtle tactile tick for selections, toggles, and card touch-downs.
 */
fun HapticFeedback.tick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/**
 * Firm confirmation haptic for major actions, CTA launches, and completions.
 */
fun HapticFeedback.confirm() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Double-pulse tactile feedback (Tick-Tick 75ms apart) for successful answers,
 * matching pairs, and milestone completions.
 */
fun HapticFeedback.doubleTick(scope: CoroutineScope) {
    scope.launch {
        tick()
        delay(75)
        tick()
    }
}
