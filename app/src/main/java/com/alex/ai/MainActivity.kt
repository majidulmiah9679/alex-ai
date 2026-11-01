package com.alex.ai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alex.ai.data.SettingsRepository
import com.alex.ai.domain.PersonalitiesRegistry
import com.alex.ai.domain.Personality
import com.alex.ai.live.LiveConnectionState
import com.alex.ai.service.AlexVoiceService
import com.alex.ai.service.VoiceSessionState
import com.alex.ai.ui.screens.*
import com.alex.ai.ui.theme.AlexTheme
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String, val icon: ImageVector) {
    HOME("Alex 3D", Icons.Default.GraphicEq),
    CHAT("Chat", Icons.Default.ChatBubbleOutline),
    FORGE("3D Forge", Icons.Default.AutoAwesome),
    PERSONAS("Personas", Icons.Default.Face),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private var voiceService: AlexVoiceService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AlexVoiceService.LocalBinder
            voiceService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNeededPermissions()

        val settingsRepo = SettingsRepository(applicationContext)

        // Bind to AlexVoiceService
        val serviceIntent = Intent(this, AlexVoiceService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            AlexTheme {
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(ScreenTab.HOME) }

                // Observe Settings
                val currentPersonalityId by settingsRepo.personalityFlow.collectAsState(initial = "friendly")
                val currentLanguage by settingsRepo.languageFlow.collectAsState(initial = "hinglish")
                val echoCancellerEnabled by settingsRepo.echoCancellerFlow.collectAsState(initial = true)
                val noiseSuppressorEnabled by settingsRepo.noiseSuppressorFlow.collectAsState(initial = true)
                val edgeGlowEnabled by settingsRepo.edgeGlowFlow.collectAsState(initial = true)
                var apiKey by remember { mutableStateOf(settingsRepo.getApiKey()) }

                val personality = remember(currentPersonalityId) {
                    PersonalitiesRegistry.getById(currentPersonalityId)
                }

                // Observe Voice Session State from Service
                val sessionState by (voiceService?.sessionState?.collectAsState()
                    ?: remember { mutableStateOf(VoiceSessionState()) })

                // In-memory Chat Messages
                val chatMessages = remember {
                    mutableStateListOf(
                        UiChatMessage(
                            text = personality.sampleGreetings[currentLanguage]
                                ?: personality.sampleGreetings["hinglish"]
                                ?: "Alex is online. Bolo kya plan hai?",
                            isUser = false
                        )
                    )
                }

                // If Forge was triggered by voice, auto-switch to Forge screen
                LaunchedEffect(sessionState.activeWebsiteBuildTopic) {
                    if (sessionState.activeWebsiteBuildTopic != null) {
                        currentTab = ScreenTab.FORGE
                    }
                }

                // Sync Model text to Chat messages
                LaunchedEffect(sessionState.lastModelText) {
                    if (sessionState.lastModelText.isNotBlank()) {
                        chatMessages.add(
                            UiChatMessage(text = sessionState.lastModelText, isUser = false)
                        )
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF10152D),
                            contentColor = MarigoldAccent
                        ) {
                            ScreenTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepIndigoNight,
                                        selectedTextColor = MarigoldAccent,
                                        indicatorColor = MarigoldAccent,
                                        unselectedIconColor = Color(0xFF8B94B2),
                                        unselectedTextColor = Color(0xFF8B94B2)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            ScreenTab.HOME -> {
                                HomeScreen(
                                    personality = personality,
                                    language = currentLanguage,
                                    onLanguageChange = { newLang ->
                                        scope.launch { settingsRepo.setLanguage(newLang) }
                                    },
                                    connectionState = sessionState.connectionState,
                                    micAmplitude = sessionState.micAmplitude,
                                    speakerAmplitude = sessionState.speakerAmplitude,
                                    lastUserText = sessionState.lastUserText,
                                    lastModelText = sessionState.lastModelText,
                                    onToggleVoiceSession = {
                                        // Check microphone permission first
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            requestNeededPermissions()
                                            return@HomeScreen
                                        }

                                        if (sessionState.connectionState == LiveConnectionState.STREAMING ||
                                            sessionState.connectionState == LiveConnectionState.CONNECTED
                                        ) {
                                            val stopIntent = Intent(this@MainActivity, AlexVoiceService::class.java).apply {
                                                action = AlexVoiceService.ACTION_STOP_SESSION
                                            }
                                            startService(stopIntent)
                                            voiceService?.stopSession()
                                        } else {
                                            val startIntent = Intent(this@MainActivity, AlexVoiceService::class.java).apply {
                                                action = AlexVoiceService.ACTION_START_SESSION
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                startForegroundService(startIntent)
                                            } else {
                                                startService(startIntent)
                                            }
                                            voiceService?.startSession()
                                        }
                                    },
                                    onOpenPersonalityPicker = {
                                        currentTab = ScreenTab.PERSONAS
                                    }
                                )
                            }

                            ScreenTab.CHAT -> {
                                ChatScreen(
                                    personality = personality,
                                    language = currentLanguage,
                                    onSendMessage = { userText ->
                                        chatMessages.add(UiChatMessage(text = userText, isUser = true))
                                        val lower = userText.lowercase()
                                        if (lower.contains("call") || lower.contains("phone")) {
                                            chatMessages.add(UiChatMessage(text = "Opening device dialer...", isUser = false, isActionBadge = true))
                                        } else if (lower.contains("forge") || lower.contains("website") || lower.contains("cafe") || lower.contains("animated")) {
                                            com.alex.ai.forge.ForgeController.instance.startWebsiteBuild(userText)
                                            currentTab = ScreenTab.FORGE
                                        } else {
                                            // Call Gemini Brain
                                            scope.launch {
                                                val reply = settingsRepo.generateChatMessage(
                                                    userText,
                                                    personality.systemPrompt,
                                                    currentLanguage
                                                )
                                                chatMessages.add(UiChatMessage(text = reply, isUser = false))
                                            }
                                        }
                                    },
                                    messages = chatMessages
                                )
                            }

                            ScreenTab.FORGE -> {
                                ForgeScreen(
                                    initialTopic = sessionState.activeWebsiteBuildTopic ?: "Artisan Italian Cafe"
                                )
                            }

                            ScreenTab.PERSONAS -> {
                                PersonalityScreen(
                                    currentPersonality = personality,
                                    currentLanguage = currentLanguage,
                                    onSelectPersonality = { newPersona ->
                                        scope.launch { settingsRepo.setPersonality(newPersona.id) }
                                    }
                                )
                            }

                            ScreenTab.SETTINGS -> {
                                SettingsScreen(
                                    currentApiKey = apiKey,
                                    onSaveApiKey = { newKey ->
                                        settingsRepo.saveApiKey(newKey)
                                        apiKey = newKey
                                    },
                                    onValidateApiKey = { key ->
                                        settingsRepo.validateApiKey(key)
                                    },
                                    echoCancellerEnabled = echoCancellerEnabled,
                                    onToggleEchoCanceller = { scope.launch { settingsRepo.setEchoCanceller(it) } },
                                    noiseSuppressorEnabled = noiseSuppressorEnabled,
                                    onToggleNoiseSuppressor = { scope.launch { settingsRepo.setNoiseSuppressor(it) } },
                                    edgeGlowEnabled = edgeGlowEnabled,
                                    onToggleEdgeGlow = { scope.launch { settingsRepo.setEdgeGlow(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
