package com.app.grammarai.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper

class PcmTrackAudioHelper {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentAudioTrack: AudioTrack? = null

    fun play(
        pcmBytes: ByteArray,
        token: Long,
        currentTokenProvider: () -> Long,
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ): AudioTrack {
        val sampleRate = WavAudioHelper.SAMPLE_RATE
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(minBuf * 2, pcmBytes.size)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        currentAudioTrack = track
        track.play()
        mainHandler.post { onStart() }

        Thread {
            try {
                var written = 0
                val chunkSize = 4096
                while (written < pcmBytes.size && token == currentTokenProvider()) {
                    val len = minOf(chunkSize, pcmBytes.size - written)
                    val ret = track.write(pcmBytes, written, len)
                    if (ret <= 0) break
                    written += ret
                }
                val bytesPerSample = WavAudioHelper.BITS_PER_SAMPLE / 8
                val totalDurationMs = (pcmBytes.size * 1000L) / (sampleRate * WavAudioHelper.CHANNELS * bytesPerSample)
                Thread.sleep(totalDurationMs.coerceAtLeast(60L))
                if (token == currentTokenProvider()) {
                    mainHandler.post { onCompletion() }
                }
            } catch (_: Exception) {
                if (token == currentTokenProvider()) {
                    mainHandler.post { onCompletion() }
                }
            } finally {
                if (token == currentTokenProvider()) {
                    runCatching {
                        track.stop()
                        track.release()
                    }
                    if (currentAudioTrack == track) currentAudioTrack = null
                }
            }
        }.start()
        return track
    }

    fun stop() {
        runCatching {
            currentAudioTrack?.pause()
            currentAudioTrack?.flush()
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
        }
        currentAudioTrack = null
    }
}
