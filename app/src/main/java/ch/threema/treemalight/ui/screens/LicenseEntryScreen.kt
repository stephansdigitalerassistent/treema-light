package ch.threema.treemalight.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.threema.app.ThreemaApplication
import ch.threema.domain.models.SerialCredentials
import ch.threema.treemalight.ui.components.BigButton
import ch.threema.treemalight.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * License entry screen shown when no valid license is present.
 * Easter egg: Tap the logo 5 times to auto-fill the license key.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LicenseEntryScreen(
    onLicenseValid: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // License key parts
    var keyPart1 by remember { mutableStateOf("") }
    var keyPart2 by remember { mutableStateOf("") }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Easter egg tap counter
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    // Focus requesters for auto-moving between fields
    val focusPart1 = remember { FocusRequester() }
    val focusPart2 = remember { FocusRequester() }
    
    // Hidden license key for easter egg
    val hiddenKey1 = "G55TH"
    val hiddenKey2 = "RBV2S"
    
    // Validate license function
    fun validateLicense() {
        val fullKey = "${keyPart1.uppercase()}-${keyPart2.uppercase()}"
        
        if (keyPart1.length != 5 || keyPart2.length != 5) {
            errorMessage = "Bitte vollständigen Schlüssel eingeben (XXXXX-XXXXX)"
            return
        }
        
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val serviceManager = ThreemaApplication.getServiceManager()
                if (serviceManager == null) {
                    errorMessage = "App nicht vollständig initialisiert"
                    isLoading = false
                    return@launch
                }
                
                val licenseService = serviceManager.licenseService
                val credentials = SerialCredentials(fullKey)
                
                val result = withContext(Dispatchers.IO) {
                    licenseService.validate(credentials)
                }
                
                isLoading = false
                
                if (result == null) {
                    // Success - null means no error
                    Toast.makeText(context, "Lizenz aktiviert! ✓", Toast.LENGTH_LONG).show()
                    onLicenseValid()
                } else {
                    errorMessage = result
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Fehler: ${e.message ?: "Unbekannter Fehler"}"
            }
        }
    }
    
    // Easter egg handler
    fun handleLogoTap() {
        val now = System.currentTimeMillis()
        
        // Reset if too slow (> 2 seconds between taps)
        if (now - lastTapTime > 2000) {
            tapCount = 1
        } else {
            tapCount++
        }
        lastTapTime = now
        
        when {
            tapCount >= 5 -> {
                // Easter egg activated!
                keyPart1 = hiddenKey1
                keyPart2 = hiddenKey2
                tapCount = 0
                Toast.makeText(context, "🔓 Lizenz erkannt", Toast.LENGTH_SHORT).show()
            }
            tapCount >= 3 -> {
                val remaining = 5 - tapCount
                Toast.makeText(context, "Noch $remaining...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Title - Easter egg trigger
        Box(
            modifier = Modifier.combinedClickable(
                onClick = { handleLogoTap() },
                onLongClick = { }
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = PrimaryGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Treema Light",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Explanation text
        Text(
            text = "Bitte gib deinen Lizenzschlüssel ein",
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // License key input fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyPart1,
                onValueChange = { value ->
                    val filtered = value.filter { it.isLetterOrDigit() }.take(5).uppercase()
                    keyPart1 = filtered
                    if (filtered.length == 5) {
                        focusPart2.requestFocus()
                    }
                },
                modifier = Modifier
                    .width(140.dp)
                    .focusRequester(focusPart1),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                placeholder = {
                    Text(
                        text = "XXXXX",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusPart2.requestFocus() }
                ),
                enabled = !isLoading
            )
            
            Text(
                text = " - ",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = keyPart2,
                onValueChange = { value ->
                    val filtered = value.filter { it.isLetterOrDigit() }.take(5).uppercase()
                    keyPart2 = filtered
                },
                modifier = Modifier
                    .width(140.dp)
                    .focusRequester(focusPart2),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                placeholder = {
                    Text(
                        text = "XXXXX",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        validateLicense()
                    }
                ),
                enabled = !isLoading
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error message
        if (errorMessage != null) {
            Surface(
                color = ErrorRed.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Validate button
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = PrimaryGreen
            )
        } else {
            BigButton(
                text = "Lizenz prüfen",
                onClick = { validateLicense() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                },
                containerColor = PrimaryGreen,
                contentDesc = "Lizenz prüfen"
            )
        }
    }
}
