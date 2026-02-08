package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.heuscher.gentlemessaging.data.ThreemaBridge.ChatEntry
import ch.heuscher.gentlemessaging.ui.components.PinDialog
import ch.heuscher.gentlemessaging.ui.theme.*
import ch.threema.app.R
import ch.threema.app.compose.common.AvatarAsync
import ch.threema.storage.models.GroupModel

/**
 * Unified Home Screen displaying all Contacts and Groups.
 * Acts like a simple phone book / chat list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    chatEntries: List<ChatEntry>,
    onChatClick: (ChatEntry) -> Unit,
    onAdminClick: () -> Unit,
    isAdminMode: Boolean = false,
    adminPin: String = "1234"
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    
    if (showPinDialog) {
        PinDialog(
            onDismiss = { 
                showPinDialog = false 
                pinError = false
            },
            onPinEntered = { pin ->
                if (pin == adminPin) {
                    showPinDialog = false
                    pinError = false
                    onAdminClick()
                } else {
                    pinError = true
                }
            },
            isError = pinError
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Simple Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = { showPinDialog = true }
                )
            ) {
                Text(
                    text = "gentle messages",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isAdminMode) {
                    Text(
                        text = "Admin Modus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Combined List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatEntries) { entry ->
                ChatListItem(
                    entry = entry,
                    onClick = { onChatClick(entry) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(60.dp)) // Bottom padding
            }
        }
    }
}

@Composable
fun ChatListItem(
    entry: ChatEntry,
    onClick: () -> Unit
) {
    val isGroup = entry is ChatEntry.GroupEntry
    
    // DISTINCT VISUALS
    // Groups: Purple Card
    // Contacts: Teal Card (or Surface)
    
    val containerColor = if (isGroup) {
         MaterialTheme.colorScheme.primaryContainer // Purple 200 equivalent
    } else {
        MaterialTheme.colorScheme.secondaryContainer // Teal equivalent
    }
    
    val contentColor = if (isGroup) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar - using real Threema avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarAsync(
                    modifier = Modifier.fillMaxSize(),
                    receiverModel = entry.receiverModel,
                    contentDescription = entry.name,
                    fallbackIcon = if (isGroup) R.drawable.ic_group else R.drawable.ic_contact,
                    showWorkBadge = false
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                if (isGroup) {
                    val groupEntry = entry as ChatEntry.GroupEntry
                    Text(
                        text = "${groupEntry.memberCount} Teilnehmer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f)
            )
        }
    }
}

