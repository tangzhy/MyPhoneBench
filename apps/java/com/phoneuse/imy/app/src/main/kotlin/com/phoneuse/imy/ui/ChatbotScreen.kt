// Copyright 2025 PhoneUse Privacy Protocol Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.phoneuse.imy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen() {
    var showHistoryPanel by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("gpt-4o") }
    var showModelDialog by remember { mutableStateOf(false) }
    
    // Chat sessions
    var sessions by remember {
        mutableStateOf(
            listOf(
                ChatSession(
                    id = "1",
                    title = "New Session",
                    messages = mutableListOf()
                )
            )
        )
    }
    
    var currentSessionId by remember { mutableStateOf(sessions.firstOrNull()?.id ?: "") }
    val currentSession = remember(currentSessionId) {
        sessions.find { it.id == currentSessionId }
    }
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto scroll to bottom when new message added
    LaunchedEffect(currentSession?.messages?.size) {
        if (currentSession?.messages?.isNotEmpty() == true) {
            listState.animateScrollToItem(currentSession.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Use Agent") },
                navigationIcon = {
                    IconButton(onClick = { showHistoryPanel = !showHistoryPanel }) {
                        Icon(Icons.Default.Menu, contentDescription = "History")
                    }
                },
                actions = {
                    // Model selection button
                    TextButton(
                        onClick = { showModelDialog = true },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Model", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedModel, fontSize = 12.sp)
                    }
                    // New session button
                    IconButton(onClick = {
                        val newSession = ChatSession(
                            title = "New Session ${sessions.size + 1}",
                            messages = mutableListOf()
                        )
                        sessions = sessions + newSession
                        currentSessionId = newSession.id
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Session")
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // History Panel (Left Side)
            if (showHistoryPanel) {
                HistoryPanel(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSessionSelect = { sessionId ->
                        currentSessionId = sessionId
                        showHistoryPanel = false
                    },
                    onSessionDelete = { sessionId ->
                        sessions = sessions.filter { it.id != sessionId }
                        if (currentSessionId == sessionId) {
                            currentSessionId = sessions.firstOrNull()?.id ?: ""
                        }
                    },
                    onClose = { showHistoryPanel = false },
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                )
            }
            
            // Main Chat Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentSession?.messages?.isEmpty() == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "💬",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Text(
                                        text = "Start a conversation",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Describe your task for the Phone Use Agent",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(currentSession?.messages ?: emptyList()) { message ->
                            ChatMessageBubble(message = message)
                        }
                    }
                }
                
                // Input Area
                ChatInputArea(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() && currentSession != null) {
                            // Add user message
                            val userMessage = ChatMessage(
                                content = inputText,
                                isUser = true
                            )
                            currentSession.messages.add(userMessage)
                            
                            // Add agent response (placeholder)
                            val agentMessage = ChatMessage(
                                content = "I understand your task: \"$inputText\". This is a placeholder response. The actual Phone Use Agent will process this task.",
                                isUser = false
                            )
                            currentSession.messages.add(agentMessage)
                            
                            // Update session title if it's the first message
                            if (currentSession.messages.size == 2) {
                                val newTitle = if (inputText.length > 30) {
                                    inputText.take(30) + "..."
                                } else {
                                    inputText
                                }
                                sessions = sessions.map {
                                    if (it.id == currentSession.id) {
                                        it.copy(title = newTitle)
                                    } else {
                                        it
                                    }
                                }
                            }
                            
                            inputText = ""
                        }
                    },
                    onModelClick = { showModelDialog = true }
                )
            }
        }
    }
    
    // Model Selection Dialog
    if (showModelDialog) {
        ModelSelectionDialog(
            currentModel = selectedModel,
            onModelSelect = { model ->
                selectedModel = model
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false }
        )
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            // Agent avatar
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        
        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onModelClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Model selector in input area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = true,
                    onClick = onModelClick,
                    label = { Text("gpt-4o", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            // Input field and send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Describe your task for the Phone Use Agent...") },
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(
                            onClick = onSend,
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryPanel(
    sessions: List<ChatSession>,
    currentSessionId: String,
    onSessionSelect: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Divider()
            
            // Sessions list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions) { session ->
                    HistoryItem(
                        session = session,
                        isSelected = session.id == currentSessionId,
                        onClick = { onSessionSelect(session.id) },
                        onDelete = { onSessionDelete(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${session.messages.size} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ModelSelectionDialog(
    currentModel: String,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val models = listOf(
        "gpt-4o",
        "gpt-4-turbo",
        "gpt-4",
        "gpt-3.5-turbo",
        "claude-3-opus",
        "claude-3-sonnet",
        "gemini-pro"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Model") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                models.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelect(model) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = model == currentModel,
                            onClick = { onModelSelect(model) }
                        )
                        Text(
                            text = model,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
