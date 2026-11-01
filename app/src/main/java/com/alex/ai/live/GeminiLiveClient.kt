package com.alex.ai.live

import android.util.Base64
import android.util.Log
import com.alex.ai.domain.Personality
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    INTERRUPTED,
    ERROR
}

interface GeminiLiveListener {
    fun onConnected()
    fun onDisconnected(reason: String)
    fun onAudioChunkReceived(pcmData: ByteArray)
    fun onTranscriptReceived(text: String, isModel: Boolean)
    fun onInterrupted()
    fun onTurnComplete()
    fun onToolCall(id: String, name: String, args: JsonObject?)
    fun onError(error: Throwable)
}

class GeminiLiveClient(
    private val listener: GeminiLiveListener
) {
    private val tag = "GeminiLiveClient"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(LiveConnectionState.DISCONNECTED)
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private var currentApiKey: String = ""
    private var currentPersonality: Personality? = null
    private var currentLanguage: String = "hinglish"

    fun connect(apiKey: String, personality: Personality, language: String) {
        if (_connectionState.value == LiveConnectionState.CONNECTED ||
            _connectionState.value == LiveConnectionState.STREAMING) {
            disconnect()
        }

        currentApiKey = apiKey
        currentPersonality = personality
        currentLanguage = language

        _connectionState.value = LiveConnectionState.CONNECTING
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened")
                _connectionState.value = LiveConnectionState.CONNECTED
                listener.onConnected()

                // Send the initial Setup Frame
                sendSetupFrame(ws, personality, language)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure: ${t.message}", t)
                _connectionState.value = LiveConnectionState.ERROR
                listener.onError(t)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $code / $reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: $code / $reason")
                _connectionState.value = LiveConnectionState.DISCONNECTED
                listener.onDisconnected(reason)
            }
        })
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            Log.e(tag, "Error closing WebSocket", e)
        }
        webSocket = null
        _connectionState.value = LiveConnectionState.DISCONNECTED
    }

    /**
     * Send initial BidiGenerateContentSetup message with tool schemas
     */
    private fun sendSetupFrame(ws: WebSocket, personality: Personality, language: String) {
        val compositePrompt = buildCompositeSystemPrompt(personality, language)

        val toolsList = listOf(
            BidiToolDeclaration(
                functionDeclarations = listOf(
                    BidiFunctionSpec(
                        name = "make_call",
                        description = "Initiate a phone call to a contact or phone number on Android",
                        parameters = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("phoneNumber") {
                                    put("type", "STRING")
                                    put("description", "Phone number to dial")
                                }
                                putJsonObject("contactName") {
                                    put("type", "STRING")
                                    put("description", "Name of the contact if known")
                                }
                            }
                            putJsonArray("required") { add("phoneNumber") }
                        }
                    ),
                    BidiFunctionSpec(
                        name = "send_sms",
                        description = "Draft and prepare an SMS message on Android",
                        parameters = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("phoneNumber") { put("type", "STRING") }
                                putJsonObject("message") { put("type", "STRING") }
                            }
                            putJsonArray("required") { add("phoneNumber"); add("message") }
                        }
                    ),
                    BidiFunctionSpec(
                        name = "open_whatsapp",
                        description = "Open WhatsApp chat with contact or number with pre-filled message",
                        parameters = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("phoneNumber") { put("type", "STRING") }
                                putJsonObject("message") { put("type", "STRING") }
                            }
                            putJsonArray("required") { add("phoneNumber") }
                        }
                    ),
                    BidiFunctionSpec(
                        name = "open_app",
                        description = "Open an installed app like YouTube, Spotify, Camera, Maps, Settings",
                        parameters = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("appName") { put("type", "STRING") }
                            }
                            putJsonArray("required") { add("appName") }
                        }
                    ),
                    BidiFunctionSpec(
                        name = "build_website",
                        description = "Trigger the Live 3D Website Forge to build a full animated 3D website in real-time",
                        parameters = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("topic") {
                                    put("type", "STRING")
                                    put("description", "Description of website e.g. cafe, futuristic portfolio, space nebula")
                                }
                            }
                            putJsonArray("required") { add("topic") }
                        }
                    )
                )
            )
        )

        val setupMsg = BidiClientMessage(
            setup = BidiSetup(
                model = "models/gemini-2.0-flash-exp",
                generationConfig = BidiGenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = BidiSpeechConfig(
                        voiceConfig = BidiVoiceConfig(
                            prebuiltVoiceConfig = BidiPrebuiltVoice(
                                voiceName = personality.voiceName
                            )
                        )
                    )
                ),
                systemInstruction = BidiContent(
                    parts = listOf(BidiPart(text = compositePrompt))
                ),
                tools = toolsList
            )
        )

        val jsonStr = json.encodeToString(setupMsg)
        ws.send(jsonStr)
        _connectionState.value = LiveConnectionState.STREAMING
    }

    private fun buildCompositeSystemPrompt(personality: Personality, language: String): String {
        val langInstruction = when (language) {
            "hi" -> "Respond primarily in pure, natural Hindi (हिन्दी). Use clear, authentic expressions."
            "bn" -> "কথাবার্তা মিষ্টি ও সাবলীল বাংলা (Bangla) ভাষায় বলুন। কলকাতা ও বাঙালি আড্ডার মিষ্টতা বজায় রাখুন।"
            "hinglish" -> "Speak in casual, modern urban Hinglish (mix of Hindi & English in Latin script). Keep it relatable, energetic, and punchy."
            else -> "Respond in crisp, natural, conversational English."
        }

        return """
            ${personality.systemPrompt}

            [LANGUAGE CONTEXT]:
            $langInstruction
            You can effortlessly switch mid-sentence between Hindi, Hinglish, Bengali, and English whenever the user shifts languages.

            [PHONE & WEBPAGE TOOLS]:
            You are running inside a native Android system. Use function calls:
            - 'make_call': When asked to call someone.
            - 'send_sms': When asked to send or draft an SMS.
            - 'open_whatsapp': When asked to message someone on WhatsApp.
            - 'open_app': When asked to launch YouTube, Camera, Spotify, etc.
            - 'build_website': When the user says 'Alex, ek website banao' or requests to create/design any webpage.
            Always invoke the tools promptly without asking unnecessary questions.
        """.trimIndent()
    }

    /**
     * Send Real-Time Mic PCM Audio Chunk (16kHz, 16-bit Mono PCM)
     */
    fun sendAudioChunk(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (_connectionState.value != LiveConnectionState.STREAMING) return

        val base64Data = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val msg = BidiClientMessage(
            realtimeInput = BidiRealtimeInput(
                mediaChunks = listOf(
                    BidiBlob(
                        mimeType = "audio/pcm;rate=16000",
                        data = base64Data
                    )
                )
            )
        )
        ws.send(json.encodeToString(msg))
    }

    /**
     * Send Text Chat fallback
     */
    fun sendTextMessage(text: String) {
        val ws = webSocket ?: return
        val msg = BidiClientMessage(
            clientContent = BidiClientContent(
                turns = listOf(
                    BidiTurn(
                        role = "user",
                        parts = listOf(BidiPart(text = text))
                    )
                ),
                turnComplete = true
            )
        )
        ws.send(json.encodeToString(msg))
    }

    /**
     * Send Tool Execution Response
     */
    fun sendToolResponse(callId: String, output: JsonObject) {
        val ws = webSocket ?: return
        val msg = BidiClientMessage(
            toolResponse = BidiToolResponse(
                functionResponses = listOf(
                    BidiFunctionResponse(
                        id = callId,
                        response = output
                    )
                )
            )
        )
        ws.send(json.encodeToString(msg))
    }

    private fun handleServerMessage(text: String) {
        try {
            val serverMsg = json.decodeFromString<BidiServerMessage>(text)

            // 1. Check for User Interruption (Barge-In)
            if (serverMsg.serverContent?.interrupted == true) {
                Log.d(tag, "Barge-in signal received from server")
                listener.onInterrupted()
            }

            // 2. Check for Incoming Audio chunks & Transcripts
            serverMsg.serverContent?.modelTurn?.parts?.forEach { part ->
                part.inlineData?.let { blob ->
                    if (blob.data.isNotBlank()) {
                        val pcmBytes = Base64.decode(blob.data, Base64.DEFAULT)
                        listener.onAudioChunkReceived(pcmBytes)
                    }
                }
                part.text?.let { transcript ->
                    if (transcript.isNotBlank()) {
                        listener.onTranscriptReceived(transcript, true)
                    }
                }
            }

            // 3. Check for Tool Invocation
            serverMsg.toolCall?.functionCalls?.forEach { call ->
                Log.d(tag, "Tool call received: ${call.name} (id: ${call.id})")
                listener.onToolCall(call.id, call.name, call.args)
            }

            // 4. Check for Turn Complete
            if (serverMsg.serverContent?.turnComplete == true) {
                listener.onTurnComplete()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing server message", e)
        }
    }
}
