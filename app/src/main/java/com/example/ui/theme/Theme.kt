package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemePreset

val MaraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MaraManagerTheme(
    themePresetKey: String = ThemePreset.SUNNY_MANGO.key,
    customAccentHex: String = "",
    customTextHex: String = "",
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val preset = ThemePreset.fromKey(themePresetKey)
    val basePrimary = parseHexColorOrNull(customAccentHex) ?: when (preset) {
        ThemePreset.SUNNY_MANGO -> MangoPrimary
        ThemePreset.MINT_FIESTA -> MintPrimary
        ThemePreset.BERRY_BLOSSOM -> BerryPrimary
        ThemePreset.ROYAL_SAFFRON -> SaffronPrimary
    }
    val baseSecondary = when (preset) {
        ThemePreset.SUNNY_MANGO -> MangoSecondary
        ThemePreset.MINT_FIESTA -> MintSecondary
        ThemePreset.BERRY_BLOSSOM -> BerrySecondary
        ThemePreset.ROYAL_SAFFRON -> SaffronSecondary
    }
    val baseTertiary = when (preset) {
        ThemePreset.SUNNY_MANGO -> MangoTertiary
        ThemePreset.MINT_FIESTA -> MintTertiary
        ThemePreset.BERRY_BLOSSOM -> BerryTertiary
        ThemePreset.ROYAL_SAFFRON -> SaffronTertiary
    }
    val customTextColor = parseHexColorOrNull(customTextHex)

    val colorScheme = if (darkTheme) {
        val onBg = customTextColor ?: Color(0xFFFDF6EE)
        darkColorScheme(
            primary = basePrimary,
            onPrimary = Color.White,
            primaryContainer = basePrimary.copy(alpha = 0.24f),
            onPrimaryContainer = Color(0xFFFFEDE4),
            secondary = baseSecondary,
            onSecondary = Color.White,
            secondaryContainer = baseSecondary.copy(alpha = 0.22f),
            onSecondaryContainer = Color(0xFFD6FFF1),
            tertiary = baseTertiary,
            onTertiary = Color(0xFF1F1600),
            background = Color(0xFF161311),
            onBackground = onBg,
            surface = Color(0xFF201C19),
            onSurface = onBg,
            surfaceVariant = Color(0xFF2C2622),
            onSurfaceVariant = onBg.copy(alpha = 0.78f),
            outline = Color(0xFF4D433C)
        )
    } else {
        val onBg = customTextColor ?: Color(0xFF1E1916)
        lightColorScheme(
            primary = basePrimary,
            onPrimary = Color.White,
            primaryContainer = basePrimary.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF3E1400),
            secondary = baseSecondary,
            onSecondary = Color.White,
            secondaryContainer = baseSecondary.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF00382B),
            tertiary = baseTertiary,
            onTertiary = Color(0xFF2B1C00),
            background = CreamSurface,
            onBackground = onBg,
            surface = WarmCardLight,
            onSurface = onBg,
            surfaceVariant = WarmContainerLight,
            onSurfaceVariant = onBg.copy(alpha = 0.72f),
            outline = Color(0xFFE7D8CC)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaraTypography,
        shapes = MaraShapes,
        content = content
    )
}
