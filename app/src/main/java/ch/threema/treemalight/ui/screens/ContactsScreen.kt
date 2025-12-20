package ch.threema.treemalight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.treemalight.data.Contact
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.components.ContactCard
import ch.threema.treemalight.ui.theme.ButtonBlue

/**
 * Contacts screen showing favorite people with large cards.
 */
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Title
        Text(
            text = "Meine Leute",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Contact list
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Kontakte vorhanden",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 28.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts) { contact ->
                    ContactCard(
                        contact = contact,
                        onMessageClick = { onContactClick(contact) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
