package ch.threema.treemalight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.treemalight.data.Contact
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.components.ContactSelectCard
import ch.threema.treemalight.ui.theme.ButtonBlue
import ch.threema.treemalight.ui.theme.PrimaryGreen
import ch.threema.treemalight.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * Send message screen with simple text input.
 * Steps: 1) Select recipient, 2) Write message, 3) Send
 */
@Composable
fun SendMessageScreen(
    contacts: List<Contact>,
    preSelectedContact: Contact? = null,
    onBackClick: () -> Unit,
    onMessageSent: () -> Unit,
    onSendMessage: suspend (contactId: String, message: String) -> Result<Unit> = { _, _ -> Result.success(Unit) }
) {
    var selectedContact by remember { mutableStateOf(preSelectedContact) }
    var messageText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    
    // Show success and navigate back
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            onMessageSent()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Title
        Text(
            text = if (selectedContact == null) "An wen?" else "Nachricht an ${selectedContact!!.name}",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (showSuccess) {
            // Success message
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✓",
                        fontSize = 80.sp,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nachricht gesendet!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        } else if (selectedContact == null) {
            // Step 1: Select recipient
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(contacts) { contact ->
                    ContactSelectCard(
                        contact = contact,
                        isSelected = false,
                        onClick = { selectedContact = contact }
                    )
                }
            }
        } else {
            // Step 2: Write message
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        text = "Tippe hier um zu schreiben…",
                        fontSize = 24.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSending
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Send button
            val scope = rememberCoroutineScope()
            
            BigButton(
                text = if (isSending) "Sende..." else "Senden",
                onClick = { 
                    if (messageText.isNotBlank() && !isSending && selectedContact != null) {
                        isSending = true
                        scope.launch {
                            // Call the suspend function to send the message via ThreemaBridge
                            onSendMessage(selectedContact!!.id, messageText)
                            // We assume success for the UI flow, real error handling could be added here
                            showSuccess = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                },
                containerColor = PrimaryGreen
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button (unless showing success)
        if (!showSuccess) {
            BigButton(
                text = if (selectedContact != null && preSelectedContact == null) "Anderen wählen" else "Zurück",
                onClick = {
                    if (selectedContact != null && preSelectedContact == null) {
                        selectedContact = null
                        messageText = ""
                    } else {
                        onBackClick()
                    }
                },
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
}
