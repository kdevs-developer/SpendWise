package com.kdev.spendwise.data

import androidx.compose.ui.graphics.Color

data class CardTheme(val start: Color, val end: Color, val name: String)

val cardThemes = listOf(
    // --- Premium General Themes ---
    CardTheme(Color(0xFF1E3C72), Color(0xFF2A5298), "Classic Blue"),
    CardTheme(Color(0xFF00c6ff), Color(0xFF0072ff), "Azure Lane"),
    CardTheme(Color(0xFF1A2980), Color(0xFF26D0CE), "Aquamarine"),
    CardTheme(Color(0xFF11998e), Color(0xFF38ef7d), "Mint Green"),
    CardTheme(Color(0xFF56ab2f), Color(0xFFa8e063), "Lush Bamboo"),
    CardTheme(Color(0xFF8E2DE2), Color(0xFF4A00E0), "Royal Purple"),
    CardTheme(Color(0xFF834d9b), Color(0xFFd04ed6), "Mystic Purple"),
    CardTheme(Color(0xFFFF512F), Color(0xFFDD2476), "Sunset Orange"),
    CardTheme(Color(0xFFCC95C0), Color(0xFF7AA1D2), "Pastel Dream"),
    CardTheme(Color(0xFF000000), Color(0xFF434343), "Midnight Black"),

    // --- Premium Metallic Tiers ---
    CardTheme(Color(0xFF1F1C2C), Color(0xFF928DAB), "Titanium Silver"),
    CardTheme(Color(0xFFBF953F), Color(0xFFFCF6BA), "Gold Prestige"),

    // --- Indian Bank Brand Themes ---
    CardTheme(Color(0xFF002244), Color(0xFF004B8D), "HDFC Navy"),
    CardTheme(Color(0xFF003366), Color(0xFF00B2FF), "SBI Blue"),
    CardTheme(Color(0xFF8A1538), Color(0xFFF05A28), "ICICI Coral"),
    CardTheme(Color(0xFF860029), Color(0xFFD61841), "Axis Burgundy"),
    CardTheme(Color(0xFF5B0013), Color(0xFFED1C24), "Kotak Signature"),
    CardTheme(Color(0xFFCC4400), Color(0xFFFF7A00), "BoB Orange"),
    CardTheme(Color(0xFF790015), Color(0xFFD4AF37), "PNB Heritage"),
    CardTheme(Color(0xFF003876), Color(0xFF0072CE), "Yes Bank Azure"),
    CardTheme(Color(0xFF4A1016), Color(0xFF901A1E), "IndusInd Maroon"),
    CardTheme(Color(0xFF003B73), Color(0xFF0074B7), "Canara Ocean"),
    CardTheme(Color(0xFF005E42), Color(0xFF00A546), "SCB Wealth Green"),
    CardTheme(Color(0xFF141E30), Color(0xFF243B55), "IDFC First Dark"),
    CardTheme(Color(0xFFC33764), Color(0xFF1D2671), "Union Bank Blend")
)