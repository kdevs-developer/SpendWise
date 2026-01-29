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

private val LightColorScheme = lightColorScheme(
    primary = BlueGreyMain,
    onPrimary = Color.White,
    primaryContainer = BlueGreyLight,
    onPrimaryContainer = BlueGreyMain,
    secondary = BlueGreyMain,
    onSecondary = Color.White,
    secondaryContainer = BlueGreyLight,
    background = SurfaceLight,
    surface = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = RedExpense
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueGreyLight,
    onPrimary = BlueGreyMain,
    primaryContainer = Color(0xFF374379),
    onPrimaryContainer = Color.White,
    secondary = BlueGreyLight,
    onSecondary = BlueGreyMain,
    background = BlueGreyDark,
    surface = Color(0xFF263238),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFEF9A9A)
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // set to TRUE if you want Android 12+ Wallpaper colors, FALSE to enforce your BlueGrey brand
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
        // typography = Typography, // Ensure you have this file if uncommented
        content = content
    )
}