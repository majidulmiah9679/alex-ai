package com.alex.ai.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alex.ai.MainActivity
import com.alex.ai.actions.PhoneActionExecutor
import com.alex.ai.audio.AudioEngine
import com.alex.ai.audio.AudioEngineListener
import com.alex.ai.data.SettingsRepository
import com.alex.ai.domain.PersonalitiesRegistry
import com.alex.ai.domain.Personality
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

class AlexVoiceService : Service() {

    private val tag = "AlexVoiceService"
    private val channelId = "alex_voice_channel"
    private val notificationId = 1001

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var phoneActionExecutor: PhoneActionExecutor
    private var audioEngine: AudioEngine? = null
    private var geminiClient: GeminiLiveClient? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _sessionState = MutableStateFlow(VoiceSessionState())
    val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private var reconnectAttempts = 0
    private var isSessionDesired = false

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

        initAudioAndLive()
    }

    private fun initAudioAndLive() {
        audioEngine = AudioEngine(object : AudioEngineListener {
            override fun onMicData(chunk: ByteArray) {
                geminiClient?.sendAudioChunk(chunk)
            }

            override fun onMicAmplitude(amplitude: Float) {
                _sessionState.value = _sessionState.value.copy(micAmplitude = amplitude)
            }

            override fun onSpeakerAmplitude(amplitude: Float) {
                _sessionState.value = _sessionState.value.copy(speakerAmplitude = amplitude)
            }
        })

        geminiClient = GeminiLiveClient(object : GeminiLiveListener {
            override fun onConnected() {
                reconnectAttempts = 0
                _sessionState.value = _sessionState.value.copy(connectionState = LiveConnectionState.STREAMING)
                audioEngine?.startRecording()
                updateNotification("Alex is listening and live...")
            }

            override fun onDisconnected(reason: String) {
                audioEngine?.stopRecording()
                _sessionState.value = _sessionState.value.copy(connectionState = LiveConnectionState.DISCONNECTED)
                if (isSessionDesired) {
                    attemptReconnect()
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
                // User barge-in! Stop playback instantly
                audioEngine?.stopPlayback()
            }

            override fun onTurnComplete() {
                Log.d(tag, "Turn complete")
            }

            override fun onToolCall(id: String, name: String, args: JsonObject?) {
                serviceScope.launch {
                    handleToolCall(id, name, args)
                }
            }

            override fun onError(error: Throwable) {
                Log.e(tag, "Gemini client error", error)
                if (isSessionDesired) {
                    attemptReconnect()
                }
            }
        })
    }

    fun startSession() {
        isSessionDesired = true
        startForegroundServiceNotification()

        wakeLock?.acquire(12 * 60 * 60 * 1000L /* 12 hours max */)

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

            geminiClient?.connect(apiKey, personality, language)
        }
    }

    fun stopSession() {
        isSessionDesired = false
        geminiClient?.disconnect()
        audioEngine?.stopRecording()
        audioEngine?.stopPlayback()

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

    private fun attemptReconnect() {
        if (!isSessionDesired || reconnectAttempts >= 5) return
        reconnectAttempts++
        val backoffDelay = (1000L * (1 shl reconnectAttempts)).coerceAtMost(10000L)
        Log.d(tag, "Attempting reconnect #$reconnectAttempts in ${backoffDelay}ms")

        serviceScope.launch {
            delay(backoffDelay)
            if (isSessionDesired) {
                startSession()
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

        // Delegate to native PhoneActionExecutor
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
        serviceScope.cancel()
        super.onDestroy()
    }
}
