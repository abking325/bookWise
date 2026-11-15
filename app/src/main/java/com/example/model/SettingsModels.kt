package com.example.model

enum class AppThemeMode(val id: String, val labelEn: String, val labelAm: String) {
  SYSTEM("system", "System Default", "የስርዓት ነባሪ"),
  LIGHT("light", "Pure Light", "ብሩህ (ነጭ)"),
  DARK("dark", "Archival Dark", "ጨለማ");

  companion object {
    fun fromId(id: String?): AppThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
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
  PURE_WHITE("pure_white", "Pure White", "ንጹህ ነጭ"),
  SOFT_PAPER("soft_paper", "Soft Paper", "ለስላሳ ወረቀት"),
  SEPIA("sepia", "Sepia", "ሴፒያ"),
  DARK_CHARCOAL("dark_charcoal", "Dark Charcoal", "ጥቁር ከሰል"),
  PURE_BLACK("pure_black", "Pure Black", "ንጹህ ጥቁር");

  companion object {
    fun fromId(id: String?): ReaderTheme = entries.firstOrNull { it.id == id } ?: SOFT_PAPER
  }
}

enum class ReaderFont(val id: String, val labelEn: String, val labelAm: String) {
  LORA_SERIF("lora", "Lora Editorial", "ሎራ (Serif)"),
  PLAYFAIR_CLASSIC("playfair", "Playfair Classic", "ፕሌይፌር (Classic)"),
  INTER_SANS("inter", "Inter Clean", "ኢንተር (Sans)"),
  ETHIOPIC_SERIF("ethiopic_serif", "Ethiopic Serif", "ግዕዝ ሴሪፍ"),
  ETHIOPIC_SANS("ethiopic_sans", "Ethiopic Sans", "ግዕዝ ሳንስ");

  companion object {
    fun fromId(id: String?): ReaderFont = entries.firstOrNull { it.id == id } ?: LORA_SERIF
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
  val readerFont: ReaderFont = ReaderFont.LORA_SERIF,
  val readerFontSizeSp: Float = 18f,
  val readerLineHeightMultiplier: Float = 1.65f,
  val readerParagraphSpacingDp: Float = 14f,
  val readerMarginDp: Float = 22f,
  val readerTheme: ReaderTheme = ReaderTheme.PURE_WHITE,
  val readerTextAlignment: TextAlignmentMode = TextAlignmentMode.LEFT,
  val autoSaveReadingPosition: Boolean = true,
  val ttsVoiceName: String = "",
  val ttsSpeechRate: Float = 1.0f,
  val ttsPitch: Float = 1.0f,
  val ttsDoubleTapEnabled: Boolean = true,
  val ttsBackgroundPlaybackEnabled: Boolean = true,
  val appLanguage: AppLanguage = AppLanguage.ENGLISH,
)
