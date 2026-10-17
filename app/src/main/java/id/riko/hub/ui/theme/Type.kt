package id.riko.hub.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.riko.hub.R

internal val NotoSans = FontFamily(
    Font(R.font.noto_sans_regular, FontWeight.Normal),
    Font(R.font.noto_sans_semibold, FontWeight.SemiBold),
)
internal val NotoCondensedBlack = FontFamily(Font(R.font.noto_sans_condensed_black, FontWeight.Black))
private fun type(size: Int, height: Int, weight: FontWeight, display: Boolean = false) = TextStyle(
    fontFamily = if (display) NotoCondensedBlack else NotoSans,
    fontWeight = weight, fontSize = size.sp, lineHeight = height.sp,
    letterSpacing = if (display) (-0.4).sp else 0.sp,
)
internal val RikoTypography = Typography(
    displayLarge = type(36, 40, FontWeight.Black, true),
    displayMedium = type(28, 32, FontWeight.Black, true),
    displaySmall = type(24, 28, FontWeight.Black, true),
    headlineLarge = type(28, 34, FontWeight.Black, true),
    headlineMedium = type(24, 30, FontWeight.Black, true),
    headlineSmall = type(22, 28, FontWeight.SemiBold),
    titleLarge = type(22, 28, FontWeight.SemiBold),
    titleMedium = type(17, 24, FontWeight.SemiBold),
    titleSmall = type(15, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24, FontWeight.Normal),
    bodyMedium = type(14, 20, FontWeight.Normal),
    bodySmall = type(12, 18, FontWeight.Normal),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 16, FontWeight.SemiBold),
    labelSmall = type(10, 14, FontWeight.SemiBold),
)
