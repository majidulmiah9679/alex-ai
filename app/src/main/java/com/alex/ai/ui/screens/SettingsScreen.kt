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
    currentGroqApiKey: String = "",
    onSaveGroqApiKey: (String) -> Unit = {},
    onValidateGroqApiKey: suspend (String) -> Pair<Boolean, String> = { Pair(true, "Groq active") },
    echoCancellerEnabled: Boolean,
    onToggleEchoCanceller: (Boolean) -> Unit,
    noiseSuppressorEnabled: Boolean,
    onToggleNoiseSuppressor: (Boolean) -> Unit,
    edgeGlowEnabled: Boolean,
    onToggleEdgeGlow: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Gemini API State
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var keySavedFeedback by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var verificationMsg by remember { mutableStateOf("") }

    // Groq API State
    var groqKeyInput by remember { mutableStateOf(currentGroqApiKey) }
    var groqSavedFeedback by remember { mutableStateOf(false) }
    var isGroqVerifying by remember { mutableStateOf(false) }
    var groqVerificationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var groqVerificationMsg by remember { mutableStateOf("") }

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

    LaunchedEffect(currentGroqApiKey) {
        if (currentGroqApiKey.isNotBlank()) {
            isGroqVerifying = true
            val (ok, msg) = onValidateGroqApiKey(currentGroqApiKey)
            groqVerificationSuccess = ok
            groqVerificationMsg = msg
            isGroqVerifying = false
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
                    text = "Manage Gemini Live API key, Groq failover, and audio pipeline.",
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
            // Gemini API Key Vault
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141936),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GEMINI LIVE API KEY (PRIMARY)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Required for Gemini 3.5 Brain & 24kHz Live Voice.",
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
                            focusedContainerColor = Color(0xFF0D1127),
                            unfocusedContainerColor = Color(0xFF0D1127),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Gemini Verification Status Card
                    if (isVerifying) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MarigoldAccent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Testing Gemini key with Google API...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (verificationSuccess != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (verificationSuccess == true) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (verificationSuccess == true) Color(0xFF10B981) else Color(0xFFEF4444)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (verificationSuccess == true) "✓" else "✗",
                                    color = if (verificationSuccess == true) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (verificationSuccess == true) "KEY VERIFIED & ACTIVE" else "KEY ERROR / INVALID",
                                        color = if (verificationSuccess == true) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
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
                                text = "TEST GEMINI",
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

            // Groq Failover API Key Vault
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141936),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF55036).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GROQ FAILOVER API KEY",
                            color = Color(0xFFF55036),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x33F55036)
                        ) {
                            Text(
                                text = "LLAMA-3.3 70B",
                                color = Color(0xFFFFA07A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "When Gemini quota limits run out, Alex automatically switches to Groq so conversations never stop.",
                        color = Color(0xFF8B94B2),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    TextField(
                        value = groqKeyInput,
                        onValueChange = {
                            groqKeyInput = it
                            groqSavedFeedback = false
                            groqVerificationSuccess = null
                        },
                        placeholder = { Text("Paste Groq API key (gsk_...)", color = Color(0xFF6B7280)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D1127),
                            unfocusedContainerColor = Color(0xFF0D1127),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Groq Verification Status Card
                    if (isGroqVerifying) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFF55036)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Testing Groq API key...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (groqVerificationSuccess != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (groqVerificationSuccess == true) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (groqVerificationSuccess == true) Color(0xFF10B981) else Color(0xFFEF4444)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (groqVerificationSuccess == true) "✓" else "✗",
                                    color = if (groqVerificationSuccess == true) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (groqVerificationSuccess == true) "GROQ VERIFIED & ACTIVE" else "GROQ ERROR / INVALID",
                                        color = if (groqVerificationSuccess == true) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = groqVerificationMsg.ifEmpty { "Check Groq key or network" },
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
                        OutlinedButton(
                            onClick = {
                                val keyToTest = groqKeyInput.trim()
                                scope.launch {
                                    isGroqVerifying = true
                                    val (ok, msg) = onValidateGroqApiKey(keyToTest)
                                    groqVerificationSuccess = ok
                                    groqVerificationMsg = msg
                                    isGroqVerifying = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF55036)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "TEST GROQ",
                                color = Color(0xFFF55036),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val cleanKey = groqKeyInput.trim()
                                onSaveGroqApiKey(cleanKey)
                                groqSavedFeedback = true
                                scope.launch {
                                    isGroqVerifying = true
                                    val (ok, msg) = onValidateGroqApiKey(cleanKey)
                                    groqVerificationSuccess = ok
                                    groqVerificationMsg = msg
                                    isGroqVerifying = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF55036)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (groqSavedFeedback) "✓ SAVED" else "SAVE GROQ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Audio & Echo Guard
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
        }
    }
}
