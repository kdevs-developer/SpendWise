package com.kdev.spendwise.data

import androidx.compose.ui.graphics.Color

data class CardTheme(val start: Color, val end: Color, val name: String)

val cardThemes = listOf(
    CardTheme(Color(0xFF1E3C72), Color(0xFF2A5298), "Classic Blue"),
    CardTheme(Color(0xFF00c6ff), Color(0xFF0072ff), "Azure Lane"),
    CardTheme(Color(0xFF1A2980), Color(0xFF26D0CE), "Aquamarine"),
    CardTheme(Color(0xFF11998e), Color(0xFF38ef7d), "Mint Green"),
    CardTheme(Color(0xFF56ab2f), Color(0xFFa8e063), "Lush Bamboo"),
    CardTheme(Color(0xFF8E2DE2), Color(0xFF4A00E0), "Royal Purple"),
    CardTheme(Color(0xFF834d9b), Color(0xFFd04ed6), "Mystic Purple"),
    CardTheme(Color(0xFF860029), Color(0xFFC31432), "Axis Burgundy"),
    CardTheme(Color(0xFFFF512F), Color(0xFFDD2476), "Sunset Orange"),
    CardTheme(Color(0xFFCC95C0), Color(0xFF7AA1D2), "Pastel Dream"),
    CardTheme(Color(0xFF000000), Color(0xFF434343), "Midnight Black")
)