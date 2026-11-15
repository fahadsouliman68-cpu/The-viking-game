package com.theviking.lounge

import android.graphics.Color as AColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** ألوان الثيم الحالي. الألوان العامة (BG, CardBg...) تقرأ منه، فيتغير الثيم فوراً عند الحفظ. */
data class Palette(
    val bg: Color, val card: Color, val line: Color, val mut: Color,
    val accent: Color, val accent2: Color, val text: Color, val blue: Color, val surfV: Color,
    val light: Boolean,
)

object Pal {
    var cur by mutableStateOf(presetPalette("viking"))
}

private fun hex(v: Long) = Color(v)

fun presetPalette(id: String): Palette = when (id) {
    "light" -> Palette(
        hex(0xFFF4F6F8), hex(0xFFFFFFFF), hex(0xFFC9D2DB), hex(0xFF5B6B7B),
        hex(0xFFB7791F), hex(0xFF1E88C7), hex(0xFF1A2530), hex(0xFF2E7BFF), hex(0xFFE3E9EF), true,
    )
    "ocean" -> Palette(
        hex(0xFF071A2E), hex(0xFF0E2A45), hex(0xFF2C5378), hex(0xFF8FB0CC),
        hex(0xFF4FC3F7), hex(0xFF80DEEA), hex(0xFFE8F4FF), hex(0xFF2E7BFF), hex(0xFF163A5A), false,
    )
    "forest" -> Palette(
        hex(0xFF0B1A12), hex(0xFF13271B), hex(0xFF2E5A40), hex(0xFF9CB8A6),
        hex(0xFF9BD770), hex(0xFF7BE0B0), hex(0xFFE9F5EC), hex(0xFF2E7BFF), hex(0xFF1E3A2A), false,
    )
    "crimson" -> Palette(
        hex(0xFF150A0C), hex(0xFF211014), hex(0xFF5A2A33), hex(0xFFB39AA0),
        hex(0xFFE0524A), hex(0xFFFF8A80), hex(0xFFF6ECEE), hex(0xFF2E7BFF), hex(0xFF3A1B21), false,
    )
    else -> Palette( // viking (الافتراضي)
        hex(0xFF0A0F14), hex(0xFF141C24), hex(0xFF34475A), hex(0xFF93A3B3),
        hex(0xFFD9A441), hex(0xFF7FD6FF), hex(0xFFEEF1F4), hex(0xFF2E7BFF), hex(0xFF243241), false,
    )
}

fun parseHex(h: String): Color? =
    runCatching { Color(AColor.parseColor("#" + h.trim().removePrefix("#"))) }.getOrNull()

/** ثيم يدوي: يُشتق بقية الألوان من لون الخلفية ولون التمييز. */
fun customPalette(bgHex: String, accentHex: String): Palette {
    val bg = parseHex(bgHex) ?: Color(0xFF0A0F14)
    val acc = parseHex(accentHex) ?: Color(0xFFD9A441)
    val light = bg.luminance() > 0.5f
    val tint = if (light) Color.Black else Color.White
    return Palette(
        bg = bg, card = lerp(bg, tint, 0.06f), line = lerp(bg, tint, 0.25f), mut = lerp(bg, tint, 0.5f),
        accent = acc, accent2 = acc,
        text = if (light) Color(0xFF1A2530) else Color(0xFFEEF1F4),
        blue = Color(0xFF2E7BFF), surfV = lerp(bg, tint, 0.15f), light = light,
    )
}

fun paletteOf(cfg: Cfg): Palette =
    if (cfg.themeMode == "custom") customPalette(cfg.customBg, cfg.customAccent) else presetPalette(cfg.themeMode)

fun schemeOf(p: Palette): ColorScheme {
    val base = if (p.light) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = p.blue, secondary = p.accent, background = p.bg, surface = p.card,
        onBackground = p.text, onSurface = p.text, surfaceVariant = p.surfV, error = Bad,
    )
}

val THEME_PRESETS = listOf("viking", "light", "ocean", "forest", "crimson")
