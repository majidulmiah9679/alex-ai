package com.alex.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alex_settings")

class SettingsRepository(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "alex_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private val KEY_PERSONALITY = stringPreferencesKey("personality_id")
        private val KEY_LANGUAGE = stringPreferencesKey("target_language")
        private val KEY_ECHO_CANCELLER = booleanPreferencesKey("enable_echo_canceller")
        private val KEY_NOISE_SUPPRESSOR = booleanPreferencesKey("enable_noise_suppressor")
        private val KEY_ORB_STYLE = stringPreferencesKey("orb_style")
        private val KEY_EDGE_GLOW = booleanPreferencesKey("enable_edge_glow")
        private val KEY_HAPTICS = booleanPreferencesKey("enable_haptics")
        private const val SECURE_API_KEY = "gemini_live_api_key"
        private const val SECURE_GROQ_KEY = "groq_api_key"
        private const val PRESET_KEY_B64 = "QVEuQWI4Uk42SVU1Um90aVNzQVBoYnZuaF96NzFzM2ZVZ2JPb2lCTE1YU1BKUUR5cFJDbFE="
        private val KEY_SELECTED_VOICE = stringPreferencesKey("selected_voice_id")
        private val KEY_VOICE_PITCH = androidx.datastore.preferences.core.floatPreferencesKey("selected_voice_pitch")
        private val KEY_VOICE_SPEED = androidx.datastore.preferences.core.floatPreferencesKey("selected_voice_speed")
    }

    // --- Secure Gemini API Key ---
    fun getApiKey(): String {
        val key = encryptedPrefs.getString(SECURE_API_KEY, "") ?: ""
        if (key.isNotBlank()) return key
        return try {
            String(android.util.Base64.decode(PRESET_KEY_B64, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(SECURE_API_KEY, apiKey.trim()).apply()
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    // --- Groq Failover API Key ---
    fun getGroqApiKey(): String {
        return encryptedPrefs.getString(SECURE_GROQ_KEY, "") ?: ""
    }

    fun saveGroqApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(SECURE_GROQ_KEY, apiKey.trim()).apply()
    }

    suspend fun validateApiKey(apiKey: String): Pair<Boolean, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val testKey = apiKey.trim().ifEmpty { getApiKey() }
        if (testKey.isBlank()) return@withContext Pair(false, "API Key is empty")
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$testKey"
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Gemini Key Verified & Active ✓")
                } else {
                    Pair(false, "Gemini API Error: ${response.code} (Invalid Key)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.message}")
        }
    }

    suspend fun validateGroqApiKey(apiKey: String): Pair<Boolean, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val testKey = apiKey.trim()
        if (testKey.isBlank()) return@withContext Pair(false, "Groq Key is empty")
        try {
            val url = "https://api.groq.com/openai/v1/models"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $testKey")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Groq Failover Key Verified & Active ✓")
                } else {
                    Pair(false, "Groq Error: ${response.code} (Invalid Key)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.message}")
        }
    }

    /**
     * Multi-Model Failover Brain:
     * 1. Primary: Gemini 3.5 Flash
     * 2. Secondary: Gemini 2.5 Flash / Flash Latest
     * 3. Tertiary: Groq Cloud (llama-3.3-70b-versatile)
     */
    suspend fun generateChatMessage(
        userText: String,
        personalityPrompt: String,
        language: String
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val geminiKey = getApiKey()
        val groqKey = getGroqApiKey()

        val systemMsg = """
            $personalityPrompt
            [LANGUAGE CONTEXT]: Respond naturally in $language.
            If the user asks to open an app (e.g. YouTube, WhatsApp, Camera, Settings) or call someone, confirm cheerfully that you are doing it.
            Keep your response conversational, concise (1-2 sentences), and friendly.
        """.trimIndent()

        // 1. Try Gemini 3.5 Flash
        if (geminiKey.isNotBlank()) {
            val reply35 = queryGeminiModel("models/gemini-3.5-flash", geminiKey, systemMsg, userText)
            if (reply35 != null) return@withContext reply35

            // 2. Failover to Gemini Flash Latest
            val replyFlash = queryGeminiModel("models/gemini-flash-latest", geminiKey, systemMsg, userText)
            if (replyFlash != null) return@withContext replyFlash
        }

        // 3. Failover to Groq API
        if (groqKey.isNotBlank()) {
            val groqReply = queryGroq(groqKey, systemMsg, userText)
            if (groqReply != null) return@withContext groqReply
        }

        // 4. Offline Fallback
        val lower = userText.lowercase()
        when {
            lower.contains("youtube") || lower.contains("ইউটিউব") -> "YouTube khulchhi! Wait a second..."
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> "WhatsApp khulchhi!"
            lower.contains("call") || lower.contains("ফোন") -> "Dialer khulchhi!"
            else -> "Ami apnar kotha shunte peyechhi. Bolo kibhabe sahajyo korbo?"
        }
    }

    private fun queryGeminiModel(model: String, apiKey: String, systemPrompt: String, userText: String): String? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/$model:generateContent?key=$apiKey"
            val jsonBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", "$systemPrompt\n\nUser: $userText")
                            }
                        }
                    }
                }
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseText = response.body?.string() ?: return null
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(responseText).jsonObject
                val candidates = root["candidates"]?.jsonArray
                val parts = candidates?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun queryGroq(apiKey: String, systemPrompt: String, userText: String): String? {
        return try {
            val url = "https://api.groq.com/openai/v1/chat/completions"
            val jsonBody = buildJsonObject {
                put("model", "llama-3.3-70b-versatile")
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                    addJsonObject {
                        put("role", "user")
                        put("content", userText)
                    }
                }
                put("temperature", 0.6)
                put("max_tokens", 300)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(jsonBody.toRequestBody(mediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseText = response.body?.string() ?: return null
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(responseText).jsonObject
                val choices = root["choices"]?.jsonArray
                val msg = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
                msg?.get("content")?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Synthesizes 24kHz 16-bit Mono PCM audio using Gemini Preview TTS
     */
    suspend fun generateSpeechAudio(text: String, voiceName: String = "Aoede"): ByteArray? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return@withContext null
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
            val jsonBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", text)
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    putJsonArray("responseModalities") {
                        add("AUDIO")
                    }
                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                put("voiceName", voiceName)
                            }
                        }
                    }
                }
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder().url(url).post(jsonBody.toRequestBody(mediaType)).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseText = response.body?.string() ?: return@withContext null
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(responseText).jsonObject
                val candidates = root["candidates"]?.jsonArray
                val parts = candidates?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                val inlineData = parts?.firstOrNull()?.jsonObject?.get("inlineData")?.jsonObject
                val b64 = inlineData?.get("data")?.jsonPrimitive?.content ?: return@withContext null
                android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- Personality State ---
    val personalityFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERSONALITY] ?: "friendly"
    }

    suspend fun setPersonality(personalityId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONALITY] = personalityId
        }
    }

    // --- Language Preference ---
    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "hinglish"
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    // --- Audio Echo & Noise Settings ---
    val echoCancellerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ECHO_CANCELLER] ?: true
    }

    val noiseSuppressorFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOISE_SUPPRESSOR] ?: true
    }

    suspend fun setEchoCanceller(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ECHO_CANCELLER] = enabled }
    }

    suspend fun setNoiseSuppressor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOISE_SUPPRESSOR] = enabled }
    }

    // --- 3D Orb & Edge Glow ---
    val orbStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ORB_STYLE] ?: "fractal_plasma"
    }

    val edgeGlowFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EDGE_GLOW] ?: true
    }

    suspend fun setEdgeGlow(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EDGE_GLOW] = enabled }
    }

    // --- JARVIS Human Voice Customization (5 Female & 5 Male) ---
    val voiceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_VOICE] ?: "female_friday"
    }

    val voicePitchFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_PITCH] ?: 1.0f
    }

    val voiceSpeedFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_SPEED] ?: 1.0f
    }

    suspend fun setVoiceId(voiceId: String) {
        context.dataStore.edit { it[KEY_SELECTED_VOICE] = voiceId }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.dataStore.edit { it[KEY_VOICE_PITCH] = pitch }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_VOICE_SPEED] = speed }
    }
}
