package com.app.grammarai.data.remote

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiNativeAudioClient(
    private val apiKeyProvider: () -> String
) {
    companion object {
        private const val TAG = "GeminiNativeAudio"
        const val MODEL_LIVE = "models/gemini-2.5-flash-native-audio-latest"
        private const val WS_BASE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        const val DEFAULT_VOICE = "Despina"
        const val FALLBACK_VOICE = "Erinome"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read timeout for persistent WebSocket
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var activeSocket: WebSocket? = null

    fun connectPersistentLiveSession(
        systemInstruction: String,
        voiceName: String = DEFAULT_VOICE,
        onConnected: () -> Unit,
        onTranscriptChunk: (String) -> Unit,
        onUserTranscriptChunk: (String) -> Unit = {},
        onAudioChunk: (ByteArray) -> Unit,
        onTurnComplete: () -> Unit,
        onInterrupted: () -> Unit = {},
        onError: (Throwable) -> Unit
    ): WebSocket? {
        close()
        val key = apiKeyProvider()
        if (key.isBlank()) {
            val err = IllegalStateException("Gemini API Key is empty")
            Log.e(TAG, err.message ?: "")
            onError(err)
            return null
        }

        Log.d(TAG, "Connecting persistent Live WebSocket with model=$MODEL_LIVE...")
        val request = Request.Builder().url("$WS_BASE?key=$key").build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected. Sending setup payload...")
                val setupPayload = buildSetupJson(voiceName, systemInstruction)
                val sent = webSocket.send(setupPayload)
                Log.d(TAG, "Setup sent successfully: $sent")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLiveServerMessage(text, onConnected, onTranscriptChunk, onUserTranscriptChunk, onAudioChunk, onTurnComplete, onInterrupted)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleLiveServerMessage(bytes.utf8(), onConnected, onTranscriptChunk, onUserTranscriptChunk, onAudioChunk, onTurnComplete, onInterrupted)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket server closing: code=$code, reason=$reason")
                webSocket.close(code, reason)
                onError(IllegalStateException("Server closing Live WebSocket (code $code): $reason"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket server closed: code=$code, reason=$reason")
                onError(IllegalStateException("Server closed Live WebSocket (code $code): $reason"))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorBody = runCatching { response?.body?.string() }.getOrNull()
                val fullMsg = "WebSocket failure: ${t.message}${if (!errorBody.isNullOrBlank()) " | $errorBody" else ""}"
                Log.e(TAG, fullMsg, t)
                onError(IllegalStateException(fullMsg, t))
            }
        }

        val socket = httpClient.newWebSocket(request, listener)
        activeSocket = socket
        return socket
    }

    fun sendTurn(text: String): Boolean {
        val socket = activeSocket
        if (socket == null) {
            Log.e(TAG, "sendTurn failed: activeSocket is null")
            return false
        }
        val turnJson = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }))
                put("turnComplete", true)
            })
        }
        val sent = socket.send(turnJson.toString())
        Log.d(TAG, "sendTurn ('$text') sent result: $sent")
        return sent
    }

    /**
     * Envía bytes de audio crudo (PCM 16kHz, 16-bit, Mono) directamente al modelo por el WebSocket.
     */
    fun sendAudioChunk(pcmBytes: ByteArray): Boolean {
        val socket = activeSocket
        if (socket == null) {
            Log.e(TAG, "sendAudioChunk failed: activeSocket is null")
            return false
        }
        
        val base64Audio = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val turnJson = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", base64Audio)
                }))
            })
        }
        
        return socket.send(turnJson.toString())
    }

    private val audioSynthesizer = GeminiAudioSynthesizer(httpClient, apiKeyProvider, WS_BASE)

    suspend fun synthesizeSpeech(textToSpeak: String, voiceName: String = DEFAULT_VOICE): String? {
        return audioSynthesizer.synthesizeSpeech(textToSpeak, voiceName, FALLBACK_VOICE, ::buildSetupJson)
    }

    private fun handleLiveServerMessage(
        text: String,
        onConnected: () -> Unit,
        onTranscriptChunk: (String) -> Unit,
        onUserTranscriptChunk: (String) -> Unit,
        onAudioChunk: (ByteArray) -> Unit,
        onTurnComplete: () -> Unit,
        onInterrupted: () -> Unit = {}
    ) {
        runCatching {
            val root = JSONObject(text)
            if (root.has("setupComplete")) {
                Log.d(TAG, "Live WebSocket setupComplete received from server!")
                onConnected()
                return
            }
            val sc = root.optJSONObject("serverContent") ?: return
            
            // Check for server barge-in / interruption signal
            if (sc.optBoolean("interrupted") || root.optBoolean("interrupted")) {
                Log.d(TAG, "Live WebSocket interrupted received from server")
                onInterrupted()
            }

            // AI Spoken Output Transcript
            val transcript = sc.optJSONObject("outputTranscription")?.optString("text")
            if (!transcript.isNullOrEmpty()) {
                onTranscriptChunk(transcript)
            }

            // User Spoken Input Transcript (streamed back from Gemini Native Live)
            val userTranscript = sc.optJSONObject("inputTranscription")?.optString("text")
            if (!userTranscript.isNullOrEmpty()) {
                onUserTranscriptChunk(userTranscript)
            }

            val parts = sc.optJSONObject("modelTurn")?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val b64 = parts.getJSONObject(i).optJSONObject("inlineData")?.optString("data")
                    if (!b64.isNullOrEmpty()) {
                        val pcm = Base64.decode(b64, Base64.DEFAULT)
                        if (pcm.isNotEmpty()) onAudioChunk(pcm)
                    }
                }
            }
            if (sc.optBoolean("turnComplete")) {
                Log.d(TAG, "Live WebSocket turnComplete received")
                onTurnComplete()
            }
        }.onFailure { e ->
            Log.e(TAG, "handleLiveServerMessage parse error: ${e.message}")
        }
    }

    private fun buildSetupJson(voiceName: String, instruction: String): String {
        return JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", MODEL_LIVE)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName))
                        })
                    })
                })
                put("inputAudioTranscription", JSONObject())
                put("outputAudioTranscription", JSONObject())
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", instruction)))
                })
            })
        }.toString()
    }

    fun close() {
        runCatching {
            activeSocket?.close(1000, "Closed by client")
            activeSocket = null
        }
    }
}
