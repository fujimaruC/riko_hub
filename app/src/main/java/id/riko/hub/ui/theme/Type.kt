package id.riko.hub.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.riko.hub.R

private val Nunito = FontFamily(
    Font(R.font.nunito_500, FontWeight.Medium),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold),
)
private val Fredoka = FontFamily(Font(R.font.fredoka_600, FontWeight.SemiBold))
private fun type(size: Int, height: Int, weight: FontWeight, display: Boolean = false) = TextStyle(
    fontFamily = if (display) Fredoka else Nunito, fontWeight = weight,
    fontSize = size.sp, lineHeight = height.sp, letterSpacing = 0.sp,
)
internal val RikoTypography = Typography(
    displayLarge = type(36, 40, FontWeight.SemiBold, true),
    displayMedium = type(28, 32, FontWeight.SemiBold, true),
    titleLarge = type(22, 28, FontWeight.ExtraBold),
    titleMedium = type(17, 24, FontWeight.Bold),
    titleSmall = type(15, 20, FontWeight.Bold),
    bodyLarge = type(16, 24, FontWeight.Medium),
    bodyMedium = type(14, 20, FontWeight.Medium),
    bodySmall = type(14, 20, FontWeight.Medium),
    labelLarge = type(14, 20, FontWeight.Bold),
    labelMedium = type(12, 16, FontWeight.SemiBold),
    labelSmall = type(11, 14, FontWeight.SemiBold),
)
