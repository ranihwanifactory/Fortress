package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameColorScheme = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = DeepNavyOutline,
    primaryContainer = PrimaryGoldDark,
    onPrimaryContainer = Color.White,
    secondary = PlayerBlue,
    onSecondary = Color.White,
    secondaryContainer = PlayerBlueLight,
    onSecondaryContainer = DeepNavyOutline,
    tertiary = EnemyCoral,
    onTertiary = Color.White,
    background = Color(0xFF102036),
    onBackground = TextOnDark,
    surface = HudSurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = Color(0xFF1E3250),
    onSurfaceVariant = TextMuted,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun CannonBattleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = AppTypography,
        content = content
    )
}
