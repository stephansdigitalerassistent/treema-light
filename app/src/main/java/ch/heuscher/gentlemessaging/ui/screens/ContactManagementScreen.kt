package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.heuscher.gentlemessaging.data.ThreemaBridge
import ch.heuscher.gentlemessaging.data.GentlePreferences
import ch.threema.app.R
import ch.threema.app.compose.common.AvatarAsync

/**
 * Contact Management screen for the admin.
 * Shows all contacts/groups with a star toggle to mark favorites.
 *
 * WHY: Elderly users should only see a curated set of contacts.
 * The caregiver stars the relevant ones here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactManagementScreen(
    chatEntries: List<ThreemaBridge.ChatEntry>,
    gentlePrefs: GentlePreferences,
    onBackClick: () -> Unit
) {
    // Mutable state to track favorites for instant UI updates
    var favoriteIds by remember { mutableStateOf(gentlePrefs.getFavoriteIds()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontakte verwalten") },
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
        ) {
            // Explanation text
            Text(
                text = "Tippe auf den Stern ⭐, um Kontakte als Favoriten zu markieren. Favoriten werden oben in der Kontaktliste angezeigt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatEntries) { entry ->
                    val isFav = favoriteIds.contains(entry.id)
                    val isGroup = entry is ThreemaBridge.ChatEntry.GroupEntry

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
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

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name and type
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isGroup) "Gruppe" else "Kontakt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Star toggle
                            IconButton(
                                onClick = {
                                    gentlePrefs.toggleFavorite(entry.id)
                                    favoriteIds = gentlePrefs.getFavoriteIds()
                                },
                                modifier = Modifier.size(56.dp) // Large touch target
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = if (isFav) "Favorit entfernen" else "Als Favorit markieren",
                                    tint = if (isFav) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
