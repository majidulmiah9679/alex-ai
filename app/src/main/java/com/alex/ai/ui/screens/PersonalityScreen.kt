package com.alex.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.ai.domain.PersonalitiesRegistry
import com.alex.ai.domain.Personality
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent

@Composable
fun PersonalityScreen(
    currentPersonality: Personality,
    currentLanguage: String,
    onSelectPersonality: (Personality) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoNight)
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
                    text = "10 AI PERSONALITIES",
                    color = MarigoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Pick your companion's persona, vocal timbre, and cultural nuance.",
                    color = Color(0xFF8B94B2),
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PersonalitiesRegistry.all, key = { it.id }) { personality ->
                val isSelected = personality.id == currentPersonality.id
                val greeting = personality.sampleGreetings[currentLanguage]
                    ?: personality.sampleGreetings["hinglish"]
                    ?: personality.sampleGreetings.values.first()

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF1E2850) else Color(0xFF141936),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(personality.primaryColorHex) else Color(0x224A90E2)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPersonality(personality) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(personality.primaryColorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = personality.emoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = personality.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = personality.tagline,
                                    color = Color(personality.accentColorHex),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(personality.primaryColorHex)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = DeepIndigoNight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Greeting Preview Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0C1024),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Voice Sample (${currentLanguage.uppercase()}):",
                                    color = Color(0xFF8B94B2),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"$greeting\"",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
