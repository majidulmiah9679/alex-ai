package com.alex.ai.live

import android.content.Context
import android.util.Log
import com.alex.ai.actions.ActionExecutor
import com.alex.ai.actions.DirectActionExecutor
import com.alex.ai.actions.StoreActionExecutor
import com.alex.ai.audio.AudioEngine
import com.alex.ai.audio.AudioEngineListener
import com.alex.ai.data.SettingsRepository
import com.alex.ai.domain.PersonalitiesRegistry
import com.alex.ai.domain.Personality
import com.alex.ai.forge.ForgeController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class VoiceManagerState(
    val connectionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val personality: Personality = PersonalitiesRegistry.all.first(),
    val language: String = "hinglish",
    val micAmplitude: Float = 0f,
    val speakerAmplitude: Float = 0f,
    val lastUserText: String = "",
    val lastModelText: String = ""
)

/**
 * Architecture at a glance:
 * VoiceSessionManager is a singleton that outlives screens.
 * Coordinates:
 * - WebSocket <-> Gemini Live
 * - Tool calls <-> ActionExecutor (per flavor)
 * - build_website <-> ForgeController
 * - Feeds 3D orb, edge glow & Compose UI
 */
class VoiceSessionManager private constructor(private val appContext: Context) {

    private val tag = "VoiceSessionManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val settingsRepo = SettingsRepository(appContext)
    private val actionExecutor: ActionExecutor = DirectActionExecutor(appContext, StoreActionExecutor(appContext))

    private var audioEngine: AudioEngine? = null
    private var geminiClient: GeminiLiveClient? = null

    private val _state = MutableStateFlow(VoiceManagerState())
    val state: StateFlow<VoiceManagerState> = _state.asStateFlow()

    companion object {
        @Volatile
        private var instance: VoiceSessionManager? = null

        fun getInstance(context: Context): VoiceSessionManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        audioEngine = AudioEngine(object : AudioEngineListener {
            override fun onMicData(chunk: ByteArray) {
                geminiClient?.sendAudioChunk(chunk)
            }
            override fun onMicAmplitude(amplitude: Float) {
                _state.value = _state.value.copy(micAmplitude = amplitude)
            }
            override fun onSpeakerAmplitude(amplitude: Float) {
                _state.value = _state.value.copy(speakerAmplitude = amplitude)
            }
        })

        geminiClient = GeminiLiveClient(object : GeminiLiveListener {
            override fun onConnected() {
                _state.value = _state.value.copy(connectionState = LiveConnectionState.STREAMING)
                audioEngine?.startRecording()
            }

            override fun onDisconnected(reason: String) {
                audioEngine?.stopRecording()
                _state.value = _state.value.copy(connectionState = LiveConnectionState.DISCONNECTED)
            }

            override fun onAudioChunkReceived(pcmData: ByteArray) {
                audioEngine?.enqueuePlaybackChunk(pcmData)
            }

            override fun onTranscriptReceived(text: String, isModel: Boolean) {
                if (isModel) {
                    _state.value = _state.value.copy(lastModelText = text)
                } else {
                    _state.value = _state.value.copy(lastUserText = text)
                }
            }

            override fun onInterrupted() {
                audioEngine?.stopPlayback()
            }

            override fun onTurnComplete() {}

            override fun onToolCall(id: String, name: String, args: JsonObject?) {
                scope.launch {
                    if (name == "build_website") {
                        val topic = args?.get("topic")?.toString()?.replace("\"", "") ?: "Modern 3D Showcase"
                        ForgeController.instance.startWebsiteBuild(topic)

                        geminiClient?.sendToolResponse(id, buildJsonObject {
                            put("status", "success")
                            put("message", "3D Website Forge opened. Reactor core is charging for '$topic'.")
                        })
                        return@launch
                    }

                    // Delegate to ActionExecutor
                    val result = actionExecutor.execute(name, args)
                    geminiClient?.sendToolResponse(id, buildJsonObject {
                        put("status", if (result.success) "success" else "failure")
                        put("message", result.message)
                    })
                }
            }

            override fun onError(error: Throwable) {
                Log.e(tag, "Gemini client error", error)
                _state.value = _state.value.copy(connectionState = LiveConnectionState.ERROR)
            }
        })
    }

    fun startSession() {
        scope.launch {
            val apiKey = settingsRepo.getApiKey()
            val personalityId = settingsRepo.personalityFlow.first()
            val language = settingsRepo.languageFlow.first()
            val personality = PersonalitiesRegistry.getById(personalityId)

            _state.value = _state.value.copy(
                personality = personality,
                language = language,
                connectionState = LiveConnectionState.CONNECTING
            )

            geminiClient?.connect(apiKey, personality, language)
        }
    }

    fun stopSession() {
        geminiClient?.disconnect()
        audioEngine?.stopRecording()
        audioEngine?.stopPlayback()
        _state.value = _state.value.copy(
            connectionState = LiveConnectionState.DISCONNECTED,
            micAmplitude = 0f,
            speakerAmplitude = 0f
        )
    }

    fun sendTextMessage(text: String) {
        geminiClient?.sendTextMessage(text)
    }
}
