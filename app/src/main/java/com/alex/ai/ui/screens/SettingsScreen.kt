package com.alex.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent

@Composable
fun SettingsScreen(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    echoCancellerEnabled: Boolean,
    onToggleEchoCanceller: (Boolean) -> Unit,
    noiseSuppressorEnabled: Boolean,
    onToggleNoiseSuppressor: (Boolean) -> Unit,
    edgeGlowEnabled: Boolean,
    onToggleEdgeGlow: (Boolean) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var keySavedFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoNight)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = Color(0xFF131834),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "SETTINGS & CREDENTIALS",
                    color = MarigoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Manage Gemini Live API key, audio pipeline, and security.",
                    color = Color(0xFF8B94B2),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key Vault
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141936),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GEMINI LIVE API KEY",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Required for full-duplex real-time voice streaming.",
                        color = Color(0xFF8B94B2),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    TextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keySavedFeedback = false
                        },
                        placeholder = { Text("Paste AIzaSy... key", color = Color(0xFF6B7280)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1024),
                            unfocusedContainerColor = Color(0xFF0C1024),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onSaveApiKey(apiKeyInput.trim())
                            keySavedFeedback = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarigoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (keySavedFeedback) "✓ KEY SAVED" else "SAVE SECURELY",
                            color = DeepIndigoNight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Audio & Echo Guard (Part 05)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141936),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUDIO PIPELINE & ECHO GUARD",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acoustic Echo Canceler", color = Color.White, fontSize = 14.sp)
                            Text("Prevents speaker output feedback into mic", color = Color(0xFF8B94B2), fontSize = 11.sp)
                        }
                        Switch(
                            checked = echoCancellerEnabled,
                            onCheckedChange = onToggleEchoCanceller,
                            colors = SwitchDefaults.colors(checkedThumbColor = MarigoldAccent)
                        )
                    }

                    HorizontalDivider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Noise Suppressor", color = Color.White, fontSize = 14.sp)
                            Text("Filters ambient room noise for clear voice", color = Color(0xFF8B94B2), fontSize = 11.sp)
                        }
                        Switch(
                            checked = noiseSuppressorEnabled,
                            onCheckedChange = onToggleNoiseSuppressor,
                            colors = SwitchDefaults.colors(checkedThumbColor = MarigoldAccent)
                        )
                    }

                    HorizontalDivider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Listening Screen Edge Glow", color = Color.White, fontSize = 14.sp)
                            Text("Screen edges light up when Alex is listening", color = Color(0xFF8B94B2), fontSize = 11.sp)
                        }
                        Switch(
                            checked = edgeGlowEnabled,
                            onCheckedChange = onToggleEdgeGlow,
                            colors = SwitchDefaults.colors(checkedThumbColor = MarigoldAccent)
                        )
                    }
                }
            }

            // Architecture Details
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141936),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM TELEMETRY", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Gemini Live: gemini-2.0-flash-exp (WebSockets)", color = Color(0xFF8B94B2), fontSize = 12.sp)
                    Text("• Input Audio: 16,000 Hz Mono Linear PCM", color = Color(0xFF8B94B2), fontSize = 12.sp)
                    Text("• Output Audio: 24,000 Hz High-Fidelity AudioTrack", color = Color(0xFF8B94B2), fontSize = 12.sp)
                    Text("• 3D Orb: Hardware AGSL RuntimeShader + Tilt Sensors", color = Color(0xFF8B94B2), fontSize = 12.sp)
                    Text("• Website Forge: Live Three.js In-App 3D Reactor", color = Color(0xFF8B94B2), fontSize = 12.sp)
                }
            }
        }
    }
}
