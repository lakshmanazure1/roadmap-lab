package com.lakshman.roadmaplab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lakshman.roadmaplab.R

/**
 * One colour system, light and dark. Neutral surfaces carry the content; one accent marks the action;
 * the eight areas use the Okabe–Ito palette (designed to be told apart with colour-blindness) and are
 * always paired with a label, never colour alone. Text colours meet WCAG AA contrast on their surfaces.
 */
@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    private val areas: Map<String, Color>,
) {
    fun area(id: String): Color = areas[id] ?: text2
}

private val OKABE_ITO_LIGHT = mapOf(
    "python" to Color(0xFF0072B2), "accounting" to Color(0xFFE69F00), "french" to Color(0xFF56B4E9),
    "comms" to Color(0xFFD55E00), "linkedin" to Color(0xFFCC79A7), "evidence" to Color(0xFFC4AD00),
    "app" to Color(0xFF6B7280), "life" to Color(0xFF009E73), "review" to Color(0xFF3D4BC4),
)
private val OKABE_ITO_DARK = OKABE_ITO_LIGHT + mapOf(
    "python" to Color(0xFF3F97D6), "evidence" to Color(0xFFE2CB3A), "app" to Color(0xFF9CA3AF), "review" to Color(0xFF8E9BFF),
)

val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFF4F5F7), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEF0F3), border = Color(0xFFE1E4E9),
    text = Color(0xFF15181C), text2 = Color(0xFF545D69), text3 = Color(0xFF7A838E),
    accent = Color(0xFF2B57D0), onAccent = Color(0xFFFFFFFF), accentSoft = Color(0xFFE6ECFB),
    success = Color(0xFF1D7A4C), successSoft = Color(0xFFE3F3EA), warning = Color(0xFF9A5B00), warningSoft = Color(0xFFFBF0DD),
    danger = Color(0xFFB8382B), dangerSoft = Color(0xFFFBE7E4), areas = OKABE_ITO_LIGHT,
)

val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF0F1114), surface = Color(0xFF171A1F), surface2 = Color(0xFF1F2329), border = Color(0xFF2A2F37),
    text = Color(0xFFE8EBEE), text2 = Color(0xFFA9B1BB), text3 = Color(0xFF7F8893),
    accent = Color(0xFF86A6FF), onAccent = Color(0xFF0D1530), accentSoft = Color(0xFF1C2643),
    success = Color(0xFF52C48D), successSoft = Color(0xFF14291F), warning = Color(0xFFE6A84A), warningSoft = Color(0xFF2E2414),
    danger = Color(0xFFF07C70), dangerSoft = Color(0xFF331B19), areas = OKABE_ITO_DARK,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val PlexSans = FontFamily(
    Font(R.font.plex_sans_light, FontWeight.Light),
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
)

/** Fixed-width digits so numbers line up in stats. */
val PlexMono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_medium, FontWeight.Medium),
)

/** The type scale: few sizes, clear steps. Nothing below 12 sp. */
object Type {
    val display = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp)
    val title = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp)
    val section = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyStrong = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
    val small = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val smallStrong = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp)
    val stat = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp)
    val statSmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp)
    val mono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
}

private fun typography(): Typography {
    val b = Typography()
    fun TextStyle.plex() = copy(fontFamily = PlexSans)
    return Typography(
        displayLarge = b.displayLarge.plex(), displayMedium = b.displayMedium.plex(), displaySmall = b.displaySmall.plex(),
        headlineLarge = b.headlineLarge.plex(), headlineMedium = b.headlineMedium.plex(), headlineSmall = b.headlineSmall.plex(),
        titleLarge = Type.title, titleMedium = Type.section, titleSmall = Type.smallStrong,
        bodyLarge = Type.body, bodyMedium = Type.body, bodySmall = Type.small,
        labelLarge = Type.bodyStrong, labelMedium = Type.smallStrong, labelSmall = Type.caption,
    )
}

/** "light" (default), "dark" or "system". Older values ("soft", "classic") mean light. */
@Composable
fun RoadmapLabTheme(theme: String, content: @Composable () -> Unit) {
    val p = when (theme) {
        "dark" -> DarkPalette
        "system" -> if (isSystemInDarkTheme()) DarkPalette else LightPalette
        else -> LightPalette
    }
    val scheme = if (p.isDark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.text,
        secondary = p.accent, onSecondary = p.onAccent, secondaryContainer = p.surface2, onSecondaryContainer = p.text,
        background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2,
        onSurfaceVariant = p.text2, outline = p.border, outlineVariant = p.border, error = p.danger,
        surfaceContainerLowest = p.bg, surfaceContainerLow = p.surface, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.text,
        secondary = p.accent, onSecondary = p.onAccent, secondaryContainer = p.surface2, onSecondaryContainer = p.text,
        background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2,
        onSurfaceVariant = p.text2, outline = p.border, outlineVariant = p.border, error = p.danger,
        surfaceContainerLowest = p.surface, surfaceContainerLow = p.surface, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}
