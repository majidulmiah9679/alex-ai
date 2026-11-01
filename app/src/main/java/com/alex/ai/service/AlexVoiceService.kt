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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
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
            textToSpeech = TextToSpeech(applicationContext, this)
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
                    audioEngine?.setSpeakerPlaying(true)
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0.75f)
                }

                override fun onDone(utteranceId: String?) {
                    audioEngine?.setSpeakerPlaying(false)
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                    // Auto-listen again for seamless two-way conversation
                    if (isSessionDesired && isUsingFallbackEngine) {
                        serviceScope.launch {
                            delay(350)
                            startListeningContinuous()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    audioEngine?.setSpeakerPlaying(false)
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
            // Anti-Self-Talk Barrier: Never start listening if speaker is active!
            if (audioEngine?.isSpeakerPlaying() == true || mediaPlayer?.isPlaying == true || textToSpeech?.isSpeaking == true) {
                Log.d(tag, "Alex speaker active: speech listener paused")
                return@launch
            }

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
                        Log.d(tag, "SpeechRecognizer ready for user speech")
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
                        // Anti-Self-Talk: Do NOT restart listening if Alex is speaking!
                        if (isSessionDesired && isUsingFallbackEngine && audioEngine?.isSpeakerPlaying() == false) {
                            serviceScope.launch {
                                delay(800)
                                if (audioEngine?.isSpeakerPlaying() == false) {
                                    startListeningContinuous()
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _sessionState.value = _sessionState.value.copy(micAmplitude = 0f)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: ""

                        if (spokenText.isNotBlank()) {
                            // Self-Echo Discard Gate:
                            val lastModel = _sessionState.value.lastModelText.lowercase().trim()
                            val heard = spokenText.lowercase().trim()
                            if (lastModel.isNotBlank() && (lastModel.contains(heard) || heard.contains(lastModel))) {
                                Log.w(tag, "Self-speech echo detected and discarded: '$spokenText'")
                                serviceScope.launch {
                                    delay(800)
                                    if (audioEngine?.isSpeakerPlaying() == false) {
                                        startListeningContinuous()
                                    }
                                }
                                return
                            }

                            // Legitimate user speech: stop speech recognizer immediately
                            stopSpeechRecognizer()
                            processSpokenVoiceInput(spokenText)
                        } else if (isSessionDesired && isUsingFallbackEngine && audioEngine?.isSpeakerPlaying() == false) {
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

            // 1. Direct Local & In-App Automation Routing
            var actionMsg: String? = null
            when {
                // In-App Navigation: Back
                lower == "back" || lower.contains("back jao") || lower.contains("পিছনে যাও") || lower.contains("ব্যাকে যাও") || lower == "go back" -> {
                    val res = phoneActionExecutor.goBack()
                    actionMsg = res.message
                }
                // In-App Navigation: Home
                lower == "home" || lower.contains("home jao") || lower.contains("হোম") || lower.contains("হোমে যাও") || lower == "go home" -> {
                    val res = phoneActionExecutor.goHome()
                    actionMsg = res.message
                }
                // In-App Navigation: Recent Apps
                lower.contains("recent") || lower.contains("রিসেন্ট") -> {
                    val res = phoneActionExecutor.openRecentApps()
                    actionMsg = res.message
                }
                // In-App Navigation: Notification Bar
                lower.contains("notification") || lower.contains("নোটিফিকেশন") -> {
                    val res = phoneActionExecutor.openNotifications()
                    actionMsg = res.message
                }
                // In-App Gestures: Scroll Down
                lower.contains("scroll down") || lower.contains("niche scroll") || lower.contains("নিচে স্ক্রোল") || lower.contains("নিচে যাও") -> {
                    val res = phoneActionExecutor.scrollScreen("down")
                    actionMsg = res.message
                }
                // In-App Gestures: Scroll Up
                lower.contains("scroll up") || lower.contains("upore scroll") || lower.contains("উপরে স্ক্রোল") || lower.contains("উপরে যাও") -> {
                    val res = phoneActionExecutor.scrollScreen("up")
                    actionMsg = res.message
                }
                // In-App Gestures: Swipe Left / Right
                lower.contains("swipe left") || lower.contains("বামে সোয়াইপ") -> {
                    val res = phoneActionExecutor.scrollScreen("left")
                    actionMsg = res.message
                }
                lower.contains("swipe right") || lower.contains("ডানে সোয়াইপ") -> {
                    val res = phoneActionExecutor.scrollScreen("right")
                    actionMsg = res.message
                }
                // In-App Action: Click Element by Text / ID
                lower.startsWith("click ") || lower.contains("click karo") || lower.contains("ক্লিক করো") || lower.contains("ট্যাপ করো") -> {
                    val target = lower.replace("click karo", "")
                        .replace("ক্লিক করো", "")
                        .replace("ট্যাপ করো", "")
                        .replace("click", "")
                        .trim()
                    val res = phoneActionExecutor.clickOnScreen(target)
                    actionMsg = res.message
                }
                // Hardware: Flashlight
                lower.contains("flashlight") || lower.contains("torch") || lower.contains("টর্চ") -> {
                    val enable = !lower.contains("off") && !lower.contains("বন্ধ")
                    val res = phoneActionExecutor.toggleFlashlight(enable)
                    actionMsg = res.message
                }
                // Hardware: Volume Controls
                lower.contains("volume") || lower.contains("ভলিউম") || lower.contains("awaaz") -> {
                    val dir = if (lower.contains("down") || lower.contains("কম")) "down" else if (lower.contains("mute")) "mute" else if (lower.contains("full") || lower.contains("max")) "max" else "up"
                    val res = phoneActionExecutor.controlVolume(dir)
                    actionMsg = res.message
                }
                // Hardware: Settings toggles
                lower.contains("wifi") || lower.contains("ওয়াইফাই") -> {
                    val res = phoneActionExecutor.openSettings("wifi")
                    actionMsg = res.message
                }
                lower.contains("bluetooth") || lower.contains("ব্লুটুথ") -> {
                    val res = phoneActionExecutor.openSettings("bluetooth")
                    actionMsg = res.message
                }
                lower.contains("battery") || lower.contains("ব্যাটারি") || lower.contains("charge") -> {
                    val res = phoneActionExecutor.getBatteryStatus()
                    actionMsg = res.message
                }
                lower.contains("ram") || lower.contains("র‍্যাম") || lower.contains("memory") -> {
                    val res = phoneActionExecutor.getRamStatus()
                    actionMsg = res.message
                }
                lower.contains("storage") || lower.contains("স্টোরেজ") || lower.contains("space") -> {
                    val res = phoneActionExecutor.getStorageStatus()
                    actionMsg = res.message
                }
                lower.contains("device info") || lower.contains("specs") || lower.contains("phone details") -> {
                    val res = phoneActionExecutor.getDeviceSpecs()
                    actionMsg = res.message
                }
                lower.contains("alarm") || lower.contains("অ্যালার্ম") -> {
                    val res = phoneActionExecutor.setAlarm(7, 0, "Alex JARVIS Alarm")
                    actionMsg = res.message
                }
                lower.contains("timer") || lower.contains("টাইমার") -> {
                    val res = phoneActionExecutor.setTimer(300, "Alex JARVIS Timer")
                    actionMsg = res.message
                }
                lower.contains("navigate") || lower.contains("hospital") || lower.contains("restaurant") || lower.contains("ম্যাপ") -> {
                    val res = phoneActionExecutor.navigateMaps(spokenText)
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
                lower.contains("accessibility") || lower.contains("অ্যাক্সেসিবিলিটি") -> {
                    val res = phoneActionExecutor.openAccessibilitySettings()
                    actionMsg = res.message
                }
                lower.contains("restricted") || lower.contains("রিস্ট্রিক্টেড") -> {
                    val res = phoneActionExecutor.openAppDetailsSettings()
                    actionMsg = res.message
                }
                lower.contains("website") || lower.contains("forge") || lower.contains("ওয়েবসাইট") || lower.contains("cafe") -> {
                    ForgeController.instance.startWebsiteBuild(spokenText)
                    _sessionState.value = _sessionState.value.copy(activeWebsiteBuildTopic = spokenText)
                    actionMsg = "3D Website Forge reactor core ready korchhi!"
                }
                // Universal Dynamic App Launch (Opens ANY App)
                lower.contains("kholo") || lower.contains("open") || lower.contains("খুলবে") || lower.contains("খোল") || lower.contains("খোলো") || lower.contains("chalao") -> {
                    val cleanApp = phoneActionExecutor.cleanAppQuery(lower)
                    val res = phoneActionExecutor.openApp(cleanApp)
                    actionMsg = res.message
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

            // 3. Speak reply aloud using selected JARVIS voice
            speakResponse(replyText)
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    private suspend fun playNeuralVoiceStream(text: String, lang: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val langCode = when (lang) {
                "bn" -> "bn"
                "hi", "hinglish" -> "hi"
                else -> "en"
            }
            val encodedText = java.net.URLEncoder.encode(text.take(200), "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=$langCode&client=tw-ob&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false
            val audioBytes = response.body?.bytes() ?: return@withContext false
            if (audioBytes.size < 100) return@withContext false

            val tempFile = java.io.File(cacheDir, "alex_speech_${System.currentTimeMillis()}.mp3")
            tempFile.writeBytes(audioBytes)

            withContext(Dispatchers.Main) {
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.isSpeakerphoneOn = true

                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        audioEngine?.setSpeakerPlaying(true)
                        _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0.75f)

                        setOnCompletionListener {
                            audioEngine?.setSpeakerPlaying(false)
                            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                            try { tempFile.delete() } catch (_: Exception) {}
                            if (isSessionDesired && isUsingFallbackEngine) {
                                serviceScope.launch {
                                    delay(800)
                                    if (audioEngine?.isSpeakerPlaying() == false) {
                                        startListeningContinuous()
                                    }
                                }
                            }
                        }

                        setOnErrorListener { _, _, _ ->
                            audioEngine?.setSpeakerPlaying(false)
                            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                            try { tempFile.delete() } catch (_: Exception) {}
                            false
                        }

                        start()
                    }
                } catch (e: Exception) {
                    audioEngine?.setSpeakerPlaying(false)
                    _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
                    try { tempFile.delete() } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            Log.w(tag, "Neural TTS failed: ${e.message}")
            false
        }
    }

    private suspend fun speakResponse(text: String) {
        val selectedVoiceId = settingsRepository.voiceIdFlow.first()
        val voice = com.alex.ai.domain.JarvisVoiceRegistry.getById(selectedVoiceId)
        val voicePitch = settingsRepository.voicePitchFlow.first()
        val voiceSpeed = settingsRepository.voiceSpeedFlow.first()
        val lang = _sessionState.value.language

        // 1. Attempt Gemini 24kHz Studio PCM TTS first (if quota available)
        val pcmAudio = settingsRepository.generateSpeechAudio(text, voice.geminiVoice)
        if (pcmAudio != null && pcmAudio.isNotEmpty()) {
            audioEngine?.setSpeakerPlaying(true)
            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0.7f)
            audioEngine?.enqueuePlaybackChunk(pcmAudio)
            delay((pcmAudio.size / (24000 * 2) * 1000L).coerceAtLeast(1000L))
            audioEngine?.setSpeakerPlaying(false)
            _sessionState.value = _sessionState.value.copy(speakerAmplitude = 0f)
            if (isSessionDesired && isUsingFallbackEngine) {
                delay(800)
                if (audioEngine?.isSpeakerPlaying() == false) {
                    startListeningContinuous()
                }
            }
            return
        }

        // 2. High-Definition Online Voice Stream (Bangla, Hindi, English full loud audio)
        val playedOnline = playNeuralVoiceStream(text, lang)
        if (playedOnline) return

        // 3. Android Native TextToSpeech with smart language fallback
        withContext(Dispatchers.Main) {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true

                if (ttsReady && textToSpeech != null) {
                    val targetLocale = when (lang) {
                        "bn" -> Locale("bn", "BD")
                        "hi", "hinglish" -> Locale("hi", "IN")
                        else -> Locale.ENGLISH
                    }
                    val avail = textToSpeech?.isLanguageAvailable(targetLocale)
                    if (avail == TextToSpeech.LANG_AVAILABLE || avail == TextToSpeech.LANG_COUNTRY_AVAILABLE || avail == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                        textToSpeech?.language = targetLocale
                    } else {
                        val hiAvail = textToSpeech?.isLanguageAvailable(Locale("hi", "IN"))
                        if (hiAvail != null && hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                            textToSpeech?.language = Locale("hi", "IN")
                        } else {
                            textToSpeech?.language = Locale.US
                        }
                    }

                    textToSpeech?.setPitch(voicePitch * voice.defaultPitch)
                    textToSpeech?.setSpeechRate(voiceSpeed * voice.defaultSpeed)

                    audioEngine?.setSpeakerPlaying(true)
                    val params = Bundle().apply {
                        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "alex_speech_id")
                } else if (isSessionDesired && isUsingFallbackEngine) {
                    delay(1000)
                    startListeningContinuous()
                }
            } catch (e: Exception) {
                Log.e(tag, "Native TTS playback error", e)
                audioEngine?.setSpeakerPlaying(false)
                if (isSessionDesired && isUsingFallbackEngine) {
                    delay(1000)
                    startListeningContinuous()
                }
            }
            Unit
        }
    }

    fun speakVoiceSample(text: String, voiceName: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        serviceScope.launch {
            val pcmAudio = settingsRepository.generateSpeechAudio(text, voiceName)
            if (pcmAudio != null && pcmAudio.isNotEmpty()) {
                audioEngine?.setSpeakerPlaying(true)
                audioEngine?.enqueuePlaybackChunk(pcmAudio)
                return@launch
            }

            val lang = _sessionState.value.language
            val playedOnline = playNeuralVoiceStream(text, lang)
            if (playedOnline) return@launch

            withContext(Dispatchers.Main) {
                if (ttsReady && textToSpeech != null) {
                    audioEngine?.setSpeakerPlaying(true)
                    textToSpeech?.setPitch(pitch)
                    textToSpeech?.setSpeechRate(speed)
                    val params = Bundle().apply {
                        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sample_speech_id")
                }
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
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
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
