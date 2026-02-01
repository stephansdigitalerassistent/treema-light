package ch.threema.treemalight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import ch.threema.app.passphrase.PassphraseUnlockActivity
import ch.threema.app.services.license.LicenseService
import ch.threema.treemalight.data.Contact
import ch.threema.treemalight.data.Message
import ch.threema.treemalight.data.ThreemaBridge
import ch.threema.treemalight.ui.screens.*
import ch.threema.treemalight.ui.theme.TreemaLightTheme
import ch.threema.localcrypto.MasterKeyManager
import org.koin.android.ext.android.inject

/**
 * Main activity for Treema Light - the accessible Threema interface.
 * Uses ThreemaBridge to connect to real Threema services.
 */
class TreemaLightActivity : ComponentActivity() {

    private val masterKeyManager: MasterKeyManager by inject()
    private val userService: ch.threema.app.services.UserService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure identity exists before proceeding
        if (!userService.hasIdentity()) {
            val intent = Intent(this, ch.threema.app.activities.wizard.WizardStartActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContent {
            TreemaLightTheme {
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
    data object Home : Screen()
    data object Contacts : Screen()
    data class SendMessage(val preSelectedContact: Contact? = null) : Screen()
    data object ReadMessages : Screen()
    data object Admin : Screen()
}

@Composable
fun TreemaLightApp() {
    val context = LocalContext.current
    
    // Check license status
    val serviceManager = ThreemaApplication.getServiceManager()
    val hasValidLicense = remember {
        serviceManager?.licenseService?.let { license ->
            license.hasCredentials() && license.isLicensed()
        } ?: false
    }
    
    // Start on LicenseEntry if not licensed, else Home
    var currentScreen by remember { 
        mutableStateOf<Screen>(if (hasValidLicense) Screen.Home else Screen.LicenseEntry) 
    }
    var isAdminMode by remember { mutableStateOf(false) }
    
    // Initialize ThreemaBridge (only after licensed)
    val bridge = remember(currentScreen) { 
        if (currentScreen != Screen.LicenseEntry) ThreemaBridge(context) else null
    }
    
    // Collect contacts from Threema
    val contacts by (bridge?.getContacts() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    
    // Collect messages from Threema
    val messages by (bridge?.getMessages() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    
    when (val screen = currentScreen) {
        is Screen.LicenseEntry -> {
            LicenseEntryScreen(
                onLicenseValid = {
                    currentScreen = Screen.Home
                }
            )
        }
        
        is Screen.Home -> {
            HomeScreen(
                onContactsClick = { currentScreen = Screen.Contacts },
                onSendMessageClick = { currentScreen = Screen.SendMessage() },
                onReadMessagesClick = { currentScreen = Screen.ReadMessages },
                onAdminClick = { 
                    isAdminMode = true
                    currentScreen = Screen.Admin 
                },
                isAdminMode = isAdminMode
            )
        }
        
        is Screen.Contacts -> {
            ContactsScreen(
                contacts = contacts,
                onContactClick = { contact ->
                    currentScreen = Screen.SendMessage(contact)
                },
                onBackClick = { currentScreen = Screen.Home }
            )
        }
        
        is Screen.SendMessage -> {
            SendMessageScreen(
                contacts = contacts,
                preSelectedContact = screen.preSelectedContact,
                onBackClick = { currentScreen = Screen.Home },
                onMessageSent = { currentScreen = Screen.Home },
                onSendMessage = { contactId, message ->
                    bridge?.sendMessage(contactId, message) ?: Result.success(Unit)
                }
            )
        }
        
        is Screen.ReadMessages -> {
            ReadMessagesScreen(
                messages = messages,
                onBackClick = { currentScreen = Screen.Home }
            )
        }
        
        is Screen.Admin -> {
            AdminScreen(
                contacts = contacts,
                onExitAdmin = {
                    isAdminMode = false
                    currentScreen = Screen.Home
                },
                onBackClick = { currentScreen = Screen.Home }
            )
        }
    }
}
