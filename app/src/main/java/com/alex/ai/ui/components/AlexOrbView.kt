package com.alex.ai.ui.components

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alex.ai.domain.Personality
import com.alex.ai.shader.AlexOrbShader
import kotlin.math.sin

@Composable
fun AlexOrbView(
    modifier: Modifier = Modifier,
    personality: Personality,
    amplitude: Float,
    isListening: Boolean
) {
    val context = LocalContext.current
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    // Register Tilt Sensor (Accelerometer)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 2) {
                    // Normalize tilt values between -1.0 and 1.0
                    tiltX = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    tiltY = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Continuous Time Animation
    val infiniteTransition = rememberInfiniteTransition(label = "OrbShaderTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    // Pulse Scale based on Amplitude & Listening state
    val basePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val scale = basePulse + (amplitude * 0.25f)

    val primaryColor = Color(personality.primaryColorHex)
    val accentColor = Color(personality.accentColorHex)

    Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = (minOf(canvasWidth, canvasHeight) / 2.2f) * scale
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // High-Performance Hardware AGSL Runtime Shader
                try {
                    val shader = AlexOrbShader.createShader()
                    shader.setFloatUniform("uResolution", canvasWidth, canvasHeight)
                    shader.setFloatUniform("uTime", time)
                    shader.setFloatUniform("uAmplitude", amplitude.coerceIn(0f, 1f))
                    shader.setFloatUniform("uTilt", tiltX, tiltY)
                    shader.setFloatUniform("uColorPrimary", primaryColor.red, primaryColor.green, primaryColor.blue)
                    shader.setFloatUniform("uColorAccent", accentColor.red, accentColor.green, accentColor.blue)

                    val paint = android.graphics.Paint().apply {
                        this.shader = shader
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
                    }
                } catch (e: Exception) {
                    // Fallback to Canvas Radial Gradient
                    drawFallbackOrb(center, radius, primaryColor, accentColor, tiltX, tiltY, amplitude, time)
                }
            } else {
                drawFallbackOrb(center, radius, primaryColor, accentColor, tiltX, tiltY, amplitude, time)
            }
        }
    }
}

/**
 * High-quality fallback rendering for Android versions below 13
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackOrb(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    accentColor: Color,
    tiltX: Float,
    tiltY: Float,
    amplitude: Float,
    time: Float
) {
    val lightOffset = Offset(
        center.x + (tiltX * radius * 0.45f),
        center.y - (tiltY * radius * 0.45f)
    )

    // Outer Glow Corona
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.5f + amplitude * 0.4f), Color.Transparent),
            center = center,
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = center
    )

    // Spherical Shading with Tilt-Driven Light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                accentColor,
                primaryColor,
                primaryColor.copy(alpha = 0.85f),
                Color(0xFF070B19)
            ),
            center = lightOffset,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Fresnel Rim Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.8f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * Part 10: Glowing Screen Edge Overlay
 * Pulses gently when Alex is actively listening for the user's voice.
 */
@Composable
fun GlowingScreenEdgeOverlay(
    isListening: Boolean,
    accentColor: Color
) {
    if (!isListening) return

    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EdgeAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = alpha),
                        accentColor.copy(alpha = alpha * 0.4f),
                        accentColor.copy(alpha = alpha)
                    )
                )
            )
    )
}
