# 🌟 Alex AI — Native Android Voice-First Companion (Kotlin + Jetpack Compose)

**Alex** is a native Android voice-first AI companion. You talk to it in **Bangla (বাংলা)**, **Hindi (हिन्दी)**, **Hinglish**, or **English** in real time. It opens apps, calls contacts, prepares WhatsApp and SMS messages, chats in text, controls the screen via accessibility (direct flavor), and builds live 3D animated websites via the in-app **3D Website Forge**.

---

## 🏗️ 15-Part Architecture & Implementation Index

| Part | Component | Source Location | Description |
| :--- | :--- | :--- | :--- |
| **01** | **Project Setup & Two Build Flavors** | [`build.gradle.kts`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/build.gradle.kts) | Kotlin + Jetpack Compose with two product flavors: `store` and `direct` |
| **02** | **Settings, State & API Key Storage** | [`SettingsRepository.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/data/SettingsRepository.kt) | `EncryptedSharedPreferences` for Gemini API Key + DataStore Preferences |
| **03** | **10 Personalities & Multilingual Brain** | [`Personalities.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/domain/Personalities.kt) | 10 Archetypes: Normal, Friendly, Funny, Teacher, Developer, Nautanki, Kolkata Adda, Desi Bro, Zen, Roast |
| **04** | **Gemini Live Full-Duplex Client** | [`GeminiLiveClient.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/live/GeminiLiveClient.kt) | OkHttp WebSocket client for `BidiGenerateContent` bidirectional PCM voice stream |
| **05** | **Microphone, Speaker & Echo Guard** | [`AudioEngine.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/audio/AudioEngine.kt) | 16kHz `AudioRecord` + `AcousticEchoCanceler` + `NoiseSuppressor` + 24kHz `AudioTrack` + instant barge-in |
| **06** | **Voice Session & Background Service** | [`AlexVoiceService.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/service/AlexVoiceService.kt) | Persistent microphone Foreground Service, partial wake lock, and auto-reconnect backoff |
| **07** | **Phone Actions & App Launcher** | [`PhoneActionExecutor.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/actions/PhoneActionExecutor.kt) | Native Android Intents for Calls, SMS, WhatsApp, YouTube, Camera, Maps, Settings |
| **08** | **Text Chat with Gemini** | [`ChatScreen.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/screens/ChatScreen.kt) | Synchronized chat bubbles, action badges, and keyboard input fallback |
| **09** | **Design System & Theme** | [`Color.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/theme/Color.kt), [`Theme.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/theme/Theme.kt) | Deep Indigo Night (`#0B0E1E`) + Marigold Accent (`#FFB300`) design system |
| **10** | **3D Orb, Tilt Light & Edge Glow** | [`AlexOrbShader.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/shader/AlexOrbShader.kt), [`AlexOrbView.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/components/AlexOrbView.kt) | AGSL RuntimeShader on GPU with analytic normals, fractal noise, accelerometer tilt, voice ripples & glowing edge |
| **11** | **All Screens & Navigation** | [`HomeScreen.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/screens/HomeScreen.kt), [`PersonalityScreen.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/screens/PersonalityScreen.kt), [`SettingsScreen.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/screens/SettingsScreen.kt) | Full bottom navigation between Home/Voice, Chat, Forge, Personas, and Settings |
| **11b** | **3D Look for Every Screen** | Compose 3D Surface Elevation | Frosted glass cards with cyan borders, specular shadows, and depth layers |
| **12** | **Social Visual Agent (direct only)** | [`AlexAccessibilityService.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/accessibility/AlexAccessibilityService.kt) | Screen hierarchy inspector, auto-click, typing, and social media posting with safety checkpoints |
| **13** | **Website Forge: Live 3D Builder** | [`Forge3DSceneGenerator.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/forge/Forge3DSceneGenerator.kt), [`ForgeScreen.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/ui/screens/ForgeScreen.kt) | 3-Stage build animation (Reactor core charges -> Glass slabs stack -> Stage ring lights up) + final site reveal |
| **14/15** | **Build & Integration** | [`MainActivity.kt`](file:///data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/alex-ai/app/src/main/java/com/alex/ai/MainActivity.kt) | Service binding, runtime permissions, and Gradle build instructions |

---

## 🛠️ Building & Packaging the APK

### Build Flavor 1: `store` (Play Store Distribution)
Builds standard APK without Accessibility Service to comply with Play Store policies:
```bash
./gradlew assembleStoreRelease
```
Output:
`app/build/outputs/apk/store/release/app-store-release.apk`

### Build Flavor 2: `direct` (Full Visual Agent APK)
Builds direct APK with full Visual Agent, Accessibility Service, and social automation enabled:
```bash
./gradlew assembleDirectRelease
```
Output:
`app/build/outputs/apk/direct/release/app-direct-release.apk`
