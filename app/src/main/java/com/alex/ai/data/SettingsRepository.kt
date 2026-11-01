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

    companion object {
        private val KEY_PERSONALITY = stringPreferencesKey("personality_id")
        private val KEY_LANGUAGE = stringPreferencesKey("target_language")
        private val KEY_ECHO_CANCELLER = booleanPreferencesKey("enable_echo_canceller")
        private val KEY_NOISE_SUPPRESSOR = booleanPreferencesKey("enable_noise_suppressor")
        private val KEY_ORB_STYLE = stringPreferencesKey("orb_style")
        private val KEY_EDGE_GLOW = booleanPreferencesKey("enable_edge_glow")
        private val KEY_HAPTICS = booleanPreferencesKey("enable_haptics")
        private const val SECURE_API_KEY = "gemini_live_api_key"
        private const val PRESET_KEY_B64 = "QVEuQWI4Uk42SVU1Um90aVNzQVBoYnZuaF96NzFzM2ZVZ2JPb2lCTE1YU1BKUUR5cFJDbFE="
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

    suspend fun validateApiKey(apiKey: String): Pair<Boolean, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val testKey = apiKey.trim().ifEmpty { getApiKey() }
        if (testKey.isBlank()) return@withContext Pair(false, "API Key is empty")
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$testKey"
            val request = okhttp3.Request.Builder().url(url).build()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "API Key Verified & Supported ✓")
                } else {
                    Pair(false, "API Error: ${response.code} (Invalid Key)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.message}")
        }
    }

    suspend fun generateChatMessage(
        userText: String,
        personalityPrompt: String,
        language: String
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return@withContext "Please configure your Gemini API Key in Settings first."
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val systemMsg = """
                $personalityPrompt
                [LANGUAGE INSTRUCTION]: Respond in $language. Keep it conversational, warm, and brief (2-3 sentences max).
            """.trimIndent()

            val jsonBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", "$systemMsg\n\nUser: $userText")
                            }
                        }
                    }
                }
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()
            val client = OkHttpClient()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Gemini API error ${response.code}"
                val responseText = response.body?.string() ?: ""
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(responseText).jsonObject
                val candidates = root["candidates"]?.jsonArray
                val parts = candidates?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                val reply = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                reply ?: "..."
            }
        } catch (e: Exception) {
            "Connection error: ${e.message}"
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
        prefs[KEY_LANGUAGE] ?: "hinglish" // "hi", "hinglish", "bn", "en"
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    // --- Audio Echo & Noise Settings (Part 05) ---
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

    // --- 3D Orb & Edge Glow (Part 10) ---
    val orbStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ORB_STYLE] ?: "fractal_plasma"
    }

    val edgeGlowFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EDGE_GLOW] ?: true
    }

    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTICS] ?: true
    }

    suspend fun setOrbStyle(style: String) {
        context.dataStore.edit { it[KEY_ORB_STYLE] = style }
    }

    suspend fun setEdgeGlow(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EDGE_GLOW] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS] = enabled }
    }
}
