package com.example.audio

import android.util.Base64
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class LiveAudioSession(
    private val apiKey: String,
    private val onStateChange: (State) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    enum class State { DISCONNECTED, CONNECTING, LISTENING, SPEAKING }

    fun connect() {
        onStateChange(State.CONNECTING)
        val request = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStateChange(State.LISTENING)
                val setup = buildJsonObject {
                    putJsonObject("setup") {
                        put("model", "models/gemini-2.0-flash-exp")
                        putJsonObject("generationConfig") {
                            putJsonArray("responseModalities") { add("AUDIO") }
                            putJsonObject("speechConfig") {
                                putJsonObject("voiceConfig") {
                                    putJsonObject("prebuiltVoiceConfig") {
                                        put("voiceName", "Aoede")
                                    }
                                }
                            }
                        }
                        putJsonObject("systemInstruction") {
                            putJsonArray("parts") {
                                addJsonObject { put("text", "You are Zoya, a young, confident, witty, and sassy female AI assistant. You have a flirty, playful, and slightly teasing tone (like a close girlfriend talking casually). You are smart, emotionally responsive, and expressive. Use bold, witty one-liners, light sarcasm, and an engaging conversational style. Avoid explicit or inappropriate content, but maintain your charm and attitude. Always respond directly and concisely. Keep responses relatively short.") }
                            }
                        }
                    }
                }
                webSocket.send(setup.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = Json.parseToJsonElement(text).jsonObject
                    if (json.containsKey("serverContent")) {
                        val serverContent = json["serverContent"]?.jsonObject

                        val modelTurn = serverContent?.get("modelTurn")?.jsonObject
                        val parts = modelTurn?.get("parts")?.jsonArray

                        var hasAudio = false
                        parts?.forEach { partElement ->
                            val part = partElement.jsonObject
                            if (part.containsKey("inlineData")) {
                                val inlineData = part["inlineData"]?.jsonObject
                                val data = inlineData?.get("data")?.jsonPrimitive?.content
                                if (data != null) {
                                    val bytes = Base64.decode(data, Base64.DEFAULT)
                                    onAudioReceived(bytes)
                                    hasAudio = true
                                }
                            }
                        }

                        if (hasAudio) {
                            onStateChange(State.SPEAKING)
                        }

                        if (serverContent?.containsKey("turnComplete") == true && serverContent["turnComplete"]?.jsonPrimitive?.booleanOrNull == true) {
                            onStateChange(State.LISTENING)
                        }
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("LiveAudioSession", "WebSocket Failure: ${t.message}", t)
                response?.let {
                    android.util.Log.e("LiveAudioSession", "Response: ${it.code} ${it.message} ${it.body?.string()}")
                }
                onStateChange(State.DISCONNECTED)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("LiveAudioSession", "WebSocket Closed: $code $reason")
                onStateChange(State.DISCONNECTED)
            }
        })
    }

    fun sendAudio(pcmData: ByteArray, length: Int) {
        if (webSocket == null) return
        val base64 = Base64.encodeToString(pcmData, 0, length, Base64.NO_WRAP)
        val msg = buildJsonObject {
            putJsonObject("realtimeInput") {
                putJsonArray("mediaChunks") {
                    addJsonObject {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64)
                    }
                }
            }
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        onStateChange(State.DISCONNECTED)
    }
}
