package com.rajon.jarvis.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.ArcReactor
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.ui.components.JarvisHudBackground
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanDark
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisError
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    navController: NavController,
    autoStart: Boolean = false,
    viewModel: VoiceAssistantViewModel = hiltViewModel()
) {
    val transcript by viewModel.transcript.collectAsStateWithLifecycle()
    val reply by viewModel.reply.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val agoraState by viewModel.agoraState.collectAsStateWithLifecycle()
    val agoraTranscripts by viewModel.agoraTranscripts.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startListening()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for voice commands.")
            }
        }
    }

    LaunchedEffect(autoStart) {
        if (autoStart) {
            if (hasPermission) {
                viewModel.startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "VOICE ASSISTANT",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (isSpeaking) {
                        OutlinedButton(
                            onClick = { viewModel.stopSpeaking() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(" Stop", fontSize = 11.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Voice.route,
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
        JarvisHudBackground(isActive = isListening || isSpeaking) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isListening) JarvisSuccess else JarvisCyan.copy(alpha = 0.55f),
                    contentPadding = PaddingValues(vertical = 18.dp, horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ArcReactor(
                            coreSize = 184.dp,
                            isActive = isListening || isProcessing || isSpeaking,
                            isListening = isListening,
                            isSpeaking = isSpeaking
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when {
                                isListening -> "LISTENING..."
                                isProcessing -> "PROCESSING..."
                                isSpeaking -> "SPEAKING..."
                                else -> "TAP THE MIC TO SPEAK"
                            },
                            color = when {
                                isListening -> JarvisSuccess
                                isProcessing -> JarvisOrange
                                isSpeaking -> JarvisCyan
                                else -> Color.Gray
                            },
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            if (isListening) JarvisOrange else JarvisCyan,
                                            JarvisCyanDark
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.toggleListening()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Start listening",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isListening) "Tap again to stop" else "Tap to start listening",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!hasPermission) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = JarvisOrange.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Microphone permission required",
                            color = JarvisOrange,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Voice commands need RECORD_AUDIO access. J.A.R.V.I.S. only records while you hold a listening session.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Microphone Access")
                        }
                    }
                }
            }

            if (transcript.isNotBlank()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(text = "YOU SAID", accent = JarvisOrange)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transcript,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (reply.isNotBlank()) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = JarvisCyan.copy(alpha = 0.55f)
                    ) {
                        HudSectionTitle(text = "J.A.R.V.I.S.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = reply,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisCyan.copy(alpha = 0.20f),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "Bengali + English voice",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recognition uses your device's speech service. Speech output uses the installed TTS voice; if no Bangla voice pack is installed, J.A.R.V.I.S. speaks Bengali text with the English voice.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            item { HudSectionTitle(text = "AGORA LIVE VOICE", accent = JarvisOrange) }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisOrange.copy(alpha = 0.35f)
                ) {
                    val agoraRunning = agoraState !is com.rajon.jarvis.agora.AgoraSessionState.Idle &&
                        agoraState !is com.rajon.jarvis.agora.AgoraSessionState.Error
                    val status = when (agoraState) {
                        is com.rajon.jarvis.agora.AgoraSessionState.Connecting -> "Connecting…"
                        is com.rajon.jarvis.agora.AgoraSessionState.Listening -> "Listening through Agora…"
                        is com.rajon.jarvis.agora.AgoraSessionState.Speaking -> "J.A.R.V.I.S. is speaking…"
                        is com.rajon.jarvis.agora.AgoraSessionState.Error ->
                            (agoraState as com.rajon.jarvis.agora.AgoraSessionState.Error).message
                        else -> "Not connected"
                    }
                    Text(
                        text = status,
                        color = if (agoraState is com.rajon.jarvis.agora.AgoraSessionState.Error) {
                            JarvisError
                        } else {
                            Color.White
                        },
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.toggleAgoraVoice() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (agoraRunning) JarvisError else JarvisCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (agoraRunning) "End Agora conversation" else "Start Agora conversation")
                    }

                    if (agoraTranscripts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        agoraTranscripts.takeLast(6).forEach { entry ->
                            Text(
                                text = "${entry.type}: ${entry.text}",
                                color = if (entry.type.contains("agent", ignoreCase = true)) {
                                    JarvisCyan
                                } else {
                                    Color.LightGray
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        } // JarvisHudBackground
    }
}
