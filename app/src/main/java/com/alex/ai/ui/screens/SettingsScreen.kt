package com.alex.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
    onValidateApiKey: suspend (String) -> Pair<Boolean, String> = { Pair(true, "Key active") },
    echoCancellerEnabled: Boolean,
    onToggleEchoCanceller: (Boolean) -> Unit,
    noiseSuppressorEnabled: Boolean,
    onToggleNoiseSuppressor: (Boolean) -> Unit,
    edgeGlowEnabled: Boolean,
    onToggleEdgeGlow: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var keySavedFeedback by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var verificationMsg by remember { mutableStateOf("") }

    // Auto-verify on first load if key is present
    LaunchedEffect(currentApiKey) {
        if (currentApiKey.isNotBlank()) {
            isVerifying = true
            val (ok, msg) = onValidateApiKey(currentApiKey)
            verificationSuccess = ok
            verificationMsg = msg
            isVerifying = false
        }
    }

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
                            verificationSuccess = null
                        },
                        placeholder = { Text("Paste Gemini API key", color = Color(0xFF6B7280)) },
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

                    // Green Tick / Red Cross Indicator Badge
                    if (isVerifying) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E284A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = MarigoldAccent,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Testing Gemini Brain connection...",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (verificationSuccess == true) {
                        // Green Tick Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B2E1C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFF00E676),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "API Key Supported & Active",
                                        color = Color(0xFF00E676),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = verificationMsg.ifEmpty { "Connected to Gemini Live model pipeline" },
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else if (verificationSuccess == false) {
                        // Red Cross Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E0B0E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3D00)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✗",
                                    color = Color(0xFFFF3D00),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "API Key Error / Not Supported",
                                        color = Color(0xFFFF3D00),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = verificationMsg.ifEmpty { "Check API key or network connection" },
                                        color = Color(0xFFFECACA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Test Button
                        OutlinedButton(
                            onClick = {
                                val keyToTest = apiKeyInput.trim().ifEmpty { currentApiKey }
                                scope.launch {
                                    isVerifying = true
                                    val (ok, msg) = onValidateApiKey(keyToTest)
                                    verificationSuccess = ok
                                    verificationMsg = msg
                                    isVerifying = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MarigoldAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "TEST KEY",
                                color = MarigoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Save Button
                        Button(
                            onClick = {
                                val cleanKey = apiKeyInput.trim()
                                onSaveApiKey(cleanKey)
                                keySavedFeedback = true
                                scope.launch {
                                    isVerifying = true
                                    val (ok, msg) = onValidateApiKey(cleanKey)
                                    verificationSuccess = ok
                                    verificationMsg = msg
                                    isVerifying = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MarigoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (keySavedFeedback) "✓ SAVED" else "SAVE SECURELY",
                                color = DeepIndigoNight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
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
