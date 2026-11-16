# RAJON J.A.R.V.I.S. - Architecture Blueprint

## Architectural Paradigm
The J.A.R.V.I.S. Android application is built following Modern Android Architecture (MAD) standards, using the **MVVM (Model-View-ViewModel)** design pattern with unidirectional data flow (UDF) and Clean Architecture separation.

### Layers

### 1. Presentation Layer (Jetpack Compose UI)
- **Declarative Composable Screens**: `DashboardScreen`, `ChatScreen`, `DeviceControlScreen`, `SettingsScreen`.
- **Reusable Atoms & Components**:
  - `MessageBubble`: Animated chat messages distinguishing user prompts and assistant replies with timestamp and cyan border.
  - `StatusCard`: Displays system metrics (battery, memory, storage) with live progress indicators.
  - `VoiceButton`: Animated circular microphone with state toggling.
  - `LoadingDots`: Bouncing 3-dot typing indicator.
  - `GlowingText`: Sci-fi neon title headers.
  - `BottomNavBar`: Navigation bar coordinating between screens.
- **ViewModels**: State holders surviving configuration changes, powered by Hilt `@HiltViewModel`.
- **State Holders**: Flows exposed as `StateFlow` and consumed in Compose via `collectAsStateWithLifecycle()`.

### 2. Domain & AI Integration Layer
- **`JarvisAI`**: Core neural coordinator integrating Google Generative AI (Gemini 1.5 Flash).
  - Dynamically synthesizes user instructions, device telemetry, owner memories, and conversational history.
  - Parses structured JSON actions.
  - Generates culturally aligned Bengali / English natural responses addressing the user as "Sir".
- **`ActionParser`**: Sanitizes LLM outputs and extracts executable action payloads (e.g. `OPEN_APP`, `CHECK_BATTERY`, `SET_REMINDER`, `OPEN_SETTINGS`).
- **`ConversationManager`**: Coordinates session context and persists recent turns into SQLite.

### 3. Device & Hardware Control Layer
- **`DeviceController`**: Queries hardware status:
  - Battery capacity and charging state via `BatteryManager` and `Intent.ACTION_BATTERY_CHANGED`.
  - Storage statistics using `StatFs` on `/data`.
  - RAM availability via `ActivityManager.MemoryInfo`.
  - Network state via `ConnectivityManager` and `NetworkCapabilities`.
  - Package Manager integration for searching and launching installed Android apps.
  - Intent dispatches for WhatsApp, YouTube search, and System Settings.
- **`ReminderManager`**: Schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()` targeting `ReminderReceiver`.
- **`SpeechManager`**: Integrates `SpeechRecognizer` and `TextToSpeech` with Bengali (`bn-BD`) and English fallback.

### 4. Data Layer (Room & DataStore)
- **Room Database (`JarvisDatabase`)**:
  - `ChatMessageDao`: Stores chat history with conversation ID segregation.
  - `MemoryDao`: Key-value persistent memory storage for owner facts and preferences.
  - `ReminderDao`: Stores active and historical scheduled alarms.
- **DataStore (`PreferencesManager`)**:
  - Non-blocking asynchronous key-value persistence for API keys, theme choice, language selection, and speech settings.
