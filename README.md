# RAJON J.A.R.V.I.S. (v1.0)
### Personal Artificial Intelligence Assistant for Android

**Owner:** Rajon  
**Platform:** Android 8.0+ (API 26+) · Target Android 15 (API 35)  
**Primary Languages:** Bengali (বাংলা) & English  
**Architecture:** MVVM + Jetpack Compose + Clean Architecture + Hilt DI + Room DB + Google Gemini Generative AI  

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
    VM --> AI[JarvisAI Brain (Gemini 1.5 Flash)]
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
- **AI Brain:** Google Generative AI (Gemini 1.5 Flash) with fallback offline replies
- **Speech Engine:** Android `SpeechRecognizer` & `TextToSpeech` with Bengali (`bn-BD`) and English support
- **Background Tasks:** Android Foreground Service with persistent status notifications and Boot receiver

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
