package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.heuscher.gentlemessaging.data.Message
import ch.heuscher.gentlemessaging.data.ThreemaBridge
import ch.threema.app.R
import ch.threema.app.compose.common.AvatarAsync
import ch.threema.storage.models.ContactModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified Chat Screen.
 * Shows message history and input field.
 * Avatar in header is clickable to open profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatEntry: ThreemaBridge.ChatEntry,
    messages: List<Message>,
    quickReplies: List<String> = emptyList(),
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val isGroup = chatEntry is ThreemaBridge.ChatEntry.GroupEntry
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAvatarClick() }
                    ) {
                        // Avatar(s)
                        if (isGroup) {
                            val groupEntry = chatEntry as ThreemaBridge.ChatEntry.GroupEntry
                            // Stacked avatars for group
                            Box(modifier = Modifier.width(56.dp).height(40.dp)) {
                                // Group avatar first (background)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.CenterStart)
                                        .zIndex(1f)
                                ) {
                                    AvatarAsync(
                                        modifier = Modifier.fillMaxSize(),
                                        receiverModel = groupEntry.receiverModel,
                                        contentDescription = groupEntry.name,
                                        fallbackIcon = R.drawable.ic_group,
                                        showWorkBadge = false
                                    )
                                }
                                // Show first member avatar offset (if available)
                                if (groupEntry.members.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.BottomEnd)
                                            .zIndex(2f)
                                    ) {
                                        AvatarAsync(
                                            modifier = Modifier.fillMaxSize(),
                                            receiverModel = groupEntry.members.first(),
                                            contentDescription = null,
                                            fallbackIcon = R.drawable.ic_contact,
                                            showWorkBadge = false
                                        )
                                    }
                                }
                            }
                        } else {
                            // Single contact avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            ) {
                                AvatarAsync(
                                    modifier = Modifier.fillMaxSize(),
                                    receiverModel = chatEntry.receiverModel,
                                    contentDescription = chatEntry.name,
                                    fallbackIcon = R.drawable.ic_contact,
                                    showWorkBadge = false
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = chatEntry.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                reverseLayout = true, // Show newest at bottom
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }

            // Quick Reply Strip
            if (quickReplies.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickReplies) { reply ->
                        SuggestionChip(
                            onClick = { onSendMessage(reply) },
                            label = {
                                Text(
                                    text = reply,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
            }
            
            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nachricht...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Senden",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isMe = message.isOutgoing
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (!isMe) {
                 Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
        
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp, 
                    topEnd = 16.dp, 
                    bottomStart = if (isMe) 16.dp else 4.dp, 
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.content,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

