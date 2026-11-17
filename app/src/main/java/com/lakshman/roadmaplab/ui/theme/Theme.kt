package com.lakshman.roadmaplab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material You. On Android 12 and later the colours come from your wallpaper (dynamic colour; Settings can turn
 * it off); otherwise a calm indigo baseline. Everything on screen reads its colours from the Material colour
 * scheme through this small [Palette]. The 10 categories use the Okabe–Ito palette (designed to be told apart
 * with colour-blindness) unchanged, and are always paired with a label, never colour alone. The done / warning
 * colours are gently shifted towards the theme's colour so they sit well with any wallpaper.
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
    val onAccentSoft: Color,
    val tertiarySoft: Color,
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

private val CATEGORY_LIGHT = mapOf(
    "sap" to Color(0xFF3D4BC4), "accounting" to Color(0xFFB87D00), "python" to Color(0xFF0072B2), "career" to Color(0xFFB3548A),
    "french" to Color(0xFF2E8FC4), "comms" to Color(0xFFD55E00), "evidence" to Color(0xFF9C8A00), "life" to Color(0xFF009E73),
    "practice" to Color(0xFF7E57C2), "review" to Color(0xFF5F6B7A), "app" to Color(0xFF6B7280),
)
private val CATEGORY_DARK = mapOf(
    "sap" to Color(0xFF9AA5FF), "accounting" to Color(0xFFE69F00), "python" to Color(0xFF56A8E0), "career" to Color(0xFFE08FBF),
    "french" to Color(0xFF7CC8F0), "comms" to Color(0xFFF08A4B), "evidence" to Color(0xFFE2CB3A), "life" to Color(0xFF3CC79A),
    "practice" to Color(0xFFB39DDB), "review" to Color(0xFFA9B4C2), "app" to Color(0xFF9CA3AF),
)

/** Baseline Material 3 schemes (seed: indigo), for phones without dynamic colour or when it's off. */
private val BaselineLight = lightColorScheme(
    primary = Color(0xFF4355B9), onPrimary = Color.White, primaryContainer = Color(0xFFDEE0FF), onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72), onSecondary = Color.White, secondaryContainer = Color(0xFFE0E1F9), onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD7F1), onTertiaryContainer = Color(0xFF2D1228),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onError = Color.White, onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21), surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC), onSurfaceVariant = Color(0xFF46464F), outline = Color(0xFF767680), outlineVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5F2FA), surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF), surfaceContainerHighest = Color(0xFFE4E1E9), inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7), inversePrimary = Color(0xFFBAC3FF),
)
private val BaselineDark = darkColorScheme(
    primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF08218A), primaryContainer = Color(0xFF293CA0), onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC3C5DD), onSecondary = Color(0xFF2D2F42), secondaryContainer = Color(0xFF434659), onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE6BAD7), onTertiary = Color(0xFF44263E), tertiaryContainer = Color(0xFF5D3C55), onTertiaryContainer = Color(0xFFFFD7F1),
    error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A), onError = Color(0xFF690005), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318), onBackground = Color(0xFFE4E1E9), surface = Color(0xFF121318), onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F), onSurfaceVariant = Color(0xFFC7C5D0), outline = Color(0xFF90909A), outlineVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1B1B21), surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF29292F), surfaceContainerHighest = Color(0xFF34343A), inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036), inversePrimary = Color(0xFF4355B9),
)

/**
 * A lighter version of Material's "harmonize": turn a fixed colour's hue up to 15° towards the theme's primary,
 * keeping its saturation and brightness, so meaning colours (green = done, categories) stay recognisable.
 */
fun harmonize(color: Color, primary: Color): Color {
    val a = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    val b = FloatArray(3).also { android.graphics.Color.colorToHSV(primary.toArgb(), it) }
    var diff = b[0] - a[0]
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    val turn = (diff * 0.5f).coerceIn(-15f, 15f)
    a[0] = (a[0] + turn + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor((color.alpha * 255).toInt(), a))
}

fun paletteOf(c: ColorScheme, dark: Boolean): Palette {
    val h = { x: Color -> harmonize(x, c.primary) }
    // Category colours stay exactly Okabe–Ito: shifting their hue would make some pairs hard to tell apart
    // (orange accounting turned olive next to the yellow evidence log). Only the status colours follow the theme.
    val cats = if (dark) CATEGORY_DARK else CATEGORY_LIGHT
    return Palette(
        isDark = dark,
        bg = c.surface, surface = c.surfaceContainerLow, surface2 = c.surfaceContainerHighest, border = c.outlineVariant,
        text = c.onSurface, text2 = c.onSurfaceVariant, text3 = c.outline,
        accent = c.primary, onAccent = c.onPrimary, accentSoft = c.primaryContainer, onAccentSoft = c.onPrimaryContainer,
        tertiarySoft = c.tertiaryContainer,
        success = h(if (dark) Color(0xFF6DD69E) else Color(0xFF1B7A4A)), successSoft = h(if (dark) Color(0xFF173626) else Color(0xFFD9F2E3)),
        warning = h(if (dark) Color(0xFFF0B45A) else Color(0xFF8F5300)), warningSoft = h(if (dark) Color(0xFF3A2A12) else Color(0xFFFBEBD3)),
        danger = c.error, dangerSoft = c.errorContainer,
        areas = cats,
    )
}

val LightPalette = paletteOfStatic(BaselineLight, false)
val DarkPalette = paletteOfStatic(BaselineDark, true)

/** Same as [paletteOf] without android.graphics (safe for static init in tests): no hue shift. */
private fun paletteOfStatic(c: ColorScheme, dark: Boolean) = Palette(
    isDark = dark, bg = c.surface, surface = c.surfaceContainerLow, surface2 = c.surfaceContainerHighest, border = c.outlineVariant,
    text = c.onSurface, text2 = c.onSurfaceVariant, text3 = c.outline, accent = c.primary, onAccent = c.onPrimary,
    accentSoft = c.primaryContainer, onAccentSoft = c.onPrimaryContainer, tertiarySoft = c.tertiaryContainer,
    success = if (dark) Color(0xFF6DD69E) else Color(0xFF1B7A4A), successSoft = if (dark) Color(0xFF173626) else Color(0xFFD9F2E3),
    warning = if (dark) Color(0xFFF0B45A) else Color(0xFF8F5300), warningSoft = if (dark) Color(0xFF3A2A12) else Color(0xFFFBEBD3),
    danger = c.error, dangerSoft = c.errorContainer, areas = if (dark) CATEGORY_DARK else CATEGORY_LIGHT,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Roboto, Android's own typeface (no fonts to download or bundle). Digits are tabular so numbers line up. */
private val Roboto = FontFamily.Default
private const val TABULAR = "tnum"

/**
 * The Material 3 type scale, used by role. Nothing below 12 sp; line heights from the M3 spec.
 * display → headlineSmall · title → titleLarge · section → titleMedium · body → bodyLarge ·
 * small → bodyMedium · caption → labelMedium.
 */
object Type {
    val display = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp)
    val title = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp)
    val section = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp)
    val body = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.25.sp)
    val bodyStrong = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp)
    val small = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
    val smallStrong = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
    val caption = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
    val stat = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp, fontFeatureSettings = TABULAR)
    val statSmall = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR)
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
}

private fun typography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge, displayMedium = b.displayMedium, displaySmall = b.displaySmall,
        headlineLarge = b.headlineLarge, headlineMedium = b.headlineMedium, headlineSmall = Type.display,
        titleLarge = Type.title, titleMedium = Type.section, titleSmall = Type.smallStrong,
        bodyLarge = Type.body, bodyMedium = Type.small, bodySmall = b.bodySmall,
        labelLarge = Type.smallStrong, labelMedium = Type.caption, labelSmall = b.labelSmall,
    )
}

/** Expressive, rounded shapes. */
private val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

/** [theme]: "light", "dark" or "system" (older values mean light). [dynamic]: colours from the wallpaper on Android 12+. */
@Composable
fun RoadmapLabTheme(theme: String, dynamic: Boolean = true, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    val context = LocalContext.current
    val useDynamic = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> BaselineDark
        else -> BaselineLight
    }
    val palette = remember(scheme, dark) { paletteOf(scheme, dark) }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typography(), shapes = LabShapes, content = content)
    }
}
