package ch.heuscher.gentlemessaging

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.threema.app.ThreemaApplication
import ch.threema.app.listeners.MessageListener
import ch.threema.app.managers.ListenerManager
import ch.threema.app.passphrase.PassphraseUnlockActivity
import ch.threema.app.services.ContactService
import ch.threema.app.services.license.LicenseService
import ch.heuscher.gentlemessaging.data.Contact
import ch.heuscher.gentlemessaging.data.GentlePreferences
import ch.heuscher.gentlemessaging.data.Message
import ch.heuscher.gentlemessaging.data.ThreemaBridge
import ch.heuscher.gentlemessaging.notifications.GentleNotificationHelper
import ch.heuscher.gentlemessaging.ui.screens.*
import ch.heuscher.gentlemessaging.ui.theme.TreemaLightTheme
import ch.threema.localcrypto.MasterKeyManager
import ch.threema.storage.models.AbstractMessageModel
import ch.threema.storage.models.MessageModel
import ch.threema.storage.models.MessageType
import ch.threema.app.services.UserService
import org.koin.android.ext.android.inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

/**
 * Main activity for gentle messaging - the accessible Threema interface.
 * Uses ThreemaBridge to connect to real Threema services.
 */
class TreemaLightActivity : ComponentActivity() {

    private val masterKeyManager: MasterKeyManager by inject()
    private val contactService: ContactService by inject()
    
    // Notification listener — registered in onCreate, removed in onDestroy
    private val notificationMessageListener = object : MessageListener {
        override fun onNew(newMessage: AbstractMessageModel) {
            if (newMessage is MessageModel && !newMessage.isOutbox) {
                // Skip internal status types — no notification needed
                when (newMessage.type) {
                    MessageType.STATUS, MessageType.VOIP_STATUS, MessageType.DATE_SEPARATOR,
                    MessageType.GROUP_CALL_STATUS, MessageType.FORWARD_SECURITY_STATUS,
                    MessageType.GROUP_STATUS -> return
                    else -> { /* continue */ }
                }

                val senderId = newMessage.identity?.toString() ?: return
                val senderName = contactService.getByIdentity(senderId)?.let { contact ->
                    val first = contact.firstName
                    val last = contact.lastName
                    when {
                        !first.isNullOrBlank() && !last.isNullOrBlank() -> "$first $last"
                        !first.isNullOrBlank() -> first
                        !last.isNullOrBlank() -> last
                        else -> senderId
                    }
                } ?: senderId

                // Use readable text instead of raw body for non-text types
                val body = when (newMessage.type) {
                    MessageType.TEXT -> newMessage.body ?: "Neue Nachricht"
                    MessageType.IMAGE -> "📷 Bild"
                    MessageType.VIDEO -> "🎬 Video"
                    MessageType.VOICEMESSAGE -> "🎤 Sprachnachricht"
                    MessageType.FILE -> "📎 Datei"
                    MessageType.LOCATION -> "📍 Standort"
                    MessageType.BALLOT -> "📊 Abstimmung"
                    MessageType.CONTACT -> "👤 Kontakt"
                    else -> "Neue Nachricht"
                }
                GentleNotificationHelper.showMessageNotification(
                    this@TreemaLightActivity, senderName, body, senderId
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize notification channel
        GentleNotificationHelper.createNotificationChannel(this)
        
        // Register notification listener
        ListenerManager.messageListeners.add(notificationMessageListener)
        
        // Ensure identity exists before proceeding
        // Check license status first
        val serviceManager = ThreemaApplication.getServiceManager()
        val licenseService = serviceManager?.licenseService
        val hasLicense = licenseService?.hasCredentials() == true && licenseService.isLicensed()
        val userService = serviceManager?.userService
        
        // Ensure credentials are loaded into UserService (crucial for identity creation)
        if (hasLicense && userService != null) {
            val credentials = licenseService?.loadCredentials()
            if (credentials != null) {
                 userService.setCredentials(credentials)
            }
        }
        
        // LOGGING: Check status
        android.util.Log.d("TreemaLight", "onCreate: hasLicense=$hasLicense, hasIdentity=${userService?.hasIdentity()}")


        
        setContent {
            val fontSizeLevel = remember { GentlePreferences.getInstance(applicationContext).getFontSizeLevel() }
            TreemaLightTheme(fontSizeLevel = fontSizeLevel) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check if unlocked state changes
                    var isUnlocked by remember { mutableStateOf(isKeyUnlocked()) }

                    // Resume check
                    DisposableEffect(Unit) {
                        val listener = androidx.core.util.Consumer<Intent> { 
                            isUnlocked = isKeyUnlocked()
                        }
                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }
                    
                    // Periodically check unlocking in lifecycle onResume
                    LifecycleResumeEffect(Unit) {
                        if (!isKeyUnlocked()) {
                            // If locked, launch unlock activity
                            val intent = Intent(this@TreemaLightActivity, PassphraseUnlockActivity::class.java)
                            startActivity(intent)
                        } else {
                            isUnlocked = true
                        }
                    }

                    if (isUnlocked) {
                        TreemaLightApp()
                    } else {
                        // Loading / Locked placeholder
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Bitte entsperren...")
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        ListenerManager.messageListeners.remove(notificationMessageListener)
        super.onDestroy()
    }
    
    // Check if key is usable
    private fun isKeyUnlocked(): Boolean {
        return !masterKeyManager.isLocked()
    }
}

// Helper for Lifecycle effects
@Composable
fun LifecycleResumeEffect(key1: Any?, onResume: () -> Unit) {
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(key1, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}


sealed class Screen {
    data object LicenseEntry : Screen()
    data object Welcome : Screen()
    data object Home : Screen()
    data class Chat(val entry: ThreemaBridge.ChatEntry) : Screen()
    data class Profile(val entry: ThreemaBridge.ChatEntry, val returnToChat: Boolean = true) : Screen()
    data object Admin : Screen()
    data object ContactManagement : Screen()
}


@Composable
fun TreemaLightApp() {
    val context = LocalContext.current
    
    // Check license status
    val serviceManager = ThreemaApplication.getServiceManager()
    val userService = serviceManager?.userService
    val hasValidLicense = remember {
        serviceManager?.licenseService?.let { license ->
            license.hasCredentials() && license.isLicensed()
        } ?: false
    }
    
    // Start on LicenseEntry if not licensed OR if identity is missing, else Home
    // This ensures that even if we have a "ghost" license, we force the user through the license screen
    // if they haven't set up an identity yet.
    val hasIdentity = remember { userService?.hasIdentity() == true }
    
    var currentScreen by remember { 
        mutableStateOf<Screen>(if (hasValidLicense && hasIdentity) Screen.Home else Screen.LicenseEntry) 
    }
    var isAdminMode by remember { mutableStateOf(false) }

    // GentlePreferences for all settings
    val gentlePrefs = remember { GentlePreferences.getInstance(context) }
    
    // Initialize ThreemaBridge (only after licensed)
    val bridge = remember(currentScreen) { 
        if (currentScreen != Screen.LicenseEntry && currentScreen != Screen.Welcome) ThreemaBridge(context) else null
    }
    
    // Collect unified chat entries (Contacts + Groups)
    val chatEntries by (bridge?.getChatEntries() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
        
    // Collect messages if in Chat Screen
    val activeChatEntry = (currentScreen as? Screen.Chat)?.entry
    val messages by (if (activeChatEntry != null && bridge != null) {
        val isGroup = activeChatEntry is ThreemaBridge.ChatEntry.GroupEntry
        bridge.getMessages(activeChatEntry.id, isGroup)
    } else {
        kotlinx.coroutines.flow.flowOf(emptyList())
    }).collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    
    when (val screen = currentScreen) {
        is Screen.LicenseEntry -> {
            LicenseEntryScreen(
                onLicenseValid = { isEasterEgg ->
                    if (isEasterEgg && userService?.hasIdentity() == false) {
                        // Easter egg: restore identity A62B5XJ3 from Threema Safe
                        scope.launch {
                            try {
                                val sm = ThreemaApplication.getServiceManager()!!
                                val safeService = sm.getThreemaSafeService()
                                val safePassword = "gentleMessages"
                                val safeId = "A62B5XJ3"
                                val serverInfo = ch.threema.app.threemasafe.ThreemaSafeServerInfo()
                                
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    // Restore identity + contacts from Threema Safe backup
                                    safeService.restoreBackup(safeId, safePassword, serverInfo)
                                    
                                    // Re-enable Threema Safe with the same password
                                    val masterKey = safeService.deriveMasterKey(safePassword, safeId)
                                    if (masterKey != null) {
                                        safeService.storeMasterKey(masterKey)
                                        sm.preferenceService.setThreemaSafeServerInfo(serverInfo)
                                        safeService.setEnabled(true)
                                        safeService.uploadNow(true)
                                    }
                                }
                                
                                android.widget.Toast.makeText(context, "🎉 Identität wiederhergestellt!", android.widget.Toast.LENGTH_LONG).show()
                                
                                if (!gentlePrefs.hasSeenWelcome()) {
                                    currentScreen = Screen.Welcome
                                } else {
                                    currentScreen = Screen.Home
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("TreemaLight", "Easter egg restore failed", e)
                                android.widget.Toast.makeText(context, "Wiederherstellung fehlgeschlagen: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                // Fallback: launch wizard
                                val intent = Intent(context, ch.threema.app.activities.wizard.WizardStartActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                            }
                        }
                    } else if (userService?.hasIdentity() == false) {
                         val intent = Intent(context, ch.threema.app.activities.wizard.WizardStartActivity::class.java)
                         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                         context.startActivity(intent)
                    } else if (!gentlePrefs.hasSeenWelcome()) {
                        currentScreen = Screen.Welcome
                    } else {
                        currentScreen = Screen.Home
                    }
                }
            )
        }

        is Screen.Welcome -> {
            WelcomeScreen(
                onContinue = {
                    gentlePrefs.setHasSeenWelcome(true)
                    currentScreen = Screen.Home
                }
            )
        }
        
        is Screen.Home -> {
            HomeScreen(
                chatEntries = chatEntries,
                onChatClick = { entry ->
                    // Cancel notification for this chat when opening it
                    GentleNotificationHelper.cancelNotification(context, entry.id)
                    currentScreen = Screen.Chat(entry)
                },
                onAdminClick = { 
                    isAdminMode = true
                    currentScreen = Screen.Admin 
                },
                isAdminMode = isAdminMode,
                adminPin = gentlePrefs.getAdminPin()
            )
        }
        
        is Screen.Chat -> {
            val entry = screen.entry
            val isGroup = entry is ThreemaBridge.ChatEntry.GroupEntry
            
            ChatScreen(
                chatEntry = entry,
                messages = messages,
                quickReplies = gentlePrefs.getQuickReplies(),
                onSendMessage = { text ->
                    kotlinx.coroutines.GlobalScope.launch {
                        bridge?.sendMessage(entry.id, text, isGroup)
                    }
                },
                onBackClick = { currentScreen = Screen.Home },
                onAvatarClick = { currentScreen = Screen.Profile(entry, returnToChat = true) }
            )
        }
        
        is Screen.Profile -> {
            ProfileScreen(
                chatEntry = screen.entry,
                onBackClick = { 
                    currentScreen = if (screen.returnToChat) Screen.Chat(screen.entry) else Screen.Home
                }
            )
        }
        
        is Screen.Admin -> {
            AdminScreen(
                gentlePrefs = gentlePrefs,
                onExitAdmin = {
                    isAdminMode = false
                    currentScreen = Screen.Home
                },
                onManageContacts = {
                    currentScreen = Screen.ContactManagement
                },
                onSyncContacts = {
                    scope.launch {
                        bridge?.syncContacts()
                        android.widget.Toast.makeText(context, "Kontakte werden synchronisiert...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenBackup = {
                    val intent = Intent(context, ch.threema.app.activities.BackupAdminActivity::class.java)
                    context.startActivity(intent)
                },
                onBackClick = { currentScreen = Screen.Home }
            )
        }

        is Screen.ContactManagement -> {
            ContactManagementScreen(
                chatEntries = chatEntries,
                gentlePrefs = gentlePrefs,
                onBackClick = { currentScreen = Screen.Admin }
            )
        }
    }
}

