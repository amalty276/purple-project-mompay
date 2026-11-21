package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FairSplitDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = TextPrimary,
    primaryContainer = CardSurfaceElevated,
    onPrimaryContainer = SoftLavender,
    secondary = FintechBlue,
    onSecondary = TextPrimary,
    secondaryContainer = IndigoDark,
    onSecondaryContainer = SoftLavender,
    tertiary = PendingAmber,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = IndigoDark,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CoralRed,
    onError = NavyDeep
)

private val FairSplitShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FairSplitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FairSplitDarkColorScheme,
        typography = FairSplitTypography,
        shapes = FairSplitShapes,
        content = content
    )
}
