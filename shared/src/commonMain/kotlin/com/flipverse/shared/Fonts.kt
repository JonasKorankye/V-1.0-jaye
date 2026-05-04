package com.flipverse.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import flipverse.shared.generated.resources.Res
import flipverse.shared.generated.resources.bebas_neue_regular
import flipverse.shared.generated.resources.gotham_book
import flipverse.shared.generated.resources.lexend_medium
import flipverse.shared.generated.resources.staatliches_regular
import flipverse.shared.generated.resources.work_sans_bold
import flipverse.shared.generated.resources.work_sans_regular
import org.jetbrains.compose.resources.Font

@Composable
fun BebasNeueFont() = FontFamily(
    Font(Res.font.bebas_neue_regular)
)

@Composable
fun GothamBookFont() = FontFamily(
    Font(Res.font.gotham_book)
)

@Composable
fun StaatlichesFont() = FontFamily(
    Font(Res.font.staatliches_regular)
)
@Composable
fun WorkSansFont() = FontFamily(
    Font(Res.font.work_sans_regular)
)

@Composable
fun WorkSansBoldFont() = FontFamily(
    Font(Res.font.work_sans_bold)
)
@Composable
fun LexendMediumFont() = FontFamily(
    Font(Res.font.lexend_medium)
)

// Font size preference enum
enum class FontSizePreference(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f)
}

// Font size preference manager
object FontSizePreferenceManager {
    fun saveFontSizePreference(preference: FontSizePreference) {
        PreferencesRepository.saveFontSizeScale(preference.scale)
    }

    fun getFontSizePreference(): FontSizePreference {
        val savedScale = PreferencesRepository.getFontSizeScale()
        return FontSizePreference.values().find {
            kotlin.math.abs(it.scale - savedScale) < 0.01f
        } ?: FontSizePreference.NORMAL
    }

    fun getFontSizeScale(): Float {
        return PreferencesRepository.getFontSizeScale()
    }
}

object FontSize {
    private fun getScaledSize(baseSize: Float) =
        (baseSize * FontSizePreferenceManager.getFontSizeScale()).sp

    val EXTRA_SMALL get() = getScaledSize(10f)
    val SMALL get() = getScaledSize(12f)
    val REGULAR get() = getScaledSize(14f)
    val EXTRA_REGULAR get() = getScaledSize(16f)
    val MEDIUM get() = getScaledSize(18f)
    val EXTRA_MEDIUM get() = getScaledSize(20f)
    val LARGE get() = getScaledSize(30f)
    val EXTRA_LARGE get() = getScaledSize(40f)
}