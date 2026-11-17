package com.rajon.jarvis.ui.screens.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.ai.provider.ProviderConfig
import com.rajon.jarvis.ai.provider.ProviderType
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkOutline
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisError
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    navController: NavController,
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val savedKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val providerConfigs by viewModel.providerConfigs.collectAsStateWithLifecycle()
    val providerStates by viewModel.providerStates.collectAsStateWithLifecycle()
    val failoverChain by viewModel.failoverChain.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var apiKeyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "AI SETTINGS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.AiSettings.route,
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
            item { HudSectionTitle(text = "GEMINI CONNECTION") }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Google Gemini API Key",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stored in this app's private DataStore, never printed to logs and never sent anywhere except Google's API. Both new AQ... Auth keys and older AIza... keys are accepted; this app does not check the prefix.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter AI Studio API key", color = Color.Gray) },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = JarvisCyan
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveApiKey(apiKeyInput) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Key")
                        }

                        OutlinedButton(
                            onClick = { viewModel.testConnection(apiKeyInput) },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = JarvisCyan,
                                    strokeWidth = 2.dp
                                )
                                Text(" Testing", fontSize = 12.sp)
                            } else {
                                Text("Test Connection", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://aistudio.google.com/apikey")
                                    )
                                )
                            } catch (_: Exception) {
                                // No browser available; the URL is still shown below.
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Get a key at aistudio.google.com/apikey", fontSize = 11.sp)
                    }
                }
            }

            if (testResult != null) {
                val result = testResult!!
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (result.isSuccess) JarvisSuccess.copy(alpha = 0.6f) else JarvisError.copy(alpha = 0.6f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (result.isSuccess) JarvisSuccess else JarvisError
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (result.isSuccess) "Connection successful" else "Connection failed",
                                color = if (result.isSuccess) JarvisSuccess else JarvisError,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result.message,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("HTTP ${result.httpCode ?: "—"}")
                                result.latencyMs?.let { append(" • ${it} ms") }
                            },
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            item { HudSectionTitle(text = "MULTI-AI FAILOVER") }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisCyan.copy(alpha = 0.30f)
                ) {
                    Text(
                        text = "Automatic provider chain",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (failoverChain.isEmpty()) {
                            "No provider is ready yet. Add at least one API key below."
                        } else {
                            failoverChain.joinToString("   >   ") { "${it.displayName} (${it.model})" }
                        },
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When a provider returns 429 (quota or rate limit), any 5xx, a timeout or an empty 404, J.A.R.V.I.S. switches to the next provider automatically and keeps the conversation context. Coding and research requests prefer specialist models, short conversation prefers a fast model.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            items(providerConfigs.filter { it.id != "gemini" }) { config ->
                ProviderCard(
                    config = config,
                    state = providerStates[config.id],
                    onSave = { model, key -> viewModel.saveProvider(config.id, model, key) },
                    onTest = { model, key -> viewModel.testProvider(config.id, model, key) },
                    onDiscover = { key -> viewModel.discoverModels(config.id, key) },
                    onToggle = { enabled -> viewModel.setProviderEnabled(config.id, enabled) }
                )
            }

            item { HudSectionTitle(text = "MODEL & TRANSPORT") }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisCyan.copy(alpha = 0.30f)
                ) {
                    InfoLine("Model", viewModel.modelName, JarvisCyan)
                    InfoLine("API version", "Gemini REST ${viewModel.apiVersion}", Color.LightGray)
                    InfoLine("Inputs", "Text, image, audio, video, PDF", Color.LightGray)
                    InfoLine("Output", "Text (spoken by Android TTS)", Color.LightGray)
                    InfoLine("Timeouts", "15s connect • 60s read • 90s total", Color.LightGray)
                    InfoLine("Retry", "1 automatic retry on transport reset", Color.LightGray)
                    InfoLine("Thinking", "low (optimised for voice latency)", Color.LightGray)
                }
            }

            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisOrange.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "Why REST instead of the old SDK?",
                        color = JarvisOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The bundled Google AI SDK is frozen at 0.9.0 (July 2024) and cannot set Gemini 3 thinking levels or custom timeouts. The app now uses the same Gemini API over HTTPS with OkHttp, including real HTTP status codes and retry logic.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun ProviderCard(
    config: ProviderConfig,
    state: ProviderUiState?,
    onSave: (String, String) -> Unit,
    onTest: (String, String) -> Unit,
    onDiscover: (String) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    var modelInput by remember(config.id, config.model) { mutableStateOf(config.model) }
    var keyInput by remember(config.id, config.apiKey) { mutableStateOf(config.apiKey) }
    var keyVisible by remember(config.id) { mutableStateOf(false) }

    val isBusy = state?.isBusy == true
    val ready = config.apiKey.isNotBlank() && config.model.isNotBlank()

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (ready) JarvisSuccess.copy(alpha = 0.45f) else JarvisDarkOutline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (config.type) {
                        ProviderType.GEMINI -> "Gemini native REST"
                        ProviderType.OPENAI_COMPATIBLE -> "OpenAI compatible REST"
                        ProviderType.ANTHROPIC -> "Anthropic Messages API"
                    },
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JarvisCyan,
                    checkedTrackColor = JarvisCyan.copy(alpha = 0.35f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = JarvisDarkOutline
                )
            )
        }

        if (config.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = config.notes, color = Color.Gray, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Model id (discover it with Models)", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("API key", color = Color.Gray) },
            singleLine = true,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = JarvisCyan
                )
            },
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle key visibility",
                        tint = Color.Gray
                    )
                }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSave(modelInput, keyInput) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { onTest(modelInput, keyInput) },
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = JarvisCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Test", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { onDiscover(keyInput) },
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(" Models", fontSize = 12.sp)
            }
        }

        val currentState = state
        if (currentState != null && currentState.message != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentState.message,
                color = when (currentState.isSuccess) {
                    true -> JarvisSuccess
                    false -> JarvisError
                    null -> Color.LightGray
                },
                fontSize = 11.sp
            )
        }

        if (currentState != null && currentState.discoveredModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Available models: " + currentState.discoveredModels.take(10).joinToString(", "),
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
