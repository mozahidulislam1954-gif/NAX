package com.example.audio

import android.util.Base64
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class LiveAudioSession(
    private val apiKey: String,
    private val onStateChange: (State) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onAppAction: (String) -> Unit
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
                                addJsonObject { put("text", "You are MeMax, a highly capable, serious, and professional AI assistant. Your primary goal is to assist the user efficiently and reliably. Complete any task given to you directly without unnecessary banter. If the user asks to open an app (e.g., YouTube, Camera, Maps), you MUST call the 'open_app' tool immediately.") }
                            }
                        }
                        putJsonArray("tools") {
                            addJsonObject {
                                putJsonArray("functionDeclarations") {
                                    addJsonObject {
                                        put("name", "open_app")
                                        put("description", "Opens an application on the user's Android phone. Only call this when the user explicitly asks to open an app (e.g. 'open youtube', 'start maps', 'launch browser', 'open camera').")
                                        putJsonObject("parameters") {
                                            put("type", "OBJECT")
                                            putJsonObject("properties") {
                                                putJsonObject("app_name") {
                                                    put("type", "STRING")
                                                    put("description", "The name of the app to open. Examples: 'youtube', 'maps', 'browser', 'camera', 'calculator'")
                                                }
                                            }
                                            putJsonArray("required") { add("app_name") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                webSocket.send(setup.toString())
                
                val initialMessage = buildJsonObject {
                    putJsonObject("clientContent") {
                        putJsonArray("turns") {
                            addJsonObject {
                                put("role", "user")
                                putJsonArray("parts") {
                                    addJsonObject {
                                        put("text", "Hello! Please greet me briefly and tell me you are ready.")
                                    }
                                }
                            }
                        }
                        put("turnComplete", true)
                    }
                }
                webSocket.send(initialMessage.toString())
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
                            if (part.containsKey("functionCall")) {
                                val functionCall = part["functionCall"]?.jsonObject
                                val callName = functionCall?.get("name")?.jsonPrimitive?.content
                                val callId = functionCall?.get("id")?.jsonPrimitive?.content
                                if (callName == "open_app") {
                                    val args = functionCall["args"]?.jsonObject
                                    val appName = args?.get("app_name")?.jsonPrimitive?.content
                                    if (appName != null) {
                                        onAppAction(appName)
                                        sendToolResponse(callName, callId, "App $appName launched successfully.")
                                    }
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

    private fun sendToolResponse(name: String, id: String?, result: String) {
        if (webSocket == null) return
        val msg = buildJsonObject {
            putJsonObject("toolResponse") {
                putJsonArray("functionResponses") {
                    addJsonObject {
                        if (id != null) put("id", id)
                        put("name", name)
                        putJsonObject("response") {
                            put("result", result)
                        }
                    }
                }
            }
        }
        webSocket?.send(msg.toString())
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
