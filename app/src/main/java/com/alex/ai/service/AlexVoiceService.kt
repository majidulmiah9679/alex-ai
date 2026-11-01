package com.alex.ai.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alex.ai.MainActivity
import com.alex.ai.actions.PhoneActionExecutor
import com.alex.ai.audio.AudioEngine
import com.alex.ai.audio.AudioEngineListener
import com.alex.ai.data.SettingsRepository
import com.alex.ai.domain.PersonalitiesRegistry
import com.alex.ai.domain.Personality
import com.alex.ai.forge.ForgeController
import com.alex.ai.live.GeminiLiveClient
import com.alex.ai.live.GeminiLiveListener
import com.alex.ai.live.LiveConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

data class VoiceSessionState(
    val connectionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val personality: Personality = PersonalitiesRegistry.all.first(),
    val language: String = "hinglish",
    val micAmplitude: Float = 0f,
    val speakerAmplitude: Float = 0f,
    val lastUserText: String = "",
    val lastModelText: String = "",
    val activeWebsiteBuildTopic: String? = null
)

class AlexVoiceService : Service(), TextToSpeech.OnInitListener {

    private val tag = "AlexVoiceService"
    private val channelId = "alex_voice_channel"
    private val notificationId = 1001

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var phoneActionExecutor: PhoneActionExecutor
    private var audioEngine: AudioEngine? = null
    private var geminiClient: GeminiLiveClient? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val _sessionState = MutableStateFlow(VoiceSessionState())
    val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private var isSessionDesired = false
    private var isUsingFallbackEngine = false

    inner class LocalBinder : Binder() {
        fun getService(): AlexVoiceService = this@AlexVoiceService
    }

    companion object {
        const val ACTION_START_SESSION = "com.alex.ai.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.alex.ai.action.STOP_SESSION"
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION -> stopSession()
            ACTION_START_SESSION -> startSession()
            else -> startSession()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        phoneActionExecutor = PhoneActionExecutor(applicationContext)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlexAI::VoiceWakeLock")

        createNotificationChannel()

        // Init TextToSpeech for zero-quota voice synthesis
        try {
            textToSpeech = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e(tag, "TTS init error", e)
        }

        initAudioAndLive()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0.65f)
                }

                override fun onDone(utteranceId: String?) {
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                    // Auto-listen again for seamless two-way conversation
                    if (isSessionDesired && isUsingFallbackEngine) {
                        serviceScope.launch {
                            delay(400)
                            startListeningContinuous()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                    if (isSessionDesired && isUsingFallbackEngine) {
                        serviceScope.launch {
                            delay(500)
                            startListeningContinuous()
                        }
                    }
                }
            })
        }
    }

    private fun initAudioAndLive() {
        audioEngine = AudioEngine(object : AudioEngineListener {
            override fun onMicData(chunk: ByteArray) {
                if (!isUsingFallbackEngine) {
                    geminiClient?.sendAudioChunk(chunk)
                }
            }

            override fun onMicAmplitude(amplitude: Float) {
                if (!isUsingFallbackEngine) {
                    _sessionState.value = _sessionState.value.copy(micAmplitude = amplitude)
                }
            }

            override fun onSpeakerAmplitude(amplitude: Float) {
                _sessionState.value = _sessionState.value.copy(speakerAmplitude = amplitude)
            }
        })

        geminiClient = GeminiLiveClient(object : GeminiLiveListener {
            override fun onConnected() {
                isUsingFallbackEngine = false
                _sessionState.value = _sessionState.value.copy(connectionState = LiveConnectionState.STREAMING)
                audioEngine?.startRecording()
                updateNotification("Alex is live and listening...")
            }

            override fun onDisconnected(reason: String) {
                audioEngine?.stopRecording()
                Log.w(tag, "Live client disconnected ($reason). Switching to continuous fallback engine.")
                if (isSessionDesired) {
                    activateFallbackVoiceEngine()
                }
            }

            override fun onAudioChunkReceived(pcmData: ByteArray) {
                audioEngine?.enqueuePlaybackChunk(pcmData)
            }

            override fun onTranscriptReceived(text: String, isModel: Boolean) {
                if (isModel) {
                    _sessionState.value = _sessionState.value.copy(lastModelText = text)
                } else {
                    _sessionState.value = _sessionState.value.copy(lastUserText = text)
                }
            }

            override fun onInterrupted() {
                audioEngine?.stopPlayback()
                textToSpeech?.stop()
            }

            override fun onTurnComplete() {
                Log.d(tag, "Live turn complete")
            }

            override fun onToolCall(id: String, name: String, args: JsonObject?) {
                serviceScope.launch {
                    handleToolCall(id, name, args)
                }
            }

            override fun onError(error: Throwable) {
                Log.e(tag, "Gemini live error: ${error.message}. Activating fallback engine.")
                if (isSessionDesired) {
                    activateFallbackVoiceEngine()
                }
            }
        })
    }

    fun startSession() {
        isSessionDesired = true
        startForegroundServiceNotification()

        wakeLock?.acquire(12 * 60 * 60 * 1000L)

        serviceScope.launch {
            val apiKey = settingsRepository.getApiKey()
            val personalityId = settingsRepository.personalityFlow.first()
            val language = settingsRepository.languageFlow.first()
            val personality = PersonalitiesRegistry.getById(personalityId)

            _sessionState.value = _sessionState.value.copy(
                personality = personality,
                language = language,
                connectionState = LiveConnectionState.CONNECTING
            )

            // Connect to WebSocket; if quota 1011 or error occurs, onDisconnected/onError activates fallback
            geminiClient?.connect(apiKey, personality, language)
        }
    }

    fun stopSession() {
        isSessionDesired = false
        isUsingFallbackEngine = false
        geminiClient?.disconnect()
        audioEngine?.stopRecording()
        audioEngine?.stopPlayback()
        textToSpeech?.stop()
        stopSpeechRecognizer()

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        _sessionState.value = _sessionState.value.copy(
            connectionState = LiveConnectionState.DISCONNECTED,
            micAmplitude = 0f,
            speakerAmplitude = 0f
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Continuous Voice Fallback Engine:
     * When Gemini Live socket has quota 1011 or rate limits,
     * this engine keeps the 2-way real-time voice conversation alive:
     * SpeechRecognizer -> Multi-Model Brain (Gemini 3.5 / Groq) -> 24kHz Audio / TTS!
     */
    private fun activateFallbackVoiceEngine() {
        isUsingFallbackEngine = true
        _sessionState.value = _sessionState.value.copy(connectionState = LiveConnectionState.STREAMING)
        updateNotification("Alex is listening (Dual Engine Active)...")
        startListeningContinuous()
    }

    private fun startListeningContinuous() {
        if (!isSessionDesired) return

        serviceScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    if (!SpeechRecognizer.isRecognitionAvailable(this@AlexVoiceService)) {
                        Log.w(tag, "Speech recognition not available on device")
                        return@launch
                    }
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@AlexVoiceService)
                }

                val currentLang = _sessionState.value.language
                val localeTag = when (currentLang) {
                    "bn" -> "bn-BD"
                    "hi" -> "hi-IN"
                    "hinglish" -> "hi-IN"
                    else -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(tag, "SpeechRecognizer ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        _sessionState.value = _sessionState.value.copy(micAmplitude = 0.6f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val norm = (rmsdB / 10f).coerceIn(0.1f, 1f)
                        _sessionState.value = _sessionState.value.copy(micAmplitude = norm)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _sessionState.value = _sessionState.value.copy(micAmplitude = 0f)
                    }

                    override fun onError(error: Int) {
                        _sessionState.value = _sessionState.value.copy(micAmplitude = 0f)
                        if (isSessionDesired && isUsingFallbackEngine) {
                            serviceScope.launch {
                                delay(600)
                                startListeningContinuous()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _sessionState.value = _sessionState.value.copy(micAmplitude = 0f)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        if (spokenText.isNotBlank()) {
                            processSpokenVoiceInput(spokenText)
                        } else if (isSessionDesired && isUsingFallbackEngine) {
                            startListeningContinuous()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            _sessionState.value = _sessionState.value.copy(lastUserText = partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(tag, "Error starting SpeechRecognizer", e)
            }
        }
    }

    private fun stopSpeechRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping SpeechRecognizer", e)
        }
        speechRecognizer = null
    }

    private fun processSpokenVoiceInput(spokenText: String) {
        _sessionState.value = _sessionState.value.copy(lastUserText = spokenText)

        serviceScope.launch {
            val lower = spokenText.lowercase().trim()

            // 1. Direct Local Automation Routing
            var actionMsg: String? = null
            when {
                lower.contains("youtube") || lower.contains("ইউটিউব") -> {
                    val res = phoneActionExecutor.openApp("youtube")
                    actionMsg = if (res.success) "YouTube khulchhi!" else res.message
                }
                lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> {
                    val res = phoneActionExecutor.openWhatsApp()
                    actionMsg = if (res.success) "WhatsApp khulchhi!" else res.message
                }
                lower.contains("camera") || lower.contains("ক্যামেরা") -> {
                    val res = phoneActionExecutor.openApp("camera")
                    actionMsg = if (res.success) "Camera open korchhi!" else res.message
                }
                lower.contains("flashlight") || lower.contains("torch") || lower.contains("টর্চ") -> {
                    val enable = !lower.contains("off") && !lower.contains("বন্ধ")
                    val res = phoneActionExecutor.toggleFlashlight(enable)
                    actionMsg = res.message
                }
                lower.contains("settings") || lower.contains("সেটিংস") -> {
                    val res = phoneActionExecutor.openSettings("general")
                    actionMsg = "Settings khulchhi!"
                }
                lower.contains("call") || lower.contains("ফোন") -> {
                    val res = phoneActionExecutor.makeCall("")
                    actionMsg = "Dialer open korchhi!"
                }
                lower.contains("website") || lower.contains("forge") || lower.contains("ওয়েবসাইট") || lower.contains("cafe") -> {
                    ForgeController.instance.startWebsiteBuild(spokenText)
                    _sessionState.value = _sessionState.value.copy(activeWebsiteBuildTopic = spokenText)
                    actionMsg = "3D Website Forge reactor core ready korchhi!"
                }
            }

            // 2. Query Multi-Model Brain (Gemini 3.5 -> Flash -> Groq)
            val replyText = if (actionMsg != null) {
                actionMsg
            } else {
                val personality = _sessionState.value.personality
                val language = _sessionState.value.language
                settingsRepository.generateChatMessage(spokenText, personality.systemPrompt, language)
            }

            _sessionState.value = _sessionState.value.copy(lastModelText = replyText)

            // 3. Speak reply aloud
            speakResponse(replyText)
        }
    }

    private suspend fun speakResponse(text: String) {
        val personality = _sessionState.value.personality
        val lang = _sessionState.value.language

        // Attempt Gemini 24kHz Studio PCM TTS first
        val pcmAudio = settingsRepository.generateSpeechAudio(text, personality.voiceName)
        if (pcmAudio != null && pcmAudio.isNotEmpty()) {
            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0.7f)
            audioEngine?.enqueuePlaybackChunk(pcmAudio)
            delay((pcmAudio.size / (24000 * 2) * 1000L).coerceAtLeast(1000L))
            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
            if (isSessionDesired && isUsingFallbackEngine) {
                delay(300)
                startListeningContinuous()
            }
            return
        }

        // Seamless fallback to Android Native TextToSpeech
        withContext(Dispatchers.Main) {
            if (ttsReady && textToSpeech != null) {
                val loc = when (lang) {
                    "bn" -> Locale("bn", "BD")
                    "hi", "hinglish" -> Locale("hi", "IN")
                    else -> Locale.ENGLISH
                }
                textToSpeech?.language = loc
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alex_speech_id")
            } else if (isSessionDesired && isUsingFallbackEngine) {
                delay(1000)
                startListeningContinuous()
            }
        }
    }

    private suspend fun handleToolCall(id: String, name: String, args: JsonObject?) {
        if (name == "build_website") {
            val topic = args?.get("topic")?.toString()?.replace("\"", "") ?: "Modern Showcase"
            _sessionState.value = _sessionState.value.copy(activeWebsiteBuildTopic = topic)

            geminiClient?.sendToolResponse(id, buildJsonObject {
                put("status", "success")
                put("message", "3D Website Forge launched for '$topic'. Constructing reactor core and wireframes now.")
            })
            return
        }

        val result = phoneActionExecutor.execute(name, args)
        geminiClient?.sendToolResponse(id, buildJsonObject {
            put("status", if (result.success) "success" else "failure")
            put("message", result.message)
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alex AI Voice Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground microphone session for Alex AI full-duplex conversations"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = buildNotification("Connecting to Alex...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildNotification(text))
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alex AI Companion")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopSession()
        audioEngine?.release()
        audioEngine = null
        geminiClient = null
        textToSpeech?.shutdown()
        textToSpeech = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
