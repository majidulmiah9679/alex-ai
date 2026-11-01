package com.alex.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.ai.domain.JarvisProtocol
import com.alex.ai.domain.JarvisProtocolRegistry
import com.alex.ai.domain.JarvisVoice
import com.alex.ai.domain.JarvisVoiceRegistry
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent

@Composable
fun AutomationScreen(
    currentVoiceId: String,
    onSelectVoice: (String) -> Unit,
    onTestVoiceSample: (JarvisVoice) -> Unit,
    voicePitch: Float,
    onPitchChange: (Float) -> Unit,
    voiceSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onExecuteProtocol: (JarvisProtocol) -> String,
    batteryStatus: String,
    ramStatus: String,
    storageStatus: String,
    deviceSpecs: String,
    onOpenAccessibilitySettings: () -> Unit = {},
    onOpenRestrictedSettings: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var voiceGenderTab by remember { mutableStateOf("Female") }
    var executionFeedback by remember { mutableStateOf<String?>(null) }

    val filteredProtocols = remember(selectedCategory) {
        if (selectedCategory == "All") {
            JarvisProtocolRegistry.allProtocols
        } else {
            JarvisProtocolRegistry.allProtocols.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoNight)
    ) {
        // Top Header
        Surface(
            color = Color(0xFF131834),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JARVIS AUTOMATION HUB",
                                color = MarigoldAccent,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Text(
                                    text = "280+ ONLINE",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real JARVIS device automations & 10 human voices",
                            color = Color(0xFF8B94B2),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 0. Accessibility & Android 13/14 Restricted Settings Bypass HUD
            item {
                val isRunning = com.alex.ai.accessibility.AlexAccessibilityService.isRunning()
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isRunning) Color(0xFF064E3B) else Color(0xFF1E2856),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRunning) Color(0xFF10B981) else MarigoldAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isRunning) "✓ ACCESSIBILITY SERVICE ACTIVE" else "⚡ ACCESSIBILITY & BYPASS AUTOMATION",
                                color = if (isRunning) Color(0xFF34D399) else MarigoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isRunning) {
                            Text(
                                text = "ফুল অ্যাক্সেসিবিলিটি রানিং আছে! স্ক্রিন ক্লিক, স্ক্রোল, ব্যাক, হোম এবং সব অ্যাপ অটোমেশন রেডি।",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "Android 13/14+ এ সেটিংস অন না হলে বা ধূসর (greyed out) থাকলে নিচের ধাপ অনুসরণ করুন:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "১. 'রিস্ট্রিক্টেড সেটিংস বাইপাস' চাপুন -> উপরে ৩ ডট (⋮) এ গিয়ে 'Allow restricted settings' সিলেক্ট করে ফিঙ্গারপ্রিন্ট দিন।\n২. 'অ্যাক্সেসিবিলিটি অন করুন' চাপুন এবং Alex AI অন করুন।\n(নোট: অ্যাক্সেসিবিলিটি ছাড়াও সব অ্যাপ ওপেন, ভলিউম, টর্চ, হোম স্ক্রিন অটো বাইপাস কাজ করবে)",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenRestrictedSettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = MarigoldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "১. বাইপাস সেটিংস",
                                        color = DeepIndigoNight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenAccessibilitySettings,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "২. অ্যাক্সেসিবিলিটি",
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Live Telemetry HUD
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141936),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MarigoldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYSTEM TELEMETRY & DIAGNOSTICS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D1127),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("BATTERY", color = Color(0xFF8B94B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(batteryStatus.ifEmpty { "100% (Ready)" }, color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D1127),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("RAM MEMORY", color = Color(0xFF8B94B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(ramStatus.ifEmpty { "Available" }, color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0D1127),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("DEVICE & OS MATRIX", color = Color(0xFF8B94B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(deviceSpecs.ifEmpty { "Android OS Telemetry Connected" }, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. Feedback Card
            if (executionFeedback != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF064E3B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = executionFeedback ?: "",
                                color = Color(0xFF6EE7B7),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. 10 Human JARVIS Voices Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141936),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MarigoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "HUMAN-LIKE JARVIS VOICES",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "5 Real Female & 5 Real Male Voice Models",
                                    color = Color(0xFF8B94B2),
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MarigoldAccent.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MarigoldAccent)
                            ) {
                                Text(
                                    text = "10 VOICES",
                                    color = MarigoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gender Switch Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { voiceGenderTab = "Female" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (voiceGenderTab == "Female") MarigoldAccent else Color(0xFF0D1127)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "👩 5 Female Voices",
                                    color = if (voiceGenderTab == "Female") DeepIndigoNight else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { voiceGenderTab = "Male" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (voiceGenderTab == "Male") MarigoldAccent else Color(0xFF0D1127)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "👨 5 Male Voices",
                                    color = if (voiceGenderTab == "Male") DeepIndigoNight else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Cards List
                        val voicesToShow = if (voiceGenderTab == "Female") JarvisVoiceRegistry.femaleVoices else JarvisVoiceRegistry.maleVoices

                        voicesToShow.forEach { voice ->
                            val isSelected = voice.id == currentVoiceId

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF1E2856) else Color(0xFF0D1127),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MarigoldAccent else Color(0x224A90E2)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectVoice(voice.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = voice.avatarEmoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = voice.name,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "✓ ACTIVE",
                                                    color = Color(0xFF00E676),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = voice.description,
                                            color = Color(0xFF8B94B2),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { onTestVoiceSample(voice) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MarigoldAccent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MarigoldAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sample", color = MarigoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pitch & Speed Sliders
                        Text("VOICE PITCH: ${String.format("%.2f", voicePitch)}x", color = Color(0xFF8B94B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = voicePitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.7f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = MarigoldAccent, activeTrackColor = MarigoldAccent)
                        )

                        Text("VOICE SPEED: ${String.format("%.2f", voiceSpeed)}x", color = Color(0xFF8B94B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = voiceSpeed,
                            onValueChange = onSpeedChange,
                            valueRange = 0.8f..1.3f,
                            colors = SliderDefaults.colors(thumbColor = MarigoldAccent, activeTrackColor = MarigoldAccent)
                        )
                    }
                }
            }

            // 4. 280+ Automation Protocols Hub
            item {
                Column {
                    Text(
                        text = "280+ AUTOMATION PROTOCOLS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Tap RUN to test immediately, or speak the voice command in Bengali/English.",
                        color = Color(0xFF8B94B2),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Horizontal Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JarvisProtocolRegistry.categories.forEach { cat ->
                            val isSel = cat == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) MarigoldAccent else Color(0xFF141936),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) MarigoldAccent else Color(0x334A90E2)),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) DeepIndigoNight else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Protocols List Items
            items(filteredProtocols, key = { it.id }) { proto ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141936),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x224A90E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = proto.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x224A90E2)
                                ) {
                                    Text(
                                        text = proto.category,
                                        color = Color(0xFF60A5FA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🗣️ \"${proto.voiceCommandExampleBn}\" • \"${proto.voiceCommandExampleEn}\"",
                                color = MarigoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = proto.description,
                                color = Color(0xFF8B94B2),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val res = onExecuteProtocol(proto)
                                executionFeedback = res
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2856)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "RUN",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
