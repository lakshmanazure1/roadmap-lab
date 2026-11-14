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
 * Lab palette. Track and status colours use the Okabe–Ito palette, the colour-blind-safe set
 * recommended for scientific figures (Okabe & Ito; Wong, Nature Methods 2011): everyone can
 * tell the colours apart. Text stays neutral for contrast; colour marks tracks and status.
 */
@Immutable
data class LabColors(
    val bg: Color,
    val panel: Color,
    val grid: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val amber: Color,
    val error: Color,
    val isDark: Boolean,
    val ai: Color = Color(0xFF56B4E9),        // sky blue
    val accounting: Color = Color(0xFFE69F00), // orange
    val communication: Color = Color(0xFFCC79A7), // reddish purple
    val networking: Color = Color(0xFF009E73), // bluish green
    val done: Color = Color(0xFF009E73),
    val partly: Color = Color(0xFFF0E442),
    val morning: Color = Color(0xFFF0E442),
    val evening: Color = Color(0xFFCC79A7),
    /** The soft, airy light style (white cards on a sky-blue wash, black pill buttons). */
    val soft: Boolean = false,
    val bgTop: Color = bg,
    val bgMid: Color = bg,
) {
    fun track(name: String): Color = when {
        name.startsWith("AI") -> ai
        name.startsWith("Accounting") -> accounting
        name.startsWith("Communication") -> communication
        name.startsWith("Networking") -> networking
        else -> accent
    }

    /** Card topics follow their track's colour. */
    fun topic(name: String): Color = when (name) {
        "Python", "SQL", "ERP rules" -> ai
        "Accounting", "SAP" -> accounting
        "Communication" -> communication
        else -> accent
    }
}

val DarkLab = LabColors(
    bg = Color(0xFF0E1116), panel = Color(0xFF161B22), grid = Color(0xFF2A313C), text = Color(0xFFE6EDF3),
    muted = Color(0xFF8B949E), accent = Color(0xFF2BD4BD), onAccent = Color(0xFF0E1116), amber = Color(0xFFE3B341),
    error = Color(0xFFF85149), isDark = true,
)

val LightLab = LabColors(
    bg = Color(0xFFF6F8FA), panel = Color(0xFFFFFFFF), grid = Color(0xFFD0D7DE), text = Color(0xFF1F2328),
    muted = Color(0xFF59636E), accent = Color(0xFF0B7A6F), onAccent = Color(0xFFFFFFFF), amber = Color(0xFF9A6700),
    error = Color(0xFFCF222E), isDark = false,
    // Same Okabe–Ito hues, a little deeper so they show on white.
    ai = Color(0xFF2B8CC4), accounting = Color(0xFFC98700), communication = Color(0xFFB3588E),
    networking = Color(0xFF00805D), done = Color(0xFF00805D), partly = Color(0xFFA68F00),
    morning = Color(0xFFA68F00), evening = Color(0xFFB3588E),
)

/** Default look: white rounded cards on a soft sky-blue wash, black pills, thin type. */
val SoftLab = LabColors(
    bg = Color(0xFFF3F6F8), panel = Color(0xFFFFFFFF), grid = Color(0xFFE4E9ED), text = Color(0xFF111418),
    muted = Color(0xFF7D8690), accent = Color(0xFF111418), onAccent = Color(0xFFFFFFFF), amber = Color(0xFFB7791F),
    error = Color(0xFFD1432E), isDark = false,
    ai = Color(0xFF2B8CC4), accounting = Color(0xFFD08C00), communication = Color(0xFFB3588E),
    networking = Color(0xFF00805D), done = Color(0xFF00805D), partly = Color(0xFFC9A400),
    morning = Color(0xFFD08C00), evening = Color(0xFFB3588E),
    soft = true, bgTop = Color(0xFFCADDE7), bgMid = Color(0xFFE8F0F4),
)

val LocalLab = staticCompositionLocalOf { SoftLab }

val PlexSans = FontFamily(
    Font(R.font.plex_sans_light, FontWeight.Light),
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
)

/** Fixed-width digits so values line up. */
val PlexMono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_medium, FontWeight.Medium),
)

object LabType {
    val bigNumber = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 50.sp)
    val greeting = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 34.sp)
    val number = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
    val mono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val monoSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val label = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp)
    val softLabel = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
}

private fun plexTypography(): Typography {
    val b = Typography()
    fun TextStyle.plex() = copy(fontFamily = PlexSans)
    return Typography(
        displayLarge = b.displayLarge.plex(), displayMedium = b.displayMedium.plex(), displaySmall = b.displaySmall.plex(),
        headlineLarge = b.headlineLarge.plex(), headlineMedium = b.headlineMedium.plex(),
        headlineSmall = b.headlineSmall.plex().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = b.titleLarge.plex().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.plex().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.plex().copy(fontWeight = FontWeight.Medium),
        bodyLarge = b.bodyLarge.plex(), bodyMedium = b.bodyMedium.plex(), bodySmall = b.bodySmall.plex(),
        labelLarge = b.labelLarge.plex().copy(fontWeight = FontWeight.Medium),
        labelMedium = b.labelMedium.plex(), labelSmall = b.labelSmall.plex(),
    )
}

@Composable
fun RoadmapLabTheme(theme: String, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val lab = when (theme) {
        "dark" -> DarkLab
        "system" -> if (systemDark) DarkLab else SoftLab
        "classic" -> LightLab
        else -> SoftLab
    }
    val dark = lab.isDark
    val scheme = if (dark) {
        darkColorScheme(
            primary = lab.accent, onPrimary = lab.onAccent, secondary = lab.accent, onSecondary = lab.onAccent,
            tertiary = lab.amber, background = lab.bg, onBackground = lab.text, surface = lab.bg, onSurface = lab.text,
            surfaceVariant = lab.panel, onSurfaceVariant = lab.muted, outline = lab.grid, outlineVariant = lab.grid,
            error = lab.error, surfaceContainerLowest = lab.bg, surfaceContainerLow = lab.panel,
            surfaceContainer = lab.panel, surfaceContainerHigh = lab.panel, surfaceContainerHighest = lab.grid,
            secondaryContainer = lab.grid, onSecondaryContainer = lab.text, primaryContainer = lab.grid, onPrimaryContainer = lab.text,
        )
    } else {
        lightColorScheme(
            primary = lab.accent, onPrimary = lab.onAccent, secondary = lab.accent, onSecondary = lab.onAccent,
            tertiary = lab.amber, background = lab.bg, onBackground = lab.text, surface = lab.bg, onSurface = lab.text,
            surfaceVariant = lab.panel, onSurfaceVariant = lab.muted, outline = lab.grid, outlineVariant = lab.grid,
            error = lab.error, surfaceContainerLowest = lab.panel, surfaceContainerLow = lab.panel,
            surfaceContainer = lab.panel, surfaceContainerHigh = lab.panel, surfaceContainerHighest = lab.grid,
            secondaryContainer = lab.grid, onSecondaryContainer = lab.text, primaryContainer = lab.grid, onPrimaryContainer = lab.text,
        )
    }
    CompositionLocalProvider(LocalLab provides lab) {
        MaterialTheme(colorScheme = scheme, typography = plexTypography(), content = content)
    }
}
