package ch.threema.treemalight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.treemalight.data.Message
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.theme.ButtonBlue
import ch.threema.treemalight.ui.theme.ButtonOrange
import ch.threema.treemalight.ui.theme.ButtonPurple
import java.text.SimpleDateFormat
import java.util.*

/**
 * Read messages screen - one message at a time with large text.
 */
@Composable
fun ReadMessagesScreen(
    messages: List<Message>,
    onBackClick: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val unreadMessages = messages.filter { !it.isOutgoing }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Title with count
        Text(
            text = "Nachrichten",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (unreadMessages.isNotEmpty()) {
            Text(
                text = "${currentIndex + 1} von ${unreadMessages.size}",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Message content
        if (unreadMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📭",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keine neuen Nachrichten",
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val message = unreadMessages[currentIndex]
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Sender info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message.senderName.first().uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = message.senderName,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTime(message.timestamp),
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Message text - LARGE
                    Text(
                        text = message.content,
                        fontSize = 32.sp,
                        lineHeight = 44.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Read aloud button
                    OutlinedButton(
                        onClick = { /* TODO: Text-to-speech */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Vorlesen",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Vorlesen",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation - Previous / Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigButton(
                    text = "Vorherige",
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    containerColor = if (currentIndex > 0) ButtonPurple else ButtonPurple.copy(alpha = 0.3f)
                )
                
                BigButton(
                    text = "Nächste",
                    onClick = { if (currentIndex < unreadMessages.size - 1) currentIndex++ },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    containerColor = if (currentIndex < unreadMessages.size - 1) ButtonOrange else ButtonOrange.copy(alpha = 0.3f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        BigButton(
            text = "Zurück",
            onClick = onBackClick,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            },
            containerColor = ButtonBlue
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 3600000 -> "Gerade eben"
        diff < 86400000 -> "Heute"
        diff < 172800000 -> "Gestern"
        else -> {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            sdf.format(Date(timestamp))
        }
    }
}
