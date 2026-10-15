package com.app.grammarai.core.audio.delivery

import android.util.Log

class OnDemandAudioPrefetcher(
    private val deliveryManager: PlayAssetDeliveryManager
) {
    companion object {
        private const val TAG = "OnDemandAudioPrefetch"
        // Lookahead margin: triggers background download 5 lessons before crossing tier boundary
        private const val LOOKAHEAD_LESSONS = 5
    }

    /**
     * Checks available/unlocked progression index and triggers background prefetch
     * for all tiers covering available lessons up to (highestAvailableLessonIndex + LOOKAHEAD_LESSONS).
     */
    fun checkPrefetch(highestAvailableLessonIndex: Int, lang: String = "es") {
        val targetIndex = highestAvailableLessonIndex + LOOKAHEAD_LESSONS
        var tier = 10
        while (tier <= targetIndex) {
            val packName = PlayAssetDeliveryManager.getPackName(lang, tier)
            if (packName != null && !deliveryManager.isPackInstalled(packName)) {
                Log.d(TAG, "Available lessons reach index $highestAvailableLessonIndex. Triggering prefetch for upcoming $packName.")
                deliveryManager.requestPackDownload(packName)
            }
            tier += 10
        }
    }

    /**
     * Ensures audio packs for all currently available/unlocked lesson indices are prefetched.
     */
    fun prefetchForAvailableIndices(availableIndices: Collection<Int>, lang: String = "es") {
        if (availableIndices.isEmpty()) return
        val maxAvailable = availableIndices.maxOrNull() ?: 0
        checkPrefetch(maxAvailable, lang)
        availableIndices.filter { it >= 10 }.forEach { idx ->
            ensurePackForLessonIndex(idx, lang)
        }
    }

    /**
     * Ensures the audio pack for a specific lesson is downloaded or initiates download.
     */
    fun ensurePackForLessonIndex(lessonIndex: Int, lang: String = "es") {
        val packName = PlayAssetDeliveryManager.getPackName(lang, lessonIndex)
        if (packName != null && !deliveryManager.isPackInstalled(packName)) {
            Log.d(TAG, "Ensuring pack $packName for lesson index $lessonIndex.")
            deliveryManager.requestPackDownload(packName)
        }
    }

    fun isAudioAvailableForLessonIndex(lessonIndex: Int, lang: String = "es"): Boolean {
        if (lessonIndex < 10) return true // In Base APK assets
        val packName = PlayAssetDeliveryManager.getPackName(lang, lessonIndex) ?: return true
        return deliveryManager.isPackInstalled(packName)
    }
}
