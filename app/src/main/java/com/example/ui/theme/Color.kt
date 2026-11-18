package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// BookWise Editorial Light Palette — Pure White Gallery Canvas, Deep Charcoal Ink, Warm Terracotta Accent
val PureWhite = Color(0xFFFFFFFF)
val SoftPaperSurface = Color(0xFFF7F6F3)
val WarmStoneContainer = Color(0xFFF7F6F3)
val HairlineLight = Color(0xFFE9E9E6)
val InkBlack = Color(0xFF191919)
val SlateSecondary = Color(0xFF777772)
val MutedTertiary = Color(0xFF99958F)
val DeepInkNavy = Color(0xFF191919)
val TerracottaAccent = Color(0xFFDF8154)
val TerracottaRich = Color(0xFFC46B40)
val TerracottaContainer = Color(0xFFFBF1EC)
val MutedBurgundy = Color(0xFF9E3B3B)

// BookWise Editorial Dark Palette — True Editorial Charcoal
val DarkBackground = Color(0xFF111111)
val DarkSurface = Color(0xFF1B1B1B)
val DarkSurfaceContainer = Color(0xFF232322)
val HairlineDark = Color(0xFF303030)
val IvoryText = Color(0xFFF4F2EE)
val MutedAsh = Color(0xFFA5A19B)
val WarmTerracottaDark = Color(0xFFE48F64)
val WarmTerracottaDarkContainer = Color(0xFF33231B)

// Independent Reader Palettes (Pure White, Soft Paper, Sepia, Mint, Dark Charcoal, Pure Black)
data class ReaderPalette(
  val background: Color,
  val text: Color,
  val secondaryText: Color,
  val accent: Color,
  val highlightParagraph: Color,
  val highlightSentence: Color,
  val userHighlightYellow: Color,
  val divider: Color,
)

val ReaderSoftPaperPalette =
  ReaderPalette(
    background = Color(0xFFFAF6EE),
    text = Color(0xFF282622),
    secondaryText = Color(0xFF777268),
    accent = Color(0xFFC46B40),
    highlightParagraph = Color(0xFFF3EBD8),
    highlightSentence = Color(0xFFEAD9BA),
    userHighlightYellow = Color(0xFFF9E086),
    divider = Color(0xFFE7E0D2),
  )

val ReaderPureWhitePalette =
  ReaderPalette(
    background = Color(0xFFFFFFFF),
    text = Color(0xFF191919),
    secondaryText = Color(0xFF777772),
    accent = Color(0xFFDF8154),
    highlightParagraph = Color(0xFFFDF2EB),
    highlightSentence = Color(0xFFF7DFCF),
    userHighlightYellow = Color(0xFFFFE799),
    divider = Color(0xFFE9E9E6),
  )

val ReaderSepiaPalette =
  ReaderPalette(
    background = Color(0xFFEFE3CA),
    text = Color(0xFF423426),
    secondaryText = Color(0xFF7C6650),
    accent = Color(0xFF9A5528),
    highlightParagraph = Color(0xFFE5D2AE),
    highlightSentence = Color(0xFFD9BE8E),
    userHighlightYellow = Color(0xFFECC978),
    divider = Color(0xFFDECAA6),
  )

val ReaderMintPalette =
  ReaderPalette(
    background = Color(0xFFEBF3EF),
    text = Color(0xFF263632),
    secondaryText = Color(0xFF5C736E),
    accent = Color(0xFF2D6A5B),
    highlightParagraph = Color(0xFFD5E7E0),
    highlightSentence = Color(0xFFBEDBD0),
    userHighlightYellow = Color(0xFFC9E8D2),
    divider = Color(0xFFCDE0D8),
  )

val ReaderDarkCharcoalPalette =
  ReaderPalette(
    background = Color(0xFF242321),
    text = Color(0xFFE6E1D8),
    secondaryText = Color(0xFFA5A19B),
    accent = Color(0xFFE48F64),
    highlightParagraph = Color(0xFF352E27),
    highlightSentence = Color(0xFF4A3B2F),
    userHighlightYellow = Color(0xFF5C4928),
    divider = Color(0xFF383633),
  )

val ReaderPureBlackPalette =
  ReaderPalette(
    background = Color(0xFF000000),
    text = Color(0xFFE0DCD4),
    secondaryText = Color(0xFF8C8982),
    accent = Color(0xFFE48F64),
    highlightParagraph = Color(0xFF1F1B14),
    highlightSentence = Color(0xFF362A1F),
    userHighlightYellow = Color(0xFF4F4119),
    divider = Color(0xFF242424),
  )
