package com.cevier.shinian.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF182237)
val Muted = Color(0xFF697386)
val Paper = Color(0xFFF7F8FB)
val Surface = Color.White
val Blue = Color(0xFF2F6FED)
val BlueDark = Color(0xFF174EA6)
val BlueSoft = Color(0xFFE8F0FF)
val Orange = Color(0xFFFF8A34)
val OrangeSoft = Color(0xFFFFF0E4)
val Line = Color(0x1C182237)

private val ShinianColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Ink,
    secondary = Orange,
    onSecondary = Ink,
    secondaryContainer = OrangeSoft,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0EDF3),
    onSurfaceVariant = Muted,
    outline = Line,
)

private val ShinianTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    ),
)

@Composable
fun ShinianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShinianColors,
        typography = ShinianTypography,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
