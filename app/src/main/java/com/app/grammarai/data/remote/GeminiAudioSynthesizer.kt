package com.app.grammarai.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class GeminiAudioSynthesizer(
    private val httpClient: OkHttpClient,
    private val apiKeyProvider: () -> String,
    private val wsBase: String
) {
    companion object {
        private const val TAG = "GeminiAudioSynthesizer"
    }

    suspend fun synthesizeSpeech(
        textToSpeak: String,
        voiceName: String,
        fallbackVoice: String,
        buildSetupJson: (String, String) -> String
    ): String? {
        val result = synthesizeSpeechInternal(textToSpeak, voiceName, buildSetupJson)
        if (result != null) return result
        if (voiceName != fallbackVoice) {
            Log.w(TAG, "synthesizeSpeech with $voiceName failed, attempting fallback to $fallbackVoice")
            return synthesizeSpeechInternal(textToSpeak, fallbackVoice, buildSetupJson)
        }
        return null
    }

    private suspend fun synthesizeSpeechInternal(
        textToSpeak: String,
        voiceName: String,
        buildSetupJson: (String, String) -> String
    ): String? {
        val key = apiKeyProvider()
        if (key.isBlank() || textToSpeak.isBlank()) {
            Log.w(TAG, "synthesizeSpeech aborted: key or text is empty")
            return null
        }

        Log.d(TAG, "synthesizeSpeech starting for: '$textToSpeak' with voice $voiceName")
        val deferred = CompletableDeferred<String?>()
        val audioBuffer = ByteArrayOutputStream()
        val request = Request.Builder().url("$wsBase?key=$key").build()

        val socket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "synthesizeSpeech onOpen. Sending setup...")
                webSocket.send(
                    buildSetupJson(
                        voiceName,
                        "You are a professional English speaker. Read the text aloud clearly and naturally. Do NOT add any greeting or commentary."
                    )
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSynthesizeMessage(text, audioBuffer, deferred, webSocket, textToSpeak)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleSynthesizeMessage(bytes.utf8(), audioBuffer, deferred, webSocket, textToSpeak)
            }

            private fun handleSynthesizeMessage(
                text: String,
                buffer: ByteArrayOutputStream,
                def: CompletableDeferred<String?>,
                ws: WebSocket,
                phrase: String
            ) {
                try {
                    val root = JSONObject(text)
                    if (root.has("setupComplete")) {
                        Log.d(TAG, "synthesizeSpeech setup complete. Sending turn text...")
                        val turn = JSONObject().apply {
                            put("clientContent", JSONObject().apply {
                                put("turns", JSONArray().put(JSONObject().apply {
                                    put("role", "user")
                                    put("parts", JSONArray().put(JSONObject().put("text", "Read: '$phrase'")))
                                }))
                                put("turnComplete", true)
                            })
                        }
                        ws.send(turn.toString())
                    }
                    val parts = root.optJSONObject("serverContent")?.optJSONObject("modelTurn")?.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val chunk = parts.getJSONObject(i).optJSONObject("inlineData")?.optString("data")
                            if (!chunk.isNullOrBlank()) {
                                buffer.write(Base64.decode(chunk, Base64.DEFAULT))
                            }
                        }
                    }
                    if (root.optJSONObject("serverContent")?.optBoolean("turnComplete") == true) {
                        val pcmBytes = buffer.toByteArray()
                        Log.d(TAG, "synthesizeSpeech finished! Generated ${pcmBytes.size} PCM bytes")
                        def.complete(Base64.encodeToString(pcmBytes, Base64.NO_WRAP))
                        ws.close(1000, "Done")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "synthesizeSpeech message parsing error: ${e.message}")
                    def.complete(null)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (!deferred.isCompleted) deferred.complete(null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!deferred.isCompleted) deferred.complete(null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val err = response?.body?.string()
                Log.e(TAG, "synthesizeSpeech onFailure: ${t.message} | $err")
                if (!deferred.isCompleted) deferred.complete(null)
            }
        })

        val result = withTimeoutOrNull(20000) { deferred.await() }
        runCatching { socket.close(1000, "Finished") }
        return result
    }
}
