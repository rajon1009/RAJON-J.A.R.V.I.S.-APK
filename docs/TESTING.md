# RAJON J.A.R.V.I.S. - Testing Documentation & QA Checklist

## 1. Automated Unit Tests
Automated tests are located in `app/src/test/java/com/rajon/jarvis/`:
- **`ActionParserTest`**:
  - `testExtractOpenAppAction`: Verifies JSON extraction of `OPEN_APP` payload.
  - `testExtractCheckBatteryAction`: Verifies JSON extraction of `CHECK_BATTERY` payload.
  - `testExtractSetReminderAction`: Verifies parsing of reminder title and delay parameter.
  - `testCleanResponseText`: Confirms removal of raw JSON delimiters from user-facing text.

To run automated unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 2. Manual Verification Checklist

### A. Conversation & AI Core
- [ ] Launch application and verify initial greeting displays "Good [morning/afternoon/evening], Rajon Sir".
- [ ] Send text message in English: verify response addresses user as "Sir".
- [ ] Send text message in Bangla: verify J.A.R.V.I.S. responds fluently in Bangla.
- [ ] Tap microphone button: speak a voice instruction in Bangla or English; verify transcription and automatic submission.
- [ ] Verify speech synthesis (TTS) reads assistant response aloud when TTS is enabled.

### B. Device Telemetry & Actions
- [ ] Dashboard: verify battery percentage matches system battery.
- [ ] Dashboard: verify storage used / total matches Android Settings.
- [ ] Dashboard: verify RAM usage reflects available memory.
- [ ] Dashboard: tap "Refresh" icon and observe telemetry update.
- [ ] Chat: ask "Open YouTube" or "Open WhatsApp" and verify application opens.
- [ ] Chat: ask "What is my battery level?" and verify accurate reading.

### C. Device Controller Screen
- [ ] Navigate to Device screen.
- [ ] Verify hardware details (Manufacturer, Model, Android version).
- [ ] Test shortcuts: "Settings", "Wi-Fi", "Battery", "Apps".
- [ ] Filter installed applications using search bar and launch one by tapping.

### D. Settings & Data Persistence
- [ ] Update Gemini API key; kill app and relaunch; verify key remains saved.
- [ ] Change owner name to custom title; verify greetings and responses reflect new title.
- [ ] Toggle TTS off; verify assistant replies are silent.
- [ ] Tap "Clear Chat History"; verify confirmation dialog appears and history is wiped upon confirmation.
- [ ] Tap "Clear Learned Memories"; verify confirmation dialog appears and memories are erased.
