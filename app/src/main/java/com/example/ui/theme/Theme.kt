package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode
import com.example.model.ReaderTheme

private val BookWiseLightScheme =
  lightColorScheme(
    primary = DeepInkNavy,
    onPrimary = PureWhite,
    primaryContainer = WarmStoneContainer,
    onPrimaryContainer = DeepInkNavy,
    secondary = AntiqueBronze,
    onSecondary = PureWhite,
    secondaryContainer = AntiqueBronzeContainer,
    onSecondaryContainer = InkBlack,
    tertiary = SlateSecondary,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftPaperSurface,
    onSurfaceVariant = SlateSecondary,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftPaperSurface,
    surfaceContainer = WarmStoneContainer,
    surfaceContainerHigh = ColorTokens.LightHigh,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = MutedBurgundy,
    onError = PureWhite,
  )

private val BookWiseDarkScheme =
  darkColorScheme(
    primary = WarmGoldDark,
    onPrimary = DarkBackground,
    primaryContainer = WarmGoldDarkContainer,
    onPrimaryContainer = IvoryText,
    secondary = WarmGoldDark,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceContainer,
    onSecondaryContainer = IvoryText,
    tertiary = MutedAsh,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = IvoryText,
    surface = DarkBackground,
    onSurface = IvoryText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = MutedAsh,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = ColorTokens.DarkHigh,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = ColorTokens.DarkError,
    onError = DarkBackground,
  )

private object ColorTokens {
  val LightHigh = androidx.compose.ui.graphics.Color(0xFFECEAE3)
  val DarkHigh = androidx.compose.ui.graphics.Color(0xFF282D37)
  val DarkError = androidx.compose.ui.graphics.Color(0xFFE28B8B)
}

val BookWiseShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun BookWiseTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (themeMode) {
      AppThemeMode.LIGHT -> false
      AppThemeMode.DARK -> true
      AppThemeMode.SYSTEM -> systemDark
    }
  val colorScheme = if (isDark) BookWiseDarkScheme else BookWiseLightScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BookWiseTypography,
    shapes = BookWiseShapes,
    content = content,
  )
}

fun readerPaletteFor(theme: ReaderTheme): ReaderPalette =
  when (theme) {
    ReaderTheme.PURE_WHITE -> ReaderPureWhitePalette
    ReaderTheme.SOFT_PAPER -> ReaderSoftPaperPalette
    ReaderTheme.SEPIA -> ReaderSepiaPalette
    ReaderTheme.DARK_CHARCOAL -> ReaderDarkCharcoalPalette
    ReaderTheme.PURE_BLACK -> ReaderPureBlackPalette
  }
