package com.alex.ai.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.alex.ai.forge.ForgeController
import com.alex.ai.forge.Forge3DSceneGenerator
import com.alex.ai.ui.theme.DeepIndigoNight
import com.alex.ai.ui.theme.MarigoldAccent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ForgeScreen(
    initialTopic: String = "luxury perfume brand website"
) {
    val forgeController = ForgeController.instance
    val streamState by forgeController.streamState.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        if (!streamState.isBuilding && !streamState.isComplete) {
            forgeController.startWebsiteBuild(initialTopic)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D1C))
    ) {
        // 3D Build Scene Viewport
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    val html = Forge3DSceneGenerator.generateForgeSceneHtml(streamState.topic)
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Header Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Building your website",
                color = Color(0xFF8B94B2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = streamState.topic,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Glassmorphic Forge Telemetry Card (Exact Match to Image 3)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF0121735),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334A90E2))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Stage Title & Big Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = streamState.stageTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stage ${streamState.stageNumber} of ${streamState.totalStages}",
                            color = Color(0xFF8B94B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${streamState.progressPercent}%",
                        color = MarigoldAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 6 Segmented Progress Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..6) {
                        val isActive = i <= streamState.stageNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isActive) MarigoldAccent else Color(0xFF1E2850)
                                )
                        )
                    }
                }

                // 4 Telemetry Metric Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryItem(value = "${streamState.kbWritten}", label = "KB written")
                    TelemetryItem(value = "${streamState.sectionsCount}", label = "sections")
                    TelemetryItem(value = "${streamState.tokensPerSec}", label = "tokens/s")
                    TelemetryItem(value = "${streamState.secondsElapsed}s", label = "elapsed")
                }

                // Cancel Build Button
                Text(
                    text = if (streamState.isComplete) "✓ Build Complete (Tap to Explore)" else "Cancel build",
                    color = if (streamState.isComplete) MarigoldAccent else Color(0xFF8B94B2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!streamState.isComplete) {
                                forgeController.cancelBuild()
                            }
                        }
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TelemetryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFF8B94B2),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
