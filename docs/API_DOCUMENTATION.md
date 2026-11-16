# RAJON J.A.R.V.I.S. - API & Action Documentation

## 1. Action Protocol Specification
When J.A.R.V.I.S. recognizes a task request from the owner, it appends a structured JSON code block to its response. The `ActionParser` extracts and executes this block immediately.

### Action JSON Format
```json
{
  "action": "<ACTION_NAME>",
  "params": {
    "<key>": "<value>"
  }
}
```

### Supported Actions

#### 1. `OPEN_APP`
Launches an installed Android application by name or package identifier.
```json
{
  "action": "OPEN_APP",
  "params": {
    "app_name": "whatsapp"
  }
}
```

#### 2. `CHECK_BATTERY`
Requests battery telemetry (level percentage, charging state).
```json
{
  "action": "CHECK_BATTERY",
  "params": {}
}
```

#### 3. `CHECK_STORAGE`
Queries internal storage space (free and total GB).
```json
{
  "action": "CHECK_STORAGE",
  "params": {}
}
```

#### 4. `CHECK_INTERNET`
Returns current active transport (Wi-Fi, Mobile Data, or Disconnected).
```json
{
  "action": "CHECK_INTERNET",
  "params": {}
}
```

#### 5. `CHECK_PERFORMANCE`
Returns complete diagnostic metrics summary (Device model, Android OS, Battery, RAM, and Storage).
```json
{
  "action": "CHECK_PERFORMANCE",
  "params": {}
}
```

#### 6. `OPEN_WHATSAPP`
Opens WhatsApp application or prepares a draft for a specific phone number.
```json
{
  "action": "OPEN_WHATSAPP",
  "params": {
    "phone": "+8801700000000",
    "message": "Hello Sir"
  }
}
```

#### 7. `SEARCH_YOUTUBE`
Searches YouTube videos by query term.
```json
{
  "action": "SEARCH_YOUTUBE",
  "params": {
    "query": "techno ambient mix"
  }
}
```

#### 8. `SET_REMINDER`
Schedules an exact high-priority reminder alarm on the Android system.
```json
{
  "action": "SET_REMINDER",
  "params": {
    "title": "Meeting with Client",
    "minutes": 30
  }
}
```

#### 9. `OPEN_SETTINGS`
Opens specific Android Settings panels.
```json
{
  "action": "OPEN_SETTINGS",
  "params": {
    "section": "android.settings.WIFI_SETTINGS"
  }
}
```

---

## 2. Gemini System Prompt Structure
Every prompt sent to Google Gemini 1.5 Flash includes:
1. **Identity & Honorific Rules**: Address user as "Rajon Sir" or "Sir".
2. **Language Directives**: Speak natural Bengali (Bangla) or English.
3. **Hardware Telemetry Context**: Live battery %, free storage, free RAM, network status.
4. **Owner Persistent Memories**: Stored facts from `MemoryRepository`.
5. **Recent Conversational Turns**: 10 most recent dialogue exchanges.
6. **Current User Instruction**: The active user message.
