package com.rajon.jarvis.ui.screens.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.model.Reminder
import com.rajon.jarvis.device.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationViewModel @Inject constructor(
    private val reminderManager: ReminderManager
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = reminderManager.getUpcomingReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _minutes = MutableStateFlow("10")
    val minutes: StateFlow<String> = _minutes.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateMinutes(value: String) {
        _minutes.value = value.filter { it.isDigit() }.take(4)
    }

    fun createReminder() {
        val title = _title.value.trim()
        val minutes = _minutes.value.toIntOrNull()

        if (title.isBlank()) {
            _feedback.value = "Enter a reminder title first."
            return
        }
        if (minutes == null || minutes <= 0) {
            _feedback.value = "Enter a valid number of minutes."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                reminderManager.scheduleReminder(
                    title = title,
                    description = "Scheduled from Automation",
                    triggerTime = triggerAt
                )
                _title.value = ""
                _feedback.value = "Reminder '$title' set for $minutes minutes from now."
            } catch (e: Exception) {
                _feedback.value = "Could not schedule reminder: ${e.message ?: "unknown error"}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun cancelReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                reminderManager.cancelReminder(reminder)
                _feedback.value = "Reminder '${reminder.title}' cancelled."
            } catch (e: Exception) {
                _feedback.value = "Could not cancel reminder: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun dismissFeedback() {
        _feedback.value = null
    }
}
