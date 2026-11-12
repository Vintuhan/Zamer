package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ZamerColorScheme = lightColorScheme(
    primary = DeepGreen,
    onPrimary = PureWhite,
    primaryContainer = DeepGreen,
    onPrimaryContainer = PureWhite,
    secondary = PureBlack,
    onSecondary = PureWhite,
    secondaryContainer = DeepGreenSoft,
    onSecondaryContainer = PureBlack,
    tertiary = DeepGreen,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = PureWhite,
    onSurfaceVariant = PureBlack,
    outline = PureBlack,
    outlineVariant = BlackBorderSubtle,
    error = PureBlack,
    onError = PureWhite
)

private val ZamerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun ZamerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZamerColorScheme,
        typography = ZamerTypography,
        shapes = ZamerShapes,
        content = content
    )
}
