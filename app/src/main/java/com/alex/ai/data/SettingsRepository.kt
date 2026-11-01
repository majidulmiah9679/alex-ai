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
    }

    // --- Secure Gemini API Key ---
    fun getApiKey(): String {
        return encryptedPrefs.getString(SECURE_API_KEY, "") ?: ""
    }

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(SECURE_API_KEY, apiKey.trim()).apply()
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
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
