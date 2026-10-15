package com.app.grammarai.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavAudioHelper(
    private val context: Context
) {
    companion object {
        private const val TAG = "WavAudioHelper"
        const val SAMPLE_RATE = 24000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun createWavFile(pcmData: ByteArray): File {
        val file = File.createTempFile("speech_", ".wav", context.cacheDir)
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
        val blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8)
        val totalAudioLen = pcmData.size

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalAudioLen + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort()) // PCM format
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(totalAudioLen)
        }.array()

        FileOutputStream(file).use { fos ->
            fos.write(header)
            fos.write(pcmData)
            fos.flush()
        }
        return file
    }

    fun playWavFallback(
        player: MediaPlayer,
        pcmBytes: ByteArray,
        token: Long,
        currentTokenProvider: () -> Long,
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ): Boolean {
        return try {
            val wavFile = createWavFile(pcmBytes)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(wavFile.absolutePath)
            player.setOnPreparedListener { mp ->
                if (token != currentTokenProvider()) {
                    wavFile.delete()
                    return@setOnPreparedListener
                }
                mp.start()
                mainHandler.post { onStart() }
            }
            player.setOnCompletionListener {
                if (token != currentTokenProvider()) {
                    wavFile.delete()
                    return@setOnCompletionListener
                }
                mainHandler.post { onCompletion() }
                wavFile.delete()
            }
            player.setOnErrorListener { _, _, _ ->
                wavFile.delete()
                mainHandler.post { onCompletion() }
                true
            }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            Log.e(TAG, "playWavFallback failed: ${e.message}", e)
            false
        }
    }
}
