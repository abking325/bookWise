package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode
import com.example.model.ReaderTheme

private val BookWiseLightScheme =
  lightColorScheme(
    primary = InkBlack,
    onPrimary = PureWhite,
    primaryContainer = SoftPaperSurface,
    onPrimaryContainer = InkBlack,
    secondary = TerracottaAccent,
    onSecondary = PureWhite,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = InkBlack,
    tertiary = MutedTertiary,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftPaperSurface,
    onSurfaceVariant = SlateSecondary,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftPaperSurface,
    surfaceContainer = SoftPaperSurface,
    surfaceContainerHigh = ColorTokens.LightHigh,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = MutedBurgundy,
    onError = PureWhite,
  )

private val BookWiseDarkScheme =
  darkColorScheme(
    primary = IvoryText,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceContainer,
    onPrimaryContainer = IvoryText,
    secondary = WarmTerracottaDark,
    onSecondary = DarkBackground,
    secondaryContainer = WarmTerracottaDarkContainer,
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
  val LightHigh = Color(0xFFF1F0EC)
  val DarkHigh = Color(0xFF282827)
  val DarkError = Color(0xFFE28B8B)
}

val BookWiseShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
  )

@Composable
fun BookWiseTheme(
  themeMode: AppThemeMode = AppThemeMode.LIGHT,
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
    ReaderTheme.SOFT_PAPER -> ReaderSoftPaperPalette
    ReaderTheme.PURE_WHITE -> ReaderPureWhitePalette
    ReaderTheme.SEPIA -> ReaderSepiaPalette
    ReaderTheme.MINT -> ReaderMintPalette
    ReaderTheme.DARK_CHARCOAL -> ReaderDarkCharcoalPalette
    ReaderTheme.PURE_BLACK -> ReaderPureBlackPalette
  }
