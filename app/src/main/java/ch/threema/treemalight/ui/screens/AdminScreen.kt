package ch.threema.treemalight.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.treemalight.data.Contact
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.theme.*

/**
 * Admin settings screen - only accessible with PIN.
 * Contains advanced features hidden from regular users.
 */
@Composable
fun AdminScreen(
    contacts: List<Contact>,
    onExitAdmin: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Admin header with yellow indicator
        Surface(
            color = AdminYellow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Einstellungen (Admin)",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contacts management
            item {
                AdminSettingCard(
                    title = "Kontakte verwalten",
                    description = "${contacts.size} Kontakte",
                    icon = Icons.Default.Person,
                    onClick = { /* TODO: Contact management */ }
                )
            }
            
            // PIN change
            item {
                AdminSettingCard(
                    title = "PIN ändern",
                    description = "Admin-Zugang schützen",
                    icon = Icons.Default.Lock,
                    onClick = { /* TODO: PIN change */ }
                )
            }
            
            // Font size
            item {
                AdminSettingCard(
                    title = "Schriftgröße",
                    description = "Aktuell: Sehr groß",
                    icon = Icons.Default.TextFields,
                    onClick = { /* TODO: Font size settings */ }
                )
            }
            
            // Message history
            item {
                AdminSettingCard(
                    title = "Nachrichtenverlauf",
                    description = "Alle Nachrichten anzeigen",
                    icon = Icons.Default.History,
                    onClick = { /* TODO: Message history */ }
                )
            }
            
            // Threema connection status
            item {
                AdminSettingCard(
                    title = "Threema Verbindung",
                    description = "Status prüfen",
                    icon = Icons.Default.Wifi,
                    onClick = { /* TODO: Connection status */ }
                )
            }
            
            // About
            item {
                AdminSettingCard(
                    title = "Über Treema Light",
                    description = "Version 1.0 (Threema-Backend)",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: About screen */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exit admin mode
        BigButton(
            text = "Admin beenden",
            onClick = onExitAdmin,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            },
            containerColor = ErrorRed
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Back to home
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

@Composable
private fun AdminSettingCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
