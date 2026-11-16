# RAJON J.A.R.V.I.S. - Security & Permissions Implementation

## 1. Android Permission Manifest Audit
The application requests only necessary permissions required to fulfill the J.A.R.V.I.S. blueprint:

| Permission | Purpose |
|------------|---------|
| `android.permission.INTERNET` | Connect to Google Gemini Generative AI endpoints. |
| `android.permission.ACCESS_NETWORK_STATE` | Detect Wi-Fi and Cellular connectivity states. |
| `android.permission.ACCESS_WIFI_STATE` | Verify active Wi-Fi link parameters. |
| `android.permission.RECORD_AUDIO` | Voice speech recognition via Android `SpeechRecognizer`. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Schedule exact reminder notifications. |
| `android.permission.USE_EXACT_ALARM` | Ensure punctual execution on Android 13+. |
| `android.permission.POST_NOTIFICATIONS` | Deliver reminder and status alerts on Android 13+. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Resume assistant background service upon device boot. |
| `android.permission.QUERY_ALL_PACKAGES` | Inspect installed packages to launch apps on voice command. |
| `android.permission.FOREGROUND_SERVICE` | Maintain persistent assistant service with user-visible notification. |

## 2. API Key Security
- **DataStore Storage**: The Google Gemini API key is persisted inside private app sandbox storage (`jarvis_preferences.preferences_pb`), preventing external applications without root privileges from reading it.
- **Obfuscation**: User inputs are masked with `PasswordVisualTransformation` in the Settings screen.
- **Zero Third-Party Relays**: All requests are dispatched directly between the Android device and Google's official Gemini API endpoints.

## 3. Privacy Safeguards
- **On-Device Hardware Telemetry**: Battery, RAM, and Storage stats are inspected locally via standard Android SDK APIs.
- **No Unauthorized Actions**: Critical destructive actions (e.g. deleting files, clearing memories) require explicit user confirmation dialogs.
