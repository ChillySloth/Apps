package com.chillysloth.neomusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chillysloth.neomusic.R

/**
 * Neubrutalism tokens: flat saturated fills, thick ink outlines and hard offset shadows.
 * In dark mode the ink flips to cream so outlines and shadows stay visible.
 */
@Immutable
data class NeoColors(
    val paper: Color,        // app background
    val surface: Color,      // neutral cards
    val surfaceAlt: Color,   // pressed / secondary neutral
    val ink: Color,          // outlines, shadows
    val text: Color,         // text on paper/surface
    val textMuted: Color,
    val onAccent: Color,     // text on accent fills (always dark)
    val yellow: Color,
    val pink: Color,
    val green: Color,
    val purple: Color,
    val orange: Color,
    val blue: Color,
    val isDark: Boolean,
) {
    val accents: List<Color> get() = listOf(yellow, pink, green, purple, orange, blue)

    /** Stable accent for an id, so the same album always gets the same color. */
    fun accentFor(key: Long): Color = accents[((key % accents.size) + accents.size).toInt() % accents.size]
}

val LightNeo = NeoColors(
    paper = Color(0xFFFFF4E0),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF3E6CC),
    ink = Color(0xFF111111),
    text = Color(0xFF111111),
    textMuted = Color(0xFF5B5548),
    onAccent = Color(0xFF111111),
    yellow = Color(0xFFFFD23F),
    pink = Color(0xFFFF90E8),
    green = Color(0xFFB8F36B),
    purple = Color(0xFFA78BFA),
    orange = Color(0xFFFF7A59),
    blue = Color(0xFF7CC6FE),
    isDark = false,
)

val DarkNeo = NeoColors(
    paper = Color(0xFF161513),
    surface = Color(0xFF242320),
    surfaceAlt = Color(0xFF302E2A),
    ink = Color(0xFFF4EBDD),
    text = Color(0xFFF4EBDD),
    textMuted = Color(0xFFB3AA9B),
    onAccent = Color(0xFF111111),
    yellow = Color(0xFFF5C83A),
    pink = Color(0xFFF08ADB),
    green = Color(0xFFA6E05E),
    purple = Color(0xFF9F86F2),
    orange = Color(0xFFF07355),
    blue = Color(0xFF6DB8EE),
    isDark = true,
)

@Immutable
data class NeoDimens(
    val border: Dp = 2.5.dp,
    val shadow: Dp = 4.dp,
    val shadowSmall: Dp = 3.dp,
    val radius: Dp = 14.dp,
    val radiusSmall: Dp = 10.dp,
)

val Display = FontFamily(Font(R.font.archivo_black, FontWeight.Normal))

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun grotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Grotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

@Immutable
data class NeoType(
    val hero: TextStyle = TextStyle(fontFamily = Display, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    val h1: TextStyle = TextStyle(fontFamily = Display, fontSize = 28.sp, lineHeight = 32.sp),
    val h2: TextStyle = TextStyle(fontFamily = Display, fontSize = 20.sp, lineHeight = 24.sp),
    val h3: TextStyle = TextStyle(fontFamily = Display, fontSize = 16.sp, lineHeight = 20.sp),
    val title: TextStyle = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    val body: TextStyle = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    val label: TextStyle = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp),
    val caption: TextStyle = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp),
)

val LocalNeoColors = staticCompositionLocalOf { LightNeo }
val LocalNeoDimens = staticCompositionLocalOf { NeoDimens() }
val LocalNeoType = staticCompositionLocalOf { NeoType() }

object Neo {
    val colors: NeoColors @Composable get() = LocalNeoColors.current
    val dimens: NeoDimens @Composable get() = LocalNeoDimens.current
    val type: NeoType @Composable get() = LocalNeoType.current
}

@Composable
fun NeoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkNeo else LightNeo
    // Material components we still use (dialogs, sliders, text fields) pick up these colors.
    val scheme = if (dark) darkColorScheme(
        primary = c.yellow, onPrimary = c.onAccent,
        secondary = c.pink, onSecondary = c.onAccent,
        background = c.paper, onBackground = c.text,
        surface = c.surface, onSurface = c.text,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
        outline = c.ink,
    ) else lightColorScheme(
        primary = c.ink, onPrimary = c.paper,
        secondary = c.pink, onSecondary = c.onAccent,
        background = c.paper, onBackground = c.text,
        surface = c.surface, onSurface = c.text,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
        outline = c.ink,
    )
    CompositionLocalProvider(
        LocalNeoColors provides c,
        LocalNeoDimens provides NeoDimens(),
        LocalNeoType provides NeoType(),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
