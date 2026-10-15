package com.app.grammarai.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Continuous streaming audio player using Android AudioTrack in MODE_STREAM.
 * Provides:
 * 1. Immediate playback feeding on incoming PCM chunks with ~200ms pre-buffering to prevent underrun clicks.
 * 2. Instantaneous barge-in / interruption handling (pause -> flush -> play).
 * 3. Low latency turn completion synchronization.
 */
class PcmStreamAudioPlayer(
    private val sampleRate: Int = WavAudioHelper.SAMPLE_RATE, // 24000
    private val preBufferBytes: Int = 9600 // ~200ms for 24kHz 16-bit mono PCM (48,000 bytes/sec)
) {
    companion object {
        private const val TAG = "PcmStreamAudioPlayer"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioTrack: AudioTrack? = null
    private val chunkQueue: BlockingQueue<ByteArray> = LinkedBlockingQueue()

    @Volatile
    private var isPlayingStarted = false

    @Volatile
    private var accumulatedPreBuffer = 0

    @Volatile
    private var accumulatedChunks = 0

    @Volatile
    private var totalBytesWrittenInTurn = 0L

    @Volatile
    private var isWorkerRunning = false

    @Volatile
    private var isTurnCompletePending = false

    private var workerThread: Thread? = null

    private var onPlaybackStartedCallback: (() -> Unit)? = null
    private var onTurnFinishedCallback: (() -> Unit)? = null

    @Synchronized
    fun prepare(
        onPlaybackStarted: () -> Unit = {},
        onTurnFinished: () -> Unit = {}
    ) {
        stop()
        this.onPlaybackStartedCallback = onPlaybackStarted
        this.onTurnFinishedCallback = onTurnFinished

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Ensure at least 1 second buffer in AudioTrack to absorb network jitter
        val bufferSize = maxOf(minBuf * 4, 48000)

        audioTrack = AudioTrack.Builder()
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

        isPlayingStarted = false
        accumulatedPreBuffer = 0
        accumulatedChunks = 0
        totalBytesWrittenInTurn = 0L
        isTurnCompletePending = false
        chunkQueue.clear()

        isWorkerRunning = true
        workerThread = Thread({ runPlaybackLoop() }, "PcmStreamPlayerThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun writeChunk(pcm: ByteArray) {
        if (!isWorkerRunning || pcm.isEmpty()) return
        chunkQueue.offer(pcm)
    }

    /**
     * Instantly interrupts playback on barge-in / user speech.
     * Clears pending queue and flushes hardware buffer immediately.
     */
    fun handleInterruption() {
        Log.d(TAG, "Barge-in: Interrupting AI streaming playback immediately")
        chunkQueue.clear()
        isTurnCompletePending = false
        val track = audioTrack
        if (track != null) {
            runCatching {
                track.pause()
                track.flush()
                track.play()
            }
        }
        accumulatedPreBuffer = 0
        accumulatedChunks = 0
        totalBytesWrittenInTurn = 0L
        isPlayingStarted = false
    }

    fun onTurnComplete(onFinished: (() -> Unit)? = null) {
        if (onFinished != null) {
            this.onTurnFinishedCallback = onFinished
        }
        isTurnCompletePending = true
    }

    private fun runPlaybackLoop() {
        while (isWorkerRunning) {
            try {
                val chunk = chunkQueue.poll(40, TimeUnit.MILLISECONDS)
                val track = audioTrack ?: break

                if (chunk != null && chunk.isNotEmpty()) {
                    var written = 0
                    while (written < chunk.size && isWorkerRunning) {
                        val ret = track.write(chunk, written, chunk.size - written, AudioTrack.WRITE_BLOCKING)
                        if (ret <= 0) break
                        written += ret
                    }
                    totalBytesWrittenInTurn += written

                    if (!isPlayingStarted) {
                        accumulatedPreBuffer += written
                        accumulatedChunks++
                        // Start AudioTrack once ~200ms pre-buffer or 2-3 chunks reached
                        if (accumulatedPreBuffer >= preBufferBytes || accumulatedChunks >= 3 || isTurnCompletePending) {
                            startPlayback(track)
                        }
                    }
                } else if (isTurnCompletePending && chunkQueue.isEmpty()) {
                    // Turn complete and no more pending chunks in queue
                    if (!isPlayingStarted && accumulatedPreBuffer > 0) {
                        startPlayback(track)
                    }

                    // Await the remaining frames in the hardware buffer
                    drainRemainingAudio(track)

                    isTurnCompletePending = false
                    accumulatedPreBuffer = 0
                    accumulatedChunks = 0
                    totalBytesWrittenInTurn = 0L
                    isPlayingStarted = false

                    mainHandler.post {
                        onTurnFinishedCallback?.invoke()
                    }
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming audio loop: ${e.message}", e)
            }
        }
    }

    private fun startPlayback(track: AudioTrack) {
        if (isPlayingStarted) return
        isPlayingStarted = true
        runCatching {
            track.play()
        }
        mainHandler.post {
            onPlaybackStartedCallback?.invoke()
        }
    }

    private fun drainRemainingAudio(track: AudioTrack) {
        runCatching {
            val totalFrames = totalBytesWrittenInTurn / 2L
            val currentPlayhead = track.playbackHeadPosition.toLong()
            val remainingFrames = totalFrames - currentPlayhead
            val remainingMs = if (remainingFrames > 0) {
                (remainingFrames * 1000L) / sampleRate
            } else {
                100L
            }
            Thread.sleep((remainingMs + 60L).coerceIn(40L, 3000L))
        }
    }

    @Synchronized
    fun stop() {
        isWorkerRunning = false
        isTurnCompletePending = false
        chunkQueue.clear()
        workerThread?.interrupt()
        workerThread = null

        val track = audioTrack
        audioTrack = null
        if (track != null) {
            runCatching {
                track.pause()
                track.flush()
                track.stop()
                track.release()
            }
        }
        accumulatedPreBuffer = 0
        accumulatedChunks = 0
        totalBytesWrittenInTurn = 0L
        isPlayingStarted = false
    }

    fun release() {
        stop()
    }
}
