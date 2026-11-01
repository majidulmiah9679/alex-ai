package com.alex.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.ai.domain.Personality
import com.alex.ai.live.LiveConnectionState
import com.alex.ai.ui.components.AlexOrbView
import com.alex.ai.ui.components.GlowingScreenEdgeOverlay
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent

@Composable
fun HomeScreen(
    personality: Personality,
    language: String,
    onLanguageChange: (String) -> Unit,
    connectionState: LiveConnectionState,
    micAmplitude: Float,
    speakerAmplitude: Float,
    lastUserText: String,
    lastModelText: String,
    onToggleVoiceSession: () -> Unit,
    onOpenPersonalityPicker: () -> Unit
) {
    val isSessionActive = connectionState == LiveConnectionState.STREAMING ||
            connectionState == LiveConnectionState.CONNECTED
    val isConnecting = connectionState == LiveConnectionState.CONNECTING

    // Active amplitude for the 3D Orb (combines mic input or speaker output)
    val activeAmplitude = if (speakerAmplitude > 0.05f) speakerAmplitude else micAmplitude

    val languages = listOf("hinglish" to "⚡ Hinglish", "hi" to "🇮🇳 हिन्दी", "bn" to "🐅 বাংলা", "en" to "🌐 English")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoNight)
    ) {
        // Glowing Screen Edge Overlay when actively listening
        GlowingScreenEdgeOverlay(
            isListening = isSessionActive && speakerAmplitude < 0.1f,
            accentColor = Color(personality.accentColorHex)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD: Language Selector & Personality Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Personality Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131834),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(personality.primaryColorHex).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onOpenPersonalityPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = personality.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = personality.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Language Cycle Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131834),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MarigoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        val currentIndex = languages.indexOfFirst { it.first == language }
                        val nextIndex = (currentIndex + 1) % languages.size
                        onLanguageChange(languages[nextIndex].first)
                    }
                ) {
                    val label = languages.find { it.first == language }?.second ?: "⚡ Hinglish"
                    Text(
                        text = label,
                        color = MarigoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Center: 3D Orb with Per-Pixel GPU Shader & Device Tilt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = personality.tagline.uppercase(),
                    color = Color(personality.accentColorHex),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AlexOrbView(
                    modifier = Modifier
                        .size(280.dp)
                        .clickable { onToggleVoiceSession() },
                    personality = personality,
                    amplitude = activeAmplitude,
                    isListening = isSessionActive
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection status label
                Text(
                    text = when (connectionState) {
                        LiveConnectionState.STREAMING -> if (speakerAmplitude > 0.05f) "ALEX IS SPEAKING..." else "ALEX IS LISTENING • BARGE IN ANY TIME"
                        LiveConnectionState.CONNECTING -> "ESTABLISHING GEMINI LIVE LINK..."
                        LiveConnectionState.ERROR -> "CONNECTION FAILED • TAP TO RETRY"
                        else -> "TAP ORB TO TALK TO ALEX"
                    },
                    color = if (isSessionActive) Color.White else Color(0xFF8B94B2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Subtitle Card with 3D Depth Glassmorphism
            AnimatedVisibility(
                visible = lastModelText.isNotBlank() || lastUserText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD131834),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (lastUserText.isNotBlank()) {
                            Text(
                                text = "You: $lastUserText",
                                color = Color(0xFF8B94B2),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (lastModelText.isNotBlank()) {
                            Text(
                                text = "${personality.name}: $lastModelText",
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSessionActive -> Color(0xFF0B2E1C)
                        isConnecting -> Color(0xFF2E240B)
                        else -> Color(0xFF131834)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSessionActive -> Color(0xFF00E676)
                            isConnecting -> Color(0xFFFFB300)
                            else -> Color(0x334A90E2)
                        }
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSessionActive -> Color(0xFF00E676)
                                        isConnecting -> Color(0xFFFFB300)
                                        else -> Color(0xFF8B94B2)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isSessionActive -> if (speakerAmplitude > 0.05f) "🔴 LIVE: Alex bolche..." else "🔴 LIVE: Shunchhi... (Bolun)"
                                isConnecting -> "Gemini Brain-e connect hocche..."
                                else -> "Real-time Live Mode ready"
                            },
                            color = when {
                                isSessionActive -> Color(0xFF00E676)
                                isConnecting -> Color(0xFFFFB300)
                                else -> Color(0xFF8B94B2)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Primary Start/Stop Voice Action Pill
                Button(
                    onClick = onToggleVoiceSession,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSessionActive) Color(0xFFFF3D00) else if (isConnecting) Color(0xFFFFB300) else Color(0xFF00E676)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                isConnecting -> "⏳"
                                isSessionActive -> "⏹️"
                                else -> "🎙️"
                            },
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                isConnecting -> "CONNECTING TO GEMINI..."
                                isSessionActive -> "STOP LIVE MODE (কথা শেষ করুন)"
                                else -> "START LIVE MODE (লাইভ কথা বলুন)"
                            },
                            color = DeepIndigoNight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
