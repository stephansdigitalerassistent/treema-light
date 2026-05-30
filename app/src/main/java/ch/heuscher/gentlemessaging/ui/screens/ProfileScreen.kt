package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.heuscher.gentlemessaging.data.ThreemaBridge
import ch.threema.app.R
import ch.threema.app.compose.common.AvatarAsync

/**
 * Simple Profile/Group Info Screen.
 * Shows avatar, name, and basic info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    chatEntry: ThreemaBridge.ChatEntry,
    onBackClick: () -> Unit
) {
    val isGroup = chatEntry is ThreemaBridge.ChatEntry.GroupEntry

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGroup) "Gruppen-Info" else "Kontakt-Info") },
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
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Large Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ) {
                AvatarAsync(
                    modifier = Modifier.fillMaxSize(),
                    receiverModel = chatEntry.receiverModel,
                    contentDescription = chatEntry.name,
                    fallbackIcon = if (isGroup) R.drawable.ic_group else R.drawable.ic_contact,
                    showWorkBadge = false
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name
            Text(
                text = chatEntry.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ID or member count
            if (isGroup) {
                val groupEntry = chatEntry as ThreemaBridge.ChatEntry.GroupEntry
                Text(
                    text = "${groupEntry.memberCount} Teilnehmer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val contactEntry = chatEntry as ThreemaBridge.ChatEntry.ContactEntry
                Text(
                    text = "Threema ID: ${contactEntry.id}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Minimal content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isGroup) "Gruppe erstellt in Threema" else "Kontakt aus Threema",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
