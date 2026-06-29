package com.kdev.spendwise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// PREMIUM FINTECH COLOR PALETTE
// ============================================================================

// --- Light Theme Colors ---
private val PremiumLightPrimary = Color(0xFF0F172A) // Deep, authoritative Slate/Navy
private val PremiumLightOnPrimary = Color.White
private val PremiumLightPrimaryContainer = Color(0xFFE2E8F0)
private val PremiumLightOnPrimaryContainer = Color(0xFF0F172A)
private val PremiumLightSecondary = Color(0xFF2563EB) // Vibrant Royal Blue Accent
private val PremiumLightBackground = Color(0xFFF8FAFC) // Crisp, cool off-white for depth
private val PremiumLightSurface = Color(0xFFFFFFFF) // Pure white cards
private val PremiumLightSurfaceVariant = Color(0xFFF1F5F9) // Subtle card backgrounds
private val PremiumLightOnBackground = Color(0xFF0F172A) // High contrast text
private val PremiumLightOutline = Color(0xFFE2E8F0) // Soft borders
private val PremiumLightError = Color(0xFFEF4444) // Crisp Red for expenses/errors

// --- Dark Theme Colors ---
private val PremiumDarkPrimary = Color(0xFF38BDF8) // Bright, glowing blue for dark mode readability
private val PremiumDarkOnPrimary = Color(0xFF022C22)
private val PremiumDarkPrimaryContainer = Color(0xFF1E293B)
private val PremiumDarkOnPrimaryContainer = Color(0xFFE0E7FF)
private val PremiumDarkSecondary = Color(0xFF818CF8) // Soft Indigo Accent
private val PremiumDarkBackground = Color(0xFF0B0F19) // Ultra-deep premium dark (OLED friendly)
private val PremiumDarkSurface = Color(0xFF131B2F) // Slightly elevated for floating cards
private val PremiumDarkSurfaceVariant = Color(0xFF1E293B)
private val PremiumDarkOnBackground = Color(0xFFF8FAFC) // Crisp white text
private val PremiumDarkOutline = Color(0xFF334155) // Visible but subtle dark borders
private val PremiumDarkError = Color(0xFFF87171) // Soft red for dark mode

// ============================================================================
// COLOR SCHEMES
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary = PremiumLightPrimary,
    onPrimary = PremiumLightOnPrimary,
    primaryContainer = PremiumLightPrimaryContainer,
    onPrimaryContainer = PremiumLightOnPrimaryContainer,
    secondary = PremiumLightSecondary,
    onSecondary = Color.White,
    background = PremiumLightBackground,
    surface = PremiumLightSurface,
    surfaceVariant = PremiumLightSurfaceVariant,
    onBackground = PremiumLightOnBackground,
    onSurface = PremiumLightOnBackground,
    outline = PremiumLightOutline,
    error = PremiumLightError,
    surfaceTint = Color.Transparent // PREVENTS MUDDY MATERIAL 3 TINTING
)

private val DarkColorScheme = darkColorScheme(
    primary = PremiumDarkPrimary,
    onPrimary = PremiumDarkOnPrimary,
    primaryContainer = PremiumDarkPrimaryContainer,
    onPrimaryContainer = PremiumDarkOnPrimaryContainer,
    secondary = PremiumDarkSecondary,
    onSecondary = Color.Black,
    background = PremiumDarkBackground,
    surface = PremiumDarkSurface,
    surfaceVariant = PremiumDarkSurfaceVariant,
    onBackground = PremiumDarkOnBackground,
    onSurface = PremiumDarkOnBackground,
    outline = PremiumDarkOutline,
    error = PremiumDarkError,
    surfaceTint = Color.Transparent // PREVENTS MUDDY MATERIAL 3 TINTING
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to TRUE if you want Android 12+ Wallpaper colors, FALSE to enforce your Premium brand
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // [1] Color Status Bar (Top)
            window.statusBarColor = colorScheme.background.toArgb()

            // [2] Color Navigation Bar (Bottom) - Makes app feel immersive
            window.navigationBarColor = colorScheme.background.toArgb()

            // [3] Handle Icons for both bars
            val wic = WindowCompat.getInsetsController(window, view)
            wic.isAppearanceLightStatusBars = !darkTheme
            // If background is light, nav icons should be dark (and vice versa)
            wic.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Ensure you have your typography file set up here if needed
        content = content
    )
}