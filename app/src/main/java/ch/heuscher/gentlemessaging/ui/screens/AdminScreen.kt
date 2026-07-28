package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.heuscher.gentlemessaging.data.GentlePreferences
import ch.heuscher.gentlemessaging.ui.components.BigButton
import ch.heuscher.gentlemessaging.ui.components.PinDialog
import ch.heuscher.gentlemessaging.ui.theme.*

/**
 * Admin Settings Screen - PIN protected, for caregivers only.
 *
 * Working features:
 * - Contact management (navigate to favorites screen)
 * - PIN change (dialog)
 * - Font size (radio picker)
 * - Quick replies editing
 * - Connection status display
 * - About section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    gentlePrefs: GentlePreferences,
    onExitAdmin: () -> Unit,
    onManageContacts: () -> Unit,
    onSyncContacts: () -> Unit,
    onOpenBackup: () -> Unit = {},
    onBackClick: () -> Unit
) {
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showQuickRepliesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚙️ Admin-Bereich",
                        fontWeight = FontWeight.Bold
                    )
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
                    containerColor = AdminYellow.copy(alpha = 0.2f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Admin mode warning
            Surface(
                color = AdminYellow.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔒 Admin-Modus aktiv\nÄnderungen hier betreffen die Bedienung der App.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Contact Management ──
            AdminSettingCard(
                title = "Kontakte verwalten",
                description = "Favoriten festlegen (⭐)",
                icon = Icons.Default.Person,
                onClick = onManageContacts
            )
            
            // ── Contact Sync ──
            AdminSettingCard(
                title = "Kontakte synchronisieren",
                description = "Telefonbuch mit Threema abgleichen",
                icon = Icons.Default.Refresh,
                onClick = onSyncContacts
            )

            // ── Backup Management ──
            AdminSettingCard(
                title = "Backup erstellen / verwalten",
                description = "Daten-Backup (ZIP) & Threema Safe",
                icon = Icons.Default.Share,
                onClick = onOpenBackup
            )

            // ── PIN Change ──
            AdminSettingCard(
                title = "PIN ändern",
                description = "Aktueller PIN: ${gentlePrefs.getAdminPin()}",
                icon = Icons.Default.Lock,
                onClick = { showPinChangeDialog = true }
            )

            // ── Font Size ──
            val fontSizeLabels = listOf("Mittel", "Groß", "Sehr groß")
            AdminSettingCard(
                title = "Schriftgröße",
                description = "Aktuell: ${fontSizeLabels[gentlePrefs.getFontSizeLevel()]}",
                icon = Icons.Default.Settings,
                onClick = { showFontSizeDialog = true }
            )

            // ── Quick Replies ──
            AdminSettingCard(
                title = "Schnellantworten",
                description = gentlePrefs.getQuickReplies().joinToString(", "),
                icon = Icons.Default.Email,
                onClick = { showQuickRepliesDialog = true }
            )

            // ── About ──
            AdminSettingCard(
                title = "Über gentle messages",
                description = "Version & Informationen",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exit admin mode
            BigButton(
                text = "Admin-Modus beenden",
                onClick = onExitAdmin,
                containerColor = ErrorRed,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                },
                contentDesc = "Admin Modus verlassen"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Dialogs ──

    if (showPinChangeDialog) {
        PinChangeDialog(
            currentPin = gentlePrefs.getAdminPin(),
            onDismiss = { showPinChangeDialog = false },
            onPinChanged = { newPin ->
                gentlePrefs.setAdminPin(newPin)
                showPinChangeDialog = false
            }
        )
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentLevel = gentlePrefs.getFontSizeLevel(),
            onDismiss = { showFontSizeDialog = false },
            onLevelSelected = { level ->
                gentlePrefs.setFontSizeLevel(level)
                showFontSizeDialog = false
            }
        )
    }

    if (showQuickRepliesDialog) {
        QuickRepliesDialog(
            currentReplies = gentlePrefs.getQuickReplies(),
            onDismiss = { showQuickRepliesDialog = false },
            onSave = { replies ->
                gentlePrefs.setQuickReplies(replies)
                showQuickRepliesDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("gentle messages", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Eine vereinfachte Messaging-App für Senioren.\n\n" +
                    "Basierend auf Threema.\n" +
                    "Entwickelt mit ❤️ für Barrierefreiheit.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", fontSize = 20.sp)
                }
            }
        )
    }
}

// ─── Admin Setting Card ─────────────────────────────────────────────────────

@Composable
private fun AdminSettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Gray
            )
        }
    }
}

// ─── PIN Change Dialog ──────────────────────────────────────────────────────

@Composable
private fun PinChangeDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0=verify old, 1=enter new, 2=confirm new
    var newPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val title = when (step) {
        0 -> "Aktuellen PIN eingeben"
        1 -> "Neuen PIN eingeben"
        else -> "Neuen PIN bestätigen"
    }

    PinDialog(
        onDismiss = onDismiss,
        onPinEntered = { enteredPin ->
            when (step) {
                0 -> {
                    if (enteredPin == currentPin) {
                        step = 1
                        isError = false
                    } else {
                        isError = true
                    }
                }
                1 -> {
                    newPin = enteredPin
                    step = 2
                    isError = false
                }
                2 -> {
                    if (enteredPin == newPin) {
                        onPinChanged(newPin)
                    } else {
                        isError = true
                        step = 1 // Go back to re-enter
                    }
                }
            }
        },
        isError = isError
    )
}

// ─── Font Size Dialog ───────────────────────────────────────────────────────

@Composable
private fun FontSizeDialog(
    currentLevel: Int,
    onDismiss: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    val options = listOf(
        Triple(GentlePreferences.FONT_SIZE_MEDIUM, "Mittel", "Aa"),
        Triple(GentlePreferences.FONT_SIZE_LARGE, "Groß", "Aa"),
        Triple(GentlePreferences.FONT_SIZE_VERY_LARGE, "Sehr groß", "Aa")
    )
    val fontSizes = listOf(18.sp, 24.sp, 32.sp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schriftgröße wählen", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, (level, label, preview) ->
                    Card(
                        onClick = { onLevelSelected(level) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (level == currentLevel)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = level == currentLevel,
                                onClick = { onLevelSelected(level) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$label: $preview",
                                fontSize = fontSizes[index],
                                fontWeight = if (level == currentLevel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", fontSize = 20.sp)
            }
        }
    )
}

// ─── Quick Replies Dialog ───────────────────────────────────────────────────

@Composable
private fun QuickRepliesDialog(
    currentReplies: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var replies by remember { mutableStateOf(currentReplies.toMutableList()) }
    var newReply by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schnellantworten bearbeiten", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.forEachIndexed { index, reply ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                replies = replies.toMutableList().also { it.removeAt(index) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Entfernen",
                                tint = ErrorRed
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newReply,
                        onValueChange = { newReply = it },
                        placeholder = { Text("Neue Antwort...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newReply.isNotBlank()) {
                                replies = replies.toMutableList().also { it.add(newReply.trim()) }
                                newReply = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hinzufügen",
                            tint = PrimaryGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(replies) }) {
                Text("Speichern", fontSize = 20.sp, color = PrimaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", fontSize = 20.sp)
            }
        }
    )
}
