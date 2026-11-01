package com.alex.ai.shader

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object AlexOrbShader {

    /**
     * AGSL Fragment Shader for Alex's 3D Orb:
     * - Analytic sphere normal computation
     * - Fractal noise interior plasma
     * - Phone tilt-driven specular lighting
     * - Fresnel edge glow
     * - Voice-reactive ripples
     */
    const val ORB_AGSL_SHADER = """
        uniform float2 uResolution;
        uniform float uTime;
        uniform float uAmplitude;
        uniform float2 uTilt;
        uniform float3 uColorPrimary;
        uniform float3 uColorAccent;

        // Simple 3D Simplex-like noise helper
        float hash(vec3 p) {
            p = fract(p * 0.3183099 + 0.1);
            p *= 17.0;
            return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
        }

        float noise(vec3 x) {
            vec3 i = floor(x);
            vec3 f = fract(x);
            f = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(mix(hash(i + vec3(0,0,0)), hash(i + vec3(1,0,0)), f.x),
                    mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
                mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                    mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
        }

        float fbm(vec3 p) {
            float v = 0.0;
            float a = 0.5;
            for (int i = 0; i < 4; ++i) {
                v += a * noise(p);
                p = p * 2.0 + vec3(100.0);
                a *= 0.5;
            }
            return v;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * uResolution) / (0.45 * min(uResolution.x, uResolution.y));
            float r2 = dot(uv, uv);

            // Analytic sphere intersection
            if (r2 > 1.0) {
                // Soft glowing outer corona
                float halo = exp(-6.0 * (sqrt(r2) - 1.0));
                return half4(uColorAccent * halo * (0.6 + uAmplitude * 1.5), halo * 0.4);
            }

            // Normal on sphere
            float z = sqrt(1.0 - r2);
            vec3 normal = vec3(uv, z);

            // Voice ripple distortion
            float ripple = sin(r2 * 15.0 - uTime * 6.0) * uAmplitude * 0.15;
            vec3 distortedNormal = normalize(normal + vec3(uv * ripple, 0.0));

            // Tilt-driven light vector from phone accelerometer/gyroscope
            vec3 lightDir = normalize(vec3(uTilt.x * 2.0, -uTilt.y * 2.0, 1.2));
            float diff = max(dot(distortedNormal, lightDir), 0.0);

            // Specular reflection
            vec3 viewDir = vec3(0.0, 0.0, 1.0);
            vec3 halfDir = normalize(lightDir + viewDir);
            float spec = pow(max(dot(distortedNormal, halfDir), 0.0), 32.0);

            // Fresnel rim
            float fresnel = pow(1.0 - normal.z, 2.8);

            // Fractal noise plasma interior
            vec3 plasmaCoord = normal * 2.0 + vec3(uTime * 0.3, uTime * 0.2, uTime * 0.4);
            float plasma = fbm(plasmaCoord + vec3(uAmplitude * 3.0));

            // Color composite
            vec3 baseColor = mix(uColorPrimary, uColorAccent, plasma);
            vec3 finalColor = baseColor * (diff * 0.7 + 0.3) + (uColorAccent * fresnel * 1.8) + (vec3(1.0) * spec * 0.8);

            return half4(finalColor, 0.95 + fresnel * 0.05);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader {
        return RuntimeShader(ORB_AGSL_SHADER)
    }
}
