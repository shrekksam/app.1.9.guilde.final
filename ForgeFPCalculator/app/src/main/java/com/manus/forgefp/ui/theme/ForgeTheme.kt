package com.manus.forgefp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manus.forgefp.R

/** Couleurs de l'interface. Les états restent distincts des accents décoratifs. */
object ForgePalette {
    // En-têtes et panneaux de résultat
    val BackgroundDark = Color(0xFF132A35)
    val BackgroundDarkSecondary = Color(0xFF254651)
    val DarkDivider = Color(0xFF3A5560)
    val OnDarkPrimary = Color(0xFFF9F8F3)
    val OnDarkSecondary = Color(0xFFC1D0CF)
    val OnDarkMuted = Color(0xFFB2C2C1)

    // Accents : turquoise pour les actions, or pour la mise en valeur
    val Primary = Color(0xFF267B78)
    val PrimaryLight = Color(0xFF328E86)
    val PrimaryStrong = Color(0xFF17605E)
    val Gold = Color(0xFFD3AB67)
    val GoldStrong = Color(0xFF795624)
    val GoldSoft = Color(0xFFFFF4DE)

    // Fonds et textes du contenu
    val Surface = Color(0xFFF6F5F1)
    val Card = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFF1F3EF)
    val Border = Color(0xFFE2E6E0)
    val TextPrimary = Color(0xFF21353B)
    val TextSecondary = Color(0xFF617378)

    // Le rouge ne sert qu'aux erreurs et aux actions destructives
    val Success = Color(0xFF27836B)
    val SuccessStrong = Color(0xFF176D57)
    val Warning = Color(0xFFC28B3E)
    val WarningStrong = Color(0xFF855919)
    val Danger = Color(0xFFCF5450)
    val DangerStrong = Color(0xFFA5302D)
    val OnAccent = Color.White
}

/** Espacements standardisés (échelle 4 pt). */
object ForgeSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object ForgeRadii {
    val sm = 8.dp
    val md = 12.dp
    val lg = 18.dp
    val xl = 24.dp
    val pill = 999.dp
}

object ForgeElevation {
    val none = 0.dp
    val card = 1.dp
    val raised = 3.dp
    val overlay = 8.dp
}

object ForgeMotion {
    const val fast = 150
    const val normal = 200
    const val slow = 250
}

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int): Font = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val InterFamily: FontFamily = FontFamily(
    inter(400),
    inter(500),
    inter(600),
    inter(700),
    inter(800),
)

val ForgeTypography = Typography(
    headlineLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 29.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val ForgeColorScheme = lightColorScheme(
    primary = ForgePalette.Primary,
    onPrimary = ForgePalette.OnAccent,
    primaryContainer = ForgePalette.SurfaceSecondary,
    onPrimaryContainer = ForgePalette.PrimaryStrong,
    secondary = ForgePalette.GoldStrong,
    onSecondary = ForgePalette.OnAccent,
    secondaryContainer = ForgePalette.GoldSoft,
    onSecondaryContainer = ForgePalette.GoldStrong,
    tertiary = ForgePalette.Success,
    onTertiary = ForgePalette.OnAccent,
    background = ForgePalette.Surface,
    onBackground = ForgePalette.TextPrimary,
    surface = ForgePalette.Card,
    onSurface = ForgePalette.TextPrimary,
    surfaceVariant = ForgePalette.SurfaceSecondary,
    onSurfaceVariant = ForgePalette.TextSecondary,
    outline = ForgePalette.Border,
    outlineVariant = ForgePalette.Border,
    error = ForgePalette.Danger,
    onError = ForgePalette.OnAccent,
    errorContainer = Color(0xFFFFEBE9),
    onErrorContainer = ForgePalette.DangerStrong,
)

@Composable
fun ForgeFpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColorScheme,
        typography = ForgeTypography,
        content = content,
    )
}
