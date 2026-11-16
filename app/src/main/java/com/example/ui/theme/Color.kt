package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Joyful "Happy Hospitality" Palette Tokens
val MangoPrimary = Color(0xFFFF6B35)
val MangoSecondary = Color(0xFF00B894)
val MangoTertiary = Color(0xFFFFB703)
val CreamSurface = Color(0xFFFFFBF5)
val WarmCardLight = Color(0xFFFFFFFF)
val WarmContainerLight = Color(0xFFFFF1E6)

val MintPrimary = Color(0xFF00A878)
val MintSecondary = Color(0xFFFF9F1C)
val MintTertiary = Color(0xFF2EC4B6)

val BerryPrimary = Color(0xFFE84393)
val BerrySecondary = Color(0xFF00CEC9)
val BerryTertiary = Color(0xFFFDCB6E)

val SaffronPrimary = Color(0xFFD97706)
val SaffronSecondary = Color(0xFF2563EB)
val SaffronTertiary = Color(0xFF10B981)

val ProfitEmerald = Color(0xFF0E9F6E)
val LossCoral = Color(0xFFE02424)
val ZomatoRed = Color(0xFFE23744)
val SwiggyOrange = Color(0xFFFC8019)
val HappyAmber = Color(0xFFF59E0B)

fun parseHexColorOrNull(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val value = clean.toLong(16)
        if (clean.length == 6) Color(0xFF000000L or value) else Color(value)
    } catch (_: Exception) {
        null
    }
}
