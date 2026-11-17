package com.rajon.jarvis.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.BuildConfig
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisError
import com.rajon.jarvis.ui.theme.JarvisOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var ownerNameInput by remember(state.ownerName) { mutableStateOf(state.ownerName) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showClearMemoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "GENERAL SETTINGS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.GeneralSettings.route,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = JarvisDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HudSectionTitle(text = "IDENTITY & LANGUAGE") }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Owner Name",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ownerNameInput,
                        onValueChange = { ownerNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = JarvisCyan
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveOwnerName(ownerNameInput) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Name")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Preferred Language",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.language == "bn",
                            onClick = { viewModel.setLanguage("bn") },
                            label = { Text("বাংলা (Bangla)") },
                            leadingIcon = if (state.language == "bn") {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = state.language == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text("English") },
                            leadingIcon = if (state.language == "en") {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The AI replies and the voice output follow this language.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            item { HudSectionTitle(text = "ASSISTANT BEHAVIOUR") }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    SettingToggleRow(
                        title = "Voice replies (TTS)",
                        description = "J.A.R.V.I.S. speaks every reply aloud",
                        checked = state.isTtsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) }
                    )
                    SettingToggleRow(
                        title = "Voice input",
                        description = "Allow microphone commands in Chat and Voice",
                        checked = state.isVoiceInputEnabled,
                        onCheckedChange = { viewModel.setVoiceInputEnabled(it) }
                    )
                    SettingToggleRow(
                        title = "Dark sci-fi theme",
                        description = "Turn off for the light palette",
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            }

            item { HudSectionTitle(text = "VOICE OUTPUT") }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Text-to-Speech voice",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (voices.isEmpty()) {
                            "No TTS voice found yet. Install Google TTS voice data (Bengali bn-BD) from Android Settings > Accessibility/Text-to-speech output."
                        } else {
                            "J.A.R.V.I.S. prefers a bn-BD male voice, then bn-IN, then any Bengali voice, then English."
                        },
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.selectBestVoice() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Use best voice", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.refreshVoices() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Refresh list", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    voices.forEach { voice ->
                        val isSelected = state.ttsVoice == voice.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${voice.locale}${if (voice.isMale) " - male" else ""}",
                                    color = if (isSelected) JarvisCyan else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = "${voice.name} - ${voice.quality}${if (voice.networkRequired) " - network" else " - offline"}",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.previewVoice(voice) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Play", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.size(6.dp))
                            OutlinedButton(
                                onClick = { viewModel.selectVoice(voice) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isSelected) "In use" else "Use", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item { HudSectionTitle(text = "DATA & MEMORY") }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showClearChatDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Text(text = " Clear Chat History", modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearMemoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Text(text = " Clear Learned Memories", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisCyan.copy(alpha = 0.20f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlowingText(
                            text = "RAJON J.A.R.V.I.S.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} • Android assistant",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Personal AI assistant created for Rajon Sir",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("Delete all stored conversation history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearChatDialog = false
                    }
                ) {
                    Text("Clear", color = JarvisError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearMemoryDialog = false },
            title = { Text("Clear Learned Memories?") },
            text = { Text("Delete all long-term memory points remembered by J.A.R.V.I.S.?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMemories()
                        showClearMemoryDialog = false
                    }
                ) {
                    Text("Clear All", color = JarvisError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = JarvisCyan,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
