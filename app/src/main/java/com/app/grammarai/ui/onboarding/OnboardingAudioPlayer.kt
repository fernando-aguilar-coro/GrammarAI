package com.app.grammarai.ui.onboarding

import com.app.grammarai.core.audio.AudioPlayerHelper

class OnboardingAudioPlayer {

    private val audioPlayer = AudioPlayerHelper()

    fun playAudio(
        messageId: String,
        text: String,
        appLanguage: String,
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()

        val lang = appLanguage.lowercase().takeIf { it in listOf("es", "en") } ?: "es"
        val candidates = listOf(
            "audio/$lang/$messageId.m4a",
            "audio/$messageId.m4a",
            "audio/$lang/$messageId.wav",
            "audio/$messageId.wav"
        )

        for (path in candidates) {
            val played = audioPlayer.playAssetAudio(
                assetPath = path,
                fallbackText = "",
                allowTtsFallback = false,
                onStart = onStart,
                onCompletion = onCompletion
            )
            if (played) return
        }

        val expectedClip = "audio/$lang/$messageId.m4a"
        val errorMsg = "Audio clip not found: $expectedClip"
        android.util.Log.w("OnboardingAudioPlayer", "$errorMsg. Falling back to Android TTS: '$text'")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(com.app.grammarai.GrammarApplication.instance, "$errorMsg (Using Android TTS)", android.widget.Toast.LENGTH_SHORT).show()
        }

        audioPlayer.playAudio(
            base64Pcm = null,
            fallbackText = text,
            allowTtsFallback = true,
            onStart = onStart,
            onCompletion = onCompletion
        )
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun release() {
        audioPlayer.release()
    }
}
