package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
        // Simple Header — long-press anywhere in header to open admin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showPinDialog = true },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
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

        // Split into favorites and others
        val favorites = chatEntries.filter { entry ->
            when (entry) {
                is ChatEntry.ContactEntry -> entry.isFavorite
                is ChatEntry.GroupEntry -> entry.isFavorite
            }
        }
        val others = chatEntries.filter { entry ->
            when (entry) {
                is ChatEntry.ContactEntry -> !entry.isFavorite
                is ChatEntry.GroupEntry -> !entry.isFavorite
            }
        }

        // Combined List: Favorites first, then divider, then the rest
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Favorites section
            if (favorites.isNotEmpty()) {
                items(favorites) { entry ->
                    ChatListItem(
                        entry = entry,
                        onClick = { onChatClick(entry) },
                        showStar = true
                    )
                }
            } else {
                item {
                    Text(
                        text = "Noch keine Favoriten ⭐\nBitte einen Betreuer bitten, Kontakte einzurichten.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            // Divider between favorites and others
            if (others.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  Weitere Kontakte  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                items(others) { entry ->
                    ChatListItem(
                        entry = entry,
                        onClick = { onChatClick(entry) },
                        showStar = false
                    )
                }
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
    onClick: () -> Unit,
    showStar: Boolean = false
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
        modifier = Modifier.fillMaxWidth().height(100.dp),
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
            // Star indicator for favorites
            if (showStar) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorit",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Avatar - using real Threema avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
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

