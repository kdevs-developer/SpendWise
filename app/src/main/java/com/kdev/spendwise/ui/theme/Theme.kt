package com.kdev.spendwise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BlueGreyMain,
    primaryContainer = BlueGreyMain,
    onPrimaryContainer = Color.White,
    secondary = BlueGreyLight,
    background = Color.White,
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueGreyLight,
    primaryContainer = Color(0xFF374379),
    onPrimaryContainer = Color.White,
    background = BlueGreyDark,
    surface = Color(0xFF263238)
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}