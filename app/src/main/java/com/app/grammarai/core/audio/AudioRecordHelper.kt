package com.app.grammarai.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages low-latency 16kHz PCM audio recording for Gemini Live.
 * Reuses the native AudioRecord instance across turns to prevent
 * AudioFlinger client exhaustion (which previously caused mic failure at turn 4).
 */
class AudioRecordHelper(private val context: Context) {
    companion object {
        private const val TAG = "AudioRecordHelper"
        private const val SAMPLE_RATE = 16000
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var onChunkReadyCallback: ((ByteArray) -> Unit)? = null

    @Volatile
    var isMuted: Boolean = false
        private set

    @Volatile
    private var isRecordingLoopActive: Boolean = false

    val isRecording: Boolean get() = audioRecord != null && isRecordingLoopActive && !isMuted

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    @Synchronized
    fun startRecording(onChunkReady: (ByteArray) -> Unit, onError: (Exception) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError(SecurityException("Microphone permission not granted"))
            return
        }

        onChunkReadyCallback = onChunkReady
        isMuted = false

        // If AudioRecord is already initialized and thread is reading, simply unmute!
        val currentRecord = audioRecord
        if (currentRecord != null && currentRecord.state == AudioRecord.STATE_INITIALIZED && isRecordingLoopActive) {
            try {
                if (currentRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    currentRecord.startRecording()
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume existing AudioRecord, re-initializing: ${e.message}")
                stopRecording()
            }
        }

        stopRecording()
        isMuted = false

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            // Try VOICE_COMMUNICATION first to enable Android OS hardware Acoustic Echo Cancellation (AEC)
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord VOICE_COMMUNICATION init exception: ${e.message}")
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                try {
                    record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        SAMPLE_RATE,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "AudioRecord VOICE_RECOGNITION init exception: ${e.message}")
                }
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                onError(IllegalStateException("AudioRecord initialization failed"))
                return
            }

            try {
                if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                    android.media.audiofx.AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                        enabled = true
                        Log.d(TAG, "Hardware AcousticEchoCanceler enabled on session ${record.audioSessionId}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "AcousticEchoCanceler init exception: ${e.message}")
            }

            try {
                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    android.media.audiofx.NoiseSuppressor.create(record.audioSessionId)?.apply {
                        enabled = true
                        Log.d(TAG, "Hardware NoiseSuppressor enabled on session ${record.audioSessionId}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "NoiseSuppressor init exception: ${e.message}")
            }

            audioRecord = record
            record.startRecording()
            isRecordingLoopActive = true

            recordingJob = scope.launch {
                val buffer = ByteArray(2048) 
                while (isActive && isRecordingLoopActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0 && !isMuted) {
                        val chunk = buffer.copyOf(read)
                        onChunkReadyCallback?.invoke(chunk)
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord read error code: $read")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    @Synchronized
    fun stopRecording() {
        isRecordingLoopActive = false
        isMuted = true
        recordingJob?.cancel()
        recordingJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping/releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    fun release() {
        stopRecording()
    }
}
