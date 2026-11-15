package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial Light Palette — Pure White base with warm charcoal ink and antique bronze accent
val PureWhite = Color(0xFFFFFFFF)
val SoftPaperSurface = Color(0xFFFAF9F6)
val WarmStoneContainer = Color(0xFFF3F1EC)
val HairlineLight = Color(0xFFE6E3DC)
val InkBlack = Color(0xFF14171C)
val SlateSecondary = Color(0xFF5A626D)
val DeepInkNavy = Color(0xFF162032)
val AntiqueBronze = Color(0xFF9A7230)
val AntiqueBronzeContainer = Color(0xFFF4ECDC)
val MutedBurgundy = Color(0xFF8C3B3B)

// Editorial Dark Palette — Refined archival charcoal
val DarkBackground = Color(0xFF111317)
val DarkSurface = Color(0xFF181B21)
val DarkSurfaceContainer = Color(0xFF21252D)
val HairlineDark = Color(0xFF2E333D)
val IvoryText = Color(0xFFEDEAE3)
val MutedAsh = Color(0xFFA0A7B2)
val WarmGoldDark = Color(0xFFD2AE6D)
val WarmGoldDarkContainer = Color(0xFF332917)

// Independent Reader Theme Palettes
data class ReaderPalette(
  val background: Color,
  val text: Color,
  val secondaryText: Color,
  val accent: Color,
  val highlightParagraph: Color,
  val userHighlightYellow: Color,
  val divider: Color,
)

val ReaderPureWhitePalette =
  ReaderPalette(
    background = Color(0xFFFFFFFF),
    text = Color(0xFF181A1F),
    secondaryText = Color(0xFF666D77),
    accent = Color(0xFF8B6428),
    highlightParagraph = Color(0xFFFFF5DC),
    userHighlightYellow = Color(0xFFFFE799),
    divider = Color(0xFFEAE8E3),
  )

val ReaderSoftPaperPalette =
  ReaderPalette(
    background = Color(0xFFFAF7F2),
    text = Color(0xFF1E1D1A),
    secondaryText = Color(0xFF6A665E),
    accent = Color(0xFF8C6226),
    highlightParagraph = Color(0xFFF3E9D2),
    userHighlightYellow = Color(0xFFF9E086),
    divider = Color(0xFFE8E2D6),
  )

val ReaderSepiaPalette =
  ReaderPalette(
    background = Color(0xFFF2E6D0),
    text = Color(0xFF35281C),
    secondaryText = Color(0xFF705B47),
    accent = Color(0xFF7D4E20),
    highlightParagraph = Color(0xFFE6D2B0),
    userHighlightYellow = Color(0xFFECC978),
    divider = Color(0xFFDFCEB3),
  )

val ReaderDarkCharcoalPalette =
  ReaderPalette(
    background = Color(0xFF1A1C20),
    text = Color(0xFFE3DFD7),
    secondaryText = Color(0xFF9AA0AA),
    accent = Color(0xFFD2AE6D),
    highlightParagraph = Color(0xFF2B271D),
    userHighlightYellow = Color(0xFF5A4B20),
    divider = Color(0xFF2D3139),
  )

val ReaderPureBlackPalette =
  ReaderPalette(
    background = Color(0xFF000000),
    text = Color(0xFFDCD8D0),
    secondaryText = Color(0xFF8C929C),
    accent = Color(0xFFD2AE6D),
    highlightParagraph = Color(0xFF1F1B12),
    userHighlightYellow = Color(0xFF4F4119),
    divider = Color(0xFF1F2228),
  )
