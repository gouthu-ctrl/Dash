package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.theme.*
import com.dash.travel.data.model.SupabaseTrip
import com.dash.travel.ui.viewmodel.AIPlannerViewModel
import com.dash.travel.ui.screens.ai.ChatMessage
import com.dash.travel.ui.screens.ai.MessageRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIPlannerScreen(
    viewModel: AIPlannerViewModel,
    onNavigateBack: () -> Unit,
    onCreateTripFromPlan: (SupabaseTrip) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val isGenerating by viewModel.isLoading

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Trip Planner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp), // Larger touch target
                        placeholder = { Text("Where do you want to go?") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (prompt.isNotBlank()) {
                                viewModel.sendMessage(prompt)
                                prompt = ""
                            }
                        }),
                        shape = MaterialTheme.shapes.large
                    )
                    // Large thumb-friendly send button
                    FilledIconButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                viewModel.sendMessage(prompt)
                                prompt = ""
                            }
                        },
                        modifier = Modifier.size(56.dp), // 56dp minimum touch target
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Primary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, 
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            reverseLayout = false, 
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             // Messages
             items(messages) { message ->
                AIChatBubble(message = message)
             }
             
             if (isGenerating) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun AIChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) Primary else SurfaceContainerHigh,
            contentColor = if (isUser) OnPrimary else OnSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Copy button for Assistant messages
        if (!isUser) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                TextButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.content))
                        // Android 13+ has built-in visual feedback, but Toast is safe backup
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
