package ch.heuscher.gentlemessaging.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Accessible typography with 3 scaling levels.
 * Level 0 (Medium): standard-ish sizes, still larger than system default
 * Level 1 (Large): the default — comfortable for most elderly users
 * Level 2 (Very Large): maximum accessibility
 */
fun scaledTypography(fontSizeLevel: Int = 1): Typography {
    val scale = when (fontSizeLevel) {
        0 -> 0.8f   // Medium
        1 -> 1.0f   // Large (default)
        else -> 1.2f // Very Large
    }

    return Typography(
        // Giant title
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (48 * scale).sp,
            lineHeight = (56 * scale).sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (40 * scale).sp,
            lineHeight = (48 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Large headers
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * scale).sp,
            lineHeight = (44 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Button text
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Normal text (minimum for accessibility)
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Secondary text
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (20 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Small text (admin mode only) — not scaled, it's already small by design
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )
    )
}

// Keep backward compatibility
val AccessibleTypography = scaledTypography(1)
