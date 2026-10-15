package com.app.grammarai.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.app.grammarai.GrammarApplication

class AudioPlayerHelper(
    private val context: Context = GrammarApplication.instance
) {

    companion object {
        private const val TAG = "AudioPlayerHelper"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val pcmTrackHelper = PcmTrackAudioHelper()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPlayToken: Long = 0L

    private val ttsHelper = TtsAudioHelper(context)
    private val wavHelper = WavAudioHelper(context)

    fun ensureTtsInitialized(onReady: (() -> Unit)? = null) {
        ttsHelper.ensureInitialized(onReady)
    }

    private fun getOrCreatePlayer(): MediaPlayer {
        val current = mediaPlayer
        if (current != null) {
            try {
                current.reset()
                return current
            } catch (e: Exception) {
                releasePlayer()
            }
        }
        val fresh = MediaPlayer()
        mediaPlayer = fresh
        return fresh
    }

    fun playAudio(
        base64Pcm: String?,
        fallbackText: String = "",
        allowTtsFallback: Boolean = false,
        targetLocale: java.util.Locale? = null,
        onStart: () -> Unit = {},
        onCompletion: () -> Unit = {}
    ) {
        stop()
        if (!base64Pcm.isNullOrBlank()) {
            val pcmBytes = runCatching { Base64.decode(base64Pcm, Base64.DEFAULT) }.getOrNull()
            if (pcmBytes != null && pcmBytes.isNotEmpty()) {
                Log.d(TAG, "playAudio: Decoded ${pcmBytes.size} PCM bytes. Playing via MediaPlayer...")
                if (playPcmBytes(pcmBytes, onStart, onCompletion)) return
            } else {
                showToast("Audio error: Failed to decode base64 audio stream.")
                Log.w(TAG, "playAudio: Base64 decode failed or empty bytes")
            }
        } else {
            Log.w(TAG, "playAudio: base64Pcm is null or blank")
        }

        if (allowTtsFallback && fallbackText.isNotBlank()) {
            Log.d(TAG, "playAudio: Using TTS fallback for text: '$fallbackText' (targetLocale: $targetLocale)")
            ttsHelper.speak(fallbackText, currentPlayToken, { currentPlayToken }, targetLocale, onStart, onCompletion)
        } else {
            Log.w(TAG, "playAudio: No audio to play and allowTtsFallback is false")
            mainHandler.post { onCompletion() }
        }
    }

    /**
     * Plays an audio file from on-demand asset packs or directly from app assets.
     */
    fun playAssetAudio(
        assetPath: String,
        fallbackText: String = "",
        allowTtsFallback: Boolean = false,
        targetLocale: java.util.Locale? = null,
        onStart: () -> Unit = {},
        onCompletion: () -> Unit = {}
    ): Boolean {
        // 1. Check if asset pack has the file on disk
        val packFile = runCatching {
            GrammarApplication.instance.playAssetDeliveryManager.getAssetFile(assetPath)
        }.getOrNull()

        if (packFile != null && packFile.exists() && packFile.length() > 0) {
            return playFileAudio(
                file = packFile,
                fallbackText = fallbackText,
                allowTtsFallback = allowTtsFallback,
                targetLocale = targetLocale,
                onStart = onStart,
                onCompletion = onCompletion
            )
        }

        stop()
        val token = currentPlayToken
        return try {
            val afd = context.assets.openFd(assetPath)
            val player = getOrCreatePlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setOnPreparedListener { mp ->
                if (token != currentPlayToken) return@setOnPreparedListener
                Log.d(TAG, "Asset MediaPlayer prepared for $assetPath. Starting...")
                mp.start()
                mainHandler.post { onStart() }
            }
            player.setOnCompletionListener {
                if (token != currentPlayToken) return@setOnCompletionListener
                Log.d(TAG, "Asset MediaPlayer completed for $assetPath")
                mainHandler.post { onCompletion() }
            }
            player.setOnErrorListener { _, what, extra ->
                if (token != currentPlayToken) return@setOnErrorListener true
                val errorMsg = "Audio playback error for $assetPath: code $what, extra $extra"
                Log.e(TAG, errorMsg)
                showToast(errorMsg)
                mainHandler.post { onCompletion() }
                releasePlayer()
                true
            }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            val fileName = assetPath.substringAfterLast('/')
            val errorMsg = "Audio clip not found: $fileName"
            Log.w(TAG, "$errorMsg - ${e.message}")
            if (allowTtsFallback && fallbackText.isNotBlank()) {
                ttsHelper.speak(fallbackText, token, { currentPlayToken }, targetLocale, onStart, onCompletion)
                true
            } else {
                showToast(errorMsg)
                mainHandler.post { onCompletion() }
                false
            }
        }
    }

    /**
     * Plays an audio file directly from device disk storage (extracted asset pack).
     */
    fun playFileAudio(
        file: java.io.File,
        fallbackText: String = "",
        allowTtsFallback: Boolean = false,
        targetLocale: java.util.Locale? = null,
        onStart: () -> Unit = {},
        onCompletion: () -> Unit = {}
    ): Boolean {
        stop()
        val token = currentPlayToken
        if (!file.exists() || file.length() == 0L) {
            if (allowTtsFallback && fallbackText.isNotBlank()) {
                ttsHelper.speak(fallbackText, token, { currentPlayToken }, targetLocale, onStart, onCompletion)
                return true
            }
            mainHandler.post { onCompletion() }
            return false
        }

        return try {
            val player = getOrCreatePlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { mp ->
                if (token != currentPlayToken) return@setOnPreparedListener
                Log.d(TAG, "File MediaPlayer prepared for ${file.name}. Starting...")
                mp.start()
                mainHandler.post { onStart() }
            }
            player.setOnCompletionListener {
                if (token != currentPlayToken) return@setOnCompletionListener
                Log.d(TAG, "File MediaPlayer completed for ${file.name}")
                mainHandler.post { onCompletion() }
            }
            player.setOnErrorListener { _, what, extra ->
                if (token != currentPlayToken) return@setOnErrorListener true
                val errorMsg = "Audio playback error for ${file.name}: code $what, extra $extra"
                Log.e(TAG, errorMsg)
                showToast(errorMsg)
                mainHandler.post { onCompletion() }
                releasePlayer()
                true
            }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            val errorMsg = "Audio playback error for ${file.name}: ${e.message}"
            Log.w(TAG, errorMsg)
            if (allowTtsFallback && fallbackText.isNotBlank()) {
                ttsHelper.speak(fallbackText, token, { currentPlayToken }, targetLocale, onStart, onCompletion)
                true
            } else {
                showToast(errorMsg)
                mainHandler.post { onCompletion() }
                false
            }
        }
    }

    fun playPcmBytes(
        pcmBytes: ByteArray,
        onStart: () -> Unit = {},
        onCompletion: () -> Unit = {}
    ): Boolean {
        stop()
        val token = currentPlayToken
        if (pcmBytes.isEmpty()) {
            mainHandler.post { onCompletion() }
            return false
        }
        return try {
            pcmTrackHelper.play(
                pcmBytes = pcmBytes,
                token = token,
                currentTokenProvider = { currentPlayToken },
                onStart = onStart,
                onCompletion = onCompletion
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "playPcmBytes AudioTrack failed, falling back to WAV: ${e.message}", e)
            wavHelper.playWavFallback(
                player = getOrCreatePlayer(),
                pcmBytes = pcmBytes,
                token = token,
                currentTokenProvider = { currentPlayToken },
                onStart = onStart,
                onCompletion = onCompletion
            )
        }
    }

    fun stop() {
        currentPlayToken++
        pcmTrackHelper.stop()

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
        } catch (e: Exception) {
            releasePlayer()
        }
        ttsHelper.stop()
    }

    fun release() {
        stop()
        releasePlayer()
        ttsHelper.release()
    }

    private fun releasePlayer() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            runCatching {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
