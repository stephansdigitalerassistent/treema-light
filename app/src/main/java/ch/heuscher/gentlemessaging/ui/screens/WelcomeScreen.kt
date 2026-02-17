package ch.heuscher.gentlemessaging.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.heuscher.gentlemessaging.ui.components.BigButton
import ch.heuscher.gentlemessaging.ui.theme.PrimaryGreen

/**
 * Welcoming first-launch screen shown after setup is complete.
 * Reassures the elderly user that everything is ready.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Friendly heart icon
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Willkommen!",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Alles ist eingerichtet.\nDu kannst jetzt Nachrichten\nsenden und empfangen.",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        BigButton(
            text = "Los geht's! 🎉",
            onClick = onContinue,
            containerColor = PrimaryGreen,
            contentDesc = "Weiter zur App"
        )
    }
}
