package com.app.grammarai.core.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class SpeechRecognizerHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognizerHelper"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {},
        onListeningState: (Boolean) -> Unit = {}
    ) {
        stopListening()

        if (!hasRecordAudioPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission missing!")
            onListeningState(false)
            onError("Permiso de micrófono no concedido")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer is NOT available on this device!")
            onListeningState(false)
            onError("Reconocimiento de voz no disponible en el dispositivo")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech: ready to listen")
                    onListeningState(true)
                }
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech: user started speaking")
                    onListeningState(true)
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech: speech ended, waiting for results")
                }
                override fun onError(error: Int) {
                    onListeningState(false)
                    val desc = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Error de grabación de audio"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono insuficiente"
                        SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera agotado"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó ninguna palabra"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera de voz agotado"
                        else -> "Error de reconocimiento: $error"
                    }
                    Log.e(TAG, "Recognition error: $desc ($error)")
                    onError(desc)
                }
                override fun onResults(results: Bundle?) {
                    onListeningState(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull() ?: ""
                    Log.d(TAG, "onResults transcript: '$transcript'")
                    if (transcript.isNotBlank()) onResult(transcript)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("en-US", "es-ES", "es-US", "en-GB"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        Log.d(TAG, "Starting SpeechRecognizer listening intent...")
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun release() = stopListening()
}
