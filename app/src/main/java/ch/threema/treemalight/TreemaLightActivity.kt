package ch.threema.treemalight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ch.threema.treemalight.data.Contact
import ch.threema.treemalight.data.Message
import ch.threema.treemalight.data.ThreemaBridge
import ch.threema.treemalight.ui.screens.*
import ch.threema.treemalight.ui.theme.TreemaLightTheme


/**
 * Main activity for Treema Light - the accessible Threema interface.
 * Uses ThreemaBridge to connect to real Threema services.
 */
class TreemaLightActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TreemaLightTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TreemaLightApp()
                }
            }
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data object Contacts : Screen()
    data class SendMessage(val preSelectedContact: Contact? = null) : Screen()
    data object ReadMessages : Screen()
    data object Admin : Screen()
}

@Composable
fun TreemaLightApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isAdminMode by remember { mutableStateOf(false) }
    
    // Initialize ThreemaBridge
    val bridge = remember { ThreemaBridge(context) }
    
    // Collect contacts from Threema
    val contacts by bridge.getContacts().collectAsState(initial = emptyList())
    
    // Collect messages from Threema
    val messages by bridge.getMessages().collectAsState(initial = emptyList())
    
    when (val screen = currentScreen) {
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
                    bridge.sendMessage(contactId, message)
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
