package ch.threema.treemalight.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.components.PinDialog
import ch.threema.treemalight.ui.theme.*
import androidx.compose.ui.platform.LocalContext

/**
 * Main home screen with 3 large buttons.
 * Admin mode activated by long-pressing the title.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onContactsClick: () -> Unit,
    onSendMessageClick: () -> Unit,
    onReadMessagesClick: () -> Unit,
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title - long press for admin mode
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = { },
                    onLongClick = { }
                )
                .semantics { contentDescription = "Treema Light - Halte gedrückt für Einstellungen" }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = { showPinDialog = true }
                )
            ) {
                Text(
                    text = "Treema Light",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                if (isAdminMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = AdminYellow,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "⚙️ Admin-Modus",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Main buttons - equally spaced
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Button 1: Meine Leute (Contacts)
            BigButton(
                text = "Meine Leute",
                onClick = onContactsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                containerColor = ButtonBlue,
                contentDesc = "Meine Leute - Zeige Kontakte"
            )
            
            // Button 2: Nachricht senden
            BigButton(
                text = "Nachricht senden",
                onClick = onSendMessageClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                containerColor = PrimaryGreen,
                contentDesc = "Nachricht senden - Neue Nachricht schreiben"
            )
            
            // Button 3: Nachrichten lesen
            BigButton(
                text = "Nachrichten lesen",
                onClick = onReadMessagesClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                containerColor = ButtonOrange,
                contentDesc = "Nachrichten lesen - Empfangene Nachrichten anzeigen"
            )
            
            // Button 4: Profil
            val context = LocalContext.current
            BigButton(
                text = "Profil",
                onClick = {
                    val intent = android.content.Intent(context, ch.threema.treemalight.ProfileActivity::class.java)
                    context.startActivity(intent)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                containerColor = ButtonPurple,
                contentDesc = "Profil - Profilbild und Name ändern"
            )
        }
        
        // Tiny admin hint (barely visible)
        if (!isAdminMode) {
            Text(
                text = "Titel lange drücken für Einstellungen",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
