# RAJON J.A.R.V.I.S. (v1.2.0)
### Personal Artificial Intelligence Assistant for Android

**Owner:** Rajon  
**Platform:** Android 8.0+ (API 26+) · Target Android 15 (API 35)  
**Primary Languages:** Bengali (বাংলা) & English  
**Architecture:** MVVM + Jetpack Compose + Clean Architecture + Hilt DI + Room DB + Multi-AI provider router (Gemini / Claude / GPT / Grok / DeepSeek / Qwen / Llama / Mistral / Kimi / GLM / Cohere)  

---

## 🌟 Overview

**RAJON J.A.R.V.I.S.** is an advanced, production-grade personal AI assistant built for Android. Unlike typical chatbots, J.A.R.V.I.S. acts as a true digital secretary and systems operator:
- **Speaks & Understands Bengali and English**: Converses naturally, switches languages seamlessly, and always addresses its owner respectfully as *"Rajon Sir"* or *"Sir"*.
- **Device Control & Telemetry**: Reads battery levels, internal storage stats, RAM usage, network connectivity, and hardware specifications.
- **Action Execution**: Directly launches installed applications, triggers WhatsApp messaging, initiates YouTube searches, adjusts settings, and sets exact reminders.
- **Persistent Memory & Context**: Remembers personal details, preferences, and recent conversational history across sessions via Room Database.
- **Sci-Fi Cyan Aesthetic**: Powered by Jetpack Compose Material 3 with animated glows, pulsing voice buttons, bouncing loading indicators, and dark mode.

---

## 📱 Architecture & Tech Stack

```mermaid
graph TD
    UI[Jetpack Compose UI & Screens] --> VM[ViewModels (Hilt)]
    VM --> AI[JarvisAI Brain]
    AI --> ROUTER[AiRouter - multi-AI failover]
    ROUTER --> P1[Gemini Provider]
    ROUTER --> P2[OpenAI-compatible Provider]
    ROUTER --> P3[Anthropic Provider]
    VM --> DEV[DeviceController]
    VM --> REPO[Repositories]
    REPO --> ROOM[Room Local SQLite DB]
    REPO --> PREF[Jetpack DataStore Preferences]
    AI --> ACT[ActionParser & Executor]
    ACT --> DEV
    VM --> SPK[SpeechManager (STT & TTS)]
```

- **UI Framework:** 100% Jetpack Compose (Material 3) with dynamic responsive layouts
- **Dependency Injection:** Dagger Hilt 2.51
- **Database:** Room SQLite 2.6.1 with Coroutines Flow
- **Preferences:** Jetpack DataStore Preferences
- **AI Brain:** provider-agnostic core — Gemini native REST, any OpenAI-compatible endpoint (OpenAI, OpenRouter, Groq, DeepSeek, xAI, Mistral, Qwen, Moonshot, Z.ai, Cohere, Together) and the Anthropic Messages API
- **Speech Engine:** Android `SpeechRecognizer` & `TextToSpeech` with Bengali (`bn-BD`) and English support, best-voice selection (bn-BD male first) and a voice picker
- **Background Tasks:** Android Foreground Service with persistent status notifications and Boot receiver

---

## 🧠 Multi-AI Core & Automatic Failover

`AiRouter` resolves every enabled provider that has a credential and a model, orders them
by task specialisation and priority, and calls them until one answers:

| Situation | What J.A.R.V.I.S. does |
|---|---|
| Provider returns **429** (quota / rate limit) | marks it as cooling down and immediately tries the next provider |
| Provider returns **5xx** | same hand-over |
| **Timeout / dropped connection** | same hand-over, the conversation continues |
| Network returns an **empty 404** (middlebox) | same hand-over and reports the network problem |
| Provider answers | the reply is shown with the provider name, and the chat header shows which brain answered |

* Conversation history and the J.A.R.V.I.S. system prompt are handed to the next provider
  unchanged, so a switch never loses context.
* Task routing (`AiTaskClassifier`) prefers coding/research specialists for code and
  research requests, a fast model for short conversation and Bengali-aware models for
  Bengali text.
* Providers are configured in **More → AI Settings**. Each card has its own key, model,
  *Models* (live discovery from the provider's `/models` endpoint) and *Test* button.
* The **OpenRouter** preset is the cheapest way to reach Claude, GPT, Gemini, Llama, Qwen,
  DeepSeek and Grok with a single key.

---

## 📂 Project Structure

```
jarvis/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/rajon/jarvis/
│   │   │   │   ├── JarvisApplication.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ai/
│   │   │   │   │   ├── ActionParser.kt
│   │   │   │   │   ├── ConversationManager.kt
│   │   │   │   │   └── JarvisAI.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── JarvisDatabase.kt
│   │   │   │   │   │   └── PreferencesManager.kt
│   │   │   │   │   └── model/
│   │   │   │   │       ├── ChatMessage.kt
│   │   │   │   │       ├── DeviceInfo.kt
│   │   │   │   │       ├── Memory.kt
│   │   │   │   │       └── Reminder.kt
│   │   │   │   ├── device/
│   │   │   │   │   ├── DeviceController.kt
│   │   │   │   │   └── ReminderManager.kt
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   └── JarvisNavHost.kt
│   │   │   │   ├── receiver/
│   │   │   │   │   ├── BootReceiver.kt
│   │   │   │   │   └── ReminderReceiver.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ChatRepository.kt
│   │   │   │   │   ├── MemoryRepository.kt
│   │   │   │   │   └── ReminderRepository.kt
│   │   │   │   ├── service/
│   │   │   │   │   └── JarvisService.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── BottomNavBar.kt
│   │   │   │   │   │   ├── GlowingText.kt
│   │   │   │   │   │   ├── LoadingDots.kt
│   │   │   │   │   │   ├── MessageBubble.kt
│   │   │   │   │   │   ├── StatusCard.kt
│   │   │   │   │   │   └── VoiceButton.kt
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── chat/
│   │   │   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   │   │   └── ChatViewModel.kt
│   │   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   │   │   └── DashboardViewModel.kt
│   │   │   │   │   │   ├── device/
│   │   │   │   │   │   │   ├── DeviceControlScreen.kt
│   │   │   │   │   │   │   └── DeviceControlViewModel.kt
│   │   │   │   │   │   └── settings/
│   │   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │   │       └── SettingsViewModel.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── voice/
│   │   │   │       └── SpeechManager.kt
│   │   │   └── res/
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   │   └── test/
│   │       └── java/com/rajon/jarvis/
│   │           └── ActionParserTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew
```

---

## 🚀 Setup & Build Instructions

### 1. Open in Android Studio
1. Launch **Android Studio Ladybug (or newer)**.
2. Select **Open** and choose `/data/user/0/com.termux/files/home/projects/jarvis`.
3. Allow Gradle to sync dependencies automatically.

### 2. Configure Gemini API Key
1. Go to [Google AI Studio](https://aistudio.google.com/) and create a free Gemini API key.
2. Open the J.A.R.V.I.S. app on your Android device.
3. Tap **Settings** in the bottom navigation bar.
4. Paste your key in the **Google Gemini API Key** field and tap **Save Key**.

### 3. Build & Run
- Connect your device or emulator.
- Select the `app` configuration.
- Click **Run** (`Shift + F10`) or execute in terminal:
  ```bash
  ./gradlew assembleDebug
  ```
  The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Security & Privacy
- **Encrypted Local Storage**: DataStore Preferences for sensitive keys.
- **Zero Cloud Transmission of Private Telemetry**: System metrics and personal memories remain locally on device.
- **Prompt Isolation**: Telemetry injected into context is strictly sanitized.
