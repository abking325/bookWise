package com.example.model

enum class AppThemeMode(val id: String, val labelEn: String, val labelAm: String) {
  SYSTEM("system", "System", "የስርዓት ነባሪ"),
  LIGHT("light", "Pure Light", "ብሩህ (ነጭ)"),
  DARK("dark", "Charcoal Dark", "ጨለማ");

  companion object {
    fun fromId(id: String?): AppThemeMode = entries.firstOrNull { it.id == id } ?: LIGHT
  }
}

enum class NavigationStyle(val id: String, val labelEn: String, val labelAm: String) {
  TOP_BAR("top_bar", "Top Navigation", "የላይኛው አሰሳ"),
  FIXED_BOTTOM("fixed_bottom", "Fixed Bottom Bar", "ቋሚ የታችኛው አሞሌ"),
  FLOATING_BOTTOM("floating_bottom", "Floating Bottom Pill", "ተንሳፋፊ የታችኛው አሞሌ"),
  HAMBURGER("hamburger", "Hamburger Drawer", "የጎን መሳቢያ (ሜኑ)");

  companion object {
    fun fromId(id: String?): NavigationStyle = entries.firstOrNull { it.id == id } ?: TOP_BAR
  }
}

enum class ReaderTheme(val id: String, val labelEn: String, val labelAm: String) {
  SOFT_PAPER("soft_paper", "Paper", "ወረቀት"),
  PURE_WHITE("pure_white", "Pure White", "ንጹህ ነጭ"),
  SEPIA("sepia", "Sepia", "ሴፒያ"),
  MINT("mint", "Mint", "አረንጓዴ"),
  DARK_CHARCOAL("dark_charcoal", "Night", "ጥቁር ከሰል"),
  PURE_BLACK("pure_black", "Pure Black", "ንጹህ ጥቁር");

  companion object {
    fun fromId(id: String?): ReaderTheme =
      when (id) {
        "paper" -> SOFT_PAPER
        "night" -> DARK_CHARCOAL
        else -> entries.firstOrNull { it.id == id } ?: SOFT_PAPER
      }
  }
}

enum class ReaderFont(val id: String, val labelEn: String, val labelAm: String) {
  LITERATA_SERIF("literata", "Literata Editorial", "ሊተራታ (Serif)"),
  BODONI_MODA("bodoni_moda", "Bodoni Classic", "ቦዶኒ (Classic)"),
  MANROPE_SANS("manrope", "Manrope Clean", "ማንሮፕ (Sans)"),
  LORA_SERIF("lora", "Lora Book", "ሎራ (Book)"),
  ETHIOPIC_SERIF("ethiopic_serif", "Ethiopic Serif", "ግዕዝ ሴሪፍ"),
  ETHIOPIC_SANS("ethiopic_sans", "Ethiopic Sans", "ግዕዝ ሳንስ");

  companion object {
    fun fromId(id: String?): ReaderFont =
      when (id) {
        "playfair" -> BODONI_MODA
        "inter" -> MANROPE_SANS
        else -> entries.firstOrNull { it.id == id } ?: LITERATA_SERIF
      }
  }
}

enum class TextAlignmentMode(val id: String, val labelEn: String, val labelAm: String) {
  LEFT("left", "Left Aligned", "ወደ ግራ"),
  JUSTIFIED("justified", "Justified", "የተስተካከለ");

  companion object {
    fun fromId(id: String?): TextAlignmentMode = entries.firstOrNull { it.id == id } ?: LEFT
  }
}

enum class AppLanguage(val code: String, val labelEn: String, val labelAm: String) {
  ENGLISH("en", "English", "English"),
  AMHARIC("am", "አማርኛ (Amharic)", "አማርኛ");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      if (code == "am") AMHARIC else ENGLISH
  }
}

data class AppSettings(
  val themeMode: AppThemeMode = AppThemeMode.LIGHT,
  val navigationStyle: NavigationStyle = NavigationStyle.TOP_BAR,
  val carouselDepthEnabled: Boolean = true,
  val carouselShowSpineThickness: Boolean = true,
  val readerFont: ReaderFont = ReaderFont.LITERATA_SERIF,
  val readerFontSizeSp: Float = 18f,
  val readerLineHeightMultiplier: Float = 1.65f,
  val readerParagraphSpacingDp: Float = 14f,
  val readerMarginDp: Float = 22f,
  val readerTheme: ReaderTheme = ReaderTheme.SOFT_PAPER,
  val readerTextAlignment: TextAlignmentMode = TextAlignmentMode.LEFT,
  val autoSaveReadingPosition: Boolean = true,
  val ttsVoiceName: String = "",
  val ttsSpeechRate: Float = 1.0f,
  val ttsPitch: Float = 1.0f,
  val ttsDoubleTapEnabled: Boolean = true,
  val ttsBackgroundPlaybackEnabled: Boolean = true,
  val ttsAutoAdvanceChapter: Boolean = true,
  val ttsHighlightSentence: Boolean = true,
  val appLanguage: AppLanguage = AppLanguage.ENGLISH,
)
